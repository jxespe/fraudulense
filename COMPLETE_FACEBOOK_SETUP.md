# Complete Facebook App Setup - Step by Step

This guide will help you complete ALL Facebook app requirements.

---

## ✅ Checklist

- [ ] App Icon (1024 x 1024)
- [ ] Privacy Policy URL
- [ ] User Data Deletion URL
- [ ] Category Selection
- [ ] Update strings.xml with App ID

---

## 📱 Step 1: Create App Icon (1024 x 1024)

### Option A: Use Existing App Icon

1. **Find your app icon:**
   - Location: `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp`
   - Or: `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`

2. **Convert to 1024x1024:**
   - **Online tool:** https://www.iloveimg.com/resize-image
   - Upload your icon
   - Set size to 1024 x 1024 pixels
   - Download as PNG

3. **Or use Android Studio:**
   - Right-click `ic_launcher` → **New** → **Image Asset**
   - Create new icon
   - Export as 1024x1024 PNG

### Option B: Create New Icon

1. Use design tool (Canva, Figma, etc.)
2. Create 1024x1024 square design
3. Export as PNG
4. Save as `facebook-app-icon.png`

### Upload to Facebook:

1. Go to Facebook App Dashboard
2. **Settings** → **Basic**
3. Scroll to **App Icon**
4. Click **Upload**
5. Select your 1024x1024 PNG file
6. Click **Save Changes**

---

## 📄 Step 2: Host Privacy Policy

### Option A: GitHub Pages (Free & Easy - Recommended)

1. **Create GitHub repository:**
   - Go to: https://github.com/new
   - Repository name: `fraudulens-pages` (or any name)
   - Make it **Public**
   - Click **Create repository**

2. **Upload privacy policy:**
   - In your repository, click **Add file** → **Upload files**
   - Drag and drop `privacy-policy-template.html`
   - Rename it to `index.html` (or keep as `privacy-policy.html`)
   - Click **Commit changes**

3. **Enable GitHub Pages:**
   - Go to repository **Settings**
   - Scroll to **Pages** (left sidebar)
   - **Source:** Deploy from a branch
   - **Branch:** `main` (or `master`)
   - **Folder:** `/ (root)`
   - Click **Save**

4. **Get your URL:**
   - Your privacy policy will be at:
   - `https://yourusername.github.io/fraudulens-pages/privacy-policy.html`
   - Or: `https://yourusername.github.io/fraudulens-pages/` (if named index.html)

### Option B: Firebase Hosting (Free)

1. **Install Firebase CLI:**
   ```bash
   npm install -g firebase-tools
   ```

2. **Login to Firebase:**
   ```bash
   firebase login
   ```

3. **Initialize Hosting:**
   ```bash
   cd C:\Users\MSI\AndroidStudioProjects\FrauduLens
   firebase init hosting
   ```
   - Select your project: `fraudulense`
   - Public directory: `public` (or create new folder)
   - Single-page app: No
   - GitHub: No

4. **Add files:**
   - Copy `privacy-policy-template.html` to `public/privacy-policy.html`
   - Copy `data-deletion.html` to `public/data-deletion.html`

5. **Deploy:**
   ```bash
   firebase deploy --only hosting
   ```

6. **Get your URL:**
   - Your privacy policy will be at:
   - `https://fraudulense.web.app/privacy-policy.html`
   - Or: `https://fraudulense.firebaseapp.com/privacy-policy.html`

### Option C: Netlify (Free)

1. Go to: https://www.netlify.com/
2. Sign up (free)
3. Drag and drop folder containing `privacy-policy-template.html`
4. Get instant URL: `https://random-name.netlify.app/privacy-policy.html`

---

## 🗑️ Step 3: Host Data Deletion Page

Use the same hosting method as Privacy Policy:

1. **Upload `data-deletion.html`** to the same location
2. **Get URL:** `https://your-site.com/data-deletion.html`

---

## 🔗 Step 4: Add URLs to Facebook App

1. **Go to Facebook App Dashboard:**
   - https://developers.facebook.com/apps/
   - Select your app

2. **Add Privacy Policy URL:**
   - **Settings** → **Basic**
   - Scroll to **Privacy Policy URL**
   - Paste your privacy policy URL
   - Example: `https://yourusername.github.io/fraudulens-pages/privacy-policy.html`

