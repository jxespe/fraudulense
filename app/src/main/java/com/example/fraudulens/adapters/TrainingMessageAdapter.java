package com.example.fraudulens.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.R;
import com.example.fraudulens.models.Report;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TrainingMessageAdapter extends RecyclerView.Adapter<TrainingMessageAdapter.Holder> {
    public interface KeyProvider {
        String getKey(Report report);
    }

    public interface OnSelectionChanged {
        void onSelectionChanged(int selectedCount);
    }

    private List<Report> items;
    private final Set<String> selectedKeys;
    private final KeyProvider keyProvider;
    private final OnSelectionChanged selectionChanged;

    public TrainingMessageAdapter(
            List<Report> items,
            Set<String> selectedKeys,
            KeyProvider keyProvider,
            OnSelectionChanged selectionChanged
    ) {
        this.items = items;
        this.selectedKeys = selectedKeys;
        this.keyProvider = keyProvider;
        this.selectionChanged = selectionChanged;
    }

    public void update(List<Report> data) {
        this.items = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_training_message, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Report report = items.get(position);
        String message = report.getMessage() != null ? report.getMessage() : "";
        String preview = message.length() > 120 ? message.substring(0, 120) + "…" : message;
        holder.tvMessagePreview.setText(preview.isEmpty() ? "—" : preview);
        String source = report.getSource() != null && !report.getSource().trim().isEmpty()
                ? "From: " + report.getSource()
                : holder.itemView.getContext().getString(R.string.training_unknown_sender);
        holder.tvSource.setText(source);

        Timestamp ts = report.getTimestamp();
        if (ts != null) {
            holder.tvTimestamp.setText(formatDate(ts.toDate().getTime()));
        } else {
            holder.tvTimestamp.setText("—");
        }

        String key = keyProvider.getKey(report);
        boolean checked = selectedKeys.contains(key);
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(checked);
        holder.cbSelect.setOnCheckedChangeListener((v, isChecked) -> toggleSelection(key, isChecked));

        holder.itemView.setOnClickListener(v -> toggleSelection(key, !selectedKeys.contains(key)));
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    private void toggleSelection(String key, boolean shouldSelect) {
        if (shouldSelect) {
            selectedKeys.add(key);
        } else {
            selectedKeys.remove(key);
        }
        if (selectionChanged != null) {
            selectionChanged.onSelectionChanged(selectedKeys.size());
        }
        notifyDataSetChanged();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvMessagePreview;
        TextView tvSource;
        TextView tvTimestamp;
        CheckBox cbSelect;

        Holder(@NonNull View v) {
            super(v);
            tvMessagePreview = v.findViewById(R.id.tvMessagePreview);
            tvSource = v.findViewById(R.id.tvSource);
            tvTimestamp = v.findViewById(R.id.tvTimestamp);
            cbSelect = v.findViewById(R.id.cbSelect);
        }
    }

    private String formatDate(long millis) {
        return new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(millis);
    }
}
