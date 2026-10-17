# TicTic build artifacts — v1.9.8

## Current debug APK

- File: `/home/user/TicTic-v1.9.8-debug.apk`
- Package: `app.tictic.clock`
- Version: `1.9.8` (versionCode `18`); min API 23; target/compile API 34
- Size: **693,483 bytes**
- SHA-256: `2c56dbc2b320263819bfbff461b8ab4d805a38690a3a7f866d826ac47feaab0c`
- Signing: Gradle Android debug key; for sideload/testing only, not production/store signed.
- Verification: clean Android SDK 34 debug build passed; AAPT reports the expected package/version/SDK metadata; `zipalign -c -p 4` passed; `apksigner verify --verbose --print-certs` verified APK v1 and v2 signatures. The standard `META-INF/com/android/build/gradle/app-metadata.properties` warning remains.

## Source, QA and implementation notes

- Source archive: `/home/user/TicTic-Android-Source-v1.9.8.zip`
- Checksums: `/home/user/SHA256SUMS-v1.9.8.txt`
- Test record: [`TESTING-v1.9.8.md`](TESTING-v1.9.8.md)
- Manual QA plan: [`qa/QA-v1.9.8-manual.md`](qa/QA-v1.9.8-manual.md)
- Engineering/decision log: [`ENGINEERING-LOG-v1.9.8.md`](ENGINEERING-LOG-v1.9.8.md)

## Publication status

- APK is debug-signed and not a store-ready artifact.
- Repository URL supplied: `https://github.com/tarungittarun/TicTic.git`. Anonymous inspection found it public but empty (`main` configured, no refs/commits). The GitHub connector is unsupported in this session and no authorized write credentials are available. No commit or push was made.
- App runtime/device QA and lint are not run; see the test record before distributing.
