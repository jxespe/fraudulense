# FrauduLens ML Training Pipeline

This Functions package builds training datasets from Firestore
`ml_training_samples` and optionally calls a training endpoint.

## What it does
- `queueTrainingSample`: increments a queue counter whenever a new training
  sample is added.
- `buildTrainingDataset`: runs hourly, exports new samples to
  `gs://<default-bucket>/ml/datasets/` and calls `TRAINING_ENDPOINT` if set.

## Environment variables
- `MIN_TRAIN_SAMPLES` (default: 50) — samples needed to trigger a dataset build.
- `MAX_TRAIN_SAMPLES` (default: 1000) — maximum samples per dataset.
- `TRAINING_ENDPOINT` — HTTPS endpoint that trains a model from the dataset.

The endpoint receives JSON:
```
{
  "datasetPath": "ml/datasets/dataset_*.jsonl",
  "outputModelPath": "models/scam_detector.tflite",
  "sampleCount": 123
}
```

The repo includes a reference Cloud Run trainer in `training_service/`.

## Deploy
1. Install deps:
   - `cd functions`
   - `npm install`
2. Deploy:
   - `firebase deploy --only functions`

## Notes
- The Android app already uploads labeled samples to `ml_training_samples`.
- The app downloads the latest model from `models/scam_detector.tflite`.
