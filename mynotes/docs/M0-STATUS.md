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

## Slice 3 — independent directory and Notes-only automation

Owner approved isolating MyNotes in its own directory and pausing other workflows.

- Moved new Notes modules to mynotes/app, mynotes/runtime and mynotes/domain. There
  is one source implementation; old root module directories were removed, not copied.
- Added independent Gradle root/settings, wrapper, properties and a separate copy of
  the existing sideload signing identity. Parent settings no longer registers Notes.
- Moved Notes tests/tools/evidence under mynotes. Root qa has a small compatibility
  loader so full local validation still includes Notes checks.
- Added guards against parent Gradle dependencies and non-Notes Java imports.
- GitHub global workflow disable API returned 403. Instead removed push/PR triggers
  from old Android CI and Tests on THIS branch, and hard-disabled every job with
  `if: ${{ false }}`. Other branches were not changed; global API state remains active.
- Made Notes workflow path-filtered push-triggered; it builds from mynotes only,
  runs only the Notes source/protocol suite, compiles domain and Android tests, and
  publishes the standalone APK with checksum/source revision if successful.
- Commit 5638032 intentionally omitted [skip ci] to start this Notes-only run.
  GitHub registered the new workflow (398597736), and push run 40561897413 started.
  Explicit dispatch API is also permission-blocked (403); push is the working trigger.
  No legacy workflow was dispatched/rerun.
- Validation: 30 Notes checks OK; 420 total local checks OK. No changes under app/,
  calc/, feature-calculator/, feature-notes/, core-data/ or root build.gradle.

CI result will be recorded separately once observed. Android device behaviour, SAF,
ML and full planned feature acceptance remain unverified/pending as listed above.


### Slice 3 CI outcome — observed

MyNotes-only push run **40561897413 succeeded** in 1m26s. Domain JVM test task,
release assembly and Android test APK assembly all passed. Five Android instrumented
persistence tests were compiled but NOT executed. No emulator/device outcome is claimed.
Workflow published MyNotes-v0.1.0-1.apk and checksum to mynotes/releases in commit
5abdfcc; pulled via fast-forward and verified `sha256sum -c`: OK. Built source is
563803284a27cdd617ae73868989fc63eb6c81d6. The old workflows' last runs remain historical;
no calculator/MySpace jobs ran for this push. This resolves the prior build-registration
blocker via Notes-only push events, not via permission-blocked manual dispatch.

## Slice 4 — 0.2.0 (2), storage/recovery and release compatibility

Implementation added: separate document codec retaining v1 compatibility; local SAF tree
backend limited to ExternalStorageProvider; immutable SHA-256 generations; stale/fork
checks; verified empty-folder migration; explicit return to private storage; durable
private recovery before external writes; read/write persisted folder grant callbacks;
Storage UI with limitations and retry. No MySpace/calculator source/workflow changes.

Release gate added: apksigner certificate equality + stable application ID + increasing
versionCode compared to published APKs; published version overwrites rejected. CI now
compiles debug/release/test APKs, attempts previous-release update, and runs 13 Android
persistence/protocol tests before publishing. Eight new instrumented tests cover provider-
independent fault paths and original v1 format; five Python release-policy checks pass.

Local gate at this point: 425 tests OK (35 Notes + 390 legacy checks). Java compilation,
Android tests, update smoke and actual folder-provider qualification have NOT yet run
for this slice. The next deliberate push triggers Notes CI; record its outcome below.
Actual SAF picker/device/SD tests remain distinct from in-memory protocol tests.
