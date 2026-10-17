# TicTic build artifacts — current release pointer

> Current v1.11.2 debug APK, checksum and test status are documented in [`ARTIFACTS-v1.11.2.md`](ARTIFACTS-v1.11.2.md). Historical records remain below.

## Current v1.11.2 debug APK — holistic responsive UI follow-up

- APK: [`artifacts/TicTic-v1.11.2-debug.apk`](artifacts/TicTic-v1.11.2-debug.apk)
- Package/version: `app.tictic.clock`, `1.11.2` (versionCode 23); min API 23; target/compile API 34
- Size: **724,118 bytes**; SHA-256: `746e979746481d3bd1c6f4849aef0afaf3161948d698464c77a259d95762faa2`
- Checks: clean unit-test/assemble and lint builds passed; 7 JUnit tests and 57 Python source-contract checks passed; 0 lint errors / 242 warnings; APK alignment and debug-signature verification passed.
- Runtime/device QA has **not** been completed. See [`TESTING-v1.11.2.md`](TESTING-v1.11.2.md); this remains a debug candidate, not a release-ready APK.
- Latest five APK collection: [`TicTic-Debug-APKs-v1.9.9-to-v1.11.2.zip`](TicTic-Debug-APKs-v1.9.9-to-v1.11.2.zip), 2,701,063 bytes, SHA-256 `8c8fca005edd3f3f470734f5c5450e89b371fb4a5a571b5a008fb69a4dcb9ba8`. Source bundle: [`TicTic-v1.11.2-GitHub-Import.zip`](TicTic-v1.11.2-GitHub-Import.zip), with sibling `.zip.sha256` checksum.

## Historical v1.10.0 debug APK

- File: [`artifacts/TicTic-v1.10.0-debug.apk`](artifacts/TicTic-v1.10.0-debug.apk)
- Package/version: `app.tictic.clock`, `1.10.0` (versionCode 20); min API 23; target/compile API 34
- Size: **703,423 bytes**; SHA-256 `2bc03738ade4b7103c8a13b5b75b1d6478edfd332fc2998e962d7896d1ff33cb`
- Historical test record: [`TESTING-v1.10.0.md`](TESTING-v1.10.0.md). It remains a debug candidate, not release-ready.

## Historical v1.9.9 debug APK

- File: [`artifacts/TicTic-v1.9.9-debug.apk`](artifacts/TicTic-v1.9.9-debug.apk)
- Package/version: `app.tictic.clock`, `1.9.9` (versionCode 19); min API 23; target/compile API 34
- Size: **699,616 bytes**; SHA-256 `f84543d7583914a8b0ab482a7ab33c4797b44aa9f3d72150009006fb68427da1`
- Historical test record: [`TESTING-v1.9.9.md`](TESTING-v1.9.9.md). It was a debug candidate, not release-ready.

## All retained debug APKs

- Version index with direct per-file links, sizes and SHA-256 values: [`artifacts/APK-INDEX.md`](artifacts/APK-INDEX.md), plus the direct-download page [`artifacts/index.html`](artifacts/index.html).
- Current collection download (all five APKs, sidecars and indexes): [`TicTic-Debug-APKs-v1.9.9-to-v1.11.2.zip`](TicTic-Debug-APKs-v1.9.9-to-v1.11.2.zip). Its `.sha256` companion is beside it.
- Versions retained in `artifacts/`: 1.9.9, 1.10.0, 1.11.0, 1.11.1 and 1.11.2. Older standalone APK versions were pruned at the user’s request.

## Historical v1.9.5 debug APK

- File: [`artifacts/TicTic-v1.9.5-debug.apk`](artifacts/TicTic-v1.9.5-debug.apk).
- Version: `1.9.5` (versionCode 15); min API 23; target/compile API 34
- Size: **673,685 bytes** (about 658 KiB)
- SHA-256: `f7eeb85e6df75daed503939b1c368d7b71fe55375404238c926135ae9e6ff274`
- Historical checks: aapt metadata, zipalign and v1/v2 signature checks passed. The benign `META-INF/com/android/build/gradle/app-metadata.properties` signature warning was recorded.

## Historical v1.9.5 source and QA artifacts

- Versioned source archive: `/home/user/TicTic-Android-Source-v1.9.5.zip`
- SHA-256 values for both files: [`../SHA256SUMS-v1.9.5.txt`](../SHA256SUMS-v1.9.5.txt). The source archive excludes build outputs and that checksum file.
- 49-case Clock/Alarm/Timer/Stopwatch/Reminders and Google Clock comparison: [`QA-Clock-Comparison-v1.9.5.xlsx`](QA-Clock-Comparison-v1.9.5.xlsx). Functional workbook cases remain **NOT RUN**.
- Test record: [`TESTING-v1.9.5.md`](TESTING-v1.9.5.md)
- Python source-contract suite: [`qa/test_source_contracts.py`](qa/test_source_contracts.py)

## Historical v1.9.5 verification limits

- Python source-contract checks: **20/20 PASS**; these verify source wiring, not Android runtime behavior.
- Gradle `testDebugUnitTest`: **NO-SOURCE** at that version; no JUnit tests were yet configured.
- Lint was **not run** for that historical build.
- No Android runtime behavior is inferred from the historical build record.

## Historical v1.9.4 artifact record

The prior **v1.9.4** debug APK was versionCode 14, 650,429 bytes, SHA-256 `d738bc55b2b8cf5c39c08544060ffea7b70ec938c5814434de70bf218702282c`; it was delivered earlier in this conversation.

## Publishing status

Artifacts are local debug/test builds, not store-ready release files. The public GitHub repository's `main` branch was observed with one existing commit and a root file; this workspace has no authorized write mechanism and no commit or push was made. Complete handset regression and use protected release signing before external publication.
