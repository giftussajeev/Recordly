# Recordly 1.7

A beautiful, lightweight, privacy-first screen recorder for Android. Built natively with Kotlin and Jetpack Compose.

No ads. No analytics. No internet needed. Everything stays on your device.

**[⬇ Download latest debug APK (v1.7)](https://github.com/giftussajeev/Recordly/releases/latest)**

> This is a debug build for testing. Not a Play Store release. Enable "Install unknown apps" in Android settings to sideload.

---

## What's in v1.5

**New home screen** — four large rounded preset cards for Resolution, FPS, Quality, and Audio. Tap any card to open a clean selector sheet.

**Theme actually works now** — Light, Dark, AMOLED, and System all apply immediately when changed in Settings. No restart needed. AMOLED uses true black for OLED screens.

**Settings no longer lags** — rewrote the settings screen to be properly performant. Composables no longer re-render everything on every preference change.

**Recording more reliable** — MediaProjection callback now registered before `createVirtualDisplay` (was missing, causing silent failures on Android 14+). Auto-fallback to 1080p/30fps if your chosen config fails.

**About page polished** — proper hero with icon, real GitHub link, clean credits.

---

## Supports

| Android version | Supported |
|-----------------|-----------|
| Android 8.0 (API 26) | ✅ |
| Android 9.0 (API 28) | ✅ |
| Android 10+ (API 29) | ✅ |
| Android 12+ Material You | ✅ |
| Android 14+ (API 34) | ✅ |
| Android 15/16 (API 35) | ✅ |

Internal audio capture (Android 10+) — coming in a future version.

---

## Features

- Screen recording to MP4
- Resolution: 720p / 1080p / 1440p / Native
- FPS: 30 / 60 / 90 / 120 (capped to display refresh rate)
- Quality: Low / Balanced / High / Max
- Audio: None or Microphone
- Floating overlay stop button while recording
- Library with search — browse, share, delete recordings
- Theme: System / Light / Dark / AMOLED
- Material You (Android 12+)
- Recordings saved to `Movies/Recordly`

---

## Build from source

```bash
# Windows
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
.\gradlew.bat :app:assembleDebug

# macOS/Linux
./gradlew :app:assembleDebug
```

Install to a connected device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Credits

Vibecoded with love by Giftus Sajeev and Sanjith KS.

Made with GPT-5.5 Extended, Gemini 3.1 Pro High, Claude Opus 4.7, and Google Stitch v3.

---

## License

Apache License 2.0. See [LICENSE](LICENSE).
