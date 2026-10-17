# TicTic QA record — v1.10.0

**Build date:** 2026-10-15 (workspace local date)  
**Disposition:** Debug candidate verified by build/static checks; **not release-ready** because Android runtime/device QA has not been completed.

## Build and automated checks

Environment: Gradle wrapper 7.6.4, JDK 11, Android SDK Platform 34, Build Tools 34.0.0; `minSdk 23`, `targetSdk 34`.

```sh
bash gradlew --no-daemon --max-workers=2 \
  -Dorg.gradle.jvmargs='-Xmx768m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8' \
  clean testDebugUnitTest assembleDebug lintDebug
python3 -m unittest discover -s qa -v
```

- **Gradle:** `clean testDebugUnitTest assembleDebug lintDebug` — **BUILD SUCCESSFUL**.
- **JUnit:** 7 `TimeMathTest` tests, **7 passed / 0 failed / 0 errors / 0 skipped**. Includes timer hundredths formatting; these are pure time-math tests, not Android UI/service tests.
- **Python source-contract suite:** **48/48 passed**. These check source wiring and static structure; they are not runtime behavior tests.
- **Lint:** **0 errors, 241 warnings**. Warnings remain and have not been represented as fixed. Full reports: `app/build/reports/lint-results-debug.html` and `app/build/intermediates/lint_intermediate_text_report/debug/lint-results-debug.txt`.
- **APK metadata:** package `app.tictic.clock`, versionName `1.10.0`, versionCode `20`, min API 23, target/compile API 34.
- **Artifact checks:** `zipalign -c -v 4` passed; `apksigner verify --verbose --print-certs` passed v1/v2; APK contains no JUnit/Hamcrest test JARs. `apksigner` emitted the standard warning that Gradle's `META-INF/com/android/build/gradle/app-metadata.properties` entry is not covered by the signature; signature verification itself succeeded.

## Runtime/device validation — NOT RUN

No TicTic v1.10.0 workflow was exercised on a booted emulator or physical handset. In the earlier emulator attempt in this workspace, the API 34 x86_64 emulator stopped before boot because it requested 2.5 GB of RAM while about 1 GB was available, and hardware acceleration was unavailable. No `adb` device was available. This is a test-environment limitation, not evidence that app behavior passes or fails.

All cases in [`qa/QA-v1.10.0-manual.md`](qa/QA-v1.10.0-manual.md) remain **NOT RUN**, including volume-key migration/actions, the new glass sliders and contrast, visible card elevations, settings scroll retention, optional-name sequence generation, haptics, and timer/stopwatch hundredths. Re-run prior alarm/reminder/audio/notification/widget regression cases as well. Do not claim an accuracy percentage or same-device Google Clock parity.

## Release and repository status

- The APK is Gradle debug-signed for local sideload/testing only, not Play/store signed.
- Do not label it final until the manual matrix is run on representative devices, including the target handset, and lint warnings are reviewed/triaged.
- Public GitHub repo `https://github.com/tarungittarun/TicTic.git` still requires owner-authenticated push. No commit or push was made from this session.
- Artifact size, checksum and signatures are recorded in [`ARTIFACTS-v1.10.0.md`](ARTIFACTS-v1.10.0.md).