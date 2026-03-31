package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {
    public static final String EXTRA_EMAIL = "reset_email";
    public static final String EXTRA_USER_ID = "reset_user_id";
    public static final String EXTRA_OTP_VERIFIED = "reset_otp_verified";

    private EditText etResetEmail;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnResetPassword;
    private String resetEmail;
    private String resetUserId;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_reset_password);

        etResetEmail = findViewById(R.id.etResetEmail);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        boolean otpVerified = getIntent().getBooleanExtra(EXTRA_OTP_VERIFIED, false);
        if (!otpVerified) {
            Toast.makeText(this, "Please verify OTP first.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        resetEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        resetUserId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (resetEmail != null) {
            etResetEmail.setText(resetEmail);
        }

        btnResetPassword.setOnClickListener(v -> handleReset());
    }

    private void handleReset() {
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(resetEmail)) {
            Toast.makeText(this, "Account email missing. Please restart reset flow.", Toast.LENGTH_LONG).show();
            return;
        }
        if (newPass.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        btnResetPassword.setEnabled(false);
        FirebaseHelper.SimpleCallback<Boolean> finishReset = success -> runOnUiThread(() -> {
            if (!success) {
                btnResetPassword.setEnabled(true);
                Toast.makeText(this, "Unable to reset password", Toast.LENGTH_LONG).show();
                return;
            }

            // OTP used Firebase Phone Auth only to satisfy Firestore rules — do not keep that session.
            // App login is the existing email/username + password (Firestore) flow, not a new Auth account.
            FirebaseAuth.getInstance().signOut();

            // Log in against the same user doc we just updated — avoids email-query mismatch and Auth migration
            // that could delete or bypass the real profile document.
            boolean useDocLogin = resetUserId != null && !resetUserId.trim().isEmpty();
            FirebaseHelper.SimpleCallback<Boolean> afterLogin = loginSuccess -> runOnUiThread(() -> {
                btnResetPassword.setEnabled(true);
                if (!loginSuccess) {
                    Toast.makeText(this, "Password changed. Please log in manually.", Toast.LENGTH_LONG).show();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.putExtra("email", resetEmail);
                    startActivity(i);
                    finish();
                    return;
                }
                Toast.makeText(this, "Password updated. Logged in successfully.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
            if (useDocLogin) {
                FirebaseHelper.loginWithUserDocumentId(this, resetUserId, newPass, true, afterLogin);
            } else {
                FirebaseHelper.login(this, resetEmail, newPass, true, afterLogin);
            }
        });

        if (resetUserId != null && !resetUserId.trim().isEmpty()) {
            FirebaseHelper.resetPasswordByUserId(resetUserId, newPass, finishReset);
        } else {
            FirebaseHelper.resetPassword(resetEmail, newPass, finishReset);
        }
    }
}
