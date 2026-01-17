@echo off
echo ========================================
echo Getting SHA-1 Fingerprint for Firebase
echo ========================================
echo.

keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

echo.
echo ========================================
echo Look for the SHA1: line above
echo Copy the SHA-1 value (format: AA:BB:CC:DD:...)
echo Add it to Firebase Console > Project Settings > Your Apps > Android App
echo ========================================
pause
