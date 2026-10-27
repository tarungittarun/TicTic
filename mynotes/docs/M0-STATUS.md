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


### Slice 4 first build outcome

Run 40563316630 succeeded: release/debug/test compilation, certificate/package/version
checks, install-over-0.1.0 smoke and all 13 Android tests passed; APK 0.2.0 (2)
published in 147208e. This does not qualify the actual SAF picker/provider or SD card.
Review found that storage explanations could crowd action buttons on small screens.
Before recommending a build, added a scrolling explanation area with anchored actions
and an actual-layout test; correction is 0.2.1 (3), not an overwritten 0.2.0 APK.
Second versioned build will execute 14 Android tests. No old workflow runs were requested.


### Slice 4 delivered outcome — 0.2.1 (3)

Run **40564058421** succeeded in all three jobs: standalone, android-tests and publish.
Seven JVM rules tests passed. All **14 Android tests passed** on API 29 x86_64 Nexus 5:
five private persistence, eight generation/format/provider-rejection tests, one actual
layout visibility test. The emulator installed release 0.1.0 then 0.2.1 with `adb install
-r` and launched it without an uninstall. The release gate verified certificate/package
identity and increasing code against both published 0.1.0 and 0.2.0 APKs. This is install
compatibility, not seeded release-data migration or all-phone/provider qualification.

Published artifact commit: 979d386. Built source: 835e5fd91950c0a2f7082ec481f344200f269211.
Fetched via fast-forward and checked MyNotes-v0.2.1-3.apk.sha256: OK. Final local gate
425 tests OK. Version 0.2.0 was retained, not overwritten; recommend 0.2.1 for testing.
Only MyNotes runs were created; calculator/MySpace remain paused and their code unchanged.
Remaining major scope: SAF real-provider fault/SD/grant tests, library import/export,
compaction/scaling, rich content/lists, labels/media, OCR/ASR, diagrams, reminders/widgets
and final UI design. M0 offline qualification is still open; this is not feature-complete.

## Slice 5 — 0.3.0 (4), reopen existing libraries and portable restore

Written: non-destructive merge domain with stable conflict-copy IDs; versioned/checksummed
8 MiB bounded text-backup envelope; local-only create/open document host capabilities;
export close/reopen byte verification; folder inspection + explicit merge preview/confirm;
private pre-restore checkpoint + Previous snapshot recovery; selected-folder reopen after
re-grant; deferred picker results during library startup; source-aware storage status.
Existing v1 library readers and release ID/key remain unchanged. No MySpace/calculator edits.

Tests written: seven JVM merge scenarios; eight Android backup envelope/validation cases;
two private repository checkpoint cases; four injected-backend repository transactions
including stale preview and recovery across restart. Total expected: 14 JVM tests,
28 Android tests (14 prior + 14 new), and 430 local checks (40 Notes + 390 legacy).
At this entry local checks pass; CI/Android results for this slice are not yet known.
Actual SAF picker/SD/physical fault qualification remains outstanding. Notes-only CI
must pass before this APK is published.


### Slice 5 delivered outcome — 0.3.0 (4)

Run **40566162164** succeeded in all jobs (standalone 1m27s, Android tests 1m43s,
publish 11s). Fourteen JVM domain tests passed. All **28 Android tests passed** on API
29 x86_64 Nexus 5: seven private persistence, eight protocol/legacy/provider rejection,
eight backup validation, four injected repository transfer/recovery and one layout.

Release gate verified app.space.notes + matching signing certificate and increasing
versionCode against all prior published APKs. Install-over-0.1.0 and launch smoke passed.
This is install compatibility, not a real-provider picker/reinstall round-trip or seeded
release-data migration on every version. No such device results are claimed.

Published commit 1effdaf; source b9156ed67e503b00cca5109564957ea42f453609. Fetched by
fast-forward; MyNotes-v0.3.0-4.apk.sha256 checked OK. Final local gate: 430 tests OK.
All changes since the previous delivery are under mynotes/ and notes-ci.yml. Existing
MySpace/calculator workflows remain paused; no legacy build/test was triggered.

Still open: real SAF/provider/SD qualification, startup recovery UI for corrupt private
cache, bounded generation compaction and scale, streaming media backup, structured rich
content/lists, labels/colour UI, OCR/ASR, diagrams, reminders/widgets and final UI polish.
Do not mark the original inventory complete from this storage milestone.

## Slice 6 — substantial core Notes jump, 0.4.0 (5)

Written: immutable schema-2 document/checklist/mark/label model, version-1 migration,
restore equality including structure with legacy conflict-ID compatibility, native span
codec, RecyclerView masonry/list board with pinned sections and contrast-resolved 12
colours, sort/view prefs, label filters, structured list editor and selected-range text
formatting, colour/label pickers, bulk actions, duplicate/share, merge-to-new plus Trash
sources/Undo, and bounded rotation draft snapshots. Runtime remains standalone-only.

