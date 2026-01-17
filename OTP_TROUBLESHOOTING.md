# OTP Not Receiving - Troubleshooting Guide

## 🔍 Common Issues & Solutions

### 1. Firebase Phone Authentication Not Enabled

**Check:**
1. Go to: https://console.firebase.google.com/project/fraudulense/authentication/providers
2. Click **Phone** provider
3. Make sure it's **Enabled**
4. If not enabled, click **Enable** and save

**Fix:** Enable Phone Authentication in Firebase Console

---

### 2. Invalid Phone Number Format

**Current format in code:**
- Input: `9123456789` (10 digits)
- Code adds: `+63` prefix
- Result: `+639123456789`

**Valid formats:**
- ✅ `9123456789` (10 digits, no leading 0)
- ✅ `09123456789` (11 digits with leading 0 - code removes it)
- ❌ `+639123456789` (don't include +63, code adds it)
- ❌ `639123456789` (don't include country code)

**Fix:** Enter phone number without country code (e.g., `9123456789`)

---

### 3. Firebase Phone Auth Quota Exceeded

**Symptoms:**
- Error message: "QUOTA_EXCEEDED"
- No SMS received

**Causes:**
- Free tier has limited SMS per day
- Too many requests

**Solutions:**
- Wait 24 hours for quota reset
- Upgrade Firebase plan
- Use test phone numbers (for development)

**Check quota:**
- Firebase Console → Authentication → Usage

---

### 4. Test Phone Numbers (For Development)

**Firebase provides test phone numbers:**
1. Firebase Console → Authentication → Sign-in method → Phone
2. Scroll to **Phone numbers for testing**
3. Add test number: `+639123456789`
4. Add test code: `123456`
5. Use this number in your app - OTP will be `123456` (no SMS sent)

**Benefits:**
- No SMS quota used
- Instant verification
- Free for testing

---

### 5. Network/Internet Issues

**Check:**
- Device has internet connection
- Firebase services are accessible
- No firewall blocking Firebase

**Test:**
- Try on different network (WiFi vs Mobile data)
- Check Firebase status: https://status.firebase.google.com/

---

### 6. Phone Number Already Registered

**Issue:**
- Phone number already verified in Firebase
- Firebase may not send new OTP

**Solution:**
- Use a different phone number for testing
- Or delete the phone number from Firebase Console

---

### 7. SMS Permissions (Not Required for Firebase)

**Note:** Firebase Phone Auth doesn't require SMS permissions. It uses Firebase's backend to send SMS.

---

## 🔧 Code Improvements Made

I've updated the code to:
1. ✅ Better error handling - shows specific error messages
2. ✅ Better phone number validation
3. ✅ Removes leading 0 from phone numbers
4. ✅ Better logging for debugging
5. ✅ User feedback during OTP sending

---

## 📋 Step-by-Step Debugging

### Step 1: Check Firebase Console

1. Go to: https://console.firebase.google.com/project/fraudulense/authentication/providers
2. Verify **Phone** is enabled
3. Check **Usage** tab for quota issues

### Step 2: Check Logcat

1. Open Android Studio
2. Run app
3. Try to send OTP
4. Check Logcat for:
   - `FirebaseUtils: Sending OTP to: +63...`
   - `FirebaseUtils: OTP code sent successfully`
   - Or error messages

### Step 3: Test with Test Phone Number

1. Firebase Console → Authentication → Phone
2. Add test number: `+639123456789`
3. Add test code: `123456`
4. Use this number in app
5. Enter `123456` as OTP

### Step 4: Verify Phone Number Format

**Enter phone number as:**
- ✅ `9123456789` (10 digits)
- ✅ `09123456789` (11 digits with 0)

**Don't enter:**
- ❌ `+639123456789`
- ❌ `639123456789`

---

## 🎯 Quick Fixes

### Fix 1: Enable Phone Auth
```
Firebase Console → Authentication → Sign-in method → Phone → Enable
```

### Fix 2: Use Test Number
```
Firebase Console → Authentication → Phone → Add test number
Phone: +639123456789
Code: 123456
```

### Fix 3: Check Phone Format
```
Enter: 9123456789 (10 digits, no country code)
Code will format: +639123456789
```

### Fix 4: Check Logcat
```
Look for: "OTP code sent successfully" or error messages
```

---

## 📱 Testing Checklist

- [ ] Phone Authentication enabled in Firebase
- [ ] Phone number format correct (10 digits, no +63)
- [ ] Internet connection active
- [ ] Check Logcat for errors
- [ ] Try test phone number first
- [ ] Verify SMS quota not exceeded

---

## 🐛 Still Not Working?

1. **Check Logcat** - Look for specific error messages
2. **Try test phone number** - Use Firebase test numbers
3. **Check Firebase Console** - Verify Phone Auth is enabled
4. **Try different phone number** - May be already registered
5. **Wait and retry** - If quota exceeded, wait 24 hours

---

## 💡 Pro Tips

1. **Use test numbers during development** - Saves quota
2. **Check Logcat first** - Errors are logged there
3. **Verify Firebase setup** - Phone Auth must be enabled
4. **Format matters** - Enter 10 digits without country code

---

## ✅ After Fixes

The updated code now:
- Shows clear error messages
- Validates phone number format
- Logs all steps for debugging
- Handles errors gracefully

**Rebuild the app and try again!**
