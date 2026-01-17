package com.example.fraudulens.fragments;

import android.os.Bundle;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.activities.PremiumActivity;
import com.example.fraudulens.activities.ReportScamActivity;
import com.example.fraudulens.activities.ScamMessagesActivity;
import com.example.fraudulens.activities.TrustedContactsActivity;
import com.example.fraudulens.receivers.SmsReceiver;
import com.example.fraudulens.utils.ScamDetector;
import com.google.firebase.firestore.*;

public class HomeFragment extends Fragment {
    private static final int REQ_READ_SMS = 2001;
    TextView tvGreeting;
    TextView tvBuyPremium;
    TextView tvViewDetails;
    TextView tvDailyCount;
    View viewDailyDot;
    View cardTrustedContacts;
    View cardReportScam;
    ListenerRegistration reg;
    private final BroadcastReceiver scamReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!SmsReceiver.ACTION_SCAM_SMS.equals(intent.getAction())) {
                return;
            }
            updateDailyCount();
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle b) {
        View v = inf.inflate(R.layout.fragment_home, c, false);
        
        // Initialize views that exist in the layout
        tvGreeting = v.findViewById(R.id.tvGreeting);
        tvBuyPremium = v.findViewById(R.id.tvBuyPremium);
        tvViewDetails = v.findViewById(R.id.tvViewDetails);
        tvDailyCount = v.findViewById(R.id.tvDailyCount);
        viewDailyDot = v.findViewById(R.id.viewDailyDot);
        cardTrustedContacts = v.findViewById(R.id.cardTrustedContacts);
        cardReportScam = v.findViewById(R.id.cardReportScam);
        
        // Update greeting with user's first name if available
        String userEmail = FirebaseHelper.getLoggedInEmail(getContext());
        if (userEmail != null && tvGreeting != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("email", userEmail.toLowerCase())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            String fullName = snapshot.getDocuments().get(0).getString("name");
                            String firstName = extractFirstName(fullName, userEmail);
                            tvGreeting.setText("Hello, " + firstName + "!");
                        } else {
                            String firstName = extractFirstName(null, userEmail);
                            tvGreeting.setText("Hello, " + firstName + "!");
                        }
                    })
                    .addOnFailureListener(e -> {
                        String firstName = extractFirstName(null, userEmail);
                        tvGreeting.setText("Hello, " + firstName + "!");
                    });
        }

        if (tvBuyPremium != null) {
            tvBuyPremium.setOnClickListener(view -> {
                if (getContext() == null) return;
                startActivity(new Intent(getContext(), PremiumActivity.class));
            });
        }

        if (tvViewDetails != null) {
            tvViewDetails.setOnClickListener(view -> {
                if (getContext() == null) return;
                startActivity(new Intent(getContext(), ScamMessagesActivity.class));
            });
        }

        if (cardTrustedContacts != null) {
            cardTrustedContacts.setOnClickListener(view -> {
                if (getContext() == null) return;
                startActivity(new Intent(getContext(), TrustedContactsActivity.class));
            });
        }

        if (cardReportScam != null) {
            cardReportScam.setOnClickListener(view -> {
                if (getContext() == null) return;
                startActivity(new Intent(getContext(), ReportScamActivity.class));
            });
        }

        updateDailyCount();
        
        // Listen to reports (optional - only if tvAlerts exists in layout)
        // For now, we'll skip this since the layout doesn't have tvAlerts
        // You can add it back later if needed
        
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getContext() != null) {
            requireContext().registerReceiver(scamReceiver, new IntentFilter(SmsReceiver.ACTION_SCAM_SMS));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getContext() != null) {
            requireContext().unregisterReceiver(scamReceiver);
        }
    }

    private void updateDailyCount() {
        if (tvDailyCount == null || getContext() == null) return;
        if (!hasSmsPermission()) {
            requestSmsPermission();
            tvDailyCount.setText("• 0 SCAMS DETECTED");
            if (viewDailyDot != null) viewDailyDot.setVisibility(View.GONE);
            return;
        }

        int count = countScamMessagesSinceLastSeen();
        tvDailyCount.setText("• " + count + " SCAMS DETECTED");
        if (viewDailyDot != null) {
            viewDailyDot.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private int countScamMessagesSinceLastSeen() {
        int count = 0;
        long lastSeen = FirebaseHelper.getLastSeenScamTimestamp(requireContext());
        Uri uri = Uri.parse("content://sms/inbox");
        String[] projection = new String[]{"body", "date", "address"};
        Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, "date DESC");
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                    String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    if (date <= lastSeen) {
                        continue;
                    }
                    if (FirebaseHelper.isTrustedMessage(requireContext(), address, body)) {
                        continue;
                    }
                    if (ScamDetector.isScam(body)) {
                        count++;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return count;
    }

    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        if (getActivity() == null) return;
        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_SMS}, REQ_READ_SMS);
    }

    private String extractFirstName(String fullName, String emailFallback) {
        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length > 0 && !parts[0].isEmpty()) {
                return parts[0];
            }
        }
        if (emailFallback != null && emailFallback.contains("@")) {
            return emailFallback.split("@")[0];
        }
        return "there";
    }

    @Override
    public void onDestroyView() {
        if (reg != null) {
            reg.remove();
            reg = null;
        }
        super.onDestroyView();
    }
}
