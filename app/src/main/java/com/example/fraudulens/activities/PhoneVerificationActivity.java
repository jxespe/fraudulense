package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fraudulens.R;
import com.example.fraudulens.utils.FirebaseUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.function.Consumer;

public class PhoneVerificationActivity extends AppCompatActivity {

    private TextInputEditText etPhoneNumber;
    private MaterialButton btnGetOtp;
    private TextView tvLoginLink;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verification);

        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnGetOtp = findViewById(R.id.btnGetOtp);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        btnGetOtp.setOnClickListener(v -> sendOTP());

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void sendOTP() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        
        if (phoneNumber.isEmpty()) {
            etPhoneNumber.setError("Please enter your phone number");
            return;
        }

        // Remove any non-digit characters
        phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
        
        // Validate phone number length (Philippines: 10 digits without country code)
        if (phoneNumber.length() < 10 || phoneNumber.length() > 11) {
            etPhoneNumber.setError("Please enter a valid 10-digit phone number");
            return;
        }

        // Remove leading 0 if present (e.g., 09123456789 -> 9123456789)
        if (phoneNumber.startsWith("0")) {
            phoneNumber = phoneNumber.substring(1);
        }

        btnGetOtp.setEnabled(false);
        btnGetOtp.setText("Sending OTP...");
        String fullPhoneNumber = "+63" + phoneNumber;

        android.util.Log.d("PhoneVerification", "Sending OTP to: " + fullPhoneNumber);

        // Send OTP using Firebase
        FirebaseUtils.sendVerificationCode(fullPhoneNumber, this, new Consumer<String>() {
            @Override
            public void accept(String verificationId) {
                runOnUiThread(() -> {
                    btnGetOtp.setEnabled(true);
                    btnGetOtp.setText("Get OTP");
                    
                    if (verificationId != null && !verificationId.isEmpty()) {
                        android.util.Log.d("PhoneVerification", "OTP sent successfully, navigating to OTP screen");
                        Toast.makeText(PhoneVerificationActivity.this, "OTP sent to " + fullPhoneNumber, Toast.LENGTH_SHORT).show();
                        // Navigate to OTP screen
                        Intent intent = new Intent(PhoneVerificationActivity.this, OtpActivity.class);
                        intent.putExtra("verificationId", verificationId);
                        intent.putExtra("phoneNumber", fullPhoneNumber);
                        startActivity(intent);
                    } else {
                        Toast.makeText(PhoneVerificationActivity.this, "Failed to send OTP. Please try again.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        
        // Re-enable button after timeout (in case callback never fires)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!btnGetOtp.isEnabled()) {
                btnGetOtp.setEnabled(true);
                btnGetOtp.setText("Get OTP");
                android.util.Log.w("PhoneVerification", "OTP request timed out - re-enabling button");
            }
        }, 65000); // 65 seconds (slightly longer than Firebase timeout)
    }
}
