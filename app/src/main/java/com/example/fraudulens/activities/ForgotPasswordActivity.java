package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.utils.FirebaseUtils;
import com.example.fraudulens.utils.PhoneFormatUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etForgotPhone;
    private MaterialButton btnSendReset;
    private Spinner spForgotCountryCode;
    private boolean isFormatting;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_forgot_password);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ImageButton btnBackNav = findViewById(R.id.btnBackNav);
        if (btnBackNav != null) {
            btnBackNav.setOnClickListener(v -> onBackPressed());
        }

        etForgotPhone = findViewById(R.id.etForgotPhone);
        btnSendReset = findViewById(R.id.btnSendReset);
        spForgotCountryCode = findViewById(R.id.spForgotCountryCode);

        if (spForgotCountryCode != null) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.country_codes,
                    R.layout.item_country_code_spinner
            );
            adapter.setDropDownViewResource(R.layout.item_country_code_spinner_dropdown);
            spForgotCountryCode.setAdapter(adapter);
            int defaultIndex = adapter.getPosition("+63");
            if (defaultIndex >= 0) {
                spForgotCountryCode.setSelection(defaultIndex);
            }
        }

        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);
        if (tvBackToLogin != null) {
            tvBackToLogin.setOnClickListener(v -> {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
        }

        if (etForgotPhone != null) {
            etForgotPhone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
            etForgotPhone.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence seq, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence seq, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable seq) {
                    if (isFormatting) return;
                    isFormatting = true;
                    String digits = PhoneFormatUtil.digitsOnly(seq.toString());
                    if (digits.length() > 10) {
                        digits = digits.substring(0, 10);
                    }
                    String formatted = formatInput(digits);
                    etForgotPhone.setText(formatted);
                    etForgotPhone.setSelection(formatted.length());
                    isFormatting = false;
                }
            });
        }

        btnSendReset.setOnClickListener(v -> handleReset());
    }

    /** Same grouping as {@link PhoneVerificationActivity} (registration). */
    private String formatInput(String digits) {
        if (digits.length() <= 3) return digits;
        if (digits.length() <= 6) return digits.substring(0, 3) + " " + digits.substring(3);
        return digits.substring(0, 3) + " " + digits.substring(3, 6) + " " + digits.substring(6);
    }

    private void handleReset() {
        if (etForgotPhone == null) return;
        String phoneInput = etForgotPhone.getText() != null ? etForgotPhone.getText().toString().trim() : "";

        if (phoneInput.isEmpty()) {
            etForgotPhone.setError("Please enter your phone number");
            return;
        }

        String local10 = PhoneFormatUtil.toLocal10(phoneInput);
        if (local10.length() != 10) {
            etForgotPhone.setError("Please enter a valid 10-digit phone number");
            return;
        }

        String selectedCode = "+63";
        if (spForgotCountryCode != null && spForgotCountryCode.getSelectedItem() != null) {
            selectedCode = spForgotCountryCode.getSelectedItem().toString();
        }
        String fullPhone = PhoneFormatUtil.toE164(local10, selectedCode);

        btnSendReset.setEnabled(false);
        btnSendReset.setText("Sending OTP...");

        FirebaseHelper.findPasswordAccountByPhone(fullPhone, account -> runOnUiThread(() -> {
            if (account == null) {
                btnSendReset.setEnabled(true);
                btnSendReset.setText(getString(R.string.continue_label));
                Toast.makeText(this, "No password account found for this phone number", Toast.LENGTH_LONG).show();
                return;
            }

            String accountPhone = account.get("phoneNumber");
            String targetPhone = accountPhone != null && !accountPhone.trim().isEmpty() ? accountPhone : fullPhone;

            // Clear any Firebase Auth session so phone verification always uses signIn, not linkWithCredential.
            FirebaseAuth.getInstance().signOut();

            FirebaseUtils.sendVerificationCode(targetPhone, this, verificationId -> runOnUiThread(() -> {
                btnSendReset.setEnabled(true);
                btnSendReset.setText(getString(R.string.continue_label));
                if (verificationId == null || verificationId.trim().isEmpty()) {
                    Toast.makeText(this, "Failed to send OTP. Please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent i = new Intent(this, ResetPasswordOtpActivity.class);
                i.putExtra("verificationId", verificationId);
                i.putExtra("phoneNumber", targetPhone);
                i.putExtra(ResetPasswordActivity.EXTRA_EMAIL, account.get("email"));
                i.putExtra(ResetPasswordActivity.EXTRA_USER_ID, account.get("userId"));
                startActivity(i);
            }));

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing() && !btnSendReset.isEnabled()) {
                    btnSendReset.setEnabled(true);
                    btnSendReset.setText(getString(R.string.continue_label));
                }
            }, 65000);
        }));
    }
}
