# MyNotes — independent Android project

Open **this directory**, not the repository root, in Android Studio.

- `app/`: standalone app.space.notes host (debug uses .debug).
- `runtime/`: reusable Notes UI and private-storage repository.
- `domain/`: pure Java document rules.
- `qa/`, `tools/`, `docs/`: Notes checks, protocol reference and development evidence.
- Own Gradle wrapper, plugin configuration, settings, properties and signing copy.

```sh
cd mynotes
./gradlew :notes-domain:test :notes:assembleRelease :notes:assembleDebugAndroidTest
```

From the repository root, run just Notes source/protocol tests (Python3 and Node22+ for the emulator-observer self-test):

```sh
python3 -m unittest discover -s mynotes/qa -t .
```

MyNotes does not read any parent Gradle build/configuration, depend on parent projects,
or use the existing shared MySpace data store. Its signing file is an independent copy
of the existing sideload identity, preserving the certificate for upgrades. It is not
loaded from the parent's signing directory. Do not rotate or regenerate it per build.
The copied wrapper/properties are now independently maintained, not symlinked.

Integration into MySpace is a later explicit runtime dependency, host adapter and data
migration. No code has been copied from calculator. The root settings no longer includes
these modules. The existing root Notes planning/PDF files remain historical/reference
entry points; implementation evidence is under mynotes/docs.

## Workflow policy (owner instruction)

`.github/workflows/notes-ci.yml` runs automatically on this branch, path-filtered
to MyNotes and its workflow. `notes-model-audit.yml` separately inspects the official
offline speech model only when that provenance workflow changes; it does not publish an app. Existing Android CI and Tests are paused here: automatic
triggers removed and all jobs set to `if: ${{ false }}`. Their step definitions are
retained so they can be restored deliberately. GitHub's global disable API returned
403, so their API state may still display active. Other branches are not modified.

See [interaction repair](docs/INTERACTION-REPAIR-v0.10.md): Plus → Reminder, dedicated Labels/Colour, list-only Copy/Trash, fresh sheet state, precise native word selection and reminder reconciliation fixes.

Current published release: **0.11.0 (12)**. [Download APK](https://github.com/tarungittarun/TicTic/releases/download/mynotes-v0.11.0-12/MyNotes-v0.11.0-12.apk) · [checksum](releases/MyNotes-v0.11.0-12.apk.sha256) · [receipt](releases/MyNotes-v0.11.0-12.apk.release.json).

Its publication run [40712278512](https://github.com/tarungittarun/TicTic/actions/runs/40712278512) passed all required gates /21 Android lanes, but subsequent physical-device feedback identified folder/image and usability defects. Those device reports supersede emulator-only acceptance; do not describe0.11 as fully working on the user's phone.

Candidate **0.12.0 (13)** is being qualified for those repairs. [Device evidence, implemented changes and current limits](docs/DEVICE-REPAIR-v0.12.md). New runtime inputs require fresh qualification and rendered approval; no candidate APK is published yet. The stable package/certificate is unchanged; schema6 reads1–6. Future MySpace embedding remains a contract, not certification.

Published0.11 APK: **66,558,740 bytes**; SHA-256 `83428e4fbd99e368c9d439e11ae8669e03eaf01d3976c1b6fd669f155b24150f`.
Large APKs use versioned GitHub Release assets and checked-in provenance receipts, never replacement
uploads or growing binary copies in Git. Prior releases remain unchanged. Install over the old app;
do not uninstall/clear data. JSON backups exclude voice WAVs: export important recordings separately.

Reminders are **inexact**, notifications/microphone are optional and contextual, and no Internet or
exact-alarm permission is declared. Widget is a four-title projection, not an interactive checklist.
Media attachments, richer/recurring reminders, full undo/markdown/hashtag workflows, physical-device
qualification, Play publication and eventual MySpace integration remain open. Wall dragging stays last.
See [device guide](docs/STANDALONE-TEST-GUIDE.md), [local OS surfaces](docs/LOCAL-SURFACES-v0.7.md)
and [offline model scope](docs/OFFLINE-ML-v0.6.md). The historical, source-scoped native/disk soak **passed 70 measured
minutes each on API26/35, totaling 5,300 complete OCR/ASR/PCM/repository/backup cycles**. This is debug
component evidence, not a long UI/diagnostics test of 0.8 or an all-device/inference-peak-memory guarantee.
See [UI symbols and diagnostics guide](docs/UI-AND-DIAGNOSTICS-v0.8.md).
[Full report card](docs/REPORT-CARD.md) · [durable qualification metrics](docs/evidence/2026-10-28-qualification.json)
· [next large acceptance slices](docs/NEXT-ACCEPTANCE.md).

## Continuous integration

See [parallel CI architecture and maintenance](docs/CI-ARCHITECTURE.md) for job gates, exhaustive test partitions, APK identity evidence and CI-only same-version handling.
