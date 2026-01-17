package com.example.fraudulens.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;

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
                FirebaseHelper.hasPinForLogin(emailOrUsername, hasPin -> runOnUiThread(() -> {
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
