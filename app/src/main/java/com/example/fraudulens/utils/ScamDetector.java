package com.example.fraudulens.utils;

import android.content.Context;

import java.util.regex.Pattern;

public class ScamDetector {
    private static final Pattern suspicious = Pattern.compile("(?i)(bit\\.ly|tinyurl|verify|account|bank|login|confirm|transfer|prize|winner|urgent)");
    private static final float ML_THRESHOLD = 0.6f;

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

    private static boolean isScamHeuristic(String text) {
        if (text == null) return false;
        if (suspicious.matcher(text).find()) return true;
        if (text.length() < 5) return false;
        // heuristic: many links
        int links = (text.split("http").length - 1);
        return links > 0;
    }
}
