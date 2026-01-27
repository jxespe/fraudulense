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

        btnCreateProfile.setOnClickListener(v -> createProfile());
    }

    private void createProfile() {
        String name = valueOf(etName);
        String username = valueOf(etUsername);
        String password = valueOf(etPassword);
        String confirmPassword = valueOf(etConfirmPassword);
        if (name.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
        ) {
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

        String existingEmail = FirebaseHelper.getLoggedInEmail(this);
        FirebaseHelper.registerAfterPhoneVerification(
                name,
                username,
                password,
                phoneNumber,
                existingEmail,
                success -> runOnUiThread(() -> {
                    btnCreateProfile.setEnabled(true);
                    if (success) {
                        Toast.makeText(this, "Account created. Set your PIN.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(this, PinSetupActivity.class);
                        intent.putExtra(PinSetupActivity.EXTRA_LOGIN_ID, username);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Username or phone number already exists.", Toast.LENGTH_LONG).show();
                    }
                })
        );
    }

    private String valueOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
