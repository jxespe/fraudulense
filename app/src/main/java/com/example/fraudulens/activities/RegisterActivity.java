package com.example.fraudulens.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPass;
    private Button btnCreate;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etRegEmail);
        etPass = findViewById(R.id.etRegPassword);
        btnCreate = findViewById(R.id.btnCreate);

        btnCreate.setOnClickListener(v -> register());
    }

    private void register() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPass.getText().toString().trim();

        // ✅ Validation
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email address");
            return;
        }

        if (pass.length() < 6) {
            etPass.setError("Password must be at least 6 characters");
            return;
        }

        btnCreate.setEnabled(false);

        // Pass plain password - FirebaseHelper.register() will hash it
        FirebaseHelper.register(name, email, pass, success ->
                runOnUiThread(() -> {
                    btnCreate.setEnabled(true);

                    if (success) {
                        Toast.makeText(this,
                                "Account created. You can now log in.",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Email already exists or registration failed.",
                                Toast.LENGTH_LONG).show();
                    }
                })
        );
    }
}
