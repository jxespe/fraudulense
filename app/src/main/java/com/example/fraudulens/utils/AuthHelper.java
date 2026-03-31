package com.example.fraudulens.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.activities.MainActivity;
import com.example.fraudulens.activities.PhoneVerificationActivity;
import com.example.fraudulens.activities.PremiumActivity;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AuthHelper {
    private static final String TAG = "AuthHelper";
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    public static final int RC_GOOGLE_SIGN_IN = 9001;

    /**
     * Check if Google Sign-In is properly configured
     */
    public static boolean isGoogleSignInConfigured(Activity activity) {
        String clientId = activity.getString(com.example.fraudulens.R.string.default_web_client_id);
        return clientId != null && !clientId.isEmpty() && !clientId.equals("YOUR_WEB_CLIENT_ID");
    }

    /**
     * Initialize Google Sign-In
     */
    public static GoogleSignInClient getGoogleSignInClient(Activity activity) {
        String clientId = activity.getString(com.example.fraudulens.R.string.default_web_client_id);
        
        // Check if properly configured
        if (clientId == null || clientId.isEmpty() || clientId.equals("YOUR_WEB_CLIENT_ID")) {
            Log.e(TAG, "Google Sign-In not configured. Please set default_web_client_id in strings.xml");
            return null;
        }
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .build();
        return GoogleSignIn.getClient(activity, gso);
    }

    /**
     * Handle Google Sign-In result
     */
    public static void handleGoogleSignInResult(Intent data, Activity activity) {
        if (!isGoogleSignInConfigured(activity)) {
            Toast.makeText(activity, "Google Sign-In is not configured. Please check your Firebase setup.", Toast.LENGTH_LONG).show();
            return;
        }
        
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken(), activity);
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google sign in failed", e);
            String errorMessage = "Google sign in failed";
            
            // Provide more helpful error messages
            switch (e.getStatusCode()) {
                case 10: // DEVELOPER_ERROR
                    errorMessage = "Google Sign-In not configured. Please set up Firebase and add Web Client ID.";
                    break;
                case 12501: // SIGN_IN_CANCELLED
                    errorMessage = "Sign in cancelled";
                    break;
                case 7: // NETWORK_ERROR
                    errorMessage = "Network error. Please check your internet connection.";
                    break;
                default:
                    errorMessage = "Google sign in failed: " + e.getMessage();
                    break;
            }
            
            Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Authenticate with Firebase using Google credentials
     */
    private static void firebaseAuthWithGoogle(String idToken, Activity activity) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveSocialLoginUser(user, "google", activity);
                        }
                    } else {
                        Log.e(TAG, "Firebase auth with Google failed", task.getException());
                        Toast.makeText(activity, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Check if Facebook Login is properly configured
     */
    public static boolean isFacebookLoginConfigured(Activity activity) {
        String appId = activity.getString(com.example.fraudulens.R.string.facebook_app_id);
        return appId != null && !appId.isEmpty() && !appId.equals("YOUR_FACEBOOK_APP_ID");
    }

    /**
     * Initialize Facebook Login
     */
    public static CallbackManager initializeFacebookLogin(Activity activity) {
        if (!isFacebookLoginConfigured(activity)) {
            Log.e(TAG, "Facebook Login not configured. Please set facebook_app_id in strings.xml");
            return null;
        }
        return CallbackManager.Factory.create();
    }

    /**
     * Trigger Facebook Login
     */
    public static void loginWithFacebook(Activity activity, CallbackManager callbackManager) {
        if (!isFacebookLoginConfigured(activity)) {
            Toast.makeText(activity, "Facebook Login is not configured. Please check your Facebook App setup.", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (callbackManager == null) {
            Toast.makeText(activity, "Facebook Login not initialized. Please check configuration.", Toast.LENGTH_LONG).show();
            return;
        }
        
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken(), activity);
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "Facebook login cancelled");
                Toast.makeText(activity, "Facebook login cancelled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(TAG, "Facebook login error", error);
                String errorMessage = "Facebook login failed";
                
                // Provide more helpful error messages
                if (error.getMessage() != null && error.getMessage().contains("Invalid App ID")) {
                    errorMessage = "Invalid Facebook App ID. Please configure facebook_app_id in strings.xml";
                } else if (error.getMessage() != null) {
                    errorMessage = "Facebook login failed: " + error.getMessage();
                }
                
                Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
        
        LoginManager.getInstance().logInWithReadPermissions(activity, Arrays.asList("email", "public_profile"));
    }

    /**
     * Handle Facebook access token
     */
    private static void handleFacebookAccessToken(AccessToken token, Activity activity) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveSocialLoginUser(user, "facebook", activity);
                        }
                    } else {
                        Log.e(TAG, "Firebase auth with Facebook failed", task.getException());
                        Toast.makeText(activity, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Save user data from social login to Firestore
     */
    private static void saveSocialLoginUser(FirebaseUser firebaseUser, String provider, Activity activity) {
        String email = firebaseUser.getEmail();
        String name = firebaseUser.getDisplayName();
        String uid = firebaseUser.getUid();
        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null;

        if (email == null) {
            email = firebaseUser.getUid() + "@" + provider + ".com";
        }
        final String normalizedEmail = email.toLowerCase();
        final String finalName = name != null ? name : "User";

        Log.d(TAG, "Social login authenticated: " + normalizedEmail);
        proceedAfterSocialLogin(activity, normalizedEmail, finalName, uid, provider, photoUrl);
    }

    /**
     * Proceed after successful social login
     */
    private static void proceedAfterSocialLogin(
            Activity activity,
            String email,
            String name,
            String uid,
            String provider,
            String photoUrl
    ) {
        // Save login state
        FirebaseHelper.setLoggedIn(activity, email);
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> FirebaseHelper.saveFcmToken(activity, token));
        
        if (uid == null || uid.trim().isEmpty()) {
            Intent intent = new Intent(activity, PhoneVerificationActivity.class);
            intent.putExtra("email", email);
            intent.putExtra("name", name);
            activity.startActivity(intent);
            activity.finishAffinity();
            return;
        }

        ensureOAuthEmailStored(uid, email, provider, name, photoUrl);

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists() && snapshot.getBoolean("hasProfile") != null
                            && Boolean.TRUE.equals(snapshot.getBoolean("hasProfile"))) {
                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.putExtra("email", email);
                        activity.startActivity(intent);
                        activity.finishAffinity();
                    } else {
                        Intent intent = new Intent(activity, PhoneVerificationActivity.class);
                        intent.putExtra("email", email);
                        intent.putExtra("name", name);
                        activity.startActivity(intent);
                        activity.finishAffinity();
                    }
                })
                .addOnFailureListener(e -> {
                    Intent intent = new Intent(activity, PhoneVerificationActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("name", name);
                    activity.startActivity(intent);
                    activity.finishAffinity();
                });
    }

    private static void ensureOAuthEmailStored(
            String uid,
            String email,
            String provider,
            String name,
            String photoUrl
    ) {
        if (uid == null || uid.trim().isEmpty()) return;
        String normalizedEmail = email != null ? email.trim().toLowerCase() : null;
        if (normalizedEmail == null || normalizedEmail.isEmpty()) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("email", normalizedEmail);
        updates.put("authUid", uid.trim());
        if (provider != null && !provider.trim().isEmpty()) {
            updates.put("provider", provider.trim());
        }
        if (name != null && !name.trim().isEmpty()) {
            updates.put("name", name.trim());
        }
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            updates.put("photoUrl", photoUrl.trim());
        }
        db.collection("users")
                .document(uid.trim())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to store OAuth email", e));
    }

    /**
     * Sign out from all providers
     */
    public static void signOut(Context context) {
        // Sign out from Firebase
        auth.signOut();
        
        // Sign out from Google (if configured)
        try {
            if (context instanceof android.app.Activity) {
                GoogleSignInClient googleSignInClient = getGoogleSignInClient((android.app.Activity) context);
                googleSignInClient.signOut();
            }
        } catch (Exception e) {
            // Google Sign-In not configured, ignore
            android.util.Log.d(TAG, "Google Sign-In not configured for sign out");
        }
        
        // Sign out from Facebook
        try {
            LoginManager.getInstance().logOut();
        } catch (Exception e) {
            // Facebook not configured, ignore
            android.util.Log.d(TAG, "Facebook not configured for sign out");
        }
        
        // Clear local session
        FirebaseHelper.logout(context);
    }

    /**
     * Trigger Apple Sign-In via Firebase OAuth provider
     */
    public static void loginWithApple(Activity activity) {
        OAuthProvider.Builder provider = OAuthProvider.newBuilder("apple.com");
        provider.setScopes(Arrays.asList("email", "name"));

        auth.startActivityForSignInWithProvider(activity, provider.build())
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        saveSocialLoginUser(user, "apple", activity);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase auth with Apple failed", e);
                    Toast.makeText(activity, "Apple sign in failed. Please check your configuration.", Toast.LENGTH_LONG).show();
                });
    }
}
