package com.example.fraudulens.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
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

        // 🔒 Firebase Auth REMOVED — custom auth in use
        Toast.makeText(
                this,
                "Password reset is not available.\nPlease contact support or register again.",
                Toast.LENGTH_LONG
        ).show();
    }
}
