# FortiMune Injection — Android Shell

This repository is the Android application shell for the FortiMune Injection Management system.

## Architecture

- Master application / business logic: `https://ebrahimemary3-beep.github.io/FortiMune-Injection-Management/`
- Backend: Supabase used by the Master web application.
- Android role: WebView shell + Android-specific permissions/download/back navigation.
- Business logic is intentionally NOT duplicated here.

## Upload to GitHub

Create a public/private repository named:

`FortiMune-Injection-Android`

Upload the contents of this folder to the repository root (do not upload the outer folder itself).

## Build automatically

The workflow in `.github/workflows/build-apk.yml` builds a release APK on every push to `main` and on manual workflow runs.

Go to **GitHub → Actions → Build Android APK → Artifacts** and download the APK.

## Important

Do not modify request/assignment/close/settlement business logic in this repository. Those features belong to the Master web application so one change is reflected on Android and iOS.
