package com.example.fraudulens;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MyApp extends Application {
    private static FirebaseAnalytics analytics;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        analytics = FirebaseAnalytics.getInstance(this);
    }

    public static FirebaseAnalytics getAnalytics() {
        return analytics;
    }
}
