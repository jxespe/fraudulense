package com.example.fraudulens.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.fraudulens.R;
import com.example.fraudulens.utils.CallAlertHelper;

import java.util.ArrayList;
import java.util.Locale;

public class CallSpeechService extends Service {
    private static final String ACTION_START = "com.example.fraudulens.action.CALL_SPEECH_START";
    private static final String ACTION_STOP = "com.example.fraudulens.action.CALL_SPEECH_STOP";
    private static final String CHANNEL_ID = "call_speech";
    private static final int NOTIF_ID = 4410;

    private SpeechRecognizer recognizer;
    private boolean isListening;
    private AudioManager audioManager;
    private Integer prevMode;
    private Boolean prevSpeaker;

    public static void start(Context context, String number) {
        Intent intent = new Intent(context, CallSpeechService.class);
        intent.setAction(ACTION_START);
        intent.putExtra("number", number);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, CallSpeechService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopListening();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIF_ID, buildNotification());
        enableSpeakerphoneBestEffort();
        startListening();
        return START_STICKY;
    }

    private void startListening() {
        if (isListening) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf();
            return;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(android.os.Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                restartListening();
            }
            @Override public void onResults(android.os.Bundle results) {
                handleResults(results);
                restartListening();
            }
            @Override public void onPartialResults(android.os.Bundle partialResults) {
                handleResults(partialResults);
            }
            @Override public void onEvent(int eventType, android.os.Bundle params) {}
        });
        isListening = true;
        recognizer.startListening(buildRecognizerIntent());
    }

    private void restartListening() {
        if (recognizer == null) return;
        try {
            recognizer.cancel();
        } catch (Exception ignored) {}
        recognizer.startListening(buildRecognizerIntent());
    }

    private void handleResults(android.os.Bundle results) {
        if (results == null) return;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) return;
        String text = matches.get(0);
        if (text == null) return;
        String normalized = text.toLowerCase(Locale.US);
        updateNotification(text);
        if (containsScamKeyword(normalized)) {
            CallAlertHelper.showSpeechAlert(this);
        }
    }

    private boolean containsScamKeyword(String text) {
        if (TextUtils.isEmpty(text)) return false;
        String[] keywords = new String[]{
                "otp", "one time password", "pin", "bank", "account",
                "verify", "verification", "send money", "transfer",
                "payment", "urgent", "immediately", "prize", "lottery",
                "login", "password", "gcash", "wallet"
        };
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    private Intent buildRecognizerIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        return intent;
    }

    private Notification buildNotification() {
        ensureChannel();
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.call_speech_service_title))
                .setContentText(getString(R.string.call_speech_service_body))
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String transcript) {
        String text = transcript == null ? "" : transcript.trim();
        if (text.length() > 80) {
            text = text.substring(0, 80) + "…";
        }
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.call_speech_service_title))
                .setContentText(text.isEmpty() ? getString(R.string.call_speech_service_body) : text)
                .setOngoing(true)
                .build();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID, notification);
        }
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);
        if (channel != null) return;
        channel = new NotificationChannel(CHANNEL_ID, "Call speech", NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(channel);
    }

    private void stopListening() {
        isListening = false;
        if (recognizer != null) {
            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (Exception ignored) {}
        }
        recognizer = null;
        restoreAudioState();
    }

    private void enableSpeakerphoneBestEffort() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;
        prevMode = audioManager.getMode();
        prevSpeaker = audioManager.isSpeakerphoneOn();
        try {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(true);
        } catch (Exception ignored) {}
    }

    private void restoreAudioState() {
        if (audioManager == null) return;
        try {
            if (prevMode != null) {
                audioManager.setMode(prevMode);
            }
            if (prevSpeaker != null) {
                audioManager.setSpeakerphoneOn(prevSpeaker);
            }
        } catch (Exception ignored) {}
        audioManager = null;
        prevMode = null;
        prevSpeaker = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
