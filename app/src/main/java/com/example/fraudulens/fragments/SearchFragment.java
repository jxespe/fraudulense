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

public class SearchFragment extends Fragment {
    private EditText etQuery;
    private Button btnCheck;
    private TextView tvResult;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_search, container, false);

        etQuery = v.findViewById(R.id.etQuery);
        btnCheck = v.findViewById(R.id.btnCheck);
        tvResult = v.findViewById(R.id.tvResult);

        btnCheck.setOnClickListener(x -> {
            String q = etQuery.getText().toString().trim();

            if (q.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a phone number or message content", Toast.LENGTH_SHORT).show();
                return;
            }

            tvResult.setText("Checking...");
            FirebaseHelper.getReportByMessage(q, exists -> {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        tvResult.setText(exists ? "⚠️ Found: Similar report exists" : "✅ No reports found")
                );
            });
        });

        return v;
    }
}
