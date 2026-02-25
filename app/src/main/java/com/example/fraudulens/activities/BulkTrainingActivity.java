package com.example.fraudulens.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.adapters.TrainingMessageAdapter;
import com.example.fraudulens.models.Report;
import com.example.fraudulens.utils.ScamDetector;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BulkTrainingActivity extends AppCompatActivity {
    private static final int REQ_READ_SMS = 3001;
    private static final int MAX_INBOX_MESSAGES = 500;

    private TrainingMessageAdapter adapter;
    private TextView tvEmpty;
    private MaterialButton btnAddSelected;
    private MaterialButton btnSelectAll;
    private MaterialButton btnClearSelection;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilters;

    private final List<Report> allMessages = new ArrayList<>();
    private final List<Report> filteredMessages = new ArrayList<>();
    private final Set<String> selectedKeys = new HashSet<>();

    private String selectedCategory = "all";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bulk_training);

        ImageButton btnBack = findViewById(R.id.btnBackNav);
        btnBack.setOnClickListener(v -> onBackPressed());

        tvEmpty = findViewById(R.id.tvEmpty);
        btnAddSelected = findViewById(R.id.btnAddSelected);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnClearSelection = findViewById(R.id.btnClearSelection);
        etSearch = findViewById(R.id.etSearch);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);

        RecyclerView rvMessages = findViewById(R.id.rvMessages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrainingMessageAdapter(
                new ArrayList<>(),
                selectedKeys,
                this::buildKey,
                this::updateAddButton
        );
        rvMessages.setAdapter(adapter);

        setupFilters();
        setupSearch();
        setupActions();
        updateAddButton(selectedKeys.size());

        if (hasSmsPermission()) {
            loadInboxMessages();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, REQ_READ_SMS);
        }
    }

    private void setupFilters() {
        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            selectedCategory = mapChipToCategory(checkedId);
            applyFilters();
        });
        Chip chipAll = findViewById(R.id.chipAll);
        if (chipAll != null) {
            chipAll.setChecked(true);
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim().toLowerCase(Locale.US) : "";
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void setupActions() {
        btnSelectAll.setOnClickListener(v -> {
            for (Report report : filteredMessages) {
                selectedKeys.add(buildKey(report));
            }
            adapter.update(new ArrayList<>(filteredMessages));
            updateAddButton(selectedKeys.size());
        });

        btnClearSelection.setOnClickListener(v -> {
            selectedKeys.clear();
            adapter.update(new ArrayList<>(filteredMessages));
            updateAddButton(0);
        });

        btnAddSelected.setOnClickListener(v -> addSelectedToTraining());
    }

    private void addSelectedToTraining() {
        if (selectedKeys.isEmpty()) {
            Toast.makeText(this, getString(R.string.bulk_training_none_selected), Toast.LENGTH_SHORT).show();
            return;
        }
        int added = 0;
        for (Report report : allMessages) {
            String key = buildKey(report);
            if (!selectedKeys.contains(key)) continue;
            String message = report.getMessage();
            if (message == null || message.trim().isEmpty()) continue;
            FirebaseHelper.addTrainingSample(this, message, true, "bulk_inbox");
            added++;
        }
        FirebaseHelper.logUserActivity(this, "bulk_training_inbox");
        selectedKeys.clear();
        adapter.update(new ArrayList<>(filteredMessages));
        updateAddButton(0);
        Toast.makeText(this, getString(R.string.bulk_training_added, added), Toast.LENGTH_LONG).show();
    }

    private void loadInboxMessages() {
        allMessages.clear();
        Uri uri = Uri.parse("content://sms/inbox");
        String[] projection = new String[]{"address", "body", "date"};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, "date DESC");
        if (cursor != null) {
            try {
                int count = 0;
                while (cursor.moveToNext() && count < MAX_INBOX_MESSAGES) {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                    if (body == null || body.trim().isEmpty()) continue;
                    if (FirebaseHelper.isTrustedMessage(this, address, body)) {
                        continue;
                    }
                    Report report = new Report(
                            "sms",
                            body,
                            getString(R.string.training_inbox_item),
                            new Timestamp(new Date(date)),
                            "sms"
                    );
                    report.setSource(address);
                    allMessages.add(report);
                    count++;
                }
            } finally {
                cursor.close();
            }
        }
        applyFilters();
    }

    private void applyFilters() {
        filteredMessages.clear();
        for (Report report : allMessages) {
            if (!matchesCategory(report, selectedCategory)) continue;
            if (!matchesSearch(report, searchQuery)) continue;
            filteredMessages.add(report);
        }
        adapter.update(new ArrayList<>(filteredMessages));
        tvEmpty.setVisibility(filteredMessages.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean matchesSearch(Report report, String query) {
        if (TextUtils.isEmpty(query)) return true;
        String message = report.getMessage() != null ? report.getMessage().toLowerCase(Locale.US) : "";
        String source = report.getSource() != null ? report.getSource().toLowerCase(Locale.US) : "";
        return message.contains(query) || source.contains(query);
    }

    private boolean matchesCategory(Report report, String category) {
        String message = safeLower(report.getMessage());
        switch (category) {
            case "all":
                return true;
            case "likely":
                return report.getMessage() != null && ScamDetector.isScam(this, report.getMessage());
            case "phishing":
                return containsAny(message,
                        "verify", "verification", "account", "login", "password", "bank",
                        "suspended", "locked", "unusual activity", "confirm", "security alert",
                        "update info", "click link", "sign in", "identity", "authenticate");
            case "delivery":
                return containsAny(message,
                        "delivery", "package", "parcel", "tracking", "courier", "shipping",
                        "dhl", "fedex", "ups", "usps", "lbc", "j&t", "ninja van");
            case "otp":
                return containsAny(message,
                        "otp", "one time", "verification code", "security code",
                        "passcode", "login code", "pin code");
            case "loan":
                return containsAny(message,
                        "loan", "cash advance", "credit", "approved", "lending",
                        "interest", "payday", "installment", "borrow");
            case "crypto":
                return containsAny(message,
                        "crypto", "bitcoin", "btc", "eth", "wallet", "token",
                        "airdrop", "nft", "mining", "blockchain");
            case "ransom":
                return containsAny(message,
                        "ransom", "rans0m", "pay to unlock", "pay to release",
                        "bitcoin", "btc", "decrypt", "decryption", "your files", "locked files");
            case "employment":
                return containsAny(message,
                        "job offer", "job opportunity", "work from home", "remote job",
                        "hiring", "recruiting", "interview", "application fee",
                        "processing fee", "employment", "urgent hiring", "salary offer");
            case "charity":
                return containsAny(message,
                        "donate", "donation", "charity", "fundraiser", "help us",
                        "please help", "support our cause", "give now",
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

    private String mapChipToCategory(int checkedId) {
        if (checkedId == R.id.chipLikely) return "likely";
        if (checkedId == R.id.chipPhishing) return "phishing";
        if (checkedId == R.id.chipDelivery) return "delivery";
        if (checkedId == R.id.chipOtp) return "otp";
        if (checkedId == R.id.chipLoan) return "loan";
        if (checkedId == R.id.chipCrypto) return "crypto";
        if (checkedId == R.id.chipRansom) return "ransom";
        if (checkedId == R.id.chipEmployment) return "employment";
        if (checkedId == R.id.chipCharity) return "charity";
        if (checkedId == R.id.chipInvestment) return "investment";
        if (checkedId == R.id.chipPromo) return "promo";
        return "all";
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

    private void updateAddButton(int selectedCount) {
        btnAddSelected.setText(getString(R.string.bulk_training_add_selected_count, selectedCount));
        btnAddSelected.setEnabled(selectedCount > 0);
    }

    private String buildKey(Report report) {
        long timestamp = report.getTimestamp() != null ? report.getTimestamp().toDate().getTime() : 0L;
        String normalized = FirebaseHelper.normalizePhoneNumber(report.getSource());
        String body = report.getMessage() != null ? report.getMessage().trim() : "";
        return normalized + "|" + timestamp + "|" + body;
    }

    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadInboxMessages();
            } else {
                Toast.makeText(this, getString(R.string.bulk_training_permission_required), Toast.LENGTH_LONG).show();
                tvEmpty.setVisibility(View.VISIBLE);
            }
        }
    }
}
