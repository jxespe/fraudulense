package com.example.fraudulens.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.R;
import com.example.fraudulens.models.ActivityLog;
import com.google.firebase.Timestamp;

import java.util.List;

public class ActivityLogAdapter extends RecyclerView.Adapter<ActivityLogAdapter.Holder> {
    private List<ActivityLog> items;

    public ActivityLogAdapter(List<ActivityLog> items) {
        this.items = items;
    }

    public void update(List<ActivityLog> data) {
        this.items = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_log, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ActivityLog log = items.get(position);
        holder.tvUser.setText(log.getUser() != null ? log.getUser() : "unknown");
        holder.tvAction.setText(log.getAction() != null ? log.getAction() : "action");
        Timestamp ts = log.getTimestamp();
        holder.tvTime.setText(ts != null ? ts.toDate().toString() : "—");
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvUser, tvAction, tvTime;

        Holder(@NonNull View v) {
            super(v);
            tvUser = v.findViewById(R.id.tvUser);
            tvAction = v.findViewById(R.id.tvAction);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}
