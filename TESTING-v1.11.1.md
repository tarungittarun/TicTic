# TicTic QA record — v1.11.1

**Build date:** 2026-10-16 (workspace local date)  
**Disposition:** Debug build, automated checks and APK integrity checks passed; **not release-ready** because Android runtime/device QA has not been completed.

## Build and automated checks

Environment: Gradle wrapper 7.6.4, JDK 17, Android SDK Platform 34, Build Tools 34.0.0; minSdk 23, targetSdk 34. The SDK and JDK are machine-local and are not included in the source bundle.

```sh
bash gradlew --no-daemon --max-workers=2 \
  -Dorg.gradle.jvmargs='-Xmx768m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8' \
  clean testDebugUnitTest assembleDebug lintDebug
python3 -m unittest discover -s qa -v
```

- Gradle `clean testDebugUnitTest assembleDebug lintDebug`: **BUILD SUCCESSFUL**.
- JUnit `TimeMathTest`: **7 passed / 0 failed / 0 errors / 0 skipped**.
- Python source-contract suite: **54/54 passed**, including full-screen opaque alarm-editor and scroll-clipping regression checks. These inspect source wiring; they do not execute Android UI.
- Lint: **0 errors, 240 warnings**; warnings have not been represented as fixed. Report: `app/build/reports/lint-results-debug.html`.
- APK metadata: `app.tictic.clock`, versionName `1.11.1`, versionCode `22`, min API 23, target/compile API 34.
- Artifact verification: `zipalign -c -v 4` passed; `apksigner verify --verbose --print-certs` verified v1/v2 debug signatures. The signer reports the existing non-fatal warning about the Gradle app-metadata JAR entry not being protected by the signature. Size/checksum are in `ARTIFACTS-v1.11.1.md`.

## Runtime/device validation — NOT RUN

No v1.11.1 screen or interaction has been exercised on a physical phone or booted emulator. In particular, full-screen alarm editor insets, actual scroll clipping, soft-keyboard resizing, alarm create/edit/date/repeat behavior, Save/Delete/Cancel actions, and Light/Dark/Glass contrast remain **unverified at runtime**. This workspace has no attached Android device. All cases in [`qa/QA-v1.11.1-manual.md`](qa/QA-v1.11.1-manual.md) are **NOT RUN**, as are inherited alarm/reminder/timer/widget regressions.

## Research and rationale

The editor prioritizes time and repeat/date before secondary settings, following the task sequence documented in Android Clock help. Material 3 dialog guidance supports full-screen treatment for multi-step compact-screen tasks and advises that scrollable content not obscure the heading/actions. Sources: [Google Clock Help](https://support.google.com/android/answer/2840926?hl=en), [Material 3 dialogs](https://m3.material.io/components/dialogs/guidelines). This was a design reference, not hands-on parity testing.

## Release limits

- APK is debug-signed for local sideload/testing only, not Play/store distribution.
- Do not label it release-ready until the focused and inherited manual QA matrix is executed on representative devices and lint warnings are reviewed/triaged.
- No commit or GitHub push was made in this session.
- Artifact checksums and bundle details: [`ARTIFACTS-v1.11.1.md`](ARTIFACTS-v1.11.1.md).
