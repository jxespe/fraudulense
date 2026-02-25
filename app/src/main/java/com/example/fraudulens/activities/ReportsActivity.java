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
    ListenerRegistration reg;
    TextView tvEmpty;

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

        String userId = com.example.fraudulens.FirebaseHelper.getLoggedInEmail(this);
        if (userId == null || userId.trim().isEmpty()) {
            userId = "anonymous";
        }
        reg = FirebaseFirestore.getInstance()
                .collection("reports")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
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
                items.add(r);
            }
            runOnUiThread(() -> {
                adapter.update(items);
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        if (reg != null) reg.remove();
        super.onDestroy();
    }
}
