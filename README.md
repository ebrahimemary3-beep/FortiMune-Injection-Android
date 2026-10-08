FortiMune Android 1.4.2 — versionCode 5

This source builds the signed APK using .github/workflows/build-apk.yml.
The existing applicationId com.innoval.fortimuneinjection and existing signing secrets are preserved so this is an update to 1.3.0.
The WebView loads the deployed FortiMune website; publish Website/index.html too.
No new OneSignal keys or permissions are required for this update.
Image saving uses Android's document chooser; image sharing uses a private, non-exported FileProvider restricted to cache/image-exports.
PNG, JPEG and WebP are transferred in bounded ordered chunks only from the trusted main frame. Existing XLSX transfer and notification binding remain.
No signing key, API secret, Android SDK or compiled APK is included.
Build in GitHub Actions, then install FortiMune-Injection-v1.4.2.apk as an update using the same signing key.
