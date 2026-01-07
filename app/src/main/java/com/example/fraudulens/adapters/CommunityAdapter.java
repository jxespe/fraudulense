package com.example.fraudulens.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.R;
import com.example.fraudulens.models.Report;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.Holder> {

    private final Context ctx;
    private List<Report> items = new ArrayList<>();

    public CommunityAdapter(Context ctx) {
        this.ctx = ctx;
    }

    public void update(List<Report> data) {
        if (data == null) data = new ArrayList<>();
        this.items = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_report, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        Report r = items.get(pos);

        // Match current Report.java structure
        String message = r.getMessage() != null && r.getMessage().length() > 40
                ? r.getMessage().substring(0, 40) + "…"
                : r.getMessage();

        h.tvTarget.setText(message != null ? message : "No message provided");
        h.tvSummary.setText(r.getResult() != null ? r.getResult() : "No result");
        h.tvStatus.setText(r.getStatus() != null ? r.getStatus() : "Unknown");

        // ✅ Handle Firestore Timestamp
        Timestamp ts = r.getTimestamp();
        if (ts != null) {
            long millis = ts.toDate().getTime();
            h.tvTimestamp.setText(formatTimeAgo(millis));
            h.tvTimestamp.setVisibility(View.VISIBLE);
        } else {
            h.tvTimestamp.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvTarget, tvSummary, tvStatus, tvTimestamp;

        public Holder(@NonNull View v) {
            super(v);
            tvTarget = v.findViewById(R.id.tvTarget);
            tvSummary = v.findViewById(R.id.tvSummary);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvTimestamp = v.findViewById(R.id.tvTimestamp);
        }
    }

    // ✅ Utility to format readable "time ago"
    private String formatTimeAgo(long timeMillis) {
        long now = System.currentTimeMillis();
        long diff = now - timeMillis;

        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 60) return minutes + "m ago";
        else if (hours < 24) return hours + "h ago";
        else if (days < 7) return days + "d ago";
        else
            return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(new Date(timeMillis));
    }
}
