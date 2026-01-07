package com.example.fraudulens.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.R;
import com.example.fraudulens.adapters.PostAdapter;
import com.example.fraudulens.models.Post;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class CommunityFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private ListenerRegistration registration;
    private FirebaseFirestore firestore;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);

        recyclerView = view.findViewById(R.id.recyclerCommunity);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        adapter = new PostAdapter(getContext());
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();

        listenToPosts();

        return view;
    }

    private void listenToPosts() {
        registration = firestore.collection("posts")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    List<Post> items = new ArrayList<>();
                    for (DocumentSnapshot d : snapshots.getDocuments()) {
                        Post p = d.toObject(Post.class);
                        if (p != null) items.add(p);
                    }

                    if (getActivity() != null)
                        getActivity().runOnUiThread(() -> adapter.updateList(items));
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) registration.remove();
    }
}
