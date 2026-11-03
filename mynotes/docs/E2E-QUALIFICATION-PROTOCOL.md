# Executed E2E report protocol

This is the reproducible execution/evidence protocol, not the old usage guide and not a physical-device acceptance claim.

## Candidate and independent gates

Candidate **0.57.0 / code57** is Android source `38401d2c6c18334c3dcd20baea845930c6344a6b`, built only in Actions run `41591079488`. Actual source-bound results: **419 distinct native methods, 1,420/1,420 repeated profile outcomes**, and **317/317 JVM tests** independently recovered from the successful exact-execution gate. Root Python QA is a separate count, never added to these outcomes.

The final report gate requires **all** of:

1. Eight exact native shards, complete method inventory, no failed or missing selected outcomes.
2. Source-matching install-over results from codes33,35,40,52,55; retained Note/Doc content, unchanged first-install time, no uninstall/clear.
3. The signed APK's real Android SAF flows for PDF/Word/CSV, actual stopped background-process death, changed PID, restored query/page/CSV anchor and unchanged original bytes.
4. Optional diagnostic export through Android's actual create-document picker, completed bounded file readback, and every expected reader/picker/diagnostic capture. Log contents are deliberately not retained.
5. Independent raw artifact byte count/SHA256, stable signer, package/version/SDK/no-INTERNET manifest checks and executed JVM report.

A native-only green run cannot produce a fully qualified PDF while the signed-release system workflow or its screenshots are missing. Failed historical runs are retained, never relabelled or substituted.

## Matrix and workflow map

API26 and API36 each execute all419 native methods in disjoint215/204 shards. API34 landscape and API34 font2/night each execute the focused291-method subset in151/140 shards. Therefore419×2+291×2=1,420 outcomes; this is **not** every method on every device/profile.

The PDF links each discovered method to its exact source line, actions, assertions, selected-profile outcomes and available captures. Workflow groups cover:

- Home Notes/Doc, new/open, search/keyboard/filter/view, labels/pin/order, folder positioning and Back.
- Notes and rich Doc text/format/numbering/caret/selection, hyperlinks, checklist/table/block operations, Undo/Redo, save/reopen.
- Images/drawings/diagram shapes, zoom/context menus, group operations, selected-only OCR review and explicit append.
- Voice recording/inbox/transports/seek/progress/error/data-preservation workflows; no claim of physical speech/acoustic accuracy.
- Library, Viewed originals versus explicit copies, large paginated TXT, Word prepared pages, PDF text/selection/page controls, CSV/sheet parsing and viewers.
- Folders, pending recovery/review, save/create/move, backups/restore, Trash/Undo/retention, reminders and settings/diagnostics/storage.
- Real signed-release Android picker, process-death/relaunch and diagnostic-file-save phases, separately identified from native debug-host assertions.

The source-derived literal title inventory exposes unpictured/unassociated UI surfaces; a title declaration alone is never marked traversed. Dynamic titles/vendor/provider states are not an exhaustive finite inventory. Named screenshot scenes are workflow steps, not a claim of150 visually unique application pages. The PDF shows one representative per scene; all retained profile JPEG variants remain in the evidence set.

## Reproduction

After successful workflows preserve their actual results and bounded JPEGs:

```sh
python -m mynotes.tools.build_e2e_report \
  --run 41591079488 \
  --source 38401d2c6c18334c3dcd20baea845930c6344a6b \
  --preserved mynotes/docs/design-2026-10-29/native/41591079488 \
  --output mynotes/docs/e2e/41591079488
```

Python generation needs ReportLab and Pillow; PDF inspection additionally uses PyMuPDF/pypdf. No Gradle/Android build is run locally. Test fixtures are public synthetic fresh-emulator data only. Never use a personal library/log/image/audio upload as an Actions fixture.

## Acceptance boundary

Physical20-card top-edge drag precision, the supplied image's OCR accuracy/font/script errors, real microphone/speech/playback and arbitrary complex Word fidelity remain open. TXT Find is page-local and Word rendering approximate. A qualified automation bundle is a development artifact, not a public release, blanket migration guarantee or physical sign-off.

The code40→57 emulator assertions passed in `41591928821`; only its later Git evidence push collided. Recovery job `41592942199` independently validated and preserved the already-uploaded report, without repeating or altering its assertions. The original failed retention status remains historical evidence.
