# Scoped writing stage — plan, safety and runner ledger

## Stages

1. **Current implementation only:** Aa with visible controls and selected/new-typing scope; quiet text writing; named checklist boxes converted from selected text; whole-checklist movement into a writing-line position with native marker and tap-to-place alternative. Preserve existing row grips, + List item and other editor capabilities.
2. **Deferred:** broader unified + toolbar, table headers/contextual table redesign, new visual previews/editing flows and general object-movement redesign. Not implemented or claimed by this stage.
3. **Deferred:** whole-editor layout polish/usability acceptance of the wider proposal. No blanket redesign of earlier simple/legacy notes, Home, Folders, Calculator, Archive or Wall.

## Current interaction design

Within the existing opt-in document editor, normal text has no permanent Text heading/handle row. Native selected-text actions include Make checklist and Text section options; the latter can reveal the existing section handle when required. The existing Selection → checklist control remains, as do Insert, Labels, Colour, Writing lines, Document, visual/table editing and their native drag behaviour. No automatic upgrade of existing notes.

Aa opens a scrollable, labelled formatting panel. Bold/Italic/Underline, Strike, paragraph headings, font and size controls are visible rather than a single long list. Common formatting choices show on versus off/mixed and use the existing selection-preserving engine. A scope label explains Selected text or New typing. Existing hyperlink/numbering/preset operations remain under More text options; their functionality and safety remain.

Converting a nonempty selected range preserves all unselected text/marks and produces Checklist01-style defaults. A bordered box groups name, whole-box grip, rows and existing row actions. Tap its name or grip for Rename/Move checklist/Insert after/Remove checklist. Long-press the grip opens the same options; moving beyond touch slop starts native drag. Row grips remain independent. Names are bounded to200 UTF-16 units, nonblank on rename and validated against malformed UTF-16.

Drop the whole box into a writing field: the marker identifies the start of the visible line under the pointer (including soft-wrapped lines); below the field's final line means its end. A drop splits rich text there and moves the existing immutable box/rows, not copies. Screen-wrapped lines change with font/width; the committed destination is a text offset. A Move checklist option enters explicit tap-to-place mode with Cancel move, so no drag is required. Existing block-boundary drops still work. Back/navigation cancels by leaving the editor without an implicit move.

## Data safety

The existing schema7 block already stores name. This stage accepts named checklist blocks in that field, preserving empty names from older files. Older APKs reject nonempty checklist names rather than silently flattening them; do not downgrade. No schema6 note is automatically rewritten/upgraded. Every row edit/add/reorder/check/hide and note merge preserves the checklist name; fingerprint, library/backup codec and exported text/PDF retain it. Existing bounds, local-file restrictions, recovery snapshots and anchored restart history are unchanged.

Moving validates rich slices/UTF-16 boundaries and the complete resulting document before changing the note. Invalid offsets, stale IDs or content limits leave the original intact. Movement and rename are structural Undo steps. No external drag payload is trusted. No clipboard/note text appears in drag ClipData.

## Reservation1 — BEFORE launch

Four individual jobs: build (compilation, JVM inventory, debug/release lint, stable signing), full API26, dark2×API34 and landscapeAPI34. Existing35-job allowance:19 previously started; **23/35 reserved cumulative,12 unreserved**. This stage reserves4, not a fresh allowance. Billing is not inferred from a runner count.

Expected authored inventories:221 JVM,244 full native and114 focused. New tests exercise named-checklist codec/backup roundtrip, name-sensitive immutable model/moves, selected/new-typing panel behaviour, name/edit/move/Undo/Redo/cold-controller reopen and actual injected native checklist-to-text drag with visible marker. Existing native assertions remain; only Format→Aa and the explicitly reorganised More text options paths change. Source guards pass562 root/172 MyNotes (overlapping discovery). These are not Android runtime PASS claims.

Code21/version0.18.0 retains package/signer. Qualification pending. No final APK recommendation until native outcomes and screenshots are reviewed. Physical Motorola acceptance is separate.
