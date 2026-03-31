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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.fraudulens.R;
import com.example.fraudulens.adapters.ReportAdapter;
import com.example.fraudulens.models.Report;
import com.example.fraudulens.receivers.SmsReceiver;
import com.example.fraudulens.utils.ScamDetector;
import com.example.fraudulens.utils.ScamModelManager;
import com.example.fraudulens.FirebaseHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScamMessagesActivity extends AppCompatActivity {

    private static final int REQ_READ_SMS = 1001;
    /** Inbox rows to scan — full history + ML per SMS on the UI thread causes black screen / ANR on large inboxes. */
    private static final int MAX_INBOX_ROWS = 800;
    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor();

    private RecyclerView rvScamMessages;
    private TextView tvEmpty;
    private ReportAdapter adapter;
    private final List<Report> items = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;

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
            String key = FirebaseHelper.buildScamKey(address, body, date);
            if (FirebaseHelper.isDismissedScam(ScamMessagesActivity.this, key)) {
                return;
            }
            if (FirebaseHelper.isDismissedScamBody(ScamMessagesActivity.this, body)) {
                return;
            }
            if (containsScamKey(key)) {
                return;
            }
            Report r = new Report(
                    "sms",
                    body,
                    "Potential Scam",
                    new Timestamp(new Date(date)),
                    "sms"
            );
            r.setStatus(categorizeMessage(body));
            r.setSource(address);
            items.add(0, r);
            adapter.update(new ArrayList<>(items));
            tvEmpty.setVisibility(items.isEmpty() ? TextView.VISIBLE : TextView.GONE);
            FirebaseHelper.saveDetectedScamMessage(ScamMessagesActivity.this, address, body, date);
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
        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvScamMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(new ArrayList<>(), report -> showMessageDialog(report));
        rvScamMessages.setAdapter(adapter);

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                ScamModelManager.forceRefreshModel(this);
                loadScamMessages();
            });
        }

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
        ScamModelManager.prefetchLatestModel(this);
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }

        final Context appCtx = getApplicationContext();
        loadExecutor.execute(() -> {
            List<Report> merged = new ArrayList<>();
            merged.addAll(FirebaseHelper.getDetectedScamMessages(appCtx));
            for (Report report : merged) {
                if (report == null) continue;
                if (report.getStatus() == null || "sms".equalsIgnoreCase(report.getStatus())) {
                    report.setStatus(categorizeMessage(report.getMessage()));
                }
            }
            Set<String> seen = new HashSet<>();
            for (Report report : merged) {
                long timestamp = report.getTimestamp() != null ? report.getTimestamp().toDate().getTime() : 0L;
                seen.add(FirebaseHelper.buildScamKey(report.getSource(), report.getMessage(), timestamp));
            }

            boolean smsDenied = false;
            boolean smsQueryFailed = false;
            try {
                Uri uri = Uri.parse("content://sms/inbox");
                String[] projection = new String[]{"address", "body", "date"};
                Cursor cursor = appCtx.getContentResolver().query(uri, projection, null, null, "date DESC");
                if (cursor != null) {
                    try {
                        int rows = 0;
                        while (cursor.moveToNext() && rows < MAX_INBOX_ROWS) {
                            rows++;
                            String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                            String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                            long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                            if (FirebaseHelper.isTrustedMessage(appCtx, address, body)) {
                                continue;
                            }
                            // Bulk inbox scan: keywords + heuristic only (no TFLite per row — avoids ANR / black screen).
                            // Real-time SMS still uses full on-device ML in SmsReceiver.
                            if (isLikelyScamByKeywords(body) || ScamDetector.isScam(body)) {
                                String key = FirebaseHelper.buildScamKey(address, body, date);
                                if (FirebaseHelper.isDismissedScam(appCtx, key)) {
                                    continue;
                                }
                                if (FirebaseHelper.isDismissedScamBody(appCtx, body)) {
                                    continue;
                                }
                                if (seen.contains(key)) {
                                    continue;
                                }
                                Report r = new Report(
                                        "sms",
                                        body,
                                        "Potential Scam",
                                        new Timestamp(new Date(date)),
                                        "sms"
                                );
                                r.setStatus(categorizeMessage(body));
                                r.setSource(address);
                                merged.add(r);
                                seen.add(key);
                                FirebaseHelper.saveDetectedScamMessage(appCtx, address, body, date);
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            } catch (SecurityException se) {
                smsDenied = true;
            } catch (Exception e) {
                smsQueryFailed = true;
            }

            Collections.sort(merged, (a, b) -> {
                long ta = a.getTimestamp() != null ? a.getTimestamp().toDate().getTime() : 0L;
                long tb = b.getTimestamp() != null ? b.getTimestamp().toDate().getTime() : 0L;
                return Long.compare(tb, ta);
            });

            final List<Report> toShow = new ArrayList<>(merged);
            final boolean denied = smsDenied;
            final boolean failed = smsQueryFailed;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                items.clear();
                items.addAll(toShow);
                adapter.update(new ArrayList<>(items));
                tvEmpty.setVisibility(items.isEmpty() ? TextView.VISIBLE : TextView.GONE);
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                if (denied) {
                    Toast.makeText(ScamMessagesActivity.this, "SMS access blocked on this device.", Toast.LENGTH_LONG).show();
                } else if (failed) {
                    Toast.makeText(ScamMessagesActivity.this, "Unable to load SMS on this device.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        loadExecutor.shutdown();
        super.onDestroy();
    }

    private boolean containsScamKey(String key) {
        for (Report report : items) {
            long timestamp = report.getTimestamp() != null ? report.getTimestamp().toDate().getTime() : 0L;
            if (FirebaseHelper.buildScamKey(report.getSource(), report.getMessage(), timestamp).equals(key)) {
                return true;
            }
        }
        return false;
    }

    private void showMessageDialog(Report report) {
        if (report == null) return;
        String source = report.getSource() != null ? report.getSource() : "Unknown";
        String message = report.getMessage() != null ? report.getMessage() : "";
        boolean isUserReport = report.getUserId() != null
                && !"sms".equalsIgnoreCase(report.getUserId());
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("From: " + source)
                .setMessage(message)
                .setNeutralButton(getString(R.string.close), null);
        if (!isUserReport) {
            builder.setPositiveButton(getString(R.string.report_scam), (d, w) -> {
                FirebaseHelper.addTrainingSample(this, message, true, "sms_feedback");
                FirebaseHelper.logUserActivity(this, "sms_marked_scam");
                String reportUserId = getReportUserId();
                Report reportEntry = new Report(
                        reportUserId,
                        message,
                        "Reported Scam",
                        new Timestamp(new Date()),
                        "open"
                );
                reportEntry.setSource(source);
                FirebaseHelper.addReport(reportEntry.toMap(), ok -> runOnUiThread(() -> {
                    if (ok) {
                        Toast.makeText(this, getString(R.string.feedback_marked_scam), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.report_submit_failed, Toast.LENGTH_LONG).show();
                    }
                }));
            });
            builder.setNegativeButton(getString(R.string.mark_as_safe), (d, w) -> {
                FirebaseHelper.addTrainingSample(this, message, false, "sms_feedback");
                FirebaseHelper.logUserActivity(this, "sms_marked_safe");
                long dateMillis = report.getTimestamp() != null ? report.getTimestamp().toDate().getTime() : 0L;
                String key = FirebaseHelper.buildScamKey(report.getSource(), report.getMessage(), dateMillis);
                FirebaseHelper.dismissScamMessageByBody(this, message);
                FirebaseHelper.dismissScamMessage(this, key);
                String normalized = safeLower(message);
                items.removeIf(item ->
                        safeLower(item.getMessage()).equals(normalized)
                );
                adapter.update(new ArrayList<>(items));
                tvEmpty.setVisibility(items.isEmpty() ? TextView.VISIBLE : TextView.GONE);
                Toast.makeText(this, getString(R.string.feedback_marked_safe), Toast.LENGTH_SHORT).show();
            });
        }
        builder.show();
    }

    private String categorizeMessage(String message) {
        String text = safeLower(message);
        if (containsAny(text, "ransom", "rans0m", "decrypt", "decryption", "your files", "locked files",
                "pay to unlock", "pay to release", "bitcoin", "btc", "crypto", "compensation", "pay now",
                "private photos", "hacked through your camera", "recorded you", "adult sites",
                "company data has been encrypted", "downloaded your contact list", "we know your home address",
                "countdown", "proof attached", "planted malware", "reputation will be destroyed")) {
            return "Ransom Scam";
        }
        if (containsAny(text, "job offer", "job opportunity", "work from home", "remote job",
                "hiring", "recruiting", "interview", "application fee", "processing fee",
                "employment", "urgent hiring", "salary offer", "no experience", "start today",
                "send id", "bank details", "training fee", "limited slots", "personal details",
                "starter equipment", "approved vendor", "ssn", "pre-employment verification",
                "reshipping packages", "shortlisted without an interview")) {
            return "Employment Scam";
        }
        if (containsAny(text, "donate", "donation", "charity", "fundraiser", "help us",
                "please help", "support our cause", "give now", "urgent help", "relief fund", "begging",
                "emergency relief", "critical condition", "quick transfer link", "save lives",
                "family tragedy", "sent directly to this wallet", "war victims", "sponsor an orphan",
                "urgent surgery", "disaster recovery")) {
            return "Fake Charity Scam";
        }
        if (containsAny(text, "investment", "invest", "guaranteed return", "high returns",
                "profit", "double your money", "crypto", "forex", "trading",
                "investment plan", "risk free", "300% return", "private crypto signal group",
                "insider trading", "ai trading bot", "withdrawals only after upgrading account",
                "vip investment window", "passive income guaranteed", "proof of earnings attached")) {
            return "Investment Scam";
        }
        if (containsAny(text, "promo", "promotion", "discount", "free", "offer",
                "limited time", "exclusive deal", "claim now", "voucher", "coupon", "giveaway",
                "won a cash prize", "reward before it expires", "promo winner", "loyalty bonus",
                "final notice", "unclaimed prize", "pay shipping fee", "won a raffle",
                "free gadget", "promotional payout", "spin & win")) {
            return "Promo Scam";
        }
        return "Scam";
    }

    private boolean isLikelyScamByKeywords(String message) {
        String text = safeLower(message);
        boolean hasSuspiciousLink = containsAny(text,
                "http://", "https://", "bit.ly", "tinyurl", "t.co", "goo.gl", "ow.ly", "rb.gy");
        boolean keywordHit = containsAny(text,
                // Ransom
                "ransom", "rans0m", "compensation", "pay to unlock", "pay to release",
                "pay now", "bitcoin", "btc", "crypto", "decrypt", "decryption",
                "your files", "locked files", "private photos", "hacked through your camera",
                "recorded you", "adult sites", "company data has been encrypted",
                "downloaded your contact list", "we know your home address", "countdown",
                "proof attached", "planted malware", "reputation will be destroyed",
                // Employment
                "job offer", "job opportunity", "work from home", "remote job",
                "hiring", "recruiting", "interview", "application fee",
                "processing fee", "employment", "urgent hiring", "salary offer",
                "no experience", "start today", "send id", "bank details",
                "training fee", "limited slots", "personal details",
                "starter equipment", "approved vendor", "ssn", "pre-employment verification",
                "reshipping packages", "shortlisted without an interview",
                // Charity
                "donate", "donation", "charity", "fundraiser", "help us",
                "please help", "begging", "support our cause", "give now",
                "urgent help", "relief fund", "emergency relief", "critical condition",
                "quick transfer link", "save lives", "family tragedy",
                "sent directly to this wallet", "war victims", "sponsor an orphan",
                "urgent surgery", "disaster recovery",
                // Investment
                "investment", "invest", "guaranteed return", "high returns",
                "profit", "double your money", "forex", "trading",
                "investment plan", "risk free", "300% return",
                "private crypto signal group", "insider trading",
                "ai trading bot", "withdrawals only after upgrading account",
                "vip investment window", "passive income guaranteed",
                "proof of earnings attached",
                // Promo
                "promo", "promotion", "discount", "free", "offer",
                "limited time", "exclusive deal", "claim now",
                "voucher", "coupon", "giveaway", "won a cash prize",
                "reward before it expires", "promo winner", "loyalty bonus",
                "final notice", "unclaimed prize", "pay shipping fee",
                "won a raffle", "free gadget", "promotional payout",
                "spin & win",
                // Urgency / fear / threats
                "urgent", "act now", "immediately", "last chance", "final notice",
                "account closure", "legal action", "suspended", "locked",
                "verify now", "avoid consequences", "threat", "warning",
                // Impersonation
                "bank", "government", "irs", "tax", "police", "customs",
                "courier", "dhl", "fedex", "ups", "amazon", "paypal",
                "support team", "executive", "ceo", "director",
                // Sensitive info requests
                "password", "one-time passcode", "otp", "verification code",
                "bank details", "credit card", "cvv", "pin code", "ssn", "id number",
                // Unusual payment methods
                "gift card", "wire transfer", "cryptocurrency", "wallet address",
                // Secrecy / pressure
                "do not share", "keep this private", "confidential", "secret",
                // Generic greetings / awkward phrasing
                "dear customer", "dear user", "hello friend", "valued customer",
                "kindly", "click here");
        boolean attachmentHit = containsAny(text,
                "attachment", "attached file", "download", "open the file", "see attachment");
        return keywordHit || hasSuspiciousLink || attachmentHit;
    }

    private String safeLower(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.US).replaceAll("\\s+", " ");
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isEmpty()) return false;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String getReportUserId() {
        String email = FirebaseHelper.getLoggedInEmail(this);
        if (email != null && !email.trim().isEmpty()) {
            return email.trim().toLowerCase(Locale.US);
        }
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser != null && authUser.getUid() != null && !authUser.getUid().trim().isEmpty()) {
            return authUser.getUid();
        }
        return "anonymous";
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            ContextCompat.registerReceiver(
                    this,
                    scamReceiver,
                    new IntentFilter(SmsReceiver.ACTION_SCAM_SMS),
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
        } catch (Exception e) {
            registerReceiver(scamReceiver, new IntentFilter(SmsReceiver.ACTION_SCAM_SMS));
        }
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
