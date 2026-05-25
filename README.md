# <img src="assets/logo.png" width="48" align="center" alt="Recordly Logo"> Recordly

[![Latest Release](https://img.shields.io/github/v/release/giftussajeev/Recordly?color=brightgreen&label=release&logo=github)](https://github.com/giftussajeev/Recordly/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-orange?logo=android)](https://developer.android.com)
[![License](https://img.shields.io/github/license/giftussajeev/Recordly?color=blue)](LICENSE)

Recordly is a native, lightweight, and privacy-focused screen recorder for Android. Built with Jetpack Compose, Material 3, and Kotlin. 

It does not contain ads, trackers, or analytics. It works fully offline with no internet required.

---

<p align="center">
  <a href="https://github.com/giftussajeev/Recordly/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest%20APK-success?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
</p>

---

## Screenshots

<p align="center">
  <img src="assets/dashboard.png" width="300" alt="Dashboard Screen" />
</p>

## Features

* **Screen Recording**: Record screen up to 1440p (QHD) matching your device's native orientation.
* **Audio Capture**: Record microphone audio or capture internal device audio natively (Android 10+).
* **Smart Bitrate**: Quality presets (Low, Balanced, High, Max) automatically map to the right bitrate based on your resolution and FPS.
* **Match Screen Refresh Rate**: Record up to 120 FPS by locking the frame rate to your device's refresh rate.
* **Auto-Collapse Overlay**: Draggable floating bubble for quick controls that automatically collapses to a small timer dot after 4 seconds of inactivity.
* **Telegram-Style Themes**: Switching themes (Light, Dark, System, AMOLED) triggers a premium circular reveal animation expanding from the top-right corner.
* **Built-in Library**: Manage recordings with search, renaming, multi-select, and batch share/delete.
* **Custom Save Location**: Saves to `Movies/Recordly` by default, or you can pick any custom directory via SAF.

## Compatibility

| OS Version | API | Scoped Storage | Internal Audio | Theme Style |
|:---|:---:|:---:|:---:|:---:|
| **Android 8.0 - 9.0** | 26 - 28 | Legacy | ❌ | Material 3 (Static) |
| **Android 10 - 11** | 29 - 30 | Scoped | ✅ | Material 3 (Static) |
| **Android 12+** | 31+ | Scoped | ✅ | Material You (Dynamic) |

## Building from source

Prerequisites: JDK 17, Android SDK (API 35 target).

```bash
# Windows
.\gradlew.bat :app:assembleDebug

# macOS / Linux
chmod +x gradlew && ./gradlew :app:assembleDebug
```

The compiled APK will be located under `app/build/outputs/apk/debug/app-debug.apk`.

## Credits

* **Giftus Sajeev** (Lead Developer)
* **Sanjith KS** (Developer)
* **AI Collaborators**: GPT-5.5 Extended, Gemini 3.1 Pro High, Claude Opus 4.7, and Google Stitch v3.

## License

Recordly is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
