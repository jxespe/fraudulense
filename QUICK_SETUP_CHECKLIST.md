# Quick Setup Checklist - Complete All Facebook Requirements

## ✅ Step-by-Step Action Items

### 📱 1. Create App Icon (5 minutes)

**Option A: Use Online Tool**
1. Go to: https://www.iloveimg.com/resize-image
2. Upload your app logo (from `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp`)
3. Set size: **1024 x 1024 pixels**
4. Download as PNG
5. Save as `facebook-app-icon.png` in project folder

**Option B: Use Android Studio**
1. Open Android Studio
2. Right-click `app/src/main/res` → **New** → **Image Asset**
3. Create icon → Export as 1024x1024 PNG

**Upload to Facebook:**
- Facebook App Dashboard → **Settings** → **Basic** → **App Icon**
- Upload your 1024x1024 PNG file

---

### 📄 2. Host Privacy Policy (10 minutes)

**Easiest: GitHub Pages**

1. **Create GitHub account** (if needed): https://github.com/signup

2. **Create repository:**
   - Go to: https://github.com/new
   - Name: `fraudulens-pages`
   - Make it **Public**
   - Create repository

3. **Upload files:**
   - Click **Add file** → **Upload files**
   - Drag and drop:
     - `privacy-policy-template.html`
     - `data-deletion.html`
   - Click **Commit changes**

4. **Enable GitHub Pages:**
   - Repository → **Settings** → **Pages**
   - Source: **Deploy from a branch**
   - Branch: `main`
   - Folder: `/ (root)`
   - Save

5. **Get your URLs** (wait 1-2 minutes):
   - Privacy Policy: `https://yourusername.github.io/fraudulens-pages/privacy-policy-template.html`
   - Data Deletion: `https://yourusername.github.io/fraudulens-pages/data-deletion.html`

**See `host-on-github.md` for detailed instructions**

---

### 🔗 3. Add URLs to Facebook (2 minutes)

1. Go to: https://developers.facebook.com/apps/
2. Select your app
3. **Settings** → **Basic**
4. **Privacy Policy URL:** Paste your privacy policy URL
5. **User Data Deletion:** Click **Add** → Paste your data deletion URL
6. Click **Save Changes**

---

### 📂 4. Set Category (1 minute)

1. Facebook App Dashboard → **Settings** → **Basic**
2. **Category:** Select **Utilities** (or Consumer/Business)
3. Click **Save Changes**

---

### 🎨 5. Upload App Icon (1 minute)

1. Facebook App Dashboard → **Settings** → **Basic**
2. Scroll to **App Icon**
3. Click **Upload**
4. Select your `facebook-app-icon.png` (1024x1024)
5. Wait for upload
6. Click **Save Changes**

---

### ⚙️ 6. Update App Configuration (3 minutes)

1. **Get Facebook App ID:**
   - Facebook App Dashboard → **Settings** → **Basic**
   - Copy **App ID** (numbers only)

2. **Get App Secret (Client Token):**
   - **Settings** → **Basic**
   - **App Secret** → Click **Show** → Copy

3. **Update `app/src/main/res/values/strings.xml`:**

   Find these lines:
   ```xml
   <string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
   <string name="facebook_client_token">YOUR_FACEBOOK_CLIENT_TOKEN</string>
   <string name="fb_login_protocol_scheme">fbYOUR_FACEBOOK_APP_ID</string>
   ```

   Replace with your values:
   ```xml
   <string name="facebook_app_id">1234567890123456</string>
   <string name="facebook_client_token">your_app_secret_here</string>
   <string name="fb_login_protocol_scheme">fb1234567890123456</string>
   ```

   **Important:** 
   - Replace `1234567890123456` with your actual App ID
   - Replace `your_app_secret_here` with your App Secret
   - `fb_login_protocol_scheme` = `fb` + your App ID

---

### 🚀 7. Rebuild & Test (2 minutes)

1. **Rebuild project:**
   - Android Studio: **Build** → **Rebuild Project**

2. **Run app**

3. **Test Facebook Login:**
   - Click "Sign in with Facebook"
   - Should work now!

---

## ✅ Final Verification

Check all items are complete:

- [ ] App Icon uploaded to Facebook (1024x1024)
- [ ] Privacy Policy URL added to Facebook
- [ ] Data Deletion URL added to Facebook
- [ ] Category selected in Facebook
- [ ] `strings.xml` updated with App ID
- [ ] `strings.xml` updated with Client Token
- [ ] `strings.xml` updated with protocol scheme
- [ ] App rebuilt
- [ ] Facebook Login tested

---

## 📋 Files You Need

All files are ready in your project:

✅ `privacy-policy-template.html` - Privacy policy (ready to host)
✅ `data-deletion.html` - Data deletion page (ready to host)
✅ `COMPLETE_FACEBOOK_SETUP.md` - Detailed guide
✅ `host-on-github.md` - GitHub Pages instructions
✅ `QUICK_SETUP_CHECKLIST.md` - This file

---

## 🎯 Estimated Time

- **Total time:** ~25 minutes
- **App Icon:** 5 min
- **Host Pages:** 10 min
- **Facebook Setup:** 5 min
- **App Config:** 3 min
- **Test:** 2 min

---

## 🐛 Need Help?

- **Detailed guide:** See `COMPLETE_FACEBOOK_SETUP.md`
- **GitHub hosting:** See `host-on-github.md`
- **Troubleshooting:** See `FACEBOOK_APP_REQUIREMENTS.md`

---

## 🎉 You're Done!

Once you complete these steps, your Facebook app will have all required fields, and Facebook Login will work perfectly in your Android app!
