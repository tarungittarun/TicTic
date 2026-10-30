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

## Attempt 1 — run 41005605946

**5 jobs used, 12 available before the next reservation.** Source `1723f0b`: Android build/JVM/lint/signing passed. API26 191/192; landscape and dark/2.0-font each 68/69. Typing/body-formatting/checklist history tests passed. The sole failing case incorrectly searched the entire view tree for absence of Hide checked, finding the intentionally separate hidden Checklist options panel. Scope the assertion to the shown Note actions panel; do not suppress any missing action or reorder failure.

## Second source batch / attempt 2 reservation

- Add a dedicated Open note file picker, compatible content-URI Open with handler, and single-file external drop intake. Local provider allowlist, bounded decode and existing non-destructive review/merge/checkpoint behavior are reused. Source files are not edited in place. Temporary drag grants release after reading or on close; repeated releases are idempotent; concurrent preview/queue overflow is rejected safely.
- Add read-only File location in the document menu. Distinguish the private multi-note library from a verified provider document, show stale/missing copy state honestly, and never create a file merely to display its location. No location data enters diagnostics.
- Explain Open note file versus Open library versus Choose folder in storage settings. Further visual simplification remains pending.
- Use a private checklist drag MIME type rather than text/plain, preventing native EditText from treating an internal row move as text insertion. Keep local-only state and no global drag flags. Add a real input-dispatched drag regression, separate from accessible position reordering.

Reserve **5 more jobs**, maximum cumulative **10/17**, leaving **7**. Code17 is not shared until qualification results and its partial scope are recorded. The major mixed-document, tables, hyperlinks, names, lines, fonts and numbering work remains pending; no unsafe schema migration is introduced to imply progress.

## Attempt 2 — run 41006920985

Five more jobs completed: **10/17 used**. Build/JVM/lint/signing passed. API26 194/195; large-font/landscape 71/72 each. Menu scoping, new file-location display, cloud-file rejection/grant release and text history passed. Sole failure: the new drag test called the text-body-only `laidOut()` helper on a checklist (which has no body), timing out before any drag. Replace that precondition with measured checklist-handle/row/window/IME readiness; preserve the actual input injection and reorder assertions.

## Targeted probe — reservation 2 jobs

Use a build plus one API26 native job for exactly three named cases: actual handle drag, app-only folder setup, and ruled writing guides. This is explicitly a **probe**, not full qualification. Maximum cumulative **12/17**, leaving **5** for a final full three-profile qualification after any needed fix. Exact three-case inventory is still enforced; no skipped/duplicate outcomes or coordinate fallback.

The new folder setup screen treats app-only storage as a normal configuration, not a failed folder read. The attached diagnostics contained 563 parsed events, including five folder-open IOException events; their messages are intentionally absent, so their exact cause is not inferred. Raw attachment/device identifiers are not committed.

Ruled backgrounds now draw guide lines at actual native text baselines rather than a fixed grid behind images. The persisted existing background value 3 is reused; no schema migration. Select Background → Ruled. Other background rendering remains unchanged. PDF guide alignment and physical-device behavior are separate acceptance items.
