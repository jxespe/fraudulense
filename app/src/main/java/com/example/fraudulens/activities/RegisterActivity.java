package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.utils.AuthHelper;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etUsername, etEmail, etPass, etConfirmPass;
    private CheckBox cbTerms;
    private Button btnCreate, btnGoogleSignIn;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etRegEmail);
        etPass = findViewById(R.id.etRegPassword);
        etConfirmPass = findViewById(R.id.etConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnCreate = findViewById(R.id.btnCreate);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        // Initialize Google Sign-In (only if configured)
        googleSignInClient = AuthHelper.getGoogleSignInClient(this);
        
        // Hide/disable Google Sign-In button if not configured
        if (googleSignInClient == null || !AuthHelper.isGoogleSignInConfigured(this)) {
            btnGoogleSignIn.setVisibility(android.view.View.GONE);
            Log.d("RegisterActivity", "Google Sign-In not configured, hiding button");
        }

        btnCreate.setOnClickListener(v -> register());
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
    }

    private void register() {
        String name = etName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPass.getText().toString().trim();
        String confirmPass = etConfirmPass.getText().toString().trim();

        // ✅ Validation
        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
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

        if (!pass.equals(confirmPass)) {
            etConfirmPass.setError("Passwords do not match");
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to the Terms & Conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreate.setEnabled(false);

        // Pass plain password - FirebaseHelper.register() will hash it
        FirebaseHelper.register(name, username, email, pass, success ->
                runOnUiThread(() -> {
                    btnCreate.setEnabled(true);

                    if (success) {
                        // Navigate to phone verification
                        Intent intent = new Intent(RegisterActivity.this, PhoneVerificationActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Email already exists or registration failed.",
                                Toast.LENGTH_LONG).show();
                    }
                })
        );
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
            Log.e("RegisterActivity", "Error starting Google Sign-In", e);
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
}
