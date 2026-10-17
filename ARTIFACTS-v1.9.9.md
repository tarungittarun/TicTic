# TicTic v1.9.9 artifact record

## Debug APK

- Download: [`artifacts/TicTic-v1.9.9-debug.apk`](artifacts/TicTic-v1.9.9-debug.apk)
- Gradle output: `app/build/outputs/apk/debug/app-debug.apk`
- Package: `app.tictic.clock`
- Version: `1.9.9` (versionCode `19`); min API `23`; target/compile API `34`
- Size: **699,616 bytes**
- SHA-256: `f84543d7583914a8b0ab482a7ab33c4797b44aa9f3d72150009006fb68427da1`
- Checksum file: [`artifacts/TicTic-v1.9.9-debug.apk.sha256`](artifacts/TicTic-v1.9.9-debug.apk.sha256)
- Signing: Android Gradle **debug key**; for local installation/testing only, not Play/store distribution.

## Verification

- `clean testDebugUnitTest assembleDebug lintDebug`: **BUILD SUCCESSFUL**.
- JUnit timing math: **6/6 passed**; Python source-contract suite: **43/43 passed**.
- Lint: **0 errors, 239 warnings**; see [`TESTING-v1.9.9.md`](TESTING-v1.9.9.md) for limitations.
- `aapt dump badging`: package/version/min/target/compile metadata confirmed.
- `zipalign -c -v 4`: passed.
- `apksigner verify --verbose --print-certs`: passed v1 and v2; debug certificate. It emitted the standard warning that Gradle's `META-INF/com/android/build/gradle/app-metadata.properties` entry is not signed; this did not fail signature verification.
- JUnit/Hamcrest test libraries are not packaged in the APK.

## QA and publication limits

All Android runtime/device scenarios remain **NOT RUN**. An API 34 emulator launch was attempted but stopped before boot because the host lacked RAM (2.5 GB requested, about 1 GB available) and hardware acceleration; no physical device was attached. This file records a validated debug build, **not a final/release-ready APK**. See [`qa/QA-v1.9.9-manual.md`](qa/QA-v1.9.9-manual.md) and [`TESTING-v1.9.9.md`](TESTING-v1.9.9.md).

No source archive was generated for this build. No commit or push was made; the public repository remains empty and this session has no authorized write mechanism.