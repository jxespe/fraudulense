package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DELAY_MS = 1200;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (FirebaseHelper.isLoggedIn(this)) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                startActivity(new Intent(this, StarterActivity.class));
            }
            finish();
        }, SPLASH_DELAY_MS);
    }
}
