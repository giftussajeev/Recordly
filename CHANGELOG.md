# Changelog

## 1.4 (versionCode 5)
### Fixed
- **Critical: Recording startup crash** — removed `display?.refreshRate` and `resources.displayMetrics` calls from Service context (they throw "Context not associated with display" on Android 11+). All display metrics (width, height, density, refresh rate) are now passed from Activity as Intent extras.
- **About page crash** — replaced `LazyColumn` with `Column + verticalScroll`, replaced `Image(painterResource(ic_launcher))` with Icon-based composable to avoid mipmap load crashes on some devices.
- **Settings lag** — replaced `LazyColumn` in Settings with `Column + verticalScroll`. LazyColumn was triggering unnecessary recomposition of all items on state changes.
- **MediaRecorder order** — moved audio encoder setup before video encoder (required by API contract). Ensures even dimensions for H264 encoder.
- **Partial file cleanup** — corrupted/empty output files are now deleted when recording fails to start.

### Added
- **Welcome / onboarding screen** — shown on first launch. Step-by-step permission setup for notifications, microphone, and overlay. Explains screen capture consent. Shows credits tastefully. Stored in DataStore.
- **"Run setup again"** option in Settings to re-open onboarding anytime.
- **Countdown chip** on home screen quick edit.
- **Internal audio helper text** — Android 10+ note in audio selector, Android 8/9 unavailability explained.

### Changed
- Default FPS: 60 → 30 (safer, more compatible)
- Default quality: High → Balanced
- Default countdown: 3s → None (immediate start)
- NavGraph waits for DataStore to load before showing any screen (prevents flash of wrong screen)
- Settings now shows helper text for each section

## 1.3 (versionCode 4)
### Fixed
- `setAudioSource(REMOTE_SUBMIX)` crash — internal audio now falls back to No audio
- Audio encoder operator precedence bug — audio encoder no longer set unconditionally on Android 10+
- NavGraph padding — content no longer obscured by bottom navigation bar
- About page intent crash — all external intents wrapped in try-catch

### Added
- Interactive preset chips on home screen with ModalBottomSheet selectors
- Theme switching (System / Light / Dark / AMOLED) via DataStore
- Dynamic color toggle (Material You, Android 12+)
- Performance mode toggle
- Library search bar

## 1.2 (versionCode 3)
- Initial MediaStore library integration
- Settings restructure

## 1.1 (versionCode 2)
- Native Jetpack Compose UI overhaul
- Permission flow fixes
- Recording state machine

## 1.0 (versionCode 1)
- Initial release
