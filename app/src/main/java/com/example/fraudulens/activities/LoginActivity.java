package com.example.fraudulens.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.ImageButton;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_REMEMBER_EMAIL = "remember_email";
    private static final String KEY_SAVED_EMAIL = "saved_email";

    private EditText etEmail, etPass;
    private Button btnLogin;
    private TextView tvForgot, tvRegister;
    private CheckBox cbRememberMe;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        ImageButton btnBackNav = findViewById(R.id.btnBackNav);
        if (btnBackNav != null) {
            btnBackNav.setOnClickListener(v -> onBackPressed());
        }

        etEmail = findViewById(R.id.etEmail);
        etPass = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgot = findViewById(R.id.tvForgot);
        tvRegister = findViewById(R.id.tvRegister);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvForgot.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class))
        );
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, StarterActivity.class))
        );

        // Load saved email if Remember me was checked
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_EMAIL, false);
        if (rememberMe) {
            String savedEmail = prefs.getString(KEY_SAVED_EMAIL, "");
            etEmail.setText(savedEmail);
            cbRememberMe.setChecked(true);
        }

        // ✅ Custom session check (NO FirebaseAuth)
        if (FirebaseHelper.isLoggedIn(this)) {
            startMain();
        }
    }

    private void attemptLogin() {
        String emailOrUsername = etEmail.getText().toString().trim();
        String pass  = etPass.getText().toString();

        if (emailOrUsername.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this,
                    "Please enter email/username and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        // Save email/username if Remember me is checked
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (cbRememberMe.isChecked()) {
            editor.putBoolean(KEY_REMEMBER_EMAIL, true);
            editor.putString(KEY_SAVED_EMAIL, emailOrUsername);
        } else {
            editor.putBoolean(KEY_REMEMBER_EMAIL, false);
            editor.remove(KEY_SAVED_EMAIL);
        }
        editor.apply();

        // ✅ Supports both email and username login
        FirebaseHelper.login(this, emailOrUsername, pass, false, success -> runOnUiThread(() -> {
            btnLogin.setEnabled(true);

            if (success) {
                FirebaseHelper.checkUserSuspension(emailOrUsername, result -> runOnUiThread(() -> {
                    boolean suspended = result.get("suspended") instanceof Boolean && (Boolean) result.get("suspended");
                    if (suspended) {
                        com.google.firebase.Timestamp suspendedAt = result.get("suspendedAt") instanceof com.google.firebase.Timestamp
                                ? (com.google.firebase.Timestamp) result.get("suspendedAt")
                                : null;
                        com.google.firebase.Timestamp suspendedUntil = result.get("suspendedUntil") instanceof com.google.firebase.Timestamp
                                ? (com.google.firebase.Timestamp) result.get("suspendedUntil")
                                : null;
                        String since = formatTimestamp(suspendedAt);
                        String until = formatTimestamp(suspendedUntil);
                        String durationText = formatDuration(suspendedAt, suspendedUntil);
                        String reason = result.get("suspendedReason") instanceof String ? (String) result.get("suspendedReason") : "";
                        String message = "Your account was suspended for " + durationText +
                                " since " + (since.isEmpty() ? "N/A" : since) + ".\n\n" +
                                "Until: " + (until.isEmpty() ? "N/A" : until) + "\n";
                        if (!reason.isEmpty()) {
                            message += "\nReason: " + reason + "\n";
                        }
                        message += "\nIf you want to appeal, contact administration.";
                        new AlertDialog.Builder(this)
                                .setTitle("Account Suspended")
                                .setMessage(message)
                                .setPositiveButton("OK", (d, which) -> d.dismiss())
                                .setCancelable(false)
                                .show();
                        return;
                    }
                    FirebaseHelper.hasPinForLogin(emailOrUsername, hasPin -> runOnUiThread(() -> {
                        FirebaseMessaging.getInstance().getToken()
                                .addOnSuccessListener(token -> FirebaseHelper.saveFcmToken(this, token));
                        if (hasPin) {
                            FirebaseHelper.logUserActivity(this, "login_password_verified");
                            Intent intent = new Intent(this, PinLoginActivity.class);
                            intent.putExtra(PinLoginActivity.EXTRA_LOGIN_ID, emailOrUsername);
                            startActivity(intent);
                        } else {
                            FirebaseHelper.logUserActivity(this, "login_password_verified");
                            Intent intent = new Intent(this, PinSetupActivity.class);
                            intent.putExtra(PinSetupActivity.EXTRA_LOGIN_ID, emailOrUsername);
                            startActivity(intent);
                        }
                    }));
                }));
            } else {
                Toast.makeText(this,
                        "Invalid email or password",
                        Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private String formatTimestamp(Object value) {
        if (value instanceof com.google.firebase.Timestamp) {
            java.util.Date date = ((com.google.firebase.Timestamp) value).toDate();
            return new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(date);
        }
        return "";
    }

    private String formatDuration(com.google.firebase.Timestamp start, com.google.firebase.Timestamp end) {
        if (start == null || end == null) return "a period";
        long millis = end.toDate().getTime() - start.toDate().getTime();
        if (millis <= 0) return "a period";
        long days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(millis);
        if (days <= 0) return "a short period";
        return days + (days == 1 ? " day" : " days");
    }

    private void startMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
