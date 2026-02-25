package com.example.fraudulens.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageButton;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.models.Report;
import com.example.fraudulens.utils.ScamModelManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class ReportScamActivity extends AppCompatActivity {

    private EditText etReportDescription;
    private CheckBox cbAnonymous;
    private ImageView imgMediaPreview;
    private Uri selectedImageUri;
    private String ocrText;
    private ActivityResultLauncher<String> pickImageLauncher;
    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_scam);

        ImageButton btnBackNav = findViewById(R.id.btnBackNav);
        if (btnBackNav != null) {
            btnBackNav.setOnClickListener(v -> finish());
        }
        View tvViewReports = findViewById(R.id.tvViewReports);
        if (tvViewReports != null) {
            tvViewReports.setOnClickListener(v ->
                    startActivity(new android.content.Intent(this, ReportsActivity.class)));
        }

        etReportDescription = findViewById(R.id.etReportDescription);
        cbAnonymous = findViewById(R.id.cbAnonymous);
        imgMediaPreview = findViewById(R.id.imgMediaPreview);
        MaterialCardView cardAddPhoto = findViewById(R.id.cardAddPhoto);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            selectedImageUri = uri;
            imgMediaPreview.setVisibility(View.VISIBLE);
            imgMediaPreview.setImageURI(uri);
            analyzeImageText(uri);
        });

        if (cardAddPhoto != null) {
            cardAddPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        }

        findViewById(R.id.btnSubmitReport).setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        String description = etReportDescription.getText().toString().trim();
        if (description.isEmpty()) {
            etReportDescription.setError("Please provide a description");
            return;
        }

        String userId = cbAnonymous.isChecked()
                ? "anonymous"
                : (FirebaseHelper.getLoggedInEmail(this) != null ? FirebaseHelper.getLoggedInEmail(this) : "anonymous");

        if (selectedImageUri != null) {
            uploadImageThenSubmit(description, userId);
        } else {
            submitReportData(description, userId, null, null);
        }
    }

    private void analyzeImageText(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            textRecognizer.process(image)
                    .addOnSuccessListener(result -> {
                        String extracted = result.getText();
                        ocrText = extracted != null ? extracted.trim() : null;
                        if (ocrText != null && !ocrText.isEmpty()
                                && etReportDescription.getText().toString().trim().isEmpty()) {
                            etReportDescription.setText(ocrText);
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "OCR failed. Please try a clearer image.", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Toast.makeText(this, "Unable to read image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageThenSubmit(String description, String userId) {
        String safeUser = userId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String fileName = "reports/" + System.currentTimeMillis() + "_" + safeUser + ".jpg";
        StorageReference ref = FirebaseHelper.getStorageRoot().child(fileName);

        ref.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> submitReportData(description, userId, uri.toString(), ocrText))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Image upload failed. Try again.", Toast.LENGTH_SHORT).show());
    }

    private void submitReportData(String description, String userId, String imageUrl, String imageText) {
        Report report = new Report(
                userId,
                description,
                "Reported Scam",
                new Timestamp(new java.util.Date()),
                "open"
        );

        java.util.Map<String, Object> data = report.toMap();
        if (imageUrl != null) data.put("imageUrl", imageUrl);
        if (imageText != null && !imageText.trim().isEmpty()) {
            data.put("imageText", imageText.trim());
        }

        FirebaseHelper.addReport(data, success -> runOnUiThread(() -> {
            if (success) {
                FirebaseHelper.logUserActivity(this, "report_scam_submitted");
                ScamModelManager.forceRefreshModel(this);
                Toast.makeText(this, "✅ Report submitted successfully!", Toast.LENGTH_SHORT).show();
                etReportDescription.setText("");
                cbAnonymous.setChecked(false);
                imgMediaPreview.setVisibility(View.GONE);
                selectedImageUri = null;
                ocrText = null;
            } else {
                Toast.makeText(this, "❌ Failed to submit. Try again.", Toast.LENGTH_SHORT).show();
            }
        }));
    }
}
