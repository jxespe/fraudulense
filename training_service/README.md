# FrauduLens Training Service

Cloud Run service that trains a TFLite model from a JSONL dataset in GCS.

## Deploy (example)
```
gcloud run deploy fraudulens-trainer \
  --source . \
  --region us-central1 \
  --set-env-vars GCS_BUCKET=fraudulense.firebasestorage.app \
  --allow-unauthenticated
```

## Endpoint
- `POST /train`

Body:
```
{
  "datasetPath": "ml/datasets/dataset_*.jsonl",
  "outputModelPath": "models/scam_detector.tflite",
  "sampleCount": 123
}
```

## Notes
- If you pass `gs://bucket/path` for dataset or output, it will use that bucket.
- The model shape matches the app vectorizer (`VECTOR_SIZE = 1024`).
