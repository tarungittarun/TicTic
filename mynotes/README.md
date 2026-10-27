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

Current release: **0.5.0 (6)** — editable flowcharts, local vector PDF export,
incoming plain-text sharing, global/additive labels and responsive editor/selection
controls, plus the previous core and local storage/backup. CI **40578488274** passed
51 JVM tests and **441 executions of 63 unique Android tests across 13 lanes**;
468 local checks passed. See docs/STANDALONE-TEST-GUIDE.md and
[diagram/export scope](docs/DIAGRAMS-AND-EXPORT-v0.5.md). Required offline OCR/ASR,
media, reminders/widgets and full plan/device qualification remain open.

## Continuous integration

See [parallel CI architecture and maintenance](docs/CI-ARCHITECTURE.md) for job gates, exhaustive test partitions, APK identity evidence and CI-only same-version handling.
