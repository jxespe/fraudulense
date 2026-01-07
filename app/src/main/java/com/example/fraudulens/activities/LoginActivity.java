package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPass;
    private Button btnLogin, btnRegister;
    private TextView tvForgot;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPass = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvForgot = findViewById(R.id.tvForgot);

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
        tvForgot.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class))
        );

        // ✅ Custom session check (NO FirebaseAuth)
        if (FirebaseHelper.isLoggedIn(this)) {
            startMain();
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String pass  = etPass.getText().toString();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this,
                    "Please enter email and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        // ✅ FIXED: pass Context as first argument
        FirebaseHelper.login(this, email, pass, success -> runOnUiThread(() -> {
            btnLogin.setEnabled(true);

            if (success) {
                FirebaseHelper.setLoggedIn(this, email);
                startMain();
            } else {
                Toast.makeText(this,
                        "Invalid email or password",
                        Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
