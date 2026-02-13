import json
import os
import re
from datetime import datetime

import numpy as np
from flask import Flask, jsonify, request
from google.cloud import storage
import tensorflow as tf

VECTOR_SIZE = 1024
TOKEN_RE = re.compile(r"[^a-z0-9 ]")

app = Flask(__name__)


def vectorize(text: str) -> np.ndarray:
    vec = np.zeros((VECTOR_SIZE,), dtype=np.float32)
    if not text:
        return vec
    cleaned = TOKEN_RE.sub(" ", text.lower())
    tokens = [t for t in cleaned.strip().split() if t]
    if not tokens:
        return vec
    for token in tokens:
        idx = (java_string_hash(token) & 0x7FFFFFFF) % VECTOR_SIZE
        vec[idx] += 1.0
    norm = np.linalg.norm(vec)
    if norm > 0:
        vec /= norm
    return vec


def java_string_hash(value: str) -> int:
    h = 0
    for ch in value:
        h = (31 * h + ord(ch)) & 0xFFFFFFFF
    if h & 0x80000000:
        h = -((~h + 1) & 0xFFFFFFFF)
    return h


def parse_gcs_path(path: str):
    if path.startswith("gs://"):
        trimmed = path[5:]
        bucket, blob = trimmed.split("/", 1)
        return bucket, blob
    bucket = os.environ.get("GCS_BUCKET")
    if not bucket:
        raise ValueError("GCS_BUCKET is not set and datasetPath is not gs://")
    return bucket, path


def load_dataset(dataset_path: str):
    bucket_name, blob_path = parse_gcs_path(dataset_path)
    client = storage.Client()
    blob = client.bucket(bucket_name).blob(blob_path)
    raw = blob.download_as_text(encoding="utf-8")
    samples = []
    for line in raw.splitlines():
        line = line.strip()
        if not line:
            continue
        data = json.loads(line)
        text = (data.get("text") or "").strip()
        label = 1 if data.get("label") == 1 else 0
        if text:
            samples.append((text, label))
    return samples


def train_model(samples):
    x = np.stack([vectorize(t) for t, _ in samples])
    y = np.array([lbl for _, lbl in samples], dtype=np.float32)

    model = tf.keras.Sequential(
        [
            tf.keras.layers.Input(shape=(VECTOR_SIZE,)),
            tf.keras.layers.Dense(32, activation="relu"),
            tf.keras.layers.Dense(1, activation="sigmoid"),
        ]
    )
    model.compile(optimizer="adam", loss="binary_crossentropy", metrics=["accuracy"])
    model.fit(x, y, epochs=5, batch_size=32, verbose=0)
    return model


def save_tflite(model, output_path: str):
    bucket_name, blob_path = parse_gcs_path(output_path)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    client = storage.Client()
    blob = client.bucket(bucket_name).blob(blob_path)
    blob.upload_from_string(tflite_model, content_type="application/octet-stream")


@app.post("/train")
def train_endpoint():
    payload = request.get_json(silent=True) or {}
    dataset_path = payload.get("datasetPath")
    output_path = payload.get("outputModelPath")
    if not dataset_path or not output_path:
        return jsonify({"error": "datasetPath and outputModelPath are required"}), 400

    samples = load_dataset(dataset_path)
    if len(samples) < 10:
        return jsonify({"error": "not enough samples to train", "count": len(samples)}), 400

    model = train_model(samples)
    save_tflite(model, output_path)

    return jsonify(
        {
            "status": "ok",
            "trainedAt": datetime.utcnow().isoformat() + "Z",
            "sampleCount": len(samples),
            "outputModelPath": output_path,
        }
    )


@app.get("/healthz")
def healthz():
    return jsonify({"ok": True})
