package com.example.fraudulens.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;

public class ResetPasswordActivity extends AppCompatActivity {
    public static final String EXTRA_EMAIL = "reset_email";

    private EditText etResetEmail;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnResetPassword;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_reset_password);

        etResetEmail = findViewById(R.id.etResetEmail);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        String email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email != null) {
            etResetEmail.setText(email);
        }

        btnResetPassword.setOnClickListener(v -> handleReset());
    }

    private void handleReset() {
        String email = etResetEmail.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
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
        FirebaseHelper.resetPassword(email, newPass, success -> runOnUiThread(() -> {
            btnResetPassword.setEnabled(true);
            if (success) {
                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Unable to reset password", Toast.LENGTH_LONG).show();
            }
        }));
    }
}
