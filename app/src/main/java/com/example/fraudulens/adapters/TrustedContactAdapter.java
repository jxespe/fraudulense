package com.example.fraudulens.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.R;

import java.util.List;
import java.util.Set;

public class TrustedContactAdapter extends RecyclerView.Adapter<TrustedContactAdapter.Holder> {

    public static class ContactItem {
        public final String name;
        public final String number;

        public ContactItem(String name, String number) {
            this.name = name;
            this.number = number;
        }
    }

    public interface OnToggle {
        void onToggle(ContactItem item, boolean isChecked);
    }

    private final List<ContactItem> items;
    private final Set<String> trustedNumbers;
    private final OnToggle listener;

    public TrustedContactAdapter(List<ContactItem> items, Set<String> trustedNumbers, OnToggle listener) {
        this.items = items;
        this.trustedNumbers = trustedNumbers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trusted_contact, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ContactItem item = items.get(position);
        holder.tvName.setText(item.name);
        holder.tvNumber.setText(item.number);
        boolean isTrusted = trustedNumbers.contains(item.number);
        holder.cbTrusted.setOnCheckedChangeListener(null);
        holder.cbTrusted.setChecked(isTrusted);
        holder.cbTrusted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggle(item, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvNumber;
        CheckBox cbTrusted;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvNumber = itemView.findViewById(R.id.tvNumber);
            cbTrusted = itemView.findViewById(R.id.cbTrusted);
        }
    }
}
