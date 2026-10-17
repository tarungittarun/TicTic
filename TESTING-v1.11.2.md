# TicTic QA record — v1.11.2

**Build date:** 2026-10-16 (workspace local date)  
**Disposition:** Debug build and static/automated checks passed; **not release-ready** because Android runtime/device QA has not been completed.

## Build and automated checks

Environment: Gradle wrapper 7.6.4, JDK 17, Android SDK Platform 34, Build Tools 34.0.0; minSdk 23, targetSdk 34. SDK/JDK installations are machine-local and are not included in the source bundle.

Commands run:

```sh
JAVA_HOME=/home/user/.local/jdk17 \
ANDROID_HOME=/home/user/.local/android-sdk \
bash gradlew clean testDebugUnitTest assembleDebug --no-daemon --max-workers=1 \
  -Dorg.gradle.jvmargs=-Xmx1024m
python3 -m unittest discover -s qa -v
JAVA_HOME=/home/user/.local/jdk17 \
ANDROID_HOME=/home/user/.local/android-sdk \
bash gradlew lintDebug --no-daemon --max-workers=1 \
  -Dorg.gradle.jvmargs=-Xmx1024m
```

- Gradle clean compile, `testDebugUnitTest` and `assembleDebug`: **BUILD SUCCESSFUL**.
- JUnit `TimeMathTest`: **7 tests passed / 0 failures / 0 errors**.
- Python source-contract tests: **57/57 passed**, including responsive dialog/editor structure, bounded Settings and Widget Studio scrolling, pinned Studio preview/actions, five glass presets/custom persistence, and five selectable touch-feedback modes. These inspect source wiring; they do not execute Android UI.
- Android lint: **0 errors, 242 warnings**. Warnings remain; report: `app/build/reports/lint-results-debug.html`.
- APK metadata: `app.tictic.clock`, versionName `1.11.2`, versionCode `23`, min API 23, target/compile API 34.
- APK zip alignment: `zipalign -c -v 4` passed.
- `apksigner verify --verbose --print-certs`: debug signature verification passed. It emits the pre-existing non-fatal warning that the Gradle `META-INF/com/android/build/gradle/app-metadata.properties` JAR entry is not protected by the signature.
- APK size/SHA-256: see [`ARTIFACTS-v1.11.2.md`](ARTIFACTS-v1.11.2.md).

## Runtime/device validation — NOT RUN

`adb devices -l` showed no attached physical device. An API 34 x86_64 emulator was attempted, but it exited before boot with **Insufficient RAM free for launching emulator** (the image requested 2.5 GB while the workspace had about 1 GB available); `/dev/kvm` is also absent. No v1.11.2 screen or interaction was exercised on a physical phone or a booted emulator. All rows in [`qa/QA-v1.11.2-manual.md`](qa/QA-v1.11.2-manual.md) are **NOT RUN**. In particular, compact/full-screen window insets, keyboard resizing, actual scroll clipping, alarm create/edit and defaults persistence, Widget Studio placement/reconfiguration, text contrast, preset rendering, and touch motion under system reduce-motion settings remain unverified at runtime.

## Release limits

- APK is locally debug-signed for sideload testing, not Play/store distribution.
- Do not label it release-ready until the focused and inherited manual QA matrix is executed on representative devices and the 242 lint warnings are reviewed/triaged.
- No commit or GitHub push was made in this session.
- The APK and workspace file viewer/download path have not been confirmed usable by the user; do not infer successful delivery from the artifact being present here.
