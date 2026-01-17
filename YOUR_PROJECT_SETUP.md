# OAuth Setup for Your Project

## Your Firebase Project: `fraudulense`
**Project Number:** 838199002873

---

## 🔵 GOOGLE SIGN-IN SETUP

### Step 1: Get SHA-1 Fingerprint

**Easiest way:**
- Double-click `get_sha1.bat` in this folder
- Copy the SHA-1 value

**Or run:**
```cmd
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Look for: `SHA1: AA:BB:CC:DD:EE:FF:...`

### Step 2: Add SHA-1 to Firebase

1. Go to: https://console.firebase.google.com/project/fraudulense/settings/general
2. Scroll to **Your apps**
3. Find Android app: `com.example.fraudulens`
4. Click **Add fingerprint**
5. Paste your SHA-1
6. Click **Save**

### Step 3: Create Web App (if not exists)

1. In Firebase Console: https://console.firebase.google.com/project/fraudulense/settings/general
2. Scroll to **Your apps**
3. Click **Add app** → Select **Web** (</> icon)
4. Register app:
   - **App nickname:** FrauduLens Web
   - **Firebase Hosting:** (optional, skip for now)
   - Click **Register app**
5. **Copy the config** - you'll see something like:
   ```javascript
   const firebaseConfig = {
     apiKey: "...",
     authDomain: "...",
     projectId: "fraudulense",
     ...
   };
   ```
6. **IMPORTANT:** You need the **Web Client ID** for OAuth
   - This is NOT in the config above
   - Continue to next step

### Step 4: Get Web Client ID

**Option A: From Google Cloud Console (Recommended)**
1. Go to: https://console.cloud.google.com/apis/credentials?project=fraudulense
2. Look for **OAuth 2.0 Client IDs**
3. Find one with type **Web client** (or create one)
4. Copy the **Client ID** (looks like: `838199002873-xxxxx.apps.googleusercontent.com`)

**Option B: Create OAuth Client**
1. Go to: https://console.cloud.google.com/apis/credentials?project=fraudulense
2. Click **+ CREATE CREDENTIALS** → **OAuth client ID**
3. **Application type:** Web application
4. **Name:** FrauduLens Web Client
5. **Authorized JavaScript origins:** (leave empty for now)
6. **Authorized redirect URIs:** (leave empty for now)
7. Click **Create**
8. **Copy the Client ID**

### Step 5: Enable Google Sign-In

1. Go to: https://console.firebase.google.com/project/fraudulense/authentication/providers
2. Click **Google**
3. **Enable** the toggle
4. **Project support email:** (your email)
5. Click **Save**

### Step 6: Update strings.xml

Open `app/src/main/res/values/strings.xml`

Replace this line:
```xml
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID</string>
```

With your actual Web Client ID:
```xml
<string name="default_web_client_id">838199002873-xxxxx.apps.googleusercontent.com</string>
```

**Replace `xxxxx` with your actual client ID from Step 4**

### Step 7: Download Updated google-services.json

1. Go to: https://console.firebase.google.com/project/fraudulense/settings/general
2. Scroll to **Your apps** → Android app
3. Click **Download google-services.json**
4. Replace `app/google-services.json` with the downloaded file

---

## 🔵 FACEBOOK LOGIN SETUP (Optional)

### Step 1: Create Facebook App

1. Go to: https://developers.facebook.com/apps/
2. Click **Create App**
3. Choose **Consumer** → **Next**
4. Fill:
   - **App Name:** FrauduLens
   - **App Contact Email:** (your email)
5. Click **Create App**

### Step 2: Get App ID and Secret

1. In Facebook App dashboard
2. Go to **Settings** → **Basic**
3. **App ID:** Copy this number (e.g., `1234567890123456`)
4. **App Secret:** Click "Show" → Enter password → Copy

### Step 3: Configure Facebook Login

1. In Facebook App dashboard
2. Click **Add Product** → **Facebook Login** → **Set Up**
3. Go to **Facebook Login** → **Settings**
4. Add **Valid OAuth Redirect URIs:**
   ```
   fbYOUR_APP_ID://authorize
   ```
   (Replace `YOUR_APP_ID` with your App ID)
5. Enable **Client OAuth Login**
6. Enable **Web OAuth Login**
7. Click **Save Changes**

### Step 4: Add Android Platform

1. Go to **Settings** → **Basic**
2. Scroll to **Platforms**
3. Click **Add Platform** → **Android**
4. Fill:
   - **Package Name:** `com.example.fraudulens`
   - **Class Name:** `com.example.fraudulens.activities.StarterActivity`
5. Click **Save**

### Step 5: Update strings.xml

Open `app/src/main/res/values/strings.xml`

Replace:
```xml
<string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
<string name="facebook_client_token">YOUR_FACEBOOK_CLIENT_TOKEN</string>
<string name="fb_login_protocol_scheme">fbYOUR_FACEBOOK_APP_ID</string>
```

With your actual values:
```xml
<string name="facebook_app_id">1234567890123456</string>
<string name="facebook_client_token">your_app_secret_here</string>
<string name="fb_login_protocol_scheme">fb1234567890123456</string>
```

**Example:**
- If App ID is `9876543210987654`
- Then `fb_login_protocol_scheme` = `fb9876543210987654`

---

## ✅ Final Steps

1. **Rebuild Project:**
   - Android Studio: **Build** → **Rebuild Project**
   - Or: `./gradlew clean build`

2. **Test:**
   - Run the app
   - Google Sign-In button should work
   - Facebook Login button should work (if configured)

---

## 🐛 Troubleshooting

### Google Sign-In still not working?

1. ✅ Check SHA-1 is added in Firebase
2. ✅ Verify Web Client ID is correct in `strings.xml`
3. ✅ Make sure `google-services.json` is updated
4. ✅ Rebuild the project
5. ✅ Check Logcat for specific error codes

### Facebook Login not working?

1. ✅ Verify App ID matches in `strings.xml` and `AndroidManifest.xml`
2. ✅ Check `fb_login_protocol_scheme` format: `fb` + App ID
3. ✅ Make sure Android platform is added
4. ✅ Verify OAuth Redirect URIs are configured

---

## 📞 Quick Links

- **Firebase Console:** https://console.firebase.google.com/project/fraudulense
- **Google Cloud Console:** https://console.cloud.google.com/apis/credentials?project=fraudulense
- **Facebook Developers:** https://developers.facebook.com/apps/
