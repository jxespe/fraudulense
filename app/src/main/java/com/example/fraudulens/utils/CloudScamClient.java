package com.example.fraudulens.utils;

import android.content.Context;
import android.util.Log;

import com.example.fraudulens.BuildConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CloudScamClient {
    private static final String TAG = "CloudScamClient";
    private static final int TIMEOUT_MS = 7000;

    public interface ScoreCallback {
        void onScore(Float score);
    }

    public static boolean isConfigured() {
        String endpoint = BuildConfig.CLOUD_SCAM_ENDPOINT;
        return endpoint != null && !endpoint.trim().isEmpty();
    }

    public static void scoreAsync(Context ctx, String text, ScoreCallback cb) {
        if (cb == null) return;
        if (text == null || text.trim().isEmpty()) {
            cb.onScore(null);
            return;
        }
        String endpoint = BuildConfig.CLOUD_SCAM_ENDPOINT;
        if (endpoint == null || endpoint.trim().isEmpty()) {
            cb.onScore(null);
            return;
        }
        new Thread(() -> {
            Float score = null;
            HttpURLConnection conn = null;
            try {
                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                JSONObject payload = new JSONObject();
                payload.put("text", text);
                payload.put("source", "android");
                byte[] body = payload.toString().getBytes("UTF-8");
                conn.setFixedLengthStreamingMode(body.length);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body);
                }

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        JSONObject res = new JSONObject(sb.toString());
                        if (res.has("score")) {
                            score = (float) res.optDouble("score", -1);
                        } else if (res.has("probability")) {
                            score = (float) res.optDouble("probability", -1);
                        } else if (res.has("confidence")) {
                            score = (float) res.optDouble("confidence", -1);
                        }
                        if (score != null && score < 0) {
                            score = null;
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Cloud score failed", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            cb.onScore(score);
        }).start();
    }
}
