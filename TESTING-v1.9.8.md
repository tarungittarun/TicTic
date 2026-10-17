# TicTic QA record — v1.9.8

**Build date:** 2026-10-15 (workspace local date)

## Build and static checks

- Package `app.tictic.clock`; versionName `1.9.8`, versionCode `18`; min API 23; target/compile API 34.
- Clean debug build command: `bash gradlew --no-daemon --max-workers=2 -Dorg.gradle.jvmargs='-Xmx768m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8' clean testDebugUnitTest assembleDebug` — **BUILD SUCCESSFUL**.
- Python source-contract checks: `python3 -m unittest discover -s qa -v` — **36/36 PASS**. These assert static wiring only.
- Gradle `testDebugUnitTest`: **NO-SOURCE**; no Android/JUnit unit tests are defined in the app module.
- APK package metadata, alignment, signatures and SHA-256 are in [`ARTIFACTS-v1.9.8.md`](ARTIFACTS-v1.9.8.md).
- Lint: **NOT RUN** for v1.9.8; no lint pass is claimed.

## Runtime/device testing

- No emulator or physical handset is attached. No v1.9.8 install, screenshot/layout review, popup touch test, accessibility test, audio test, alarm delivery test, volume-key test, or launcher test was performed.
- All cases in [`qa/QA-v1.9.8-manual.md`](qa/QA-v1.9.8-manual.md) are **NOT RUN**. The previous 58-case base regression workbook and v1.9.7 focused checklist also remain outstanding.
- Google Clock comparison remains documentation-based; no same-device hands-on comparison was performed.
- Android owns the hardware Power key. The app configures a shared action for Volume Up/Down only while the alarm screen is foreground; OEM/OS behavior can vary. Do not describe this as Power-key support.
- Android/OEM ringtone picker screens are OS-owned and cannot receive TicTic's custom close affordance. The requirement applies to app-owned windows; platform picker surfaces retain their platform controls.

## Environment notes / recovery

- The workspace's temporary Android SDK under `/tmp` was absent at the start of this build attempt, so Gradle initially reported SDK location unavailable. Android SDK Platform 34 and Build Tools 34.0.0 were reinstalled, then the final clean build passed.
- The final Gradle build used a 768 MB heap and two workers. In a previous workspace build, the default 2 GB Gradle daemon disappeared; reduced heap/workers are a reproducible workaround in this constrained environment.

## Repository/push status

- Repository URL supplied: `https://github.com/tarungittarun/TicTic.git`. Anonymous inspection found a public empty repository with no branches or commits.
- GitHub connector is unsupported in this session; no authorized push credential is present. No commit or push was made. See [`ENGINEERING-LOG-v1.9.8.md`](ENGINEERING-LOG-v1.9.8.md) for inspection detail and the future commit ledger format.

## QA interpretation

Build/package success and static source tests do not establish that dialogs are visually rounded on every API level, that outside touches close them on every OEM, or that widgets and hardware keys behave consistently. Execute the manual checklist on representative API/OEM devices before calling these behaviors verified.
