package com.example.fraudulens.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.activities.LoginActivity;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail;
    private Button btnLogout;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName = v.findViewById(R.id.tvName);
        tvEmail = v.findViewById(R.id.tvEmail);
        btnLogout = v.findViewById(R.id.btnLogout);

        // ✅ Get logged-in email from SharedPreferences
        String email = FirebaseHelper.getLoggedInEmail(requireContext());

        tvEmail.setText(email != null ? email : "Unknown user");
        tvName.setText("User"); // or fetch name from Firestore later

        btnLogout.setOnClickListener(view -> {
            FirebaseHelper.logout(requireContext());

            Intent i = new Intent(getActivity(), LoginActivity.class);
            startActivity(i);
            requireActivity().finish();
        });

        return v;
    }
}
