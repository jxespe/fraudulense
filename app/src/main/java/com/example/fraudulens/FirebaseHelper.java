package com.example.fraudulens;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.example.fraudulens.utils.PasswordUtil;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "FirebaseHelper";

    // ───────────── SESSION (LOCAL LOGIN) ─────────────

    private static final String PREFS = "fraudulens_prefs";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_EMAIL = "email";

    public interface SimpleCallback<T> {
        void onComplete(T result);
    }

    public static boolean isLoggedIn(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_LOGGED_IN, false);
    }

    public static void setLoggedIn(Context ctx, String email) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public static void logout(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    public static String getLoggedInEmail(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_EMAIL, null);
    }

    // ───────────── USERS (CUSTOM AUTH) ─────────────

    /** REGISTER */
    public static void register(
            String name,
            String email,
            String password,
            SimpleCallback<Boolean> cb
    ) {
        // Normalize email to lowercase (must match login normalization)
        final String normalizedEmail = email.trim().toLowerCase();
        // Trim password to ensure consistency
        final String trimmedPassword = password.trim();
        String hash = PasswordUtil.hashPassword(trimmedPassword);

        db.collection("users")
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        cb.onComplete(false);
                        return;
                    }

                    Map<String, Object> user = new HashMap<>();
                    user.put("name", name);
                    user.put("email", normalizedEmail);
                    user.put("passwordHash", hash);
                    user.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("users")
                            .add(user)
                            .addOnSuccessListener(d -> cb.onComplete(true))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "register failed", e);
                                cb.onComplete(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "email check failed", e);
                    cb.onComplete(false);
                });
    }

    /** LOGIN */
    public static void login(
            Context ctx,
            String email,
            String password,
            SimpleCallback<Boolean> cb
    ) {
        final String normalizedEmail = email.trim().toLowerCase();
        // Trim password to ensure consistency with registration
        final String trimmedPassword = password.trim();
        final String hash = PasswordUtil.hashPassword(trimmedPassword);

        Log.d("LOGIN_DEBUG", "Email input: " + normalizedEmail);
        Log.d("LOGIN_DEBUG", "Password hash input: " + hash);

        db.collection("users")
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap.isEmpty()) {
                        Log.d("LOGIN_DEBUG", "No user found");
                        cb.onComplete(false);
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    String storedHash = doc.getString("passwordHash");

                    Log.d("LOGIN_DEBUG", "Stored hash: " + storedHash);

                    boolean match = hash.equals(storedHash);
                    Log.d("LOGIN_DEBUG", "Hash match: " + match);

                    if (match) {
                        setLoggedIn(ctx, normalizedEmail); // ✅ SAFE
                    }

                    cb.onComplete(match);
                })
                .addOnFailureListener(e -> {
                    Log.e("LOGIN_DEBUG", "Login query failed", e);
                    cb.onComplete(false);
                });
    }


    /** PASSWORD RESET (CHECK ONLY – CUSTOM FLOW) */
    public static void sendPasswordReset(String email, SimpleCallback<Boolean> cb) {
        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnCompleteListener(task ->
                        cb.onComplete(task.isSuccessful()
                                && task.getResult() != null
                                && !task.getResult().isEmpty()));
    }

    // ───────────── REPORTS ─────────────

    public static void addReport(Map<String, Object> data, SimpleCallback<Boolean> cb) {
        db.collection("reports")
                .add(data)
                .addOnSuccessListener(d -> cb.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "addReport failed", e);
                    cb.onComplete(false);
                });
    }

    public static ListenerRegistration listenReports(EventListener<QuerySnapshot> listener) {
        return db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    /** Get single report */
    public static void getReport(String id, SimpleCallback<DocumentSnapshot> cb) {
        db.collection("reports")
                .document(id)
                .get()
                .addOnSuccessListener(cb::onComplete)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getReport failed", e);
                    cb.onComplete(null);
                });
    }

    /** ✅ REQUIRED BY SearchFragment */
    public static void getReportByMessage(
            String query,
            SimpleCallback<Boolean> cb
    ) {
        db.collection("reports")
                .whereEqualTo("message", query)
                .limit(1)
                .get()
                .addOnCompleteListener(task ->
                        cb.onComplete(
                                task.isSuccessful()
                                        && task.getResult() != null
                                        && !task.getResult().isEmpty()
                        )
                )
                .addOnFailureListener(e -> cb.onComplete(false));
    }

    /** Resolve report */
    public static void resolveReport(String id, SimpleCallback<Boolean> cb) {
        db.collection("reports")
                .document(id)
                .update("status", "resolved")
                .addOnSuccessListener(v -> cb.onComplete(true))
                .addOnFailureListener(e -> cb.onComplete(false));
    }

    // ───────────── ANALYTICS (OPTIONAL) ─────────────

    public static void logEvent(String name, Bundle params) {
        FirebaseAnalytics analytics = MyApp.getAnalytics();
        if (analytics != null) {
            analytics.logEvent(name, params);
        }
    }
}
