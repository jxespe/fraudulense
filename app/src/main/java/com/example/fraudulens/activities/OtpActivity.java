package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.ImageButton;
import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.utils.FirebaseUtils;
import com.example.fraudulens.utils.PhoneFormatUtil;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import java.util.function.Consumer;

public class OtpActivity extends AppCompatActivity {

    private TextInputEditText[] otpInputs = new TextInputEditText[6];
    private Button btnSubmit;
    private TextView tvTimer, tvResend, tvPhoneNumber;
    private String verificationId, phoneNumber;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

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

        verificationId = getIntent().getStringExtra("verificationId");
        phoneNumber = getIntent().getStringExtra("phoneNumber");

        if (verificationId == null || phoneNumber == null) {
            Toast.makeText(this, "Verification data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupOtpInputs();
        setupKeypad();
        startTimer();

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initializeViews() {
        otpInputs[0] = findViewById(R.id.etOtp1);
        otpInputs[1] = findViewById(R.id.etOtp2);
        otpInputs[2] = findViewById(R.id.etOtp3);
        otpInputs[3] = findViewById(R.id.etOtp4);
        otpInputs[4] = findViewById(R.id.etOtp5);
        otpInputs[5] = findViewById(R.id.etOtp6);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvTimer = findViewById(R.id.tvTimer);
        tvResend = findViewById(R.id.tvResend);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);

        tvPhoneNumber.setText(PhoneFormatUtil.formatLocal(phoneNumber));
        btnSubmit.setOnClickListener(v -> verifyOtp());
        tvResend.setOnClickListener(v -> resendOtp());
    }

    private void setupOtpInputs() {
        for (int i = 0; i < otpInputs.length; i++) {
            final int index = i;
            otpInputs[i].setShowSoftInputOnFocus(false);
            otpInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpInputs.length - 1) {
                        otpInputs[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupKeypad() {
        int[] buttonIds = {R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5,
                R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn0};

        for (int i = 0; i < buttonIds.length; i++) {
            final String digit = String.valueOf(i == 9 ? 0 : i + 1);
            Button btn = findViewById(buttonIds[i]);
            btn.setOnClickListener(v -> {
                for (TextInputEditText input : otpInputs) {
                    if (input.getText().toString().isEmpty()) {
                        input.setText(digit);
                        input.requestFocus();
                        break;
                    }
                }
            });
        }

        Button btnBackspace = findViewById(R.id.btnBackspace);
        if (btnBackspace != null) {
            btnBackspace.setOnClickListener(v -> clearLastDigit());
        }
    }

    private void clearLastDigit() {
        for (int i = otpInputs.length - 1; i >= 0; i--) {
            TextInputEditText input = otpInputs[i];
            if (input.getText() != null && !input.getText().toString().isEmpty()) {
                input.setText("");
                input.requestFocus();
                break;
            }
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(152000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                tvResend.setEnabled(true);
            }
        }.start();
    }

    private void verifyOtp() {
        StringBuilder otpCode = new StringBuilder();
        for (TextInputEditText input : otpInputs) {
            otpCode.append(input.getText().toString());
        }

        if (otpCode.length() != 6) {
            Toast.makeText(this, "Please enter complete OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otpCode.toString());
        FirebaseUtils.signInWithPhoneCredential(credential, task -> {
            btnSubmit.setEnabled(true);
            if (task.isSuccessful()) {
                FirebaseHelper.setVerifiedPhone(this, phoneNumber);
                FirebaseHelper.markPhoneVerified(phoneNumber);
                Intent intent = new Intent(this, CreateProfileActivity.class);
                            intent.putExtra("phoneNumber", phoneNumber);
                            startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendOtp() {
        FirebaseUtils.sendVerificationCode(phoneNumber, this, new Consumer<String>() {
            @Override
            public void accept(String id) {
                verificationId = id;
                tvResend.setEnabled(false);
                startTimer();
                Toast.makeText(OtpActivity.this, "OTP resent successfully", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
