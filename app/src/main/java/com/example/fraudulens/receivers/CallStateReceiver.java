package com.example.fraudulens.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.example.fraudulens.services.CallSpeechService;
import com.example.fraudulens.utils.CallAlertHelper;

public class CallStateReceiver extends BroadcastReceiver {
    private static final String PREFS = "fraudulens_settings";
    private static final String KEY_CALL_SPEECH = "security_call_speech_detection";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) return;

        if (!CallAlertHelper.hasPhonePermission(context)) {
            return;
        }

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (state == null) return;

        String lastState = CallAlertHelper.getLastState(context);
        if (state.equals(lastState)) {
            return;
        }
        CallAlertHelper.setLastState(context, state);

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            String normalized = CallAlertHelper.normalizeNumber(number);
            if (normalized == null || normalized.trim().isEmpty()) return;

            boolean inContacts = CallAlertHelper.isNumberInContacts(context, number);
            if (inContacts) {
                CallAlertHelper.setLastCall(context, normalized, CallAlertHelper.LEVEL_NONE);
                return;
            }

            int count = CallAlertHelper.incrementCount(context, normalized);
            int level = count >= 4 ? CallAlertHelper.LEVEL_SPAM : CallAlertHelper.LEVEL_POSSIBLE;
            CallAlertHelper.setLastCall(context, normalized, level);
            CallAlertHelper.showAlert(context, level, false);
            return;
        }

        if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            int level = CallAlertHelper.getLastLevel(context);
            if (level != CallAlertHelper.LEVEL_NONE) {
                CallAlertHelper.showAlert(context, level, true);
            }
            boolean speechEnabled = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getBoolean(KEY_CALL_SPEECH, false);
            String lastNumber = CallAlertHelper.getLastNumber(context);
            if (speechEnabled && lastNumber != null && !lastNumber.trim().isEmpty()) {
                boolean inContacts = CallAlertHelper.isNumberInContacts(context, lastNumber);
                if (!inContacts) {
                    CallSpeechService.start(context, lastNumber);
                }
            }
            return;
        }

        if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            CallAlertHelper.cancelAlert(context);
            CallSpeechService.stop(context);
        }
    }
}
