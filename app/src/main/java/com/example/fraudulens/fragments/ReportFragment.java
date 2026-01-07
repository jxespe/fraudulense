package com.example.fraudulens.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.models.Report;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;

public class ReportFragment extends Fragment {
    private EditText etNumber, etSummary;
    private Button btnSubmit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_report, container, false);

        etNumber = v.findViewById(R.id.etNumber);
        etSummary = v.findViewById(R.id.etSummary);
        btnSubmit = v.findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(view -> handleSubmit());

        return v;
    }

    private void handleSubmit() {
        String number = etNumber.getText().toString().trim();
        String summary = etSummary.getText().toString().trim();

        if (number.isEmpty()) {
            etNumber.setError("Please enter a number");
            return;
        }
        if (summary.isEmpty()) {
            etSummary.setError("Please provide a brief summary");
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "anonymous";

        // Construct the report correctly
        Report r = new Report(
                userId,
                number,                     // message or target number
                summary,                    // result or description
                new Timestamp(new java.util.Date()), // Firestore Timestamp
                "open"                      // default status
        );

        // Submit report to Firestore
        FirebaseHelper.addReport(r.toMap(), success -> {
            if (success) {
                Toast.makeText(getContext(), "✅ Report submitted successfully!", Toast.LENGTH_SHORT).show();
                etNumber.setText("");
                etSummary.setText("");
            } else {
                Toast.makeText(getContext(), "❌ Failed to submit. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
