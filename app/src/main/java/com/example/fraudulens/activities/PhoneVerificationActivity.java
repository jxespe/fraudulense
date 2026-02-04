package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.fraudulens.R;
import com.example.fraudulens.utils.FirebaseUtils;
import com.example.fraudulens.utils.PhoneFormatUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.function.Consumer;
import android.widget.ImageButton;
import android.text.InputFilter;

public class PhoneVerificationActivity extends AppCompatActivity {

    private TextInputEditText etPhoneNumber;
    private MaterialButton btnGetOtp;
    private TextView tvLoginLink;
    private Spinner spCountryCode;
    private boolean isFormatting;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verification);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        ImageButton btnBackNav = findViewById(R.id.btnBackNav);
        if (btnBackNav != null) {
            btnBackNav.setOnClickListener(v -> onBackPressed());
        }

        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        spCountryCode = findViewById(R.id.spCountryCode);
        btnGetOtp = findViewById(R.id.btnGetOtp);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        if (spCountryCode != null) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.country_codes,
                    R.layout.item_country_code_spinner
            );
            adapter.setDropDownViewResource(R.layout.item_country_code_spinner_dropdown);
            spCountryCode.setAdapter(adapter);
            int defaultIndex = adapter.getPosition("+63");
            if (defaultIndex >= 0) {
                spCountryCode.setSelection(defaultIndex);
            }
        }

        etPhoneNumber.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                String digits = PhoneFormatUtil.digitsOnly(s.toString());
                if (digits.length() > 10) {
                    digits = digits.substring(0, 10);
                }
                String formatted = formatInput(digits);
                etPhoneNumber.setText(formatted);
                etPhoneNumber.setSelection(formatted.length());
                isFormatting = false;
            }
        });

        btnGetOtp.setOnClickListener(v -> sendOTP());

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

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

        String local10 = PhoneFormatUtil.toLocal10(phoneNumber);
        if (local10.length() != 10) {
            etPhoneNumber.setError("Please enter a valid 10-digit phone number");
            return;
        }

        btnGetOtp.setEnabled(false);
        btnGetOtp.setText("Sending OTP...");
        String selectedCode = "+63";
        if (spCountryCode != null && spCountryCode.getSelectedItem() != null) {
            selectedCode = spCountryCode.getSelectedItem().toString();
        }
        String fullPhoneNumber = PhoneFormatUtil.toE164(local10, selectedCode);
        String displayPhone = selectedCode + " " + PhoneFormatUtil.formatLocal(local10);

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
                        Toast.makeText(PhoneVerificationActivity.this, "OTP sent to " + displayPhone, Toast.LENGTH_SHORT).show();
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

    private String formatInput(String digits) {
        if (digits.length() <= 3) return digits;
        if (digits.length() <= 6) return digits.substring(0, 3) + " " + digits.substring(3);
        return digits.substring(0, 3) + " " + digits.substring(3, 6) + " " + digits.substring(6);
    }
}
