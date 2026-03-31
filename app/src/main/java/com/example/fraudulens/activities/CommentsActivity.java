package com.example.fraudulens.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.adapters.CommentAdapter;
import com.example.fraudulens.models.Comment;
import com.example.fraudulens.models.Post;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommentsActivity extends AppCompatActivity {
    public static final String EXTRA_POST_ID = "extra_post_id";

    private ListenerRegistration commentsReg;
    private CommentAdapter adapter;
    private String postId;
    private EditText etNewComment;
    private ImageView btnSendComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        if (postId != null) {
            java.util.Map<String, Object> extras = new java.util.HashMap<>();
            extras.put("postId", postId);
            FirebaseHelper.logUserActivity(this, "post_viewed", extras);
        }

        RecyclerView rvComments = findViewById(R.id.rvComments);
        adapter = new CommentAdapter(this);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(adapter);
        etNewComment = findViewById(R.id.etNewComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        if (btnSendComment != null) {
            btnSendComment.setOnClickListener(v -> submitComment());
        }

        bindPostHeader(postId);
        listenComments(postId);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commentsReg != null) {
            commentsReg.remove();
            commentsReg = null;
        }
    }

    private void bindPostHeader(String id) {
        if (id == null) return;
        FirebaseHelper.getPostById(id, doc -> {
            if (doc == null || !doc.exists()) return;
            Post post = doc.toObject(Post.class);
            if (post == null) return;
            bindPostToHeader(post);
        });
    }

    private void bindPostToHeader(Post post) {
        ImageView imgUser = findViewById(R.id.imgUser);
        ImageView imgPost = findViewById(R.id.imgPost);
        ImageView btnLike = findViewById(R.id.btnLike);
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvTime = findViewById(R.id.tvTime);
        TextView tvCaption = findViewById(R.id.tvCaption);
        TextView tvLikeCount = findViewById(R.id.tvLikeCount);
        TextView tvCommentCount = findViewById(R.id.tvCommentCount);
        TextView tvShareCount = findViewById(R.id.tvShareCount);
        View layoutActionsRow = findViewById(R.id.layoutActionsRow);
        View cardSharedContainer = findViewById(R.id.cardSharedContainer);
        TextView tvShareLabel = findViewById(R.id.tvShareLabel);
        ImageView imgSharedUser = findViewById(R.id.imgSharedUser);
        ImageView imgSharedPost = findViewById(R.id.imgSharedPost);
        TextView tvSharedUserName = findViewById(R.id.tvSharedUserName);
        TextView tvSharedTime = findViewById(R.id.tvSharedTime);
        TextView tvSharedCaption = findViewById(R.id.tvSharedCaption);

        tvUserName.setText(post.getUserName() != null ? post.getUserName() : "Anonymous");
        if (post.getTimestamp() != null) {
            long millis = post.getTimestamp().toDate().getTime();
            tvTime.setText(formatTimeAgo(millis));
        } else {
            tvTime.setText("");
        }

        boolean hasCaption = post.getCaption() != null && !post.getCaption().trim().isEmpty();
        tvCaption.setVisibility(hasCaption ? View.VISIBLE : View.GONE);
        tvCaption.setText(hasCaption ? post.getCaption().trim() : "");

        boolean hasImage = post.getImageUrl() != null && !post.getImageUrl().trim().isEmpty();
        imgPost.setVisibility(hasImage ? View.VISIBLE : View.GONE);
        if (hasImage) {
            Picasso.get()
                    .load(post.getImageUrl())
                    .placeholder(R.drawable.sample_post)
                    .into(imgPost);
        }

        String userPhoto = post.getUserPhotoUrl();
        if (userPhoto != null && !userPhoto.trim().isEmpty()) {
            Picasso.get().load(userPhoto).placeholder(R.drawable.ic_profile).into(imgUser);
        } else {
            imgUser.setImageResource(R.drawable.ic_profile);
        }

        tvLikeCount.setText(post.getLikeCount() + " Likes");
        tvCommentCount.setText(post.getCommentCount() + " Comments");
        tvShareCount.setText(post.getShareCount() + " Shares");

        if (layoutActionsRow != null) layoutActionsRow.setVisibility(View.GONE);
        if (btnLike != null) btnLike.setVisibility(View.GONE);

        Map<String, Object> sharedPost = post.getSharedPost();
        boolean hasShared = sharedPost != null && !sharedPost.isEmpty();
        if (tvShareLabel != null) tvShareLabel.setVisibility(hasShared ? View.VISIBLE : View.GONE);
        if (cardSharedContainer != null) cardSharedContainer.setVisibility(hasShared ? View.VISIBLE : View.GONE);
        if (hasShared && cardSharedContainer != null) {
            String sharedUser = valueOf(sharedPost.get("userName"), "Unknown");
            String sharedCaption = valueOf(sharedPost.get("caption"), "");
            String sharedImage = valueOf(sharedPost.get("imageUrl"), "");
            String sharedPhoto = valueOf(sharedPost.get("userPhotoUrl"), "");
            String sharedTime = formatSharedTime(sharedPost.get("timestamp"));

            if (tvSharedUserName != null) tvSharedUserName.setText(sharedUser);
            if (tvSharedTime != null) tvSharedTime.setText(sharedTime);

            if (imgSharedUser != null) {
                if (!sharedPhoto.isEmpty()) {
                    Picasso.get().load(sharedPhoto).placeholder(R.drawable.ic_profile).into(imgSharedUser);
                } else {
                    imgSharedUser.setImageResource(R.drawable.ic_profile);
                }
            }

            if (imgSharedPost != null) {
                boolean sharedHasImage = !sharedImage.isEmpty();
                imgSharedPost.setVisibility(sharedHasImage ? View.VISIBLE : View.GONE);
                if (sharedHasImage) {
                    Picasso.get().load(sharedImage).placeholder(R.drawable.sample_post).into(imgSharedPost);
                }
            }

            if (tvSharedCaption != null) {
                boolean sharedHasCaption = !sharedCaption.isEmpty();
                tvSharedCaption.setVisibility(sharedHasCaption ? View.VISIBLE : View.GONE);
                tvSharedCaption.setText(sharedHasCaption ? sharedCaption : "");
            }
        }
    }

    private void listenComments(String id) {
        if (id == null) return;
        commentsReg = FirebaseHelper.listenComments(id, (QuerySnapshot snap, com.google.firebase.firestore.FirebaseFirestoreException e) -> {
            if (snap == null) return;
            List<Comment> items = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Comment c = d.toObject(Comment.class);
                if (c != null) {
                    c.setId(d.getId());
                    items.add(c);
                }
            }
            adapter.update(items);
        });
    }

    private void submitComment() {
        if (postId == null || etNewComment == null) return;
        String text = etNewComment.getText().toString().trim();
        if (text.isEmpty()) return;
        FirebaseHelper.getCurrentUserProfile(this, profile -> {
            java.util.Map<String, Object> comment = new java.util.HashMap<>();
            comment.put("text", text);
            comment.put("timestamp", com.google.firebase.Timestamp.now());
            comment.put("userId", profile.get("userId"));
            comment.put("userName", profile.get("userName"));
            comment.put("userPhotoUrl", profile.get("userPhotoUrl"));
            FirebaseHelper.addCommentToPost(postId, comment, ok -> {
                if (ok) {
                    etNewComment.setText("");
                    animateSend();
                    hideKeyboard();
                    java.util.Map<String, Object> extras = new java.util.HashMap<>();
                    extras.put("postId", postId);
                    FirebaseHelper.logUserActivity(this, "comment_added", extras);
                }
            });
        });
    }

    private void hideKeyboard() {
        try {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && etNewComment != null) {
                imm.hideSoftInputFromWindow(etNewComment.getWindowToken(), 0);
            }
        } catch (Exception ignored) {}
    }

    private void animateSend() {
        if (btnSendComment == null) return;
        btnSendComment.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .alpha(0.7f)
                .setDuration(120)
                .withEndAction(() -> btnSendComment.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(120)
                        .start())
                .start();
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
