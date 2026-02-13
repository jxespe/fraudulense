package com.example.fraudulens.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.app.AlertDialog;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.adapters.ReportAdapter;
import com.example.fraudulens.models.Report;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FilteredReportsActivity extends AppCompatActivity {
    public static final String EXTRA_CATEGORY = "filter_category";
    private static final int REQ_READ_SMS = 2001;

    private ReportAdapter adapter;
    private TextView tvEmpty;
    private String category;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtered_reports);

        category = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (category == null) category = "";

        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderTitle.setText(getTitleForCategory(category));

        ImageButton btnBack = findViewById(R.id.btnBackNav);
        btnBack.setOnClickListener(v -> onBackPressed());

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView rvReports = findViewById(R.id.rvReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(new ArrayList<>(), this::showMessageDialog);
        rvReports.setAdapter(adapter);

        if (hasSmsPermission()) {
            loadFilteredMessages();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, REQ_READ_SMS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFilteredMessages();
            } else {
                Toast.makeText(this, "SMS permission is required to show messages.", Toast.LENGTH_LONG).show();
                tvEmpty.setVisibility(TextView.VISIBLE);
            }
        }
    }

    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void loadFilteredMessages() {
        List<Report> items = new ArrayList<>();
        Uri uri = Uri.parse("content://sms/inbox");
        String[] projection = new String[]{"address", "body", "date"};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, "date DESC");
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                    if (FirebaseHelper.isTrustedMessage(this, address, body)) {
                        continue;
                    }
                    Report r = new Report(
                            "sms",
                            body,
                            "Potential Scam",
                            new Timestamp(new Date(date)),
                            "sms"
                    );
                    r.setSource(address);
                    if (matchesCategory(r, category)) {
                        items.add(r);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        adapter.update(items);
        tvEmpty.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private String getTitleForCategory(String category) {
        switch (category.toLowerCase(Locale.US)) {
            case "ransom":
                return "Ransom Scams";
            case "employment":
                return "Employment Scams";
            case "charity":
                return "Fake Charity Scams";
            case "investment":
                return "Investment Scams";
            case "promo":
                return "Promo Scams";
            default:
                return getString(R.string.scam_filter_title);
        }
    }

    private boolean matchesCategory(Report r, String category) {
        String message = safeLower(r.getMessage());
        switch (category.toLowerCase(Locale.US)) {
            case "ransom":
                return containsAny(message,
                        "ransom", "rans0m", "compensation", "pay to unlock",
                        "pay to release", "pay now", "bitcoin", "btc", "crypto",
                        "decrypt", "decryption", "your files", "locked files");
            case "employment":
                return containsAny(message,
                        "job offer", "job opportunity", "work from home", "remote job",
                        "hiring", "recruiting", "interview", "application fee",
                        "processing fee", "employment", "urgent hiring", "salary offer");
            case "charity":
                return containsAny(message,
                        "donate", "donation", "charity", "fundraiser", "help us",
                        "please help", "begging", "support our cause", "give now",
                        "urgent help", "relief fund");
            case "investment":
                return containsAny(message,
                        "investment", "invest", "guaranteed return", "high returns",
                        "profit", "double your money", "crypto", "forex", "trading",
                        "investment plan", "risk free");
            case "promo":
                return containsAny(message,
                        "promo", "promotion", "discount", "free", "offer",
                        "limited time", "exclusive deal", "claim now",
                        "voucher", "coupon", "giveaway");
            default:
                return true;
        }
    }

    private String safeLower(String value) {
        if (TextUtils.isEmpty(value)) return "";
        return value.toLowerCase(Locale.US);
    }

    private boolean containsAny(String text, String... keywords) {
        if (TextUtils.isEmpty(text)) return false;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void showMessageDialog(Report report) {
        if (report == null) return;
        String source = report.getSource() != null ? report.getSource() : "Unknown";
        String message = report.getMessage() != null ? report.getMessage() : "";
        new AlertDialog.Builder(this)
                .setTitle("From: " + source)
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show();
    }
}
