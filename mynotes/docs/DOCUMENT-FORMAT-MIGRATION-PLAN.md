# Mixed-document migration design — not implemented

This is the remaining architecture for requested items 1, 2, 3, 5, 7 and 10. It is **not shipped functionality**. The current code17 candidate deliberately still writes schema6.

## Canonical representation

Use an ordered sequence of stable-ID blocks: paragraph, checklist section, image reference, drawing reference, diagram, and table. Text must be allowed before, between and after every visual block. A visual's immutable kind must be separate from its editable display name. Tables need editable cells with bounded dimensions and predictable row/column insertion/removal; a static picture of a table is not a table editor.

Choose one authoritative content model. Do not write independent editable legacy body/items/images and blocks that can diverge. Search/card excerpts/plain-text sharing are projections, not a second editable copy. Retain media pixels by reference to the existing immutable attachment, not by copying base64 into every history snapshot.

Paragraphs carry ranged formatting, hyperlink destinations, numbering/checklist semantics and font/size choices. Fonts must be a bundled/system allowlist, not downloaded or file-loaded native code. Handle IME composition and native text selection; never replace selection with custom text dragging. Resolve selection to paragraph ranges before conversion; preserve unaffected text and formatting.

## Legacy migration

- Plain note: paragraph content with the same text, marks and line breaks, then existing media/diagram in their current order. Do not invent an insertion position for legacy attachments.
- Checklist note: checklist section preserving every item ID, checked state, depth, order and hide-checked behavior. Preserve title, labels, colour, background, reminder, folder, archive/Trash and voice references.
- Legacy drawing kind is inferred once from the historical literal name, then stored independently. Subsequent renaming must never change the editing route or reset ink.
- Preserve original display names on migration; numbered defaults apply to new content. Make generated names unique in their content kind without renaming the user's custom labels.
- Existing single diagram becomes a named diagram block with stable node/edge/group IDs. Multiple diagrams cannot share a mutable singleton or overwrite each other.

## Persistence gates before activation

1. Bump the document schema; older readers must refuse the future schema rather than silently discard blocks or unknown formatting.
2. Validate bounded JSON depth, total bytes, block count, table/cell sizes, names, ranges, IDs, references and hyperlinks before allocation-heavy work or mutation. Reject missing/duplicate media references; no orphan image deletion during failed import.
3. Read legacy data without rewriting it just because a screen opened. Migration writes only after a fully verified candidate and prior recovery checkpoint exist.
4. Encode/decode complete-library and portable-file candidates off-main; compare canonical content and media identities. Verify backup capacity before commit. Preserve a readable old-schema recovery generation and source files on failure.
5. Extend DocumentCodec, backup, generation protocol, NoteMerge, all Note mutations, hashing/fingerprints, duplicate/merge, Trash/Undo, PDF/sharing, search, widgets, selected-folder reconciliation and restore together. A new editor alone is not a safe migration.
6. Keep source-file imports read-only with explicit review. Decline cloud/web/file-scheme inputs and unsupported formats. Handle revoked grants, malicious nesting, oversized payloads and process death without replacing the private journal.

## Hyperlinks and actions

Treat destinations as untrusted data. Validate an explicit URL scheme allowlist; never execute javascript, intent, file, content or app-component links. Opening a web link is a deliberate external-browser action with disclosure, not an app upload or hidden network permission. Cancellation must leave text and selection unchanged. Table cells and imported text never become executable commands or automatic reminder/file operations.

## Editor and history gates

A block reorder must create one undoable transaction and preserve cursor/selection in unaffected blocks. Text Undo/Redo needs a document-level history design across block changes and style operations; the present bounded per-field session history is an incremental improvement, not the finished Word-like history. Rotation/process death must not duplicate blocks, lose a table draft, apply stale selections or overwrite a changed source.

Qualify migration fixtures (all legacy schema versions), corrupt/unknown formats, round trips, existing backup restore, conflicted folder writers, keyboard/large-font/landscape, native drag/selection, exports and actual Motorola provider flows before declaring the remaining features complete. No permissive legacy flattening or fake placeholder buttons are acceptable substitutes.

## Continuation checkpoint — 2026-10-30

The user requested completion of the pending items after receiving code17. Implementation resumed without launching any more CI jobs. First isolated primitive: `TextTable`, an immutable rectangular plain-text table draft supporting cell edits and row/column insertion/deletion, deep immutable snapshots, strict dimension/text limits, and rejection rather than truncation of invalid content. Six JUnit cases cover snapshots, structural edits, invalid input, limits and unchanged originals after rejection.

This primitive is **not connected to Note, persistence, imports, export or the Android UI**, and tables remain pending for the user. No schema is activated. The Python source suites pass (551 root/161 Notes), but this environment has no JDK; the new Java tests have **not been compiled or executed**. Android builds remain Actions-only. The previous 17-job allocation remains exhausted. Obtain a new explicit job allowance before any build/qualification workflow; use `[skip ci]` for intermediate pushes.
