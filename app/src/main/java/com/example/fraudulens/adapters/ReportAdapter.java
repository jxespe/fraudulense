package com.example.fraudulens.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.R;
import com.example.fraudulens.models.Report;
import com.google.firebase.Timestamp;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.Holder> {
    private List<Report> items;
    private final OnItemClick listener;
    private final boolean showExactTime;

    public interface OnItemClick { void onClick(Report report); }

    public ReportAdapter(List<Report> items, OnItemClick listener) {
        this.items = items;
        this.listener = listener;
        this.showExactTime = false;
    }

    public ReportAdapter(List<Report> items, OnItemClick listener, boolean showExactTime) {
        this.items = items;
        this.listener = listener;
        this.showExactTime = showExactTime;
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

        if (r.getReporterName() != null && !r.getReporterName().trim().isEmpty()) {
            holder.tvReporter.setVisibility(View.VISIBLE);
            holder.tvReporter.setText("Reported by: " + r.getReporterName());
        } else {
            holder.tvReporter.setVisibility(View.GONE);
        }

        String imageUrl = r.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            holder.imgReport.setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.sample_post)
                    .into(holder.imgReport);
        } else {
            holder.imgReport.setVisibility(View.GONE);
        }

        // ✅ Safely handle Firestore Timestamp
        Timestamp ts = r.getTimestamp();
        if (ts != null) {
            long millis = ts.toDate().getTime();
            holder.tvTimestamp.setText(showExactTime ? exactTime(millis) : timeAgo(millis));
        } else {
            holder.tvTimestamp.setText("—");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(r);
        });

        holder.imgReport.setOnClickListener(v -> {
            if (listener != null) listener.onClick(r);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvTarget, tvSource, tvReporter, tvSummary, tvStatus, tvTimestamp;
        ImageView imgReport;

        Holder(@NonNull View v) {
            super(v);
            tvTarget = v.findViewById(R.id.tvTarget);
            tvSource = v.findViewById(R.id.tvSource);
            tvReporter = v.findViewById(R.id.tvReporter);
            tvSummary = v.findViewById(R.id.tvSummary);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvTimestamp = v.findViewById(R.id.tvTimestamp);
            imgReport = v.findViewById(R.id.imgReport);
        }
    }

    private String timeAgo(long epochMillis) {
        long diff = System.currentTimeMillis() - epochMillis;
        long minutes = diff / 60000;
        long hours = minutes / 60;
        long days = hours / 24;

        String timeText = new SimpleDateFormat("h:mm a", Locale.getDefault())
                .format(epochMillis);
        if (days > 7) {
            String dateText = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(epochMillis);
            return dateText + " • " + timeText;
        }
        if (minutes < 60) return minutes + "m ago • " + timeText;
        if (hours < 24) return hours + "h ago • " + timeText;
        return days + "d ago • " + timeText;
    }

    private String exactTime(long epochMillis) {
        return new SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
                .format(epochMillis);
    }
}
