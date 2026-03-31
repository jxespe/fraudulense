package com.example.fraudulens.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.example.fraudulens.R;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import android.text.Editable;
import android.text.TextWatcher;

public class PremiumActivity extends AppCompatActivity {

    private MaterialButton btn1Month, btn6Months, btn1Year, btnFreeTrial;
    private TextView tvRestore, tvTerms;
    private String email, phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

        email = getIntent().getStringExtra("email");
        phoneNumber = getIntent().getStringExtra("phoneNumber");

        btn1Month = findViewById(R.id.btn1Month);
        btn6Months = findViewById(R.id.btn6Months);
        btn1Year = findViewById(R.id.btn1Year);
        btnFreeTrial = findViewById(R.id.btnFreeTrial);
        tvRestore = findViewById(R.id.tvRestore);
        tvTerms = findViewById(R.id.tvTerms);

        btn1Month.setOnClickListener(v -> selectPlan("1_month", 349.00));
        btn6Months.setOnClickListener(v -> selectPlan("6_months", 999.00));
        btn1Year.setOnClickListener(v -> selectPlan("1_year", 1599.00));
        btnFreeTrial.setOnClickListener(v -> startFreeTrial());
        tvRestore.setOnClickListener(v -> {
            Toast.makeText(this, "Restore Purchase functionality coming soon", Toast.LENGTH_SHORT).show();
        });
        tvTerms.setOnClickListener(v -> {
            Toast.makeText(this, "Terms & Privacy Policy", Toast.LENGTH_SHORT).show();
        });
    }

    private void selectPlan(String planType, double price) {
        startPaymentFlow(planType, price);
    }

    private void startFreeTrial() {
        // Save free trial status
        SharedPreferences prefs = getSharedPreferences("premium_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("is_premium", true).putLong("trial_end", System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)).apply();
        proceedToMain();
    }

    private void startPaymentFlow(String planType, double price) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_method, null, false);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnPayGcash).setOnClickListener(v -> {
            dialog.dismiss();
            showPaymentForm("GCash", planType, price);
        });
        dialogView.findViewById(R.id.btnPayCard).setOnClickListener(v -> {
            dialog.dismiss();
            showPaymentForm("Card", planType, price);
        });
        dialogView.findViewById(R.id.btnPayMaya).setOnClickListener(v -> {
            dialog.dismiss();
            showPaymentForm("Maya", planType, price);
        });
        dialogView.findViewById(R.id.btnPayBank).setOnClickListener(v -> {
            dialog.dismiss();
            showPaymentForm("Bank", planType, price);
        });

        dialog.show();
    }

    private void showPaymentForm(String method, String planType, double price) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_form, null, false);
        TextView tvTitle = dialogView.findViewById(R.id.tvPaymentTitle);
        EditText etPhone = dialogView.findViewById(R.id.etPhone);
        EditText etCardNumber = dialogView.findViewById(R.id.etCardNumber);
        EditText etExpiry = dialogView.findViewById(R.id.etExpiry);
        EditText etCvv = dialogView.findViewById(R.id.etCvv);
        EditText etBankAccount = dialogView.findViewById(R.id.etBankAccount);
        View layoutCardRow = dialogView.findViewById(R.id.layoutCardRow);

        tvTitle.setText(method + " details");
        etPhone.setVisibility(View.GONE);
        etCardNumber.setVisibility(View.GONE);
        layoutCardRow.setVisibility(View.GONE);
        etBankAccount.setVisibility(View.GONE);

        if ("GCash".equals(method) || "Maya".equals(method)) {
            etPhone.setVisibility(View.VISIBLE);
        } else if ("Card".equals(method)) {
            etCardNumber.setVisibility(View.VISIBLE);
            layoutCardRow.setVisibility(View.VISIBLE);
            attachCardFormatter(etCardNumber);
        } else if ("Bank".equals(method)) {
            etBankAccount.setVisibility(View.VISIBLE);
        }

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Continue", (d, w) -> {
                    Map<String, String> payload = new HashMap<>();
                    payload.put("method", method);
                    if ("GCash".equals(method) || "Maya".equals(method)) {
                        String phone = valueOf(etPhone);
                        if (phone.isEmpty()) {
                            toast("Please enter a phone number.");
                            return;
                        }
                        payload.put("phone", phone);
                    } else if ("Card".equals(method)) {
                        String card = valueOf(etCardNumber);
                        String cardDigits = card.replaceAll("\\s+", "");
                        String expiry = valueOf(etExpiry);
                        String cvv = valueOf(etCvv);
                        if (cardDigits.isEmpty()) {
                            toast("Please enter card number.");
                            return;
                        }
                        if (!cardDigits.matches("\\d{16}")) {
                            toast("Card number must be 16 digits.");
                            return;
                        }
                        if (!isExpiryValid(expiry)) {
                            toast("Expiration date is invalid or expired.");
                            return;
                        }
                        if (!cvv.matches("\\d{3}")) {
                            toast("CVV must be 3 digits.");
                            return;
                        }
                        payload.put("card", formatCardDisplay(cardDigits));
                        payload.put("expiry", expiry);
                        payload.put("cvv", cvv);
                    } else if ("Bank".equals(method)) {
                        String acct = valueOf(etBankAccount);
                        if (!acct.matches("\\d{9}")) {
                            toast("Bank account must be 9 digits.");
                            return;
                        }
                        payload.put("bankAccount", acct);
                    }
                    showConfirmation(planType, price, payload);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showConfirmation(String planType, double price, Map<String, String> payload) {
        String formattedPrice = formatPrice(price);
        StringBuilder msg = new StringBuilder();
        msg.append("Plan: ").append(planType.replace("_", " ")).append("\n");
        msg.append("Amount: ").append(formattedPrice).append("\n\n");
        msg.append("Payment method: ").append(payload.get("method")).append("\n");
        if (payload.containsKey("phone")) {
            msg.append("Phone: ").append(payload.get("phone")).append("\n");
        }
        if (payload.containsKey("card")) {
            msg.append("Card: ").append(payload.get("card")).append("\n");
            msg.append("Expiry: ").append(payload.get("expiry")).append("\n");
        }
        if (payload.containsKey("bankAccount")) {
            msg.append("Bank account: ").append(payload.get("bankAccount")).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Subscription")
                .setMessage(msg.toString())
                .setPositiveButton("Confirm", (d, w) -> {
                    savePremiumStatus(planType, price, payload.get("method"));
                    showSuccess(planType, formattedPrice);
                })
                .setNegativeButton("Back", null)
                .show();
    }

    private void showSuccess(String planType, String formattedPrice) {
        String benefits = "• Real-time scam alerts\n"
                + "• Community insights\n"
                + "• Priority scam detection\n"
                + "• Advanced reporting";
        String message = "Subscription successful!\n\n"
                + "Plan: " + planType.replace("_", " ") + "\n"
                + "Amount: " + formattedPrice + "\n\n"
                + "Benefits:\n" + benefits;

        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton("Continue", (d, w) -> proceedToMain())
                .show();
    }

    private void savePremiumStatus(String planType, double price, String method) {
        SharedPreferences prefs = getSharedPreferences("premium_prefs", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("is_premium", true)
                .putString("plan_type", planType)
                .putString("payment_method", method)
                .putFloat("price", (float) price)
                .putLong("premium_start", System.currentTimeMillis())
                .apply();
    }

    private boolean isExpiryValid(String expiry) {
        if (expiry == null) return false;
        String trimmed = expiry.trim();
        if (!trimmed.matches("\\d{2}/\\d{2}")) return false;
        String[] parts = trimmed.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]) + 2000;
        if (month < 1 || month > 12) return false;
        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        int currentMonth = now.get(Calendar.MONTH) + 1;
        if (year < currentYear) return false;
        if (year == currentYear && month < currentMonth) return false;
        return true;
    }

    private String valueOf(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private String formatPrice(double price) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        return format.format(price);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void attachCardFormatter(EditText etCardNumber) {
        if (etCardNumber == null) return;
        etCardNumber.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                String digits = s.toString().replaceAll("\\D", "");
                if (digits.length() > 16) {
                    digits = digits.substring(0, 16);
                }
                String formatted = formatCardDisplay(digits);
                s.replace(0, s.length(), formatted);
                isFormatting = false;
            }
        });
    }

    private String formatCardDisplay(String digits) {
        if (digits == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0) sb.append(" ");
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    private void proceedToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("email", email);
        intent.putExtra("phoneNumber", phoneNumber);
        startActivity(intent);
        finishAffinity();
    }
}
