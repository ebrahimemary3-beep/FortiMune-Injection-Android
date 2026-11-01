FortiMune Android 1.4.12 — versionCode 17

Install Supabase PATCH 7.7 after 7.6, then publish Website 7.7.
Upload this Android source including .github/workflows/build-apk.yml.
The workflow builds the release using the SAME existing signing secrets.
This package contains source, not a prebuilt or device-tested APK.
After building, test the APK, publish the release asset and only then set
Website/app-update.json available=true. The website can update without an APK.
Native report save/share and application identity are unchanged.

IVAS rebrand — Android 1.5.0, versionCode 18
App label is now "IVAS" (Innoval Vaccine Application System) with the new IVAS intro and launcher icon.
applicationId, signing key and release asset names are unchanged, so it updates in place over 1.4.12.
After the APK is built and tested, set Website/app-update.json to versionName 1.5.0 / versionCode 18.
