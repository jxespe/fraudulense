# OAuth Setup - Step by Step Guide

Follow these steps in order to configure Google Sign-In and Facebook Login.

## 🔵 PART 1: Google Sign-In Setup

### Step 1: Get Web Client ID from Firebase

1. **Go to Firebase Console:**
   - Visit: https://console.firebase.google.com/
   - Sign in with your Google account
   - Select your project (or create a new one)

2. **Get Web Client ID:**
   - Click the ⚙️ **Settings** icon (gear) → **Project settings**
   - Scroll down to **Your apps** section
   - Look for your **Web app** (or click **Add app** → **Web** if you don't have one)
   - In the Web app configuration, find **Web client ID**
   - It looks like: `123456789-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com`
   - **COPY THIS VALUE** - you'll need it in Step 3

3. **Enable Google Sign-In in Firebase:**
   - In Firebase Console, go to **Authentication** (left sidebar)
   - Click **Sign-in method** tab
   - Find **Google** in the list
   - Click on it → **Enable**
   - Add a **Support email** (your email)
   - Click **Save**

4. **Add SHA-1 Fingerprint (IMPORTANT!):**
   
   **Get your SHA-1 fingerprint:**
   
   **Windows (PowerShell):**
   ```powershell
   keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```
   
   **Windows (Command Prompt):**
   ```cmd
   keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
   
   **Mac/Linux:**
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
   
   **Look for this line in the output:**
   ```
   SHA1: AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE
   ```
   
   **Copy the SHA1 value (without spaces if needed)**
   
   **Add to Firebase:**
   - Firebase Console → **Project Settings** → **Your apps**
   - Find your **Android app** (package: `com.example.fraudulens`)
   - Click **Add fingerprint**
   - Paste your SHA-1 fingerprint
   - Click **Save**
   - **Download the updated `google-services.json`**
   - Replace `app/google-services.json` with the new file

### Step 2: Update strings.xml

Open `app/src/main/res/values/strings.xml` and replace:

```xml
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID</string>
```

With your actual Web Client ID:

```xml
<string name="default_web_client_id">123456789-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com</string>
```

---

## 🔵 PART 2: Facebook Login Setup

### Step 1: Create Facebook App

1. **Go to Facebook Developers:**
   - Visit: https://developers.facebook.com/
   - Sign in with your Facebook account
   - Click **My Apps** → **Create App**

2. **Choose App Type:**
   - Select **Consumer** or **Business**
   - Click **Next**

3. **Fill App Details:**
   - **App Name:** FrauduLens (or your preferred name)
   - **App Contact Email:** Your email
   - Click **Create App**

### Step 2: Get App ID and Client Token

1. **Get App ID:**
   - In your Facebook App dashboard
   - Go to **Settings** → **Basic**
   - Find **App ID** - **COPY THIS VALUE**

2. **Get App Secret (for Client Token):**
   - Still in **Settings** → **Basic**
   - Find **App Secret** → Click **Show**
   - Enter your password
   - **COPY THE APP SECRET**

3. **Generate Client Token:**
   - Go to **Settings** → **Advanced**
   - Find **Client Token** section
   - If it's empty, you can use your **App Secret** as Client Token
   - Or generate one if available
   - **COPY THE CLIENT TOKEN**

### Step 3: Configure Facebook Login

1. **Add Facebook Login Product:**
   - In Facebook App dashboard
   - Click **Add Product** (left sidebar)
   - Find **Facebook Login**
   - Click **Set Up**

2. **Configure Settings:**
   - Go to **Facebook Login** → **Settings**
   - Add **Valid OAuth Redirect URIs:**
     ```
     fbYOUR_APP_ID://authorize
     ```
     (Replace `YOUR_APP_ID` with your actual App ID)
   - Add **Client OAuth Login:** Enable
   - Add **Web OAuth Login:** Enable
   - Click **Save Changes**

3. **Add Platform (Android):**
   - Go to **Settings** → **Basic**
   - Scroll to **Platforms** section
   - Click **Add Platform** → **Android**
   - **Package Name:** `com.example.fraudulens`
   - **Class Name:** `com.example.fraudulens.activities.StarterActivity`
   - Click **Save**

### Step 4: Update strings.xml

Open `app/src/main/res/values/strings.xml` and replace:

```xml
<string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
<string name="facebook_client_token">YOUR_FACEBOOK_CLIENT_TOKEN</string>
<string name="fb_login_protocol_scheme">fbYOUR_FACEBOOK_APP_ID</string>
```

With your actual values:

```xml
<string name="facebook_app_id">1234567890123456</string>
<string name="facebook_client_token">your_client_token_here</string>
<string name="fb_login_protocol_scheme">fb1234567890123456</string>
```

**Important:** 
- `facebook_app_id` = Your App ID (numbers only)
- `facebook_client_token` = Your Client Token or App Secret
- `fb_login_protocol_scheme` = `fb` + Your App ID (e.g., `fb1234567890123456`)

---

## ✅ Final Steps

1. **Rebuild the app:**
   ```bash
   ./gradlew clean build
   ```
   Or in Android Studio: **Build** → **Rebuild Project**

2. **Test Google Sign-In:**
   - Run the app
   - Click "Continue with Google" button
   - Should open Google Sign-In dialog
   - After signing in, should redirect to app

3. **Test Facebook Login:**
   - Run the app
   - Click "Sign in with Facebook" button
   - Should open Facebook Login dialog
   - After signing in, should redirect to app

---

## 🐛 Troubleshooting

### Google Sign-In Issues:

**Error: "10: DEVELOPER_ERROR"**
- ✅ Check Web Client ID is correct in `strings.xml`
- ✅ Verify SHA-1 fingerprint is added in Firebase
- ✅ Make sure `google-services.json` is updated
- ✅ Rebuild the app after changes

**Error: "7: NETWORK_ERROR"**
- ✅ Check internet connection
- ✅ Verify Firebase project is active

### Facebook Login Issues:

**Error: "Invalid App ID"**
- ✅ Check App ID matches in `strings.xml` and `AndroidManifest.xml`
- ✅ Verify `fb_login_protocol_scheme` format: `fb` + App ID
- ✅ Make sure Facebook Login product is added
- ✅ Verify package name is correct in Facebook App settings

**Error: "App Not Setup"**
- ✅ Add Android platform in Facebook App settings
- ✅ Configure OAuth Redirect URIs
- ✅ Enable Client OAuth Login

---

## 📝 Quick Checklist

- [ ] Firebase Web Client ID copied
- [ ] Google Sign-In enabled in Firebase
- [ ] SHA-1 fingerprint added to Firebase
- [ ] Updated `google-services.json` downloaded
- [ ] `default_web_client_id` updated in `strings.xml`
- [ ] Facebook App created
- [ ] Facebook App ID copied
- [ ] Facebook Client Token copied
- [ ] Facebook Login product added
- [ ] Android platform added in Facebook
- [ ] `facebook_app_id` updated in `strings.xml`
- [ ] `facebook_client_token` updated in `strings.xml`
- [ ] `fb_login_protocol_scheme` updated in `strings.xml`
- [ ] App rebuilt
- [ ] Tested Google Sign-In
- [ ] Tested Facebook Login

---

## 💡 Need Help?

If you get stuck:
1. Check the error message in Logcat
2. Verify all values in `strings.xml` match your Firebase/Facebook settings
3. Make sure you rebuilt the app after making changes
4. Check that `google-services.json` is in the `app/` folder