3. **Add Data Deletion URL:**
   - Still in **Settings** → **Basic**
   - Scroll to **User Data Deletion**
   - Click **Add** or **Edit**
   - Paste your data deletion URL
   - Example: `https://yourusername.github.io/fraudulens-pages/data-deletion.html`

4. **Click Save Changes**

---

## 📂 Step 5: Set App Category

1. **In Facebook App Dashboard:**
   - **Settings** → **Basic**
   - Find **Category** dropdown

2. **Select Category:**
   - **Recommended:** `Utilities` (for scam detection tool)
   - **Alternative:** `Consumer`, `Business`, or `Productivity`

3. **Click Save Changes**

---

## 🎨 Step 6: Upload App Icon

1. **In Facebook App Dashboard:**
   - **Settings** → **Basic**
   - Scroll to **App Icon**

2. **Upload:**
   - Click **Upload** or **Change**
   - Select your 1024x1024 PNG file
   - Wait for upload to complete

3. **Click Save Changes**

---

## ✅ Step 7: Verify All Requirements

Check that all fields are filled:

- [x] **App Icon:** Uploaded (1024 x 1024)
- [x] **Privacy Policy URL:** Added
- [x] **User Data Deletion:** Added
- [x] **Category:** Selected

---

## 🔧 Step 8: Update Your App Configuration

### Update strings.xml

1. **Get your Facebook App ID:**
   - Facebook App Dashboard → **Settings** → **Basic**
   - Copy **App ID** (numbers only, e.g., `1234567890123456`)

2. **Get Client Token:**
   - **Settings** → **Basic**
   - Find **App Secret** → Click **Show**
   - Copy the secret (use this as Client Token)

3. **Update `app/src/main/res/values/strings.xml`:**

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

   **Important:** 
   - `fb_login_protocol_scheme` = `fb` + your App ID
   - Example: If App ID is `9876543210987654`, then `fb_login_protocol_scheme` = `fb9876543210987654`

---

## 🚀 Step 9: Test Facebook Login

1. **Rebuild your app:**
   - Android Studio: **Build** → **Rebuild Project**

2. **Run the app**

3. **Test Facebook Login:**
   - Click "Sign in with Facebook" button
   - Should open Facebook login dialog
   - After login, should redirect back to app

---

## 📋 Final Checklist

Before submitting for review:

- [ ] App Icon uploaded (1024x1024)
- [ ] Privacy Policy URL added and accessible
- [ ] Data Deletion URL added and accessible
- [ ] Category selected
- [ ] `strings.xml` updated with App ID
- [ ] `strings.xml` updated with Client Token
- [ ] `strings.xml` updated with protocol scheme
- [ ] App rebuilt
- [ ] Facebook Login tested and working

---

## 🎯 Next Steps

### For Development Mode:
- You're done! Test Facebook Login now.

### For Production/Public Access:
1. **Submit for App Review:**
   - Go to **App Review** → **Permissions and Features**
   - Request `email` and `public_profile` permissions
   - Fill out submission form
   - Submit for review

2. **Wait for Approval:**
   - Usually takes 1-7 business days
   - Facebook will review your app

3. **Once Approved:**
   - Switch App Mode from Development to Live
   - Public users can now use Facebook Login

---

## 🐛 Troubleshooting

### Privacy Policy URL not working?
- ✅ Check URL is publicly accessible (no login required)
- ✅ Test URL in incognito/private browser
- ✅ Verify file is actually uploaded

### Data Deletion URL not working?
- ✅ Same checks as Privacy Policy
- ✅ Make sure file is named correctly

### App Icon not uploading?
- ✅ Check file is exactly 1024x1024 pixels
- ✅ File format must be PNG or JPG
- ✅ File size should be under 5MB

### Facebook Login still not working?
- ✅ Verify App ID in `strings.xml` matches Facebook App
- ✅ Check `fb_login_protocol_scheme` format: `fb` + App ID
- ✅ Rebuild app after updating `strings.xml`
- ✅ Check Logcat for specific error messages

---

## 💡 Quick Reference

**Your Files:**
- Privacy Policy: `privacy-policy-template.html`
- Data Deletion: `data-deletion.html`
- App Icon: Create 1024x1024 PNG from your logo

**Where to Host:**
- GitHub Pages (easiest): https://github.com
- Firebase Hosting: Already have Firebase project
- Netlify: https://www.netlify.com

**Facebook App Dashboard:**
- https://developers.facebook.com/apps/

---

## ✅ You're All Set!

Once you complete these steps, your Facebook app will have all required fields filled, and Facebook Login will work in your Android app!
