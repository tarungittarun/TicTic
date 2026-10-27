# M0 execution report — slice 1

Date: 2026-10-27. Status: **M0 in progress, not complete**.

## Actions and observed outcomes

1. Verified branch HEAD e017752 and clean worktree before work.
2. Inspected Gradle module graph and legacy NotesStore. Existing shared Room/prefs and
   cap confirmed. No existing app/feature/schema/workflow files modified.
3. Checked local JVM availability: java and javac are absent. No Java compilation,
   Android build, emulator test or physical-device result is claimed. Android builds
   remain on GitHub Actions; no workflow dispatched in this slice.
4. Added ADR-001 for additive domain/runtime/host isolation and immutable recoverable
   folder generations. Android SAF durability remains an explicit qualification gate.
5. Implemented a stdlib-only executable storage-protocol reference. No Android app
   consumes it. 13 fault tests passed: Unicode reopen; partial and before-write
   failure; process death before acknowledgement; stale writer; corrupt latest;
   corrupt-only library; divergent heads; missing parent; future schema; cycle;
   mutable caller isolation; explicit empty commit.
6. Created traceability ledger: exactly K001–K100 plus 21 expanded requirements.
   Initial extraction caught six unrelated numbered rules and failed its assertion;
   corrected extraction to inventory section 4 only. Two temporary missing-ledger
   test errors then cleared after generating the ledger. No failed gate was pushed.
7. Added five planning/isolation checks. Final gate:
   `python3 -m unittest discover -s qa`: **408 tests, OK** (390 existing + 18 new).
   These are protocol and source/planning checks, not Android behaviour evidence.

## What exists now

- docs/notes/ADR-001-runtime-and-storage.md — architecture decision.
- docs/notes/REQUIREMENTS.md — all original features and expanded acceptance obligations.
- tools/notes_storage_probe.py — executable reference, not production persistence.
- qa/test_notes_storage_probe.py — 13 executable protocol fault tests.
- qa/test_notes_m0_contracts.py — five scope/isolation checks.
- tools/build_notes_requirements.py — initial ledger generator. Do not regenerate over
  hand-added evidence once implementation starts.

## Next execution slice / open gates

1. Versioned structured document schema and test fixtures, including rich text,
   checklist rows, diagram graph, attachment references, reminders and trash metadata.
2. Buildable additive :notes-domain/:notes-runtime/:notes skeleton, host capability
   contracts and dedicated Notes build/test workflow. No existing screen replacement.
3. Android local-provider adapter: permissions, readback, interrupted saves, tree
   migration, recovery journal and index rebuild on actual SAF providers.
4. Offline ML dependency/model license/hash selection and native ABI/build spike.
   Benchmark real low/mid/high-memory phones before deciding defaults. OCR/ASR have
   NOT been integrated, run or qualified in this slice.

Known protocol limits: no media blobs, export/import, GC, durability fsync, provider
adapter, encryption or cross-process lock yet. The protocol detects forks but does
not resolve them. A complete commit whose acknowledgement is lost is recovered as
saved; the UI must reconcile operation identity instead of blindly repeating edits.
No APK is available from this slice. No calculator edits or tests.yml reruns occurred.


## Slice 2 — larger standalone vertical slice

- Added :notes-domain, :notes-runtime and :notes; MySpace and old :feature-notes do not
  consume them. Added pure immutable text document metadata and seven JUnit tests.
- Added app-private versioned JSON/AtomicFile repository, duplicate-ID checks, failed
  load protection, no 500-item cap, and serialized background disk operations.
- Added standalone host and reusable UI controller: text create/edit, 350 ms autosave,
  lifecycle flush, basic rotation state, 150 ms search debounce on separate worker,
  latest-query rejection, pins, archive, seven-day Trash/restore and one-level state
  Undo. System light/dark palette and honeycomb-family launcher icon included.
- Added five Android persistence tests (Unicode metadata, 650 notes, corruption,
  empty commit and failed AtomicFile write). These tests have not compiled/run.
- Added manual-only MyNotes workflow to run domain tests and build release + Android
  test APK. Existing workflows unchanged. Commit 5b5a294 pushed with [skip ci].
- Deliberate `gh workflow run notes-ci.yml --ref arena/3caca636-tictic` returned HTTP
  404. GitHub workflow listing contains only Android CI and tests.yml. New manual
  workflow is not registered/available for dispatch; no Notes build ran. Did not
  rerun frozen tests, trigger unrelated Android CI, or modify main to work around it.
- Final local checks: 416 Python tests OK (390 original + 26 new). New JUnit and
  Android tests remain unexecuted; no APK or emulator/device pass is claimed.

### Current limitations / release blockers

Early text UI, not final PDF design. App-private storage only: user-picked directory,
recovery journal, export/import, attachments, rich schema, reminders, widgets, recording,
OCR/ASR and diagrams are not implemented. Autosave is debounced; killing the process
inside that window can lose latest keystrokes. Failed writes retain memory only, so
a durable recovery journal is required before calling storage complete. Rotation
uses Bundle for draft text and is not qualified for very large documents yet.
Undo is one-level state restoration, not full rich-editor undo or final snackbar UX.
The repository serializes whole-library snapshots, so scale/memory qualification
and incremental persistence are still needed. Shared process disk queue prevents
rotation from opening ahead of previous queued writes; this is not a cross-app lock.

Next: unblock deliberate standalone GitHub build registration without touching frozen
workflows/main, then compile and instrument; implement SAF storage + recovery, evolve
structured rich-content schema with migration tests and qualify offline ML.
