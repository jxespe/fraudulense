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

        Log.d("REGISTER_DEBUG", "=== REGISTRATION ATTEMPT ===");
        Log.d("REGISTER_DEBUG", "Original email: " + email);
        Log.d("REGISTER_DEBUG", "Normalized email: " + normalizedEmail);
        Log.d("REGISTER_DEBUG", "Password length: " + trimmedPassword.length());
        Log.d("REGISTER_DEBUG", "Password hash: " + hash);

        db.collection("users")
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        Log.d("REGISTER_DEBUG", "Email already exists: " + normalizedEmail);
                        cb.onComplete(false);
                        return;
                    }

                    Map<String, Object> user = new HashMap<>();
                    user.put("name", name);
                    user.put("email", normalizedEmail);
                    user.put("passwordHash", hash);
                    user.put("createdAt", FieldValue.serverTimestamp());

                    Log.d("REGISTER_DEBUG", "Creating user with email: " + normalizedEmail);

                    db.collection("users")
                            .add(user)
                            .addOnSuccessListener(d -> {
                                Log.d("REGISTER_DEBUG", "Registration successful! Document ID: " + d.getId());
                                cb.onComplete(true);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "register failed", e);
                                e.printStackTrace();
                                cb.onComplete(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "email check failed", e);
                    e.printStackTrace();
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
        final String originalEmail = email.trim(); // For legacy accounts
        // Trim password to ensure consistency with registration
        final String trimmedPassword = password.trim();
        final String hash = PasswordUtil.hashPassword(trimmedPassword);

        Log.d("LOGIN_DEBUG", "=== LOGIN ATTEMPT ===");
        Log.d("LOGIN_DEBUG", "Original email: " + email);
        Log.d("LOGIN_DEBUG", "Normalized email: " + normalizedEmail);
        Log.d("LOGIN_DEBUG", "Password length: " + trimmedPassword.length());
        Log.d("LOGIN_DEBUG", "Password hash: " + hash);

        // Try normalized email first (for accounts created after fix)
        attemptLoginWithEmail(ctx, normalizedEmail, originalEmail, hash, cb, true);
    }

    private static void attemptLoginWithEmail(
            Context ctx,
            String searchEmail,
            String fallbackEmail,
            String hash,
            SimpleCallback<Boolean> cb,
            boolean tryFallback
    ) {
        db.collection("users")
                .whereEqualTo("email", searchEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap.isEmpty()) {
                        Log.d("LOGIN_DEBUG", "No user found with email: " + searchEmail);
                        // Try fallback email if different from search email (for legacy accounts)
                        if (tryFallback && !searchEmail.equals(fallbackEmail)) {
                            Log.d("LOGIN_DEBUG", "Trying fallback with original email format: " + fallbackEmail);
                            attemptLoginWithEmail(ctx, fallbackEmail, fallbackEmail, hash, cb, false);
                        } else {
                            cb.onComplete(false);
                        }
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    String storedHash = doc.getString("passwordHash");
                    String storedEmail = doc.getString("email");

                    Log.d("LOGIN_DEBUG", "User found!");
                    Log.d("LOGIN_DEBUG", "Stored email: " + storedEmail);
                    Log.d("LOGIN_DEBUG", "Stored hash: " + storedHash);
                    Log.d("LOGIN_DEBUG", "Stored hash length: " + (storedHash != null ? storedHash.length() : "null"));
                    Log.d("LOGIN_DEBUG", "Computed hash length: " + hash.length());

                    if (storedHash == null) {
                        Log.e("LOGIN_DEBUG", "ERROR: passwordHash field is null in database!");
                        cb.onComplete(false);
                        return;
                    }

                    boolean match = hash.equals(storedHash);
                    Log.d("LOGIN_DEBUG", "Hash match: " + match);
                    
                    // If hash doesn't match and this might be a legacy account with double-hashed password,
                    // try double-hashing the input password (for backward compatibility)
                    if (!match && tryFallback) {
                        Log.d("LOGIN_DEBUG", "Trying legacy double-hash check...");
                        String doubleHash = PasswordUtil.hashPassword(hash);
                        boolean legacyMatch = doubleHash.equals(storedHash);
                        Log.d("LOGIN_DEBUG", "Legacy hash match: " + legacyMatch);
                        if (legacyMatch) {
                            Log.d("LOGIN_DEBUG", "Login successful with legacy account!");
                            setLoggedIn(ctx, storedEmail.toLowerCase()); // Normalize for future use
                            cb.onComplete(true);
                            return;
                        }
                    }

                    if (match) {
                        Log.d("LOGIN_DEBUG", "Login successful!");
                        setLoggedIn(ctx, storedEmail.toLowerCase()); // Normalize for consistency
                    } else {
                        Log.d("LOGIN_DEBUG", "Login failed - hash mismatch");
                    }

                    cb.onComplete(match);
                })
                .addOnFailureListener(e -> {
                    Log.e("LOGIN_DEBUG", "Login query failed", e);
                    e.printStackTrace();
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
