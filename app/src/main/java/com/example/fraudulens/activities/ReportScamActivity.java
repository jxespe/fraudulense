package com.example.fraudulens.activities;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.models.Report;
import com.google.firebase.Timestamp;

public class ReportScamActivity extends AppCompatActivity {

    private EditText etReportDescription;
    private CheckBox cbAnonymous;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_scam);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etReportDescription = findViewById(R.id.etReportDescription);
        cbAnonymous = findViewById(R.id.cbAnonymous);

        findViewById(R.id.btnSubmitReport).setOnClickListener(v -> submitReport());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void submitReport() {
        String description = etReportDescription.getText().toString().trim();
        if (description.isEmpty()) {
            etReportDescription.setError("Please provide a description");
            return;
        }

        String userId = cbAnonymous.isChecked()
                ? "anonymous"
                : (FirebaseHelper.getLoggedInEmail(this) != null ? FirebaseHelper.getLoggedInEmail(this) : "anonymous");

        Report report = new Report(
                userId,
                description,
                "Reported Scam",
                new Timestamp(new java.util.Date()),
                "open"
        );

        FirebaseHelper.addReport(report.toMap(), success -> runOnUiThread(() -> {
            if (success) {
                FirebaseHelper.logUserActivity(this, "report_scam_submitted");
                Toast.makeText(this, "✅ Report submitted successfully!", Toast.LENGTH_SHORT).show();
                etReportDescription.setText("");
                cbAnonymous.setChecked(false);
            } else {
                Toast.makeText(this, "❌ Failed to submit. Try again.", Toast.LENGTH_SHORT).show();
            }
        }));
    }
}
