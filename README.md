# My Daily Hisab - Production Android Studio Project

Native Android application built with **Jetpack Compose**, **Room Database** for offline-first local persistence, and **Firebase Authentication & Cloud Firestore** for private cross-device sync.

## Project Structure
- `app/`: Main application module containing all Kotlin Jetpack Compose screens, ViewModels, Room Entities/DAOs, and Firebase managers.
  - `app/google-services.json`: Firebase client configuration for `com.mydailyhisab.app`.
  - `app/build.gradle.kts`: Module build script targeting Android SDK 35 (Android 15) and Min SDK 29 with Java 17.
- `gradle/`: Gradle version catalog (`libs.versions.toml`) and official Gradle Wrapper (`gradle-wrapper.jar` v8.9).
- `build.gradle.kts` & `settings.gradle.kts`: Root Gradle configuration with Kotlin 2.0.21, AGP 8.7.0, and KSP.
- `gradlew` & `gradlew.bat`: Official Gradle Wrapper executables.

## Requirements
- **JDK 17** (Temurin, OpenJDK 17, or Android Studio bundled JDK 17)
- **Android Studio Ladybug (2024.2+)** or newer
- **Android SDK Platform 35** and Build-Tools

## Building Locally
```bash
# Make wrapper executable (Linux/macOS)
chmod +x ./gradlew

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```
The generated APK will be placed in `app/build/outputs/apk/debug/app-debug.apk`.

## Building on Codemagic CI/CD
1. Push this repository root to GitHub.
2. In Codemagic, select **Android** app type.
3. Build configuration:
   - JDK: 17
   - Build step: `./gradlew assembleDebug`
   - Artifacts: `app/build/outputs/apk/**/*.apk`
