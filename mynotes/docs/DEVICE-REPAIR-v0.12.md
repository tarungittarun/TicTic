# 0.12 / code13 — real-device repair, not yet qualified or published

## Inputs and diagnosis

User feedback supersedes the emulator approval of 0.11. Read both `MyNotes-diagnostics (1).txt` and `(2).txt` and the provided screenshot. The attached PDF is zero bytes, not a readable document.

- PDF export failed in `LocalBackupFiles.requireLocal` before PDF output. Image import reused that backup-only authority check, refusing local Gallery/MediaStore URIs. Image and backup policies are now separate: local external storage and Downloads for documents; additionally local media/document and the system local photo-picker provider for images. Arbitrary/cloud providers remain refused, including cloud photo-picker routes. No network/storage-wide permission added.
- Repeated successful switches to **Use device storage** disconnected the selected visible folder; the old copy remained but browsing required another main-folder selection. This action now explains app-private storage and requires confirmation. Folders exposes direct main-folder selection, the current storage status and actionable failures. Creation/open/import failures now retain privacy-filtered stacks.
- Root portable-note files were only written after a note had been assigned a subfolder. All current notes now materialize in the main folder too. Existing SHA-named per-note files migrate copy/verify-first, then verified same-ID obsolete copies are removed. Never remove foreign files.
- Portable files and new recovery generations use readable UTC timestamps rather than random public names; per-note same-millisecond collisions receive numeric suffixes and do not overwrite other IDs. Existing generation IDs/parents remain readable.
- No 90-day policy was found: existing automatic Trash retention is seven days. Regardless, waiting is no longer necessary: Trash exposes Empty Trash and selected-note Delete permanently, both with confirmation. Recording deletion guards are durable first; original audio and existing backups remain separate. Verified portable-file cleanup uses a per-destination durable retry journal. This is not forensic secure erasure of old snapshots.
- Home Library gestures now accept starts through the left quarter, including 20% in from the left, avoiding dependence on the OS-owned edge. Native text-editor selection is not intercepted.

## Drawing and appearance

- Pen, pencil, translucent highlighter, eraser, line, rectangle and oval; seven ink colours and five widths. Width/colour/tool are captured per stroke, so old strokes do not change when choosing a new tool.
- Smoothed historical touch samples, Undo/Redo, confirmed Clear with Undo, actual pinch zoom/two-finger pan, Fit paper, and a larger focus workspace. Export always uses the complete paper, not its zoomed viewport. Interrupted/cancelled strokes do not become saved ink.
- Saved drawings reopen for editing, replacing the same attachment rather than adding another copy. New/edit drafts are distinct, survive recreation, and are removed only after a matching durable save. Concurrent attachment changes are not silently overwritten.
- Sixteen existing background choices now have visibly coloured light/dark papers, blended with the note colour; Plain keeps the selected note colour.
- Settings uses grouped, rounded icon cards, full-row actions and explanations. Storage distinguishes visible files from app-only storage; About keeps technical model/licence material under a disclosure instead of dumping it on first entry. Diagnostics remains opt-in and user-exported.

## Verification in progress

Root509 / Notes119 checks pass; these are not Android execution. Android discovery currently155 methods / JVM114 before subsequent test additions. New native coverage includes pixel/tool histories, real MediaStore import and source deletion (API29+; API26's real stream path is SAF), timestamp collisions and owned cleanup, 20%-in swipe, Trash cancellation/selective/whole deletion, retained original audio and no transcript resurrection, and editable-drawing attachment identity. The latest additional tests are not yet executed: do not infer success from discovery.

First repair run40767520041 failed instrumentation compilation due to a missing test import; fixed. Run40768491595 was superseded/cancelled and is not a qualified run. Current qualification40768949671 targets1a44495; later local changes and added tests still need their own complete run. Publication approval for0.11 does not cover changed runtime inputs.

Workflow ceiling is45 expanded jobs (previously30). Current topology remains30 jobs /21 Android lanes; inspect actual lane execution durations and split if a lane reaches20 minutes, without dropping method/profile coverage. Keep working while CI runs. Actions remains the only Android build path; calculator/MySpace code is untouched.

## Remaining acceptance boundaries

All required technical gates, complete new native execution, actual five-profile screenshot inspection, updated overview PDF/README and immutable signed publication are still required. No physical Motorola/SD-provider/microphone/acoustic/TalkBack/ARM16KiB or future MySpace certification is implied. Broader audio-inclusive backup and reminder/widget functionality are not magically completed by these repairs; preserve honest scope and address device defects first.
