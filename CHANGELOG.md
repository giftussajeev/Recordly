# Changelog

## 1.3
- Fixed recording failure caused by invalid audio source handling (REMOTE_SUBMIX crash)
- Fixed content being cut off behind bottom navigation bar (NavGraph padding bug)
- Fixed About page crash
- Fixed Settings dropdowns replaced with modern bottom sheet selectors
- Connected theme switching (System / Light / Dark / AMOLED) to actual app behavior
- Connected dynamic color toggle to Material You on Android 12+
- Added Performance mode setting (replaces "Low-end device mode")
- Library search now filters recordings by filename
- Library refresh re-queries MediaStore properly
- Made preset chips on home screen interactive with quick-edit bottom sheets
- Removed cluttered permission checklist from home screen
- Improved recording state machine with better error handling and logging
- Safe audio source fallback when internal audio is unavailable
- Default audio source changed to "No audio" to prevent first-run crashes
- Version bumped to 1.3 everywhere (Gradle, About, README, docs)
- Rewrote README to be more honest and practical
- Added real screenshots to docs

## 1.1
- Native Material 3 UI overhaul across all screens
- Recording state machine fixes (fixed 2-sec stop bug and Start/Stop toggle behavior)
- Strict permission flow improvements
- Settings screen completely redesigned with functional components
- Library screen redesigned with real MediaStore query and Coil thumbnails
- Legal and About pages made fully native and navigable
- New professional adaptive app icon

## 1.0
- Initial debug-ready version of Recordly
- Material 3 UI
- Screen recording foundation
- Recording settings
- Recordings library
- Floating controls / notification fallback
- About, privacy, license, and Play Store draft docs
