package com.example.fraudulens;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.example.fraudulens.utils.PasswordUtil;
import com.example.fraudulens.utils.PhoneFormatUtil;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import com.example.fraudulens.models.Report;
import com.google.firebase.Timestamp;

public class FirebaseHelper {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "FirebaseHelper";
    private static final String STORAGE_BUCKET = "gs://fraudulense.firebasestorage.app";

    // ───────────── SESSION (LOCAL LOGIN) ─────────────

    private static final String PREFS = "fraudulens_prefs";
    private static final String PREFS_ALERTS = "fraudulens_alerts_prefs";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone_number";
    private static final String KEY_LAST_SEEN_SCAM = "last_seen_scam";
    private static final String KEY_TRUSTED_NUMBERS = "trusted_numbers";
    private static final String KEY_TRUSTED_NAMES = "trusted_names";
    private static final String KEY_DETECTED_SCAMS = "detected_scam_sms";
    private static final String KEY_DISMISSED_SCAMS = "dismissed_scam_alerts";
    private static final String KEY_DISMISSED_SCAM_BODIES = "dismissed_scam_bodies";
    private static final String KEY_PENDING_FCM = "pending_fcm_token";
    private static final String REPORT_OWNER_MIGRATION_VERSION = "report_owner_migration_v1";

    public interface SimpleCallback<T> {
        void onComplete(T result);
    }

