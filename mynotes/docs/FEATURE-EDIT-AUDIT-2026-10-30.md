# Basic-edit and interaction audit

Source inspection, 30 October 2026. **Not device acceptance.** “Implemented” below means source exists, not that an APK passed native qualification.

| Feature | Finding | Work/status |
|---|---|---|
| Imported image | Remove existed, but no actual image editor | Added a distinct Edit image action and preview-first crop, clockwise/anticlockwise rotation, mirror, Undo/Redo, reset and explicit Save. Same attachment ID; no duplication. |
| Image cancellation | A removal-only card could not provide safe edit cancellation | Back/Cancel now keeps the original; dirty previews require discard confirmation. Repeated Back after cancelling the sheet retains the guard. Saving blocks navigation. |
| Image precision/accessibility | Drag-only crop would exclude users who cannot drag precisely | Crop corners have generous hit targets; full-image/square/4:3/16:9 presets provide non-drag alternatives. Arbitrary numeric crop and zoom/pan are not implemented. |
| Image recreation | Full bitmap state would be too large for activity Bundle | Small validated geometry and bounded Undo/Redo histories only; original decoded again off-main. Source fingerprint rejects a preview for changed content. Bitmap ownership is separate for preview and export. Process-crash durable image-preview recovery is not implemented; only explicit Save changes the note. |
| Checklist actions | Move/indent/remove lived in general note overflow and depended on a previously focused item | Added actions on each non-trashed item, including Add item below. The item is explicitly targeted. Unavailable moves/indent levels are disabled in this sheet. |
| Checklist Undo | Invalid/no-op move could replace an existing Undo checkpoint | Moved validation before captureUndo; boundary moves and indentation no-ops do not create an edit. Existing note-level menu remains for compatibility, so hierarchy still needs native review. |
| Text/title/list typing | Native editing/selection exists, but app-level typing history is not a multi-step Undo/Redo implementation | Pending. Do not advertise the current single note-action Undo as comprehensive typing history. Preserve IME composition, native selection, spans, heading reflow and selection restoration when implementing. |
| Drawing | Edit, ink tools, preview history, Save, copy image, grid, clear/delete exist | Source inspected; previous draft/save/rotation work remains Android-unqualified. Need actual small-screen, large-font, IME and orientation walkthroughs. |
| Diagram | Shape text editing, direct corner resize, history, connectors and group prerequisites exist | Group/OCR prerequisite fixes from the prior batch remain unqualified. Connector editing, group/undo and selection edges need native review. |
| Reminder | Existing reminder is directly editable; clear button exists | Source inspected, not missing an edit action. Device permission/delivery/force-stop acceptance still pending. No need to invent a replacement scheduler. |
| OCR | Recognition review is editable; empty-result action availability improved earlier | Source exists; actual recognition quality and rotated/large-font/IME states pending. No generative correction engine added. |
| Voice | Original audio retained; transcript reviewed through notes | Local inference/model accuracy, microphone denial, pause/resume and background lifecycle still need physical acceptance. |
| Labels | Rename/remove sheet exists | Source inspected. Blank-name validation exists; duplicate/long-name/keyboard/accessibility paths still need device review. |
| Folders/storage | Real storage roots and move operations exist | Still require provider cancellation, unavailable root, interrupted writes and recovery verification. User-selected providers may sync outside the device. |
| Trash | Restore, Undo, timed retention and manual purge exist | No visual edit buttons are added to trashed cards. Purge and backup/provider boundaries remain acceptance gates. |

## Next native checks for this batch

1. Open imported image from a note with keyboard showing: keyboard must dismiss; crop workspace remains usable.
2. Drag every crop corner; cancel a gesture; add a second finger; rotate/mirror after crop; Undo/Redo and reset. Verify exact exported pixel orientation and clipping.
3. Leave unchanged; edit and Back; Cancel discard, then Back again; discard; reopen. Original must not change until Save.
4. Rotate the activity during load, after several edits, with Redo available and during save. Verify geometry/history/source identity and no duplicate attachment.
5. Force complete-library backup-size rejection, stale original, low-memory decode and storage failure. Keep original image and recoverable note Undo; never claim Save succeeded on failure.
6. First/middle/last checklist item: actions target that row without focusing it first; boundary actions disabled; Add item below inserts in place; invalid actions do not destroy Undo.
7. Font scales 1.0/1.3/2.0, narrow portrait, landscape, light/dark, TalkBack and gesture navigation. Confirm readable action labels, 48dp minimum icon targets and no empty sheets.

A JVM geometry test class and three native tests (raster pixels, image editing/recreation and checklist targeting) have been authored. Neither has been executed in Android/Gradle in this environment. Local Python source guards are separate evidence only.
