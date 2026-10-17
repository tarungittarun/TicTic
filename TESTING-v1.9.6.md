# TicTic QA record — v1.9.6

## Build and static checks

- Package: `app.tictic.clock`; versionName `1.9.6`, versionCode `16`; min API 23, target/compile API 34.
- `bash gradlew assembleDebug`: **PASS**. The Android SDK 34 debug APK compiled and packaged.
- APK metadata: `aapt dump badging` reports the expected package/version/SDK values. `zipalign -c -v 4` passed. `apksigner verify --verbose` passed v1 and v2 signature verification. The benign `META-INF/com/android/build/gradle/app-metadata.properties` signature warning remains.
- Python source-contract checks: **25/25 PASS** (`python3 -m unittest discover -s qa -v`). These check static source wiring; they do not exercise Android views, alarm scheduling/delivery, notifications, sound, or gestures.
- Gradle `testDebugUnitTest`: **NO-SOURCE**; the app module has no Android/JUnit unit tests.
- `lintDebug` was attempted but analysis did not start: Gradle failed dependency resolution for Groovy and Apache HttpClient from Maven Central with a TLS handshake failure. **No lint pass is claimed.**

## Runtime / device verification

- `adb devices -l` returned no attached emulator or handset. No app was installed or run in an Android UI during this build.
- In particular, alarm creation/editing, the newly attached alarm-dialog content, reminder delivery/actions, weekly-dismiss scheduling, permission screens, settings navigation, visual contrast, sound/vibration, reboot handling, touch behavior, and hardware keys remain **NOT RUN on device**.
- No same-device Google Clock workflow comparison was performed. The comparison notes remain documentation-based, not hands-on results.
- The QA workbook contains **58 functional scenarios**, including the v1.9.6 regressions; all remain NOT RUN until exercised on a suitable emulator or handset. Do not interpret a successful build or source check as functional/device QA.

## v1.9.6 fixes and source-level checks

- The alarm editor's custom `shell` was built but was never attached to its `Dialog`; this explains the reported dimmed screen with no visible editor. The view is now attached via `dlg.setContentView(shell)` and laid out to fill the dialog window. Source-contract coverage asserts that attachment and the weighted scroll form.
- Reminder navigation order, the single Add button, card-to-editor click, Dismiss action, repeat skip scheduling, and left-side Settings group routing have source-contract coverage.
- Reminder create/edit/delete/shown/snooze/dismiss events and relevant reminder/alarm preference changes are routed through local Usage logging where activity history is enabled.
- Those checks establish that the code paths are wired, not that Android displays or delivers them correctly. The corresponding workbook cases must still be run on a device.

## Artifacts

See [`ARTIFACTS-v1.9.6.md`](ARTIFACTS-v1.9.6.md) for APK/source paths, checksums, signing details, and the QA workbook reference. The APK is debug-signed for testing/sideloading and is not a production release.
