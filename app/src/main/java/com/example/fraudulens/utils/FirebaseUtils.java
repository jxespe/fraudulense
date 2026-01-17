package com.example.fraudulens.utils;

import android.app.Activity;
import androidx.annotation.NonNull;

import com.example.fraudulens.adapters.CommunityAdapter;
import com.example.fraudulens.models.Report;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;

public class FirebaseUtils {
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ✅ Send OTP verification
    public static void sendVerificationCode(String phone, Activity activity, Consumer<String> callback) {
        // Ensure phone number has country code
        String formattedPhone = phone.startsWith("+") ? phone : "+63" + phone;
        
        android.util.Log.d("FirebaseUtils", "Sending OTP to: " + formattedPhone);
        
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        android.util.Log.d("FirebaseUtils", "Verification completed automatically");
                        // Auto-verification (usually in emulator or test environment)
                        // You can handle this if needed
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        android.util.Log.e("FirebaseUtils", "OTP verification failed", e);
                        String errorMessage = "Failed to send OTP";
                        
                        // Provide helpful error messages
                        if (e.getMessage() != null) {
                            String msg = e.getMessage().toLowerCase();
                            if (msg.contains("invalid") && msg.contains("phone")) {
                                errorMessage = "Invalid phone number format. Please enter a valid 10-digit number.";
                            } else if (msg.contains("quota")) {
                                errorMessage = "SMS quota exceeded. Please try again later or use a test phone number.";
                            } else if (msg.contains("not allowed") || msg.contains("disabled") || 
                                       msg.contains("not enabled") || msg.contains("missing_instanceid") ||
                                       msg.contains("sign-in provider is disabled")) {
                                errorMessage = "Phone Authentication is disabled in Firebase. Please enable it in Firebase Console > Authentication > Sign-in method > Phone.";
                            } else if (msg.contains("network")) {
                                errorMessage = "Network error. Please check your internet connection.";
                            } else {
                                errorMessage = "Error sending OTP: " + e.getMessage();
                            }
                        }
                        
                        // Create final variable for lambda
                        final String finalErrorMessage = errorMessage;
                        
                        // Show error to user
                        activity.runOnUiThread(() -> {
                            android.widget.Toast.makeText(activity, finalErrorMessage, android.widget.Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        android.util.Log.d("FirebaseUtils", "OTP code sent successfully. Verification ID: " + verificationId);
                        if (callback != null) {
                            callback.accept(verificationId);
                        }
                    }
                }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // ✅ Sign in via phone OTP credential
    public static void signInWithPhoneCredential(PhoneAuthCredential credential, Consumer<Task<AuthResult>> callback) {
        auth.signInWithCredential(credential).addOnCompleteListener(callback::accept);
    }

    // ✅ Submit a report
    public static void submitReport(Report r, Consumer<Boolean> cb) {
        db.collection("reports")
                .add(r.toMap())
                .addOnSuccessListener(doc -> cb.accept(true))
                .addOnFailureListener(e -> cb.accept(false));
    }

    // ✅ Check if a phone or target has been flagged
    public static void checkIfFlagged(String query, Consumer<Boolean> cb) {
        db.collection("reports")
                .whereEqualTo("target", query)
                .whereEqualTo("status", "open")
                .limit(1)
                .get()
                .addOnCompleteListener(task -> cb.accept(task.isSuccessful()
                        && task.getResult() != null
                        && !task.getResult().isEmpty()));
    }

    // ✅ Listen to latest reports (real-time updates)
    public static ListenerRegistration listenLatestReports(Consumer<QuerySnapshot> cb) {
        return db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) cb.accept(snap);
                });
    }

    // ✅ Attach Firestore listener directly to RecyclerView adapter
    public static void attachReportsListener(CommunityAdapter adapter) {
        db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || e != null) return;

                    List<Report> items = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Report r = d.toObject(Report.class);
                        if (r != null) {
                            r.setId(d.getId()); // ✅ use setter instead of accessing private field
                            items.add(r);
                        }
                    }
                    adapter.update(items);
                });
    }

    // ✅ Admin helper
    public static void resolveReport(String docId) {
        db.collection("reports").document(docId).update("status", "resolved");
    }
}
