package com.example.fraudulens;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.example.fraudulens.utils.ScamModelManager;

public class MyApp extends Application {
    private static FirebaseAnalytics analytics;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        analytics = FirebaseAnalytics.getInstance(this);
        ScamModelManager.prefetchLatestModel(this);

        SharedPreferences prefs = getSharedPreferences("fraudulens_settings", MODE_PRIVATE);
        String tag = prefs.getString("app_language_tag", "en");
        if (tag == null || tag.trim().isEmpty()) {
            tag = "en";
            prefs.edit().putString("app_language_tag", tag).apply();
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
    }

    public static FirebaseAnalytics getAnalytics() {
        return analytics;
    }
}
