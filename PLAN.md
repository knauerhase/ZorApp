# Zapp Project Plan & Roadmap

This document outlines the master development plan for the **Zapp** native Android app. It tracks current progress, completed milestones, active tasks, and upcoming phases so you can instantly see where we are in the project lifecycle.

---

## Current Status Dashboard

| Metric | Status |
| :--- | :--- |
| **Current Phase** | **Phase 6: Gestures & Fluid Interactions (Complete) / Phase 7: Rotation & Slideshow** |
| **Overall Progress** | `[██████████████████░░] 90%` |
| **Last Completed Milestone** | Deployed Horizontal Paging swipe navigation & smooth double-tap zoom/pan and verified live |
| **Active Objective** | Review gesture handling and proceed to next automotive feature (90° Rotation or Slideshow) |

---

## Master Development Phases

### Phase 1: Toolchain Discovery & Setup
*Status: COMPLETED (100%)*
- [x] Audit Windows host environment and verify pre-existing tools (Git, Python, Android SDK).
- [x] Diagnose Java 25 vs. Android Gradle Plugin (AGP) incompatibility.
- [x] Install **Eclipse Temurin JDK 21 LTS** via `winget` and configure `JAVA_HOME`.
- [x] Install **Gradle 8.11.1** binary distribution to `LocalAppData\Programs\gradle-8.11.1` and add to `PATH`.
- [x] Configure Android SDK environment variables (`ANDROID_HOME`, `ANDROID_SDK_ROOT`).
- [x] Document all tools, installation paths, and rollback commands in [SDKandTools.md](file:///c:/Robin/src/Zapp/SDKandTools.md).

---

### Phase 2: Native Android Foundation & Baseline Build
*Status: COMPLETED (100%)*
- [x] Bootstrap standalone Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) targeting Gradle 8.11.1.
- [x] Create Gradle Version Catalog (`gradle/libs.versions.toml`) with Android Gradle Plugin 8.7.2, Kotlin 2.0.21, Jetpack Compose BOM 2024.11.00, and Material 3.
- [x] Create root project configuration (`settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `local.properties`, `.gitignore`).
- [x] Create module-level build script (`app/build.gradle.kts`) targeting `compileSdk 35` and `minSdk 26`.
- [x] Create application manifest (`app/src/main/AndroidManifest.xml`) with launcher activity and edge-to-edge theme.
- [x] Design vector icons (`ic_launcher_background.xml`, `ic_launcher_foreground.xml` with "Z" branding) and adaptive icon manifests.
- [x] Implement Material 3 Theme foundation (`Color.kt`, `Type.kt`, `Theme.kt`) with dynamic color support.
- [x] Implement initial `MainActivity.kt` with Compose UI, environment inspection, and reactive state demo.
- [x] Execute initial build (`.\gradlew.bat assembleDebug`) and verify APK output ([app-debug.apk](file:///c:/Robin/src/Zapp/app/build/outputs/apk/debug/app-debug.apk) - 9.45 MB).
- [x] Initialize chronological session log in [SESSION_NOTES.md](file:///c:/Robin/src/Zapp/SESSION_NOTES.md).

---

### Phase 3: Testing Platform & Display Targets
*Status: COMPLETED (100%)*
- **Primary Hardware Target:** **Pixel 11 Pro XL** (ready for physical deployment when device is connected)
- **Design Baseline Specification:** **2016 Cadillac CTS Infotainment Display**
  * **Screen Size:** 8.0-inch diagonal
  * **Native Resolution:** **800 x 480** pixels (WVGA, 5:3 / 15:9 aspect ratio, landscape)
  * **Design Principles:** Glanceable cards, high contrast, large touch targets (minimum 48-56dp) for in-vehicle legibility.
- [x] Install Android Emulator package (`emulator` 37.2.12) and Android 15 x86_64 system image.
- [x] Create and launch dedicated **`Cadillac_CTS_800x480`** virtual device.
- [x] Verified ADB connectivity & display dimensions (`Physical size: 800x480`, `Physical density: 160`).
- [x] Deploy live build to the test device (`adb install -r app-debug.apk`).
- [x] Verify live UI rendering and interactive touch events via ADB screencap.
- [x] Reserved physical Pixel 11 Pro XL pipeline for direct deployment when connected.

---

### Phase 4: Strict Offline-Only Architecture & Image Viewer Feature
*Status: COMPLETED (100%)*
- **Selected Architecture:**
  * **App Concept:** 100% offline image viewer & manager for `/sdcard/zapp` and system file picker.
  * **Network Policy:** **Strictly Zero Internet Connections** (`android.permission.INTERNET` is omitted from the manifest).
  * **Display Layout:** Optimized for **800 x 480 landscape** (2016 Cadillac CTS) while scaling responsively to Pixel 11 Pro XL.
  * **Supported Formats:** JPG, JPEG, PNG, and GIF (including animated GIFs via `coil-gif`).
  * **Privacy Guarantee:** Complete data isolation; zero telemetry, zero analytics, zero external network sockets.
- [x] Add Coil (`coil-compose:2.7.0` and `coil-gif:2.7.0`) to `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- [x] Configure storage permissions in `AndroidManifest.xml` (`READ_MEDIA_IMAGES`, `MANAGE_EXTERNAL_STORAGE`).
- [x] Implement `/sdcard/zapp` directory scanner in `MainActivity.kt`.
- [x] Implement system file picker (`ActivityResultContracts.GetContent()`) for user-selected images.
- [x] Build 2-column landscape UI (Left: thumbnail strip & file browser; Right: full viewport with Prev/Next).
- [x] Create and push sample JPG, PNG, and GIF test assets to `/sdcard/zapp/` on the Cadillac CTS emulator.
- [x] Compile APK, deploy to emulator, and verify interactive rendering of all image formats.

---

### Phase 5: Image Scaling, Aspect Ratio Adaptation & Viewer Controls
*Status: COMPLETED (100%)*
- **Current Objective:** Tactile Scale Mode Toggle & Option 2 Full-Screen Mode for Cadillac CTS 800x480 screen.
- [x] Implement Scale Mode Toggle (`Fit` vs. `Fill / Crop`) in `MainActivity.kt`:
  * **Fit Mode:** Shows complete image uncropped (with side/top letterboxing).
  * **Fill Mode:** Zooms & crops to fill entire 800x480 screen edge-to-edge.
  * **Controls:** Added bottom bar tactile button (`Scale: Fit/Fill`) and quick top-right viewport chip.
- [x] Implement **Option 2 Full-Screen Mode**:
  * **Trigger:** Tapping anywhere on the photo preview or tapping the `⛶ Fullscreen` / `⛶ Full` buttons.
  * **Full Immersion:** 100% of the 800x480 screen dedicated to the image (top app bar and left column completely hidden).
  * **Tactile "← Return to Menu" Button:** Prominent, high-contrast 48dp+ floating button pinned in top-left corner.
  * **In-Fullscreen Controls:** Floating Scale toggle (`Fit` / `Fill`), prominent `‹` / `›` navigation arrows, and file counter.
  * **System Back Handler:** Hardware back button / gesture cleanly exits full-screen back to album browser.
- [x] Compile APK, deploy to Cadillac CTS emulator, and verify interactive scaling & full-screen controls.

---

### Phase 6: Gestures & Fluid Interactions (Swipe & Double-Tap / Pinch to Zoom)
*Status: COMPLETED (100%)*
- **Current Objective:** Natural mobile touch gestures for both normal preview and full-screen viewer.
- [x] Implement **HorizontalPager Integration**:
  * Added `HorizontalPager` with `pagerState` synchronized bi-directionally with `selectedIndex` and the thumbnail list.
  * **Swipe to Navigate:** Flick / swipe left or right across the photo to smoothly glide to adjacent images with native physics and slide animations.
  * Enabled in both 2-column album preview and edge-to-edge full-screen mode.
- [x] Implement **ZoomableImageContainer**:
  * **Double-Tap to Zoom:** Double-tapping smoothly animates zoom between 1.0x and 2.5x with `animateFloatAsState`.
  * **Pinch-to-Zoom & Pan:** Multi-touch gesture scaling from 1.0x up to 5.0x with bounded offset panning so images never get lost off-screen.
  * **Gesture Contention Safety:** Automatically locks `userScrollEnabled = false` on `HorizontalPager` when zoomed in (`scale > 1.05f`) so panning does not accidentally flip pages.
  * **Zoom Level Indicator:** Subtle badge (e.g., `2.5x`) appears when zoomed in.
- [x] Compile APK, deploy to Cadillac CTS emulator, and verify interactive swiping, double-tap zoom, and panning.

---

### Phase 6B: ZorApp Branding & Cat Launcher Icon
*Status: COMPLETED (100%)*
- [x] **Application Rebranding:**
  * Renamed app display title to **ZorApp** (`strings.xml`, `TopAppBar`, offline badge).
  * Updated Gradle project root name to `ZorApp` in `settings.gradle.kts`.
  * Created updated screen viewer script `view_zorapp_screen.bat`.
  * Preserved local folder structure at `c:\Robin\src\Zapp` for complete stability.
- [x] **Cat Face Landmark Analysis & Icon Extraction:**
  * Analyzed `sample_images/PXL_20260913_044249065~2.jpg` (3072x4080 portrait).
  * Calculated optimal square crop centered on eyes/muzzle (`(725, 1055, 1975, 2305)`).
  * Preserved ear silhouettes and bright yellow-green eyes for maximum contrast and glanceability.
  * Generated adaptive icon foregrounds (`ic_launcher_cat_foreground.png`), master avatars (`cat_avatar.png`), and full mipmap density suites (mdpi through xxxhdpi).
  * Integrated cat avatar in `MainActivity.kt` TopAppBar with purple border accent.
  * Verified live on Cadillac CTS Android 15 emulator launcher screen and in-app header.

---

### Phase 8: Smart Frame Engine (AI-Guided Subject Framing)
*Status: COMPLETED (100%)*
- [x] **3-Way Display Mode Selector:**
  * Defined `DisplayMode`: `SMART_FRAME` (AI Subject Focused), `FIT` (Letterbox Full), `FILL` (Brute-force Center Crop).
  * Single-tap tactile cycle button (`Smart Frame` ➔ `Fit` ➔ `Fill`) in both 2-column album view and edge-to-edge full screen.
  * Glanceable emerald status badges: `🐾 Cat Focused`, `👥 Multi-Subject`, `🎯 Subject Focused`.
- [x] **Smart Framing Mathematical Engine:**
  * Headroom protection padding (ensures feline ears are never chopped off).
  * Rule-of-Thirds eye-line placement (38–40% display height).
  * Body vector expansion allocating 5:3 landscape viewport around torso and paws.
  * Multi-subject union bounding box calculation.
  * Graceful fallback to `Fit` when no subjects are detected.
- [x] **On-Device Offline Inference Pipeline:**
  * Embedded quantized `efficientdet_lite0.tflite` (4.35 MB) in `app/src/main/assets/`.
  * Configured `androidResources { noCompress += "tflite" }` for zero-copy memory mapping.
  * Benchmarked at <30ms on-device inference with zero internet permissions.
  * Asynchronous coroutine pre-fetching of adjacent images (`Dispatchers.Default`).
- [x] **Live Verification:**
  * Live comparison between `Smart Frame`, `Fit`, and `Fill` verified on Cadillac CTS 800×480 screen with user's cat portrait (`PXL_20260913_044249065~2.jpg`).
  * Documented autonomous execution logs in [AUTONOMOUSPhase8.md](file:///c:/Robin/src/Zapp/AUTONOMOUSPhase8.md).

---

### Future Enhancements Backlog (Saved for Next Phases)
*Status: QUEUED*
- [ ] **90° Rotation Toggle:** Quick-rotate button to turn portrait photos into landscape orientation.
- [ ] **Hands-Free Slideshow Mode:** Auto-advancing photo cycling with configurable delay (e.g., 5s, 10s) and Play/Pause button.
- [ ] **EXIF Orientation Auto-Detection:** Automatically read EXIF tags to ensure photos always load right-side up.
- [ ] **Brightness & Contrast Filter:** Car-night-mode dimmer for comfortable driving in low light.

---

### Phase 6: Testing, Polish & Optimization
*Status: QUEUED*
- [ ] Unit testing (JUnit, Kotlin Coroutines test).
- [ ] Compose UI tests (Compose Test Rule, Espresso).
- [ ] App size optimization (R8 shrinking, ProGuard rule tuning).
- [ ] Performance profiling (startup time, memory usage, frame drops).

---

### Phase 7: Release Preparation & Distribution
*Status: QUEUED*
- [ ] Configure release signing keystore and build variant (`assembleRelease`, `bundleRelease`).
- [ ] Generate Android App Bundle (`.aab`) for Google Play or standalone release APK.
- [ ] Final release documentation and deployment guidelines.

---

## Quick Reference Links
* **Installed Tools & Rollback:** [SDKandTools.md](file:///c:/Robin/src/Zapp/SDKandTools.md)
* **Chronological Session Log:** [SESSION_NOTES.md](file:///c:/Robin/src/Zapp/SESSION_NOTES.md)
* **Latest Debug APK:** [app-debug.apk](file:///c:/Robin/src/Zapp/app/build/outputs/apk/debug/app-debug.apk)
