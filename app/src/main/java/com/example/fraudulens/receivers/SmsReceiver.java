package com.example.fraudulens.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.example.fraudulens.utils.ScamDetector;
import com.example.fraudulens.FirebaseHelper;

public class SmsReceiver extends BroadcastReceiver {

    public static final String ACTION_SCAM_SMS = "com.example.fraudulens.SCAM_SMS";
    public static final String EXTRA_BODY = "body";
    public static final String EXTRA_DATE = "date";
    public static final String EXTRA_ADDRESS = "address";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) return;

        StringBuilder bodyBuilder = new StringBuilder();
        long timestamp = 0L;
        String address = null;
        for (SmsMessage message : messages) {
            bodyBuilder.append(message.getMessageBody());
            timestamp = Math.max(timestamp, message.getTimestampMillis());
            if (address == null) {
                address = message.getOriginatingAddress();
            }
        }

        String body = bodyBuilder.toString();
        if (com.example.fraudulens.FirebaseHelper.isTrustedMessage(context, address, body)) {
            return;
        }
        if (!ScamDetector.isScam(context, body)) {
            return;
        }

        FirebaseHelper.saveDetectedScamMessage(context, address, body, timestamp);
        Intent broadcast = new Intent(ACTION_SCAM_SMS);
        broadcast.setPackage(context.getPackageName());
        broadcast.putExtra(EXTRA_BODY, body);
        broadcast.putExtra(EXTRA_DATE, timestamp);
        broadcast.putExtra(EXTRA_ADDRESS, address);
        context.sendBroadcast(broadcast);
    }
}
