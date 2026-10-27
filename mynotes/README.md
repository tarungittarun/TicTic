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

From the repository root, run just Notes source/protocol tests:

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

Current accepted release: **0.6.0 (7)** — bundled offline English/Hindi/Kannada OCR
with review, automatic offline English voice transcription, optional foreground microphone,
retained/recoverable WAV inbox and local WAV export, plus the previous rich notes,
checklists, labels, flowcharts, vector PDF and local storage/backup.
[Download the APK](https://github.com/tarungittarun/TicTic/releases/download/mynotes-v0.6.0-7/MyNotes-v0.6.0-7.apk).
Qualification [40582488414](https://github.com/tarungittarun/TicTic/actions/runs/40582488414)
passed **64 JVM tests and 662 executions of 80 unique Android tests across 20 lanes**.
All 20 lanes verified the exact installed release hash. Large APKs now live in versioned
GitHub Release assets with checked-in SHA/source/signature receipts; prior versions are
never replaced. See [offline model scope and limitations](docs/OFFLINE-ML-v0.6.md).

**0.7.0 (8) candidate, not accepted until its gates are green:** portable note reminders,
optional notification permission, inexact local alarms with reboot/update reconciliation,
resizable/re-placeable pinned-first home-screen widget and seeded latest-release update tests.
Media attachments, broader corpus/physical-device qualification and the remaining full-plan
features are still open. See [local OS surfaces](docs/LOCAL-SURFACES-v0.7.md).

## Continuous integration

See [parallel CI architecture and maintenance](docs/CI-ARCHITECTURE.md) for job gates, exhaustive test partitions, APK identity evidence and CI-only same-version handling.
