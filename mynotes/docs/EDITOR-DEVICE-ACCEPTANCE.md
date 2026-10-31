# MyNotes 0.21 — phone and novice acceptance

Status: **not run**. Automated qualification is recorded in
[the handoff](EDITOR-COHERENCE-DEVELOPMENT-HANDOFF.md); it does not pass this checklist.
Target phone: Motorola edge50neo / Android16. Record actual device/OS if different.
No new Actions jobs are authorized by this checklist.

## Before starting — protect originals
- Export a backup and verify the exported file exists outside app-private storage.
- Separately copy important original recordings. Note backups do not include their audio files,
  empty private folders, edit history or saved styles.
- Install code24 over the existing app; never uninstall, clear data or downgrade for this test.
- Use disposable notes/documents for edits, deletion and permission experiments. Do not test power
  loss, corrupt backups, storage removal or restore-over-existing on the only copy of real data.
- Record app version, phone/OS, keyboard, font/display size, theme and orientation. Do not send
  private notes, recordings or an unredacted full backup when reporting a problem.

## Round 1 — first-time understanding (about 10 minutes)
Ask a person who has not seen the guide to attempt these tasks. Do not explain which control to
use first. Record first control tried, success without help, hesitation and their own description
of the control. Time is observational, not a forced pass threshold.

| Task | Expected outcome | Result / hesitation |
|---|---|---|
| Create a simple note and add a checklist | Notes remains the quick workspace | Not run |
| Find a word and return to writing | Same page; query/arrows/Close understood | Not run |
| Bold selected words in Doc | Existing selection changes, not the whole document | Not run |
| Set bold for new typing, then turn it off | Later text follows the selected typing style | Not run |
| Insert an image and rename it | Selected image caption changes, other content does not | Not run |
| Change document colour and labels | Whole-document scope is understood | Not run |
| Create a folder, then move the document | Creation and choosing the destination are distinct | Not run |
| Close formatting and continue writing | Writing space returns without leaving the document | Not run |

Repeated confusion is a UX failure even when the task can eventually be completed.
Do not coach the participant and then record the task as an unassisted pass.

## Round 2 — owner checks on disposable content
For each row record Pass / Fail / Not run, with a short reason. Any unexpected data loss blocks
acceptance. If a failure occurs, preserve the original and stop repeating risky operations.

| Check | Expected outcome | Status |
|---|---|---|
| Open an existing rich document | Text, formatting and attachments remain intact | Not run |
| Copy to Doc | Separate editable copy; original unchanged | Not run |
| Find while keyboard is open, then navigate matches | Controls usable; exact word revealed | Not run |
| Edit writing while Find is open, then close Find | Count updates; caret stays while typing; no saved search formatting | Not run |
| Open an image/content menu while Find is active; press Back | Topmost surface closes predictably; no unexpected editor exit | Not run |
| Cancel edited Labels, Colour and Move trays | No unintended saved change; same writer retained | Not run |
| Apply a property change and Undo | Previous property restored without losing writing | Not run |
| Move a disposable rich document and reopen it | Correct folder; exact content/formatting/attachments | Not run |
| Convert a formatted checklist to text, then Undo | Generated boxes removed; text/order/formatting retained; Undo restores checks | Not run |
| Normal portrait, landscape and 200% text | Reachable controls; no clipped required action or hidden caret | Not run |
| Combined landscape and 200% text | Scroll/reflow remains usable; no keyboard trap | Not run |
| TalkBack navigation | Named controls, clear state/scope, sensible focus and close behaviour | Not run |
| Deny microphone, then use text editing | Clear denial handling; writing still works | Not run |
| Record, pause/resume, save and reopen disposable audio | Recording/progress understandable; original audio playable | Not run |
| Cancel a file-provider picker | Current document and storage destination unchanged | Not run |
| Schedule a near-future disposable reminder | Actual notification arrives under recorded OEM/battery settings | Not run |
| Background/reopen app, then ordinary restart after Saved | Disposable content and attachments retained | Not run |
| Trash and restore a disposable note | Restored content/audio intact; other notes unaffected | Not run |

Do not use a forced power cut as an ordinary restart test. Power-loss and destructive storage
recovery require a separate isolated dataset/device and remain pending here. Restore-over-existing
and revoked-provider experiments likewise remain separate from this non-destructive walkthrough.

## Report and decision
For a failure send: task, exact taps, expected/actual outcome, reproducibility, device/OS, app version,
keyboard, font size/orientation/theme, and a redacted screenshot or short screen recording if useful.
Diagnostics are opt-in; do not publish private data or enable collection merely to complete this sheet.

Decision: **Pending physical and novice results.** Do not infer approval from an empty report.
Fix confirmed data-loss/access failures first, then repeated navigation/scope confusion, then polish.
Any changed APK requires fresh, explicitly budgeted Android qualification before delivery. Keep the
qualified code24 APK as the reference; do not silently replace it with an unqualified local change.
