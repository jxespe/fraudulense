package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.adapters.ReportAdapter;
import com.example.fraudulens.models.Report;
import com.google.firebase.firestore.*;

import java.util.*;

public class ReportsActivity extends AppCompatActivity {
    RecyclerView rv;
    ReportAdapter adapter;
    ListenerRegistration reg;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_reports);
        rv = findViewById(R.id.rvReports);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(new ArrayList<>(), report -> {
            if (report == null || report.getId() == null) return;
            Intent i = new Intent(this, DetailsActivity.class);
            i.putExtra("reportId", report.getId());
            startActivity(i);
        });
        rv.setAdapter(adapter);

        reg = FirebaseHelper.listenReports((snap, e) -> {
            if (snap == null) return;
            List<Report> items = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Report r = d.toObject(Report.class);
                if (r != null) r.setId(d.getId());
                items.add(r);
            }
            runOnUiThread(() -> adapter.update(items));
        });
    }

    @Override
    protected void onDestroy() {
        if (reg != null) reg.remove();
        super.onDestroy();
    }
}
