# Recordly 1.9

A beautiful, lightweight, privacy-first screen recorder for Android. Built natively with Kotlin and Jetpack Compose.

No ads. No analytics. No internet needed. Everything stays on your device.

**[⬇ Download latest APK (v1.9)](https://github.com/giftussajeev/Recordly/releases/latest)**

> Enable "Install unknown apps" in Android settings to sideload.

---

## What's in v1.9

**Recording actually works now** — rewrote the entire recording pipeline. Files are now properly saved via MediaStore on Android 10+ (no more lost recordings). Auto-fallback to safe defaults if your chosen config fails.

**Internal audio** — enabled for Android 10+. Uses MIC source with active MediaProjection for app audio capture. Properly disabled with a clear message on Android 8-9.

**Quality replaces Bitrate** — removed the redundant Bitrate setting. Quality presets (Low/Balanced/High/Max) now intelligently map to the right bitrate based on your resolution and FPS.

**Match display FPS** — new option that records at your screen's native refresh rate (60Hz, 90Hz, 120Hz, etc.).

**Library works** — fixed MediaStore queries that were failing silently. Recordings now show up immediately after saving.

**Record button is red** — uses Google Material Red, not the theme primary color. Consistent with what users expect from a record button.

**Cleaned up Settings** — removed fake/broken features (Performance Mode, Show Touches). Every setting that exists actually works.

**About screen polished** — shows actual Recordly app icon, expandable Android compatibility section, centered credits.

**Telegram-style theme switching** — changing themes now plays a circular reveal animation that expands from the top-right corner, revealing the new theme underneath. Like Telegram.

**Floating overlay auto-collapse** — the recording overlay collapses to a small dot + timer after 4 seconds of no interaction. Tap to expand. Unobtrusive in games and fullscreen apps.

---

## Supports

| Android version | Supported |
|-----------------|-----------|
| Android 8.0 (API 26) | ✅ |
| Android 9.0 (API 28) | ✅ |
| Android 10+ (API 29) | ✅ Internal audio |
| Android 12+ Material You | ✅ Dynamic colors |
| Android 14+ (API 34) | ✅ |
| Android 15/16 (API 35) | ✅ |

---

## Features

- Screen recording to MP4
- Resolution: 720p / 1080p / 1440p / Native
- FPS: Match display / 30 / 60 / 90 / 120 (capped to display refresh rate)
- Quality: Low / Balanced / High / Max (auto-maps to bitrate)
- Audio: None / Microphone / Internal (Android 10+)
- Floating overlay with stop/pause/timer while recording
- Library with search, multi-select, share, rename, delete
- Theme: System / Light / Dark / AMOLED
- Material You dynamic colors (Android 12+)
- Custom save location via SAF
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
