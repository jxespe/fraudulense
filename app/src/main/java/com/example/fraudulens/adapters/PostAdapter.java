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
import com.example.fraudulens.models.Post;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<Post> postList = new ArrayList<>();

    public PostAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.textUsername.setText(post.getUsername());
        holder.textCaption.setText(post.getCaption());
        holder.textDate.setText(post.getDate());

        // Load image safely with Picasso
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            Picasso.get()
                    .load(post.getImageUrl())
                    .placeholder(R.drawable.sample_post)
                    .into(holder.imagePost);
        } else {
            holder.imagePost.setImageResource(R.drawable.sample_post);
        }

        // Like icon toggle (local only)
        holder.iconLike.setOnClickListener(v -> {
            holder.iconLike.setImageResource(R.drawable.ic_heart_filled);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // 🔹 Called from CommunityFragment when Firestore data updates
    public void updateList(List<Post> newPosts) {
        postList.clear();
        postList.addAll(newPosts);
        notifyDataSetChanged();
    }

    // 🔹 ViewHolder
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView textUsername, textCaption, textDate;
        ImageView imagePost, iconLike;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            textUsername = itemView.findViewById(R.id.textUsername);
            textCaption = itemView.findViewById(R.id.textCaption);
            textDate = itemView.findViewById(R.id.textDate);
            imagePost = itemView.findViewById(R.id.imagePost);
            iconLike = itemView.findViewById(R.id.iconLike);
        }
    }
}