Expected tests: 29 JVM (14 previous + 15 structured/operation/compatibility); 40 Android
(28 previous + 8 structured codec/style + 4 actual controller/editor journeys). Local
source/protocol checks currently 435 OK. No Java/emulator success for this slice is
claimed before CI. The added Android journeys cover rich edit/properties/copy, checklist
completion/move/indent/hide/convert/Undo, bulk properties/pin/archive, share/merge/Undo.
Actual picker/SD/device/large-font/scale acceptance is still separate and outstanding.

### Slice 6 first CI outcome / diagnostic action

Run 40568910796 failed in Domain tests and standalone release (1m23s). No APK published;
Android and publish jobs skipped. Log download returned EOF from the inaccessible Actions
results host; check annotations contained only exit code 1. Added bounded compiler/JVM
failure surfacing to Notes workflow instead of guessing or touching legacy workflows.
Next push retains version 0.4.0 (5), which has never been published, and exposes the cause.

### Slice 6 diagnosed cause and fix

Diagnostic run 40569322275 exposed compileReleaseJavaWithJavac errors: literal newlines
inside the share-text separator string/character literal in NotesController lines 301–306.
This was a source-generation escaping error, not an emulator/environment failure.
Corrected to escaped newlines and added five local Java-8 literal/comment guard tests.
Added bounded Android failure annotations before the next runtime run. No APK has been
published for version 5 yet. Local gate now 440 OK; Notes-only next push validates the fix.


### Slice 6 delivered outcome — 0.4.0 (5)

Run **40569713346** succeeded: standalone 1m33s, Android tests 2m1s, publish 11s.
29 JVM tests and **40 Android tests passed**. Added tests exercise schema-1 migration,
schema-2 rich/list backup round-trip, native span serialization/edit movement, and four
real-controller journeys through view dispatch with isolated repositories. The share
journey uses a fake host, not every external share app; actual touches, large fonts,
TalkBack, physical providers and 10,000-note performance are not claimed certified.

Signing/package/increasing-version gate passed against all prior published APKs, plus
install-over-0.1.0 release and launch smoke. APK 0.4.0 (5) published in commit 30d6552;
built source cb26ef8a2b24ae38077628b404a1059631309cbc. Fetched via fast-forward and verified
MyNotes-v0.4.0-5.apk.sha256: OK. Final local gate: 440 tests (50 Notes + 390 legacy), OK.
The two earlier failed compilation runs published nothing; diagnostics exposed the
newline-literal error, and it was fixed before delivery. No environment rerun loop.

All changes since 6da7733 are under mynotes/ and Notes CI. Calculator/MySpace code and
paused legacy workflows remain unchanged. Scope limits are listed in CORE-NOTES-v0.4.md
and current guide. Offline OCR/transcription remain mandatory but not yet implemented;
diagrams/media/reminders/widgets, wall drag, full list/text parity, storage compaction
and all-device qualification stay OPEN. Do not mark M0 ML feasibility or full plan done.

### Slice 7 delivered — 0.5.0 (6), 22 expanded CI jobs

CI **40578488274** fully passed and published `MyNotes-v0.5.0-6.apk` (1,220,453 bytes),
source `957299626c3207ef559260058222c601dc3b7591`, publication `894fd9b`. SHA-256 verified
locally: `e61839edd88d7941c71e89ddb26cccdc9b1ef293cf079ead572f98f59d744451`.
51 JVM / 63 unique Android / **441 Android executions in 13 lanes**, 468 local checks,
independent lint and release-inventory jobs, all signature/update/installed-byte gates.

Delivered editable six-shape flowcharts with labeled directed arrows, immutable schema 3
(reads 1/2), backup/copy preservation and no-loss Merge refusal; native paginated vector
PDF with local readback verification; incoming plain-text sharing; global label rename/
combine/remove and additive bulk labels; full-page scrolling editor and responsive board.

Bring-up compile failure identified PdfDocument not implementing AutoCloseable on SDK34;
fixed explicit close. Independent lint exposed URI grant-mask narrowing, fixed with the
already-validated explicit read/write constants. Expanded large-font/landscape lanes
found real viewport collapse during multi-select; reduced chrome and hid navigation while
selected instead of skipping tests. Final matrix all green, no publication of failed builds.

Required OCR/automatic offline transcription, recording/media, reminders/widgets and full
physical-provider/scale/accessibility qualification remain open at this checkpoint. Model
provenance work is separate: speech acquisition run40578093243 inspected the official
Apache-2 English model without publishing an app. Current next slice must qualify actual
native inference and preserve inputs on failure, not merely claim model acquisition works.
