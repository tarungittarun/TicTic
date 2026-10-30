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
