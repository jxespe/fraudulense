# Facebook App Requirements - Quick Guide

Facebook requires these fields for app submission, but you can use your app in **Development Mode** without completing all of them immediately.

---

## 🚀 Quick Solution: Use Development Mode

**For now, you can skip the full submission and use Development Mode:**

1. In Facebook App dashboard
2. Go to **Settings** → **Basic**
3. Make sure **App Mode** is set to **Development**
4. In Development Mode:
   - Only you and added test users can use the app
   - No app review needed
   - Perfect for testing Facebook Login

**To add test users:**
- Go to **Roles** → **Test Users**
- Click **Add Test Users**
- You can use these accounts to test Facebook Login

---

## 📋 If You Need to Complete the Requirements:

### 1. App Icon (1024 x 1024)

**What you need:**
- Square image
- 1024 x 1024 pixels
- PNG or JPG format
- No transparency

**How to create:**
1. Use your app logo
2. Resize to 1024x1024 using:
   - Online tool: https://www.iloveimg.com/resize-image
   - Or Photoshop/GIMP
3. Upload in Facebook App → **Settings** → **Basic** → **App Icon**

**Quick option:**
- Use your existing app icon (`ic_launcher`)
- Export as 1024x1024 PNG

---

### 2. Privacy Policy URL

**What you need:**
- A publicly accessible URL
- Privacy policy explaining what data you collect
- Must be accessible without login

**Quick solutions:**

**Option A: Use a free privacy policy generator**
1. Go to: https://www.privacypolicygenerator.info/
2. Fill in your app details
3. Generate privacy policy
4. Host it on:
   - GitHub Pages (free)
   - Firebase Hosting (free)
   - Your own website

**Option B: Use GitHub Pages (Free & Easy)**
1. Create a file `privacy-policy.html` in your project
2. Push to GitHub
3. Enable GitHub Pages
4. Use URL: `https://yourusername.github.io/FrauduLens/privacy-policy.html`

**Option C: Use Firebase Hosting (Recommended)**
1. Install Firebase CLI: `npm install -g firebase-tools`
2. In your project: `firebase init hosting`
3. Create `public/privacy-policy.html`
4. Deploy: `firebase deploy --only hosting`
5. Use URL: `https://your-project.web.app/privacy-policy.html`

**Template Privacy Policy:**
```html
<!DOCTYPE html>
<html>
<head>
    <title>FrauduLens Privacy Policy</title>
</head>
<body>
    <h1>Privacy Policy for FrauduLens</h1>
    <p><strong>Last updated:</strong> [Date]</p>
    
    <h2>Information We Collect</h2>
    <p>FrauduLens collects the following information:</p>
    <ul>
        <li>Email address (for account creation)</li>
        <li>Username (optional)</li>
        <li>Phone number (for verification)</li>
        <li>Profile information from Facebook/Google (if you sign in with social accounts)</li>
    </ul>
    
    <h2>How We Use Your Information</h2>
    <p>We use your information to:</p>
    <ul>
        <li>Create and manage your account</li>
        <li>Verify your phone number</li>
        <li>Provide scam detection services</li>
        <li>Send important notifications</li>
    </ul>
    
    <h2>Data Security</h2>
    <p>We use Firebase Authentication and Firestore to securely store your data.</p>
    
    <h2>Contact Us</h2>
    <p>If you have questions about this privacy policy, contact us at: [your-email@example.com]</p>
</body>
</html>
```

---

### 3. User Data Deletion

**What you need:**
- Instructions on how users can delete their data
- URL to data deletion instructions
- Or a data deletion callback URL

**Quick solution:**

**Option A: Add to Privacy Policy**
Add a section to your privacy policy:
```html
<h2>Data Deletion</h2>
<p>To delete your account and all associated data:</p>
<ol>
    <li>Open the FrauduLens app</li>
    <li>Go to Settings</li>
    <li>Click "Delete Account"</li>
    <li>Confirm deletion</li>
</ol>
<p>Alternatively, contact us at [your-email@example.com] to request data deletion.</p>
```

**Option B: Create separate page**
Create `data-deletion.html` with deletion instructions
- Host it (GitHub Pages, Firebase Hosting, etc.)
- Add URL in Facebook App → **Settings** → **Basic** → **User Data Deletion**

**Option C: Use callback URL (Advanced)**
- Create an endpoint that handles data deletion requests from Facebook
- Requires backend server
- Not needed for basic login functionality

---

### 4. Category

**What you need:**
- Select a category that best describes your app

**How to set:**
1. Go to Facebook App → **Settings** → **Basic**
2. Find **Category** field
3. Select from dropdown:
   - **Business** (if it's for business use)
   - **Consumer** (if it's for personal use)
   - **Education**
   - **Entertainment**
   - **Finance**
   - **Games**
   - **Health & Fitness**
   - **Lifestyle**
   - **News**
   - **Photo & Video**
   - **Productivity**
   - **Shopping**
   - **Social**
   - **Sports**
   - **Travel**
   - **Utilities**
   - **Other**

**For FrauduLens, I recommend:**
- **Utilities** (scam detection tool)
- **Business** (if used for business security)
- **Consumer** (if used by individuals)

---

## ✅ Recommended Approach

### For Development/Testing (Now):

1. **Set App Mode to Development**
   - Go to **Settings** → **Basic**
   - Make sure it says "Development" mode
   - Add yourself as a test user

2. **Skip full submission for now**
   - You can test Facebook Login in Development Mode
   - Only you and test users can use it
   - Perfect for development

3. **Complete requirements later**
   - When you're ready to launch
   - Or when you need public access

### For Production (Later):

1. **Create Privacy Policy**
   - Use template above
   - Host on GitHub Pages or Firebase Hosting
   - Add URL to Facebook App

2. **Add App Icon**
   - Export 1024x1024 version of your logo
   - Upload to Facebook App

3. **Set Category**
   - Select appropriate category
   - Save

4. **Add Data Deletion Info**
   - Add section to privacy policy
   - Or create separate page

5. **Submit for Review**
   - Go to **App Review** → **Permissions and Features**
   - Request `email` and `public_profile` permissions
   - Submit for review

---

## 🎯 Quick Checklist

**For Development Mode (Can do now):**
- [ ] Set App Mode to Development
- [ ] Add test users (optional)
- [ ] Test Facebook Login works

**For Production (Do later):**
- [ ] Create Privacy Policy (host online)
- [ ] Add Privacy Policy URL to Facebook App
- [ ] Create 1024x1024 app icon
- [ ] Upload app icon to Facebook App
- [ ] Add data deletion instructions
- [ ] Set app category
- [ ] Submit for App Review

---

## 💡 Pro Tips

1. **Development Mode is enough** for testing Facebook Login
2. **Privacy Policy can be simple** - use the template above
3. **Host on GitHub Pages** - it's free and easy
4. **App icon** - just resize your existing logo
5. **Category** - pick the closest match, you can change it later

---

## 🚀 Next Steps

1. **For now:** Set to Development Mode and test Facebook Login
2. **Later:** Complete requirements when ready to launch publicly

You don't need to complete all requirements immediately to test Facebook Login!
