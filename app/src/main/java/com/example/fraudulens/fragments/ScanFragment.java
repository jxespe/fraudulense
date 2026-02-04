package com.example.fraudulens.fragments;

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
import com.example.fraudulens.models.Report;
import com.example.fraudulens.utils.ScamDetector;
import com.google.firebase.Timestamp;

public class ScanFragment extends Fragment {
    private EditText etMessage;
    private Button btnDetect, btnReport;
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

        btnDetect.setOnClickListener(x -> {
            String txt = etMessage.getText().toString().trim();
            if (txt.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a message to analyze.", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean scam = ScamDetector.isScam(requireContext(), txt);
            cardResult.setVisibility(View.VISIBLE);

            if (scam) {
                tvResultTitle.setText(getString(R.string.result_scam));
                tvResultSummary.setText("⚠️ This message contains suspicious indicators. Proceed with caution.");
            } else {
                tvResultTitle.setText(getString(R.string.result_safe));
                tvResultSummary.setText("✅ No immediate scam indicators found.");
            }
        });

        btnReport.setOnClickListener(x -> handleReport());

        return v;
    }

    private void handleReport() {
        String txt = etMessage.getText().toString().trim();
        if (txt.isEmpty()) {
            Toast.makeText(getContext(), "Enter the message to report.", Toast.LENGTH_SHORT).show();
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
}
