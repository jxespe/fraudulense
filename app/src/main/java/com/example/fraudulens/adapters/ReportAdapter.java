package com.example.fraudulens.adapters;

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
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.Holder> {
    private List<Report> items;
    private final OnItemClick listener;

    public interface OnItemClick { void onClick(Report report); }

    public ReportAdapter(List<Report> items, OnItemClick listener) {
        this.items = items;
        this.listener = listener;
    }

    public void update(List<Report> data) {
        this.items = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Report r = items.get(position);

        // Limit message length for preview
        String message = r.getMessage() != null && r.getMessage().length() > 40
                ? r.getMessage().substring(0, 40) + "…"
                : r.getMessage();
        holder.tvTarget.setText(message != null ? message : "No message");

        holder.tvSummary.setText(r.getResult() != null ? r.getResult() : "No analysis result");
        holder.tvStatus.setText(r.getStatus() != null ? r.getStatus() : "Unknown");
        if (r.getSource() != null && !r.getSource().trim().isEmpty()) {
            holder.tvSource.setVisibility(View.VISIBLE);
            holder.tvSource.setText("From: " + r.getSource());
        } else {
            holder.tvSource.setVisibility(View.GONE);
        }

        // ✅ Safely handle Firestore Timestamp
        Timestamp ts = r.getTimestamp();
        if (ts != null) {
            long millis = ts.toDate().getTime();
            holder.tvTimestamp.setText(timeAgo(millis));
        } else {
            holder.tvTimestamp.setText("—");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(r);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvTarget, tvSource, tvSummary, tvStatus, tvTimestamp;

        Holder(@NonNull View v) {
            super(v);
            tvTarget = v.findViewById(R.id.tvTarget);
            tvSource = v.findViewById(R.id.tvSource);
            tvSummary = v.findViewById(R.id.tvSummary);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvTimestamp = v.findViewById(R.id.tvTimestamp);
        }
    }

    private String timeAgo(long epochMillis) {
        long diff = System.currentTimeMillis() - epochMillis;
        long minutes = diff / 60000;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }
}
