# Candidate71 expanded qualification — completed, not all green

- **Original full run:** [42277337133](https://github.com/tarungittarun/TicTic/actions/runs/42277337133), source `3c1478b8c8b09d077d97b5481603387cbc47b06a`: **2,671 passed / 2,676 native executions**, five failures; 446 distinct methods across six emulator profiles. Fresh JVM 366/366, debug/release builds and lint, manifest/signing/size audit and signed release69→71 public-rich install-over probes on API26/34 passed.
- **Completed recheck:** [42282354356](https://github.com/tarungittarun/TicTic/actions/runs/42282354356): API26 owning shard **218/218** after an instrumentation-only compatible IME observer correction. The unchanged spreadsheet query discrepancy did not reproduce; it is not marked fixed. All three unchanged API36 navigation / API34 large-text checks fail again in isolation.
- **No production fix or public release in this check phase.** Existing search/Format production fixes remain partially verified. No physical acceptance or complete paired Obsidian acceptance.

## Main results

- [Full qualification PDF](Full_Qualification.pdf)
- [Readable result / five-failure triage](Full_Qualification.md)
- [Stable FQ-0001–FQ-0005 observations, classifications and next tests](findings.json)
- [Completed recheck receipts and provisioning history](rechecks.json)
- [Actual original / rebuilt debug APK and certificate identities](recheck-apk-identity.json)
- [Original full-run summary](42277337133/summary.json), [run/jobs](42277337133/run.json), [retained-file SHA256 manifest](42277337133/retention.json)
- [Original rerun evidence](../ux-improvement-ledger/implementations/42282354356/retention.json)

The rerun uses an internally consistent app/test pair from the corrected-observer build. **It is not the exact original APK:** ephemeral debug certificates differ and three unsigned native Whisper library entries differ between builds. The public proof records this rather than claiming binary equivalence or pooling speech performance. Production source/model weights and the user release signing configuration were not changed.

Original failed attempts remain in Actions and retained records. Initial retention omitted 48 ignored nested build/JVM/lint files; `retention-initial.json` preserves that attempt. The corrected canonical manifest uses nonignored `jvm`, `lint` and `quality` paths; every listed retained file is present and hash-checked. Invalid capture-command output from pre-instrumentation attempts is retained as invalid bytes, not represented as screenshots. Some receipts have a byte-identical, independently reparsed alias with its own provenance record.

Screenshots are public synthetic fixtures, not private libraries or novice usability results. Capture dimensions vary by helper: some were resized to 540px before artifact creation; PDF/selection PixelCopy captures can be full-window. Retention preserves their original artifact bytes. Actual failed checks, changed observer validation, source-supported hypotheses and still-unknown physical/comparative acceptance remain separate.
