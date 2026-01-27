package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.ImageButton;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.google.android.material.textfield.TextInputEditText;

public class PinSetupActivity extends AppCompatActivity {

    public static final String EXTRA_LOGIN_ID = "login_id";

    private TextInputEditText[] pinInputs = new TextInputEditText[4];
    private TextInputEditText[] confirmPinInputs = new TextInputEditText[4];
    private Button btnSubmitPin;
    private String loginId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_setup);

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

        loginId = getIntent().getStringExtra(EXTRA_LOGIN_ID);
        if (loginId == null || loginId.trim().isEmpty()) {
            Toast.makeText(this, "Login data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        pinInputs[0] = findViewById(R.id.etPin1);
        pinInputs[1] = findViewById(R.id.etPin2);
        pinInputs[2] = findViewById(R.id.etPin3);
        pinInputs[3] = findViewById(R.id.etPin4);
        confirmPinInputs[0] = findViewById(R.id.etConfirmPin1);
        confirmPinInputs[1] = findViewById(R.id.etConfirmPin2);
        confirmPinInputs[2] = findViewById(R.id.etConfirmPin3);
        confirmPinInputs[3] = findViewById(R.id.etConfirmPin4);
        btnSubmitPin = findViewById(R.id.btnSubmitPin);

        setupPinInputs();
        setupConfirmPinInputs();
        setupKeypad();
        btnSubmitPin.setOnClickListener(v -> setPin());
    }

    private void setPin() {
        String pin = buildPin(pinInputs);
        String confirmPin = buildPin(confirmPinInputs);

        if (pin.length() != 4) {
            Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pin.equals(confirmPin)) {
            Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitPin.setEnabled(false);
        FirebaseHelper.setPinForLogin(this, loginId, pin, false, success -> runOnUiThread(() -> {
            btnSubmitPin.setEnabled(true);
            if (success) {
                FirebaseHelper.logUserActivity(this, "pin_set");
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Failed to set PIN", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void setupPinInputs() {
        for (int i = 0; i < pinInputs.length; i++) {
            final int index = i;
            pinInputs[i].setShowSoftInputOnFocus(false);
            pinInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < pinInputs.length - 1) {
                        pinInputs[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupConfirmPinInputs() {
        for (int i = 0; i < confirmPinInputs.length; i++) {
            final int index = i;
            confirmPinInputs[i].setShowSoftInputOnFocus(false);
            confirmPinInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < confirmPinInputs.length - 1) {
                        confirmPinInputs[index + 1].requestFocus();
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
                if (fillNextEmpty(pinInputs, digit)) return;
                fillNextEmpty(confirmPinInputs, digit);
            });
        }
    }

    private boolean fillNextEmpty(TextInputEditText[] inputs, String digit) {
        for (TextInputEditText input : inputs) {
            if (input.getText() == null || input.getText().toString().isEmpty()) {
                input.setText(digit);
                input.requestFocus();
                return true;
            }
        }
        return false;
    }

    private String buildPin(TextInputEditText[] inputs) {
        StringBuilder builder = new StringBuilder();
        for (TextInputEditText input : inputs) {
            builder.append(input.getText() == null ? "" : input.getText().toString());
        }
        return builder.toString().trim();
    }
}
