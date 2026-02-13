package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText etResetEmail;
    Button btnSendReset;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_forgot_password);

        etResetEmail = findViewById(R.id.etResetEmail);
        btnSendReset = findViewById(R.id.btnSendReset);

        btnSendReset.setOnClickListener(v -> handleReset());
    }

    private void handleReset() {
        String email = etResetEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etResetEmail.setError("Email is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etResetEmail.setError("Invalid email address");
            return;
        }

        btnSendReset.setEnabled(false);
        FirebaseHelper.checkExistingAccountProvider(email, provider -> runOnUiThread(() -> {
            btnSendReset.setEnabled(true);
            if (provider == null) {
                Toast.makeText(this, "No account found for this email", Toast.LENGTH_LONG).show();
                return;
            }
            if (!"password".equals(provider)) {
                String providerName = provider.substring(0, 1).toUpperCase() + provider.substring(1);
                Toast.makeText(this, "Please sign in with " + providerName + ".", Toast.LENGTH_LONG).show();
                return;
            }
            Intent i = new Intent(this, ResetPasswordActivity.class);
            i.putExtra(ResetPasswordActivity.EXTRA_EMAIL, email);
            startActivity(i);
        }));
    }
}
