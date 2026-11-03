# Candidate52 — automated qualification completed

Date: 2026-11-03. MyNotes0.49.0 / versionCode52, development candidate;
not a public release or a claim of complete physical-device/product acceptance.

## Installable artifact identity

* App source: `7f4c0884b2090b5bf4ff59e8d8750093e0afe0bb`.
* Build/native run: https://github.com/tarungittarun/TicTic/actions/runs/41568986562
* Raw APK (not ZIP): https://github.com/tarungittarun/TicTic/actions/runs/41568986562/artifacts/13200841464
* Filename: `MyNotes-v0.49.0-52-development.apk`.
* Size:155153368 bytes.
* SHA256:`5a68f33fa8a8ba25b278b2dfa70e511272787953edf88543fcaa5f575e3995f2`.
* Package `app.space.notes`, min26, target34; no INTERNET permission; unchanged
  signing certificate SHA256 `08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`.
  Exact-byte upgrade checks independently verify these properties.

## Results

* Root:794 checks passed (latest gate rerun before documentation push).
* JVM:304-method inventory; build's exact executed JVM coverage gate passed.
* Native:405 distinct methods,1370/1370 profile outcomes, all eight jobs SUCCESS.

| Native profile | Primary | Companion |
|---|---:|---:|
| API26 portrait |208/208|197/197|
| API34 landscape |145/145|135/135|
| API34 font2/night |145/145|135/135|
| API36 portrait/motion |208/208|197/197|

These counts were independently checked from all eight committed `native-results.json`
files under `design-2026-10-29/native/41568986562`; no missing, unexpected, duplicate
or failed outcomes. No assertion timeout increase or skipped assertion was used.

## Fixed and exercised blockers

* Header Back now reaches the existing safe caller navigation while storage is
  queued. Mutating actions retain their busy guard; writes continue serially.
  Late completion generation fences prevent reopening the abandoned backup page.
* Notes↔Doc creation retains the original repeated content/IME/checklist assertions.
  Initial new-document keyboard handoff waits for modern IME/resize settling;
  accepted requests are not reissued against a later hide. Rendering/detachment
  cancels stale requests. Settling is one-time new-document session intent, not
  note content or an extra delay on ordinary inserted fields.
* Home dragging accepts a card-centre drop without overshoot; a target latch avoids
  swapping the same card twice before StaggeredGrid layout settles. Real injected
  hold/drag, exact persisted order/sort and unchanged-document assertions pass.
* Existing direct PDF hold/range-drag/floating accessibility Copy/clipboard checks,
  Word scaled-image and authored-height checks, and exact116-page handbook checks
  also passed in this complete run.

## Exact release-byte checks

*33→52,35→52 and40→52 install-over: SUCCESS.
  https://github.com/tarungittarun/TicTic/actions/runs/41569737017
* Actual stopped-background-process kill and retained-task relaunch for public PDF,
  DOCX and CSV fixtures: SUCCESS; query/page2 or CSV row41 restored; original bytes
  unchanged; observed PID changes3974→4900→5590→6106 on API34.
  https://github.com/tarungittarun/TicTic/actions/runs/41569737007
* Reports preserved under `design-2026-10-29/updates/41569737017` and
  `design-2026-10-29/process-recovery/41569737007` include exact APK identity.
  Upgrade fixtures cover two public ASCII Note/Doc records, not an exhaustive
  real rich library. No uninstall, clear-data or force-stop substitutes for the
  actual process-kill probe were used.

## Remaining boundaries

Install over the existing app; do not uninstall or clear app data. Keep verified
backup/source copies and original recordings separately (recordings are excluded
from JSON backup). No downgrade recommendation.

Full arbitrary Word fidelity, universal emoji/missing-text OCR, globally measured
smoothness, comprehensive process-restored navigation-stack behavior, and physical
acceptance remain open. The116-page guide remains a working edition with explicitly
source-pinned synthetic captures, not a physical-device or editable-Figma claim.
Its unchanged PDF and AndroidTest copy both hash to
`a70c1f2ae863c1c990e8ec387312e62a34c42d47eec050935b8dbc2271573755`.
No360–480minute elapsed-work completion claim is made.
