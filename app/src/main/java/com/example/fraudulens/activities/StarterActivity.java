package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.utils.AuthHelper;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;

public class StarterActivity extends AppCompatActivity {

    private MaterialButton btnApple, btnGoogle, btnFacebook, btnEmail;
    private TextView tvLoginLink;
    private GoogleSignInClient googleSignInClient;
    private CallbackManager facebookCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starter);

        btnApple = findViewById(R.id.btnApple);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
        btnEmail = findViewById(R.id.btnEmail);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        // Check if already logged in
        if (FirebaseHelper.isLoggedIn(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Initialize Google Sign-In
        googleSignInClient = AuthHelper.getGoogleSignInClient(this);
        
        // Hide Google button if not configured
        if (googleSignInClient == null || !AuthHelper.isGoogleSignInConfigured(this)) {
            btnGoogle.setVisibility(android.view.View.GONE);
            android.util.Log.d("StarterActivity", "Google Sign-In not configured, hiding button");
        }

        // Initialize Facebook Login
        facebookCallbackManager = AuthHelper.initializeFacebookLogin(this);
        
        // Hide Facebook button if not configured
        if (facebookCallbackManager == null || !AuthHelper.isFacebookLoginConfigured(this)) {
            btnFacebook.setVisibility(android.view.View.GONE);
            android.util.Log.d("StarterActivity", "Facebook Login not configured, hiding button");
        }

        btnApple.setOnClickListener(v -> AuthHelper.loginWithApple(this));

        btnGoogle.setOnClickListener(v -> {
            if (googleSignInClient == null || !AuthHelper.isGoogleSignInConfigured(this)) {
                Toast.makeText(this, "Google Sign-In is not configured. Please set up Firebase and add Web Client ID.", Toast.LENGTH_LONG).show();
                return;
            }
            
            try {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, AuthHelper.RC_GOOGLE_SIGN_IN);
            } catch (Exception e) {
                android.util.Log.e("StarterActivity", "Error starting Google Sign-In", e);
                Toast.makeText(this, "Unable to start Google Sign-In. Please check your configuration.", Toast.LENGTH_LONG).show();
            }
        });

        btnFacebook.setOnClickListener(v -> {
            if (facebookCallbackManager == null || !AuthHelper.isFacebookLoginConfigured(this)) {
                Toast.makeText(this, "Facebook Login is not configured. Please add Facebook App ID in strings.xml", Toast.LENGTH_LONG).show();
                return;
            }
            
            AuthHelper.loginWithFacebook(this, facebookCallbackManager);
        });

        btnEmail.setOnClickListener(v -> {
            startActivity(new Intent(this, PhoneVerificationActivity.class));
        });

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle Google Sign-In result
        if (requestCode == AuthHelper.RC_GOOGLE_SIGN_IN) {
            AuthHelper.handleGoogleSignInResult(data, this);
        } else {
            // Handle Facebook Login result
            if (facebookCallbackManager != null) {
                facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}
