# ZorApp User Guide 🐾

Welcome to **ZorApp**, the privacy-first, offline image viewer designed specifically for vehicle infotainment center consoles (modeled on the 2016 Cadillac CTS 800×480 WVGA display) and modern Android devices (Google Pixel 11 Pro XL).

---

## 📑 Table of Contents
1. [Quick Start](#1-quick-start)
2. [Adding Your Photos](#2-adding-your-photos)
3. [The 3 Display Modes](#3-the-3-display-modes)
4. [Gestures & Navigation](#4-gestures--navigation)
5. [Automotive Ergonomics](#5-automotive-ergonomics)
6. [Privacy & Air-Gapped Security](#6-privacy--air-gapped-security)
7. [Troubleshooting & FAQs](#7-troubleshooting--faqs)

---

## 1. Quick Start

When you launch ZorApp, you will see a high-contrast 2-column dashboard:
* **Left Column:** Your local photo album, file picker, and storage scanner.
* **Right Column:** The active image preview with live **Smart Frame** subject tracking and quick controls.

Tap any photo in the list (or tap **⛶ Fullscreen**) to enter edge-to-edge full-screen immersion.

---

## 2. Adding Your Photos

ZorApp is 100% air-gapped and reads images directly from local device storage without ever touching the internet.

### Option A: Direct Folder Sync (Recommended)
Place your pictures directly into either of these directories on your phone, SD card, or automotive head-unit storage:
* `/sdcard/zorapp/`
* `/sdcard/zapp/`

> [!TIP]
> Whenever you copy new photos over USB, simply tap the **Rescan** button in the left panel. ZorApp will automatically detect, index, and sort all new images in under a second.

### Option B: System File Picker
Tap **Pick File** at the top of the album panel to select any photo or animated GIF from Google Photos, Downloads, or your device files.

### Supported File Formats:
* **JPG / JPEG:** Standard photos and high-resolution camera captures.
* **PNG:** Digital graphics and screenshots with full transparency support.
* **GIF:** Animated GIFs with smooth, hardware-accelerated playback.

---

## 3. The 3 Display Modes

Vehicle displays (like the Cadillac CTS's 5:3 widescreen) have very different proportions than vertical smartphone photos. ZorApp provides three viewing modes, switchable with a single tap of the mode button:

```
[ 🐾 Mode: Smart Frame ]  ──►  [ 📐 Mode: Fit (Full) ]  ──►  [ 🔍 Mode: Fill (Crop) ]
            ▲                                                                │
            └────────────────────────────────────────────────────────────────┘
```

### 1. 🐾 Mode: Smart Frame (AI-Guided Focus) — *Default*
* **How it works:** A built-in, on-device neural network (TensorFlow Lite EfficientDet) detects subjects in real time (<30ms).
* **Cat & Pet Ergonomics:** 
  - **Headroom Protection:** Ensures feline ears and foreheads are never cut off.
  - **Rule-of-Thirds Composition:** Places the cat’s eye-line at 38–40% of the screen height for natural balance.
  - **Body Vector Inclusion:** Allocates landscape screen space downward along the torso to include paws, loaf body, and collar tags.
* **Multi-Subject Tracking:** When 2+ cats or people are present, it frames the entire group together.
* **Glanceable Badge:** Displays real-time status in the corner: `🐾 Cat Focused`, `👥 2 Cats Focused`, or `🎯 Subject Focused`.

### 2. 📐 Mode: Fit (Full Image)
* Shows the complete, original photo exactly as taken.
* Preserves 100% of the image with letterbox bars on the sides.
* Ideal when you want to view the entire original composition.

### 3. 🔍 Mode: Fill (Center Crop)
* Blows up the photo edge-to-edge to completely fill the screen with zero black bars.
* Centers on the midpoint of the image.
* Best for landscape photos where the action is dead-center.

---

## 4. Gestures & Navigation

ZorApp is built with native touch physics tailored for smooth in-car interaction:

| Gesture | Action | What Happens |
| :--- | :--- | :--- |
| **1-Finger Horizontal Swipe** | `Swipe Left / Right` | Glides smoothly to the next or previous photo in your album. |
| **Double-Tap** | `Double-Tap Screen` | Instantly zooms into fine details (1.0x ↔ 2.5x). Double-tap again to reset. |
| **Pinch-to-Zoom** | `2-Finger Pinch` | Smoothly scales anywhere from 1.0x up to 5.0x zoom. |
| **1-Finger Pan** | `Drag when Zoomed` | Pans across the zoomed photo. (Page-flipping is automatically locked while zoomed so you never lose your place). |
| **Tap Return Button** | `← Return to Menu` | Returns instantly from full-screen mode back to the 2-column album dashboard. |
| **Tap Next / Prev Arrows** | `‹` and `›` | Physical 52dp circular buttons for quick 1-tap photo cycling without swiping. |

---

## 5. Automotive Ergonomics

* **Glanceable Dark Theme:** Deep slate-black palette (`#090D16`) reduces eye fatigue during night driving and blends into car interiors.
* **Large Touch Targets:** All buttons are designed to **48dp+** specifications, preventing missed taps on bumpy roads.
* **Automatic Screen Adaptation:** Whether displayed on an 800×480 Cadillac CTS, a 1024×600 Android head-unit, an ultrawide 1920×720 dashboard, or a Pixel 11 Pro XL, the layout dynamically adjusts its proportions without manual configuration.

---

## 6. Privacy & Air-Gapped Security

> [!IMPORTANT]
> **ZorApp is strictly air-gapped.**
> * `android.permission.INTERNET` is completely omitted from the application.
> * ZorApp cannot connect to the internet, make socket connections, or upload photos.
> * All artificial intelligence (TFLite object detection) runs 100% on-device using local CPU/NNAPI hardware.
> * Your family and pet photos never leave your hardware.

---

## 7. Troubleshooting & FAQs

#### Q: I copied pictures to `/sdcard/zorapp/` but don't see them.
**A:** Tap the **Rescan** button in the left panel. On Android 11+, ensure all-files storage permission is granted if prompted.

#### Q: What if a photo doesn't have a cat?
**A:** Smart Frame automatically searches for other subjects (dogs, people, cars, objects). If no subject is recognized, it gracefully applies a balanced Rule-of-Thirds framing for portrait photos, or falls back cleanly to `Fit` mode.

#### Q: How do I exit Fullscreen mode?
**A:** Tap the tactile **← Return to Menu** button in the top-left corner, or use the Android system Back gesture/button.

#### Q: Does ZorApp drain the vehicle battery?
**A:** No. The neural network only runs for ~25 milliseconds when an image is first loaded, and results are cached in memory. When viewing or idling, CPU usage is virtually zero.
