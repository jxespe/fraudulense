package com.example.fraudulens.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.activities.LoginActivity;
import com.example.fraudulens.activities.SettingsActivity;
import com.example.fraudulens.utils.AuthHelper;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvPhone;
    private LinearLayout llSettings, llLogout;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName = v.findViewById(R.id.tvName);
        tvPhone = v.findViewById(R.id.tvPhone);
        llSettings = v.findViewById(R.id.llSettings);
        llLogout = v.findViewById(R.id.llLogout);

        // ✅ Get logged-in email from SharedPreferences
        String email = FirebaseHelper.getLoggedInEmail(requireContext());

        if (email != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("email", email.toLowerCase())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            String name = snapshot.getDocuments().get(0).getString("name");
                            String phone = snapshot.getDocuments().get(0).getString("phoneNumber");
                            if (phone == null || phone.trim().isEmpty()) {
                                phone = FirebaseHelper.getVerifiedPhone(requireContext());
                            }
                            tvName.setText(name != null && !name.trim().isEmpty() ? name : "Unknown User");
                            tvPhone.setText(phone != null && !phone.trim().isEmpty() ? phone : "No phone number");
                        }
                    })
                    .addOnFailureListener(e -> {
                        tvName.setText("Unknown User");
                        tvPhone.setText("No phone number");
                    });
        } else {
            tvName.setText("Unknown User");
            tvPhone.setText("No phone number");
        }

        llSettings.setOnClickListener(view -> {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });

        llLogout.setOnClickListener(view -> {
            // Sign out from all providers (Firebase, Google, Facebook)
            AuthHelper.signOut(requireContext());

            Intent i = new Intent(getActivity(), LoginActivity.class);
            startActivity(i);
            requireActivity().finish();
        });

        return v;
    }
}
