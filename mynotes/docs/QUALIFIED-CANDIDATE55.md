# Candidate55 — automated qualification completed

2026-11-03. MyNotes **0.52.0 / versionCode55**, development candidate, not a
public release or complete physical-device acceptance.

## Exact installable artifact

- Runtime source: `3764c98027cd059939330b7dcff03db932548b8a`.
- Actions build/native run: https://github.com/tarungittarun/TicTic/actions/runs/41581350427
- Raw APK: https://github.com/tarungittarun/TicTic/actions/runs/41581350427/artifacts/13205947558
- Name: `MyNotes-v0.52.0-55-development.apk` (not a ZIP).
- Size: **155,157,844 bytes**.
- SHA256: `1875c0cc4e9c826a8733a565a5b652c1e89bf770ef46fc4676d696782cf6d7a6`.
- Stable package `app.space.notes`; minAPI26; target34; no Internet permission.
- Stable signer: `08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`.

Actions verified actual APK bytes, manifest and signer. Independent local checks
verified artifact metadata/source/digest/size against the recorded identity and
verified every saved native result, not only workflow success labels.

## Executed qualification

Build, debug/release lint, exact310 JVM gate and **all eight native jobs SUCCESS**.
414 distinct native methods;1,400 profile outcomes:

| Profile | Shard A | Shard B |
|---|---:|---:|
| API26 portrait |212/212|202/202|
| API34 landscape |148/148|138/138|
| API34 font2/night |148/148|138/138|
| API36 portrait/app motion |212/212|202/202|

Every saved report has `ok:true`, passed==expected, and no failures, missing,
unexpected or duplicate methods. Evidence:
`design-2026-10-29/native/41581350427/`.
Root source safety gate: **805 passed** before the final documentation push.

Includes new real-RecyclerView-layout drag completion, colourless drawing pixels,
private-save/provider-backoff/explicit-retry, reviewed conflict merge, JSON-null
head restoration, Archive/Trash Back, constant half-screen filters, larger
Unicode TXT through real SAF, and view-only versus explicit-copy journeys. Existing
repeated Notes/Doc IME transitions, actual Home drags, persistence, rich content,
offline inference and other original native assertions remain.

## Exact-byte upgrade and reader recovery

Install-over run https://github.com/tarungittarun/TicTic/actions/runs/41582107853 :
**33→55,35→55,40→55 and52→55 all SUCCESS**. Each report retained both its synthetic
Note and Doc, preserved first-install identity, and performed no uninstall or
clear-data. Each target identity was checked against the exact artifact above.
Evidence: `design-2026-10-29/updates/41582107853/`.
These are public ASCII fixtures, not an exhaustive copy of a physical rich library.

Reader recovery https://github.com/tarungittarun/TicTic/actions/runs/41582107768 :
**SUCCESS**, real API34 background-process kill and retained-task relaunch:
PDF/Word query+page2 restored; CSV query+row41 restored; original file hashes unchanged.
Observed PIDs4214→5247→5925→6438. No force-stop substituted for process death,
uninstall or clear-data. Evidence:
`design-2026-10-29/process-recovery/41582107768/`.

## Failures investigated, not waived

Candidate53 run41578780795:1,388/1,400. Real half-height filter clipping was repaired
by reclaiming decorative space while keeping native touch targets. Tests now
exercise inner scrolling required by the new half-screen specification and still
require full-row visibility and real far-edge input. The old TXT journey follows
the new default viewer/explicit-copy controls while retaining no-import, Find,
persistent edits, original-byte and Back assertions.

Candidate53 also had an actual API36 Notes IME handoff failure. Candidate55 waits
for the prior resize before requesting the new Notes keyboard, verifies served
visibility twice, recovers only unowned focus and never re-shows after acceptance.
All original repeated-transition assertions passed in55.

Candidate54 run41580503782:986/1,400 executed and passed; two shards had no
instrumentation execution/output (414 unexecuted methods). It was not qualified.
Candidate55 independently completed all eight shards. Neither earlier failure is
retroactively labelled successful.

## Safe use and remaining physical acceptance

Install over52; do not uninstall, clear app data or delete the existing folder.
The repeated old warning is now separated from a verified private save. A genuine
folder-head difference is **not** silently overwritten or made safe by hiding a toast.
Its original divergence cause on the physical device is not proven.

For a still-pending folder copy: Settings → Storage & recovery → More options &
repair → **Review selected folder difference**. Inspect the preview; confirm only
if the result is expected. Differing versions are preserved as copies, the previous
private state is checkpointed, and a head changed after preview still refuses commit.
Cancel leaves both libraries unchanged. Choosing a new empty folder or app-only
storage remains available; do not rely on a pending folder copy before uninstalling.

Native offscreen-selection behavior still needs physical smoothness confirmation.
No claim of universal emoji OCR, unseen-speaker speech accuracy, full arbitrary Word
fidelity, exhaustive migration or physical crash-free operation. The private user
attachments were not committed or uploaded. Existing illustrated guide remains its
labelled working edition, not evidence that all new UI changes are illustrated.
