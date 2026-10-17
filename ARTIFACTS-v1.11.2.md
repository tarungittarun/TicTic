# TicTic v1.11.2 artifact record

- APK: [`artifacts/TicTic-v1.11.2-debug.apk`](artifacts/TicTic-v1.11.2-debug.apk)
- Package/version: `app.tictic.clock`, `1.11.2` (versionCode `23`); min API `23`; target/compile API `34`
- Size: **724,118 bytes**
- SHA-256: `746e979746481d3bd1c6f4849aef0afaf3161948d698464c77a259d95762faa2`
- Direct SHA-256 file: [`SHA256SUMS-v1.11.2.txt`](SHA256SUMS-v1.11.2.txt)
- Checksum sidecar: [`artifacts/TicTic-v1.11.2-debug.apk.sha256`](artifacts/TicTic-v1.11.2-debug.apk.sha256)
- APK integrity: zip alignment and debug-signature verification passed. See [`TESTING-v1.11.2.md`](TESTING-v1.11.2.md) for automated check counts and the runtime QA limitation.
- This is a debug-signed test/sideload candidate, **not** a store-signed release. Physical-device/emulator QA has not been run.

## Latest five retained standalone APKs

Only versions **1.9.9, 1.10.0, 1.11.0, 1.11.1 and 1.11.2** remain as standalone APKs in `artifacts/`.

- Index: [`artifacts/APK-INDEX.md`](artifacts/APK-INDEX.md), with [`artifacts/index.html`](artifacts/index.html) for direct file links.
- Collection ZIP: [`TicTic-Debug-APKs-v1.9.9-to-v1.11.2.zip`](TicTic-Debug-APKs-v1.9.9-to-v1.11.2.zip), **2,701,063 bytes**, SHA-256 `8c8fca005edd3f3f470734f5c5450e89b371fb4a5a571b5a008fb69a4dcb9ba8`.
- The ZIP also appears in `artifacts/` and includes all five APKs, checksum sidecars and indexes.

## Source and test scope

- Source import bundle: [`TicTic-v1.11.2-GitHub-Import.zip`](TicTic-v1.11.2-GitHub-Import.zip); its SHA-256 is recorded in the adjacent `.zip.sha256` file.
- The release record and manual test plan are [`TESTING-v1.11.2.md`](TESTING-v1.11.2.md) and [`qa/QA-v1.11.2-manual.md`](qa/QA-v1.11.2-manual.md). Static-source tests/build/lint do not prove Android runtime layout, accessibility, alarm delivery or device/OEM behavior. See the record before treating the debug candidate as ready for daily use or release.
