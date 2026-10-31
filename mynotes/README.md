# MyNotes — independent Android project

Open **this directory**, not the repository root, in Android Studio.

Latest development: **0.20.0 (23), Find, clean checklist conversion and content names in Notes + rich Doc**. [Raw APK, exact qualification and limits](docs/NOTES-FIND-NAMES-DEVELOPMENT-HANDOFF.md) · [Find/names guide](docs/NOTES-FIND-NAMES-USER-GUIDE.md) · [rich Doc guide](docs/NOTES-AND-DOC-USER-GUIDE.md) · [job ledger](docs/NOTES-FIND-NAMES-AND-DOC-FOLLOWUP.md). Install over existing data; never uninstall/clear data. Back up recordings separately; do not downgrade schema8 data. All4 emulator/PDF profiles pass, including Android16; automation is not novice/physical-device/public-release approval.

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

`.github/workflows/notes-ci.yml` requests broad publication qualification only on a deliberate
workflow/setup-action change or explicit dispatch. The nonpublishing refinement workflow
runs one build plus three native profiles on a deliberate workflow-file change. Reserve jobs
first: the original24 are exhausted; a later10-job allowance has4 reserved/6 unreserved.
Documentation/evidence pushes use `[skip ci]`; no automatic spending of the remainder. `notes-model-audit.yml` separately inspects the official
offline speech model only when that provenance workflow changes; it does not publish an app. Existing Android CI and Tests are paused here: automatic
triggers removed and all jobs set to `if: ${{ false }}`. Their step definitions are
retained so they can be restored deliberately. GitHub's global disable API returned
403, so their API state may still display active. Other branches are not modified.

See [interaction repair](docs/INTERACTION-REPAIR-v0.10.md): Plus → Reminder, dedicated Labels/Colour, list-only Copy/Trash, fresh sheet state, precise native word selection and reminder reconciliation fixes.

## Historical public release (not the latest development APK)

Last published release: **0.12.0 (13)**. [Download APK](https://github.com/tarungittarun/TicTic/releases/download/mynotes-v0.12.0-13/MyNotes-v0.12.0-13.apk) · [checksum](releases/MyNotes-v0.12.0-13.apk.sha256) · [receipt](releases/MyNotes-v0.12.0-13.apk.release.json).

[Qualification40779382571](https://github.com/tarungittarun/TicTic/actions/runs/40779382571) passed all **30 jobs /21 Android lanes**, **114 JVM /157 unique Android cases /1278 Android executions**, with all prior-release note seeds retained and installed APK hashes matched. Fresh light/dark/200%-font/landscape/tablet review is recorded. Source `e27dc7a863bb0f57b6ae127f8fa465e30a8072c9`; stable package/certificate; schema6 reads1–6.

This repairs real-device feedback rejecting0.11 despite its previous green emulator run: visible folders/timestamp files, image/PDF provider policy, manual Trash deletion, drawing and Settings/child navigation. [Device diagnosis, fixes and honest limits](docs/DEVICE-REPAIR-v0.12.md). **The user's physical phone must still be retested; publication is not universal device or full-product acceptance.** Future MySpace embedding remains a contract, not certification.

Published0.12 APK: **66,568,652 bytes**; SHA-256 `0dc892223689b77a2ed64cc25e93561b33db4a72b86c5f6b48845be00af2df7e`. GitHub's public asset digest matches the checked-in receipt. [Actual-runtime overview](../NOTES-UI-OVERVIEW.pdf).
Large APKs use versioned GitHub Release assets and checked-in provenance receipts, never replacement
uploads or growing binary copies in Git. Prior releases remain unchanged. Install over the old app;
do not uninstall/clear data. JSON backups exclude voice WAVs: export important recordings separately.

Reminders are **inexact**, notifications/microphone are optional and contextual, and no Internet or
exact-alarm permission is declared. Widget is a four-title projection, not an interactive checklist.
At this historical release, arbitrary attachments, audio-inclusive backups, richer recurring
reminders, full undo/markdown/hashtag workflows, physical-device qualification, Play and
MySpace integration were open. Later development added bounded restart-safe Undo/Redo
and rich Doc: the latest handoff above, not this historical list, defines current scope. Wall dragging stays last.
See [device guide](docs/STANDALONE-TEST-GUIDE.md), [local OS surfaces](docs/LOCAL-SURFACES-v0.7.md)
and [offline model scope](docs/OFFLINE-ML-v0.6.md). The historical, source-scoped native/disk soak **passed 70 measured
minutes each on API26/35, totaling 5,300 complete OCR/ASR/PCM/repository/backup cycles**. This is debug
component evidence, not a long UI/diagnostics test of 0.8 or an all-device/inference-peak-memory guarantee.
See [UI symbols and diagnostics guide](docs/UI-AND-DIAGNOSTICS-v0.8.md).
[Full report card](docs/REPORT-CARD.md) · [durable qualification metrics](docs/evidence/2026-10-29-qualification.json)
· [next large acceptance slices](docs/NEXT-ACCEPTANCE.md).

## Continuous integration

See [parallel CI architecture and maintenance](docs/CI-ARCHITECTURE.md) for job gates, exhaustive test partitions, APK identity evidence and CI-only same-version handling.
