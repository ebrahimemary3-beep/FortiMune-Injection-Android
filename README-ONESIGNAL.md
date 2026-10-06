# FortiMune Android 1.1.0 — OneSignal device test

This milestone adds native Android push reception. OneSignal FCM settings have
already been configured in the dashboard. SDK 5.10.2 is pinned.

## Build

Upload the project files to the existing FortiMune-Injection-Android repository,
keeping their paths. Keep `.github/workflows/build-apk.yml` too.
GitHub Actions builds `FortiMune-Injection-v1.1.0-debug.apk` on push to `main`.
No Android Studio or administrator installation is needed on the user's PC.

Changed files:
- app/build.gradle
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/fortimune/injection/MainActivity.java
- app/src/main/java/com/fortimune/injection/FortiMuneApplication.java (new)
- app/src/main/res/drawable-*/ic_stat_onesignal_default.png (new, five densities)
- .github/workflows/build-apk.yml

## Device verification

1. Build on GitHub Actions and install the debug APK on an Android phone with
   Google Play Services.
2. Launch the installed FortiMune app. On Android 13+, allow notifications.
3. In OneSignal, check Audience > Subscriptions for a subscribed Android device.
4. Mark that device as a test user and send a generic test push to it only.
5. Put FortiMune in the background and verify the notification uses FortiMune's
   application name rather than Chrome. Tap it and verify FortiMune opens.

Android controls the permission dialog; older Android versions may not display
it. Denial does not trigger a settings redirect. Firebase service-account files,
REST API keys and identity signing keys must not be included in this repository.
`google-services.json` is not required for this OneSignal-only setup.

## Scope and remaining work

This is a device-reception test build, not the finished account workflow.
It does not call OneSignal.login or associate the device with Sayed/Jana.
Secure account binding, server-targeted request notifications, request deep links,
and the existing web notification stale-data fix remain for the next milestone.
The existing web app and Supabase service have not been changed by this package.

The existing workflow uses temporary debug signing. If Android rejects an update
because the previous APK has a different signing certificate, do not uninstall
until local unsynced work has been checked. Stable production signing is a later
release requirement.

## Validation

XML, PNG densities, source wiring, workflow YAML and ZIP integrity were checked
locally. No Android toolchain/device is available in this workspace; successful
Gradle build and real-device delivery still need the GitHub/device steps above.
