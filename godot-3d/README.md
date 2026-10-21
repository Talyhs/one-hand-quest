# Qara Qızıl: Damcı Ovçusu — 3D prototype

This is a separate Godot 4 project beside the existing Java/Android prototype. All scene objects are generated from built-in 3D meshes and materials, so it has no third-party art-asset licensing dependency.

## Included
- Portrait-first 3D industrial oil-rig scene, metallic bucket, glowing gold drops and stone hazards
- One-finger touch and mouse movement
- Lives, score, combo, coins, best score and progressive 999-level design
- Local progress save and procedural sound effects
- Godot Compatibility renderer for wider mobile GPU support

## Run and validate
Open this folder in Godot 4.x and run the main scene. This source prototype is not a verified Android APK yet. Android export and real-device install/launch testing are still required before calling it release-ready.


## CI note
The Android workflow installs the exact Godot 4.4.1 export templates, Java 17, Android SDK platform/build tools using the runner's explicit sdkmanager path, and a temporary debug keystore before exporting. A successful CI export is required before treating the APK as ready.
