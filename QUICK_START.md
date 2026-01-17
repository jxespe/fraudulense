# Quick Start - OAuth Configuration

## 🚀 Fast Setup (5 minutes)

### Step 1: Get SHA-1 Fingerprint

**Windows:**
- Double-click `get_sha1.bat` in this folder
- Copy the SHA-1 value (looks like: `AA:BB:CC:DD:EE:FF:...`)

**Or run manually:**
```cmd
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### Step 2: Configure Firebase

1. Go to: https://console.firebase.google.com/
2. Select your project: **fraudulense**
3. **Add SHA-1:**
   - Settings ⚙️ → Project settings
   - Your apps → Android app (`com.example.fraudulens`)
   - Click "Add fingerprint"
   - Paste your SHA-1
   - Save

4. **Get Web Client ID:**
   - Settings ⚙️ → Project settings
   - Your apps → **Add app** → **Web** (if you don't have one)
   - Copy the **Web client ID**
   - It looks like: `838199002873-xxxxx.apps.googleusercontent.com`

5. **Enable Google Sign-In:**
   - Authentication → Sign-in method
   - Enable **Google**
   - Add support email
   - Save

6. **Download updated google-services.json:**
   - Settings ⚙️ → Project settings
   - Your apps → Android app
   - Download `google-services.json`
   - Replace `app/google-services.json` with the new file

### Step 3: Configure Facebook (Optional)

1. Go to: https://developers.facebook.com/apps/
2. Create app → Consumer → Fill details
3. Get **App ID** from Settings → Basic
4. Get **App Secret** from Settings → Basic (use as Client Token)
5. Add **Facebook Login** product
6. Add **Android** platform (package: `com.example.fraudulens`)

### Step 4: Update strings.xml

Open `app/src/main/res/values/strings.xml` and replace:

```xml
<!-- Replace YOUR_WEB_CLIENT_ID with your actual Web Client ID -->
<string name="default_web_client_id">838199002873-xxxxx.apps.googleusercontent.com</string>

<!-- Replace with your Facebook App ID (if using Facebook) -->
<string name="facebook_app_id">1234567890123456</string>
<string name="facebook_client_token">your_app_secret_here</string>
<string name="fb_login_protocol_scheme">fb1234567890123456</string>
```

### Step 5: Rebuild & Test

1. **Rebuild project:** Build → Rebuild Project
2. **Run app**
3. **Test Google Sign-In** - button should work now!

---

## 📋 What You Need:

- [ ] SHA-1 fingerprint (run `get_sha1.bat`)
- [ ] Firebase Web Client ID
- [ ] Updated `google-services.json`
- [ ] Google Sign-In enabled in Firebase
- [ ] Updated `strings.xml` with Web Client ID
- [ ] (Optional) Facebook App ID and Client Token

---

## ⚠️ Important Notes:

1. **SHA-1 is required** - Google Sign-In won't work without it
2. **Web app must exist** in Firebase to get Web Client ID
3. **Rebuild after changes** - Configuration changes require rebuild
4. **Test on device/emulator** - OAuth requires proper setup

---

For detailed instructions, see: `OAUTH_SETUP_STEP_BY_STEP.md`
