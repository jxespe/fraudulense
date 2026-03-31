package com.example.fraudulens.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import com.example.fraudulens.FirebaseHelper;
import com.example.fraudulens.R;
import com.example.fraudulens.activities.LoginActivity;
import com.example.fraudulens.activities.PremiumActivity;
import com.example.fraudulens.activities.SettingsActivity;
import com.example.fraudulens.utils.AuthHelper;
import com.example.fraudulens.utils.PhoneFormatUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvPhone, tvVerifyStatus;
    private ImageView ivProfile;
    private LinearLayout llSettings, llLogout, llLinkedAccounts, llTerms, llPremium, llHelp;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;
    private String currentPhotoUrl;
    private ListenerRegistration userListener;
    private String currentProvider;
    private String currentEmail;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName = v.findViewById(R.id.tvName);
        tvPhone = v.findViewById(R.id.tvPhone);
        tvVerifyStatus = v.findViewById(R.id.tvVerifyStatus);
        ivProfile = v.findViewById(R.id.ivProfile);
        llSettings = v.findViewById(R.id.llSettings);
        llLogout = v.findViewById(R.id.llLogout);
        llLinkedAccounts = v.findViewById(R.id.llLinkedAccounts);
        llTerms = v.findViewById(R.id.llTerms);
        llPremium = v.findViewById(R.id.llPremium);
        llHelp = v.findViewById(R.id.llHelp);

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            startCrop(uri);
        });

        cropLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                Uri output = UCrop.getOutput(result.getData());
                if (output != null) {
                    uploadProfilePhoto(output);
                }
            } else if (result.getResultCode() == android.app.Activity.RESULT_CANCELED && result.getData() != null) {
                Throwable error = UCrop.getError(result.getData());
                if (error != null) {
                    android.util.Log.e("ProfileFragment", "Crop failed", error);
                }
            }
        });

        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        currentEmail = authUser != null ? authUser.getEmail() : FirebaseHelper.getLoggedInEmail(requireContext());

        if (authUser != null && authUser.getUid() != null) {
            userListener = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(authUser.getUid())
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            loadProfileFromLoginId();
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            applyProfileSnapshot(snapshot);
                        } else {
                            loadProfileFromLoginId();
                        }
                    });
        } else {
            loadProfileFromLoginId();
        }

        if (ivProfile != null) {
            ivProfile.setOnClickListener(view -> showProfilePhotoDialog());
        }

        if (llLinkedAccounts != null) {
            llLinkedAccounts.setOnClickListener(view -> showLinkedAccountsDialog());
        }

        llSettings.setOnClickListener(view -> {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });

        if (llTerms != null) {
            llTerms.setOnClickListener(view -> showTermsDialog());
        }

        if (llPremium != null) {
            llPremium.setOnClickListener(view -> {
                Intent intent = new Intent(getActivity(), PremiumActivity.class);
                intent.putExtra("email", FirebaseHelper.getLoggedInEmail(requireContext()));
                startActivity(intent);
            });
        }

        if (llHelp != null) {
            llHelp.setOnClickListener(view -> showHelpDialog());
        }

        llLogout.setOnClickListener(view -> {
            // Sign out from all providers (Firebase, Google, Facebook)
            AuthHelper.signOut(requireContext());

            Intent i = new Intent(getActivity(), LoginActivity.class);
            startActivity(i);
            requireActivity().finish();
        });

        return v;
    }

    private String formatPhilippinesPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return getString(R.string.profile_no_phone);
        }
        String local = PhoneFormatUtil.toLocal10(phone);
        if (local.length() == 10 && local.startsWith("9")) {
            return "+63 " + PhoneFormatUtil.formatLocal(local);
        }
        return phone;
    }

    private void applyProfileSnapshot(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String phone = doc.getString("phoneNumber");
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (phone == null || phone.trim().isEmpty()) {
            phone = authUser != null ? authUser.getPhoneNumber() : null;
        }
        Boolean isVerified = doc.getBoolean("isVerified");
        String photoUrl = doc.getString("photoUrl");
        currentProvider = doc.getString("provider");
        String email = authUser != null ? authUser.getEmail() : null;
        currentEmail = (email != null && !email.trim().isEmpty())
                ? email.toLowerCase(Locale.US)
                : FirebaseHelper.getLoggedInEmail(requireContext());
        if (phone == null || phone.trim().isEmpty()) {
            phone = authUser != null ? authUser.getPhoneNumber() : null;
            if (phone == null || phone.trim().isEmpty()) {
                phone = FirebaseHelper.getVerifiedPhone(requireContext());
            }
        }
        if (name == null || name.trim().isEmpty()) {
            name = authUser != null ? authUser.getDisplayName() : null;
        }
        if (name == null || name.trim().isEmpty() && currentEmail != null) {
            name = currentEmail.split("@")[0];
        }
        tvName.setText(name != null && !name.trim().isEmpty()
                ? name
                : getString(R.string.profile_unknown_user));
        tvPhone.setText(formatPhilippinesPhone(phone));
        if (tvVerifyStatus != null) {
            tvVerifyStatus.setText(Boolean.TRUE.equals(isVerified)
                    ? getString(R.string.profile_verified)
                    : getString(R.string.profile_not_verified));
        }
        if (ivProfile != null) {
            if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                currentPhotoUrl = photoUrl;
                Picasso.get().load(photoUrl).placeholder(R.drawable.ic_profile).into(ivProfile);
            } else {
                currentPhotoUrl = null;
                ivProfile.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    private void applyProfileFallback() {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        String name = authUser != null ? authUser.getDisplayName() : null;
        String email = authUser != null ? authUser.getEmail() : currentEmail;
        if (name == null || name.trim().isEmpty()) {
            name = email != null && email.contains("@") ? email.split("@")[0] : null;
        }
        String phone = authUser != null ? authUser.getPhoneNumber() : null;
        if (phone == null || phone.trim().isEmpty()) {
            phone = FirebaseHelper.getVerifiedPhone(requireContext());
        }

        tvName.setText(name != null && !name.trim().isEmpty()
                ? name
                : getString(R.string.profile_unknown_user));
        tvPhone.setText(formatPhilippinesPhone(phone));
        if (tvVerifyStatus != null) {
            tvVerifyStatus.setText(getString(R.string.profile_not_verified));
        }
        if (ivProfile != null) {
            ivProfile.setImageResource(R.drawable.ic_profile);
        }
        currentPhotoUrl = null;
        currentProvider = null;
        currentEmail = email != null ? email : FirebaseHelper.getLoggedInEmail(requireContext());
    }

    private void loadProfileFromLoginId() {
        String loginId = currentEmail != null ? currentEmail : FirebaseHelper.getLoggedInEmail(requireContext());
        if (loginId == null || loginId.trim().isEmpty()) {
            applyProfileFallback();
            return;
        }
        FirebaseHelper.getUserByLoginId(loginId, doc -> {
            if (doc != null && doc.exists()) {
                applyProfileSnapshot(doc);
            } else {
                applyProfileFallback();
            }
        });
    }

    private void showProfilePhotoDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_profile_photo, null);
        ImageView dialogImage = dialogView.findViewById(R.id.ivDialogProfile);
        View btnChange = dialogView.findViewById(R.id.btnDialogChangePhoto);

        if (dialogImage != null) {
            if (currentPhotoUrl != null && !currentPhotoUrl.trim().isEmpty()) {
                Picasso.get().load(currentPhotoUrl).placeholder(R.drawable.ic_profile).into(dialogImage);
            } else {
                dialogImage.setImageResource(R.drawable.ic_profile);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        if (btnChange != null) {
            btnChange.setOnClickListener(v -> {
                dialog.dismiss();
                pickImageLauncher.launch("image/*");
            });
        }
    }

    private void startCrop(Uri source) {
        Uri destination = Uri.fromFile(new File(requireContext().getCacheDir(),
                "cropped_profile_" + System.currentTimeMillis() + ".jpg"));
        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(85);
        options.setHideBottomControls(false);
        options.setFreeStyleCropEnabled(true);
        Intent intent = UCrop.of(source, destination)
                .withAspectRatio(1, 1)
                .withMaxResultSize(800, 800)
                .withOptions(options)
                .getIntent(requireContext());
        cropLauncher.launch(intent);
    }

    private void uploadProfilePhoto(Uri croppedUri) {
        String email = FirebaseHelper.getLoggedInEmail(requireContext());
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        String safeKey = uid != null && !uid.trim().isEmpty()
                ? uid.trim()
                : (email != null
                ? email.toLowerCase(Locale.US).replaceAll("[^a-z0-9_\\-]", "_")
                : String.valueOf(System.currentTimeMillis()));
        String path = "profile_photos/" + safeKey + ".jpg";
        StorageReference ref = FirebaseHelper.getStorageRoot().child(path);

        ref.putFile(croppedUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    String url = uri.toString();
                    FirebaseHelper.updateUserPhotoUrl(uid, url, ok -> {
                        currentPhotoUrl = url;
                        if (ivProfile != null) {
                            Picasso.get().load(url).placeholder(R.drawable.ic_profile).into(ivProfile);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        android.widget.Toast.makeText(
                                getContext(),
                                getString(R.string.profile_upload_failed),
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        super.onDestroyView();
    }

    private void showLinkedAccountsDialog() {
        boolean googleLinked = isProviderLinked("google.com");
        boolean facebookLinked = isProviderLinked("facebook.com");
        boolean appleLinked = isProviderLinked("apple.com");

        String linked = getString(R.string.linked_account_linked);
        String notLinked = getString(R.string.linked_account_not_linked);
        String[] items = new String[]{
                getString(R.string.linked_account_item_format, getString(R.string.linked_account_google), googleLinked ? linked : notLinked),
                getString(R.string.linked_account_item_format, getString(R.string.linked_account_facebook), facebookLinked ? linked : notLinked),
                getString(R.string.linked_account_item_format, getString(R.string.linked_account_apple), appleLinked ? linked : notLinked)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.linked_accounts_title)
                .setItems(items, null)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private boolean isProviderLinked(String providerId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            for (UserInfo info : user.getProviderData()) {
                if (providerId.equals(info.getProviderId())) {
                    return true;
                }
            }
        }
        if (currentProvider != null) {
            if ("google".equalsIgnoreCase(currentProvider) && "google.com".equals(providerId)) return true;
            if ("facebook".equalsIgnoreCase(currentProvider) && "facebook.com".equals(providerId)) return true;
            if ("apple".equalsIgnoreCase(currentProvider) && "apple.com".equals(providerId)) return true;
        }
        if ("google.com".equals(providerId) && currentProvider == null) {
            String email = currentEmail != null ? currentEmail.trim().toLowerCase(Locale.US) : null;
            if (email != null && email.endsWith("@gmail.com")) {
                return true;
            }
        }
        return false;
    }

    private void showTermsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_menu_terms)
                .setMessage(R.string.terms_placeholder_body)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void showHelpDialog() {
        String[] labels = new String[]{
                getString(R.string.help_category_profile_photo),
                getString(R.string.help_category_change_name),
                getString(R.string.help_category_premium),
                getString(R.string.help_category_likes)
        };
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.help_title)
                .setItems(labels, (dialog, which) -> {
                    if (which == 0) {
                        showHelpDetail(R.string.help_category_profile_photo, R.string.help_profile_photo_body);
                    } else if (which == 1) {
                        showHelpDetail(R.string.help_category_change_name, R.string.help_change_name_body);
                    } else if (which == 2) {
                        showHelpDetail(R.string.help_category_premium, R.string.help_premium_body);
                    } else if (which == 3) {
                        showHelpDetail(R.string.help_category_likes, R.string.help_likes_body);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showHelpDetail(int titleRes, int bodyRes) {
        new AlertDialog.Builder(requireContext())
                .setTitle(titleRes)
                .setMessage(bodyRes)
                .setPositiveButton(R.string.close, null)
                .show();
    }
}
