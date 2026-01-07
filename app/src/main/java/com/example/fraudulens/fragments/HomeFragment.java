package com.example.fraudulens.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.google.firebase.firestore.*;

public class HomeFragment extends Fragment {
    TextView tvAlerts;
    ListenerRegistration reg;

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle b) {
        View v = inf.inflate(R.layout.fragment_home, c, false);
        tvAlerts = v.findViewById(R.id.tvAlerts);
        reg = FirebaseHelper.listenReports((snap, e) -> {
            if (snap == null) return;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (DocumentSnapshot d : snap.getDocuments()) {
                if (i++ >= 5) break;
                String msg = d.getString("message");
                String res = d.getString("result");
                sb.append(res).append(" • ").append(msg == null ? "" : (msg.length() > 40 ? msg.substring(0,40)+"…" : msg)).append("\n");
            }
            if (sb.length() == 0) sb.append("No recent alerts");
            final String out = sb.toString();
            if (getActivity() != null) getActivity().runOnUiThread(() -> tvAlerts.setText(out));
        });
        return v;
    }

    @Override
    public void onDestroyView() {
        if (reg != null) reg.remove();
        super.onDestroyView();
    }
}
