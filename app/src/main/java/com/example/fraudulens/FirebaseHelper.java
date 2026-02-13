package com.example.fraudulens;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.example.fraudulens.utils.PasswordUtil;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "FirebaseHelper";
    private static final String STORAGE_BUCKET = "gs://fraudulense.firebasestorage.app";

    // ───────────── SESSION (LOCAL LOGIN) ─────────────

    private static final String PREFS = "fraudulens_prefs";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone_number";
    private static final String KEY_LAST_SEEN_SCAM = "last_seen_scam";
    private static final String KEY_TRUSTED_NUMBERS = "trusted_numbers";
    private static final String KEY_TRUSTED_NAMES = "trusted_names";

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
    }

    public static String getLoggedInEmail(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_EMAIL, null);
    }

    public static String getVerifiedPhone(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_PHONE, null);
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
                                            cb.onComplete(false);
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
                        handlePhoneBasedRegistration(name, trimmedUsername, normalizedEmail, normalizedPhone, hash, cb);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "email check failed", e);
                        cb.onComplete(false);
                    });
            return;
        }

        handlePhoneBasedRegistration(name, trimmedUsername, normalizedEmail, normalizedPhone, hash, cb);
    }

    private static void handlePhoneBasedRegistration(
            String name,
            String trimmedUsername,
            String normalizedEmail,
            String normalizedPhone,
            String hash,
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
                        updateUserWithPhone(doc.getId(), name, trimmedUsername, normalizedEmail, normalizedPhone, hash, cb);
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
    public static void updateUserPhotoUrl(String email, String photoUrl, SimpleCallback<Boolean> cb) {
        if (email == null || email.trim().isEmpty()) {
            cb.onComplete(false);
            return;
        }
        final String normalizedEmail = email.trim().toLowerCase();
        db.collection("users")
                .whereEqualTo("email", normalizedEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Log.e(TAG, "User not found for photo update: " + normalizedEmail);
                        cb.onComplete(false);
                        return;
                    }
                    String docId = snapshot.getDocuments().get(0).getId();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("photoUrl", photoUrl);
                    db.collection("users")
                            .document(docId)
                            .update(updates)
                            .addOnSuccessListener(v -> {
                                updatePostsPhoto(docId, photoUrl);
                                cb.onComplete(true);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update photo url", e);
                                cb.onComplete(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding user for photo update", e);
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

        Log.d("LOGIN_DEBUG", "=== LOGIN ATTEMPT ===");
        Log.d("LOGIN_DEBUG", "Input (email/username): " + input);
        Log.d("LOGIN_DEBUG", "Normalized email: " + normalizedEmail);
        Log.d("LOGIN_DEBUG", "Password length: " + trimmedPassword.length());
        Log.d("LOGIN_DEBUG", "Password hash: " + hash);

        // Check if input looks like an email (contains @)
        boolean isEmail = input.contains("@");
        
        if (isEmail) {
            // Try normalized email first (for accounts created after fix)
            attemptLoginWithEmail(ctx, normalizedEmail, originalEmail, hash, cb, true, setSession);
        } else {
            // Try username login
            attemptLoginWithUsername(ctx, input, hash, cb, setSession);
        }
    }

    private static void attemptLoginWithEmail(
            Context ctx,
            String searchEmail,
            String fallbackEmail,
            String hash,
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
                        // Try fallback email if different from search email (for legacy accounts)
                        if (tryFallback && !searchEmail.equals(fallbackEmail)) {
                            Log.d("LOGIN_DEBUG", "Trying fallback with original email format: " + fallbackEmail);
                            attemptLoginWithEmail(ctx, fallbackEmail, fallbackEmail, hash, cb, false, setSession);
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
                        attemptLoginWithEmail(ctx, username.toLowerCase(), username, hash, cb, true, setSession);
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
                    } else {
                        Log.d("LOGIN_DEBUG", "Login failed - hash mismatch");
                    }

                    cb.onComplete(match);
                })
                .addOnFailureListener(e -> {
                    Log.e("LOGIN_DEBUG", "Username login query failed", e);
                    e.printStackTrace();
                    // Fallback: try as email
                    attemptLoginWithEmail(ctx, username.toLowerCase(), username, hash, cb, true, setSession);
                });
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
            handlePinCheck(ctx, doc, pinHash, cb);
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

    private static void handlePinCheck(Context ctx, DocumentSnapshot doc, String pinHash, SimpleCallback<Boolean> cb) {
        String storedPinHash = doc.getString("pinHash");
        String storedEmail = doc.getString("email");
        if (storedPinHash == null) {
            cb.onComplete(false);
            return;
        }
        boolean match = pinHash.equals(storedPinHash);
        if (match) {
            setLoggedIn(ctx, storedEmail != null ? storedEmail.toLowerCase() : "");
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
        payload.put("text", text.trim());
        payload.put("label", isScam ? 1 : 0);
        payload.put("source", source != null ? source : "unknown");
        payload.put("timestamp", FieldValue.serverTimestamp());
        payload.put("user", getLoggedInEmail(ctx) != null ? getLoggedInEmail(ctx) : "anonymous");
        payload.put("appVersion", BuildConfig.VERSION_NAME);

        db.collection("ml_training_samples")
                .add(payload)
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
        String email = getLoggedInEmail(ctx);
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("userId", "anonymous");
        fallback.put("userName", email != null ? email.split("@")[0] : "Anonymous");
        fallback.put("userPhotoUrl", null);

        if (email == null || email.trim().isEmpty()) {
            cb.onComplete(fallback);
            return;
        }

        db.collection("users")
                .whereEqualTo("email", email.toLowerCase())
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        cb.onComplete(fallback);
                        return;
                    }
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    String name = doc.getString("name");
                    if (name == null || name.trim().isEmpty()) {
                        name = doc.getString("username");
                    }
                    if (name == null || name.trim().isEmpty()) {
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
}
