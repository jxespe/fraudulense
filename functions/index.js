const admin = require("firebase-admin");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");

admin.initializeApp();

const db = admin.firestore();
const storage = admin.storage();

const DEFAULT_MIN_SAMPLES = 50;
const DEFAULT_MAX_SAMPLES = 1000;
const DEFAULT_TRAINING_ENDPOINT =
  "https://fraudulens-trainer-838199002873.us-central1.run.app/train";

function getEnvInt(key, fallback) {
  const raw = process.env[key];
  if (!raw) return fallback;
  const parsed = parseInt(raw, 10);
  return Number.isNaN(parsed) ? fallback : parsed;
}

async function buildDatasetSince(lastProcessedAt, limit) {
  let query = db
    .collection("reports")
    .orderBy("timestamp")
    .limit(limit);
  if (lastProcessedAt) {
    query = query.where("timestamp", ">", lastProcessedAt);
  }

  const snap = await query.get();
  if (snap.empty) {
    return null;
  }

  const lines = [];
  let maxTimestamp = null;

  snap.forEach((doc) => {
    const data = doc.data() || {};
    const message = typeof data.message === "string" ? data.message.trim() : "";
    const imageText = typeof data.imageText === "string" ? data.imageText.trim() : "";
    const label = data.label === 0 ? 0 : 1;
    const source = data.source || "report";
    const ts = data.timestamp && data.timestamp.toDate
      ? data.timestamp.toDate().toISOString()
      : null;

    if (message) {
      lines.push(
        JSON.stringify({
          text: message,
          label,
          source,
          timestamp: ts,
          reportId: doc.id,
        })
      );
    }

    if (imageText) {
      lines.push(
        JSON.stringify({
          text: imageText,
          label: 1,
          source: "report_image_ocr",
          timestamp: ts,
          reportId: doc.id,
        })
      );
    }

    if (data.timestamp && (!maxTimestamp || data.timestamp.toMillis() > maxTimestamp.toMillis())) {
      maxTimestamp = data.timestamp;
    }
  });

  if (!lines.length) {
    return null;
  }

  return { lines, maxTimestamp, count: lines.length };
}

async function saveDataset(lines) {
  const bucket = storage.bucket();
  const datasetId = `dataset_${Date.now()}.jsonl`;
  const filePath = `ml/datasets/${datasetId}`;
  const file = bucket.file(filePath);
  await file.save(lines.join("\n"), {
    contentType: "application/json",
    resumable: false,
  });
  return filePath;
}

async function triggerTraining(datasetPath, sampleCount, maxTimestamp) {
  const endpoint = process.env.TRAINING_ENDPOINT || DEFAULT_TRAINING_ENDPOINT;
  if (!endpoint) {
    logger.info("TRAINING_ENDPOINT not set. Dataset saved only.", { datasetPath });
    return false;
  }

  const payload = {
    datasetPath,
    outputModelPath: "models/scam_detector.tflite",
    sampleCount,
  };

  const res = await fetch(endpoint, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const text = await res.text();
    logger.error("Training endpoint failed", { status: res.status, text });
    return false;
  }

  const stateRef = db.collection("ml_training_meta").doc("state");
  await stateRef.set(
    {
      lastTrainedAt: maxTimestamp || admin.firestore.FieldValue.serverTimestamp(),
      lastModelPath: payload.outputModelPath,
      lastTrainedDataset: datasetPath,
      lastTrainedSamples: sampleCount,
      lastTrainedStatus: "success",
    },
    { merge: true }
  );

  return true;
}

async function runTrainingPipeline() {
  const queueRef = db.collection("ml_training_meta").doc("queue");
  const stateRef = db.collection("ml_training_meta").doc("state");

  const lockAcquired = await db.runTransaction(async (tx) => {
    const stateSnap = await tx.get(stateRef);
    const inProgress = stateSnap.exists ? stateSnap.get("trainingInProgress") : false;
    if (inProgress) {
      return false;
    }
    tx.set(
      stateRef,
      {
        trainingInProgress: true,
        trainingRequestedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
    return true;
  });

  if (!lockAcquired) {
    logger.info("Training already in progress.");
    return;
  }

  try {
    const maxSamples = getEnvInt("MAX_TRAIN_SAMPLES", DEFAULT_MAX_SAMPLES);
    const stateSnap = await stateRef.get();
    const lastProcessedAt = stateSnap.exists ? stateSnap.get("lastProcessedAt") : null;

    const dataset = await buildDatasetSince(lastProcessedAt, maxSamples);
    if (!dataset) {
      logger.info("No new samples to build dataset.");
      await queueRef.set({ ready: false, pendingCount: 0 }, { merge: true });
      return;
    }

    const datasetPath = await saveDataset(dataset.lines);
    logger.info("Dataset saved", { datasetPath, count: dataset.count });

    await stateRef.set(
      {
        lastProcessedAt: dataset.maxTimestamp || admin.firestore.FieldValue.serverTimestamp(),
        lastDatasetAt: admin.firestore.FieldValue.serverTimestamp(),
        lastDatasetPath: datasetPath,
        lastDatasetSamples: dataset.count,
      },
      { merge: true }
    );

    await queueRef.set({ ready: false, pendingCount: 0 }, { merge: true });

    await triggerTraining(datasetPath, dataset.count, dataset.maxTimestamp);
  } finally {
    await stateRef.set(
      { trainingInProgress: false, trainingFinishedAt: admin.firestore.FieldValue.serverTimestamp() },
      { merge: true }
    );
  }
}

exports.queueTrainingSample = onDocumentCreated(
  "reports/{id}",
  async () => {
    const minSamples = getEnvInt("MIN_TRAIN_SAMPLES", DEFAULT_MIN_SAMPLES);
    const queueRef = db.collection("ml_training_meta").doc("queue");
    const reachedThreshold = await db.runTransaction(async (tx) => {
      const snap = await tx.get(queueRef);
      const current = snap.exists ? snap.get("pendingCount") || 0 : 0;
      const next = current + 1;
      tx.set(
        queueRef,
        {
          pendingCount: next,
          ready: next >= minSamples,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          readyAt: next >= minSamples ? admin.firestore.FieldValue.serverTimestamp() : null,
        },
        { merge: true }
      );
      return next >= minSamples;
    });

    if (reachedThreshold) {
      await runTrainingPipeline();
    }
  }
);

exports.buildTrainingDataset = onSchedule("every 10 minutes", async () => {
  const queueRef = db.collection("ml_training_meta").doc("queue");
  const queueSnap = await queueRef.get();
  const queue = queueSnap.exists ? queueSnap.data() : {};
  if (!queue || !queue.ready) {
    logger.info("Training queue not ready.");
    return;
  }

  await runTrainingPipeline();
});
