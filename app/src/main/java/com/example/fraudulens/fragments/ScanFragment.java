package com.example.fraudulens.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.activities.BulkTrainingActivity;
import com.example.fraudulens.models.Report;
import com.example.fraudulens.utils.ScamDetector;
import com.google.firebase.Timestamp;

public class ScanFragment extends Fragment {
    private EditText etMessage;
    private Button btnDetect, btnReport;
    private Button btnMarkScam, btnMarkSafe;
    private Button btnTrainInbox;
    private Button btnTrainFromInbox;
    private View cardResult;
    private TextView tvResultTitle, tvResultSummary;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_scan, container, false);

        etMessage = v.findViewById(R.id.etMessage);
        btnDetect = v.findViewById(R.id.btnDetect);
        cardResult = v.findViewById(R.id.cardResult);
        tvResultTitle = v.findViewById(R.id.tvResultTitle);
        tvResultSummary = v.findViewById(R.id.tvResultSummary);
        btnReport = v.findViewById(R.id.btnReportFromScan);
        btnMarkScam = v.findViewById(R.id.btnMarkScam);
        btnMarkSafe = v.findViewById(R.id.btnMarkSafe);
        btnTrainInbox = v.findViewById(R.id.btnTrainInbox);
        btnTrainFromInbox = v.findViewById(R.id.btnTrainFromInbox);

        btnDetect.setOnClickListener(x -> {
            String txt = etMessage.getText().toString().trim();
            if (txt.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.scan_enter_message), Toast.LENGTH_SHORT).show();
                return;
            }

            boolean localScam = ScamDetector.isScam(requireContext(), txt);
            cardResult.setVisibility(View.VISIBLE);

            if (localScam) {
                tvResultTitle.setText(getString(R.string.result_scam));
                tvResultSummary.setText("⚠️ This message contains suspicious indicators. Proceed with caution.");
            } else {
                tvResultTitle.setText(getString(R.string.result_safe));
                tvResultSummary.setText("✅ No immediate scam indicators found. Checking cloud...");
            }

            ScamDetector.checkHybrid(requireContext(), txt, (isScam, score, source) -> {
                if (!"cloud".equals(source)) return;
                if (isScam) {
                    tvResultTitle.setText(getString(R.string.result_scam));
                    tvResultSummary.setText("⚠️ Cloud verification flagged this as a possible scam.");
                } else if (!localScam) {
                    tvResultTitle.setText(getString(R.string.result_safe));
                    tvResultSummary.setText("✅ Cloud verification found no scam indicators.");
                }
            });
        });

        btnReport.setOnClickListener(x -> handleReport());
        btnMarkScam.setOnClickListener(x -> handleFeedback(true));
        btnMarkSafe.setOnClickListener(x -> handleFeedback(false));
        btnTrainInbox.setOnClickListener(x -> handleSeedTraining());
        btnTrainFromInbox.setOnClickListener(x -> {
            if (getContext() == null) return;
            startActivity(new Intent(getContext(), BulkTrainingActivity.class));
        });

        return v;
    }

    private void handleFeedback(boolean isScam) {
        String txt = etMessage.getText().toString().trim();
        if (txt.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.scan_enter_message), Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseHelper.addTrainingSample(requireContext(), txt, isScam, "scan_feedback");
        FirebaseHelper.logUserActivity(requireContext(), isScam ? "scan_marked_scam" : "scan_marked_safe");
        Toast.makeText(
                getContext(),
                getString(isScam ? R.string.feedback_marked_scam : R.string.feedback_marked_safe),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void handleReport() {
        String txt = etMessage.getText().toString().trim();
        if (txt.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.scan_enter_message), Toast.LENGTH_SHORT).show();
            return;
        }

        String email = FirebaseHelper.getLoggedInEmail(requireContext());

        String uid = (email != null) ? email : "anonymous";


        boolean isScam = ScamDetector.isScam(requireContext(), txt);

        // ✅ Fixed: Using Firestore Timestamp instead of long
        Report r = new Report(
                uid,
                txt,
                isScam ? "Potential Scam" : "Looks Safe",
                new Timestamp(new java.util.Date()),
                "open"
        );

        FirebaseHelper.addReport(r.toMap(), ok -> {
            if (getActivity() != null)
                getActivity().runOnUiThread(() -> {
                    if (ok) {
                        FirebaseHelper.addTrainingSample(requireContext(), txt, true, "scan_report");
                        Toast.makeText(getContext(), "✅ Report submitted successfully!", Toast.LENGTH_SHORT).show();
                        etMessage.setText("");
                        cardResult.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(getContext(), "❌ Failed to submit report. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
        });
    }

    private void handleSeedTraining() {
        if (getContext() == null) return;
        int added = 0;
        for (Report report : FirebaseHelper.getDetectedScamMessages(requireContext())) {
            String message = report.getMessage();
            if (message == null || message.trim().isEmpty()) continue;
            FirebaseHelper.addTrainingSample(requireContext(), message, true, "seed_detected_sms");
            added++;
        }
        if (added == 0) {
            Toast.makeText(getContext(), getString(R.string.training_seed_empty), Toast.LENGTH_SHORT).show();
        } else {
            FirebaseHelper.logUserActivity(requireContext(), "seed_training_detected_scams");
            Toast.makeText(getContext(), getString(R.string.training_seed_added, added), Toast.LENGTH_SHORT).show();
        }
    }
}
