package com.example.fraudulens.activities;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;

public class DetailsActivity extends AppCompatActivity {

    TextView tvTarget, tvSummary, tvTime;
    Button btnResolve;
    String reportId;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_details);

        tvTarget  = findViewById(R.id.tvDetailTarget);
        tvSummary = findViewById(R.id.tvDetailSummary);
        tvTime    = findViewById(R.id.tvDetailTime);
        btnResolve = findViewById(R.id.btnResolve);

        reportId = getIntent().getStringExtra("reportId");
        if (reportId == null) {
            Toast.makeText(this, "Missing report id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load report
        FirebaseHelper.getReport(reportId, doc -> {
            if (doc == null || !doc.exists()) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Could not load report",
                                Toast.LENGTH_SHORT).show()
                );
                return;
            }

            String target  = doc.getString("message");
            String summary = doc.getString("result");

            long timeMillis = 0;
            Object ts = doc.get("timestamp");
            if (ts instanceof Number) {
                timeMillis = ((Number) ts).longValue();
            }

            long finalTime = timeMillis;
            runOnUiThread(() -> {
                tvTarget.setText(
                        "Target: " + (target == null ? "" :
                                (target.length() > 80
                                        ? target.substring(0, 80) + "…"
                                        : target))
                );
                tvSummary.setText("Summary: " + (summary == null ? "" : summary));

                tvTime.setText(finalTime > 0
                        ? "Reported: " + timeAgo(finalTime)
                        : "Reported: —");
            });
        });

        // ✅ FIXED: callback-based resolve
        btnResolve.setOnClickListener(v -> {
            btnResolve.setEnabled(false);

            FirebaseHelper.resolveReport(reportId, success ->
                    runOnUiThread(() -> {
                        btnResolve.setEnabled(true);

                        if (success) {
                            Toast.makeText(this,
                                    "Report marked as resolved",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this,
                                    "Failed to resolve report",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
            );
        });
    }

    private String timeAgo(long epochMillis) {
        long diff = System.currentTimeMillis() - epochMillis;
        long minutes = diff / 60000;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }
}
