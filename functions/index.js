const admin = require("firebase-admin");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { logger } = require("firebase-functions");

admin.initializeApp();

const db = admin.firestore();
const storage = admin.storage();

const DEFAULT_MIN_SAMPLES = 50;
const DEFAULT_MAX_SAMPLES = 1000;
const DEFAULT_TRAINING_ENDPOINT =
  "https://fraudulens-trainer-838199002873.us-central1.run.app/train";

function scoreHeuristic(text) {
  if (!text) return 0;
  const t = String(text).toLowerCase();
  const keywords = [
    "otp", "one time password", "pin", "bank", "account", "verify", "verification",
    "send money", "transfer", "payment", "urgent", "immediately", "prize", "lottery",
    "login", "password", "gcash", "wallet", "bit.ly", "tinyurl", "confirm", "winner"
  ];
  let hits = 0;
  keywords.forEach((k) => {
    if (t.includes(k)) hits += 1;
  });
  const linkHits = (t.split("http").length - 1);
  const raw = Math.min(1, (hits * 0.12) + (linkHits * 0.2));
  return raw;
}

exports.scoreScamText = onRequest({ cors: true }, async (req, res) => {
  try {
    const text = req.body && req.body.text ? String(req.body.text) : "";
    const score = scoreHeuristic(text);
    res.status(200).json({
      score,
      isScam: score >= 0.6,
      model: "cloud_heuristic_v1",
    });
  } catch (e) {
    res.status(500).json({ error: "inference_failed" });
  }
});

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

exports.buildTrainingDataset = onSchedule("every 24 hours", async () => {
  const queueRef = db.collection("ml_training_meta").doc("queue");
  const queueSnap = await queueRef.get();
  const queue = queueSnap.exists ? queueSnap.data() : {};
  if (!queue || !queue.ready) {
    logger.info("Training queue not ready.");
    return;
  }

  await runTrainingPipeline();
});

exports.deleteUserAccount = onCall({ region: "asia-southeast1" }, async (request) => {
  const auth = request.auth;
  if (!auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }

  const isAdminClaim = auth.token && auth.token.admin === true;
  const adminDoc = await db.collection("admin_users").doc(auth.uid).get();
  const isAdminUser = adminDoc.exists;
  if (!isAdminClaim && !isAdminUser) {
    throw new HttpsError("permission-denied", "Admin access required.");
  }

  const data = request.data || {};
  const userDocId = typeof data.userDocId === "string" ? data.userDocId.trim() : "";
  const authUid = typeof data.authUid === "string" ? data.authUid.trim() : "";
  const email = typeof data.email === "string" ? data.email.trim() : "";

  if (!userDocId && !authUid && !email) {
    throw new HttpsError("invalid-argument", "userDocId, authUid, or email is required.");
  }

  let authDeleted = false;
  if (authUid) {
    await admin.auth().deleteUser(authUid);
    authDeleted = true;
  } else if (email) {
    try {
      const userRecord = await admin.auth().getUserByEmail(email);
      await admin.auth().deleteUser(userRecord.uid);
      authDeleted = true;
    } catch (err) {
      if (err && err.code !== "auth/user-not-found") {
        throw new HttpsError("internal", "Failed to delete auth user.");
      }
    }
  }

  let userDocDeleted = false;
  if (userDocId) {
    await db.collection("users").doc(userDocId).delete();
    userDocDeleted = true;
  }

  return { ok: true, authDeleted, userDocDeleted };
});

exports.sendUserNotification = onCall({ region: "asia-southeast1" }, async (request) => {
  const auth = request.auth;
  if (!auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }

  const isAdminClaim = auth.token && auth.token.admin === true;
  const adminDoc = await db.collection("admin_users").doc(auth.uid).get();
  const isAdminUser = adminDoc.exists;
  if (!isAdminClaim && !isAdminUser) {
    throw new HttpsError("permission-denied", "Admin access required.");
  }

  const data = request.data || {};
  const userDocId = typeof data.userDocId === "string" ? data.userDocId.trim() : "";
  const email = typeof data.email === "string" ? data.email.trim().toLowerCase() : "";
  const title = typeof data.title === "string" ? data.title.trim() : "FrauduLens Alert";
  const body =
    typeof data.body === "string" && data.body.trim()
      ? data.body.trim()
      : "Your post was flagged by the admin. Please review your content.";

  let userSnap = null;
  if (userDocId) {
    userSnap = await db.collection("users").doc(userDocId).get();
  } else if (email) {
    const q = await db.collection("users").where("email", "==", email).limit(1).get();
    userSnap = q.empty ? null : q.docs[0];
  }

  if (!userSnap || !userSnap.exists) {
    throw new HttpsError("not-found", "User not found.");
  }

  const userData = userSnap.data() || {};
  const tokens = Array.isArray(userData.fcmTokens)
    ? userData.fcmTokens.filter((t) => typeof t === "string" && t.trim())
    : [];
  if (!tokens.length) {
    return { ok: false, reason: "no_tokens" };
  }

  const messagePayload = {
    notification: { title, body },
    data: {
      type: "admin_warning",
      userId: userSnap.id,
    },
  };

  try {
    const response = await admin.messaging().sendMulticast({
      tokens,
      ...messagePayload,
    });
    return { ok: true, sent: response.successCount, failed: response.failureCount };
  } catch (err) {
    const rawMessage = err && err.message ? String(err.message) : "";
    logger.error("sendMulticast failed, falling back to send()", err);
    // Fallback to per-token send to avoid /batch issues.
    if (!rawMessage.includes("/batch")) {
      throw err;
    }
    const results = await Promise.allSettled(
      tokens.map((token) =>
        admin.messaging().send({
          token,
          ...messagePayload,
        })
      )
    );
    const sent = results.filter((r) => r.status === "fulfilled").length;
    const failed = results.length - sent;
    return { ok: sent > 0, sent, failed };
  }
});
