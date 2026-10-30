# Approved document-editor upgrade — data and security first

Approval: user explicitly approved all 13 requested items and the stated safeguards. Additional Actions authorization: **15 new jobs + 2 unused = 17 available**. This is a job cap, not money. No jobs started under this approval yet.

First explicitly retained clock checkpoint for this approval: **2026-10-30T04:51:41Z**. Repository restoration and initial inspection preceded that checkpoint; their duration is not invented. No 180-minute completion claim. Requested work is tracked across turns, not falsely represented as an uninterrupted background worker.

## Safety boundaries

- Keep original note IDs, media pixels, timestamps, folder references, reminders and formatting unless the user explicitly edits them.
- No cloud processing, network permissions, model downloads or content diagnostics. The supplied diagnostic file contains device context; do not commit the raw attachment or identifying diagnostic payloads to Git.
- Preserve legacy decoding and fail closed on unknown future formats. No opportunistic schema rewrite merely to add a UI button.
- Before a format upgrade: decode/validate fully, retain a verified prior recovery generation, validate complete-library backup capacity, and only then write atomically. Reject unsupported features explicitly instead of silently flattening them.
- An editable display name must never be used as an image/drawing type discriminator. Current source infers Drawing from the literal name; that must be removed before arbitrary renaming is enabled.
- Android provider URIs are not ordinary filesystem paths. Show an honest location and permission state, not a fabricated path; no broad-storage permission or automatic external-file overwrite.
- External opening/drop is untrusted input: granted local content only, bounded reads, compatible format checks, explicit review, no automatic hyperlink execution.

## Architecture audit

Current Note enforces mutually exclusive checklist rows versus text/spans and stores images and a single diagram separately after the text. Merely rearranging controls cannot provide Word-like mixed content. A real ordered document representation and migration/round-trip/search/export/merge/recovery tests are needed. Do not claim mixed editing is complete before those paths are integrated.

First incremental implementation: session text Undo/Redo without changing stored schema; directly draggable checklist row handles with accessible position selection; remove redundant add/move commands from checklist and document menus. Keep current data format unchanged while the mixed-document migration is designed and tested.

## Requested-item ledger

1. Mixed movable text/visual content: pending document representation and full persistence integration.
2. Table and hyperlink: pending editing, safe target validation and round-trip/export behavior.
3. Editable numbered visual names: pending separation of type from display name, including legacy drawings.
4. Writing lines: pending document/view-style decision; do not silently lose it in backup.
5. Text font/style/size: existing basic styles are insufficient; pending ranged typography and persistence.
6. Text Undo/Redo: first implementation batch.
7. Selected-text checklist sections inside text: pending ordered-document migration.
8. Checklist drag handles/removal of redundant add/move commands: first implementation batch.
9. Document-only bottom overflow: first cleanup batch; remaining content tools need contextual placement.
10. Numbering: pending paragraph representation and editing.
11. Saved location in document menu: pending honest provider-aware location surface.
12. File-manager drop/Open with: pending bounded local document intake.
13. Open notes/folders and simple storage settings: pending UX and provider recovery review.

APK handoff will list done/partial/pending individually and exact source/test/run/job counts. Current delivered code16 remains unchanged until a new candidate actually builds. No public release is authorized without qualification.

## First source batch / qualification reservation

Source now implements per-field session typing Undo/Redo for body, title and checklist rows, including app formatting snapshots, IME reset, bounded memory and redo invalidation. The existing document-action Undo remains distinct through its tooltip/state. Histories are deliberately in memory; navigation/recreation can reset them. This is not yet comprehensive persistent document history.

Checklist rows have local-only drag handles, insertion-edge reordering, stale/foreign-state rejection and edge scrolling. Accessibility click on a handle offers numbered positions. Item menus retain indent/outdent/remove only. The left Checklist options sheet holds checked visibility and conversion; bottom document overflow no longer holds checklist item editing.

Stored schema remains 6, with **no migration or rewrite introduced** in this batch. New code17/version0.14.0 is only a candidate until compilation and native tests pass.

Reserve **5 of 17** jobs for attempt 1: build/JVM/lint/signing, full API26, dark/2.0-font API34, landscape API34, evidence. Up to 12 remain after that attempt. Existing code16 APK remains the last delivered artifact until an actual replacement is shared.

Local checks and native tests are separate; drag behavior, IME composition, styles, selection and persistence still need execution. No new permission, cloud engine or raw diagnostic attachment is included.
