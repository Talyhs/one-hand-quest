# Qara Qızıl: Damcı Ovçusu (Android)

Azerbaijani portrait arcade game with a dark industrial/oil-rig theme, glowing gold drops, combo scoring, haptic feedback, and one-finger controls. Move the bucket with one finger to catch golden oil drops, avoid gray stones, earn coins, beat your record, and progress through 999 levels. Progress and best score are saved locally on the device.

## Build APK with GitHub Actions
1. Open the **Pull requests** tab and create/merge the feature branch PR, or use **Actions → Build Android APK → Run workflow** and select `feature/black-gold-drop-catcher` if available.
2. Wait for the workflow to finish successfully.
3. Open the completed workflow run and download the `qara-qizil-debug-apk` artifact.

The debug APK is for testing and is not a signed Play Store release. Test it on a real Android device before release.

## Local build
Open this project in Android Studio with JDK 17 and Android SDK 35 installed, sync Gradle, then choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
