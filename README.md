# Recordly

[![Latest Release](https://img.shields.io/github/v/release/giftussajeev/Recordly?color=brightgreen&label=release&logo=github)](https://github.com/giftussajeev/Recordly/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20%28API%2026%2B%29-orange?logo=android)](https://developer.android.com)
[![License](https://img.shields.io/github/license/giftussajeev/Recordly?color=blue)](LICENSE)

A native, lightweight, and privacy-focused screen recorder for Android. Built with Jetpack Compose, Material 3, and Kotlin. 

Recordly does not contain ads, analytics, or trackers. All screen and audio recordings are processed and saved entirely on your device with no network permissions required.

---

<p align="center">
  <a href="https://github.com/giftussajeev/Recordly/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest%20APK-success?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
</p>

---

## Key Features

* **High-Performance Screen Capture**: Full support for native resolutions up to 1440p (QHD) and dynamic display resolution scaling.
* **Intelligent Quality Presets**: Select from Low, Balanced, High, and Max presets. The video pipeline automatically computes and configures the optimal target bitrate based on the chosen resolution and frame rate.
* **Match Display Refresh Rate**: Toggle a "Match display" frame-rate option (`fps = -1`) to record content smoothly at your screen's native refresh rate (e.g., 90Hz, 120Hz).
* **Flexible Audio Input**: Supports both high-fidelity microphone capture and native internal audio loopback capture (available on Android 10+).
* **System Overlay Controls**: An active floating widget provides quick access to pause, resume, and stop recording without switching away from your current app. To prevent screen clutter, the overlay automatically collapses into a subtle timer dot after 4 seconds of inactivity.
* **Telegram-Style Animated Theme Engine**: A premium UI transition effect that captures your current screen state and applies a circular reveal wipe animation when switching between Light, Dark, System, and true-black AMOLED theme modes.
* **In-App Library Manager**: Clean Jetpack Compose gallery list with instant search, multi-select, batch sharing, direct file renaming, and batch deletion.
* **Flexible Save Configurations**: Saves locally to standard scoped storage (`Movies/Recordly`) by default, with complete Storage Access Framework (SAF) tree picker integration for custom directories.

## Supported Android Versions

| Android Version | API Level | Scoped Storage | Internal Audio | Theme Style |
|:---|:---:|:---:|:---:|:---:|
| **Android 8.0 - 9.0** (Oreo, Pie) | 26 - 28 | Legacy | ❌ | Material 3 (Static) |
| **Android 10 - 11** (Q, R) | 29 - 30 | Scoped | ✅ | Material 3 (Static) |
| **Android 12+** (S, T, U, V) | 31+ | Scoped | ✅ | Material You (Dynamic) |

## Development & Building

The project is structured as a standard single-module Android app and compiles using Java 17.

### Prerequisites
* Android Studio (Koala or later recommended)
* Android SDK (API 35 target)
* JDK 17

### Building from the Command Line
Clone the repository and run the Gradle wrapper:

```bash
# Windows
.\gradlew.bat :app:assembleDebug

# Linux / macOS
chmod +x gradlew
./gradlew :app:assembleDebug
```

After building, the debug package is located under `app/build/outputs/apk/debug/app-debug.apk`.

### Sideloading
Install the APK on an active ADB-authorized device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Contributors

* **Giftus Sajeev**
* **Sanjith KS**

## License

Recordly is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
