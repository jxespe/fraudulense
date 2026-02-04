package com.example.fraudulens.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.models.Post;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<Post> postList = new ArrayList<>();
    private final PostActionListener listener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<String, String> photoCache = new HashMap<>();

    public interface PostActionListener {
        void onLike(Post post, boolean currentlyLiked);
        void onComment(Post post);
        void onShare(Post post);
    }

    public PostAdapter(Context context, PostActionListener listener) {
        this.context = context;
        this.listener = listener;
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

        holder.tvUserName.setText(post.getUserName() != null ? post.getUserName() : "Anonymous");
        holder.tvTime.setText(formatTimeAgo(post.getTimestamp() != null ? post.getTimestamp().toDate().getTime() : 0));

        boolean hasCaption = post.getCaption() != null && !post.getCaption().trim().isEmpty();
        holder.tvCaption.setVisibility(hasCaption ? View.VISIBLE : View.GONE);
        holder.tvCaption.setText(hasCaption ? post.getCaption().trim() : "");

        boolean hasImage = post.getImageUrl() != null && !post.getImageUrl().trim().isEmpty();
        holder.imgPost.setVisibility(hasImage ? View.VISIBLE : View.GONE);
        if (hasImage) {
            Picasso.get()
                    .load(post.getImageUrl())
                    .placeholder(R.drawable.sample_post)
                    .into(holder.imgPost);
        }

        bindUserPhoto(holder, post);

        holder.tvLikeCount.setText(post.getLikeCount() + " Likes");
        holder.tvCommentCount.setText(post.getCommentCount() + " Comments");
        holder.tvShareCount.setText(post.getShareCount() + " Shares");

        String currentUser = FirebaseHelper.getLoggedInEmail(context);
        boolean liked = post.getLikes() != null && currentUser != null && post.getLikes().contains(currentUser);
        holder.btnLike.setImageResource(liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

        holder.btnLike.setOnClickListener(v -> {
            if (listener != null) listener.onLike(post, liked);
        });
        holder.btnComment.setOnClickListener(v -> {
            if (listener != null) listener.onComment(post);
        });
        holder.btnShare.setOnClickListener(v -> {
            if (listener != null) listener.onShare(post);
        });

        Map<String, Object> sharedPost = post.getSharedPost();
        boolean hasShared = sharedPost != null && !sharedPost.isEmpty();
        holder.tvShareLabel.setVisibility(hasShared ? View.VISIBLE : View.GONE);
        holder.cardSharedContainer.setVisibility(hasShared ? View.VISIBLE : View.GONE);
        if (hasShared) {
            String sharedUser = valueOf(sharedPost.get("userName"), "Unknown");
            String sharedCaption = valueOf(sharedPost.get("caption"), "");
            String sharedImage = valueOf(sharedPost.get("imageUrl"), "");
            String sharedPhoto = valueOf(sharedPost.get("userPhotoUrl"), "");
            String sharedUserId = valueOf(sharedPost.get("userId"), "");
            String sharedTime = formatSharedTime(sharedPost.get("timestamp"));

            holder.tvSharedUserName.setText(sharedUser);
            holder.tvSharedTime.setText(sharedTime);

            bindSharedUserPhoto(holder.imgSharedUser, sharedUserId, sharedPhoto);

            boolean sharedHasImage = !sharedImage.isEmpty();
            holder.imgSharedPost.setVisibility(sharedHasImage ? View.VISIBLE : View.GONE);
            if (sharedHasImage) {
                Picasso.get().load(sharedImage).placeholder(R.drawable.sample_post).into(holder.imgSharedPost);
            }

            boolean sharedHasCaption = !sharedCaption.isEmpty();
            holder.tvSharedCaption.setVisibility(sharedHasCaption ? View.VISIBLE : View.GONE);
            holder.tvSharedCaption.setText(sharedHasCaption ? sharedCaption : "");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onComment(post);
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
        ImageView imgUser, imgPost, btnLike;
        TextView tvUserName, tvTime, tvCaption, tvLikeCount, tvCommentCount, tvShareCount, btnComment, btnShare;
        TextView tvShareLabel, tvSharedUserName, tvSharedTime, tvSharedCaption;
        ImageView imgSharedUser, imgSharedPost;
        View cardSharedContainer;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUser = itemView.findViewById(R.id.imgUser);
            imgPost = itemView.findViewById(R.id.imgPost);
            btnLike = itemView.findViewById(R.id.btnLike);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvCaption = itemView.findViewById(R.id.tvCaption);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            tvShareCount = itemView.findViewById(R.id.tvShareCount);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnShare = itemView.findViewById(R.id.btnShare);
            tvShareLabel = itemView.findViewById(R.id.tvShareLabel);
            cardSharedContainer = itemView.findViewById(R.id.cardSharedContainer);
            imgSharedUser = itemView.findViewById(R.id.imgSharedUser);
            tvSharedUserName = itemView.findViewById(R.id.tvSharedUserName);
            tvSharedTime = itemView.findViewById(R.id.tvSharedTime);
            imgSharedPost = itemView.findViewById(R.id.imgSharedPost);
            tvSharedCaption = itemView.findViewById(R.id.tvSharedCaption);
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

    private String valueOf(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
    }

    private String formatSharedTime(Object timestampValue) {
        if (timestampValue instanceof com.google.firebase.Timestamp) {
            long millis = ((com.google.firebase.Timestamp) timestampValue).toDate().getTime();
            return formatTimeAgo(millis);
        }
        if (timestampValue instanceof Long) {
            return formatTimeAgo((Long) timestampValue);
        }
        return "";
    }

    private void bindUserPhoto(PostViewHolder holder, Post post) {
        String userId = post.getUserId();
        String directPhoto = post.getUserPhotoUrl();
        if (userId != null && !userId.trim().isEmpty()) {
            String cached = photoCache.get(userId);
            if (cached != null && !cached.trim().isEmpty()) {
                Picasso.get().load(cached).placeholder(R.drawable.ic_profile).into(holder.imgUser);
                return;
            }
            holder.imgUser.setImageResource(R.drawable.ic_profile);
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        String url = doc.getString("photoUrl");
                        if (url != null && !url.trim().isEmpty()) {
                            photoCache.put(userId, url);
                            Picasso.get().load(url).placeholder(R.drawable.ic_profile).into(holder.imgUser);
                        }
                    });
            return;
        }
        if (directPhoto != null && !directPhoto.trim().isEmpty()) {
            Picasso.get().load(directPhoto).placeholder(R.drawable.ic_profile).into(holder.imgUser);
        } else {
            holder.imgUser.setImageResource(R.drawable.ic_profile);
        }
    }

    private void bindSharedUserPhoto(ImageView img, String userId, String fallback) {
        if (userId != null && !userId.trim().isEmpty()) {
            String cached = photoCache.get(userId);
            if (cached != null && !cached.trim().isEmpty()) {
                Picasso.get().load(cached).placeholder(R.drawable.ic_profile).into(img);
                return;
            }
            img.setImageResource(R.drawable.ic_profile);
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        String url = doc.getString("photoUrl");
                        if (url != null && !url.trim().isEmpty()) {
                            photoCache.put(userId, url);
                            Picasso.get().load(url).placeholder(R.drawable.ic_profile).into(img);
                        }
                    });
            return;
        }
        if (fallback != null && !fallback.trim().isEmpty()) {
            Picasso.get().load(fallback).placeholder(R.drawable.ic_profile).into(img);
        } else {
            img.setImageResource(R.drawable.ic_profile);
        }
    }
}
