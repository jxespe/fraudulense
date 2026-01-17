package com.example.fraudulens.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.adapters.TrustedContactAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrustedContactsActivity extends AppCompatActivity {

    private static final int REQ_READ_CONTACTS = 3001;

    private RecyclerView rvContacts;
    private TextView tvEmpty;
    private TrustedContactAdapter adapter;
    private final List<TrustedContactAdapter.ContactItem> items = new ArrayList<>();
    private Set<String> trustedNumbers;
    private Set<String> trustedNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_contacts);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvContacts = findViewById(R.id.rvContacts);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvContacts.setLayoutManager(new LinearLayoutManager(this));

        trustedNumbers = FirebaseHelper.getTrustedNumbers(this);
        trustedNames = FirebaseHelper.getTrustedNames(this);

        adapter = new TrustedContactAdapter(items, trustedNumbers, (item, isChecked) -> {
            String safeName = item.name == null ? "" : item.name.trim().toLowerCase();
            if (isChecked) {
                trustedNumbers.add(item.number);
                if (!safeName.isEmpty()) {
                    trustedNames.add(safeName);
                }
            } else {
                trustedNumbers.remove(item.number);
                if (!safeName.isEmpty()) {
                    trustedNames.remove(safeName);
                }
            }
            FirebaseHelper.saveTrustedContacts(this, trustedNumbers, trustedNames);
        });
        rvContacts.setAdapter(adapter);

        if (hasContactsPermission()) {
            loadContacts();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQ_READ_CONTACTS);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private boolean hasContactsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private void loadContacts() {
        items.clear();
        Map<String, TrustedContactAdapter.ContactItem> unique = new LinkedHashMap<>();
        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String normalizedNumber = FirebaseHelper.normalizePhoneNumber(number);
                    if (normalizedNumber.isEmpty()) continue;
                    if (!unique.containsKey(normalizedNumber)) {
                        String safeName = name == null ? "Unknown" : name.trim();
                        unique.put(normalizedNumber, new TrustedContactAdapter.ContactItem(safeName, normalizedNumber));
                    }
                }
            } finally {
                cursor.close();
            }
        }

        items.addAll(unique.values());
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(items.isEmpty() ? TextView.VISIBLE : TextView.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            } else {
                Toast.makeText(this, "Contacts permission is required to manage trusted contacts.", Toast.LENGTH_LONG).show();
                tvEmpty.setVisibility(TextView.VISIBLE);
            }
        }
    }
}
