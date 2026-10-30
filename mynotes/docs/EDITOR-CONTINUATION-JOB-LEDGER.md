# Remaining editor implementation — new job allowance

2026-10-30: user approved **up to 25 additional GitHub Actions jobs**, asking to optimize elapsed time, job count and runner use. This is a fresh allocation after the earlier 17/17 were exhausted, not approval for an unlimited workflow or a monetary estimate.

## Efficiency and release boundaries

- Deliberate workflow-path triggers only; intermediate source/docs pushes use `[skip ci]`.
- One SDK/model-free JVM job first for isolated domain foundations; do not run emulators for inactive model primitives.
- Full runtime qualification still builds APKs once and runs independent native profiles in parallel. Sequentially packing profiles into one job would reduce job count but increase elapsed time.
- Native runners download only debug/instrumentation APKs, not the unused approximately 66MB release APK per runner. The signed raw release APK remains separately uploaded and verified.
- Fresh per-job AVDs do not save unused snapshots at shutdown. No switch to paid/larger runners, no removed correctness checks, and no timing improvement claimed before measurement.
- No changes to Calculator/archive work and no public release. Existing schema6 code17 remains the last qualified APK until the complete persistence/UI path is implemented and qualified.

## Reservations and actual results

1. **Reserve 1/25**: `notes-editor-foundation.yml`, full discovered JVM/domain suite and exact executed-inventory verification. Tests cover immutable tables, ordered content draft, legacy read-only projection, visual kind/name separation, Unicode-safe rich-text selection, plus every pre-existing domain rule. Result pending until run completion. This is not Android compilation or completed editor functionality.

Remaining after this reservation: **24 jobs**. Record run ID and outcome before reserving more.

### Reservation 1 result

