# TicTic v1.11.1 artifact record

## Debug APK

- APK: [`artifacts/TicTic-v1.11.1-debug.apk`](artifacts/TicTic-v1.11.1-debug.apk)
- Package/version: `app.tictic.clock`, `1.11.1` (versionCode `22`); min API `23`; target/compile API `34`
- Size: **714,405 bytes**
- SHA-256: `e1023e41c216b9f34bc419164022af4e774118a9e99762a51f7300628f3bd5b1`
- Direct SHA-256 file: [`SHA256SUMS-v1.11.1.txt`](SHA256SUMS-v1.11.1.txt)
- Checksum sidecar: [`artifacts/TicTic-v1.11.1-debug.apk.sha256`](artifacts/TicTic-v1.11.1-debug.apk.sha256)
- Signing: Android Gradle debug key; local sideload/testing only, not production/store signed.

## Verification

- Gradle clean unit-test/assemble/lint build: **BUILD SUCCESSFUL**.
- JUnit `TimeMathTest`: **7/7 passed**; Python source-contract checks: **54/54 passed**.
- Lint: **0 errors, 240 warnings**.
- `aapt dump badging`: package, versionName/versionCode, min/target/compile API confirmed.
- `zipalign -c -v 4`: passed.
- `apksigner verify --verbose --print-certs`: v1/v2 debug signature verification passed. Non-fatal Gradle app-metadata entry signature warning is documented in `TESTING-v1.11.1.md`.
- Android runtime/device QA: **NOT RUN**. This is a verified debug build, not a release-ready APK.

## Bundles

- All retained debug APKs: [`TicTic-Debug-APKs-v1.9.8-to-v1.11.1.zip`](TicTic-Debug-APKs-v1.9.8-to-v1.11.1.zip), **2,670,525 bytes**, SHA-256 `127f0c95b76c04a7553ea35861dacf891c626640c8e61d979ec109bc6c52f57e`, with sibling `.sha256` and a copy in `artifacts/`.
- Source import: [`TicTic-v1.11.1-GitHub-Import.zip`](TicTic-v1.11.1-GitHub-Import.zip), with sibling `.zip.sha256`. Includes source, docs, QA, the five retained APKs/checksums, and indexes; excludes machine-specific local properties and generated build/cache output.
