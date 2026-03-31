package com.example.fraudulens;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.example.fraudulens.utils.ScamModelManager;
import com.google.firebase.messaging.FirebaseMessaging;

public class MyApp extends Application {
    private static FirebaseAnalytics analytics;
    private static MyApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        FirebaseApp.initializeApp(this);
        analytics = FirebaseAnalytics.getInstance(this);
        ScamModelManager.prefetchLatestModel(this);
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> com.example.fraudulens.FirebaseHelper.saveFcmToken(this, token));

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

    public static MyApp getContext() {
        return instance;
    }
}
