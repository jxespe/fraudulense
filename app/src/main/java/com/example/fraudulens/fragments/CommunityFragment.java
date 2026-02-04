package com.example.fraudulens.fragments;

import android.app.AlertDialog;
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
    private AlertDialog activeDialog;

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
                String user = FirebaseHelper.getLoggedInEmail(requireContext());
                if (user == null) {
                    Toast.makeText(requireContext(), "Please log in to like posts.", Toast.LENGTH_SHORT).show();
                    return;
                }
                FirebaseHelper.setLikeOnPost(post.getId(), user, !currentlyLiked, ok -> {});
            }

            @Override
            public void onComment(Post post) {
                if (getContext() == null) return;
                android.content.Intent intent = new android.content.Intent(getContext(), CommentsActivity.class);
                intent.putExtra(CommentsActivity.EXTRA_POST_ID, post.getId());
                startActivity(intent);
            }

            @Override
            public void onShare(Post post) {
                sharePost(post);
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
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_post, null, false);
        EditText etCaption = dialogView.findViewById(R.id.etPostCaption);
        ImageView imgPreview = dialogView.findViewById(R.id.imgPostPreview);
        TextView btnAddPhoto = dialogView.findViewById(R.id.btnAddPhoto);

        if (hasPreselectedImage && pendingImageUri != null) {
            imgPreview.setVisibility(View.VISIBLE);
            imgPreview.setImageURI(pendingImageUri);
        }

        btnAddPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        activeDialog = new AlertDialog.Builder(getContext())
                .setTitle("Create Post")
                .setView(dialogView)
                .setPositiveButton("Post", (d, which) -> {
                    String caption = etCaption.getText().toString().trim();
                    if ((caption.isEmpty()) && pendingImageUri == null) {
                        Toast.makeText(getContext(), "Please add a caption or photo.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createPost(caption, pendingImageUri);
                    pendingImageUri = null;
                    activePreview = null;
                    activeDialog = null;
                })
                .setNegativeButton("Cancel", (d, which) -> {
                    pendingImageUri = null;
                    activePreview = null;
                    activeDialog = null;
                    d.dismiss();
                })
                .create();
        activeDialog.show();
        activePreview = imgPreview;
    }

    private void createPost(String caption, Uri imageUri) {
        FirebaseHelper.getCurrentUserProfile(requireContext(), profile -> {
            if (imageUri != null) {
                uploadPostImageAndSave(profile, caption, imageUri, null);
            } else {
                savePost(profile, caption, null, null);
            }
        });
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
                FirebaseHelper.logUserActivity(requireContext(), "community_post_created");
                Toast.makeText(requireContext(), "Posted successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to post. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sharePost(Post original) {
        Map<String, Object> shared = new HashMap<>();
        shared.put("userId", original.getUserId());
        shared.put("userName", original.getUserName());
        shared.put("userPhotoUrl", original.getUserPhotoUrl());
        shared.put("caption", original.getCaption());
        shared.put("imageUrl", original.getImageUrl());
        shared.put("timestamp", original.getTimestamp());

        FirebaseHelper.getCurrentUserProfile(requireContext(), profile -> {
            savePost(profile, "", null, shared);
            FirebaseHelper.incrementShareCount(original.getId());
        });
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
