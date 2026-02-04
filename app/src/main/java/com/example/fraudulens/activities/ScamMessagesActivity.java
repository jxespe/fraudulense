package com.example.fraudulens.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.R;
import com.example.fraudulens.adapters.ReportAdapter;
import com.example.fraudulens.models.Report;
import com.example.fraudulens.receivers.SmsReceiver;
import com.example.fraudulens.utils.ScamDetector;
import com.example.fraudulens.FirebaseHelper;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ScamMessagesActivity extends AppCompatActivity {

    private static final int REQ_READ_SMS = 1001;

    private RecyclerView rvScamMessages;
    private TextView tvEmpty;
    private ReportAdapter adapter;
    private final List<Report> items = new ArrayList<>();

    private final BroadcastReceiver scamReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!SmsReceiver.ACTION_SCAM_SMS.equals(intent.getAction())) {
                return;
            }
            String body = intent.getStringExtra(SmsReceiver.EXTRA_BODY);
            String address = intent.getStringExtra(SmsReceiver.EXTRA_ADDRESS);
            long date = intent.getLongExtra(SmsReceiver.EXTRA_DATE, System.currentTimeMillis());
            if (body == null || body.trim().isEmpty()) return;
            if (FirebaseHelper.isTrustedMessage(ScamMessagesActivity.this, address, body)) {
                return;
            }
            Report r = new Report(
                    "sms",
                    body,
                    "Potential Scam",
                    new Timestamp(new Date(date)),
                    "sms"
            );
            r.setSource(address);
            items.add(0, r);
            adapter.update(new ArrayList<>(items));
            tvEmpty.setVisibility(items.isEmpty() ? TextView.VISIBLE : TextView.GONE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scam_messages);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvScamMessages = findViewById(R.id.rvScamMessages);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvScamMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(new ArrayList<>(), report -> showMessageDialog(report));
        rvScamMessages.setAdapter(adapter);

        if (hasSmsPermission()) {
            loadScamMessages();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, REQ_READ_SMS);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void loadScamMessages() {
        items.clear();
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
                    if (ScamDetector.isScam(this, body)) {
                        Report r = new Report(
                                "sms",
                                body,
                                "Potential Scam",
                                new Timestamp(new Date(date)),
                                "sms"
                        );
                        r.setSource(address);
                        items.add(r);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        adapter.update(new ArrayList<>(items));
        tvEmpty.setVisibility(items.isEmpty() ? TextView.VISIBLE : TextView.GONE);
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

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(scamReceiver, new IntentFilter(SmsReceiver.ACTION_SCAM_SMS));
        FirebaseHelper.setLastSeenScamTimestamp(this, System.currentTimeMillis());
        FirebaseHelper.logUserActivity(this, "view_scam_alerts");
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(scamReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadScamMessages();
            } else {
                Toast.makeText(this, "SMS permission is required to show scam alerts.", Toast.LENGTH_LONG).show();
                tvEmpty.setVisibility(TextView.VISIBLE);
            }
        }
    }
}
