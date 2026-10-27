# MyNotes 0.5.0 (6) — diagrams, PDF and core Notes guide

**Published:** [CI 40578488274](https://github.com/tarungittarun/TicTic/actions/runs/40578488274)
passed every required job and published the audited bytes: **51 JVM tests**, **63 unique
Android tests / 441 executions across 13 lanes**, lint, source checks, immutable inventory
and signature/update gates. **468 local checks** pass. Checksum verified after fetching.
This is a delivered milestone, not the full 100-feature product or all-device certification.

## Install / data compatibility

- [MyNotes 0.5.0 (6) APK](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/mynotes/releases/MyNotes-v0.5.0-6.apk)
- [SHA-256](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/mynotes/releases/MyNotes-v0.5.0-6.apk.sha256)

Install over your existing MyNotes release. **Do not uninstall or clear app data.**
Package/key remain fixed; code increases to 6. Data now writes schema 3 and reads old
schemas 1 and 2; no downgrade to an old reader. Export a verified backup before destructive
experiments. Existing folder libraries and old backups remain readable.

## New in 0.5

1. Open a note → **Diagram**. Add process, decision, start/end, input/output, ellipse
   and text-box shapes. Select a shape by tapping/dragging or the shape list. Try edit,
   color, directional movement, resize and delete/Undo. Connect to another shape and
   tap an arrow entry to label/remove it. Move endpoints and reopen to check retention.
2. **Export PDF** → local Internal storage or SD destination. Reopen the PDF in your
   reader; text/checklists paginate and a diagram occupies a vector page. Editable
   originals remain in Notes. PDF is not a round-trip backup; use JSON backup for that.
3. From another app, Share **plain text** to MyNotes. A new local note is created, no
   URL is fetched, and existing notes remain. Other incoming media is not implemented.
4. Home → **Filters** reveals view/sort/label controls; **Manage labels** supports global
   rename/combine/remove including Archive/Trash. In bulk label editing, check **Add to
   existing labels** to avoid replacing each selected note's individual tags.
5. Repeat selection and editing in landscape, night and 200% system font. Home controls
   collapse during selection to preserve the board viewport. Long tool rows scroll;
   editor content scrolls as a complete page so the writing area does not collapse.

Native PDF rendering/reopening, diagrams/codec/backup, and these resource variants were
exercised in emulators. Arrow text dialogs and every physical touch/picker path still
need review on your phone. Flowcharts are a fixed logical canvas, not auto-layout/freehand.
See [scope and acceptance cases](DIAGRAMS-AND-EXPORT-v0.5.md).

## Core regression journeys

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
   Home → Filters chips filter by label, and search sees labels/list text. New notes on a label
   page inherit it. Global rename/remove is now available; # autocomplete remains pending.
5. Colour: check all 12 swatches on cards in light/dark, plus title/body/chip contrast.
   Repeat on small screens and large font. Automated style/model tests are not a
   substitute for all palette/device accessibility checks.
6. Multi-select: long-press one card, tap others, then pin/archive/Trash, set labels/
   colour or share. Bulk labels REPLACE by default; the additive checkbox preserves prior tags. Cancel leaves
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
10. Export/import a mix of old text, rich text and checklists. Check schema-3 round-trip,
    metadata, unchanged duplicate skipping and deterministic conflict copies, including
    re-import of old 0.3.0 backups. Portable backup envelope remains bounded to 8 MiB.
11. Regression: folder choose/open/move/retry, backup preview/cancel/checkpoint, pins,
    archive, seven-day Trash, search focus and native selection. Actual SAF/SD faults
    remain for device qualification; injected protocol tests do not certify providers.

## Automated scope

Passed current gates: 468 local checks (78 Notes + 390 legacy), 51 JVM and
441 Android executions / 63 unique cases. The matrix includes API 26/29/34, storage/UI/
diagram/share-label aspects, 200% actual font resources, night, landscape and a Pixel C
profile. Signed release install-over/hash/launch evidence is separate from debug-host
instrumentation. Controller journeys dispatch through real views with isolated fixtures;
share host callbacks are faked, not every external share app. The expanded lanes found
and fixed an actual board viewport collapse in landscape/large-font multi-selection and
an existing URI-permission lint finding. No checks were weakened to turn those green.

## Still open

Wall/list drag gestures, markdown/list customization, hashtag autocomplete, final
snackbars and full undo/redo, media/OCR/ASR/reminders/widgets, startup corruption
recovery UI, folder compaction/index scaling, full physical-device/large-font/TalkBack
acceptance and eventual MySpace integration. No feature completion is inferred merely
from source contracts or the existence of a new control.
