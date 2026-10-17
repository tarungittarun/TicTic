# TicTic v1.10.0 artifact record

## Debug APK

- Download: [`artifacts/TicTic-v1.10.0-debug.apk`](artifacts/TicTic-v1.10.0-debug.apk)
- Gradle output: `app/build/outputs/apk/debug/app-debug.apk`
- Package: `app.tictic.clock`
- Version: `1.10.0` (versionCode `20`); min API `23`; target/compile API `34`
- Size: **703,423 bytes**
- SHA-256: `2bc03738ade4b7103c8a13b5b75b1d6478edfd332fc2998e962d7896d1ff33cb`
- Checksum file: [`artifacts/TicTic-v1.10.0-debug.apk.sha256`](artifacts/TicTic-v1.10.0-debug.apk.sha256)
- Signing: Android Gradle debug key; local install/testing only, not a production/store signature.

## Source import bundle

- The historical v1.10.0 project bundle was pruned; use the current v1.11.1 source import bundle listed in `ARTIFACTS-v1.11.1.md`.
- Contents: Android source, build configuration/wrapper, tests, QA and project documentation, v1.10.0 APK plus its checksum. Excludes machine-specific `local.properties`, Gradle/build outputs and caches.
- The APK checksum manifest is `SHA256SUMS-v1.10.0.txt` (included). The v1.10.0 import ZIP and its checksum were later pruned during the v1.11.1 cleanup.

## Verification

- Clean `testDebugUnitTest assembleDebug lintDebug`: **BUILD SUCCESSFUL**.
- JUnit timing math: **7/7 passed**; Python source-contract suite: **48/48 passed**.
- Lint: **0 errors, 241 warnings**; report locations and scope are in [`TESTING-v1.10.0.md`](TESTING-v1.10.0.md).
- `aapt dump badging`: package/version/min/target/compile metadata confirmed.
- `zipalign -c -v 4`: passed.
- `apksigner verify --verbose --print-certs`: v1/v2 verification passed, debug certificate. The tool warns that the standard Gradle app-metadata properties entry in `META-INF` is not signed; this did not fail verification.
- JUnit/Hamcrest test JARs are not packaged in the APK.

## QA and publication limits

No Android runtime/device workflow was tested for this build. All rows in [`qa/QA-v1.10.0-manual.md`](qa/QA-v1.10.0-manual.md) remain **NOT RUN**. The emulator available in the workspace previously failed before boot due to host resource limits; no handset was attached. This is a verified debug build, **not a final or release-ready APK**.

The public GitHub repository requires owner-authenticated access. No commit or push was made.