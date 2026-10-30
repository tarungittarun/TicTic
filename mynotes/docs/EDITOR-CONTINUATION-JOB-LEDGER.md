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
