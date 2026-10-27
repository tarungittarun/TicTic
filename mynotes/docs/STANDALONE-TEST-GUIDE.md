# MyNotes 0.4.0 (5) — core Notes testing guide

**Published:** CI run 40569713346 passed all jobs: 29 JVM tests, 40 Android tests,
release identity/version gate and install-over-0.1.0 smoke. 440 local checks pass;
published checksum verified. This is a substantial core milestone, not the completed
100-feature product or full physical-device qualification.

## Install / data compatibility

- [MyNotes 0.4.0 (5) APK](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/mynotes/releases/MyNotes-v0.4.0-5.apk)
- [SHA-256](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/mynotes/releases/MyNotes-v0.4.0-5.apk.sha256)

Install over your existing MyNotes release. **Do not uninstall or clear app data.**
Package/key remain fixed; code increases to 5. Data now writes schema 2 and reads old
schema 1; no downgrade to an old reader. Export a verified backup before destructive
experiments. Existing folder libraries and old backups remain readable.

## New core journeys

1. Home: make text notes with different lengths. Confirm masonry auto-height, Pinned
   and Others grouping, grid/list toggle and persistent created/edited sorting.
2. Rich text: type a body, select a range using Android selection, tap Bold, Italic,
   Underline, Strike or Heading 1/2. Reopen to check styles. Clear removes formatting
   only from the selection. Controls scroll horizontally. Sticky typing styles,
   markdown triggers and a full document undo stack are not in this milestone.
3. Checklist: create rows, edit text, tick/untick, tap a row then move up/down, indent/
   outdent (0–3), hide/show completed rows, remove/add. Convert to text and Undo. Verify
   the saved item text, order, completion and indent after reopening.
4. Labels: edit a comma-separated set on a note; duplicate spelling/case normalizes.
   Home chips filter by label, and search sees labels/list text. New notes on a label
   page inherit it. Global label rename/delete and # autocomplete are pending.
5. Colour: check all 12 swatches on cards in light/dark, plus title/body/chip contrast.
   Repeat on small screens and large font. Automated style/model tests are not a
   substitute for all palette/device accessibility checks.
6. Multi-select: long-press one card, tap others, then pin/archive/Trash, set labels/
   colour or share. Bulk labels REPLACE each selected note's label set; Cancel leaves
   notes unchanged. Done or Back exits selection. Restore trashed notes before editing.
7. Merge two notes: new plain-text note opens; source notes go to Trash. Undo restores
   source states and removes an untouched merged result. If you edit the result first,
   Undo may keep that edited result rather than discard typed content. Source rich
   formatting is flattened in the merged text; original documents remain in Trash.
8. Copy: creates a new ID/time, preserving content, list structure, formatting and tags.
   Share uses the explicit text chooser (100k-character message guard; backup for larger).
9. Rotation/background: test mid-word, styled selection and checklist rows. Bundle draft
   is bounded to 32 KiB; larger drafts depend on lifecycle flush. Forced-kill inside the
   autosave debounce is not a passed crash-proof guarantee.
10. Export/import a mix of old text, rich text and checklists. Check schema-2 round-trip,
    metadata, unchanged duplicate skipping and deterministic conflict copies, including
    re-import of old 0.3.0 backups. Portable backup envelope remains bounded to 8 MiB.
11. Regression: folder choose/open/move/retry, backup preview/cancel/checkpoint, pins,
    archive, seven-day Trash, search focus and native selection. Actual SAF/SD faults
    remain for device qualification; injected protocol tests do not certify providers.

## Automated scope

Passed current gates: 440 local checks (50 Notes + 390 legacy), 29 JVM tests and
40 Android tests. Android adds eight codec/span/migration cases and four controller
journeys covering rich properties/copy, lists, bulk actions and share/merge/Undo.
The controller journeys dispatch through real views in isolated fixtures; sharing uses
a fake host and is not proof every external share destination works.

## Still open

Wall/list drag gestures, markdown/list customization, global label manager, final
snackbars and full undo/redo, media/OCR/ASR/diagrams/reminders/widgets, startup corruption
recovery UI, folder compaction/index scaling, full physical-device/large-font/TalkBack
acceptance and eventual MySpace integration. No feature completion is inferred merely
from source contracts or the existence of a new control.
