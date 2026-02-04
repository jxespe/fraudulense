package com.example.fraudulens.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ScamModelManager {
    private static final String TAG = "ScamModelManager";
    private static final String PREFS = "fraudulens_ml";
    private static final String KEY_LAST_MODEL_CHECK = "last_model_check";
    private static final String MODEL_FILE_NAME = "scam_detector.tflite";
    private static final long MODEL_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L;

    private static Interpreter interpreter;
    private static long interpreterLastModified = 0L;

    public static void prefetchLatestModel(Context ctx) {
        ensureModelDownloaded(ctx);
    }

    public static Float predict(Context ctx, String text) {
        if (ctx == null) return null;
        ensureModelDownloaded(ctx);
        Interpreter local = getInterpreter(ctx);
        if (local == null) return null;

        float[][] input = new float[1][ScamTextVectorizer.VECTOR_SIZE];
        input[0] = ScamTextVectorizer.vectorize(text);
        float[][] output = new float[1][1];

        try {
            synchronized (ScamModelManager.class) {
                local.run(input, output);
            }
            return output[0][0];
        } catch (Exception e) {
            Log.e(TAG, "Model inference failed", e);
            return null;
        }
    }

    private static void ensureModelDownloaded(Context ctx) {
        Context appCtx = ctx.getApplicationContext();
        long now = System.currentTimeMillis();
        long lastCheck = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_MODEL_CHECK, 0L);
        if (now - lastCheck < MODEL_CHECK_INTERVAL_MS) {
            return;
        }

        appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_MODEL_CHECK, now)
                .apply();

        File modelFile = new File(appCtx.getFilesDir(), MODEL_FILE_NAME);
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("models/" + MODEL_FILE_NAME);

        ref.getFile(modelFile)
                .addOnSuccessListener(task -> {
                    synchronized (ScamModelManager.class) {
                        interpreter = null;
                        interpreterLastModified = 0L;
                    }
                    Log.d(TAG, "Model downloaded");
                })
                .addOnFailureListener(e ->
                        Log.d(TAG, "No remote model available yet", e));
    }

    private static Interpreter getInterpreter(Context ctx) {
        Context appCtx = ctx.getApplicationContext();
        File modelFile = new File(appCtx.getFilesDir(), MODEL_FILE_NAME);
        if (!modelFile.exists()) return null;

        long lastModified = modelFile.lastModified();
        synchronized (ScamModelManager.class) {
            if (interpreter != null && interpreterLastModified == lastModified) {
                return interpreter;
            }
            try {
                MappedByteBuffer buffer = loadModelFile(modelFile);
                Interpreter.Options options = new Interpreter.Options();
                options.setNumThreads(2);
                interpreter = new Interpreter(buffer, options);
                interpreterLastModified = lastModified;
                return interpreter;
            } catch (Exception e) {
                Log.e(TAG, "Failed to load model", e);
                interpreter = null;
                interpreterLastModified = 0L;
                return null;
            }
        }
    }

    private static MappedByteBuffer loadModelFile(File file) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            FileChannel channel = inputStream.getChannel();
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }
    }
}
