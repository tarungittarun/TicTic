# TicTic QA record — v1.9.5

## Build and static checks

- Package: `app.tictic.clock`, versionName `1.9.5`, versionCode `15`; min API 23, target/compile API 34.
- `:app:assembleDebug`: **PASS**. Debug APK compiled and packaged.
- APK metadata: aapt reports package/version/SDK values above; zipalign check passed; apksigner verified v1 and v2 signatures. This is a debug-signed APK, not a production release. The benign `META-INF/com/android/build/gradle/app-metadata.properties` signature warning remains.
- Python source-contract checks: **20/20 PASS** (`python3 -m unittest discover -s qa -v`). These checks confirm source wiring; they do not exercise Android views, scheduling, notification behavior, sound or physical gestures.
- Gradle `:app:testDebugUnitTest`: **NO-SOURCE**; the app module has no Android/JUnit unit tests.
- Android manifest processing completed as part of the Gradle build. Gradle emitted a non-fatal warning that Android SDK Platform-Tools was not installed; this did not block compilation/package. All 28 Java source files also parsed without syntax errors.
- Lint: **not run**. No lint pass is claimed.

## Runtime / device verification

- No Android emulator or physical device is attached; `adb` is not available in this environment. No Android UI, exact-alarm delivery, notification permission/channel, reboot, lock-screen, audio/vibration, touch/drag, hardware-key or accessibility tests were executed.
- No same-device Google Clock workflow comparison was run. The existing Google Clock notes are documentation-based, not hands-on results.
- Consequently, no device behavior or fix is described as runtime-verified. Before release, execute the `NOT RUN` cases in [`QA-Clock-Comparison-v1.9.5.xlsx`](QA-Clock-Comparison-v1.9.5.xlsx), especially alarm-dial feel, repeat/date scheduling, Reminder actions and boot restoration, notification/exact-alarm denial, per-alarm defaults/overrides, both-theme contrast and one-tap alarm controls.

## Relevant new test coverage

The 49-case workbook retains the earlier Clock/Alarm/Timer/Stopwatch cases and adds scenarios for:

- Circular hour/minute dial input, AM/PM and 12/24-hour selection.
- Working weekday Repeat and one-time date selection.
- Collapsed More settings, default seeding and per-alarm overrides.
- One-time/weekly reminders, notification actions, permission/channel states and reboot/time-change rescheduling.
- App and Widget Studio text/control contrast in light and dark modes.

All functional workbook scenarios remain **NOT RUN** until exercised on a suitable emulator or handset.
