# SDK and Tools Inventory for Zapp

This document tracks all SDKs, runtimes, command-line tools, and environment variables installed or configured to develop and build the **Zapp** native Android application. It also provides clean uninstall and rollback instructions for every component.

---

## 1. Summary of Tools & Components

| Component | Version | Installation Method | Install Location | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Eclipse Temurin JDK 21 LTS** | `21.0.12.101` | `winget` | `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` | Java runtime required by the Android Gradle Plugin (AGP) and Gradle daemon |
| **Gradle Standalone** | `8.11.1` | Binary Archive | `C:\Users\robin\AppData\Local\Programs\gradle-8.11.1` | Used to bootstrap the project and initialize the Gradle Wrapper (`gradlew.bat`) |
| **Android SDK** *(Pre-existing)* | `35.0.0` / API 35 | Verified & Configured | `C:\Users\robin\AppData\Local\Android\Sdk` | Android platform, build tools, and platform tools (`adb`) |
| **Android Emulator** | `37.2.12` | `sdkmanager` | `C:\Users\robin\AppData\Local\Android\Sdk\emulator` | Virtual Android device runtime for desktop testing |
| **Android 35 System Image** | `google_apis;x86_64` (v9) | `sdkmanager` | `C:\Users\robin\AppData\Local\Android\Sdk\system-images\android-35\google_apis\x86_64` | Android 15 (API 35) x86_64 OS image for virtual device simulation |
| **scrcpy** | `4.1` | `winget` | Path added to User PATH | Ultra-low-latency desktop window mirror to view & interact with Android screens |
| **Gradle Wrapper** | `8.11.1` | Project Scaffolding | `C:\Robin\src\Zapp\gradle\wrapper` | Project-contained Gradle executable (`gradlew.bat`) so team/CI can build without global Gradle |

---

## 2. Detailed Descriptions & Changes Made

### 2.1 Eclipse Temurin JDK 21 LTS
* **Package ID:** `EclipseAdoptium.Temurin.21.JDK`
* **Why it was installed:** The system originally had OpenJDK 25. Android Gradle Plugin (AGP 8.x) does not yet support Java 25 as the Gradle execution runtime; it requires Java 17 or 21 LTS.
* **Environment Changes:**
  * System/User `PATH`: Added `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin`
  * User `JAVA_HOME`: Set to `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`

### 2.2 Gradle 8.11.1 (Standalone)
* **Source:** Official Gradle binary distribution (`gradle-8.11.1-bin.zip`)
* **Extracted to:** `C:\Users\robin\AppData\Local\Programs\gradle-8.11.1`
* **Why it was installed:** To provide a working CLI Gradle executable to generate the project's wrapper files (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`).
* **Environment Changes:**
  * User `PATH`: Added `C:\Users\robin\AppData\Local\Programs\gradle-8.11.1\bin`

### 2.3 Android SDK Configuration (Pre-existing on system)
* **Location:** `C:\Users\robin\AppData\Local\Android\Sdk`
* **Installed Packages Verified:**
  * `build-tools;35.0.0`
  * `platforms;android-35`
  * `platform-tools` (`adb` 37.0.0)
  * `cmdline-tools;latest`
* **Environment Changes:**
  * User `ANDROID_HOME`: `C:\Users\robin\AppData\Local\Android\Sdk`
  * User `ANDROID_SDK_ROOT`: Set to `C:\Users\robin\AppData\Local\Android\Sdk` (for backwards compatibility with tools expecting this legacy variable)

### 2.4 Project-Local Build Files & Dependencies
* **Location:** `C:\Robin\src\Zapp`
* **Scaffolded Structure:**
  * `gradle/libs.versions.toml`: Version catalog pinning AGP `8.7.2`, Kotlin `2.0.21`, Jetpack Compose BOM `2024.11.00`, Material 3
  * `settings.gradle.kts` & `build.gradle.kts`: Root Gradle configuration
  * `app/build.gradle.kts`: Module build script targeting SDK 35 with Compose compiler enabled
  * `app/src/main/...`: Modern Material 3 Jetpack Compose application source code
  * `local.properties`: Defines `sdk.dir=C\:\\Users\\robin\\AppData\\Local\\Android\\Sdk`
  * `gradlew.bat` / `gradlew`: Self-bootstrapping build scripts

---

## 3. Uninstall & Rollback Instructions

If you ever wish to completely remove or roll back these tools, run the following steps in Windows PowerShell:

### Step 1: Uninstall Eclipse Temurin JDK 21
To uninstall JDK 21 via `winget`:
```powershell
winget uninstall --id EclipseAdoptium.Temurin.21.JDK -e
```
*(Alternatively: Windows Settings > Apps > Installed apps > Search "Temurin 21" > Uninstall)*

To reset or remove the `JAVA_HOME` environment variable:
```powershell
# To revert JAVA_HOME back to JDK 25:
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot", [EnvironmentVariableTarget]::User)

# Or to completely remove JAVA_HOME:
[Environment]::SetEnvironmentVariable("JAVA_HOME", $null, [EnvironmentVariableTarget]::User)
```

To clean up JDK 21 from your User `Path` (if present):
```powershell
$userPath = [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::User)
$newPath = ($userPath -split ';' | Where-Object { $_ -notlike "*jdk-21*" }) -join ';'
[Environment]::SetEnvironmentVariable("Path", $newPath, [EnvironmentVariableTarget]::User)
```

---

### Step 2: Remove Standalone Gradle 8.11.1
To delete the extracted Gradle directory:
```powershell
Remove-Item -Recurse -Force "$env:LOCALAPPDATA\Programs\gradle-8.11.1"
```

To remove Gradle from your User `Path`:
```powershell
$userPath = [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::User)
$newPath = ($userPath -split ';' | Where-Object { $_ -notlike "*gradle-8.11.1*" }) -join ';'
[Environment]::SetEnvironmentVariable("Path", $newPath, [EnvironmentVariableTarget]::User)
```

---

### Step 3: Remove `ANDROID_SDK_ROOT` Environment Variable
If you wish to remove the auxiliary variable added for Android SDK compatibility:
```powershell
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $null, [EnvironmentVariableTarget]::User)
```
*(Note: Do not delete `C:\Users\robin\AppData\Local\Android\Sdk` unless you intend to completely remove your Android SDK and platform tools.)*

---

### Step 4: Uninstall Android Emulator & System Image
To remove the downloaded emulator and system image via `sdkmanager`:
```powershell
sdkmanager.bat --uninstall "emulator" "system-images;android-35;google_apis;x86_64"
```
Or to manually delete the folders:
```powershell
Remove-Item -Recurse -Force "$env:LOCALAPPDATA\Android\Sdk\emulator" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "$env:LOCALAPPDATA\Android\Sdk\system-images\android-35\google_apis" -ErrorAction SilentlyContinue
```
To remove any created Android Virtual Devices (AVDs):
```powershell
Remove-Item -Recurse -Force "$env:USERPROFILE\.android\avd\*" -ErrorAction SilentlyContinue
```

---

### Step 5: Clean or Remove Project Files in Zapp
To clean all compiled Gradle build outputs and caches within the project:
```powershell
cd c:\Robin\src\Zapp
.\gradlew.bat clean
Remove-Item -Recurse -Force .gradle, build, app/build -ErrorAction SilentlyContinue
```

To completely delete the scaffolded Zapp project files:
```powershell
Remove-Item -Recurse -Force "c:\Robin\src\Zapp\*"
```

