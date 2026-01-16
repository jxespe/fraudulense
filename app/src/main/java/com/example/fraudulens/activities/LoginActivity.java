package com.example.fraudulens.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.utils.AuthHelper;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_REMEMBER_EMAIL = "remember_email";
    private static final String KEY_SAVED_EMAIL = "saved_email";

    private EditText etEmail, etPass;
    private Button btnLogin, btnGoogleSignIn;
    private TextView tvForgot, tvRegister;
    private CheckBox cbRememberMe;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPass = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvForgot = findViewById(R.id.tvForgot);
        tvRegister = findViewById(R.id.tvRegister);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        // Initialize Google Sign-In (only if configured)
        googleSignInClient = AuthHelper.getGoogleSignInClient(this);
        
        // Hide/disable Google Sign-In button if not configured
        if (googleSignInClient == null || !AuthHelper.isGoogleSignInConfigured(this)) {
            btnGoogleSignIn.setVisibility(android.view.View.GONE);
            Log.d("LoginActivity", "Google Sign-In not configured, hiding button");
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        tvForgot.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class))
        );
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
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
        FirebaseHelper.login(this, emailOrUsername, pass, success -> runOnUiThread(() -> {
            btnLogin.setEnabled(true);

            if (success) {
                // setLoggedIn is already called inside FirebaseHelper.login()
                startMain();
            } else {
                Toast.makeText(this,
                        "Invalid email or password",
                        Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void signInWithGoogle() {
        if (googleSignInClient == null) {
            Toast.makeText(this, "Google Sign-In is not configured. Please set up Firebase and add Web Client ID.", Toast.LENGTH_LONG).show();
            return;
        }
        
        try {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, AuthHelper.RC_GOOGLE_SIGN_IN);
        } catch (Exception e) {
            Log.e("LoginActivity", "Error starting Google Sign-In", e);
            Toast.makeText(this, "Unable to start Google Sign-In. Please check your configuration.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == AuthHelper.RC_GOOGLE_SIGN_IN) {
            AuthHelper.handleGoogleSignInResult(data, this);
        }
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
