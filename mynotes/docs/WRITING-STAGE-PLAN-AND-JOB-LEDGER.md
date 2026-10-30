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

## Reservation1 outcome — source5363548/run41091891301

Build124187617748 PASS: compilation, exact executed JVM221, debug/release lint and stable signing. API26/124188824408 **242/244**; dark2×/124188824437 and landscape/124188824496 **112/114** each. All existing journeys, named-box codec/backup and actual native whole-box drag-to-text/marker tests passed; independent PDF gates PASS in all three profiles. Reports/screenshots preserved by65340331.

Both failed methods are new: Aa scope inspection called String.contentEquals on an accessibility node with null text; named-box journey found the row's identical box-name content description instead of the actual name button, so Rename never opened. Fix the test helper's null handling and distinguish actual row accessibility labels as `box name, list item N`. Preserve all assertions. Extend the codec test with actual canonical merge name-preservation assertions. No edit to visual/table/legacy controller flows.

## Reservation2 — BEFORE launch

Four jobs: build/full API26/dark2×API34/landscapeAPI34. **27/35 cumulative reserved,8 unreserved**. This stage now reserves8 total; no fresh allowance. Inventories unchanged221 JVM/244 full/114 focused. Final qualification remains pending; original failures remain in source-pinned evidence, not waived.

## Reservation2 outcome — sourceb1e1d3f/run41093980631

Build124194830465 PASS/JVM221/lint/stable signing. API26/124196005693 **244/244** and dark2×/124196005633 **114/114** PASS. Landscape/124196005649 **113/114**: native checklist drag expected before Second but dropped before Third. All independent PDF gates PASS. Evidence preserved in2db89e33. The landscape screenshot shows the insertion marker actually before Third: the helper's original screen coordinates became stale during edge scrolling/screenshot settling. Fixed48dp scroll edge zones occupy too much of a short writing viewport, allowing the target to keep shifting while holding.

## Reservation3 — BEFORE launch

Four jobs: shared build, full API26, dark2×API34 and landscapeAPI34. **31/35 cumulative reserved,4 unreserved**;12 jobs for this stage. Adapt drag edge zones to at most one fifth of the actual viewport while retaining48dp upper bound. The native journey uses matching viewport-relative safe zones, remeasures the intended line after screenshot settling, and asserts the actual marker offset equals6 immediately before dropping. Original strict text-slice/row-preservation/Undo assertions remain; do not merely accept whichever line was hit. No table or visual editor redesign. Exact inventories remain221 JVM/244 full/114 focused.

## Reservation3 outcome — source65062be3/run41096558875

All four jobs PASS: build124203716427/JVM221/lint/stable signing; full API26/124205111364 **244/244**; dark2×/124205111381 **114/114**; landscape/124205111405 **114/114**. Independent PDF gates PASS on all profiles. Adaptive short-viewport edge scrolling and the exact pre-drop marker assertion qualify together. No assertions waived.

Screenshot review of Aa at2× revealed an avoidable word break in Underline and insufficient horizontal room for the size prompt plus both steppers. This stage is about usability, so passing automation alone is not the final acceptance.

## Reservation4 — BEFORE launch; final allowance reservation

Four jobs: shared build/full API26/dark2×API34/landscapeAPI34. **35/35 cumulative reserved,0 unreserved**;16 jobs for this stage. No further run is authorised under this allowance. Aa uses two columns for inline styles at fontScale≥1.5, a full-width Normal paragraph followed by two heading choices, and a separate equally sized minus/plus row. Shorter scope explanation, familiar font labels and styled button labels improve readability without changing the formatting engine. Add native size-increase/new-typing assertion to the existing Aa journey. Inventories stay221 JVM/244 full/114 focused. All other production changes are identical to the passing65062be3 candidate. No speculative wider redesign.

## Reservation4 final observed outcome — source9e7547b3/run41098898803

All4 jobs started, bringing actual cumulative use to **35/35**,16 jobs in this stage;0 remain. Build124211784854 PASS: compilation, exact executed JVM221, debug/release lint, stable signing and raw APK upload. Final exact native gate annotations confirm **244/244** API26/124213217727, **114/114** dark2×/124213217814 and **114/114** landscape/124213217684. Dark and landscape jobs complete successfully. API26 job completes **failure** despite its passed native gate; generic exit128/1 annotations do not identify precise failed steps. Overall run remains failure. Do not label it green.

Repeated GitHub502/504 responses interrupted status monitoring; native step arrays are missing from recovered API responses. Signed artifact/log downloads fail EOF in this workspace. Final API26 independent PDF result and latest screenshots are not recovered. Preserve actual source-linked gate notices, job conclusions and artifact metadata in `writing-stage-evidence/41098898803/check-summary.json` with this explicit limitation, not fabricated native result JSON. The previous65062be3 checkpoint has all3 full native reports, rendered screenshots and independent PDFs PASS. Its PDF/model/legacy/table/visual implementation is identical to final9e; only Aa presentation/test changes follow it. This is useful baseline evidence, not a replacement for missing final results.

Scope implementation and native behaviours are complete for development testing; full pipeline/evidence and physical novice acceptance are **not** claimed complete. No additional job is launched under the exhausted allowance. See [handoff](WRITING-STAGE-DEVELOPMENT-HANDOFF.md) and [user guide](WRITING-STAGE-USER-GUIDE.md).
