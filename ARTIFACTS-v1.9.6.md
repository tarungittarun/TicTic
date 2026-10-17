# TicTic v1.9.6 build artifacts

## Current debug APK

- File: `/home/user/TicTic-v1.9.6-debug.apk`
- Package: `app.tictic.clock`
- Version: `1.9.6` (versionCode 16); min API 23; target/compile API 34
- Size: **817,212 bytes** (about 798 KiB)
- SHA-256: `e08996224e89316ef0338de918b968404868322faa069efbb48b0f225b151335`
- Signing: Gradle Android debug key; for testing/sideloading, not a production/store signing key.
- Checks: Gradle `assembleDebug` passed; `aapt` confirmed metadata; `zipalign -c -v 4` passed; `apksigner` verified v1 and v2 signatures. The benign `META-INF/com/android/build/gradle/app-metadata.properties` signature warning remains.

## Source and QA artifacts

- Versioned source archive: `/home/user/TicTic-Android-Source-v1.9.6.zip`
- Source archive byte size and SHA-256: [`../SHA256SUMS-v1.9.6.txt`](../SHA256SUMS-v1.9.6.txt)
- 58-case Clock/Alarm/Timer/Stopwatch/Reminders/Settings/Usage and Google Clock comparison: [`QA-Clock-Comparison-v1.9.6.xlsx`](QA-Clock-Comparison-v1.9.6.xlsx). Functional cases are **NOT RUN** on a device.
- Test record: [`TESTING-v1.9.6.md`](TESTING-v1.9.6.md)
- Python source-contract suite: [`qa/test_source_contracts.py`](qa/test_source_contracts.py), **25/25 PASS**.

## Verification limits

- Gradle `testDebugUnitTest`: **NO-SOURCE**; there are no Android/JUnit tests in this app module.
- `lintDebug` analysis was blocked before execution by Maven Central dependency-resolution TLS handshake errors; no lint pass is claimed.
- `adb devices -l` returned no connected emulator or phone. Alarm editor visibility on-screen, create/edit behavior, reminder delivery, notification actions, scheduling, audio/vibration, dial gestures, navigation feel, and accessibility remain unverified at runtime.
- Google Clock comparison is based on help documentation; no same-device testing was done.
- Debug APK is not store-ready. Before release, run the NOT RUN cases on target Android/OEM versions and sign with a protected release key.

## Previous baseline

The accepted v1.9.5 debug APK and source archive remain recorded in [`ARTIFACTS.md`](ARTIFACTS.md).

## Publishing status

No GitHub repository URL or access was supplied, so these artifacts were not pushed to GitHub.
