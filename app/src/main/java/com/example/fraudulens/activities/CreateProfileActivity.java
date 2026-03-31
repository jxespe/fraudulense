package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.ImageButton;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class CreateProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etUsername, etPassword, etConfirmPassword;
    private MaterialButton btnCreateProfile;
    private String phoneNumber;
    private String pendingEmail;
    private String pendingUsername;
    private String pendingPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);

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

        phoneNumber = getIntent().getStringExtra("phoneNumber");
        pendingEmail = getIntent().getStringExtra("email");
        pendingUsername = getIntent().getStringExtra("username");
        pendingPassword = getIntent().getStringExtra("password");
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Toast.makeText(this, "Phone number not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etName = findViewById(R.id.etName);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnCreateProfile = findViewById(R.id.btnCreateProfile);

        if (pendingUsername != null && !pendingUsername.trim().isEmpty()) {
            etUsername.setText(pendingUsername.trim());
        }

        btnCreateProfile.setOnClickListener(v -> createProfile());
    }

    private void createProfile() {
        String name = valueOf(etName);
        String username = valueOf(etUsername);
        String password = valueOf(etPassword);
        String confirmPassword = valueOf(etConfirmPassword);
        com.google.firebase.auth.FirebaseUser authUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        boolean oauthFlow = authUser != null
                && authUser.getProviderData() != null
                && authUser.getProviderData().stream().anyMatch(info ->
                "google.com".equals(info.getProviderId())
                        || "facebook.com".equals(info.getProviderId())
                        || "apple.com".equals(info.getProviderId()));

        if (name.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        btnCreateProfile.setEnabled(false);

        String existingEmail = pendingEmail != null && !pendingEmail.trim().isEmpty()
                ? pendingEmail.trim()
                : FirebaseHelper.getLoggedInEmail(this);
        String authUid = authUser != null ? authUser.getUid() : null;
        if (authUid == null) {
            btnCreateProfile.setEnabled(true);
            Toast.makeText(this, "Authentication session expired. Please verify your phone again.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseHelper.saveProfileByAuthUid(
                authUid,
                name,
                username,
                password,
                existingEmail,
                phoneNumber,
                success -> runOnUiThread(() -> {
                    if (success) {
                        if (!oauthFlow) {
                            linkEmailToPhoneAuth(username, phoneNumber);
                        } else {
                            proceedToPinSetup(username);
                        }
                    } else {
                        btnCreateProfile.setEnabled(true);
                        Toast.makeText(this, "Username already exists.", Toast.LENGTH_LONG).show();
                    }
                })
        );
    }

    private String valueOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void linkEmailToPhoneAuth(String loginId, String phone) {
        if (pendingEmail == null || pendingEmail.trim().isEmpty()
                || pendingPassword == null || pendingPassword.trim().isEmpty()) {
            proceedToPinSetup(loginId);
            return;
        }

        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            proceedToPinSetup(loginId);
            return;
        }

        com.google.firebase.auth.AuthCredential credential =
                com.google.firebase.auth.EmailAuthProvider
                        .getCredential(pendingEmail.trim().toLowerCase(), pendingPassword.trim());

        user.linkWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseHelper.updateUserAuthUidByPhone(phone, user.getUid(), ok -> {});
                    proceedToPinSetup(loginId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Email link failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    proceedToPinSetup(loginId);
                });
    }

    private void proceedToPinSetup(String loginId) {
        Toast.makeText(this, "Account created. Set your PIN.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, PinSetupActivity.class);
        intent.putExtra(PinSetupActivity.EXTRA_LOGIN_ID, loginId);
        startActivity(intent);
        finish();
    }
}
