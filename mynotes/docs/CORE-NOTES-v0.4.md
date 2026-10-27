# MyNotes 0.4.0 (5) — core note workflows

Implementation scope; observed qualification is recorded separately in M0-STATUS.md.

- Home: virtualised two-column masonry or list, Pinned/Others sections, per-note colour,
  timestamp, label chips, created/edited sorting and page-local instant search of title,
  text/list contents and labels. View/sort preference persists. Wall manual drag is not
  shipped; it remains last as agreed.
- Checklists: typed rows with checkboxes, completion, add/remove, focused-row up/down,
  0–3 indentation levels, hide/show checked items and text conversion. Row drag gestures,
  custom bullet glyphs and advanced list themes remain pending.
- Rich text: select a body range then Bold/Italic/Underline/Strike/Heading 1/Heading 2/
  Clear. Native editor caret/selection remains. No sticky typing toggle or markdown
  shortcut claim. Heading controls apply selected-range size styling, not a block editor.
- Labels: edit comma-separated labels per note or replace labels on selected notes;
  normalized case-insensitive duplicates, maximum 50 labels/note and 64 characters/label.
  Home chips filter by label. Global label rename/delete manager and # autocomplete remain
  pending. Bulk replacement is described before Save; Cancel makes no change.
- Colours: app-styled 12-swatch picker supports single/bulk changes in both themes.
- Selection: long-press card then tap more cards; pin, archive/unarchive, Trash/restore,
  labels, colour, share, merge, Done. Trash editing is refused until restore.
- Share: explicit text-only system chooser, checklist state rendered as readable prefixes;
  current Android message cap 100k characters, larger content uses backup instead.
- Duplicate: separate note ID/creation time with original content and properties retained.
- Merge: selected visible order, titles/body concatenate into new plain text; sources move
  to Trash with one-level Undo. If merged text is subsequently edited, Undo may restore
  sources without deleting the edited result. Content is not silently discarded.
- Undo: one discrete operation, not a full stack. Text typing invalidates content-undo
  rather than erase later input. Metadata Undo preserves newly typed body text. Final
  snackbar UX and full document undo/redo remain pending.

This is not the complete PDF product. No media, diagrams, offline OCR/ASR, reminders,
widgets or MySpace integration is claimed. Existing folder/backup functionality remains;
schema-1 reading and schema-2 backup/restore are covered by added tests.
