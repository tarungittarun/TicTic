# TicTic QA record — v1.9.9

**Build date:** 2026-10-15 (workspace local date)  
**Disposition:** Debug candidate verified by build/static checks; **not release-ready** because Android runtime/device QA has not been completed.

## Build and automated checks

Environment: Gradle wrapper 7.6.4, JDK 11, Android SDK Platform 34, Build Tools 34.0.0; `minSdk 23`, `targetSdk 34`.

```sh
bash gradlew --no-daemon --max-workers=2 \
  -Dorg.gradle.jvmargs='-Xmx768m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8' \
  clean testDebugUnitTest assembleDebug lintDebug
```

- **Gradle build:** `BUILD SUCCESSFUL`; clean compilation, unit tests, debug assembly and lint ran in the same build.
- **JUnit:** 6 `TimeMathTest` cases, **6 passed / 0 failed / 0 errors / 0 skipped**. These exercise pure timing math, not Android UI, alarm delivery, or device services.
- **Python source-contract suite:** `python3 -m unittest discover -s qa -v` — **43/43 passed**. These checks verify source wiring/structure, not runtime behavior.
- **Lint:** **0 errors, 239 warnings**. The earlier API-24 notification-status errors were guarded for `minSdk 23`; private `NumberPicker` field reflection was removed in favor of theme-aware picker contexts. Remaining warnings are non-fatal and are not represented as resolved.
- **APK verification:** `aapt` confirms package `app.tictic.clock`, versionName `1.9.9`, versionCode `19`, min API 23 and target/compile API 34. `zipalign -c -v 4` passed. `apksigner verify --verbose` passed v1 and v2 signatures; it also emitted the standard warning that Gradle's `META-INF/com/android/build/gradle/app-metadata.properties` entry is not signed. The APK contains no JUnit/Hamcrest test JARs.
- Full lint HTML: `app/build/reports/lint-results-debug.html`; text summary: `app/build/intermediates/lint_intermediate_text_report/debug/lint-results-debug.txt`.

## Runtime/device validation — NOT RUN

No app workflow was exercised on a booted emulator or physical phone. An Android 14/API 34 x86_64 Google APIs emulator image was installed and an emulator launch was attempted, but the emulator exited before boot: the host did not have sufficient free RAM (emulator requested 2.5 GB; about 1 GB was available), and hardware acceleration was unavailable. `adb` reported no connected emulator/device. This is an environment limitation, not an app test result.

Accordingly, **all** cases in [`qa/QA-v1.9.9-manual.md`](qa/QA-v1.9.9-manual.md) remain **NOT RUN**. No rate or accuracy percentage is claimed. In particular, these are not runtime-verified: alarm create/edit visibility, dial hit-boundary/scroll gestures, preview audio and stopping behavior, volume keys/notifications/full-screen alarm, timer countdown and recovery, stopwatch hundredths/laps, themed NumberPicker contrast, reboot/time-change behavior, and OEM/launcher differences.

The volume-action options are documentation-aligned with Google Clock’s published choices (control volume, snooze, dismiss); **no same-device hands-on Google Clock comparison was performed**. Power-key behavior is Android/OEM-controlled and is not supported by TicTic.

## Release and repository status

- The APK is debug-signed for sideloading/testing. It is **not a production/store-signed release**, and should not be labeled final until the manual matrix is run on representative devices, including the target handset and relevant Android/OEM versions.
- Public repository `https://github.com/tarungittarun/TicTic.git` remains empty. No commit or push was made; no authorized GitHub write mechanism is available in this session.
- Artifact metadata/checksum and signature details are in [`ARTIFACTS-v1.9.9.md`](ARTIFACTS-v1.9.9.md).