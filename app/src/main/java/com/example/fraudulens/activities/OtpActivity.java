package com.example.fraudulens.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fraudulens.R;
import com.example.fraudulens.utils.FirebaseUtils;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class OtpActivity extends AppCompatActivity {
    EditText etOtp;
    Button btnVerify;
    String verificationId;
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_otp);
        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerify);

        String phone = getIntent().getStringExtra("phone");
        // For demo use FirebaseUtils to send OTP (simple wrapper)
        FirebaseUtils.sendVerificationCode(phone, this, id -> verificationId = id);

        btnVerify.setOnClickListener(v -> {
            String code = etOtp.getText().toString().trim();
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            FirebaseUtils.signInWithPhoneCredential(credential, task -> {
                if (task.isSuccessful()) {
                    startActivity(new android.content.Intent(this, MainActivity.class));
                    finishAffinity();
                } else {
                    // show error
                }
            });
        });
    }
}
