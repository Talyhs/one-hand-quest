# Build the Android APK for Qara Qizil: Damci Ovchusu / One-Hand Quest.
# The Android project uses Gradle; Docker is only a reproducible build environment.
FROM mingc/android-build-box:latest

WORKDIR /workspace
COPY . .

RUN gradle --no-daemon assembleDebug

# To extract the APK from a built image, run the container with a mounted output
# directory. The APK is copied by the container entrypoint below.
CMD ["bash", "-lc", "mkdir -p /out && cp app/build/outputs/apk/debug/app-debug.apk /out/app-debug.apk && echo 'APK copied to /out/app-debug.apk'"]
