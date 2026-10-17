# TicTic v1.11.0 artifact record

## Debug APK

- Download: [`artifacts/TicTic-v1.11.0-debug.apk`](artifacts/TicTic-v1.11.0-debug.apk)
- Gradle output: `app/build/outputs/apk/debug/app-debug.apk`
- Package: `app.tictic.clock`
- Version: `1.11.0` (versionCode `21`); min API `23`; target/compile API `34`
- Size: **714,290 bytes**
- SHA-256: `b7676d525f3308b9c800ed49e3ef2359ef2085d53a8b06b70d5705e068c00935`
- Checksum file: [`artifacts/TicTic-v1.11.0-debug.apk.sha256`](artifacts/TicTic-v1.11.0-debug.apk.sha256)
- Signing: Android Gradle debug key; local sideload/testing only, not production/store signed.

## Verification

- `clean testDebugUnitTest assembleDebug lintDebug`: **BUILD SUCCESSFUL**.
- JUnit `TimeMathTest`: **7/7 passed**; Python source-contract suite: **53/53 passed**.
- Lint: **0 errors, 240 warnings**. Reports and scope are in [`TESTING-v1.11.0.md`](TESTING-v1.11.0.md).
- `aapt dump badging`: package/version/min/target/compile metadata confirmed.
- `zipalign -c -v 4`: passed.
- `apksigner verify --verbose --print-certs`: v1/v2 signature verification passed with the debug certificate. It also reports a non-fatal warning that `META-INF/com/android/build/gradle/app-metadata.properties` is not covered by the JAR signature.

## Source import bundle

- Historical source bundle and APK collection archives were pruned during the v1.11.1 cleanup; use the current [`TicTic-v1.11.1-GitHub-Import.zip`](TicTic-v1.11.1-GitHub-Import.zip).
- Includes Android source, tests, documentation, focused QA plan, the interactive `Gel-Interaction-Preview.html`, and all seven retained APK versions (1.9.5–1.11.0) with checksums. Excludes machine-specific `local.properties`, Gradle/build output, caches and secrets. Older standalone APKs were later pruned; the current five-version collection is `TicTic-Debug-APKs-v1.9.8-to-v1.11.1.zip`.
- The bundle’s checksum is supplied in the sibling `.zip.sha256` file, outside the archive to avoid a self-checksum loop.

## QA and publication limits

The APK and import ZIP were regenerated after the screenshot-driven settings/glass follow-up. No Android runtime/device workflow was tested for this build. All rows in [`qa/QA-v1.11.0-manual.md`](qa/QA-v1.11.0-manual.md) remain **NOT RUN**. This is a verified debug build, **not a final or release-ready APK**. No commit or GitHub push was made.