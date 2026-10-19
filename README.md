# One-Hand Quest (Android)

Portrait arcade game starter built with native Android Canvas. Includes 120 progressive levels, three lanes, swipe/tap controls, collectible orbs, red hazards, score, coins, and local save data.

## Build APK using GitHub Actions
1. Open **Actions** and enable workflows if prompted.
2. Run **Build Android APK** using **Run workflow**, or push a commit.
3. Download the `one-hand-quest-debug-apk` artifact from the completed workflow run.

## Local build
Open this project in Android Studio with JDK 17 and Android SDK 35 installed, sync Gradle, then use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

The debug APK is for testing, not a signed Play Store release. Test on a real device before release.
