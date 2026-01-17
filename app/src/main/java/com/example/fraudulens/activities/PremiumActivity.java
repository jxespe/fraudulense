package com.example.fraudulens.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fraudulens.R;
import com.google.android.material.button.MaterialButton;

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
        // TODO: Implement payment processing
        // For now, just proceed to main activity
        proceedToMain();
    }

    private void startFreeTrial() {
        // Save free trial status
        SharedPreferences prefs = getSharedPreferences("premium_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("is_premium", true).putLong("trial_end", System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)).apply();
        proceedToMain();
    }

    private void proceedToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("email", email);
        intent.putExtra("phoneNumber", phoneNumber);
        startActivity(intent);
        finishAffinity();
    }
}
