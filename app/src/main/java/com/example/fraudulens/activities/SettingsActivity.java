package com.example.fraudulens.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;

import com.example.fraudulens.R;
import com.example.fraudulens.activities.PinSetupActivity;
import com.example.fraudulens.FirebaseHelper;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS = "fraudulens_settings";
    private static final String KEY_PHONE_ACCESS = "perm_phone_access";
    private static final String KEY_SMS_ACCESS = "perm_sms_access";
    private static final String KEY_EMAIL_ACCESS = "perm_email_access";
    private static final String KEY_CONTACTS_ACCESS = "perm_contacts_access";
    private static final String KEY_APP_LANGUAGE = "app_language_tag";
    private static final String KEY_REQUIRE_PIN = "security_require_pin";
    private static final String KEY_SECURITY_ALERTS = "security_alerts";
    private static final String KEY_NOTIF_PUSH = "notif_push";
    private static final String KEY_NOTIF_COMMUNITY = "notif_community";
    private static final String KEY_NOTIF_REPORTS = "notif_reports";
    private static final String KEY_NOTIF_SCAM = "notif_scam";
    private static final String KEY_NOTIF_EMAIL = "notif_email";

    private SwitchMaterial switchPhone;
    private SwitchMaterial switchSms;
    private SwitchMaterial switchEmail;
    private SwitchMaterial switchContacts;
    private SwitchMaterial switchRequirePin;
    private SwitchMaterial switchSecurityAlerts;
    private SwitchMaterial switchNotifPush;
    private SwitchMaterial switchNotifCommunity;
    private SwitchMaterial switchNotifReports;
    private SwitchMaterial switchNotifScam;
    private SwitchMaterial switchNotifEmail;
    private ActivityResultLauncher<String> phonePermLauncher;
    private ActivityResultLauncher<String[]> smsPermLauncher;
    private ActivityResultLauncher<String> contactsPermLauncher;
    private boolean ignoreSwitchChange;
    private SharedPreferences prefs;
    private TextView tvAppLanguageValue;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;
    private String userDocId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> onBackPressed());

        View appLanguageRow = findViewById(R.id.layoutAppLanguage);
        tvAppLanguageValue = findViewById(R.id.tvAppLanguageValue);

        View permissionsHeader = findViewById(R.id.layoutPermissionsHeader);
        View permissionsItems = findViewById(R.id.layoutPermissionsItems);
        ImageView permissionsToggle = findViewById(R.id.ivPermissionsToggle);
        View securityHeader = findViewById(R.id.layoutSecurityHeader);
        View securityItems = findViewById(R.id.layoutSecurityItems);
        ImageView securityToggle = findViewById(R.id.ivSecurityToggle);
        View notificationsHeader = findViewById(R.id.layoutNotificationsHeader);
        View notificationsItems = findViewById(R.id.layoutNotificationsItems);
        ImageView notificationsToggle = findViewById(R.id.ivNotificationsToggle);
        View rowChangePin = findViewById(R.id.rowChangePin);

        setupSectionToggle(permissionsHeader, permissionsItems, permissionsToggle);
        setupSectionToggle(securityHeader, securityItems, securityToggle);
        setupSectionToggle(notificationsHeader, notificationsItems, notificationsToggle);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        switchPhone = findViewById(R.id.switchPhone);
        switchSms = findViewById(R.id.switchSms);
        switchEmail = findViewById(R.id.switchEmail);
        switchContacts = findViewById(R.id.switchContacts);
        switchRequirePin = findViewById(R.id.switchRequirePin);
        switchSecurityAlerts = findViewById(R.id.switchSecurityAlerts);
        switchNotifPush = findViewById(R.id.switchNotifPush);
        switchNotifCommunity = findViewById(R.id.switchNotifCommunity);
        switchNotifReports = findViewById(R.id.switchNotifReports);
        switchNotifScam = findViewById(R.id.switchNotifScam);
        switchNotifEmail = findViewById(R.id.switchNotifEmail);

        phonePermLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            handlePermissionResult(switchPhone, KEY_PHONE_ACCESS, granted);
        });
        smsPermLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean granted = true;
            for (Boolean value : result.values()) {
                granted = granted && Boolean.TRUE.equals(value);
            }
            handlePermissionResult(switchSms, KEY_SMS_ACCESS, granted);
        });
        contactsPermLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            handlePermissionResult(switchContacts, KEY_CONTACTS_ACCESS, granted);
        });

        initPermissionSwitches();
        initSecurityAndNotificationSwitches();
        initLanguageSelector(appLanguageRow);
        attachUserSettingsListener();

        if (rowChangePin != null) {
            rowChangePin.setOnClickListener(v -> startActivity(new Intent(this, PinSetupActivity.class)));
        }
    }

    private void initLanguageSelector(View appLanguageRow) {
        if (appLanguageRow == null) return;
        updateLanguageValue();
        appLanguageRow.setOnClickListener(v -> showLanguageDialog());
    }

    private void showLanguageDialog() {
        String[] labels = getResources().getStringArray(R.array.app_language_labels);
        String[] tags = getResources().getStringArray(R.array.app_language_tags);
        String currentTag = getSavedLanguageTag();
        int selected = 0;
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].equalsIgnoreCase(currentTag)) {
                selected = i;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_language)
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    String tag = tags[which];
                    prefs.edit().putString(KEY_APP_LANGUAGE, tag).apply();
                    updateRemoteSetting(KEY_APP_LANGUAGE, tag);
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
                    updateLanguageValue();
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateLanguageValue() {
        if (tvAppLanguageValue == null) return;
        String[] labels = getResources().getStringArray(R.array.app_language_labels);
        String[] tags = getResources().getStringArray(R.array.app_language_tags);
        String currentTag = getSavedLanguageTag();
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].equalsIgnoreCase(currentTag)) {
                tvAppLanguageValue.setText(labels[i]);
                return;
            }
        }
        if (labels.length > 0) {
            tvAppLanguageValue.setText(labels[0]);
        }
    }

    private String getSavedLanguageTag() {
        String saved = prefs.getString(KEY_APP_LANGUAGE, "en");
        if (saved == null || saved.trim().isEmpty()) {
            return "en";
        }
        return saved;
    }

    private void initPermissionSwitches() {
        initSwitch(switchPhone, KEY_PHONE_ACCESS, new String[]{Manifest.permission.READ_PHONE_STATE});
        initSwitch(switchSms, KEY_SMS_ACCESS, new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS});
        initSwitch(switchContacts, KEY_CONTACTS_ACCESS, new String[]{Manifest.permission.READ_CONTACTS});
        initSwitch(switchEmail, KEY_EMAIL_ACCESS, null);
    }

    private void initSecurityAndNotificationSwitches() {
        initPrefSwitch(switchRequirePin, KEY_REQUIRE_PIN, true);
        initPrefSwitch(switchSecurityAlerts, KEY_SECURITY_ALERTS, true);
        initPrefSwitch(switchNotifPush, KEY_NOTIF_PUSH, true);
        initPrefSwitch(switchNotifCommunity, KEY_NOTIF_COMMUNITY, true);
        initPrefSwitch(switchNotifReports, KEY_NOTIF_REPORTS, true);
        initPrefSwitch(switchNotifScam, KEY_NOTIF_SCAM, true);
        initPrefSwitch(switchNotifEmail, KEY_NOTIF_EMAIL, false);
    }

    private void initSwitch(SwitchMaterial sw, String key, String[] perms) {
        if (sw == null) return;
        boolean granted = perms == null || hasPermissions(perms);
        boolean enabled = prefs.getBoolean(key, true);
        setSwitchChecked(sw, enabled && granted);

        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ignoreSwitchChange) return;
            if (!isChecked) {
                prefs.edit().putBoolean(key, false).apply();
                updateRemoteSetting(key, false);
                return;
            }
            if (perms == null) {
                prefs.edit().putBoolean(key, true).apply();
                updateRemoteSetting(key, true);
                Toast.makeText(this, getString(R.string.settings_email_access_enabled), Toast.LENGTH_SHORT).show();
                return;
            }
            if (hasPermissions(perms)) {
                prefs.edit().putBoolean(key, true).apply();
                updateRemoteSetting(key, true);
            } else {
                setSwitchChecked(sw, false);
                requestPermissionsForSwitch(key, perms);
            }
        });
    }

    private void initPrefSwitch(SwitchMaterial sw, String key, boolean defaultValue) {
        if (sw == null) return;
        boolean enabled = prefs.getBoolean(key, defaultValue);
        setSwitchChecked(sw, enabled);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ignoreSwitchChange) return;
            prefs.edit().putBoolean(key, isChecked).apply();
            updateRemoteSetting(key, isChecked);
        });
    }

    private void setupSectionToggle(View header, View content, ImageView icon) {
        if (header == null || content == null || icon == null) return;
        icon.setRotation(content.getVisibility() == View.VISIBLE ? 180f : 0f);
        header.setOnClickListener(v -> {
            boolean isVisible = content.getVisibility() == View.VISIBLE;
            content.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            icon.setRotation(isVisible ? 0f : 180f);
        });
    }

    private void requestPermissionsForSwitch(String key, String[] perms) {
        if (KEY_PHONE_ACCESS.equals(key) && phonePermLauncher != null) {
            phonePermLauncher.launch(Manifest.permission.READ_PHONE_STATE);
        } else if (KEY_SMS_ACCESS.equals(key) && smsPermLauncher != null) {
            smsPermLauncher.launch(new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS});
        } else if (KEY_CONTACTS_ACCESS.equals(key) && contactsPermLauncher != null) {
            contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void handlePermissionResult(SwitchMaterial sw, String key, boolean granted) {
        prefs.edit().putBoolean(key, granted).apply();
        updateRemoteSetting(key, granted);
        if (sw != null) {
            setSwitchChecked(sw, granted);
        }
        if (!granted) {
            Toast.makeText(this, getString(R.string.settings_permission_denied), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasPermissions(String[] perms) {
        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void setSwitchChecked(SwitchMaterial sw, boolean checked) {
        ignoreSwitchChange = true;
        sw.setChecked(checked);
        ignoreSwitchChange = false;
    }

    private void attachUserSettingsListener() {
        String email = FirebaseHelper.getLoggedInEmail(this);
        if (email == null || email.trim().isEmpty()) {
            return;
        }
        db = FirebaseFirestore.getInstance();
        userListener = db.collection("users")
                .whereEqualTo("email", email.toLowerCase())
                .limit(1)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || snapshot.isEmpty()) {
                        return;
                    }
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    userDocId = doc.getId();
                    applyRemoteSettings(doc);
                });
    }

    private void applyRemoteSettings(DocumentSnapshot doc) {
        ignoreSwitchChange = true;
        try {
            applyRemoteBoolean(doc, "settings.permissions.phoneAccess", switchPhone, KEY_PHONE_ACCESS);
            applyRemoteBoolean(doc, "settings.permissions.smsAccess", switchSms, KEY_SMS_ACCESS);
            applyRemoteBoolean(doc, "settings.permissions.emailAccess", switchEmail, KEY_EMAIL_ACCESS);
            applyRemoteBoolean(doc, "settings.permissions.contactsAccess", switchContacts, KEY_CONTACTS_ACCESS);

            applyRemoteBoolean(doc, "settings.security.requirePin", switchRequirePin, KEY_REQUIRE_PIN);
            applyRemoteBoolean(doc, "settings.security.alerts", switchSecurityAlerts, KEY_SECURITY_ALERTS);

            applyRemoteBoolean(doc, "settings.notifications.push", switchNotifPush, KEY_NOTIF_PUSH);
            applyRemoteBoolean(doc, "settings.notifications.community", switchNotifCommunity, KEY_NOTIF_COMMUNITY);
            applyRemoteBoolean(doc, "settings.notifications.reports", switchNotifReports, KEY_NOTIF_REPORTS);
            applyRemoteBoolean(doc, "settings.notifications.scam", switchNotifScam, KEY_NOTIF_SCAM);
            applyRemoteBoolean(doc, "settings.notifications.email", switchNotifEmail, KEY_NOTIF_EMAIL);
        } finally {
            ignoreSwitchChange = false;
        }

        String remoteTag = doc.getString("settings.appLanguageTag");
        if (remoteTag != null && !remoteTag.trim().isEmpty()) {
            String currentTag = getSavedLanguageTag();
            if (!remoteTag.equalsIgnoreCase(currentTag)) {
                prefs.edit().putString(KEY_APP_LANGUAGE, remoteTag).apply();
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(remoteTag));
                updateLanguageValue();
                recreate();
            }
        }
    }

    private void applyRemoteBoolean(DocumentSnapshot doc, String path, SwitchMaterial sw, String prefKey) {
        Boolean remote = doc.getBoolean(path);
        if (remote == null || sw == null) {
            return;
        }
        prefs.edit().putBoolean(prefKey, remote).apply();
        sw.setChecked(remote);
    }

    private void updateRemoteSetting(String key, Object value) {
        String path = getRemotePathForKey(key);
        if (path == null || value == null) return;
        String email = FirebaseHelper.getLoggedInEmail(this);
        if (email == null || email.trim().isEmpty()) return;
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        if (userDocId != null) {
            db.collection("users")
                    .document(userDocId)
                    .update(path, value);
            return;
        }
        db.collection("users")
                .whereEqualTo("email", email.toLowerCase())
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) return;
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);
                    userDocId = doc.getId();
                    db.collection("users")
                            .document(userDocId)
                            .update(path, value);
                });
    }

    private String getRemotePathForKey(String key) {
        if (KEY_PHONE_ACCESS.equals(key)) return "settings.permissions.phoneAccess";
        if (KEY_SMS_ACCESS.equals(key)) return "settings.permissions.smsAccess";
        if (KEY_EMAIL_ACCESS.equals(key)) return "settings.permissions.emailAccess";
        if (KEY_CONTACTS_ACCESS.equals(key)) return "settings.permissions.contactsAccess";
        if (KEY_REQUIRE_PIN.equals(key)) return "settings.security.requirePin";
        if (KEY_SECURITY_ALERTS.equals(key)) return "settings.security.alerts";
        if (KEY_NOTIF_PUSH.equals(key)) return "settings.notifications.push";
        if (KEY_NOTIF_COMMUNITY.equals(key)) return "settings.notifications.community";
        if (KEY_NOTIF_REPORTS.equals(key)) return "settings.notifications.reports";
        if (KEY_NOTIF_SCAM.equals(key)) return "settings.notifications.scam";
        if (KEY_NOTIF_EMAIL.equals(key)) return "settings.notifications.email";
        if (KEY_APP_LANGUAGE.equals(key)) return "settings.appLanguageTag";
        return null;
    }

    @Override
    protected void onDestroy() {
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        super.onDestroy();
    }
}
