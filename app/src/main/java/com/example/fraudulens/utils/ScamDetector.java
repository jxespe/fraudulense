package com.example.fraudulens.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.regex.Pattern;

public class ScamDetector {
    private static final Pattern suspicious = Pattern.compile("(?i)(bit\\.ly|tinyurl|verify|account|bank|login|confirm|transfer|prize|winner|urgent)");
    private static final float ML_THRESHOLD = 0.6f;
    private static final float CLOUD_THRESHOLD = 0.6f;

    public static boolean isScam(Context ctx, String text) {
        if (text == null) return false;
        Float score = ScamModelManager.predict(ctx, text);
        if (score != null) {
            return score >= ML_THRESHOLD;
        }
        return isScamHeuristic(text);
    }

    public static boolean isScam(String text) {
        return isScamHeuristic(text);
    }

    public interface HybridCallback {
        void onResult(boolean isScam, Float score, String source);
    }

    public static boolean isScamLocal(Context ctx, String text) {
        return isScam(ctx, text);
    }

    public static void checkHybrid(Context ctx, String text, HybridCallback cb) {
        if (cb == null) return;
        boolean local = isScamLocal(ctx, text);
        Float localScore = ScamModelManager.predict(ctx, text);
        runOnMain(() -> cb.onResult(local, localScore, "local"));

        if (!CloudScamClient.isConfigured()) {
            return;
        }

        CloudScamClient.scoreAsync(ctx, text, score -> {
            if (score == null) return;
            boolean scam = score >= CLOUD_THRESHOLD;
            runOnMain(() -> cb.onResult(scam, score, "cloud"));
        });
    }

    private static void runOnMain(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }

    private static boolean isScamHeuristic(String text) {
        if (text == null) return false;
        if (suspicious.matcher(text).find()) return true;
        if (text.length() < 5) return false;
        // heuristic: many links
        int links = (text.split("http").length - 1);
        return links > 0;
    }
}