Run [41014957791](https://github.com/tarungittarun/TicTic/actions/runs/41014957791), source `925c96b`: **1/1 job passed**, 2026-10-30 06:35:24–06:36:28 UTC (**64 seconds job duration**, not an actual-work tally). All **180 discovered JVM methods** passed the exact inventory gate, including 18 new table/ordered-content/rich-text tests; no skipped/duplicate/missing outcomes accepted. This compiled and executed the new Java primitives, unlike the earlier Python source-only check. **1/25 used; 24 remain.** No Android/APK job was launched.

The full runtime workflow transfer/snapshot optimizations are prepared but have not been benchmarked. The measured 64 seconds is for the new isolated domain gate only; do not compare it with a full emulator qualification as if equivalent coverage were obtained.

Next gates remain canonical schema/codec and mutation integration, rollback/round-trip/search/export/merge/recovery coverage, followed by actual mixed-block editing and native qualification. DocumentContent/RichText/TextTable are inactive and not a second editable source of truth. The qualified code17 APK and all 13 statuses in its handoff remain unchanged.

## Reservation 2 — connected editor and persistence

Reserve **2 jobs**, cumulative at most **3/25**: shared build/JVM/lint/signing and full API26 native suite. Canonical schema7 is now wired through Note mutations, duplicate/archive remapping, fingerprints/conflicts, portable/backup codecs, search/plain-text and ordered PDF. Legacy-only libraries still encode6. New UI is reached by the deliberate Document editor upgrade, whose worker verifies canonical round-trip and a retained first-upgrade private checkpoint before saving. Text projection mutation throws rather than flattening. Additional inline styles and rich checklist rows are schema-aware. No new APK is handed off at this development gate. Full three-profile qualification and native coverage of every new feature still required.

### Connected gate result and reservation 3

Run41016768825, source28ee1b7: build failed at a missing `DocumentContent` import in NotesController; all **191 JVM tests passed**, native job was skipped. Charge the full two-job reservation conservatively (**3/25**, including that unstarted native job). No APK delivered. Correct the import and continue, not suppress compilation/native failures.

Reserve **2 further jobs** for the updated shared build and full API26 suite, maximum **5/25**, leaving20. This source additionally preserves formatted table cells, supports actual caret-position block insertion including asynchronous images/drawings, has an independent persisted writing-lines switch, and rejects external URI/Intent coercion inside editable fields. All remain subject to compilation/native qualification.

### Reservation 3 result / reservation 4

Run41017331436, source50e811d: domain compilation/tests passed; Android compilation caught the checked exception declared by the existing digest helper in two new main-thread insertion-anchor call sites. Native was skipped. Charge the full reservation conservatively: **5/25 including two skipped natives**. Handle the digest failure explicitly without abandoning the verified insertion contract.

Reserve **2 more jobs**, cumulative maximum **7/25**, for corrected build + full API26. Remaining after reservation18. No APK handed off and no tests reclassified as passing.

### Reservation 4 result / reservation 5

Run41017566800, source6e30707: runtime/app compilation proceeded; instrumentation compilation failed on a duplicate `dialogTap(String)` test helper. Domain tests passed; native was skipped. Remove the duplicate and reuse the existing native dialog-action helper. A local signature guard now catches this specific class of avoidable compiler failure before another job. Charge the complete reservation conservatively: **7/25, including three skipped native jobs; four runner jobs actually started so far** (foundation plus three failed builds).

Reserve **2 further jobs**, maximum **9/25**, leaving16 unreserved: shared build and full API26 native qualification. This checkpoint includes canonical-preserving merge, numbered Enter/newline deletion and heading continuation, reverse selection restoration, keyboard viewport adaptation, independent guides, read-only Undo disablement, proper style/newline undo boundaries, bounded cursor metadata, stricter schema7 fields/types/formatting limits, readable hyperlink projection, and complete-line/column-panel PDF table pagination. Six bounded synthetic document scenes supplement, not replace, the previous scene whitelist and assertions. Additional native tests exercise those connected behaviors, including real injected block dragging and separate diagram edits. Compilation, tests, and screenshots remain pending; no code18 APK is declared qualified.

Local pre-push checks: root552 and MyNotes162 Python tests pass; changed Java files parse as Java8 using a temporary external parser. That parser is **not** a Java type-check, Android compilation, runtime result, or a repository dependency. Full three-profile qualification remains required after API26 behavior is established.

### Reservation 5 result / reservation 6

Run41019484710, sourcef947ba3: **shared build/JVM/lint/signing passed**; API26 native gate correctly failed, **209/212 passed, three failed**, no duplicate/unexpected outcomes. Two new tests incorrectly waited for the legacy writing-field hint instead of canonical field identities (keyboard and block drag); replace that helper, not its geometry or persistence assertions. Reverse selection also failed and requires preserving the captured anchor/focus across dialog focus transitions. Successful new native cases include selected checklist conversion/history/recreation, table cell/structure history, fonts/custom size/hyperlinks, numbered Enter/deletion/heading behavior, separate diagrams/rename, schema7 round-trips/rejection, and multi-page Unicode rich-table PDF rendering. No qualified APK handed off.

Conservative charge **9/25** (six actual runner jobs plus three previously skipped native jobs). Reserve **2** for corrected shared build and full API26 suite, maximum **11/25**, leaving14 unreserved. Native tests now additionally exercise actual table/image Insert menu caret ordering, inserted cell focus, physical handle tap/context plus rich-row native dragging. Source also uses window-aware SP conversion, guards strict UTF8 and surrogate preservation, preflights private JSON before recursive parsing, avoids no-op text history, and cleans only untouched transcript-pending placeholders. Full parallel three-profile gate and handoff remain pending.


### Reservation 6 result / final three-profile qualification reservation

Run41021022787 / source74ec97d: shared build/JVM/lint/signing passed; native **212/215 passed**, three failures, no missing execution beyond the failed methods, duplicates or unexpected methods. The keyboard viewport and actual native section drag now pass. Image fixture incorrectly used a deliberately forbidden file:// URI; it now requests a real local SAF grant, writes a synthetic PNG, verifies the exact UUID-only fixture root before cleanup, and never weakens production URI denial. Rich-row drag fixture incorrectly constructed formatted rows in legacy content; it now constructs canonical rich rows directly. Reverse selection failed after Colour. Android API26 Editor normalizes reverse ranges when resetting native selection controllers; writing fields now snapshot before native focus/window-focus transitions and restore afterwards, suppressing synthetic selection callbacks. That fix still needs native qualification; expected anchor/focus and injected drag assertions remain unchanged.

Conservative reservation consumed: **11/25**, including three previously skipped natives. Reserve **5 jobs** for the shared build, parallel full API26 / focused API34 dark2x / focused API34 landscape, and source-pinned evidence preservation: maximum **16/25**, leaving **9 unreserved**. No extra intermediate probe is scheduled.

This candidate also externalizes canonical IMAGE/DRAWING pixels into the existing immutable hash-verified media protocol (including cross-kind deduplication), hydrates them without changing schema/envelope versions, and retains earlier inline canonical format2 development generations. Native tests cover rich-table/link/checklist/order/media generations, corrupted blobs, older inline snapshots, explicit OOME recovery failure, and actual SAF folder moves. Invalid/incomplete generation records still fall back to verified earlier records; missing/corrupt hydrated media never becomes empty content. OOME is converted to an explicit recovery IOException in generation read/commit and private load; this is **not** a claim that total selected-folder history size or private-file I/O is bounded. No retention/deletion of user histories is introduced.

Canonical nested diagram fields/numbers and Unicode tags are validated before decoding; per-document diagram limits are1024 nodes/4096 edges with4096-character labels/256-character identities, included in the aggregate text budget. Unsupported nested diagram data must fail rather than disappear. Guide drawing traverses only clipped visible lines. Added JVM/native tests enforce these constraints. Local root552/MyNotes162 and Java8 syntax checks pass; Android/JVM type checking and all final profiles remain pending. No qualified code18 APK or physical-device acceptance is claimed.


### Three-profile candidate compile correction

Run41022310526/source8a5f5b1: domain and runtime/app main code compiled, domain tests ran; instrumentation compilation rejected one wrong-arity checklist factory in the corrected rich-row test fixture (factory takes id/items/hideChecked, not a display-name argument). Correct that call to the existing three-argument API. No emulator jobs ran. Conservatively charge all five reserved jobs despite four never starting: maximum **16/25**, nine runner jobs actually started and seven skipped/unused reservations to date. This was an avoidable fixture integration error; Java parsing did not type-check it. Do not reclassify the run as passing.

Reserve **5** for the same optimized full three-profile qualification plus source-pinned evidence job after this compile correction: maximum **21/25**, **4 unreserved**. No code18 APK handed off; new features and data paths remain gated by the native results.

The corrected fixture commit b242148 did not launch jobs because the workflow deliberately triggers only on its own file changes. This workflow checkpoint uses the same reserved five jobs, not a further reservation. Visible-guide bounds use Canvas clip coordinates directly (already scroll-adjusted).


### Three-profile results / four-job final reservation

Run41022621075/source6d1fdc3: build/JVM/lint/signing passed, API26 **219/220**, dark2x **85/86**, landscape **81/86**. All new media/SAF/inline compatibility/diagram/OOME tests pass; actual PNG insertion and rich-row handle tap/native drag pass API26 and dark2x. Exact reports and synthetic screenshots were preserved by successful evidence job (1d8243f), not reclassified as qualification success.

A diagnostic assertion proves reverse selection was already normalized synchronously on its first application setSelection, BEFORE opening Document/Colour. AOSP API26 Editor creates its selection controller lazily; its constructor resets touch/drag offsets and normalizes reverse spans. WritingField now retries the exact application-requested reverse range once if native initialization changed it, without replacing native touch handles or sorting expected anchor/focus. Existing focus/window snapshots remain.

Landscape also exposed fixture assumptions: short native dialogs require actual accessibility scrolling; two sections need not fit simultaneously before a drag. The dialog helper now scrolls the real list. The drag helper starts on a visible source, injects actual edge drag/autoscroll, then requires an actual visible lower-half target outside the moving edge zone (or a clamped end of document) before dropping. It never invokes model/onDragEvent directly. Keyboard test waits for a focused, visible writing line after IME layout, retaining original line-height/Format-control assertions and capturing its whitelisted scene even on failure. Production reveal translates the actual caret line to document-scroll coordinates; a single measured compact shrink can recover the remembered input only when no view has focus and the window is active, never stealing dialog/button focus.

Reserve **4 remaining conservatively counted jobs**, maximum **25/25**, for a shared build and all three unchanged profiles. Save the fifth evidence runner by letting API26's tail wait for both peer uploads, then run the SAME artifact download, source-pinned render/report preservation, root gates and documentation commit/push. Keep all three profiles even on failure and do not call failure a pass. No qualified code18 APK delivered. Seven earlier skipped/unstarted jobs remain conservatively charged; no attempt is made to consume that unused runner capacity.


### Exact job-ID reconciliation and corrected final four-job launch

Run41024271745/sourced7f6955 failed compilation because the new caret-line lambda reused the enclosing method's `line` local name. Rename it `caretLine`; this is not an Android API or behavior change. No emulator ran and no APK is qualified. This avoidable local-variable integration error is recorded rather than hidden by syntax-only checks.

The previous **25 reservation** count included matrix entries that GitHub NEVER CREATED after build failures. Reconcile against GitHub's actual `/actions/runs/<id>/jobs?filter=all` results, counting even skipped jobs with IDs:

| Run | Individual job IDs | Started | Skipped |
|---|---:|---:|---:|
|41014957791|1|1|0|
|41016768825|2|1|1|
|41017331436|2|1|1|
|41017566800|2|1|1|
|41019484710|2|2|0|
|41021022787|2|2|0|
|41022310526|3|1|2|
|41022621075|5|5|0|
|41024271745|2|1|1|
|**Total verified IDs**|**21**|**15**|**6**|

On failed builds a three-entry native matrix is one skipped placeholder job, NOT three individual jobs. Recover only the **four never-created reservation slots**, not the six skipped jobs that have IDs. Reserve **4 real jobs** (shared build + all three native profiles with integrated evidence tail): maximum **25/25 individual GitHub job IDs including skipped**, no further allowance assumed. Same inventories, source-pinned preservation and physical-device caveats remain. No extra approval is needed or requested within the existing25-job authorization.


### Final authorized run result and held one-job replay

Run41024603014/source8d225ec: **build/JVM204/lint/signing PASS; API26 220/220 PASS; API34 landscape86/86 PASS; API34 dark2x36/86 FAIL**. API26's integrated peer wait/download/render/root-gate/evidence commit succeeded (1c2619c), saving a fifth runner. Inspect the actual dark `control-failure.jpg` and environment-after JSON: the blocking foreground system dialog says **Pixel Launcher isn't responding**, and focused windows/ANR identify `com.google.android.apps.nexuslauncher`, not MyNotes. This remains a failed qualification gate; no exceptions/fixtures were weakened or failures erased. Current-source reverse selection, real native edge drag and focused landscape keyboard viewport pass in both successful lanes.

Verified final count: **25 individual job IDs,19 started,6 skipped**. No26th job has run. The raw code18 APK source8d225ec is a development handoff with the incomplete gate explicitly disclosed, not a public release/full qualification. Artifact12962923344,66,659,248 bytes,SHA256c555c5a630685844c0b3c2b18f4804a84eb762aa3d5c726486bfdb9470e527db, expires2026-11-06T08:11:36Z.

Prepare, but DO NOT execute, a single native dark2x replay that downloads existing build/test APKs from41024603014, requires zero non-doc MyNotes source differences from8d225ec, and invokes the existing Pixel Launcher force-stop after preserving preflight diagnostics. It keeps the exact86-test inventory and source-pinned reports/screenshots in that same runner. Original full three-profile/four-job workflow is retained as a nonexecuting template under docs/ci. This push uses **[skip ci]**; another job requires explicit cap extension. Reusable instrumentation artifacts expire after one day. Detailed done/pending/testing/security/installation handoff: EDITOR-V18-DEVELOPMENT-HANDOFF.md.
