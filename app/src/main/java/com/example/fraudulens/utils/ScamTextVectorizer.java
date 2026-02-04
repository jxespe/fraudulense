package com.example.fraudulens.utils;

import java.util.Locale;

public class ScamTextVectorizer {
    public static final int VECTOR_SIZE = 1024;

    public static float[] vectorize(String text) {
        float[] vec = new float[VECTOR_SIZE];
        if (text == null) return vec;

        String cleaned = text
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9 ]", " ");

        String[] tokens = cleaned.trim().split("\\s+");
        if (tokens.length == 1 && tokens[0].isEmpty()) {
            return vec;
        }

        for (String token : tokens) {
            if (token.isEmpty()) continue;
            int idx = (token.hashCode() & 0x7fffffff) % VECTOR_SIZE;
            vec[idx] += 1f;
        }

        float norm = 0f;
        for (float v : vec) {
            norm += v * v;
        }
        if (norm > 0f) {
            norm = (float) Math.sqrt(norm);
            for (int i = 0; i < vec.length; i++) {
                vec[i] /= norm;
            }
        }

        return vec;
    }
}
