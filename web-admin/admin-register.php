<?php
  function inline_asset($path) {
    $full = __DIR__ . "/" . ltrim($path, "/");
    if (file_exists($full)) {
      echo file_get_contents($full);
    }
  }
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>FrauduLens Admin Registration</title>
    <style>
      <?php inline_asset("styles.css"); ?>
      .register-screen {
        min-height: 100vh;
        display: grid;
        place-items: center;
        gap: 16px;
        background: linear-gradient(180deg, #2f11cc 0%, #1e6ed1 45%, #2f11cc 100%);
      }
      .register-card {
        background: #ffffff;
        border-radius: 18px;
        padding: 26px 22px 26px;
        width: min(320px, 88%);
        display: grid;
        gap: 12px;
        text-align: center;
        box-shadow: 0 18px 30px rgba(0, 0, 0, 0.15);
      }
      .register-card input {
        border: 1px solid #d0d0d0;
        border-radius: 12px;
        padding: 10px 12px;
      }
      .register-error {
        color: #f04848;
        font-size: 12px;
        min-height: 16px;
      }
      .register-success {
        color: #2f11cc;
        font-size: 12px;
        min-height: 16px;
      }
      .register-tagline {
        text-align: center;
        color: #ffffff;
        font-size: 12px;
      }
    </style>
  </head>
  <body>
    <div class="register-screen">
      <div class="register-card">
        <h2>Admin Registration</h2>
        <p>Enter the setup code to create an admin account.</p>
        <input type="password" id="setup-code" placeholder="Setup code" />
        <input type="text" id="admin-name" placeholder="Full name" />
        <input type="email" id="admin-email" placeholder="Email" />
        <input type="password" id="admin-password" placeholder="Password (min 6 chars)" />
        <button class="solid-btn" id="register-btn">Create Admin</button>
        <p class="register-error" id="register-error"></p>
        <p class="register-success" id="register-success"></p>
      </div>
      <p class="register-tagline">Clear vision, Secure decisions</p>
    </div>

    <script src="https://www.gstatic.com/firebasejs/10.12.5/firebase-app-compat.js"></script>
    <script src="https://www.gstatic.com/firebasejs/10.12.5/firebase-auth-compat.js"></script>
    <script src="https://www.gstatic.com/firebasejs/10.12.5/firebase-firestore-compat.js"></script>
    <script>
      <?php inline_asset("firebase-config.js"); ?>
    </script>
    <script>
      // Change this to a strong secret and keep this page private.
      const ADMIN_SETUP_CODE = "ADMIN-SETUP-2026";

      const errorEl = document.getElementById("register-error");
      const successEl = document.getElementById("register-success");
      const registerBtn = document.getElementById("register-btn");

      if (!window.FIREBASE_CONFIG || FIREBASE_CONFIG.apiKey === "YOUR_API_KEY") {
        errorEl.textContent = "Firebase config missing.";
      } else {
        firebase.initializeApp(FIREBASE_CONFIG);
      }

      const auth = firebase.auth();
      const db = firebase.firestore();

      registerBtn.addEventListener("click", () => {
        const code = document.getElementById("setup-code").value.trim();
        const name = document.getElementById("admin-name").value.trim();
        const email = document.getElementById("admin-email").value.trim();
        const password = document.getElementById("admin-password").value.trim();

        errorEl.textContent = "";
        successEl.textContent = "";

        if (!code || code !== ADMIN_SETUP_CODE) {
          errorEl.textContent = "Invalid setup code.";
          return;
        }
        if (!name || !email || !password) {
          errorEl.textContent = "Please fill in all fields.";
          return;
        }

        auth.createUserWithEmailAndPassword(email, password)
          .then((cred) => {
            return db.collection("admin_users").doc(cred.user.uid).set({
              uid: cred.user.uid,
              name,
              email,
              role: "admin",
              createdAt: firebase.firestore.FieldValue.serverTimestamp()
            });
          })
          .then(() => {
            successEl.textContent = "Admin account created. You can now log in.";
          })
          .catch((error) => {
            errorEl.textContent = error.message || "Registration failed.";
          });
      });
    </script>
  </body>
</html>
