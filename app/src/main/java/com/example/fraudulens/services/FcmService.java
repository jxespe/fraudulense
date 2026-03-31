package com.example.fraudulens.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FcmService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "fraudulens_alerts";

    @Override
    public void onNewToken(String token) {
        FirebaseHelper.saveFcmToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String title = "FrauduLens Alert";
        String body = "You have a new notification.";
        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null) {
                title = message.getNotification().getTitle();
            }
            if (message.getNotification().getBody() != null) {
                body = message.getNotification().getBody();
            }
        } else if (!message.getData().isEmpty()) {
            if (message.getData().get("title") != null) {
                title = message.getData().get("title");
            }
            if (message.getData().get("body") != null) {
                body = message.getData().get("body");
            }
        }
        showNotification(title, body);
    }

    private void showNotification(String title, String body) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "FrauduLens Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_promo_scam)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
