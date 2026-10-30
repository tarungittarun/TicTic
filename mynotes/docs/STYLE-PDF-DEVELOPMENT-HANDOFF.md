# Code19 — styles/typing and linked, optionally guided PDF

**Completed for signed development testing:** both requested feature areas are implemented and the API26, API34 dark2× and API34 landscape automated checks passed. Physical-device/public-release approval remains pending.

## Implemented

- Named, text-free saved styles: selection application or caret-only subsequent typing; font/size/emphasis/heading support, composing input and controller-session restoration. Adjacent matching spans coalesce; typing-style changes use separate Undo groups.
- Up to16 device-local presets; names1–48 characters, case-insensitively unique. Serialized background writes report success after disk acknowledgement. Presets do not copy selected note words or link destinations and are separate from note backups.
- Actual HTTP(S) PDF URI annotations over supported wrapped, page-split, legacy and table-panel labels. No destination fetching during export.
- Separate PDF-options checkbox for printed writing guides, initially off in a fresh activity. Guides print only for documents with Writing lines enabled. Cancelling options leaves the note unchanged.

Instructions: [Style and PDF user guide](STYLE-PDF-USER-GUIDE.md).

## Development APK

- Version0.16.0, versionCode19; package`app.space.notes`, minAPI26, target34, compile35.
- Raw file **MyNotes-v0.16.0-19-development.apk**, not a ZIP: https://github.com/tarungittarun/TicTic/actions/runs/41049424165/artifacts/12974251600
- Size66,670,552 bytes; raw artifact SHA-256 from GitHub metadata:`c83fb1f443048b8145db825ebde43d6df1816848be72602fcac15d1972bdf4ec`.
- Stable signer SHA-256:`08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`; mandatory build signing verification passed.
- Artifact expiry2026-11-06 11:39:38UTC. GitHub repository access may be required.
- APK/build source`9a932cb155044645c74cf1efe3321f683b438de3`. Production sources are unchanged since`8a0ea8373ee20f56ae8cf432ddd49b8894e477c7`; verified by a zero diff of domain/runtime/app production trees and app/build.gradle through the final replay source. Subsequent changes are tests, workflow, documentation and synthetic evidence.

## Recorded checks

| Check | Actual outcome |
|---|---|
| Local root QA |552 PASS|
| Local MyNotes QA |162 PASS|
| Exact JVM inventory |212 methods; mandatory executed-coverage build gate PASS|
| Debug/release compilation and lint |PASS, latest build124041495922|
| Full API26 native |229/229 PASS, run41047748847/job124036877373/sourcecdb783a|
| API34 dark, 2× font |100/100 PASS, same run/job124036877431/sourcecdb783a|
| API34 landscape |100/100 PASS, replay41049424165/job124042456206/source9a932cb|
| Independent pypdf actual-PDF gate |PASS on all three original profiles and the landscape replay|

Preserved first three-profile evidence: [run41047748847](design-2026-10-29/native/41047748847/README.md). Reports retain the original landscape failure; it has not been deleted or waived. The replay changes only dialog-scroll synchronization, preserving its5-second bound, real item click and formatting assertions. Initial two-job probe passed228/228 native methods and its independent PDF gate but failed later evidence preservation. The same reporting-only failure recurred after the passing landscape replay and was diagnosed: a single downloaded artifact is flattened, while the script expected per-artifact subdirectories. Evidence-only recovery run41051111000/job124047101337 **PASS** verified the original source and mandatory native/PDF conclusions, recovered into an explicit named destination and preserved [the exact passing100/100 landscape reports and screenshots](design-2026-10-29/native/41049424165/README.md) in commit81b7359. Capture and preservation run IDs are recorded separately; original failed overall runs remain visible.

Local independent rereading of all three preserved real-PDF archives also passed: canonical fixture8 pages/168 link rectangles; legacy fixture1 page/1 encoded Kannada-URL link; guides-off and guides-on fixtures1 page/0 links each. Native PdfRenderer tests separately validate opening/rendering and guide raster differences. These are synthetic fixtures, not real-user documents.

## Budget, safety and remaining acceptance

New allowance35 additional individual Actions jobs:**9 started,26 unused**. See [exact ledger](STYLE-PDF-JOB-LEDGER.md). Estimate5–9 actual engineering/testing hours; this is not a claim of measured hours or background work.

Back up notes before updating. Install over the existing app with the same package/signer; **do not uninstall or clear data**. Existing note/checkpoint schemas are unchanged. Pending typing intent is session state, not persistent cross-restart Undo. Named presets are not included in note backups.

The PDF updater accepts only internally generated fresh Android PDFs, not arbitrary external PDF input. Safety caps:200 pages,8,192 rectangles and1MiB encoded URI data. A PDF viewer must support link actions; opening a destination is governed by that viewer/browser. Android producer compatibility beyond the qualified API26/API34 environments remains subject to physical-device verification.

**Physical Motorola edge50neo Android16 acceptance and public-release approval remain pending.** This is a signed development handoff, not Play/public release approval. Persistent cross-restart Undo, broader storage/history/backup limits and arbitrary Word/DOCX import remain outside these two feature areas. Calculator remains frozen; Archive paused; Wall dragging deferred.
