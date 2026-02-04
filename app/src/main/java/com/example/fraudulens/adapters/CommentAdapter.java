package com.example.fraudulens.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.R;
import com.example.fraudulens.models.Comment;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.Holder> {
    private final Context ctx;
    private List<Comment> items = new ArrayList<>();

    public CommentAdapter(Context ctx) {
        this.ctx = ctx;
    }

    public void update(List<Comment> data) {
        if (data == null) data = new ArrayList<>();
        items = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_comment, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        Comment c = items.get(pos);
        h.tvUser.setText(c.getUserName() != null ? c.getUserName() : "Anonymous");
        h.tvText.setText(c.getText() != null ? c.getText() : "");
        long millis = c.getTimestamp() != null ? c.getTimestamp().toDate().getTime() : 0;
        h.tvTime.setText(formatTimeAgo(millis));

        String photo = c.getUserPhotoUrl();
        if (photo != null && !photo.trim().isEmpty()) {
            Picasso.get().load(photo).placeholder(R.drawable.ic_profile).into(h.imgUser);
        } else {
            h.imgUser.setImageResource(R.drawable.ic_profile);
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView imgUser;
        TextView tvUser, tvTime, tvText;

        public Holder(@NonNull View v) {
            super(v);
            imgUser = v.findViewById(R.id.imgCommentUser);
            tvUser = v.findViewById(R.id.tvCommentUser);
            tvTime = v.findViewById(R.id.tvCommentTime);
            tvText = v.findViewById(R.id.tvCommentText);
        }
    }

    private String formatTimeAgo(long timeMillis) {
        if (timeMillis <= 0) return "";
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
