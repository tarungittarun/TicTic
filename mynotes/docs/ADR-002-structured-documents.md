# ADR-002 — structured Notes schema 2

Date: 2026-10-28. Status: implemented; release qualification recorded in M0-STATUS.md.

## Decision

Schema 2 stores text marks as typed UTF-16 ranges, checklists as stable-ID rows with
text/checked/depth, per-note hide-checked state, and normalized labels. Note metadata
and 12-colour indices retain their existing meaning. Lists and rich text are distinct
content types; list content is not hidden in an unstructured plain-text preference.

Readers accept schema 1 and 2. A schema-1 text note migrates in memory without loss of
ID, text or metadata; new commits write schema 2. Unknown schema versions fail closed.
Old folder generations remain; the existing generation envelope validates either schema.
Portable backup envelope remains version 1 but its payload is versioned independently.
Older MyNotes cannot read schema 2 and must never be used as a downgrade/migration tool.
Android's versionCode gate prevents normal in-place downgrade.

Native Android editor spans move with edits; supported ranges (bold/italic/underline/
strike/heading sizes) serialize explicitly, not as an approximate HTML conversion.
Formatting affects selected text. A clear operation splits existing ranges to preserve
unselected formatting. This milestone does not implement sticky typing-style toggles,
markdown triggers, arbitrary fonts or full editor undo/redo history.

Rows retain IDs while checked, edited, moved or indented. Conversion to text writes
readable check prefixes and indentation; conversion back parses them into new row IDs.
Converting rich text to checklist intentionally flattens its style ranges; a discrete
Undo preserves the prior document until later typing invalidates that content-undo.

Restore equality and deterministic conflict-copy IDs include all new fields. Plain
schema-1 conflict IDs keep the exact 0.3.0 derivation to avoid duplicate copies when an
old backup is re-imported after upgrade. Duplicate/merge operations preserve originals
or move sources to Trash; merge creates plain text and can restore source states.

## Storage/performance consequences

No media payloads are claimed. Portable backup still has its explicit 8 MiB safety
bound. Folder history compaction, FTS indexing and 10,000-note/device performance gates
remain open. RecyclerView virtualises Home but is not proof of storage/search scale.
Rotation keeps a bounded <=32 KiB draft in Bundle; larger drafts rely on lifecycle flush,
not a claim of crash-proof recovery in the short debounce window.
