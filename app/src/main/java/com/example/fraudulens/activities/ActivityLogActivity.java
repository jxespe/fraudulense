package com.example.fraudulens.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.R;
import com.example.fraudulens.adapters.ActivityLogAdapter;
import com.example.fraudulens.models.ActivityLog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ActivityLogActivity extends AppCompatActivity {

    private ActivityLogAdapter adapter;
    private ListenerRegistration reg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_log);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView rv = findViewById(R.id.rvActivityLog);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActivityLogAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        reg = FirebaseFirestore.getInstance()
                .collection("activity_logs")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    List<ActivityLog> items = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        ActivityLog log = d.toObject(ActivityLog.class);
                        if (log != null) log.setId(d.getId());
                        items.add(log);
                    }
                    runOnUiThread(() -> adapter.update(items));
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        if (reg != null) reg.remove();
        super.onDestroy();
    }
}
