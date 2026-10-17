# TicTic QA record — v1.9.7

## Build and static checks

- Package: `app.tictic.clock`; versionName `1.9.7`, versionCode `17`; min API 23, target/compile API 34.
- Clean Android debug build: **PASS** using `bash gradlew --no-daemon --max-workers=2 -Dorg.gradle.jvmargs='-Xmx768m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8' clean testDebugUnitTest assembleDebug`. Gradle reports `testDebugUnitTest: NO-SOURCE`; there are no Android/JUnit unit tests in the app module.
- Python source-contract suite: **32/32 PASS** (`python3 -m unittest discover -s qa -v`). Checks source wiring and provider metadata, not Android runtime behavior.
- APK metadata, alignment, debug signature, and SHA-256 are recorded in [`ARTIFACTS-v1.9.7.md`](ARTIFACTS-v1.9.7.md).
- Lint: **NOT RUN** for v1.9.7; no lint pass is claimed.

## Runtime / device verification

- No Android emulator or physical handset is attached. No v1.9.7 install, UI walkthrough, notification delivery, audio/vibration audition, gesture test, or launcher resize test was performed.
- The new AM/PM controls and shared dial, reminder sound-duration choices and service cancellation, full-page Settings groups, theme/font/layout variants, timer/routine/stopwatch settings, and Widget Studio spacing/preview are only source-checked and built—not functionally verified on-device.
- Android notification permission, notification-channel user settings, exact/inexact alarm delivery, Do Not Disturb, Android-version policy, audio focus, OEM background limits, and launcher-specific widget sizing all require runtime/device coverage.
- Google Clock comparison remains documentation-based; no same-device hands-on comparison was possible.
- The existing 58-case Clock/Alarm/Timer/Stopwatch/Reminders regression workbook remains outstanding. The focused v1.9.7 cases and statuses are in [`qa/QA-v1.9.7-manual.md`](qa/QA-v1.9.7-manual.md); each is **NOT RUN** pending a suitable device/emulator.

## Scope notes

- All 24 providers advertise `horizontal|vertical` resize modes and Android 12+ 150×84 dp minimum-resize hints; launchers/OEMs may ignore these or enforce larger minimums, so universal free resizing is not guaranteed.
- The 20-item widget catalog is retained, but the prior separate request for 15 genuinely new distinct widget concepts remains outstanding; see `README.md`.
- APK is debug-signed for sideload/testing, not production/store release. No GitHub push was made; no repository URL/access was supplied.
