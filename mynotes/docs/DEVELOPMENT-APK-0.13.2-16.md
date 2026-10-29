# MyNotes 0.13.2 / code16 — installable development APK

## Download

[Download MyNotes-v0.13.2-16-development.apk](https://github.com/tarungittarun/TicTic/actions/runs/40962976726/artifacts/12937000778)

**This artifact is the APK file itself, not a ZIP.** The successful workflow uses upload-artifact v7.0.2 with `archive: false` on exactly one file. GitHub sign-in is required. Artifact expiry: **2026-11-05 22:06:03 UTC** (6 November 03:36:03 IST). This is temporary development distribution, not a public GitHub Release or Play publication.

- Filename: `MyNotes-v0.13.2-16-development.apk`
- Source: `9ed1ca4c5474d8505d6390ba45b8d616254fe4b0`
- Workflow: [40962976726](https://github.com/tarungittarun/TicTic/actions/runs/40962976726), successful
- Package: `app.space.notes`; versionName `0.13.2`; versionCode `16`; minimum Android API26
- Raw artifact size reported by GitHub: **66,605,116 bytes**
- GitHub-reported raw artifact SHA256: `878f7aaf89915cf321c34bf6c5420ef9b2efb83c61764c93b816ee42803ed603`
- Established signing-certificate SHA256: `08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`

The build explicitly ran apksigner verification and required that certificate digest before uploading the raw APK. It also produced a file SHA256/signature receipt in `refinement-foundation-lint`. The workspace could not independently download the earlier ZIP artifact because its storage host returned EOF; do not claim a locally installed copy here. The raw APK's availability is verified through the successful upload and GitHub artifact metadata.

## Installation

Back up important notes through the app first. Install the APK as an update; **do not uninstall or clear app data**. The package and signing identity are preserved and code16 increases the prior delivered code15. Android may require permission to install from the browser/file manager. If the download has expired, do not substitute an unrelated APK or uninstall to bypass an update error.

## What changed

- Imported images now have their own Edit image action: crop corners/presets, rotate left/right, mirror, bounded Undo/Redo, reset, explicit Save and guarded discard.
- Same attachment identity is preserved; image pixels process off-main; stale originals and complete-library backup limits are checked before replacement.
- Rotation restores compact image geometry/history against the exact original-content fingerprint, rather than storing bitmaps in activity state.
- Checklist rows have directly targeted actions and Add item below; unavailable moves/indentation are disabled. Invalid/no-op moves no longer erase an existing Undo checkpoint.
- OCR review actions no longer baseline-shift outside their row on API26. Blank OCR review actions and grouping prerequisites include the prior source fixes.

## Qualification evidence

| Source/run | Profile | Result |
|---|---|---|
| `9ed1ca4` / 40962976726 | API26 portrait, full selected inventory | **190/190** |
| same | API34, 2.0 font, dark portrait, focused inventory | **67/67** |
| same | API34 tablet portrait, focused inventory | **67/67** |
| `e895b04` / 40961354160, before shared action-row alignment fix | API34 landscape, focused inventory | **67/67** |

The latest source passed build, JVM tests, debug/release lint and signing verification. Exact method inventories reject missing, duplicate and skipped outcomes. The previous Pin failure passed in the new runs. Native image pixel mapping, preview Save/Cancel/recreation/history, checklist targeting, grouping, OCR and restore cases now executed rather than being merely authored.

Latest results and bounded synthetic captures: `design-2026-10-29/native/40962976726/`. Crop-preview, checklist-item-action and OCR captures were visually inspected for this handoff. This is not a claim that every screenshot/state was inspected. Latest-source landscape has **not** been rerun after the shared action-row fix; the landscape pass above belongs to the explicitly stated preceding source.

## What is not complete

- Comprehensive app-level typing Undo/Redo and remaining minute-level feature audit.
- Motorola physical-device acceptance: native selection/rotation, clipboard interoperability, notification delivery, real storage-provider recovery and OCR/voice accuracy.
- All supported profiles/permissions/host integration and full publication matrix.
- Durable image-preview recovery after process crash (explicitly saved image data remains separate).
- A generative local AI engine: none added; see LOCAL-AI-PRIVACY-ASSESSMENT.md. Existing OCR/ASR remain local.
- A 360-minute uninterrupted autonomous session. No such claim is made.

**13 of the 15 newly authorized jobs were used; 2 remain. No public release was made.** This APK is a tested development candidate, not a declaration that all app refinement is finished.
