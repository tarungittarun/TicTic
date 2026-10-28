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

Current published release: **0.10.0 (11)** — content-first Paper & Ink redesign, contextual editor tools, native paragraph reflow,
and opt-in local diagnostics in Settings. Existing local note reminders and resizable/re-placeable
pinned-first home-screen widget, plus bundled offline English/Hindi/Kannada OCR with review,
automatic offline English voice transcription, retained WAV inbox/export, rich notes,
checklists/labels, flowcharts, vector PDF and local storage/backup.

[**Download MyNotes 0.10.0 (11)**](https://github.com/tarungittarun/TicTic/releases/download/mynotes-v0.10.0-11/MyNotes-v0.10.0-11.apk)
· [checksum](releases/MyNotes-v0.10.0-11.apk.sha256) · [receipt](releases/MyNotes-v0.10.0-11.apk.release.json)

Qualification [40642752968](https://github.com/tarungittarun/TicTic/actions/runs/40642752968)
passed all required gates and all **21 Android lanes**, including **123/123 methods in each full review profile**.
Actual light/dark/200% font/landscape/tablet evidence from equivalent production source run40641156198
was inspected before approval. Local gates: **115 Notes checks / 505 root checks**.
Runtime package/certificate remain stable; schema5 reads1–5. Emulator approval is not user acceptance
or physical OEM/TalkBack/thermal/acoustic certification.

APK: **66,522,692 bytes**; SHA-256 `2ffc9457ee3ac0f6508d26981e60245cff9afebcfb1bd94e0db9475adb2aeb03`.
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
