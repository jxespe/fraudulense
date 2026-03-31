package com.example.fraudulens.services;

import android.telecom.Call;
import android.telecom.CallScreeningService;

import com.example.fraudulens.utils.CallAlertHelper;

public class FrauduCallScreeningService extends CallScreeningService {
    @Override
    public void onScreenCall(Call.Details callDetails) {
        String number = callDetails.getHandle() != null ? callDetails.getHandle().getSchemeSpecificPart() : null;
        String normalized = CallAlertHelper.normalizeNumber(number);
        boolean inContacts = CallAlertHelper.isNumberInContacts(this, number);
        if (!inContacts && normalized != null && !normalized.trim().isEmpty()) {
            int count = CallAlertHelper.incrementCount(this, normalized);
            int level = count >= 4 ? CallAlertHelper.LEVEL_SPAM : CallAlertHelper.LEVEL_POSSIBLE;
            CallAlertHelper.setLastCall(this, normalized, level);
            CallAlertHelper.showAlert(this, level, false);
        }

        CallResponse response = new CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build();
        respondToCall(callDetails, response);
    }
}
