package com.example.fraudulens.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.activities.FilteredReportsActivity;
import com.example.fraudulens.adapters.ReportAdapter;
import com.example.fraudulens.models.Report;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SearchFragment extends Fragment {
    private EditText etQuery;
    private ImageView btnCheck;
    private TextView tvResult;
    private TextView tvSearchEmpty;
    private RecyclerView rvResults;
    private ReportAdapter adapter;
    private final List<Report> allSpam = new ArrayList<>();
    private View cardRansom;
    private View cardEmployment;
    private View cardCharity;
    private View cardInvestment;
    private View cardPromo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_search, container, false);

        etQuery = v.findViewById(R.id.etSearch);
        btnCheck = v.findViewById(R.id.btnCheck);
        tvResult = v.findViewById(R.id.tvResult);
        tvSearchEmpty = v.findViewById(R.id.tvSearchEmpty);
        rvResults = v.findViewById(R.id.rvSearchResults);
        cardRansom = v.findViewById(R.id.cardRansomScams);
        cardEmployment = v.findViewById(R.id.cardEmploymentScams);
        cardCharity = v.findViewById(R.id.cardCharityScams);
        cardInvestment = v.findViewById(R.id.cardInvestmentScams);
        cardPromo = v.findViewById(R.id.cardPromoScams);

        if (rvResults != null) {
            rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new ReportAdapter(new ArrayList<>(), this::showMessageDialog);
            rvResults.setAdapter(adapter);
        }

        loadSpamMessages();

        btnCheck.setOnClickListener(x -> {
            String q = etQuery.getText().toString().trim();
            if (q.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a message to search", Toast.LENGTH_SHORT).show();
                renderResults(new ArrayList<>());
                return;
            }
            filterSpamResults(q);
        });

        etQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s != null ? s.toString().trim() : "";
                if (q.isEmpty()) {
                    renderResults(new ArrayList<>());
                } else {
                    filterSpamResults(q);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupCategoryClick(cardRansom, "ransom");
        setupCategoryClick(cardEmployment, "employment");
        setupCategoryClick(cardCharity, "charity");
        setupCategoryClick(cardInvestment, "investment");
        setupCategoryClick(cardPromo, "promo");

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSpamMessages();
        String q = etQuery != null ? etQuery.getText().toString().trim() : "";
        if (!q.isEmpty()) {
            filterSpamResults(q);
        } else {
            renderResults(new ArrayList<>());
        }
    }

    private void loadSpamMessages() {
        if (getContext() == null) return;
        allSpam.clear();
        allSpam.addAll(FirebaseHelper.getDetectedScamMessages(getContext()));
    }

    private void filterSpamResults(String query) {
        String q = safeLower(query);
        List<Report> filtered = new ArrayList<>();
        for (Report r : allSpam) {
            String message = safeLower(r.getMessage());
            String source = safeLower(r.getSource());
            if (message.contains(q) || source.contains(q)) {
                filtered.add(r);
            }
        }
        renderResults(filtered);
    }

    private void renderResults(List<Report> results) {
        if (adapter != null) {
            adapter.update(results);
        }
        if (tvResult != null) {
            if (results.isEmpty()) {
                tvResult.setVisibility(View.GONE);
            } else {
                tvResult.setVisibility(View.VISIBLE);
                tvResult.setText("Found " + results.size() + " spam message(s)");
            }
        }
        if (tvSearchEmpty != null) {
            tvSearchEmpty.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private String safeLower(String value) {
        if (TextUtils.isEmpty(value)) return "";
        return value.trim().toLowerCase(Locale.US);
    }

    private void showMessageDialog(Report report) {
        if (report == null || getContext() == null) return;
        String source = report.getSource() != null ? report.getSource() : "Unknown";
        String message = report.getMessage() != null ? report.getMessage() : "";
        new AlertDialog.Builder(getContext())
                .setTitle("From: " + source)
                .setMessage(message)
                .setPositiveButton(getString(R.string.report_scam), (d, w) -> {
                    FirebaseHelper.addTrainingSample(getContext(), message, true, "sms_feedback");
                    FirebaseHelper.logUserActivity(getContext(), "sms_marked_scam");
                    String reportUserId = getReportUserId();
                    Report reportEntry = new Report(
                            reportUserId,
                            message,
                            "Reported Scam",
                            new Timestamp(new Date()),
                            "open"
                    );
                    reportEntry.setSource(source);
                    FirebaseHelper.addReport(reportEntry.toMap(), ok -> {});
                    Toast.makeText(getContext(), getString(R.string.feedback_marked_scam), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.close), null)
                .show();
    }

    private String getReportUserId() {
        String email = FirebaseHelper.getLoggedInEmail(getContext());
        if (email != null && !email.trim().isEmpty()) {
            return email.trim().toLowerCase(Locale.US);
        }
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser != null && authUser.getUid() != null && !authUser.getUid().trim().isEmpty()) {
            return authUser.getUid();
        }
        return "anonymous";
    }

    private void setupCategoryClick(View card, String category) {
        if (card == null) return;
        card.setOnClickListener(v -> {
            if (getContext() == null) return;
            Intent i = new Intent(getContext(), FilteredReportsActivity.class);
            i.putExtra(FilteredReportsActivity.EXTRA_CATEGORY, category);
            startActivity(i);
        });
    }
}
