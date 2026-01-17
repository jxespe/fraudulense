package com.example.fraudulens.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.google.android.material.textfield.TextInputEditText;

public class AdminLoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "admin_login_prefs";
    private static final String KEY_REMEMBER_USERNAME = "remember_username";
    private static final String KEY_SAVED_USERNAME = "saved_username";

    private TextInputEditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvForgot;
    private CheckBox cbRememberMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgot = findViewById(R.id.tvForgot);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvForgot.setOnClickListener(v -> {
            Toast.makeText(this, "Please contact system administrator", Toast.LENGTH_SHORT).show();
        });

        // Load saved username if Remember me was checked
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_USERNAME, false);
        if (rememberMe) {
            String savedUsername = prefs.getString(KEY_SAVED_USERNAME, "");
            etUsername.setText(savedUsername);
            cbRememberMe.setChecked(true);
        }
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    "Please enter username and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        // Save username if Remember me is checked
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (cbRememberMe.isChecked()) {
            editor.putBoolean(KEY_REMEMBER_USERNAME, true);
            editor.putString(KEY_SAVED_USERNAME, username);
        } else {
            editor.putBoolean(KEY_REMEMBER_USERNAME, false);
            editor.remove(KEY_SAVED_USERNAME);
        }
        editor.apply();

        // Simple admin access to view activity logs (replace with real auth later)
        if ("admin".equals(username) && "admin123".equals(password)) {
            startActivity(new Intent(this, ActivityLogActivity.class));
            finish();
            return;
        }

        Toast.makeText(this, "Admin login failed", Toast.LENGTH_SHORT).show();
        btnLogin.setEnabled(true);
    }
}
