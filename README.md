# Recordly

A clean, lightweight screen recorder for Android. Built with Kotlin, Jetpack Compose, and Material 3.

No ads. No analytics. No internet required. Everything stays on your device.

**[⬇ Download latest APK (debug/testing)](https://github.com/giftussajeev/Recordly/releases/latest)**

> This is a debug testing APK, not a Play Store release. Install at your own risk. See [Releases](https://github.com/giftussajeev/Recordly/releases) for all versions.

---

## Screenshots

| Record | Library | Settings | About |
|--------|---------|----------|-------|
| ![Record](docs/screenshots/record-screen.jpg) | ![Library](docs/screenshots/library-screen.jpg) | ![Settings](docs/screenshots/settings-screen.jpg) | ![About](docs/screenshots/about-screen.jpg) |

---

## What it does

- Records your screen as MP4
- Configurable resolution (720p / 1080p / 1440p / Native), FPS, quality, and bitrate
- Microphone audio recording (optional)
- Floating overlay controls to stop/pause without switching apps
- Saves to `Movies/Recordly`
- Built-in library to browse, play, share, and delete recordings
- Theme switching: System / Light / Dark / AMOLED
- Material You dynamic color (Android 12+)

## What works in v1.4

- Screen recording with configurable settings
- First-run welcome/onboarding screen with permission setup
- Settings persist across sessions (DataStore)
- Theme switching works (including AMOLED)
- Dynamic color (Material You) toggle on Android 12+
- Library shows real recordings with search
- Quick-edit chips on home screen
- Smooth Settings screen (no more lag)
- About page stable (no longer crashes)

## What doesn't work yet

- Internal audio capture (Android OS limitation — requires `AudioPlaybackCapture` API — coming in a future version)
- Custom resolution input
- Video trimming / editing
- Save location picker
- Quick settings tile

## Android support

- **Minimum:** Android 8.0 (API 26)
- **Target:** Android 15 (API 35)
- **Internal audio:** Requires Android 10+ (not yet implemented)
- **Dynamic color:** Requires Android 12+

## Permissions

| Permission | Why |
|------------|-----|
| Screen Capture | Records your screen (asked each time) |
| Microphone | Records audio (optional) |
| Display over apps | Floating stop button while recording |
| Notifications | Recording status notification |
| Storage | Saves and reads recordings |

Nothing is uploaded or shared. All data stays on your device.

## Build from source

Requires JDK 17+. Use Android Studio's bundled JDK if needed.

```bash
# Windows
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
.\gradlew.bat :app:assembleDebug

# macOS/Linux
export JAVA_HOME=/path/to/android-studio/jbr
./gradlew :app:assembleDebug
```

Install to a connected device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Credits

Vibecoded with love by Giftus Sajeev and Sanjith KS.

Made with GPT-5.5 Extended, Gemini 3.1 Pro High, Claude Opus 4.7, and Google Stitch v3.

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.
