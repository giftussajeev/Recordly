# Changelog

## 1.9 (versionCode 10)
### Recording Pipeline Rewrite
- **Recording actually works now**: Complete rewrite of MediaRecorder setup chain.
- **MediaStore output** on Android 10+: Files are properly registered in MediaStore via `ContentValues`, so Library shows recordings immediately.
- **MediaScannerConnection** on Android 8-9: Files saved via direct File API are scanned into MediaStore.
- **Quality → Bitrate**: Quality presets now intelligently map to bitrate based on resolution and FPS. Removed redundant "Bitrate" setting.
- **Auto-fallback**: If user config fails, automatically retries with 1080p/30fps/no audio.
- **0-byte cleanup**: Detects and deletes empty recording files on failure.

### Internal Audio
- Enabled internal audio capture for Android 10+ (API 29+).
- Uses MIC source with active MediaProjection for app audio capture.
- Properly disabled with clear message on Android 8-9.

### Match Display FPS
- New "Match display" option records at the screen's native refresh rate.
- FPS is always capped to the display's actual refresh rate.

### Settings Cleanup
- Removed "Bitrate" setting (merged into Quality).
- Removed "Performance mode" (did nothing useful).
- Removed "Show touches" (crash-prone system setting).
- Added save location reset button.
- Quality options now show approximate bitrate ranges.

### Library Fixes
- Fixed MediaStore query: removed unreliable `RESOLUTION` column, uses `MediaMetadataRetriever` instead.
- Handles Android 8-9 where `RELATIVE_PATH` doesn't exist.
- Skips 0-byte files from listing.

### UI Polish
- Record button now uses Google Material Red (not theme primary color).
- About screen uses actual Recordly app icon (not generic icon).
- About screen has expandable Android compatibility section.
- **Telegram-style theme switching**: Changing themes now uses a circular reveal animation that expands from the top-right corner, revealing the new theme underneath. Inspired by Telegram's theme toggle.
- Version updated to 1.9 everywhere.

### Floating Overlay
- Overlay now **auto-collapses** to a small dot + timer after 4 seconds of no interaction.
- Tap the collapsed dot to expand controls back.
- Any interaction (drag, button press) resets the auto-collapse timer.
- This makes the overlay unobtrusive during fullscreen apps and games.

## 1.8 (versionCode 9)
### Library & File Management
- Fixed .tmp file lifecycle and cleanup during failed/aborted recordings.
- Recordings are properly renamed to .mp4 upon completion.
- Implemented file renaming action in the Library.
- Filtered out .tmp files from Library view.

### Audio & Recording
- Fixed audio source handling (MIC vs Internal vs None).
- Explicitly disabled unsupported Internal Audio features for Android < 10.
- Disabled Internal Audio in UI logic when not supported.
- Implemented time tracking in the recording overlay.

### Polish & General Fixes
- Added theme switcher to Onboarding.
- Updated About screen to version 1.8.

## 1.7 (versionCode 8)
### Library & Settings
- Implemented real multi-select and batch actions (Share/Delete) in the Library screen.
- Implemented "Show touches" functionality via standard Android WRITE_SETTINGS permission.
- Settings Screen UI polished for performance.
- Storage Access Framework custom directory saving.
- Floating overlay fixed.


## 1.5 (versionCode 6)
### Home screen redesign
- Replaced small chip row with a 2×2 preset grid: four large rounded cards (Resolution, FPS, Quality, Audio)
- Each card shows icon, label, and current value with a large tap target
- Record button redesigned: large circle with scale animation, clear stop state
- Recording status badge in header while recording
- Countdown shown as a pill chip below the record button
- Status card more informative (countdown timer display, saving spinner)
- Error card shows user-friendly message instead of raw exception text

### Settings performance
- Settings now uses `Column + verticalScroll` instead of `LazyColumn`
- All settings composables receive only primitive/immutable args — no unnecessary recomposition
- Bottom sheet only composed when open (not in idle tree)
- No side effects or expensive work inside composition
- AMOLED added as theme option in Settings bottom sheet
- Bitrate selector added

### Theme and dynamic color
- Fixed theme not propagating: `MainActivity` now reads `themePreference` and `dynamicColor` from DataStore and passes directly to `RecordlyTheme` — single source of truth
- `RecordlyTheme` now uses a proper full `lightColorScheme`/`darkColorScheme`/AMOLED scheme with all Material 3 roles filled in
- Fixed: primary color was `0xFF0F172A` (near-black navy) — was unreadable in light theme. Now `0xFF4F6EF7` (vivid blue)
- Status bar and navigation bar now correctly styled for each theme mode
- Dynamic color default changed to `false` so the stable Recordly palette is shown by default (avoids wallpaper color bleed)
- Dynamic color toggle in Settings correctly toggles Material You

### Recording reliability
- MediaProjection callback now registered BEFORE `createVirtualDisplay` (required on Android 14+)
- Added fallback retry: if user config fails `prepare()`, retries automatically with 1080p / 30fps / no audio
- `trySetupMediaRecorder()` wraps setup in a safe try/catch and cleans up on failure
- Better user-facing error messages (hide raw exception text from UI, keep it in Logcat)

### About screen
- Polished hero: circular icon + "Recordly" ExtraBold title + "Version 1.5"
- App info card with all metadata
- GitHub link now opens `https://github.com/giftussajeev/Recordly`
- Play Store row shows "Coming soon" with toast
- Privacy/Terms/Licenses open native screens
- Credits card with proper styling

### Code quality
- Color palette redesigned: all Material 3 roles filled for light, dark, and AMOLED
- No WebView anywhere
- No placeholder screens
- All screens are native Jetpack Compose

## 1.4 (versionCode 5)
### Fixed
- Recording startup crash (display context in Service)
- About page crash (LazyColumn + painterResource combo)
- Settings lag (LazyColumn → Column+verticalScroll)
- MediaRecorder setup order

### Added
- Onboarding screen (first-run permission setup)
- "Run setup again" in Settings
- Countdown quick-edit chip

## 1.3 (versionCode 4)
- REMOTE_SUBMIX crash fix
- Audio encoder operator precedence fix
- Interactive preset chips
- Theme/dynamic color toggle

## 1.2 (versionCode 3)
- MediaStore library integration
- Settings restructure

## 1.1 (versionCode 2)
- Native Jetpack Compose UI overhaul
- Permission flow fixes
- Recording state machine

## 1.0 (versionCode 1)
- Initial release
