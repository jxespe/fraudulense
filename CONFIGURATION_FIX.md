# Fixing Google Sign-In and Facebook Login Errors

## 🔴 Current Errors:
1. **Google Sign-In failed** - Invalid Web Client ID
2. **Facebook Invalid App ID** - Missing or incorrect Facebook App ID

## ✅ What I've Fixed:

### 1. **Better Error Handling**
- Added configuration checks before attempting sign-in
- More helpful error messages
- Buttons automatically hide if not configured
- Graceful fallback when services aren't set up

### 2. **Code Updates**
- `AuthHelper.java` now checks if Google/Facebook are configured
- Login/Register activities hide buttons if not configured
- Better error messages guide you to fix the issue

## 🔧 How to Fix the Errors:

### **Fix Google Sign-In Error:**

1. **Get Web Client ID from Firebase:**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Select your project
   - Click ⚙️ **Project Settings**
   - Go to **Your apps** tab
   - Find your **Web app** (or create one)
   - Copy the **Web client ID** (looks like: `123456789-abc...apps.googleusercontent.com`)

2. **Update `strings.xml`:**
   ```xml
   <string name="default_web_client_id">123456789-abc...apps.googleusercontent.com</string>
   ```
   Replace `YOUR_WEB_CLIENT_ID` with your actual Web Client ID.

3. **Enable Google Sign-In in Firebase:**
   - Firebase Console → **Authentication** → **Sign-in method**
   - Enable **Google** provider
   - Add Support email
   - Save

4. **Add SHA-1 Fingerprint:**
   ```bash
   # Get SHA-1
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```
   - Copy SHA-1 fingerprint
   - Firebase Console → Project Settings → Your apps → Android app
   - Add fingerprint
   - Download updated `google-services.json`
   - Replace `app/google-services.json`

### **Fix Facebook Login Error:**

1. **Create Facebook App:**
   - Go to [Facebook Developers](https://developers.facebook.com/apps/)
   - Click **Create App**
   - Choose **Consumer** or **Business** type
   - Fill in app details

2. **Get App ID and Client Token:**
   - In your Facebook App dashboard
   - Go to **Settings** → **Basic**
   - Copy **App ID**
   - Copy **App Secret** (you'll need this for Client Token)
   - Go to **Settings** → **Advanced**
   - Generate **Client Token** if not available

3. **Update `strings.xml`:**
   ```xml
   <string name="facebook_app_id">YOUR_ACTUAL_APP_ID</string>
   <string name="facebook_client_token">YOUR_ACTUAL_CLIENT_TOKEN</string>
   <string name="fb_login_protocol_scheme">fbYOUR_ACTUAL_APP_ID</string>
   ```
   Replace placeholders with actual values.

4. **Configure Facebook Login:**
   - In Facebook App dashboard
   - Go to **Products** → **Facebook Login** → **Settings**
   - Add **Valid OAuth Redirect URIs**
   - Add your package name: `com.example.fraudulens`

5. **Update AndroidManifest.xml:**
   - Already configured in your `AndroidManifest.xml`
   - Make sure `facebook_app_id` matches your actual App ID

## 🎯 Quick Test:

After configuration:
1. **Rebuild the app**
2. **Run the app**
3. Google/Facebook buttons should appear if configured
4. Buttons will be hidden if not configured (no errors!)
5. Try signing in - should work now!

## 📝 Alternative: Disable OAuth Temporarily

If you don't want to set up OAuth right now:
- The buttons will automatically hide
- Users can still use email/username login
- No errors will appear
- You can enable OAuth later

## 🐛 Still Having Issues?

Check:
- ✅ `google-services.json` is in `app/` folder
- ✅ SHA-1 fingerprint is added in Firebase
- ✅ Google Sign-In is enabled in Firebase Console
- ✅ Facebook App ID matches in `strings.xml` and `AndroidManifest.xml`
- ✅ App is rebuilt after configuration changes
