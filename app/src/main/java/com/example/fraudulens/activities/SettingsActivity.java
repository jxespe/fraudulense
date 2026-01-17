package com.example.fraudulens.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fraudulens.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> onBackPressed());
    }
}
