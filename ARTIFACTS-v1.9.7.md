# TicTic build artifacts — v1.9.7

## Current debug APK

- File: `/home/user/TicTic-v1.9.7-debug.apk`
- Package: `app.tictic.clock`
- Version: `1.9.7` (versionCode `17`); min API 23; target/compile API 34
- Size: **689,406 bytes**
- SHA-256: `10b54a1ba48d6c4fdbb5032e44bc6076d44c3348dc840f9fbb9cba7bec833a6f`
- Signing: Android Gradle debug key; for sideload/testing, not a production/store-signed release.
- Verification: clean `testDebugUnitTest assembleDebug` build passed (unit-test task is NO-SOURCE); AAPT reports the expected package/version/SDK metadata; `zipalign -c -p 4` passed; `apksigner verify --verbose --print-certs` verified APK v1 and v2 signatures. The standard `META-INF/com/android/build/gradle/app-metadata.properties` warning remains.

## Source and test artifacts

- Versioned source archive: `/home/user/TicTic-Android-Source-v1.9.7.zip`
- SHA-256 for both deliverables: `/home/user/SHA256SUMS-v1.9.7.txt`
- Testing record: [`TESTING-v1.9.7.md`](TESTING-v1.9.7.md)
- Focused manual regression plan: [`qa/QA-v1.9.7-manual.md`](qa/QA-v1.9.7-manual.md)
- Source-contract tests: [`qa/test_source_contracts.py`](qa/test_source_contracts.py)

## QA status and limits

- `python3 -m unittest discover -s qa -v`: **32/32 PASS** (static source-contract checks only).
- `bash gradlew clean testDebugUnitTest assembleDebug`: **BUILD SUCCESSFUL**; Android unit tests are **NO-SOURCE**.
- `zipalign` and APK v1/v2 signature verification: **PASS**.
- Lint: **NOT RUN** for this version.
- Runtime/device QA: **NOT RUN**. `adb devices -l` showed no attached emulator or handset. Alarm/reminder timing, AM/PM touch interaction, tone-duration playback/stopping, settings UI, visual contrast, widget preview/resize, notification permissions/channels, reboot flows and haptics remain to be exercised on Android.
- The existing 58 functional regression scenarios remain **NOT RUN**; the new v1.9.7 focused steps are also marked NOT RUN until device/emulator execution.
- No GitHub push was made; no repository URL or access was supplied.

## Scope note

All 24 registered widget providers declare horizontal/vertical resize modes and Android 12+ 150×84 dp minimum-resize hints. Launcher/OEM behavior and minimum dimensions still apply; the UI scales clock text and hides secondary content at compact sizes. The existing 20-item widget curation remains; the earlier request for 15 genuinely new distinct widget concepts is still outstanding.
