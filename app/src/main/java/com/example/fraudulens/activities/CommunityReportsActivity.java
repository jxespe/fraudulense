package com.example.fraudulens.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fraudulens.R;
import com.example.fraudulens.adapters.ReportAdapter;
import com.example.fraudulens.models.Report;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommunityReportsActivity extends AppCompatActivity {
    private ListenerRegistration reg;
    private ReportAdapter adapter;
    private TextView tvEmpty;
    private final Map<String, String> reporterCache = new HashMap<>();
    private List<Report> currentItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_reports);

        ImageButton btnBack = findViewById(R.id.btnBackNav);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView rv = findViewById(R.id.rvReports);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(new ArrayList<>(), report -> {
            if (report == null || report.getId() == null) return;
            android.content.Intent intent = new android.content.Intent(this, DetailsActivity.class);
            intent.putExtra("reportId", report.getId());
            startActivity(intent);
        }, true);
        rv.setAdapter(adapter);

        reg = FirebaseFirestore.getInstance()
                .collection("reports")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    List<Report> items = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Report r = d.toObject(Report.class);
                        if (r == null) continue;
                        if ("training".equalsIgnoreCase(r.getStatus())) {
                            continue;
                        }
                        r.setId(d.getId());
                        r.setReporterName(getReporterName(r.getUserId()));
                        items.add(r);
                    }
                    currentItems = items;
                    runOnUiThread(this::renderItems);
                    fetchMissingReporterNames(items);
                });
    }

    private void renderItems() {
        adapter.update(currentItems);
        if (tvEmpty != null) {
            tvEmpty.setVisibility(currentItems.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private String getReporterName(String userId) {
        if (userId == null) return "";
        String cached = reporterCache.get(userId);
        if (cached != null) return cached;
        if (userId.contains("@")) {
            String local = userId.split("@")[0].trim();
            if (local.isEmpty()) return "";
            String normalizedLocal = local.replace(".", " ")
                    .replace("_", " ")
                    .replace("-", " ");
            String firstName = extractFirstName(normalizedLocal);
            if (firstName.isEmpty()) return "";
            reporterCache.put(userId, firstName);
            return firstName;
        }
        return "";
    }

    private void fetchMissingReporterNames(List<Report> items) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (Report report : items) {
            String userId = report.getUserId();
            if (userId == null || userId.contains("@")) continue;
            if (reporterCache.containsKey(userId)) continue;
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc == null || !doc.exists()) return;
                        String name = doc.getString("name");
                        if (name == null || name.trim().isEmpty()) {
                            name = doc.getString("username");
                        }
                        if (name == null || name.trim().isEmpty()) {
                            name = doc.getString("email");
                        }
                        String first = extractFirstName(name);
                        if (!first.isEmpty()) {
                            reporterCache.put(userId, first);
                            updateReporterNames();
                        }
                    });
        }
    }

    private void updateReporterNames() {
        for (Report r : currentItems) {
            if (r.getReporterName() == null || r.getReporterName().trim().isEmpty()) {
                r.setReporterName(getReporterName(r.getUserId()));
            }
        }
        runOnUiThread(this::renderItems);
    }

    private String extractFirstName(String name) {
        if (name == null) return "";
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "";
        String[] parts = trimmed.split("\\s+");
        String first = parts[0];
        if (first.isEmpty()) return "";
        return first.substring(0, 1).toUpperCase(Locale.getDefault()) + first.substring(1);
    }

    @Override
    protected void onDestroy() {
        if (reg != null) reg.remove();
        super.onDestroy();
    }
}
