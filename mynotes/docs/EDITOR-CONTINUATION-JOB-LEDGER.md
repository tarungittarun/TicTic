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