    public static boolean isLoggedIn(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_LOGGED_IN, false);
    }

    public static void setLoggedIn(Context ctx, String email) {
        String normalized = email;
        if (normalized == null || normalized.trim().isEmpty()) {
            FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
            if (authUser != null && authUser.getEmail() != null && !authUser.getEmail().trim().isEmpty()) {
                normalized = authUser.getEmail().trim();
            }
        }
        if (normalized != null) {
            normalized = normalized.trim().toLowerCase();
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_EMAIL, normalized)
                .apply();
        flushPendingFcmToken(ctx);
    }

    public static void setVerifiedPhone(Context ctx, String phoneNumber) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PHONE, phoneNumber)
                .apply();
    }

    public static void markPhoneVerified(String phoneNumber) {
        final String normalizedPhone = phoneNumber == null ? "" : phoneNumber.trim();
        if (normalizedPhone.isEmpty()) {
            return;
        }
        db.collection("users")
                .whereEqualTo("phoneNumber", normalizedPhone)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        String docId = snapshot.getDocuments().get(0).getId();
                        db.collection("users")
                                .document(docId)
                                .update("isVerified", true);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark phone verified", e));
    }

    public static void logout(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
        // Keep alerts preferences so dismissed scam messages stay hidden.
    }

    public static String getLoggedInEmail(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_EMAIL, null);
    }

    public static String getVerifiedPhone(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_PHONE, null);
    }

    public static String getUserKeyForLikes(Context ctx) {
        com.google.firebase.auth.FirebaseUser authUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (authUser != null && authUser.getUid() != null && !authUser.getUid().trim().isEmpty()) {
            return authUser.getUid();
        }
        String email = getLoggedInEmail(ctx);
        return email != null && !email.trim().isEmpty() ? email : null;
    }

    public static void setLastSeenScamTimestamp(Context ctx, long timestampMillis) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_SEEN_SCAM, timestampMillis)
                .apply();
    }

    public static long getLastSeenScamTimestamp(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SEEN_SCAM, 0L);
    }

    public static java.util.Set<String> getTrustedNumbers(Context ctx) {
        return new java.util.HashSet<>(
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .getStringSet(KEY_TRUSTED_NUMBERS, new java.util.HashSet<>())
        );
    }

    public static java.util.Set<String> getTrustedNames(Context ctx) {
        return new java.util.HashSet<>(
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .getStringSet(KEY_TRUSTED_NAMES, new java.util.HashSet<>())
        );
    }

    public static void saveTrustedContacts(Context ctx, java.util.Set<String> numbers, java.util.Set<String> names) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_TRUSTED_NUMBERS, new java.util.HashSet<>(numbers))
                .putStringSet(KEY_TRUSTED_NAMES, new java.util.HashSet<>(names))
                .apply();
    }

    public static boolean isTrustedMessage(Context ctx, String address, String body) {
        String normalizedNumber = normalizePhoneNumber(address);
        java.util.Set<String> numbers = getTrustedNumbers(ctx);
        if (!normalizedNumber.isEmpty() && numbers.contains(normalizedNumber)) {
            return true;
        }
        String normalizedBody = body == null ? "" : body.toLowerCase();
        for (String name : getTrustedNames(ctx)) {
            if (!name.isEmpty() && normalizedBody.contains(name)) {
                return true;
            }
        }
        return false;
    }

    public static String normalizePhoneNumber(String number) {
        if (number == null) return "";
        return number.replaceAll("[^0-9]", "");
    }

    public static void saveDetectedScamMessage(Context ctx, String address, String body, long dateMillis) {
        if (ctx == null || body == null || body.trim().isEmpty()) return;
        try {
            String key = buildScamKey(address, body, dateMillis);
            if (isDismissedScam(ctx, key)) {
                return;
            }
            JSONObject obj = new JSONObject();
            obj.put("address", address != null ? address : "");
            obj.put("body", body.trim());
            obj.put("date", dateMillis);
        Set<String> existing = ctx.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
                    .getStringSet(KEY_DETECTED_SCAMS, new HashSet<>());
            Set<String> copy = new HashSet<>(existing);
            copy.add(obj.toString());
            if (copy.size() > 200) {
                copy = trimDetectedScams(copy, 200);
            }
        ctx.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
                    .edit()
                    .putStringSet(KEY_DETECTED_SCAMS, copy)
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save detected scam message", e);
        }
    }

    public static List<Report> getDetectedScamMessages(Context ctx) {
        List<Report> results = new ArrayList<>();
        if (ctx == null) return results;
        Set<String> raw = ctx.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
                .getStringSet(KEY_DETECTED_SCAMS, new HashSet<>());
        for (String entry : raw) {
            if (entry == null || entry.trim().isEmpty()) continue;
            try {
                JSONObject obj = new JSONObject(entry);
                String address = obj.optString("address", "");
                String body = obj.optString("body", "");
                long dateMillis = obj.optLong("date", 0L);
                String key = buildScamKey(address, body, dateMillis);
                if (isDismissedScam(ctx, key) || isDismissedScamBody(ctx, body)) {
                    continue;
                }
                Report r = new Report(
                        "sms",
                        body,
                        "Potential Scam",
                        new Timestamp(new Date(dateMillis)),
                        "sms"
                );
                r.setSource(address);
                results.add(r);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse detected scam message", e);
            }
        }
        Collections.sort(results, (a, b) -> {
            long ta = a.getTimestamp() != null ? a.getTimestamp().toDate().getTime() : 0L;
            long tb = b.getTimestamp() != null ? b.getTimestamp().toDate().getTime() : 0L;
            return Long.compare(tb, ta);
        });
        return results;
    }

    private static Set<String> trimDetectedScams(Set<String> input, int max) {
        List<JSONObject> parsed = new ArrayList<>();
        for (String entry : input) {
            if (entry == null || entry.trim().isEmpty()) continue;
            try {
                parsed.add(new JSONObject(entry));
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse detected scam for trimming", e);
            }
        }
        parsed.sort((a, b) -> Long.compare(b.optLong("date", 0L), a.optLong("date", 0L)));
        Set<String> trimmed = new HashSet<>();
        int count = 0;
        for (JSONObject obj : parsed) {
            trimmed.add(obj.toString());
            count++;
            if (count >= max) break;
        }
        return trimmed;
    }

    public static String buildScamKey(String address, String body, long dateMillis) {
        String normalized = normalizePhoneNumber(address);
        String safeBody = body == null ? "" : body.trim();
        return normalized + "|" + dateMillis + "|" + safeBody;
    }

    public static boolean isDismissedScam(Context ctx, String key) {
        if (ctx == null || key == null || key.trim().isEmpty()) return false;
        Set<String> dismissed = ctx.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
                .getStringSet(KEY_DISMISSED_SCAMS, new HashSet<>());
        return dismissed.contains(key);
    }

    public static void dismissScamMessage(Context ctx, String key) {
        if (ctx == null || key == null || key.trim().isEmpty()) return;
        Set<String> dismissed = ctx.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
                .getStringSet(KEY_DISMISSED_SCAMS, new HashSet<>());
        Set<String> copy = new HashSet<>(dismissed);
        copy.add(key);
        ctx.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_DISMISSED_SCAMS, copy)
                .apply();
        removeDetectedScamByKey(ctx, key);
    }

    public static void dismissScamMessageByBody(Context ctx, String body) {
        if (ctx == null) return;
        String normalized = normalizeScamBody(body);
        if (normalized.isEmpty()) return;
        Set<String> dismissed = ctx.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
                .getStringSet(KEY_DISMISSED_SCAM_BODIES, new HashSet<>());
        Set<String> copy = new HashSet<>(dismissed);
        copy.add(normalized);
        ctx.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_DISMISSED_SCAM_BODIES, copy)
                .apply();
        removeDetectedScamsByBody(ctx, normalized);
    }

    public static boolean isDismissedScamBody(Context ctx, String body) {
        if (ctx == null) return false;
        String normalized = normalizeScamBody(body);
        if (normalized.isEmpty()) return false;
        Set<String> dismissed = ctx.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
                .getStringSet(KEY_DISMISSED_SCAM_BODIES, new HashSet<>());
        return dismissed.contains(normalized);
    }

    private static String normalizeScamBody(String body) {
        if (body == null) return "";
        return body.trim().toLowerCase(java.util.Locale.US).replaceAll("\\s+", " ");
    }

    private static void removeDetectedScamByKey(Context ctx, String key) {
        Set<String> raw = ctx.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
                .getStringSet(KEY_DETECTED_SCAMS, new HashSet<>());
        if (raw.isEmpty()) return;
        Set<String> updated = new HashSet<>();
        for (String entry : raw) {
            if (entry == null || entry.trim().isEmpty()) continue;
            try {
                JSONObject obj = new JSONObject(entry);
                String address = obj.optString("address", "");
                String body = obj.optString("body", "");
                long dateMillis = obj.optLong("date", 0L);
                String entryKey = buildScamKey(address, body, dateMillis);
                if (!entryKey.equals(key)) {
                    updated.add(entry);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse detected scam entry for removal", e);
            }
        }
        ctx.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_DETECTED_SCAMS, updated)
                .apply();
    }

    private static void removeDetectedScamsByBody(Context ctx, String normalizedBody) {
        Set<String> raw = ctx.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
                .getStringSet(KEY_DETECTED_SCAMS, new HashSet<>());
        if (raw.isEmpty()) return;
        Set<String> updated = new HashSet<>();
        for (String entry : raw) {
            if (entry == null || entry.trim().isEmpty()) continue;
            try {
                JSONObject obj = new JSONObject(entry);
                String body = obj.optString("body", "");
                String entryBody = normalizeScamBody(body);
                if (!entryBody.equals(normalizedBody)) {
                    updated.add(entry);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse detected scam entry for body removal", e);
            }
        }
        ctx.getSharedPreferences(PREFS_ALERTS, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_DETECTED_SCAMS, updated)
                .apply();
    }

    public static void logUserActivity(Context ctx, String action) {
        logUserActivity(ctx, action, null);
    }

    public static void logUserActivity(Context ctx, String action, Map<String, Object> extras) {
        String email = getLoggedInEmail(ctx);
        Map<String, Object> payload = new HashMap<>();
        payload.put("user", email != null ? email : "anonymous");
        payload.put("action", action);
        payload.put("timestamp", FieldValue.serverTimestamp());
        if (extras != null) {
            payload.putAll(extras);
        }
        db.collection("activity_logs")
                .add(payload)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to log activity", e));
    }

    // ───────────── USERS (CUSTOM AUTH) ─────────────

    /** REGISTER */
    public static void register(
            String name,
            String email,
            String password,
            SimpleCallback<Boolean> cb
    ) {
        register(name, null, email, password, cb);
    }

    /** REGISTER with username */
    public static void register(
            String name,
            String username,
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
        Log.d("REGISTER_DEBUG", "Name: " + name);
        Log.d("REGISTER_DEBUG", "Username: " + username);
        Log.d("REGISTER_DEBUG", "Original email: " + email);
        Log.d("REGISTER_DEBUG", "Normalized email: " + normalizedEmail);
        Log.d("REGISTER_DEBUG", "Password length: " + trimmedPassword.length());
        Log.d("REGISTER_DEBUG", "Password hash: " + hash);

        // Check if email already exists
        db.collection("users")
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(emailSnapshot -> {
                    if (!emailSnapshot.isEmpty()) {
                        Log.d("REGISTER_DEBUG", "Email already exists: " + normalizedEmail);
                        cb.onComplete(false);
                        return;
                    }

                    // If username is provided, check if it's already taken
                    if (username != null && !username.trim().isEmpty()) {
                        final String trimmedUsername = username.trim();
                        db.collection("users")
                                .whereEqualTo("username", trimmedUsername)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(usernameSnapshot -> {
                                    if (!usernameSnapshot.isEmpty()) {
                                        Log.d("REGISTER_DEBUG", "Username already exists: " + trimmedUsername);
                                        cb.onComplete(false);
                                        return;
                                    }
                                    // Username is available, proceed with registration
                                    createUser(name, trimmedUsername, normalizedEmail, hash, cb);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "username check failed", e);
                                    e.printStackTrace();
                                    cb.onComplete(false);
                                });
                    } else {
                        // No username provided, proceed with registration
                        createUser(name, null, normalizedEmail, hash, cb);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "email check failed", e);
                    e.printStackTrace();
                    cb.onComplete(false);
                });
    }

    /**
     * Register after phone verification with 4-digit PIN.
     */
    public static void registerAfterPhoneVerification(
            String name,
            String username,
            String password,
            String phoneNumber,
            SimpleCallback<Boolean> cb
    ) {
        registerAfterPhoneVerification(name, username, password, phoneNumber, null, cb);
    }

    public static void registerAfterPhoneVerification(
            String name,
            String username,
            String password,
            String phoneNumber,
            String email,
            SimpleCallback<Boolean> cb
    ) {
        final String normalizedPhone = phoneNumber.trim();
        final String normalizedEmail = buildPhoneEmail(normalizedPhone);
        final String normalizedInputEmail = email != null ? email.trim().toLowerCase() : null;
        final String trimmedUsername = username == null ? "" : username.trim();
        final String hash = PasswordUtil.hashPassword(password.trim());

        if (normalizedInputEmail != null && !normalizedInputEmail.isEmpty()) {
            db.collection("users")
                    .whereEqualTo("email", normalizedInputEmail)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(emailSnapshot -> {
                        if (!emailSnapshot.isEmpty()) {
                            DocumentSnapshot doc = emailSnapshot.getDocuments().get(0);
                            String docId = doc.getId();

                            // Ensure phone number isn't used by another user
                            db.collection("users")
                                    .whereEqualTo("phoneNumber", normalizedPhone)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(phoneSnapshot -> {
                                        if (!phoneSnapshot.isEmpty()
                                                && !phoneSnapshot.getDocuments().get(0).getId().equals(docId)) {
                                            String phoneDocId = phoneSnapshot.getDocuments().get(0).getId();
                                            mergeUserByPhone(
                                                    phoneDocId,
                                                    docId,
                                                    name,
                                                    trimmedUsername,
                                                    normalizedInputEmail,
                                                    normalizedPhone,
                                                    hash,
                                                    cb
                                            );
                                            return;
                                        }

                                        if (!trimmedUsername.isEmpty()) {
                                            db.collection("users")
                                                    .whereEqualTo("username", trimmedUsername)
                                                    .limit(1)
                                                    .get()
                                                    .addOnSuccessListener(usernameSnapshot -> {
                                                        if (!usernameSnapshot.isEmpty()
                                                                && !usernameSnapshot.getDocuments().get(0).getId().equals(docId)) {
                                                            cb.onComplete(false);
                                                            return;
                                                        }
                                                        updateUserWithPhone(docId, name, trimmedUsername, normalizedInputEmail, normalizedPhone, hash, cb);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "username check failed", e);
                                                        cb.onComplete(false);
                                                    });
                                        } else {
                                            updateUserWithPhone(docId, name, null, normalizedInputEmail, normalizedPhone, hash, cb);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "phone check failed", e);
                                        cb.onComplete(false);
                                    });
                            return;
                        }

                        // No email doc found; fall back to phone flow
                        handlePhoneBasedRegistration(name, trimmedUsername, normalizedEmail, normalizedPhone, hash, normalizedInputEmail, cb);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "email check failed", e);
                        cb.onComplete(false);
                    });
            return;
        }

        handlePhoneBasedRegistration(name, trimmedUsername, normalizedEmail, normalizedPhone, hash, null, cb);
    }

    private static void handlePhoneBasedRegistration(
            String name,
            String trimmedUsername,
            String normalizedEmail,
            String normalizedPhone,
            String hash,
            String normalizedInputEmail,
            SimpleCallback<Boolean> cb
    ) {
        // If a user already exists (OAuth or prior), overwrite profile fields.
        db.collection("users")
                .whereEqualTo("phoneNumber", normalizedPhone)
                .limit(1)
                .get()
                .addOnSuccessListener(phoneSnapshot -> {
                    if (!phoneSnapshot.isEmpty()) {
                        DocumentSnapshot doc = phoneSnapshot.getDocuments().get(0);
                        String emailToUse = normalizedInputEmail != null && !normalizedInputEmail.isEmpty()
                                ? normalizedInputEmail
                                : normalizedEmail;
                        updateUserWithPhone(doc.getId(), name, trimmedUsername, emailToUse, normalizedPhone, hash, cb);
                        return;
                    }

                    // If username is provided, ensure it's not taken by another user
                    if (!trimmedUsername.isEmpty()) {
                        db.collection("users")
                                .whereEqualTo("username", trimmedUsername)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(usernameSnapshot -> {
                                    if (!usernameSnapshot.isEmpty()) {
                                        DocumentSnapshot existing = usernameSnapshot.getDocuments().get(0);
                                        String existingEmail = existing.getString("email");
                                        String existingPhone = existing.getString("phoneNumber");
                                        Boolean hasProfile = existing.getBoolean("hasProfile");
                                        String emailToUse = normalizedInputEmail != null && !normalizedInputEmail.isEmpty()
                                                ? normalizedInputEmail
                                                : normalizedEmail;

                                        boolean sameEmail = existingEmail != null
                                                && emailToUse != null
                                                && existingEmail.equalsIgnoreCase(emailToUse);
                                        boolean samePhone = existingPhone != null
                                                && existingPhone.equalsIgnoreCase(normalizedPhone);
                                        boolean reusable = (hasProfile != null && !hasProfile)
                                                && (sameEmail || existingEmail != null && existingEmail.equalsIgnoreCase(normalizedEmail));

                                        if (sameEmail || samePhone || reusable) {
                                            updateUserWithPhone(existing.getId(), name, trimmedUsername, emailToUse, normalizedPhone, hash, cb);
                                            return;
                                        }
                                        cb.onComplete(false);
                                        return;
                                    }
                                    createUserWithPhone(name, trimmedUsername, normalizedEmail, normalizedPhone, hash, cb);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "username check failed", e);
                                    cb.onComplete(false);
                                });
                    } else {
                        createUserWithPhone(name, null, normalizedEmail, normalizedPhone, hash, cb);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "phone check failed", e);
                    cb.onComplete(false);
                });
    }

    private static void mergeUserByPhone(
            String phoneDocId,
            String emailDocIdToDelete,
            String name,
            String username,
            String email,
            String phoneNumber,
            String hash,
            SimpleCallback<Boolean> cb
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        if (username != null && !username.isEmpty()) {
            updates.put("username", username);
        }
        updates.put("email", email);
        if (hash != null && !hash.trim().isEmpty()) {
            updates.put("passwordHash", hash);
        }
        updates.put("phoneNumber", phoneNumber);
        updates.put("isVerified", true);
        updates.put("hasProfile", true);
        updates.put("profileCompletedAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(phoneDocId)
                .update(updates)
                .addOnSuccessListener(v -> {
                    if (emailDocIdToDelete != null && !emailDocIdToDelete.equals(phoneDocId)) {
                        db.collection("users")
                                .document(emailDocIdToDelete)
                                .delete()
                                .addOnSuccessListener(x -> cb.onComplete(true))
                                .addOnFailureListener(e -> cb.onComplete(true));
                    } else {
                        cb.onComplete(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "mergeUserByPhone failed", e);
                    cb.onComplete(false);
                });
    }

    private static void mergeUserDocs(
            String targetDocId,
            String docIdToDelete,
            String name,
            String username,
            String email,
            String phoneNumber,
            String hash,
            SimpleCallback<Boolean> cb
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        if (username != null && !username.isEmpty()) {
            updates.put("username", username);
        }
        updates.put("email", email);
        if (hash != null && !hash.trim().isEmpty()) {
            updates.put("passwordHash", hash);
        }
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            updates.put("phoneNumber", phoneNumber);
        }
        updates.put("isVerified", true);
        updates.put("hasProfile", true);
        updates.put("profileCompletedAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(targetDocId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> {
                    if (docIdToDelete != null && !docIdToDelete.equals(targetDocId)) {
                        db.collection("users")
                                .document(docIdToDelete)
                                .delete()
                                .addOnSuccessListener(x -> cb.onComplete(true))
                                .addOnFailureListener(e -> cb.onComplete(true));
                    } else {
                        cb.onComplete(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "mergeUserDocs failed", e);
                    cb.onComplete(false);
                });
    }

    /**
     * Check if an account exists for the email and return provider type.
     * Returns null if no account exists, "password" if local, or provider (google/facebook/apple).
     */
    public static void checkExistingAccountProvider(
            String email,
            SimpleCallback<String> cb
    ) {
        final String normalizedEmail = email.trim().toLowerCase();
        db.collection("users")
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        cb.onComplete(null);
                        return;
                    }
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    String provider = doc.getString("provider");
                    if (provider == null || provider.trim().isEmpty()) {
                        provider = "password";
                    }
                    cb.onComplete(provider);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing account provider", e);
                    cb.onComplete("password");
                });
    }

    public static void findPasswordAccountByPhone(
            String phoneNumber,
            SimpleCallback<Map<String, String>> cb
    ) {
        final String raw = phoneNumber == null ? "" : phoneNumber.trim();
        if (raw.isEmpty()) {
            cb.onComplete(null);
            return;
        }

        String digits = PhoneFormatUtil.digitsOnly(raw);
        String local10 = digits;
        if (local10.startsWith("63") && local10.length() >= 12) {
            local10 = local10.substring(local10.length() - 10);
        } else if (local10.startsWith("0") && local10.length() == 11) {
            local10 = local10.substring(1);
        } else if (local10.length() > 10) {
            local10 = local10.substring(local10.length() - 10);
        }

        Set<String> variants = new LinkedHashSet<>();
        variants.add(raw);
        if (!digits.isEmpty()) {
            variants.add(digits);
        }
        if (!local10.isEmpty()) {
            variants.add(local10);
            variants.add("0" + local10);
        }
        if (digits.length() > 10 && local10.length() == 10 && digits.endsWith(local10)) {
            String cc = digits.substring(0, digits.length() - local10.length());
            variants.add("+" + cc + local10);
            variants.add(cc + local10);
            String grouped = PhoneFormatUtil.formatLocal(local10);
            if (grouped.contains(" ")) {
                variants.add("+" + cc + " " + grouped);
                variants.add(cc + " " + grouped);
                variants.add(grouped);
            }
        } else if (local10.length() == 10) {
            String grouped = PhoneFormatUtil.formatLocal(local10);
            if (grouped.contains(" ")) {
                variants.add(grouped);
            }
        }

        List<String> variantList = new ArrayList<>();
        for (String v : variants) {
            if (v != null) {
                String t = v.trim();
                if (!t.isEmpty()) {
                    variantList.add(t);
                }
            }
        }
        findPasswordAccountByPhoneVariants(variantList, 0, 0, cb);
    }

    /** Firestore field names that may hold a phone (camelCase + legacy). */
    private static final String[] USER_PHONE_FIELDS = {
            "phoneNumber",
            "phone",
            "mobile",
            "mobileNumber",
    };

    private static void findPasswordAccountByPhoneVariants(
            List<String> variants,
            int variantIndex,
            int fieldIndex,
            SimpleCallback<Map<String, String>> cb
    ) {
        if (variants == null || variantIndex >= variants.size()) {
            cb.onComplete(null);
            return;
        }
        String variant = variants.get(variantIndex);
        if (variant == null || variant.trim().isEmpty()) {
            findPasswordAccountByPhoneVariants(variants, variantIndex + 1, 0, cb);
            return;
        }
        if (fieldIndex >= USER_PHONE_FIELDS.length) {
            findPasswordAccountByPhoneVariants(variants, variantIndex + 1, 0, cb);
            return;
        }

        final String field = USER_PHONE_FIELDS[fieldIndex];
        final String trimmedVariant = variant.trim();

        queryUsersByPhoneValue(field, trimmedVariant, match -> {
                    if (match != null) {
                        Map<String, String> account = new HashMap<>();
                        account.put("userId", match.getId());
                        account.put("email", resolveLoginIdForPasswordReset(match));
                        account.put("phoneNumber", readPhoneFieldFromDoc(match));
                String provider = match.getString("provider");
                if (provider == null || provider.trim().isEmpty()) {
                    provider = "password";
                }
                account.put("provider", provider);
                cb.onComplete(account);
                return;
            }
            findPasswordAccountByPhoneVariants(variants, variantIndex, fieldIndex + 1, cb);
        }, e -> {
            Log.e(TAG, "findPasswordAccountByPhone failed field=" + field + " variant=" + trimmedVariant, e);
            findPasswordAccountByPhoneVariants(variants, variantIndex, fieldIndex + 1, cb);
        });
    }

    /**
     * Match string or (legacy) numeric phone in Firestore — type must match the stored field.
     */
    private static void queryUsersByPhoneValue(
            String field,
            String trimmedVariant,
            java.util.function.Consumer<DocumentSnapshot> onResult,
            java.util.function.Consumer<Exception> onFailure
    ) {
        db.collection("users")
                .whereEqualTo(field, trimmedVariant)
                .limit(25)
                .get()
                .addOnSuccessListener(snapshot -> {
                    DocumentSnapshot match = pickDocForPhonePasswordRecovery(snapshot);
                    if (match != null) {
                        onResult.accept(match);
                        return;
                    }
                    if (trimmedVariant.matches("\\d+")
                            && trimmedVariant.length() >= 10
                            && trimmedVariant.length() <= 15) {
                        try {
                            long asLong = Long.parseLong(trimmedVariant);
                            db.collection("users")
                                    .whereEqualTo(field, asLong)
                                    .limit(25)
                                    .get()
                                    .addOnSuccessListener(s2 -> {
                                        DocumentSnapshot m2 = pickDocForPhonePasswordRecovery(s2);
                                        onResult.accept(m2);
                                    })
                                    .addOnFailureListener(e -> onFailure.accept(e));
                            return;
                        } catch (NumberFormatException ignored) {
                            // fall through
                        }
                    }
                    onResult.accept(null);
                })
                .addOnFailureListener(onFailure::accept);
    }

    /**
     * Phone-based reset is allowed when the user has an email on file (needed for {@link #resetPassword})
     * and is not a pure social login without an app password path. Many accounts omit {@code provider}
     * or use phone/OAuth while still having (or gaining) a {@code passwordHash}.
     */
    private static String resolveLoginIdForPasswordReset(DocumentSnapshot doc) {
        String email = doc.getString("email");
        if (email != null && !email.trim().isEmpty()) {
            return email.trim();
        }
        String username = doc.getString("username");
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return null;
    }

    private static DocumentSnapshot pickDocForPhonePasswordRecovery(QuerySnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return null;
        }
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String loginId = resolveLoginIdForPasswordReset(doc);
            if (loginId == null || loginId.isEmpty()) {
                continue;
            }
            String provider = doc.getString("provider");
            if (provider != null) {
                String p = provider.trim().toLowerCase(java.util.Locale.US);
                if (p.contains("google")
                        || p.contains("facebook")
                        || p.contains("apple")) {
                    String hash = doc.getString("passwordHash");
                    if (hash == null || hash.trim().isEmpty()) {
                        continue;
                    }
                }
            }
            return doc;
        }
        return null;
    }

    /** Prefer {@code phoneNumber}, then other phone fields; supports String or numeric storage. */
    private static String readPhoneFieldFromDoc(DocumentSnapshot doc) {
        for (String key : USER_PHONE_FIELDS) {
            Object v = doc.get(key);
            if (v == null) {
                continue;
            }
            if (v instanceof String) {
                String s = ((String) v).trim();
                if (!s.isEmpty()) {
                    return s;
                }
            } else {
                return String.valueOf(v);
            }
        }
        return null;
    }

    /**
     * Reset password for a local (password) account.
     */
    public static void resetPassword(
            String email,
            String newPassword,
            SimpleCallback<Boolean> cb
    ) {
        final String normalizedEmail = email.trim().toLowerCase();
        final String hash = PasswordUtil.hashPassword(newPassword.trim());
        db.collection("users")
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        cb.onComplete(false);
                        return;
                    }
                    String docId = snapshot.getDocuments().get(0).getId();
                    db.collection("users")
                            .document(docId)
                            .update("passwordHash", hash, "passwordUpdatedAt", FieldValue.serverTimestamp())
                            .addOnSuccessListener(x -> cb.onComplete(true))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "resetPassword update failed", e);
                                cb.onComplete(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "resetPassword lookup failed", e);
                    cb.onComplete(false);
                });
    }

    public static void resetPasswordByUserId(
            String userId,
            String newPassword,
            SimpleCallback<Boolean> cb
    ) {
        final String normalizedId = userId == null ? "" : userId.trim();
        final String normalizedPass = newPassword == null ? "" : newPassword.trim();
        if (normalizedId.isEmpty() || normalizedPass.isEmpty()) {
            cb.onComplete(false);
            return;
        }
        final String hash = PasswordUtil.hashPassword(normalizedPass);
        db.collection("users")
                .document(normalizedId)
                .update("passwordHash", hash, "passwordUpdatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(x -> cb.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "resetPasswordByUserId failed", e);
                    cb.onComplete(false);
                });
    }

    /** Helper method to create user document */
    private static void createUser(
            String name,
            String username,
            String normalizedEmail,
            String hash,
            SimpleCallback<Boolean> cb
    ) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        if (username != null && !username.isEmpty()) {
            user.put("username", username);
        }
        user.put("email", normalizedEmail);
        user.put("passwordHash", hash);
        user.put("createdAt", FieldValue.serverTimestamp());
        user.put("isVerified", false);
        user.put("hasProfile", false);

        Log.d("REGISTER_DEBUG", "Creating user with email: " + normalizedEmail + ", username: " + username);

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
    }

    private static void createUserWithPhone(
            String name,
            String username,
            String normalizedEmail,
            String phoneNumber,
            String hash,
            SimpleCallback<Boolean> cb
    ) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        if (username != null && !username.isEmpty()) {
            user.put("username", username);
        }
        user.put("email", normalizedEmail);
        user.put("passwordHash", hash);
        user.put("phoneNumber", phoneNumber);
        user.put("createdAt", FieldValue.serverTimestamp());
        user.put("isVerified", true);
        user.put("hasProfile", true);
        user.put("profileCompletedAt", FieldValue.serverTimestamp());

        db.collection("users")
                .add(user)
                .addOnSuccessListener(d -> cb.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "register failed", e);
                    cb.onComplete(false);
                });
    }

    private static void updateUserWithPhone(
            String docId,
            String name,
            String username,
            String normalizedEmail,
            String phoneNumber,
            String hash,
            SimpleCallback<Boolean> cb
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        if (username != null && !username.isEmpty()) {
            updates.put("username", username);
        }
        updates.put("email", normalizedEmail);
        updates.put("passwordHash", hash);
        updates.put("phoneNumber", phoneNumber);
        updates.put("isVerified", true);
        updates.put("hasProfile", true);
        updates.put("profileCompletedAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(docId)
                .update(updates)
                .addOnSuccessListener(v -> cb.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "update failed", e);
                    cb.onComplete(false);
                });
    }

    private static String buildPhoneEmail(String phoneNumber) {
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        return digits + "@fraudulens.local";
    }

    /** Update user phone number after OTP verification */
    public static void updateUserPhoneNumber(String email, String phoneNumber, SimpleCallback<Boolean> cb) {
        final String normalizedEmail = email.trim().toLowerCase();
        
        db.collection("users")
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Log.e(TAG, "User not found for phone update: " + normalizedEmail);
                        cb.onComplete(false);
                        return;
                    }

                    String docId = snapshot.getDocuments().get(0).getId();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("phoneNumber", phoneNumber);
                    updates.put("isVerified", true);

                    db.collection("users")
                            .document(docId)
                            .update(updates)
                            .addOnSuccessListener(v -> {
                                Log.d(TAG, "Phone number updated for: " + normalizedEmail);
                                cb.onComplete(true);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update phone number", e);
                                cb.onComplete(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding user for phone update", e);
                    cb.onComplete(false);
                });
    }

    /** Update user photo URL */
    public static void updateUserPhotoUrl(String authUid, String photoUrl, SimpleCallback<Boolean> cb) {
        if (authUid == null || authUid.trim().isEmpty()) {
            cb.onComplete(false);
            return;
        }
        String uid = authUid.trim();
        Map<String, Object> updates = new HashMap<>();
        updates.put("photoUrl", photoUrl);
        db.collection("users")
                .document(uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> {
                    updatePostsPhoto(uid, photoUrl);
                    cb.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update photo url", e);
                    cb.onComplete(false);
                });
    }

    private static void updatePostsPhoto(String userId, String photoUrl) {
        if (userId == null || userId.trim().isEmpty()) return;
        db.collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        db.collection("posts")
                                .document(doc.getId())
                                .update("userPhotoUrl", photoUrl);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update post photos", e));
    }

    /** LOGIN - Supports both email and username */
    public static void login(
            Context ctx,
            String emailOrUsername,
            String password,
            SimpleCallback<Boolean> cb
    ) {
        login(ctx, emailOrUsername, password, true, cb);
    }

    public static void login(
            Context ctx,
            String emailOrUsername,
            String password,
            boolean setSession,
            SimpleCallback<Boolean> cb
    ) {
        final String input = emailOrUsername.trim();
        final String normalizedEmail = input.toLowerCase();
        final String originalEmail = input; // For legacy accounts
        // Trim password to ensure consistency with registration
        final String trimmedPassword = password.trim();
        final String hash = PasswordUtil.hashPassword(trimmedPassword);
        final String rawPassword = trimmedPassword;

        Log.d("LOGIN_DEBUG", "=== LOGIN ATTEMPT ===");
        Log.d("LOGIN_DEBUG", "Input (email/username): " + input);
        Log.d("LOGIN_DEBUG", "Normalized email: " + normalizedEmail);
        Log.d("LOGIN_DEBUG", "Password length: " + trimmedPassword.length());
        Log.d("LOGIN_DEBUG", "Password hash: " + hash);

        // Check if input looks like an email (contains @)
        boolean isEmail = input.contains("@");
        
        if (isEmail) {
            // Try normalized email first (for accounts created after fix)
            attemptLoginWithEmail(ctx, normalizedEmail, originalEmail, hash, rawPassword, cb, true, setSession);
        } else {
            // Try username login
            attemptLoginWithUsername(ctx, input, hash, rawPassword, cb, setSession);
        }
    }

    /**
     * Verifies password against {@code users/{userDocId}} and sets the local session — same outcome as a
     * successful email/username hash login, but without querying by email. Use after phone password reset
     * so we always re-attach to the same Firestore document; otherwise a failed email match can fall through
     * to Firebase Auth sign-in and {@link #migrateUserDocToAuthUid}, which may delete the canonical profile
     * doc or leave the session on a sparse {@code users/{authUid}} document.
     */
    public static void loginWithUserDocumentId(
            Context ctx,
            String userDocId,
            String password,
            boolean setSession,
            SimpleCallback<Boolean> cb
    ) {
        if (userDocId == null || userDocId.trim().isEmpty()) {
            cb.onComplete(false);
            return;
        }
        final String id = userDocId.trim();
        final String trimmedPassword = password == null ? "" : password.trim();
        final String hash = PasswordUtil.hashPassword(trimmedPassword);

        db.collection("users")
                .document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        cb.onComplete(false);
                        return;
                    }
                    String storedHash = doc.getString("passwordHash");
                    if (storedHash == null) {
                        cb.onComplete(false);
                        return;
                    }
                    boolean match = hash.equals(storedHash);
                    if (!match) {
                        String doubleHash = PasswordUtil.hashPassword(hash);
                        match = doubleHash.equals(storedHash);
                    }
                    if (!match) {
                        cb.onComplete(false);
                        return;
                    }
                    if (setSession) {
                        String storedEmail = doc.getString("email");
                        String username = doc.getString("username");
                        if (storedEmail != null && !storedEmail.trim().isEmpty()) {
                            setLoggedIn(ctx, storedEmail.trim().toLowerCase());
                            ensureEmailStoredForDoc(id, storedEmail.trim().toLowerCase());
                        } else if (username != null && !username.trim().isEmpty()) {
                            setLoggedIn(ctx, username.trim());
                        } else {
                            setLoggedIn(ctx, id);
                        }
                    }
                    cb.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loginWithUserDocumentId failed", e);
                    cb.onComplete(false);
                });
    }

    private static void attemptLoginWithEmail(
            Context ctx,
            String searchEmail,
            String fallbackEmail,
            String hash,
            String rawPassword,
            SimpleCallback<Boolean> cb,
            boolean tryFallback,
            boolean setSession
    ) {
        db.collection("users")
                .whereEqualTo("email", searchEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap.isEmpty()) {
                        Log.d("LOGIN_DEBUG", "No user found with email: " + searchEmail);
                        attemptAuthEmailLogin(ctx, searchEmail, rawPassword, authOk -> {
                            if (authOk) {
                                cb.onComplete(true);
                                return;
                            }
                            // If email isn't stored, try matching username with full email or local-part.
                            String localPart = searchEmail.contains("@") ? searchEmail.split("@")[0] : searchEmail;
                            attemptLoginWithUsernameOnly(ctx, searchEmail, hash, success -> {
                                if (success) {
                                    cb.onComplete(true);
                                    return;
                                }
                                if (!localPart.equals(searchEmail)) {
                                    attemptLoginWithUsernameOnly(ctx, localPart, hash, successLocal -> {
                                        if (successLocal) {
                                            cb.onComplete(true);
                                            return;
                                        }
                                        // Try fallback email if different from search email (for legacy accounts)
                                        if (tryFallback && !searchEmail.equals(fallbackEmail)) {
                                            Log.d("LOGIN_DEBUG", "Trying fallback with original email format: " + fallbackEmail);
                                            attemptLoginWithEmail(ctx, fallbackEmail, fallbackEmail, hash, rawPassword, cb, false, setSession);
                                        } else {
                                            cb.onComplete(false);
                                        }
                                    }, setSession);
                                    return;
                                }
                                // Try fallback email if different from search email (for legacy accounts)
                                if (tryFallback && !searchEmail.equals(fallbackEmail)) {
                                    Log.d("LOGIN_DEBUG", "Trying fallback with original email format: " + fallbackEmail);
                                    attemptLoginWithEmail(ctx, fallbackEmail, fallbackEmail, hash, rawPassword, cb, false, setSession);
                                } else {
                                    cb.onComplete(false);
                                }
                            }, setSession);
                        });
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
                            if (setSession) {
                                setLoggedIn(ctx, storedEmail.toLowerCase()); // Normalize for future use
                            }
                            cb.onComplete(true);
                            return;
                        }
                    }

                    if (match) {
                        Log.d("LOGIN_DEBUG", "Login successful!");
                        if (setSession) {
                            setLoggedIn(ctx, storedEmail.toLowerCase()); // Normalize for consistency
                        }
                        ensureEmailStoredForDoc(doc.getId(), searchEmail);
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

    /** LOGIN with username */
    private static void attemptLoginWithUsername(
            Context ctx,
            String username,
            String hash,
            String rawPassword,
            SimpleCallback<Boolean> cb,
            boolean setSession
    ) {
        Log.d("LOGIN_DEBUG", "Attempting login with username: " + username);
        
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Log.d("LOGIN_DEBUG", "No user found with username: " + username);
                        // If username not found, try as email (in case user entered email without @)
                        Log.d("LOGIN_DEBUG", "Trying as email: " + username.toLowerCase());
                        attemptLoginWithEmail(ctx, username.toLowerCase(), username, hash, rawPassword, cb, true, setSession);
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    String storedHash = doc.getString("passwordHash");
                    String storedEmail = doc.getString("email");
                    String storedUsername = doc.getString("username");

                    Log.d("LOGIN_DEBUG", "User found with username!");
                    Log.d("LOGIN_DEBUG", "Stored username: " + storedUsername);
                    Log.d("LOGIN_DEBUG", "Stored email: " + storedEmail);
                    Log.d("LOGIN_DEBUG", "Stored hash: " + storedHash);

                    if (storedHash == null) {
                        Log.e("LOGIN_DEBUG", "ERROR: passwordHash field is null in database!");
                        cb.onComplete(false);
                        return;
                    }

                    boolean match = hash.equals(storedHash);
                    Log.d("LOGIN_DEBUG", "Hash match: " + match);
                    
                    // If hash doesn't match, try legacy double-hash
                    if (!match) {
                        Log.d("LOGIN_DEBUG", "Trying legacy double-hash check...");
                        String doubleHash = PasswordUtil.hashPassword(hash);
                        boolean legacyMatch = doubleHash.equals(storedHash);
                        Log.d("LOGIN_DEBUG", "Legacy hash match: " + legacyMatch);
                        if (legacyMatch) {
                            Log.d("LOGIN_DEBUG", "Login successful with legacy account!");
                            if (setSession) {
                                setLoggedIn(ctx, storedEmail != null ? storedEmail.toLowerCase() : username);
                            }
                            cb.onComplete(true);
                            return;
                        }
                    }

                    if (match) {
                        Log.d("LOGIN_DEBUG", "Login successful with username!");
                        if (setSession) {
                            setLoggedIn(ctx, storedEmail != null ? storedEmail.toLowerCase() : username);
                        }
                        if (storedEmail == null || storedEmail.trim().isEmpty()) {
                            FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
                            String authEmail = authUser != null ? authUser.getEmail() : null;
                            if (authEmail != null && !authEmail.trim().isEmpty()) {
                                ensureEmailStoredForDoc(doc.getId(), authEmail);
                            }
                        }
                    } else {
                        Log.d("LOGIN_DEBUG", "Login failed - hash mismatch");
                    }

                    cb.onComplete(match);
                })
                .addOnFailureListener(e -> {
                    Log.e("LOGIN_DEBUG", "Username login query failed", e);
                    e.printStackTrace();
                    // Fallback: try as email
                    attemptLoginWithEmail(ctx, username.toLowerCase(), username, hash, rawPassword, cb, true, setSession);
                });
    }

    private static void attemptLoginWithUsernameOnly(
            Context ctx,
            String username,
            String hash,
            SimpleCallback<Boolean> cb,
            boolean setSession
    ) {
        if (username == null || username.trim().isEmpty()) {
            cb.onComplete(false);
            return;
        }
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        cb.onComplete(false);
                        return;
                    }
                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    String storedHash = doc.getString("passwordHash");
                    String storedEmail = doc.getString("email");
                    if (storedHash == null) {
                        cb.onComplete(false);
                        return;
                    }
                    boolean match = hash.equals(storedHash);
                    if (!match) {
                        String doubleHash = PasswordUtil.hashPassword(hash);
                        match = doubleHash.equals(storedHash);
                    }
                    if (match && setSession) {
                        setLoggedIn(ctx, storedEmail != null ? storedEmail.toLowerCase() : username);
                    }
                    cb.onComplete(match);
                })
                .addOnFailureListener(e -> cb.onComplete(false));
    }

    private static void attemptAuthEmailLogin(
            Context ctx,
            String email,
            String rawPassword,
            SimpleCallback<Boolean> cb
    ) {
        if (email == null || email.trim().isEmpty() || rawPassword == null || rawPassword.trim().isEmpty()) {
            cb.onComplete(false);
            return;
        }
        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email.trim().toLowerCase(), rawPassword.trim())
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null && user.getEmail() != null) {
                        setLoggedIn(ctx, user.getEmail());
                        ensureEmailStoredForAuthUser(user.getUid(), user.getEmail());
                        migrateUserDocToAuthUid(user.getUid(), user.getEmail());
                    }
                    cb.onComplete(user != null);
                })
                .addOnFailureListener(e -> cb.onComplete(false));
    }

    private static void migrateUserDocToAuthUid(String authUid, String email) {
        if (authUid == null || authUid.trim().isEmpty()) return;
        String normalizedEmail = email != null ? email.trim().toLowerCase() : null;
        if (normalizedEmail == null || normalizedEmail.isEmpty()) return;
        db.collection("users")
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) return;
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    String docId = doc.getId();
                    if (authUid.equals(docId)) return;
                    Map<String, Object> data = doc.getData() != null ? new HashMap<>(doc.getData()) : new HashMap<>();
                    data.put("email", normalizedEmail);
                    data.put("authUid", authUid);
                    db.collection("users")
                            .document(authUid)
                            .set(data, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(v -> db.collection("users").document(docId).delete())
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to migrate user doc", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to find email doc for migration", e));
    }

    private static void ensureEmailStoredForDoc(String docId, String email) {
        if (docId == null || docId.trim().isEmpty()) return;
        String normalizedEmail = email != null ? email.trim().toLowerCase() : null;
        if (normalizedEmail == null || normalizedEmail.isEmpty()) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("email", normalizedEmail);
        db.collection("users")
                .document(docId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to store email on user doc", e));
    }

    private static void ensureEmailStoredForAuthUser(String authUid, String email) {
        if (authUid == null || authUid.trim().isEmpty()) return;
        String normalizedEmail = email != null ? email.trim().toLowerCase() : null;
        if (normalizedEmail == null || normalizedEmail.isEmpty()) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("email", normalizedEmail);
        updates.put("authUid", authUid.trim());
        db.collection("users")
                .document(authUid.trim())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to store auth email on user doc", e));
    }

    /**
     * Verify 4-digit PIN after username/password login.
     */
    public static void verifyPinForLogin(
            Context ctx,
            String emailOrUsername,
            String pin,
            SimpleCallback<Boolean> cb
    ) {
        final String pinHash = PasswordUtil.hashPassword(pin.trim());
        findUserByLoginId(emailOrUsername, doc -> {
            if (doc == null) {
                cb.onComplete(false);
                return;
            }
            handlePinCheck(ctx, doc, pinHash, emailOrUsername, cb);
        });
    }

    public static void hasPinForLogin(
            String emailOrUsername,
            SimpleCallback<Boolean> cb
    ) {
        findUserByLoginId(emailOrUsername, doc -> {
            if (doc == null) {
                cb.onComplete(false);
                return;
            }
            String storedPinHash = doc.getString("pinHash");
            cb.onComplete(storedPinHash != null && !storedPinHash.trim().isEmpty());
        });
    }

    public static void setPinForLogin(
            Context ctx,
            String emailOrUsername,
            String pin,
            SimpleCallback<Boolean> cb
    ) {
        setPinForLogin(ctx, emailOrUsername, pin, true, cb);
    }

    public static void setPinForLogin(
            Context ctx,
            String emailOrUsername,
            String pin,
            boolean setSession,
            SimpleCallback<Boolean> cb
    ) {
        String pinHash = PasswordUtil.hashPassword(pin.trim());
        findUserByLoginId(emailOrUsername, doc -> {
            if (doc == null) {
                cb.onComplete(false);
                return;
            }
            String docId = doc.getId();
            String storedEmail = doc.getString("email");
            db.collection("users")
                    .document(docId)
                    .update("pinHash", pinHash)
                    .addOnSuccessListener(v -> {
                        if (setSession && storedEmail != null) {
                            setLoggedIn(ctx, storedEmail.toLowerCase());
                        }
                        cb.onComplete(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to set PIN", e);
                        cb.onComplete(false);
                    });
        });
    }

    private static void handlePinCheck(
            Context ctx,
            DocumentSnapshot doc,
            String pinHash,
            String loginId,
            SimpleCallback<Boolean> cb
    ) {
        String storedPinHash = doc.getString("pinHash");
        String storedEmail = doc.getString("email");
        if (storedPinHash == null) {
            cb.onComplete(false);
            return;
        }
        boolean match = pinHash.equals(storedPinHash);
        if (match) {
            FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
            String authEmail = authUser != null ? authUser.getEmail() : null;
            String loginKey = authEmail != null && !authEmail.trim().isEmpty()
                    ? authEmail.trim().toLowerCase()
                    : (storedEmail != null && !storedEmail.trim().isEmpty()
                    ? storedEmail.trim().toLowerCase()
                    : (loginId != null ? loginId.trim() : ""));
            setLoggedIn(ctx, loginKey);
        }
        cb.onComplete(match);
    }

    private static void findUserByEmail(String email, SimpleCallback<DocumentSnapshot> cb) {
        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        cb.onComplete(null);
                    } else {
                        cb.onComplete(snap.getDocuments().get(0));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "email lookup failed", e);
                    cb.onComplete(null);
                });
    }

    private static void findUserByUsername(String username, SimpleCallback<DocumentSnapshot> cb) {
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        cb.onComplete(null);
                    } else {
                        cb.onComplete(snap.getDocuments().get(0));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "username lookup failed", e);
                    cb.onComplete(null);
                });
    }

    private static void findUserByLoginId(String emailOrUsername, SimpleCallback<DocumentSnapshot> cb) {
        final String input = emailOrUsername.trim();
        final String normalizedEmail = input.toLowerCase();
        boolean isEmail = input.contains("@");

        if (isEmail) {
            findUserByEmail(normalizedEmail, cb);
            return;
        }

        findUserByUsername(input, doc -> {
            if (doc != null) {
                cb.onComplete(doc);
            } else {
                findUserByEmail(normalizedEmail, cb);
            }
        });
    }

    /**
     * Public helper to fetch a user document using email or username.
     */
    public static void getUserByLoginId(String emailOrUsername, SimpleCallback<DocumentSnapshot> cb) {
        if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
            cb.onComplete(null);
            return;
        }
        findUserByLoginId(emailOrUsername, cb);
    }

    public static void updateUserAuthUidByPhone(String phoneNumber, String authUid, SimpleCallback<Boolean> cb) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty() || authUid == null || authUid.trim().isEmpty()) {
            cb.onComplete(false);
            return;
        }
        String normalized = phoneNumber.trim();
        db.collection("users")
                .whereEqualTo("phoneNumber", normalized)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        cb.onComplete(false);
                        return;
                    }
                    String docId = snap.getDocuments().get(0).getId();
                    db.collection("users")
                            .document(docId)
                            .update("authUid", authUid)
                            .addOnSuccessListener(v -> cb.onComplete(true))
                            .addOnFailureListener(e -> cb.onComplete(false));
                })
                .addOnFailureListener(e -> cb.onComplete(false));
    }

    public static void isUsernameAvailable(String username, SimpleCallback<Boolean> cb) {
        if (username == null || username.trim().isEmpty()) {
            cb.onComplete(false);
            return;
        }
        String trimmed = username.trim();
        db.collection("users")
                .whereEqualTo("username", trimmed)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> cb.onComplete(snapshot.isEmpty()))
                .addOnFailureListener(e -> cb.onComplete(false));
    }

    public static void saveProfileByAuthUid(
            String authUid,
            String name,
            String username,
            String password,
            SimpleCallback<Boolean> cb
    ) {
        saveProfileByAuthUid(authUid, name, username, password, null, null, cb);
    }

    public static void saveProfileByAuthUid(
            String authUid,
            String name,
            String username,
            String password,
            String email,
            SimpleCallback<Boolean> cb
    ) {
        saveProfileByAuthUid(authUid, name, username, password, email, null, cb);
    }

    public static void saveProfileByAuthUid(
            String authUid,
            String name,
            String username,
            String password,
            String email,
            String phoneNumber,
            SimpleCallback<Boolean> cb
    ) {
        if (authUid == null || authUid.trim().isEmpty()) {
            cb.onComplete(false);
            return;
        }
        String trimmedUsername = username != null ? username.trim() : "";
        String trimmedPassword = password != null ? password.trim() : "";
        if (!trimmedUsername.isEmpty()) {
            db.collection("users")
                    .whereEqualTo("username", trimmedUsername)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()
                                && !snapshot.getDocuments().get(0).getId().equals(authUid.trim())) {
                            cb.onComplete(false);
                            return;
                        }
                        writeProfileByUid(authUid, name, trimmedUsername, trimmedPassword, email, phoneNumber, cb);
                    })
                    .addOnFailureListener(e -> cb.onComplete(false));
            return;
        }
        writeProfileByUid(authUid, name, trimmedUsername, trimmedPassword, email, phoneNumber, cb);
    }

    private static void writeProfileByUid(
            String authUid,
            String name,
            String username,
            String password,
            String email,
            String phoneNumber,
            SimpleCallback<Boolean> cb
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        if (username != null && !username.trim().isEmpty()) {
            updates.put("username", username.trim());
        }
        if (password != null && !password.trim().isEmpty()) {
            updates.put("passwordHash", PasswordUtil.hashPassword(password.trim()));
        }
        String normalizedEmail = email != null && !email.trim().isEmpty()
                ? email.trim().toLowerCase()
                : null;
        if (normalizedEmail == null) {
            FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
            if (authUser != null && authUser.getEmail() != null && !authUser.getEmail().trim().isEmpty()) {
                normalizedEmail = authUser.getEmail().trim().toLowerCase();
            }
        }
        if (normalizedEmail != null) {
            updates.put("email", normalizedEmail);
        }
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        String authPhone = authUser != null ? authUser.getPhoneNumber() : null;
        String resolvedPhone = phoneNumber != null && !phoneNumber.trim().isEmpty()
                ? phoneNumber.trim()
                : (authPhone != null ? authPhone.trim() : null);
        if (resolvedPhone != null && !resolvedPhone.isEmpty()) {
            updates.put("phoneNumber", resolvedPhone);
        }
        updates.put("hasProfile", true);
        updates.put("profileCompletedAt", FieldValue.serverTimestamp());
        updates.put("isVerified", true);
        updates.put("authUid", authUid.trim());

        db.collection("users")
                .document(authUid.trim())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> cb.onComplete(true))
                .addOnFailureListener(e -> cb.onComplete(false));
    }

    public static void saveAuthPhoneNumber(String authUid, String phoneNumber) {
        if (authUid == null || authUid.trim().isEmpty()) return;
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("phoneNumber", phoneNumber.trim());
        updates.put("authUid", authUid.trim());
        updates.put("isVerified", true);
        db.collection("users")
                .document(authUid.trim())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to store OTP phone number", e));
    }

    public static void completeOAuthProfile(
            String name,
            String username,
            String phoneNumber,
            String email,
            String authUid,
            SimpleCallback<Boolean> cb
    ) {
        if (email == null || email.trim().isEmpty()) {
            cb.onComplete(false);
            return;
        }
        final String normalizedEmail = email.trim().toLowerCase();
        final String normalizedPhone = phoneNumber != null ? phoneNumber.trim() : "";
        final String trimmedUsername = username != null ? username.trim() : "";
        final String normalizedUid = authUid != null ? authUid.trim() : "";

        if (normalizedUid.isEmpty()) {
            cb.onComplete(false);
            return;
        }
        String docId = normalizedUid;

        db.collection("users")
                .whereEqualTo("phoneNumber", normalizedPhone)
                .limit(1)
                .get()
                .addOnSuccessListener(phoneSnapshot -> {
                    if (!phoneSnapshot.isEmpty()
                            && !phoneSnapshot.getDocuments().get(0).getId().equals(docId)) {
                        String phoneDocId = phoneSnapshot.getDocuments().get(0).getId();
                        mergeUserDocs(
                                docId,
                                phoneDocId,
                                name,
                                trimmedUsername,
                                normalizedEmail,
                                normalizedPhone,
                                null,
                                cb
                        );
                        return;
                    }

                    if (!trimmedUsername.isEmpty()) {
                        db.collection("users")
                                .whereEqualTo("username", trimmedUsername)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(usernameSnapshot -> {
                                    if (!usernameSnapshot.isEmpty()
                                            && !usernameSnapshot.getDocuments().get(0).getId().equals(docId)) {
                                        cb.onComplete(false);
                                        return;
                                    }
                                    updateOAuthProfile(docId, name, trimmedUsername, normalizedPhone, cb);
                                })
                                .addOnFailureListener(e -> cb.onComplete(false));
                    } else {
                        updateOAuthProfile(docId, name, null, normalizedPhone, cb);
                    }
                })
                .addOnFailureListener(e -> cb.onComplete(false));
    }

    private static void updateOAuthProfile(
            String docId,
            String name,
            String username,
            String phoneNumber,
            SimpleCallback<Boolean> cb
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        if (username != null && !username.isEmpty()) {
            updates.put("username", username);
        }
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            updates.put("phoneNumber", phoneNumber);
        }
        updates.put("isVerified", true);
        updates.put("hasProfile", true);
        updates.put("profileCompletedAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(docId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> cb.onComplete(true))
                .addOnFailureListener(e -> cb.onComplete(false));
    }

    public static void checkUserSuspension(String emailOrUsername, SimpleCallback<Map<String, Object>> cb) {
        findUserByLoginId(emailOrUsername, doc -> {
            Map<String, Object> result = new HashMap<>();
            if (doc == null || !doc.exists()) {
                result.put("suspended", false);
                cb.onComplete(result);
                return;
            }
            Boolean suspended = doc.getBoolean("suspended");
            result.put("suspended", suspended != null && suspended);
            result.put("suspendedAt", doc.get("suspendedAt"));
            result.put("suspendedUntil", doc.get("suspendedUntil"));
            result.put("suspendedReason", doc.getString("suspendedReason"));
            result.put("email", doc.getString("email"));
            cb.onComplete(result);
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

    public static void addTrainingSample(Context ctx, String text, boolean isScam, String source) {
        if (ctx == null || text == null || text.trim().isEmpty()) return;
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", text.trim());
        payload.put("label", isScam ? 1 : 0);
        payload.put("result", isScam ? "Potential Scam" : "Looks Safe");
        payload.put("status", "training");
        payload.put("source", source != null ? source : "training");
        payload.put("timestamp", FieldValue.serverTimestamp());
        payload.put("userId", getLoggedInEmail(ctx) != null ? getLoggedInEmail(ctx) : "anonymous");
        payload.put("appVersion", BuildConfig.VERSION_NAME);

        db.collection("reports")
                .add(payload)
                .addOnSuccessListener(d -> com.example.fraudulens.utils.ScamModelManager.forceRefreshModel(ctx))
                .addOnFailureListener(e -> Log.e(TAG, "addTrainingSample failed", e));
    }

    public static void addReport(Map<String, Object> data, SimpleCallback<Boolean> cb) {
        db.collection("reports")
                .add(data)
                .addOnSuccessListener(d -> cb.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "addReport failed", e);
                    cb.onComplete(false);
                });
    }

    /**
     * One-time client-side migration:
     * Normalize older report.userId values (uid/email variants) to a single current identity key
     * so legacy reports appear consistently in "My Reports".
     */
    public static void migrateLegacyReportOwnerIds(Context ctx, SimpleCallback<Boolean> cb) {
        if (ctx == null) {
            if (cb != null) cb.onComplete(false);
            return;
        }

        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = authUser != null ? authUser.getUid() : null;
        String authEmail = authUser != null && authUser.getEmail() != null
                ? authUser.getEmail().trim().toLowerCase()
                : null;
        String sessionEmail = getLoggedInEmail(ctx);
        if (sessionEmail != null) {
            sessionEmail = sessionEmail.trim().toLowerCase();
        }

        String targetUserId = sessionEmail != null && !sessionEmail.isEmpty()
                ? sessionEmail
                : (authEmail != null && !authEmail.isEmpty() ? authEmail : uid);

        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            if (cb != null) cb.onComplete(false);
            return;
        }
        targetUserId = targetUserId.trim();
        final String finalTargetUserId = targetUserId;

        Set<String> candidates = new HashSet<>();
        candidates.add(targetUserId);
        if (uid != null && !uid.trim().isEmpty()) candidates.add(uid.trim());
        if (authEmail != null && !authEmail.trim().isEmpty()) {
            candidates.add(authEmail.trim());
            candidates.add(authEmail.trim().toLowerCase());
        }
        if (sessionEmail != null && !sessionEmail.trim().isEmpty()) {
            candidates.add(sessionEmail.trim());
            candidates.add(sessionEmail.trim().toLowerCase());
        }
        if (sessionEmail != null && sessionEmail.contains("@")) {
            String local = sessionEmail.substring(0, sessionEmail.indexOf('@')).trim();
            if (!local.isEmpty()) candidates.add(local);
        }

        List<String> sourceIds = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate == null) continue;
            String c = candidate.trim();
            if (c.isEmpty()) continue;
            if (!c.equals(targetUserId)) {
                sourceIds.add(c);
            }
        }

        if (sourceIds.isEmpty()) {
            if (cb != null) cb.onComplete(true);
            return;
        }

        resolveCurrentUserDocId(ctx, userDocId -> {
            if (userDocId == null || userDocId.trim().isEmpty()) {
                // No stable user doc to store migration state; still run migration.
                migrateReportOwnerIdsInternal(sourceIds, 0, finalTargetUserId, null, cb);
                return;
            }
            db.collection("users")
                    .document(userDocId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        List<String> migratedKeys = userDoc != null
                                ? (List<String>) userDoc.get("reportOwnerMigrationKeys")
                                : null;
                        String versionedKey = REPORT_OWNER_MIGRATION_VERSION + ":" + finalTargetUserId;
                        if (migratedKeys != null && migratedKeys.contains(versionedKey)) {
                            if (cb != null) cb.onComplete(true);
                            return;
                        }
                        migrateReportOwnerIdsInternal(sourceIds, 0, finalTargetUserId, userDocId, cb);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed reading migration state", e);
                        migrateReportOwnerIdsInternal(sourceIds, 0, finalTargetUserId, userDocId, cb);
                    });
        });
    }

    private static void migrateReportOwnerIdsInternal(
            List<String> sourceIds,
            int index,
            String targetUserId,
            String userDocId,
            SimpleCallback<Boolean> cb
    ) {
        if (index >= sourceIds.size()) {
            markReportOwnerMigrationDoneRemote(userDocId, targetUserId);
            if (cb != null) cb.onComplete(true);
            return;
        }

        String sourceId = sourceIds.get(index);
        db.collection("reports")
                .whereEqualTo("userId", sourceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        migrateReportOwnerIdsInternal(sourceIds, index + 1, targetUserId, userDocId, cb);
                        return;
                    }
                    WriteBatch batch = db.batch();
                    int updates = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (doc == null || doc.getReference() == null) continue;
                        batch.update(doc.getReference(), "userId", targetUserId);
                        updates++;
                    }
                    if (updates == 0) {
                        migrateReportOwnerIdsInternal(sourceIds, index + 1, targetUserId, userDocId, cb);
                        return;
                    }
                    batch.commit()
                            .addOnSuccessListener(v ->
                                    migrateReportOwnerIdsInternal(sourceIds, index + 1, targetUserId, userDocId, cb))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Legacy report userId migration failed for source: " + sourceId, e);
                                if (cb != null) cb.onComplete(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Legacy report query failed for source: " + sourceId, e);
                    if (cb != null) cb.onComplete(false);
                });
    }

    private static void resolveCurrentUserDocId(Context ctx, SimpleCallback<String> cb) {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser != null && authUser.getUid() != null && !authUser.getUid().trim().isEmpty()) {
            cb.onComplete(authUser.getUid().trim());
            return;
        }
        String email = getLoggedInEmail(ctx);
        if (email == null || email.trim().isEmpty()) {
            cb.onComplete(null);
            return;
        }
        String normalized = email.trim().toLowerCase();
        db.collection("users")
                .whereEqualTo("email", normalized)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        cb.onComplete(null);
                        return;
                    }
                    cb.onComplete(snapshot.getDocuments().get(0).getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to resolve user doc for migration", e);
                    cb.onComplete(null);
                });
    }

    private static void markReportOwnerMigrationDoneRemote(String userDocId, String targetUserId) {
        if (userDocId == null || userDocId.trim().isEmpty()) return;
        if (targetUserId == null || targetUserId.trim().isEmpty()) return;
        String versionedKey = REPORT_OWNER_MIGRATION_VERSION + ":" + targetUserId.trim();
        Map<String, Object> updates = new HashMap<>();
        updates.put("reportOwnerMigrationKeys", FieldValue.arrayUnion(versionedKey));
        updates.put("reportOwnerMigrationUpdatedAt", FieldValue.serverTimestamp());
        db.collection("users")
                .document(userDocId.trim())
                .set(updates, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark report owner migration done", e));
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

    // ───────────── POSTS (COMMUNITY FEED) ─────────────

    public static void getCurrentUserProfile(Context ctx, SimpleCallback<Map<String, Object>> cb) {
        com.google.firebase.auth.FirebaseUser authUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String email = authUser != null ? authUser.getEmail() : getLoggedInEmail(ctx);
        String uid = authUser != null ? authUser.getUid() : null;
        Map<String, Object> fallback = new HashMap<>();
        String safeEmail = email != null ? email.trim().toLowerCase() : null;
        fallback.put("userId", uid != null ? uid : (safeEmail != null ? safeEmail : "anonymous"));
        fallback.put("userName", safeEmail != null && safeEmail.contains("@") ? safeEmail.split("@")[0] : "Anonymous");
        fallback.put("userPhotoUrl", null);

        if (uid == null || uid.trim().isEmpty()) {
            if (safeEmail == null || safeEmail.isEmpty()) {
                cb.onComplete(fallback);
                return;
            }
            db.collection("users")
                    .whereEqualTo("email", safeEmail)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot == null || snapshot.isEmpty()) {
                            cb.onComplete(fallback);
                            return;
                        }
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        String name = doc.getString("name");
                        if (name == null || name.trim().isEmpty()) {
                            name = doc.getString("fullName");
                        }
                        if (name == null || name.trim().isEmpty()) {
                            String first = doc.getString("firstName");
                            String last = doc.getString("lastName");
                            if (first != null && !first.trim().isEmpty()) {
                                name = first.trim() + (last != null && !last.trim().isEmpty() ? " " + last.trim() : "");
                            }
                        }
                        if (name == null || name.trim().isEmpty()) {
                            name = doc.getString("username");
                        }
                        Map<String, Object> profile = new HashMap<>();
                        profile.put("userId", doc.getId());
                        profile.put("userName", name != null && !name.trim().isEmpty() ? name : fallback.get("userName"));
                        profile.put("userPhotoUrl", doc.getString("photoUrl"));
                        cb.onComplete(profile);
                    })
                    .addOnFailureListener(e -> cb.onComplete(fallback));
            return;
        }

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        cb.onComplete(fallback);
                        return;
                    }
                    String name = doc.getString("name");
                    if (name == null || name.trim().isEmpty()) {
                        name = doc.getString("fullName");
                    }
                    if (name == null || name.trim().isEmpty()) {
                        String first = doc.getString("firstName");
                        String last = doc.getString("lastName");
                        if (first != null && !first.trim().isEmpty()) {
                            name = first.trim() + (last != null && !last.trim().isEmpty() ? " " + last.trim() : "");
                        }
                    }
                    if (name == null || name.trim().isEmpty()) {
                        name = doc.getString("username");
                    }
                    if (name == null || name.trim().isEmpty() && email != null) {
                        name = email.split("@")[0];
                    }
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("userId", doc.getId());
                    profile.put("userName", name);
                    profile.put("userPhotoUrl", doc.getString("photoUrl"));
                    cb.onComplete(profile);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getCurrentUserProfile failed", e);
                    cb.onComplete(fallback);
                });
    }

    public static void addPost(Map<String, Object> data, SimpleCallback<Boolean> cb) {
        db.collection("posts")
                .add(data)
                .addOnSuccessListener(d -> cb.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "addPost failed", e);
                    cb.onComplete(false);
                });
    }

    public static ListenerRegistration listenPosts(EventListener<QuerySnapshot> listener) {
        return db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public static void setLikeOnPost(String postId, String userKey, boolean like, SimpleCallback<Boolean> cb) {
        if (postId == null || userKey == null) {
            cb.onComplete(false);
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("likes", like ? FieldValue.arrayUnion(userKey) : FieldValue.arrayRemove(userKey));
        updates.put("likeCount", FieldValue.increment(like ? 1 : -1));

        db.collection("posts")
                .document(postId)
                .update(updates)
                .addOnSuccessListener(v -> cb.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "setLikeOnPost failed", e);
                    cb.onComplete(false);
                });
    }

    public static void addCommentToPost(String postId, Map<String, Object> comment, SimpleCallback<Boolean> cb) {
        if (postId == null || comment == null) {
            cb.onComplete(false);
            return;
        }
        db.collection("posts")
                .document(postId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(d ->
                        db.collection("posts")
                                .document(postId)
                                .update("commentCount", FieldValue.increment(1))
                                .addOnSuccessListener(v -> cb.onComplete(true))
                                .addOnFailureListener(e -> cb.onComplete(false)))
                .addOnFailureListener(e -> cb.onComplete(false));
    }

    public static void incrementShareCount(String postId) {
        if (postId == null) return;
        db.collection("posts")
                .document(postId)
                .update("shareCount", FieldValue.increment(1))
                .addOnFailureListener(e -> Log.e(TAG, "incrementShareCount failed", e));
    }

    public static void getPostById(String postId, SimpleCallback<DocumentSnapshot> cb) {
        if (postId == null) {
            cb.onComplete(null);
            return;
        }
        db.collection("posts")
                .document(postId)
                .get()
                .addOnSuccessListener(cb::onComplete)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getPostById failed", e);
                    cb.onComplete(null);
                });
    }

    public static ListenerRegistration listenComments(String postId, EventListener<QuerySnapshot> listener) {
        return db.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    // ───────────── ANALYTICS (OPTIONAL) ─────────────

    public static void logEvent(String name, Bundle params) {
        FirebaseAnalytics analytics = MyApp.getAnalytics();
        if (analytics != null) {
            analytics.logEvent(name, params);
        }
    }

    public static StorageReference getStorageRoot() {
        return FirebaseStorage.getInstance().getReferenceFromUrl(STORAGE_BUCKET);
    }

    public static void saveFcmToken(String token) {
        saveFcmToken(MyApp.getContext(), token);
    }

    public static void saveFcmToken(Context ctx, String token) {
        if (token == null || token.trim().isEmpty()) return;
        com.google.firebase.auth.FirebaseUser authUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (authUser != null && authUser.getUid() != null && !authUser.getUid().trim().isEmpty()) {
            saveFcmTokenToUid(authUser.getUid().trim(), token.trim());
            clearPendingFcmToken(ctx);
            return;
        }
        String email = ctx != null ? getLoggedInEmail(ctx) : null;
        if (email != null && !email.trim().isEmpty()) {
            saveFcmTokenToEmail(email.trim().toLowerCase(), token.trim());
            clearPendingFcmToken(ctx);
            return;
        }
        storePendingFcmToken(ctx, token.trim());
    }

    private static void saveFcmTokenToUid(String uid, String token) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmTokens", FieldValue.arrayUnion(token));
        updates.put("fcmTokenUpdatedAt", FieldValue.serverTimestamp());
        db.collection("users")
                .document(uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token", e));
    }

    private static void saveFcmTokenToEmail(String email, String token) {
        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        Log.w(TAG, "No user found for email to save FCM token");
                        return;
                    }
                    String docId = snapshot.getDocuments().get(0).getId();
                    saveFcmTokenToUid(docId, token);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to lookup user for FCM token", e));
    }

    private static void storePendingFcmToken(Context ctx, String token) {
        if (ctx == null) return;
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PENDING_FCM, token)
                .apply();
    }

    private static void clearPendingFcmToken(Context ctx) {
        if (ctx == null) return;
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PENDING_FCM)
                .apply();
    }

    public static void flushPendingFcmToken(Context ctx) {
        if (ctx == null) return;
        String pending = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_PENDING_FCM, null);
        if (pending == null || pending.trim().isEmpty()) return;
        saveFcmToken(ctx, pending);
    }
}
