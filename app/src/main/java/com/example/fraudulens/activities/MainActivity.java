package com.example.fraudulens.activities;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.fraudulens.R;
import com.example.fraudulens.fragments.CommunityFragment;
import com.example.fraudulens.fragments.HomeFragment;
import com.example.fraudulens.fragments.ProfileFragment;
import com.example.fraudulens.fragments.ScanFragment;
import com.example.fraudulens.fragments.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);

        // ✅ Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        // ✅ Handle bottom navigation clicks
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_search) {
                selectedFragment = new SearchFragment();
            } else if (itemId == R.id.nav_favorites) {
                selectedFragment = new CommunityFragment(); // T3 UI - Scam Feed
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }

            return false;
        });
    }

    // ✅ Helper function to load fragments cleanly
    private void loadFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.container, fragment)
                .commit();
    }
}
