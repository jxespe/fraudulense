package com.example.fraudulens.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.fraudulens.R;

public final class CallAlertHelper {
    public static final String PREFS_CALL_ALERTS = "fraudulens_call_alerts";
    public static final String KEY_LAST_STATE = "last_state";
    public static final String KEY_LAST_NUMBER = "last_number";
    public static final String KEY_LAST_LEVEL = "last_level";

    public static final int LEVEL_NONE = 0;
    public static final int LEVEL_POSSIBLE = 1;
    public static final int LEVEL_SPAM = 2;

    private static final String CHANNEL_ID = "call_alerts";
    private static final int NOTIF_ID = 4107;
    private static final int NOTIF_SPEECH_ID = 4108;

    private CallAlertHelper() {}

    public static boolean hasPhonePermission(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasContactsPermission(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static String normalizeNumber(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("63") && digits.length() > 10) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    public static boolean isNumberInContacts(Context ctx, String number) {
        if (number == null || number.trim().isEmpty()) return false;
        if (!hasContactsPermission(ctx)) return false;
        ContentResolver resolver = ctx.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String[] projection = new String[]{ContactsContract.PhoneLookup._ID};
        try (android.database.Cursor cursor = resolver.query(uri, projection, null, null, null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    public static int incrementCount(Context ctx, String normalizedNumber) {
        if (normalizedNumber == null || normalizedNumber.trim().isEmpty()) return 0;
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_CALL_ALERTS, Context.MODE_PRIVATE);
        String key = "count_" + normalizedNumber;
        int count = prefs.getInt(key, 0) + 1;
        prefs.edit().putInt(key, count).apply();
        return count;
    }

    public static int getCount(Context ctx, String normalizedNumber) {
        if (normalizedNumber == null || normalizedNumber.trim().isEmpty()) return 0;
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_CALL_ALERTS, Context.MODE_PRIVATE);
        return prefs.getInt("count_" + normalizedNumber, 0);
    }

    public static void setLastCall(Context ctx, String number, int level) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_CALL_ALERTS, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_LAST_NUMBER, number)
                .putInt(KEY_LAST_LEVEL, level)
                .apply();
    }

    public static String getLastNumber(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_CALL_ALERTS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LAST_NUMBER, null);
    }

    public static int getLastLevel(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_CALL_ALERTS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_LAST_LEVEL, LEVEL_NONE);
    }

    public static void setLastState(Context ctx, String state) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_CALL_ALERTS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LAST_STATE, state).apply();
    }

    public static String getLastState(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_CALL_ALERTS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LAST_STATE, null);
    }

    public static void showAlert(Context ctx, int level, boolean ongoing) {
        ensureChannel(ctx);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        String title;
        String body;
        if (level == LEVEL_SPAM) {
            title = ctx.getString(R.string.call_alert_spam_title);
            body = ctx.getString(ongoing ? R.string.call_alert_ongoing_body : R.string.call_alert_spam_body);
        } else {
            title = ctx.getString(R.string.call_alert_possible_title);
            body = ctx.getString(ongoing ? R.string.call_alert_ongoing_body : R.string.call_alert_possible_body);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing);

        NotificationManagerCompat.from(ctx).notify(NOTIF_ID, builder.build());
    }

    public static void showSpeechAlert(Context ctx) {
        ensureChannel(ctx);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(ctx.getString(R.string.call_alert_speech_title))
                .setContentText(ctx.getString(R.string.call_alert_speech_body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true);
        NotificationManagerCompat.from(ctx).notify(NOTIF_SPEECH_ID, builder.build());
    }

    public static void cancelAlert(Context ctx) {
        NotificationManagerCompat.from(ctx).cancel(NOTIF_ID);
        NotificationManagerCompat.from(ctx).cancel(NOTIF_SPEECH_ID);
    }

    private static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);
        if (channel != null) return;
        channel = new NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.call_alerts_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(ctx.getString(R.string.call_alerts_channel_desc));
        nm.createNotificationChannel(channel);
    }
}
