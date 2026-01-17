# Enable Phone Authentication in Firebase

## Error Message
You're seeing this error because **Phone Authentication is not enabled** in your Firebase project:

```
This operation is not allowed. This may be because the given sign-in provider is disabled for this Firebase project. Enable it in the Firebase console, under the sign-in method tab of the Auth section.
```

## Quick Fix (5 minutes)

### Step 1: Open Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **fraudulense** (or your project name)

### Step 2: Navigate to Authentication
1. In the left sidebar, click **Authentication**
2. Click on the **Sign-in method** tab (at the top)

### Step 3: Enable Phone Authentication
1. Find **Phone** in the list of sign-in providers
2. Click on **Phone** to open its settings
3. Click the **Enable** toggle at the top
4. Click **Save**

### Step 4: Configure Phone Authentication (Optional but Recommended)
1. **Test phone numbers** (for development):
   - Click on the **Phone numbers for testing** section
   - Add test phone numbers (e.g., `+639661704551`) with verification codes (e.g., `123456`)
   - This allows you to test OTP without sending real SMS

2. **App verification** (for production):
   - Firebase will automatically use reCAPTCHA for verification
   - For Android, you may need to add your app's SHA-1 fingerprint (if not already added)
   - Check if your SHA-1 is added: **Project Settings > Your apps > Android app**

### Step 5: Verify Setup
1. After enabling, you should see **Phone** with a green checkmark ✅
2. The status should show as **Enabled**

## Testing After Setup

### Option 1: Use Test Phone Numbers (Recommended for Development)
1. In Firebase Console > Authentication > Sign-in method > Phone
2. Add a test phone number: `+639661704551`
3. Add a test code: `123456`
4. When you request OTP, Firebase will automatically use this test code instead of sending SMS

### Option 2: Use Real Phone Number
1. Make sure you're using a real phone number
2. Firebase will send an actual SMS (may have quota limits on free tier)
3. You'll receive a 6-digit code via SMS

## Troubleshooting

### Still Getting Errors?
1. **Wait a few minutes** - Firebase changes can take 1-2 minutes to propagate
2. **Check your Firebase project** - Make sure you're in the correct project
3. **Verify google-services.json** - Ensure your `app/google-services.json` matches your Firebase project
4. **Check SHA-1 fingerprint** - Make sure your app's SHA-1 is added in Firebase Console

### Common Issues

**Issue**: "Phone Authentication not enabled"
- **Solution**: Follow Step 3 above to enable it

**Issue**: "Invalid phone number"
- **Solution**: Make sure phone number is in E.164 format: `+63XXXXXXXXXX` (with country code)

**Issue**: "SMS quota exceeded"
- **Solution**: 
  - Use test phone numbers for development
  - Wait 24 hours for quota reset (free tier)
  - Upgrade to Blaze plan for higher quotas

**Issue**: "App verification failed"
- **Solution**: 
  - Add SHA-1 fingerprint in Firebase Console
  - Run `get_sha1.bat` to get your SHA-1
  - Add it in Firebase Console > Project Settings > Your apps

## Next Steps

After enabling Phone Authentication:
1. ✅ Rebuild your app
2. ✅ Test OTP sending with a test phone number
3. ✅ Verify you receive the OTP code
4. ✅ Complete the registration/login flow

## Additional Resources

- [Firebase Phone Auth Documentation](https://firebase.google.com/docs/auth/android/phone-auth)
- [Firebase Console](https://console.firebase.google.com/)
- [Firebase Pricing](https://firebase.google.com/pricing) (Phone Auth is free for development, has quotas for production)

---

**Note**: If you're still having issues after following these steps, check the Logcat output for more specific error messages and share them for further debugging.
