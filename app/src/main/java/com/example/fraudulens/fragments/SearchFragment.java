package com.example.fraudulens.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.activities.FilteredReportsActivity;

public class SearchFragment extends Fragment {
    private EditText etQuery;
    private ImageView btnCheck;
    private TextView tvResult;
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
        cardRansom = v.findViewById(R.id.cardRansomScams);
        cardEmployment = v.findViewById(R.id.cardEmploymentScams);
        cardCharity = v.findViewById(R.id.cardCharityScams);
        cardInvestment = v.findViewById(R.id.cardInvestmentScams);
        cardPromo = v.findViewById(R.id.cardPromoScams);

        btnCheck.setOnClickListener(x -> {
            String q = etQuery.getText().toString().trim();

            if (q.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a phone number or message content", Toast.LENGTH_SHORT).show();
                return;
            }

            tvResult.setVisibility(View.VISIBLE);
            tvResult.setText("Checking...");
            FirebaseHelper.getReportByMessage(q, exists -> {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        tvResult.setText(exists ? "⚠️ Found: Similar report exists" : "✅ No reports found")
                );
            });
        });

        setupCategoryClick(cardRansom, "ransom");
        setupCategoryClick(cardEmployment, "employment");
        setupCategoryClick(cardCharity, "charity");
        setupCategoryClick(cardInvestment, "investment");
        setupCategoryClick(cardPromo, "promo");

        return v;
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
