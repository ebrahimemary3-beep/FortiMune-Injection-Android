FortiMune Android 1.4.5 — versionCode 10

Build the signed APK with .github/workflows/build-apk.yml using existing signing secrets.
Application ID remains com.innoval.fortimuneinjection. Keep the existing release key to install as Update.
This folder contains source, not a built APK. Install Supabase 7.1 and publish Website 7.1 too.

Report capture:
- Explicit HTML capture opens ACTION_IMAGE_CAPTURE after runtime CAMERA permission is granted.
- Output uses a private cache/report-camera FileProvider path, temporary URI grants and full-size JPEG.
- Gallery selection is independent and does not request CAMERA permission.
- Denial, camera cancellation, absent camera app and empty output complete the WebView callback safely.
- The existing export provider, Excel/image export and notification binding are preserved.

Java syntax was parsed locally. APK compilation, signature and device camera behavior must be verified
in GitHub Actions and on the installed Android 1.4.5; an Android SDK/signing key is not available here.
