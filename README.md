# Recordly

Recordly is a clean, lightweight Material 3 screen recorder for Android. It was built with privacy and performance in mind, giving you full control over your recordings without any ads, tracking, or unnecessary bloat.

## Screenshots

Screenshots will be added after device testing.

- Record Screen: `docs/screenshots/record.png`
- Library: `docs/screenshots/library.png`
- Settings: `docs/screenshots/settings.png`
- About: `docs/screenshots/about.png`

## Features

- **High-Performance Capture:** Record your screen at up to 240 FPS (device permitting) for slow-motion editing.
- **Customizable Quality:** Choose your exact resolution (up to 1440p) and bitrate (up to 35 Mbps).
- **Internal Audio:** Capture internal device audio on Android 10+ devices, or use your microphone on older devices.
- **Floating Controls:** A secure, out-of-the-way floating overlay to pause, resume, and stop your recording.
- **Privacy First:** 100% offline. No analytics, no tracking, and no internet required. Your recordings never leave your device unless you share them.

## What works right now (Version 1.0)
- The core screen recording engine is fully functional.
- Settings are persisted and correctly affect the MediaRecorder output.
- The UI features a clean Material 3 design with light, dark, and AMOLED themes.
- Recordings are saved locally in `Movies/Recordly`.

## Android Support
Recordly supports Android 8.0 (API 26) and above. Internal audio capture is available on Android 10 (API 29) and above due to Android OS limitations.

## Permissions Explained
- **Foreground Service:** Required to keep the recording alive while you use other apps.
- **Record Audio:** Required to capture microphone or internal audio.
- **System Alert Window (Overlay):** Required to show the floating control widget.
- **Post Notifications:** Required to show the recording status in your notification drawer.

## Build Instructions

To build the project locally using Gradle:

```bash
# Set your JAVA_HOME to a compatible JDK (JDK 17+)
./gradlew :app:assembleDebug
```

## Install Debug APK

Once built, you can install the debug APK to your connected device or emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Roadmap
- Advanced video trimming in the Library screen.
- Configurable countdown timer visuals.
- Quick settings tile for immediate recording.

## Credits
Vibecoded with love by Giftus Sajeev and Sanjith KS.
Made with ChatGPT 5.5 Extended, Gemini Pro 3.1, and Claude Opus 4.7.

## License
Recordly is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
