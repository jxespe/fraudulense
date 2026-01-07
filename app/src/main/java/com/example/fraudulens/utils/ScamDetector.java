package com.example.fraudulens.utils;

import java.util.regex.Pattern;

public class ScamDetector {
    private static final Pattern suspicious = Pattern.compile("(?i)(bit\\.ly|tinyurl|verify|account|bank|login|confirm|transfer|prize|winner|urgent)");
    public static boolean isScam(String text) {
        if (text == null) return false;
        if (suspicious.matcher(text).find()) return true;
        if (text.length() < 5) return false;
        // heuristic: many links
        int links = (text.split("http").length - 1);
        return links > 0;
    }
}
