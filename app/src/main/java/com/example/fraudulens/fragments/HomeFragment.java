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
import android.widget.ImageView;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.squareup.picasso.Picasso;

public class HomeFragment extends Fragment {
    private static final int REQ_READ_SMS = 2001;
    TextView tvGreeting;
    TextView tvBuyPremium;
    TextView tvViewDetails;
    TextView tvDailyCount;
    View viewDailyDot;
    ImageView ivGreetingProfile;
    View cardDailyReport;
    View cardScamAlerts;
    View cardFraudPrevention;
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
        ivGreetingProfile = v.findViewById(R.id.ivGreetingProfile);
        tvBuyPremium = v.findViewById(R.id.tvBuyPremium);
        tvViewDetails = v.findViewById(R.id.tvViewDetails);
        tvDailyCount = v.findViewById(R.id.tvDailyCount);
        viewDailyDot = v.findViewById(R.id.viewDailyDot);
        cardDailyReport = v.findViewById(R.id.cardDailyReport);
        cardScamAlerts = v.findViewById(R.id.cardScamAlerts);
        cardFraudPrevention = v.findViewById(R.id.cardFraudPrevention);
        cardTrustedContacts = v.findViewById(R.id.cardTrustedContacts);
        cardReportScam = v.findViewById(R.id.cardReportScam);
        
        // Update greeting with user's first name if available
        com.google.firebase.auth.FirebaseUser authUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String userEmail = authUser != null ? authUser.getEmail() : FirebaseHelper.getLoggedInEmail(getContext());
        if (authUser != null && authUser.getUid() != null && tvGreeting != null) {
            reg = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(authUser.getUid())
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            loadGreetingFromLoginId(userEmail);
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            applyGreetingSnapshot(snapshot, userEmail);
                        } else {
                            loadGreetingFromLoginId(userEmail);
                        }
                    });
        } else if (tvGreeting != null) {
            loadGreetingFromLoginId(userEmail);
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

        if (tvDailyCount != null) {
            tvDailyCount.setOnClickListener(view -> {
                if (getContext() == null) return;
                startActivity(new Intent(getContext(), ScamMessagesActivity.class));
            });
        }

        if (cardDailyReport != null) {
            cardDailyReport.setOnClickListener(view -> {
                if (getContext() == null) return;
                startActivity(new Intent(getContext(), ScamMessagesActivity.class));
            });
        }

        if (cardScamAlerts != null) {
            cardScamAlerts.setOnClickListener(view -> {
                if (getContext() == null) return;
                startActivity(new Intent(getContext(), com.example.fraudulens.activities.CommunityReportsActivity.class));
            });
        }

        if (cardFraudPrevention != null) {
            cardFraudPrevention.setOnClickListener(view -> {
                if (getContext() == null) return;
                startActivity(new Intent(getContext(), com.example.fraudulens.activities.FraudPreventionTipsActivity.class));
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
            ContextCompat.registerReceiver(
                    requireContext(),
                    scamReceiver,
                    new IntentFilter(SmsReceiver.ACTION_SCAM_SMS),
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
        }
        updateDailyCount();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDailyCount();
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
            tvDailyCount.setText(getString(R.string.home_scams_detected, 0));
            if (viewDailyDot != null) viewDailyDot.setVisibility(View.GONE);
            return;
        }

        int count = countScamMessagesSinceLastSeen();
        tvDailyCount.setText(getString(R.string.home_scams_detected, count));
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
                    if (ScamDetector.isScam(requireContext(), body)) {
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

    private void applyGreetingSnapshot(DocumentSnapshot doc, String userEmail) {
        String fullName = doc.getString("name");
        String photoUrl = doc.getString("photoUrl");
        String firstName = extractFirstName(fullName, userEmail);
        tvGreeting.setText(getString(R.string.home_greeting, firstName));
        if (ivGreetingProfile != null) {
            if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                Picasso.get().load(photoUrl).placeholder(R.drawable.ic_profile).into(ivGreetingProfile);
            } else {
                ivGreetingProfile.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    private void applyGreetingFallback(String userEmail) {
        com.google.firebase.auth.FirebaseUser authUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String displayName = authUser != null ? authUser.getDisplayName() : null;
        String firstName = extractFirstName(displayName, userEmail);
        tvGreeting.setText(getString(R.string.home_greeting, firstName));
        if (ivGreetingProfile != null) {
            ivGreetingProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    private void loadGreetingFromLoginId(String userEmail) {
        if (tvGreeting == null) return;
        String loginId = userEmail != null ? userEmail : FirebaseHelper.getLoggedInEmail(getContext());
        if (loginId == null || loginId.trim().isEmpty()) {
            applyGreetingFallback(userEmail);
            return;
        }
        FirebaseHelper.getUserByLoginId(loginId, doc -> {
            if (doc != null && doc.exists()) {
                applyGreetingSnapshot(doc, userEmail);
            } else {
                applyGreetingFallback(userEmail);
            }
        });
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
