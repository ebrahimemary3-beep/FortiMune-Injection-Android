# FortiMune Android 1.2.0
Native OneSignal subscription binding and request opening are implemented in conjunction with Website v6.3 and the Supabase patch/function in this package.

Build: add the four ANDROID_* signing secrets to the Android repository first, then upload the contents of Android to its root, including .github/workflows/build-apk.yml. GitHub Actions assembles the signed release APK. The private signing HTML must never be uploaded to the repository. Keep it securely for all future releases.

This signing certificate differs from the installed v1.1.0 debug APK. The first transition needs removal of that debug APK, only after confirming there is no unsynced local work. Later v1.2.0+ releases built with this same key can update in place.

The native permission request appears from the app. Authenticated staff sessions are bound through a push-delivered device possession challenge. Account logout opts the device out and removes its registration; a subsequent account can opt back in. No OneSignal external_id/login is used. Push content is generic; tapping checks recipient, refreshes server-filtered state and checks current access before opening the request.

No Gradle/Android SDK toolchain is available here. Source checks and mocked behavior checks passed; the GitHub build and real-device account/notification tests are required before considering the release validated. iOS remains the existing PWA web-push path; no native iOS/APNs integration is included.
