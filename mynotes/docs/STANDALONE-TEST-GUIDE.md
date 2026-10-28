# MyNotes — install, use and device checks

## Release status

Published **0.12.0 (13)**: [APK](https://github.com/tarungittarun/TicTic/releases/download/mynotes-v0.12.0-13/MyNotes-v0.12.0-13.apk) · [checksum](../releases/MyNotes-v0.12.0-13.apk.sha256) · [receipt](../releases/MyNotes-v0.12.0-13.apk.release.json).

This repairs the folder/media/drawing/Settings issues reported on 0.11. [Qualification40779382571](https://github.com/tarungittarun/TicTic/actions/runs/40779382571) passed all30 jobs /21 Android lanes,114 JVM cases and1278 Android executions (157 unique methods). Fresh five-profile rendered review is complete. **Physical-device retesting is still required; green emulator gates do not supersede user feedback.** [Repair evidence/status](DEVICE-REPAIR-v0.12.md).

Install updates **over the existing app**. Do not uninstall, clear data or downgrade. Package `app.space.notes` and certificate `08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca` remain stable. Current schema6 reads1–6. Keep an exported backup and separate copies of important recordings before destructive experiments.

## Your files and folders

1. Open Library → Folders. If there is no main folder, choose **Choose main folder**. In Android's local file picker, create/select a dedicated folder such as **Documents/MyNotes**, then allow access. Android restricts some root/system directories; choose a child folder, not Android/data or a cloud provider.
2. After verification, return to Folders. Create a folder, open it and create a note. Or long-press an existing card → Selection actions → Move to folder.
3. Open Android Files at the main folder you chose. The actual subfolder and `.mynote.json` files should exist there. New note filenames use UTC timestamps, with numeric suffixes for collisions. Recovery generations and image blobs are app-managed data; do not edit their contents or delete them to tidy the folder.
4. Close/reopen MyNotes and confirm placement. Moves verify the new copy before removing a verified old copy. Revoked/removed storage keeps private recovery and requires Retry save after reconnection.

**Use device storage** in Settings means **app-only private storage**, not a publicly visible Files folder. Its confirmation explains that it disconnects visible folder browsing. Existing external files are retained. Select a main folder again for visible folders. Old random-ID recovery snapshots remain readable; older released files are not destructively rewritten just to change their names.

## Library, filters and Trash

- Tap Library or swipe right starting **about 20% in from the left**, away from Android's back-gesture strip. Only Home/library lists use this gesture; native note-text selection is not intercepted.
- Filters has swipeable **View / Sort / Labels**. Labels supports all, none, one or any combination. Choosing none intentionally produces an empty result, not a data-loss event.
- Long-press a note card for selection actions. Copy/Trash are list actions; native selected-text copy remains in the editor.
- In Trash, use **Empty Trash**, or select notes → Selection actions → **Delete permanently**. Confirm only when certain. Cancel must retain everything; live notes must remain untouched. Ordinary automatic retention is seven days, not a required wait before deletion.
- Permanent deletion removes current-library notes and verified managed portable copies. It cannot erase previous backups, original voice recordings, or all old recovery snapshots. This is not forensic secure erasure. Restore before editing a trashed note.

## Images, drawings and coloured paper

- Note → **+ / Add to note → Image**. Select a local Gallery, MediaStore, Downloads or Internal storage/SD image. Cloud routes are refused. Pixels are copied, oriented and stripped of source metadata; moving/deleting the original afterwards must not break the note.
- Detailed pictures reduce quality/resolution automatically to fit. Inputs are capped at 20 MiB, normalized images at 1600 px, and notes at eight images. The complete JSON library backup still has an 8 MiB cap; the app must refuse an addition that would break a verified backup, retaining the original and note. These limits are not an unlimited photo archive.
- **Drawing** offers pen, pencil, translucent highlighter, eraser, line, rectangle and oval. Choose ink and width, pinch to zoom, use two fingers to pan, Fit paper to reset, and Focus drawing for more room. Undo/Redo and confirmed Clear affect the current session. Save exports the full paper, not just the zoomed viewport.
- **Edit drawing** reopens an attachment and replaces that attachment on Save. Back keeps a separate draft; a failed save must not discard it. Prior-session strokes are flattened pixels, not a persistent vector/layer history.
- Choose **Background** from note actions for sixteen coloured papers. Note colour remains separate. Check both Light and Dark; Plain uses the note colour.

## Writing, diagrams and reminders

- Native text handles select words precisely. H1/H2 reflow their paragraphs; they must not overlap the following line. Inline bold/italic/underline use selected text. Manual Select All is still available.
- Diagram has twenty named shapes plus text, Edit shape, connections, basic grouping and Undo/Redo. Drag to position. Nested groups/layers and smart guides are not claimed.
- **+ → Reminder** opens date/time selection. Tap its separate reminder chip to edit, X to clear. Notification permission is contextual. Delivery is inexact and can be delayed by Android/Doze/OEM restrictions; force-stop suppresses alarms until reopening. Recurrence/snooze are not yet delivered.
- The re-placeable home widget projects pinned/recent titles; it is not a complete interactive checklist widget.

## Voice, backups and PDF

- Voice inbox has separate transcript cards, play/pause, progress/seeking and export/retry. Microphone denial must leave other features usable. Recognition is offline; punctuation is conservative, not perfect semantic restoration. Review against audio.
- Original recordings remain independently in Voice inbox, including after transcript-note deletion. Export important recordings separately: **JSON backups and selected-folder note generations do not contain original audio**.
- Settings → Storage & recovery → Backups offers verified JSON export and non-destructive import preview/merge. Images/drawings are included. Existing conflicting notes are copied rather than overwritten. Backups are readable, not encrypted.
- Open/select notes → Export PDF, then choose a local destination. Success requires readback verification. A cancelled/failed export is not a complete file; keep your source. A previous zero-byte PDF cannot be repaired into a document without exporting again.

## Appearance and useful reports

Settings groups appearance, storage, widget and support actions. Choose System/Light/Dark in the standalone app. Future embedding can follow host design/settings, but actual MySpace integration is not certified.

To report a problem: enable **Settings → Diagnostics → Collect diagnostic logs**, reproduce, export, then stop/delete collection if desired. Review the text before sharing. Note text, audio, images, clipboard and URI paths are excluded; OS-wide ANRs/kills can leave gaps. Include the exact steps, expected/actual result, whether storage was selected, and a screenshot that does not expose private content.

Before calling the release accepted on a real phone, exercise folder creation/move/reopen, Gallery import/source deletion, drawing/edit/recreation, Trash cancel/selective/all deletion, 20%-in swipe, light/dark/large font, native selection/headings, voice with actual speech, reminders and widget placement/re-placement. Emulator totals do not certify Motorola/SD providers, TalkBack, acoustics, thermal behavior, ARM16KiB or future MySpace migration.

Historical outcomes: [0.10 interaction repair](INTERACTION-REPAIR-v0.10.md), [0.11 organisation/media](ORGANISATION-AND-MEDIA-v0.11.md), [remaining product acceptance](NEXT-ACCEPTANCE.md).
