# OAuth & OTP Integration Setup Guide

## ✅ What's Been Integrated

### 1. Google OAuth (Sign-In)
- **Login Screen**: Added "Continue with Google" button
- **Register Screen**: Added "Sign up with Google" button
- **Backend**: Fully wired up in `LoginActivity.java` and `RegisterActivity.java`
- **Helper Class**: `AuthHelper.java` handles all Google Sign-In logic

### 2. OTP Using Google (Firebase Phone Auth)
- **Already Integrated**: Your app already uses Firebase Phone Authentication
- **Flow**: 
  - `PhoneVerificationActivity` → Sends OTP via Firebase
  - `OtpActivity` → Verifies OTP code
  - `FirebaseUtils.java` → Handles Firebase Phone Auth API calls

## 🔧 Configuration Required

### Step 1: Get Google Web Client ID

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click **⚙️ Project Settings** (gear icon)
4. Go to **Your apps** tab
5. Find your **Web app** (or create one if it doesn't exist)
6. Copy the **Web client ID** (looks like: `123456789-abcdefghijklmnop.apps.googleusercontent.com`)

### Step 2: Update strings.xml

Open `app/src/main/res/values/strings.xml` and replace:

```xml
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID</string>
```

With your actual Web Client ID:

```xml
<string name="default_web_client_id">123456789-abcdefghijklmnop.apps.googleusercontent.com</string>
```

### Step 3: Enable Google Sign-In in Firebase

1. In Firebase Console, go to **Authentication**
2. Click **Sign-in method** tab
3. Enable **Google** provider
4. Add your **Support email**
5. Save

### Step 4: Enable Phone Authentication in Firebase

1. In Firebase Console, go to **Authentication**
2. Click **Sign-in method** tab
3. Enable **Phone** provider
4. Save

### Step 5: Add SHA-1 Fingerprint (for Google Sign-In)

1. Get your app's SHA-1 fingerprint:
   ```bash
   # Windows (PowerShell)
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   
   # Or for release keystore
   keytool -list -v -keystore your-release-key.keystore -alias your-key-alias
   ```

2. Copy the SHA-1 fingerprint (looks like: `AA:BB:CC:DD:EE:FF:...`)

3. In Firebase Console:
   - Go to **Project Settings** → **Your apps**
   - Find your **Android app**
   - Click **Add fingerprint**
   - Paste your SHA-1
   - Download the updated `google-services.json`
   - Replace `app/google-services.json` with the new file

## 🎨 UI Customization (Optional)

The Google Sign-In buttons currently use a placeholder icon. To improve:

1. **Add Google Icon**: Create a vector drawable for the Google "G" logo
2. **Update Layouts**: Replace `app:icon="@android:drawable/ic_menu_search"` in:
   - `activity_login.xml` (line ~260)
   - `activity_register.xml` (line ~360)

## 📱 How It Works

### Google OAuth Flow:
1. User clicks "Continue with Google" or "Sign up with Google"
2. Google Sign-In dialog appears
3. User selects Google account
4. `AuthHelper` authenticates with Firebase
5. User data is saved to Firestore
6. User is redirected to `PhoneVerificationActivity` (if new user) or `PremiumActivity` (if existing)

### OTP Flow (Already Working):
1. User enters phone number in `PhoneVerificationActivity`
2. Firebase sends OTP via SMS
3. User enters OTP in `OtpActivity`
4. Firebase verifies OTP
5. Phone number is saved to user profile
6. User is redirected to `PremiumActivity`

## 🐛 Troubleshooting

### Google Sign-In Not Working:
- ✅ Check `default_web_client_id` in `strings.xml`
- ✅ Verify SHA-1 fingerprint is added in Firebase
- ✅ Ensure Google Sign-In is enabled in Firebase Console
- ✅ Check `google-services.json` is up to date

### OTP Not Sending:
- ✅ Verify Phone Authentication is enabled in Firebase
- ✅ Check phone number format (should include country code, e.g., `+63...`)
- ✅ Ensure Firebase Phone Auth quota is not exceeded
- ✅ Check device has SMS permissions (if required)

## 📚 Additional Resources

- [Firebase Authentication Docs](https://firebase.google.com/docs/auth)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start)
- [Firebase Phone Auth](https://firebase.google.com/docs/auth/android/phone-auth)
