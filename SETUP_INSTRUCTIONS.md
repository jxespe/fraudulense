# Firebase & Social Login Setup Instructions

## Firebase Setup (Already Connected)
✅ Firebase is already configured with `google-services.json`
✅ Registration and Login are working with Firestore
✅ User data is stored in the `users` collection

## Google Sign-In Setup

1. **Get Web Client ID from Firebase Console:**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Select your project: `fraudulense`
   - Go to **Project Settings** > **Your apps**
   - Find your **Web app** (or create one if it doesn't exist)
   - Copy the **Web client ID** (looks like: `xxxxx.apps.googleusercontent.com`)

2. **Add to strings.xml:**
   - Open `app/src/main/res/values/strings.xml`
   - Replace `YOUR_WEB_CLIENT_ID` with your actual Web Client ID:
   ```xml
   <string name="default_web_client_id">YOUR_ACTUAL_WEB_CLIENT_ID_HERE</string>
   ```

3. **Enable Google Sign-In in Firebase:**
   - In Firebase Console, go to **Authentication** > **Sign-in method**
   - Enable **Google** sign-in provider
   - Add your app's SHA-1 fingerprint (get it from Android Studio: Build > Generate Signed Bundle/APK)

## Facebook Login Setup

1. **Create Facebook App:**
   - Go to [Facebook Developers](https://developers.facebook.com/)
   - Create a new app or use existing one
   - Add **Facebook Login** product
   - Get your **App ID** and **Client Token**

2. **Add to strings.xml:**
   - Open `app/src/main/res/values/strings.xml`
   - Replace the placeholders:
   ```xml
   <string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
   <string name="facebook_client_token">YOUR_FACEBOOK_CLIENT_TOKEN</string>
   <string name="fb_login_protocol_scheme">fbYOUR_FACEBOOK_APP_ID</string>
   ```
   Example: If your App ID is `123456789`, then `fb_login_protocol_scheme` should be `fb123456789`

3. **Enable Facebook Login in Firebase:**
   - In Firebase Console, go to **Authentication** > **Sign-in method**
   - Enable **Facebook** sign-in provider
   - Add your Facebook App ID and App Secret

4. **Configure Facebook App:**
   - In Facebook App Settings, add your Android package name: `com.example.fraudulens`
   - Add your app's key hash (get it from Android Studio)

## Current Features

✅ **Email/Password Registration:**
- Stores: name, username, email, passwordHash, createdAt
- Navigates to phone verification after registration

✅ **Email/Password Login:**
- Validates credentials against Firestore
- Supports legacy accounts (double-hash fallback)
- Remembers email if "Remember me" is checked

✅ **Phone Verification:**
- Sends OTP via Firebase Phone Authentication
- Updates user profile with phone number after verification
- Navigates to Premium screen after successful verification

✅ **Google Sign-In:**
- Ready to use once Web Client ID is configured
- Stores user data in Firestore
- Handles new and existing users

✅ **Facebook Login:**
- Ready to use once Facebook App ID is configured
- Stores user data in Firestore
- Handles new and existing users

## Testing

1. **Test Email Registration:**
   - Register with email → Should store in Firestore
   - Check Firestore console to verify user document

2. **Test Login:**
   - Login with registered email → Should work
   - Check logs for debug information

3. **Test Social Login:**
   - After configuring credentials, test Google/Facebook login
   - Should create user in Firestore and navigate to phone verification

## Firestore Structure

```
users/
  └── {documentId}/
      ├── name: string
      ├── username: string (optional)
      ├── email: string (lowercase)
      ├── passwordHash: string (for email/password users)
      ├── phoneNumber: string (after OTP verification)
      ├── uid: string (for social login users)
      ├── provider: string ("google" | "facebook" | "email")
      ├── photoUrl: string (for social login users)
      ├── isVerified: boolean
      └── createdAt: timestamp
```
