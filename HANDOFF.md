# ZorApp Developer Handoff & Environment Bootstrap Guide 🐾

This document contains everything needed for another instance of **Antigravity** (or a human software engineer) on a fresh machine to install all prerequisites in the exact correct order, bootstrap the environment, build, test, and continue developing **ZorApp**.

---

## 1. Project Identity & Architecture Overview

* **Repository:** [https://github.com/knauerhase/ZorApp](https://github.com/knauerhase/ZorApp) (Public GitHub repository)
* **Local Workspace Convention:** `C:\Robin\src\Zapp` (Directory name `Zapp` is preserved locally for path stability; public project, branding, and Android activity names are **ZorApp**).
* **Target Audience & Form Factor:** In-cabin vehicle infotainment center consoles (modeled on the 2016 Cadillac CTS 8.0" 800×480 WVGA landscape touchscreen) while responsively scaling to modern high-density Android smartphones (e.g. Google Pixel 11 Pro XL).
* **Strict Privacy / Air-Gap Policy:** **Zero Internet Permissions.** `android.permission.INTERNET` must NEVER be added to `AndroidManifest.xml`. All image loading, caching, and AI subject framing must execute 100% on-device.

### Key Technology Stack:
* **Language:** Kotlin 2.0.21
* **UI Framework:** Jetpack Compose (BOM 2024.11.00) with Material 3
* **Image Pipeline:** Coil 2.7.0 (`coil-compose`, `coil-gif`) with hardware acceleration
* **On-Device AI Engine:** TensorFlow Lite Task Vision (`org.tensorflow:tensorflow-lite-task-vision:0.4.4`) with embedded `efficientdet_lite0.tflite` (4.35 MB INT8 quantized model)
* **Build System:** Gradle 8.11.1 Wrapper + Android Gradle Plugin (AGP) 8.7.2
* **Target SDK:** `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26` (Android 8.0 Oreo+)
* **Code Optimization:** R8 bytecode minification, resource shrinking, and native ARM64 stripping (~13.3 MB release APK)

---

## 2. Prerequisites & Installation Order on a Fresh Machine

To reproduce this environment on a new Windows workstation, execute the following steps in order:

### Step 1: Install Git & Winget
Ensure Windows Package Manager (`winget`) and Git are installed.
```powershell
winget --version
git --version
```

### Step 2: Install Eclipse Temurin JDK 21 LTS
> [!IMPORTANT]
> **Do not use Java 25 or Java 22.** Android Gradle Plugin 8.7.x requires Java 17 or Java 21 LTS as its Gradle execution daemon.

Install via `winget`:
```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e --source winget
```
* **Install Location:** `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`
* **Set Environment Variables:**
  ```powershell
  [Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot", [EnvironmentVariableTarget]::User)
  
  # Ensure JDK 21 bin is first in User PATH
  $oldPath = [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::User)
  $newPath = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin;" + ($oldPath -split ';' | Where-Object { $_ -notlike "*jdk-21*" }) -join ';'
  [Environment]::SetEnvironmentVariable("Path", $newPath, [EnvironmentVariableTarget]::User)
  ```

### Step 3: Install Android SDK & Command-Line Tools
If Android Studio or Android SDK is not yet present on the machine:
1. Set the Android SDK root directory (standard path: `C:\Users\<user>\AppData\Local\Android\Sdk`).
2. Set environment variables:
   ```powershell
   [Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", [EnvironmentVariableTarget]::User)
   [Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", "$env:LOCALAPPDATA\Android\Sdk", [EnvironmentVariableTarget]::User)
   ```
3. Install required SDK components using `sdkmanager`:
   ```powershell
   cd "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin"
   .\sdkmanager.bat --licenses
   .\sdkmanager.bat "platform-tools" "platforms;android-35" "build-tools;35.0.0" "emulator" "system-images;android-35;google_apis;x86_64"
   ```
4. Add platform-tools (`adb`) and emulator to `PATH`:
   ```powershell
   $oldPath = [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::User)
   $sdkPaths = "$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:LOCALAPPDATA\Android\Sdk\emulator"
   [Environment]::SetEnvironmentVariable("Path", "$sdkPaths;$oldPath", [EnvironmentVariableTarget]::User)
   ```

### Step 4: Install `scrcpy` (Desktop Screen Mirroring)
To mirror the emulator or connected phone to an interactive desktop window:
```powershell
winget install Genymobile.scrcpy
```

### Step 5: Clone the Repository
```powershell
git clone https://github.com/knauerhase/ZorApp.git "C:\Robin\src\Zapp"
cd "C:\Robin\src\Zapp"
```

### Step 6: Configure `local.properties`
Create `local.properties` in the project root pointing to the local Android SDK:
```properties
sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
```

---

## 3. How to Build the Project

Always use the included Gradle Wrapper (`gradlew.bat`):

### Compile Debug APK:
```powershell
.\gradlew.bat assembleDebug --no-daemon
```
* **Output Path:** `app/build/outputs/apk/debug/app-debug.apk` (Un-minified debug build with developer logs).

### Compile Stripped ARM64 Release APK:
```powershell
.\gradlew.bat assembleRelease --no-daemon
```
* **Output Path:** `app/build/outputs/apk/release/app-release.apk`
* **Features:** R8 minified, dead code eliminated, resources shrunk, debug symbols stripped, and x86/x86_64 emulator bloat removed. Signed with the debug keystore for instant installation on any device without credential setup.
* **Size:** **~13.3 MB**

---

## 4. Emulator & Testing Setup (Cadillac CTS 800×480)

The project includes pre-configured batch scripts to reproduce the exact automotive head-unit environment:

### Step 1: Create the Cadillac CTS Virtual Device (AVD)
If setting up on a new PC, create the AVD with the `system-images;android-35;google_apis;x86_64` system image:
```powershell
avdmanager create avd -n Cadillac_CTS_800x480 -k "system-images;android-35;google_apis;x86_64" --device "7in WSVGA (Tablet)" --force
```
Then edit `~/.android/avd/Cadillac_CTS_800x480.avd/config.ini` to enforce the exact Cadillac CTS display hardware profile:
```ini
hw.lcd.width=800
hw.lcd.height=480
hw.lcd.density=160
PlayStore.enabled=no
```

### Step 2: Launch the Emulator
Run the included root batch script:
```powershell
.\launch_cadillac_cts_emulator.bat
```
*(Starts the emulator in a background process targeting `emulator-5554`)*.

### Step 3: Install & Deploy ZorApp
```powershell
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5554 shell am start -n com.example.zapp/.MainActivity
```

### Step 4: Launch Interactive Desktop Mirror (`scrcpy`)
Run the included root batch script:
```powershell
.\view_zorapp_screen.bat
```
*(Opens an ultra-low-latency 60fps window displaying the live 800×480 touch display).*

### Step 5: Push Test Sample Images to Device
ZorApp scans the dedicated `/sdcard/zorapp/` folder. Push photos over ADB:
```powershell
adb shell mkdir -p /sdcard/zorapp
adb push sample_images/* /sdcard/zorapp/
```
*(Tap **Rescan** in the app to immediately index all pushed images).*

---

## 5. Codebase Map & Key Components

| File / Path | Purpose & Responsibilities |
| :--- | :--- |
| `app/src/main/java/com/example/zapp/MainActivity.kt` | **Primary UI Controller:** Compose `Scaffold`, 2-column landscape layout, `HorizontalPager` swipe carousel, touch gestures (double-tap & pinch zoom), TopAppBar with Zora avatar, storage scanner (`/sdcard/zorapp`), file picker launcher, and `StartSafetyDialog` modal. |
| `app/src/main/java/com/example/zapp/smartframe/SmartFrameEngine.kt` | **On-Device AI Subject Framing:** Initializes TensorFlow Lite `ObjectDetector` (`efficientdet_lite0.tflite`). Calculates feline ear headroom protection, Rule-of-Thirds eye-line placement (38–40% height), body-vector expansion, and fallback geometry for multiple subjects or landscapes. |
| `app/src/main/assets/efficientdet_lite0.tflite` | **Neural Network Binary:** 4.35 MB quantized INT8 TensorFlow Lite model covering 80 COCO categories (cats, dogs, persons, vehicles, food, sports, etc.). Configured as uncompressed in `androidResources { noCompress += "tflite" }`. |
| `app/src/main/res/drawable/cat_avatar.png` | **Zora Portrait Asset:** Used for launcher adaptive icon and header/start-screen avatar. |
| `app/build.gradle.kts` | **Module Build Config:** SDK 35, Java 21, Compose compiler, Coil dependencies, TensorFlow Lite Vision dependency, R8 minification, and ARM64 ABI filter. |
| `app/proguard-rules.pro` | **ProGuard Keep Rules:** Protects TFLite JNI entry points and suppresses compile-only AutoValue warnings. |
| `gradle/libs.versions.toml` | **Version Catalog:** Centralized dependency versions (Compose, Coil, TFLite, Kotlin, AGP). |

---

## 6. Critical Architectural Rules & Commitments

When continuing development on this codebase, adhere strictly to these rules:

1. **NO INTERNET PERMISSION:**
   Never add `android.permission.INTERNET`. Zero external API calls, zero analytics, zero telemetry.
2. **AUTOMOTIVE ERGONOMICS (800×480 WVGA Baseline):**
   * All touch targets must be at least **48dp × 48dp**.
   * High contrast dark theme (`#0F172A` / `#1E293B` / `#090D16`) for night-driving readability and OLED efficiency.
   * Large typography (minimum 12–14sp for labels, 18sp for headers).
3. **DRIVER SAFETY SCREEN:**
   * ZorApp must display the startup driver safety warning (*"Do not use this app while driving. Operate only when vehicle is safely parked"*).
   * Tapping the top-bar Zora avatar or the **ℹ️ Info & Safety** chip must always re-open this dialog.
4. **STORAGE DIRECTORY IS `/sdcard/zorapp`:**
   * All direct folder scanning is isolated to `/sdcard/zorapp`. Legacy `/sdcard/zapp` has been permanently removed.
5. **KEEP RELEASES LEAN (<15 MB):**
   * When publishing release APKs, ensure `abiFilters` are constrained to ARM (`arm64-v8a`) to avoid bundling ~20 MB of PC emulator C++ libraries into phone binaries.
6. **HELP SCREEN POLICY (DO NOT IMPLEMENT YET):**
   * The user has requested to *discuss* whether and how to put a dedicated Help screen in the app, but explicitly instructed **NOT to implement it yet**. Wait for explicit user direction before writing code for an in-app help screen.

---

## 7. Future Roadmap & Feature Backlog

Features queued for future phases:
* **Slideshow Auto-Advance:** Configurable timer (e.g. 5s, 10s, 30s) to cycle through `/sdcard/zorapp` images hands-free.
* **Car Night-Mode Dimmer:** One-tap luminance filter overlay to prevent headlight glare during night driving.
* **90° Manual Rotation Button:** Fast orientation override button for stubborn un-rotated photos.
* **EXIF Orientation Auto-Detection:** Automatically inspect EXIF metadata tags to normalize rotated images before rendering.
* **Help Screen Alignment:** Plan and align with user on in-app user guide implementation.

---

## 8. Summary of Useful Quick Commands

```powershell
# 1. Clean and build debug APK
.\gradlew.bat clean assembleDebug --no-daemon

# 2. Build lean 13.3 MB ARM64 Release APK
.\gradlew.bat assembleRelease --no-daemon

# 3. Launch Cadillac CTS Emulator
.\launch_cadillac_cts_emulator.bat

# 4. Install onto emulator
adb -s emulator-5554 install -r app\build\outputs\apk\release\app-release.apk

# 5. Mirror screen to desktop window
.\view_zorapp_screen.bat

# 6. Push test images
adb shell mkdir -p /sdcard/zorapp
adb push sample_images/* /sdcard/zorapp/

# 7. Check git status
git status
```
