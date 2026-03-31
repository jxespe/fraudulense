package com.example.fraudulens.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fraudulens.R;
import com.example.fraudulens.FirebaseHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.Locale;

public class DetailsActivity extends AppCompatActivity {

    TextView tvTarget, tvSummary, tvTime, tvReporter, tvSource, tvMessage, tvImageText;
    ImageView imgDetail;
    Button btnResolve;
    String reportId;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_details);

        tvTarget = findViewById(R.id.tvDetailTarget);
        tvSummary = findViewById(R.id.tvDetailSummary);
        tvTime = findViewById(R.id.tvDetailTime);
        imgDetail = findViewById(R.id.imgDetail);
        tvReporter = findViewById(R.id.tvDetailReporter);
        tvSource = findViewById(R.id.tvDetailSource);
        tvMessage = findViewById(R.id.tvDetailMessage);
        tvImageText = findViewById(R.id.tvDetailImageText);
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

            String message = doc.getString("message");
            String summary = doc.getString("result");
            String imageUrl = doc.getString("imageUrl");
            String imageText = doc.getString("imageText");
            String source = doc.getString("source");
            String userId = doc.getString("userId");

            long timeMillis = 0;
            Object ts = doc.get("timestamp");
            if (ts instanceof Number) {
                timeMillis = ((Number) ts).longValue();
            }

            long finalTime = timeMillis;
            runOnUiThread(() -> {
                String safeSource = source != null && !source.trim().isEmpty() ? source : "Unknown";
                tvTarget.setText("Target: " + safeSource);
                tvSummary.setText("Summary: " + (summary == null ? "" : summary));
                tvMessage.setText("Message: " + (message == null ? "" : message));

                if (source != null && !source.trim().isEmpty()) {
                    tvSource.setVisibility(View.VISIBLE);
                    tvSource.setText("From: " + source);
                } else {
                    tvSource.setVisibility(View.GONE);
                }

                tvTime.setText(finalTime > 0
                        ? "Reported: " + timeAgo(finalTime)
                        : "Reported: —");

                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    imgDetail.setVisibility(View.VISIBLE);
                    Picasso.get().load(imageUrl).into(imgDetail);
                } else {
                    imgDetail.setVisibility(View.GONE);
                }

                if (imageText != null && !imageText.trim().isEmpty()) {
                    tvImageText.setVisibility(View.VISIBLE);
                    tvImageText.setText("Image text: " + imageText.trim());
                } else {
                    tvImageText.setVisibility(View.GONE);
                }

                bindReporterName(userId);
            });
        });

        btnResolve.setOnClickListener(v -> finish());
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

    private void bindReporterName(String userId) {
        if (tvReporter == null) return;
        if (userId == null || userId.trim().isEmpty() || "anonymous".equalsIgnoreCase(userId)) {
            tvReporter.setVisibility(View.GONE);
            return;
        }
        if (userId.contains("@")) {
            String local = userId.split("@")[0].trim();
            String normalized = local.replace(".", " ")
                    .replace("_", " ")
                    .replace("-", " ");
            String first = extractFirstName(normalized);
            if (!first.isEmpty()) {
                tvReporter.setVisibility(View.VISIBLE);
                tvReporter.setText("Reported by: " + first);
            } else {
                tvReporter.setVisibility(View.GONE);
            }
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) return;
                    String name = doc.getString("name");
                    if (name == null || name.trim().isEmpty()) {
                        name = doc.getString("fullName");
                    }
                    if (name == null || name.trim().isEmpty()) {
                        String first = doc.getString("firstName");
                        String last = doc.getString("lastName");
                        if (first != null && !first.trim().isEmpty()) {
                            name = first.trim() + (last != null && !last.trim().isEmpty() ? " " + last.trim() : "");
                        }
                    }
                    if (name == null || name.trim().isEmpty()) {
                        name = doc.getString("username");
                    }
                    if (name != null && !name.trim().isEmpty()) {
                        tvReporter.setVisibility(View.VISIBLE);
                        tvReporter.setText("Reported by: " + extractFirstName(name));
                    }
                });
    }

    private String extractFirstName(String name) {
        if (name == null) return "";
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "";
        String[] parts = trimmed.split("\\s+");
        String first = parts[0];
        if (first.isEmpty()) return "";
        return first.substring(0, 1).toUpperCase(Locale.getDefault()) + first.substring(1);
    }
}
