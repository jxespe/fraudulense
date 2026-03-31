package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.example.fraudulens.R;
import com.example.fraudulens.adapters.ReportAdapter;
import com.example.fraudulens.models.Report;
import com.google.firebase.firestore.*;

import java.util.*;

public class ReportsActivity extends AppCompatActivity {
    RecyclerView rv;
    ReportAdapter adapter;
    ListenerRegistration regUid;
    ListenerRegistration regEmail;
    ListenerRegistration regAuthEmail;
    TextView tvEmpty;
    List<DocumentSnapshot> uidDocs = new ArrayList<>();
    List<DocumentSnapshot> emailDocs = new ArrayList<>();
    List<DocumentSnapshot> authEmailDocs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_reports);
        rv = findViewById(R.id.rvReports);
        tvEmpty = findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(new ArrayList<>(), report -> {
            if (report == null || report.getId() == null) return;
            Intent i = new Intent(this, DetailsActivity.class);
            i.putExtra("reportId", report.getId());
            startActivity(i);
        });
        rv.setAdapter(adapter);

        // One-time migration to normalize older report owner IDs to the current identity key.
        com.example.fraudulens.FirebaseHelper.migrateLegacyReportOwnerIds(this, ok -> {});

        String email = com.example.fraudulens.FirebaseHelper.getLoggedInEmail(this);
        if (email != null) {
            email = email.trim().toLowerCase(java.util.Locale.US);
        }
        com.google.firebase.auth.FirebaseUser authUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String uid = authUser != null ? authUser.getUid() : null;
        String authEmail = authUser != null && authUser.getEmail() != null
                ? authUser.getEmail().trim().toLowerCase(java.util.Locale.US)
                : null;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (uid != null && !uid.trim().isEmpty()) {
            regUid = db.collection("reports")
                    .whereEqualTo("userId", uid)
                    .addSnapshotListener((snap, e) -> {
                        uidDocs = snap != null ? snap.getDocuments() : new ArrayList<>();
                        mergeAndRender();
                    });
        }
        if (email != null && !email.trim().isEmpty()) {
            regEmail = db.collection("reports")
                    .whereEqualTo("userId", email)
                    .addSnapshotListener((snap, e) -> {
                        emailDocs = snap != null ? snap.getDocuments() : new ArrayList<>();
                        mergeAndRender();
                    });
        }
        if (authEmail != null && !authEmail.trim().isEmpty()
                && (email == null || !authEmail.equals(email))) {
            regAuthEmail = db.collection("reports")
                    .whereEqualTo("userId", authEmail)
                    .addSnapshotListener((snap, e) -> {
                        authEmailDocs = snap != null ? snap.getDocuments() : new ArrayList<>();
                        mergeAndRender();
                    });
        }
        if ((uid == null || uid.trim().isEmpty()) && (email == null || email.trim().isEmpty())) {
            regEmail = db.collection("reports")
                    .whereEqualTo("userId", "anonymous")
                    .addSnapshotListener((snap, e) -> {
                        emailDocs = snap != null ? snap.getDocuments() : new ArrayList<>();
                        mergeAndRender();
                    });
        }
    }

    private void mergeAndRender() {
        Map<String, Report> map = new HashMap<>();
        List<DocumentSnapshot> merged = new ArrayList<>();
        merged.addAll(uidDocs);
        merged.addAll(emailDocs);
        merged.addAll(authEmailDocs);
        for (DocumentSnapshot d : merged) {
            if (d == null || d.getId() == null) continue;
            if (map.containsKey(d.getId())) continue;
            Report r = d.toObject(Report.class);
            if (r == null) continue;
            if ("training".equalsIgnoreCase(r.getStatus())) {
                continue;
            }
            r.setId(d.getId());
            map.put(d.getId(), r);
        }
        List<Report> items = new ArrayList<>(map.values());
        items.sort((a, b) -> {
            long ta = a.getTimestamp() != null ? a.getTimestamp().toDate().getTime() : 0L;
            long tb = b.getTimestamp() != null ? b.getTimestamp().toDate().getTime() : 0L;
            return Long.compare(tb, ta);
        });
        runOnUiThread(() -> {
            adapter.update(items);
            if (tvEmpty != null) {
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (regUid != null) regUid.remove();
        if (regEmail != null) regEmail.remove();
        if (regAuthEmail != null) regAuthEmail.remove();
        super.onDestroy();
    }
}
