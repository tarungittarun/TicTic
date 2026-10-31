# MyNotes 0.19.0 (22) — Notes and Doc

## Candidate identity

Source: `fda80d5fed5a3b32a0d2c1c5dfdb371bd42b7055`.
[Qualification run41164036746](https://github.com/tarungittarun/TicTic/actions/runs/41164036746).
**All four jobs PASS.** Exact-source native reports, complete provenance, independent PDF
checks and screenshots are preserved in `design-2026-10-29/native/41164036746/`.
This is automated development qualification, not physical-device/novice acceptance.

[Raw signed development APK — MyNotes-v0.19.0-22-development.apk](https://github.com/tarungittarun/TicTic/actions/runs/41164036746/artifacts/13028450916)

- Raw APK, not a ZIP; GitHub sign-in may be required. Expires2026-11-07T03:23:42Z.
- 66,689,488 bytes; GitHub artifact SHA-256:
  `78e0d23ccd4d992f8d34e5ecf81d40d204696f9a9cba77ae577aa38c7991828f`.
- `app.space.notes`; version0.19.0/code22; min26/target34/compile35.
- CI verifies retained signing certificate SHA-256:
  `08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`.
- Hash above is GitHub's raw artifact digest. No separately downloaded sandbox copy is claimed;
  the previous candidate's sandbox download redirect returned EOF.

**Export a backup, then install over the existing app. Never uninstall or clear data.**
Keep important voice recordings separately. Do not downgrade after saving newer rich metadata.
This is a development APK, not a new public Release or Play publication.

## Delivered implementation

**Same APK, separate workspaces.** Notes always creates familiar simple notes/checklists,
even after using Doc or having enabled the former experimental global preference.
Existing rich documents appear in Doc without rewriting, flattening or deletion.
Explicit **Copy to Doc** makes an independent rich copy while retaining the original.

Doc includes the agreed rich work:
- Scoped Aa, headings/fonts/sizes, visual samples and selected-text/new-typing state.
- Adaptive writing-first controls, Undo/Redo, direct Labels/Colour and one + insertion menu.
- Named mixed checklists, selected-only conversion, left grips, + List item, contextual checked visibility.
- Editable2×2 tables, header role, rich cells, selected-cell row/column edits, horizontal cue,
  padded visible grid and uniform full-height wrapped rows.
- Actual image/drawing/vector-diagram previews and contextual Edit/Rename/Move/Remove.
- Whole-content movement with boundary preview/edge scroll, labelled non-drag destinations,
  cancellation and single Undo; no flattened replacement.
- Word hyperlinks with display-text entry, Edit/Open/Remove, safe HTTP/HTTPS and browser confirmation.
- Natural writing below visuals without repeatedly creating empty sections; empty-page recovery.

Retained: bounded restart-safe history, local folders/search/backups, labels, reminders,
existing capture/local ML and permission/opt-in diagnostics boundaries, PDF links and optional
writing lines. Calculator is unchanged. No cloud service was added.

[User guide](NOTES-AND-DOC-USER-GUIDE.md) ·
[Approved acceptance/research](EDITOR-USABILITY-RESEARCH-AND-REDESIGN.md) ·
[Failures and job ledger](NOTES-DOC-PLAN-AND-JOB-LEDGER.md).

## Exact-source validation

| Gate | Result |
|---|---|
| Repository Python |571 PASS |
| Standalone Notes Python |181 PASS (subset, not additive) |
| Android debug/release build, JVM and lint |PASS;232 JVM |
| API26 full native inventory |252/252 PASS |
| API34 dark200% font |121/121 PASS |
| API34 landscape |121/121 PASS |
| Independent PDF parser / complete provenance |all3 PDF checks PASS; no missing profiles |
| Final-source screenshots |Reviewed Notes home, dark200% header/wrapped table and landscape writer above IME |

Previous checkpoint6 passed252/252 API26,121/121 dark and all3 PDFs, but landscape was
120/121: IME appeared after the navigation helper's hidden-header check and hid Doc Back.
That source was not called fully qualified. This reservation added actual focused-editor
IME/layout readiness before navigating from each new editor. It retains real native DOWN/UP,
full unclipped stable targets, data assertions and the post-settling caret-line check.
No test is ignored and no control is forced visible. Production code is unchanged from checkpoint6.

The original24-job allowance was exhausted. User explicitly approved **10 more**; this run
reserves/uses **4/10**, leaving **6 unreserved**. No further spending is automatic.

## Still requiring acceptance / honest limits

Automated passing is not proof that the experience is excellent for a novice. Still needed:
- Physical Motorola edge50 neo/Android16 install-over-existing-data, restart and OEM/IME behavior.
- Novice walkthrough: create simple Notes/checklist; find an old rich Doc; Copy to Doc without
  changing the original; format selected words; name/reorder a mixed checklist; edit a header
  table; insert/edit/move a visual; resume writing below it; Undo, reopen, restore and inspect PDF.
- Real native selection/reflow, reminder/notification delivery, file providers, capture/ML accuracy,
  low storage/power-loss, performance and lifecycle checks. None is fabricated as device-tested.

Backups omit original recordings, private empty folders, edit history and saved styles.
Private writes and external folder mirrors are not one cross-filesystem transaction.
Doc is a workspace, not arbitrary Word/.docx import. Unrelated proposals such as audio-inclusive
backups, arbitrary attachments, richer recurring reminders, Play publication, MySpace embedding,
Archive's deferred work and Wall dragging are not silently declared done. No billing or
measured-hours claim is made.

### Final visual review

The actual large-font/dark table fixture shows distinct header emphasis, clear cell borders,
text inset from borders and equal row boundaries around wrapped text. The actual landscape
writer fixture shows its full writing line above the keyboard with the adaptive tools still
reachable. Notes home presents only the simple seed, its familiar dock/New note and separate
Doc tab. These are synthetic emulator fixtures, not a novice trial or the user's phone.
