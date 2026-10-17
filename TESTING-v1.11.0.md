# TicTic QA record — v1.11.0

**Build date:** 2026-10-16 (workspace local date)  
**Disposition:** Debug candidate passes build, JVM and source-contract checks; **not release-ready** because Android runtime/device QA was not completed.

## Build and automated checks

Environment: Gradle wrapper 7.6.4, JDK 11, Android SDK Platform 34, Build Tools 34.0.0; minSdk 23, targetSdk 34. The SDK was provisioned in this workspace for this build and is not included in the source import archive.

```sh
bash gradlew --no-daemon --max-workers=2 \
  -Dorg.gradle.jvmargs='-Xmx768m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8' \
  clean testDebugUnitTest assembleDebug lintDebug
python3 -m unittest discover -s qa -v
```

- **Gradle:** `clean testDebugUnitTest assembleDebug lintDebug` — **BUILD SUCCESSFUL**.
- **JUnit:** `TimeMathTest` — **7 passed / 0 failed / 0 errors / 0 skipped**. Pure time-math tests only; not Android services/UI.
- **Python source-contract suite:** **53/53 passed**. These check static source structure/wiring, not Android runtime behavior.
- **Lint:** **0 errors, 240 warnings**. Warnings remain and are not represented as fixed. Reports: `app/build/reports/lint-results-debug.html` and `app/build/intermediates/lint_intermediate_text_report/debug/lint-results-debug.txt`.
- **APK metadata:** package `app.tictic.clock`, versionName `1.11.0`, versionCode `21`, min API 23, target/compile API 34.
- **Artifact checks:** `zipalign -c -v 4` passed; `apksigner verify --verbose --print-certs` passed v1/v2 debug signature verification. APK size and checksum are in `ARTIFACTS-v1.11.0.md`. No runtime test libraries are included in the APK.

## Runtime/device validation — NOT RUN

No v1.11.0 UI—including the reorganized ringing-controls card—notification, volume-key, glass contrast/live slider response and press/scroll elasticity, CSV export, timer/alarm timing or audio workflow was exercised on a physical device or booted emulator. `adb devices -l` returned no attached device. The earlier API 34 emulator attempt in this workspace failed before boot because the host had about 1 GB free memory while the emulator requested about 2.5 GB, and hardware acceleration was unavailable. This is an environment limit, not a pass/fail result for TicTic.

All cases in [`qa/QA-v1.11.0-manual.md`](qa/QA-v1.11.0-manual.md) remain **NOT RUN**, as do inherited alarm/reminder/timer/widget regressions. In particular, verify the dedicated Snooze/Stop settings card, screen/notification actions, independent hardware-key mapping, repeating-schedule safety, CSV file creation/quoting/privacy, range filtering, and visible glass changes from subtle to strong with legibility across app pages in both themes, press deformation and edge-stretch feel on touch.

## Release and repository status

- APK is Gradle debug-signed for local sideload/testing only, not Play/store signed.
- Do not label this final until the manual QA matrix is run on representative devices and the lint warnings are reviewed/triaged.
- The public GitHub repository requires owner-authenticated push. No commit or push was made in this session.
- Artifact and import ZIP checksums are listed in [`ARTIFACTS-v1.11.0.md`](ARTIFACTS-v1.11.0.md).