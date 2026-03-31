package com.example.fraudulens.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.activities.CommentsActivity;
import com.example.fraudulens.adapters.PostAdapter;
import com.example.fraudulens.models.Post;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunityFragment extends Fragment {
    private RecyclerView rvCommunityPosts;
    private PostAdapter adapter;
    private ListenerRegistration postsReg;
    private ActivityResultLauncher<String> pickImageLauncher;
    private Uri pendingImageUri;
    private EditText etCommunitySearch;
    private final List<Post> allPosts = new ArrayList<>();
    private ImageView activePreview;
    private Dialog activeDialog;

    private ImageView imgCurrentUser;
    private Map<String, Object> currentProfile;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_community, container, false);

        rvCommunityPosts = v.findViewById(R.id.rvCommunityPosts);
        imgCurrentUser = v.findViewById(R.id.imgCurrentUser);
        TextView btnOpenCreatePost = v.findViewById(R.id.btnOpenCreatePost);
        ImageView btnQuickPhoto = v.findViewById(R.id.btnQuickPhoto);
        etCommunitySearch = v.findViewById(R.id.etCommunitySearch);

        adapter = new PostAdapter(requireContext(), new PostAdapter.PostActionListener() {
            @Override
            public void onLike(Post post, boolean currentlyLiked) {
                String user = FirebaseHelper.getUserKeyForLikes(requireContext());
                if (user == null) {
                    Toast.makeText(requireContext(), "Please log in to like posts.", Toast.LENGTH_SHORT).show();
                    return;
                }
                FirebaseHelper.setLikeOnPost(post.getId(), user, !currentlyLiked, ok -> {});
                Map<String, Object> extras = new HashMap<>();
                extras.put("postId", post.getId());
                extras.put("actionType", currentlyLiked ? "unlike" : "like");
                FirebaseHelper.logUserActivity(requireContext(),
                        currentlyLiked ? "post_unliked" : "post_liked",
                        extras);
                bumpLocalCount(post.getId(), "like", currentlyLiked ? -1 : 1);
            }

            @Override
            public void onComment(Post post) {
                if (getContext() == null) return;
                bumpLocalCount(post.getId(), "comment", 1);
                android.content.Intent intent = new android.content.Intent(getContext(), CommentsActivity.class);
                intent.putExtra(CommentsActivity.EXTRA_POST_ID, post.getId());
                startActivity(intent);
                Map<String, Object> extras = new HashMap<>();
                extras.put("postId", post.getId());
                FirebaseHelper.logUserActivity(requireContext(), "post_viewed", extras);
            }

            @Override
            public void onShare(Post post) {
                showShareDialog(post);
            }
        });

        rvCommunityPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCommunityPosts.setAdapter(adapter);

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            pendingImageUri = uri;
            if (activePreview != null) {
                activePreview.setVisibility(View.VISIBLE);
                activePreview.setImageURI(uri);
            } else {
                openCreatePostDialog(true);
            }
        });

        btnOpenCreatePost.setOnClickListener(x -> openCreatePostDialog(false));
        btnQuickPhoto.setOnClickListener(x -> pickImageLauncher.launch("image/*"));
        if (etCommunitySearch != null) {
            etCommunitySearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    applySearchFilter(s != null ? s.toString() : "");
                }
            });
        }

        loadCurrentProfile();
        listenToPosts();
        FirebaseHelper.logUserActivity(requireContext(), "community_feed_viewed");

        return v;
    }

    private void loadCurrentProfile() {
        FirebaseHelper.getCurrentUserProfile(requireContext(), profile -> {
            currentProfile = profile;
            String photoUrl = profile.get("userPhotoUrl") != null ? String.valueOf(profile.get("userPhotoUrl")) : null;
            if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                Picasso.get().load(photoUrl).placeholder(R.drawable.ic_profile).into(imgCurrentUser);
            } else {
                imgCurrentUser.setImageResource(R.drawable.ic_profile);
            }
        });
    }

    private void listenToPosts() {
        postsReg = FirebaseHelper.listenPosts((QuerySnapshot snap, com.google.firebase.firestore.FirebaseFirestoreException e) -> {
            if (snap == null) return;
            List<Post> items = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Post p = d.toObject(Post.class);
                if (p != null) {
                    p.setId(d.getId());
                    items.add(p);
                }
            }
            allPosts.clear();
            allPosts.addAll(items);
            applySearchFilter(etCommunitySearch != null ? etCommunitySearch.getText().toString() : "");
        });
    }

    private void applySearchFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) {
            adapter.updateList(new ArrayList<>(allPosts));
            return;
        }
        List<Post> filtered = new ArrayList<>();
        for (Post p : allPosts) {
            String caption = p.getCaption() != null ? p.getCaption().toLowerCase() : "";
            String name = p.getUserName() != null ? p.getUserName().toLowerCase() : "";
            if (caption.contains(q) || name.contains(q)) {
                filtered.add(p);
            }
        }
        adapter.updateList(filtered);
    }

    private void openCreatePostDialog(boolean hasPreselectedImage) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_post_fullscreen, null, false);
        EditText etCaption = dialogView.findViewById(R.id.etDialogCaption);
        ImageView imgPreview = dialogView.findViewById(R.id.imgDialogPreview);
        ImageView btnAddPhoto = dialogView.findViewById(R.id.btnDialogAddPhoto);
        ImageView btnClose = dialogView.findViewById(R.id.btnClosePost);
        TextView tvUserName = dialogView.findViewById(R.id.tvDialogUserName);
        ImageView imgUser = dialogView.findViewById(R.id.imgDialogUser);
        View btnPost = dialogView.findViewById(R.id.btnDialogPost);

        if (hasPreselectedImage && pendingImageUri != null) {
            imgPreview.setVisibility(View.VISIBLE);
            imgPreview.setImageURI(pendingImageUri);
        }

        btnAddPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        if (currentProfile != null) {
            String name = currentProfile.get("userName") != null ? String.valueOf(currentProfile.get("userName")) : "Anonymous";
            String photoUrl = currentProfile.get("userPhotoUrl") != null ? String.valueOf(currentProfile.get("userPhotoUrl")) : null;
            if (tvUserName != null) tvUserName.setText(name);
            if (imgUser != null && photoUrl != null && !photoUrl.trim().isEmpty()) {
                Picasso.get().load(photoUrl).placeholder(R.drawable.ic_profile).into(imgUser);
            }
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                pendingImageUri = null;
                activePreview = null;
                if (activeDialog != null) {
                    activeDialog.dismiss();
                    activeDialog = null;
                }
            });
        }

        if (btnPost != null) {
            btnPost.setOnClickListener(v -> {
                String caption = etCaption.getText().toString().trim();
                if ((caption.isEmpty()) && pendingImageUri == null) {
                    Toast.makeText(getContext(), "Please add a caption or photo.", Toast.LENGTH_SHORT).show();
                    return;
                }
                createPost(caption, pendingImageUri);
                pendingImageUri = null;
                activePreview = null;
                if (activeDialog != null) {
                    activeDialog.dismiss();
                    activeDialog = null;
                }
            });
        }

        activeDialog = new Dialog(getContext(), R.style.FrauduLens_SlideUpDialog);
        activeDialog.setContentView(dialogView);
        if (activeDialog.getWindow() != null) {
            activeDialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
        activeDialog.show();
        activePreview = imgPreview;
    }

    private void createPost(String caption, Uri imageUri) {
        FirebaseHelper.getCurrentUserProfile(requireContext(), profile -> {
            showPendingPost(profile, caption, imageUri);
            if (imageUri != null) {
                uploadPostImageAndSave(profile, caption, imageUri, null);
            } else {
                savePost(profile, caption, null, null);
            }
        });
    }

    private void showShareDialog(Post original) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_share_post, null, false);
        TextView tvSharedUser = dialogView.findViewById(R.id.tvSharedUser);
        TextView tvSharedTime = dialogView.findViewById(R.id.tvSharedTime);
        TextView tvSharedCaption = dialogView.findViewById(R.id.tvSharedCaption);
        ImageView imgShared = dialogView.findViewById(R.id.imgShared);
        EditText etCaption = dialogView.findViewById(R.id.etShareCaption);

        tvSharedUser.setText(original.getUserName() != null ? original.getUserName() : "Unknown");
        long tsMillis = original.getTimestamp() != null ? original.getTimestamp().toDate().getTime() : 0L;
        tvSharedTime.setText(tsMillis > 0 ? formatTimeAgo(tsMillis) : "");
        String originalCaption = original.getCaption() != null ? original.getCaption().trim() : "";
        tvSharedCaption.setText(originalCaption.isEmpty() ? "No caption" : originalCaption);
        if (original.getImageUrl() != null && !original.getImageUrl().trim().isEmpty()) {
            imgShared.setVisibility(View.VISIBLE);
            Picasso.get().load(original.getImageUrl()).placeholder(R.drawable.sample_post).into(imgShared);
        } else {
            imgShared.setVisibility(View.GONE);
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Share Post")
                .setView(dialogView)
                .setPositiveButton("Share", (d, which) -> {
                    String caption = etCaption.getText() != null ? etCaption.getText().toString().trim() : "";
                    sharePost(original, caption);
                    bumpLocalCount(original.getId(), "share", 1);
                    Map<String, Object> extras = new HashMap<>();
                    extras.put("postId", original.getId());
                    FirebaseHelper.logUserActivity(requireContext(), "post_shared", extras);
                })
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .show();
    }

    private String formatTimeAgo(long timeMillis) {
        if (timeMillis <= 0) return "";
        long now = System.currentTimeMillis();
        long diff = now - timeMillis;
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24) return hours + "h ago";
        if (days < 7) return days + "d ago";
        return new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                .format(new java.util.Date(timeMillis));
    }

    private void showPendingPost(Map<String, Object> profile, String caption, Uri imageUri) {
        Post pending = new Post();
        pending.setId("local-" + System.currentTimeMillis());
        pending.setUserId(profile.get("userId") != null ? String.valueOf(profile.get("userId")) : "local");
        pending.setUserName(profile.get("userName") != null ? String.valueOf(profile.get("userName")) : "Anonymous");
        pending.setUserPhotoUrl(profile.get("userPhotoUrl") != null ? String.valueOf(profile.get("userPhotoUrl")) : null);
        pending.setCaption(caption);
        pending.setImageUrl(imageUri != null ? imageUri.toString() : null);
        pending.setTimestamp(com.google.firebase.Timestamp.now());
        pending.setLikeCount(0);
        pending.setCommentCount(0);
        pending.setShareCount(0);
        pending.setLikes(new java.util.ArrayList<>());

        allPosts.add(0, pending);
        applySearchFilter(etCommunitySearch != null ? etCommunitySearch.getText().toString() : "");
    }

    private void uploadPostImageAndSave(Map<String, Object> profile, String caption, Uri imageUri, Map<String, Object> sharedPost) {
        String userName = profile.get("userName") != null ? String.valueOf(profile.get("userName")) : "anonymous";
        String fileName = "posts/" + System.currentTimeMillis() + "_" + userName.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".jpg";
        StorageReference ref = FirebaseHelper.getStorageRoot().child(fileName);

        ref.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> savePost(profile, caption, uri.toString(), sharedPost))
                .addOnFailureListener(e -> {
                    android.util.Log.e("CommunityFragment", "Image upload failed", e);
                    Toast.makeText(requireContext(), "Image upload failed. Posted without photo.", Toast.LENGTH_SHORT).show();
                    savePost(profile, caption, null, sharedPost);
                });
    }

    private void savePost(Map<String, Object> profile, String caption, String imageUrl, Map<String, Object> sharedPost) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", profile.get("userId"));
        data.put("userName", profile.get("userName"));
        data.put("userPhotoUrl", profile.get("userPhotoUrl"));
        data.put("caption", caption);
        data.put("imageUrl", imageUrl);
        data.put("timestamp", Timestamp.now());
        data.put("likeCount", 0);
        data.put("commentCount", 0);
        data.put("shareCount", 0);
        data.put("likes", new ArrayList<String>());
        if (sharedPost != null) {
            data.put("sharedPost", sharedPost);
        }

        FirebaseHelper.addPost(data, ok -> {
            if (ok) {
                Map<String, Object> extras = new HashMap<>();
                extras.put("hasImage", imageUrl != null && !imageUrl.trim().isEmpty());
                extras.put("hasCaption", caption != null && !caption.trim().isEmpty());
                FirebaseHelper.logUserActivity(requireContext(), "community_post_created", extras);
                Toast.makeText(requireContext(), "Posted successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to post. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sharePost(Post original, String caption) {
        Map<String, Object> shared = new HashMap<>();
        shared.put("userId", original.getUserId());
        shared.put("userName", original.getUserName());
        shared.put("userPhotoUrl", original.getUserPhotoUrl());
        shared.put("caption", original.getCaption());
        shared.put("imageUrl", original.getImageUrl());
        shared.put("timestamp", original.getTimestamp());

        FirebaseHelper.getCurrentUserProfile(requireContext(), profile -> {
            showPendingSharedPost(profile, caption, shared);
            savePost(profile, caption != null ? caption : "", null, shared);
            FirebaseHelper.incrementShareCount(original.getId());
        });
    }

    private void showPendingSharedPost(Map<String, Object> profile, String caption, Map<String, Object> sharedPost) {
        Post pending = new Post();
        pending.setId("local-share-" + System.currentTimeMillis());
        pending.setUserId(profile.get("userId") != null ? String.valueOf(profile.get("userId")) : "local");
        pending.setUserName(profile.get("userName") != null ? String.valueOf(profile.get("userName")) : "Anonymous");
        pending.setUserPhotoUrl(profile.get("userPhotoUrl") != null ? String.valueOf(profile.get("userPhotoUrl")) : null);
        pending.setCaption(caption);
        pending.setTimestamp(com.google.firebase.Timestamp.now());
        pending.setLikeCount(0);
        pending.setCommentCount(0);
        pending.setShareCount(0);
        pending.setLikes(new java.util.ArrayList<>());
        pending.setSharedPost(sharedPost);

        allPosts.add(0, pending);
        applySearchFilter(etCommunitySearch != null ? etCommunitySearch.getText().toString() : "");
    }

    private void bumpLocalCount(String postId, String type, int delta) {
        if (postId == null) return;
        for (Post p : allPosts) {
            if (postId.equals(p.getId())) {
                if ("like".equals(type)) {
                    p.setLikeCount(Math.max(0, p.getLikeCount() + delta));
                } else if ("comment".equals(type)) {
                    p.setCommentCount(Math.max(0, p.getCommentCount() + delta));
                } else if ("share".equals(type)) {
                    p.setShareCount(Math.max(0, p.getShareCount() + delta));
                }
                applySearchFilter(etCommunitySearch != null ? etCommunitySearch.getText().toString() : "");
                return;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postsReg != null) {
            postsReg.remove();
            postsReg = null;
        }
    }
}
