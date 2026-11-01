# Content-controls revision — visual and interaction review

2026-11-01. Synthetic Android screenshots, not user screenshots, generated artwork,
a novice user study, or a physical Motorola sign-off.

## Inspected rendered evidence

Initial review: source `732fcf731e08a466fdde6584b31378c6257abcc2`, run `41277795897`,
evidence commit `c76df1f8`. That run was **not qualified**: reports retain its failures.
The images remain useful for inspecting the implemented layout; test failures are not waived.

Forty API36 scenes inspected in ten local inspection sheets (original previews are in
`design-2026-10-29/native/41277795897/current-api36/`):

| Area | Inspected scenes / finding |
|---|---|
| Home and the two workspaces | home, notes-workspace, doc-workspace, editor: selected tab, creation action and search are differentiated; simple Notes remains distinct from Doc. |
| Navigation and filtering | library, filters-view, filters-sort, filters-labels: labelled destinations, selected tabs, fixed Apply control and scrollable label choices. |
| Organisation and recovery | folders, editor-folder-create, labels, backups: folder context preserved behind the explicit create dialog; Save/Cancel remain identifiable. Backup omissions are visible, not hidden. |
| Settings and privacy | settings, storage, diagnostics, about: readable section hierarchy, local-only explanation, diagnostics opt-in and original-audio backup distinction. |
| Capture | voice-inbox, read-image-language, ocr-review, ocr-review-empty: playback/progress controls, language choice, editable OCR review, disabled empty-result save. |
| Images and drawing | image-editor, image-crop-preview, drawing-tools, drawing-options: attachment boundaries, highlighted crop corners, tool selection and edit Undo/Redo. |
| Diagrams | diagram, shape-picker, diagram-options, diagram-group: selected-shape handles, named shape choices, scoped options, prerequisite-disabled grouping. |
| Reminder and paper | add-reminder, reminder-date, reminder-time, backgrounds: clear Insert grouping, native date/time pickers and separate Colours/Background tabs. These are Android pickers, not a replacement clock/calendar. |
| Find, export and restore | note-find-sheet, find-keyboard-inline, document-pdf-options, restore-preview: matches above IME, fixed navigation, explicit PDF options and merge-preview confirmation. |
| Trash and mixed writing | trash, trash-confirmation, document-mixed, stage-named-checklist: permanent-deletion warning, content-bounded checklists, distinct item/whole-list grips, preserved text around moved content. |

Additionally inspected four control scenes in API36 light, API34 dark/200% and API34 landscape:
`stage-aa-selection`, `document-saved-styles` (historical filename; now the single Format panel),
`content-pen`, `document-content-options`.

- Format is a separate outlined, raised dock with a visible gap, not an overlay over the writing area.
- Normal font scale shows B/I/U/strike/Clear in order. Large text wraps after Underline rather than
  shrinking touch targets. Clear remains immediately after Strike in reading order.
- Size/dropdown and decrease/increase share one row; deeper controls are scrollable, especially in landscape.
- Pen management and checklist structural overflow are different surfaces. Whole-list grip has six dots;
  item grip has four, with its options button separate on the right.
- Landscape menus visibly scroll; compact IME mode intentionally hides top chrome. Undo checks must
  close the keyboard first rather than programmatically click a hidden control.
- Android Toast feedback is transient and can linger into the next fast synthetic test screenshot;
  it is not a permanent document control.

## Interaction and motion checks

New native tests preserve exact text/style/identity/Undo assertions for caret List insertion,
whole-checklist Copy/deferred Cut and direct table-cell editing. Drag tests use real injected
pointer holds, native edge scrolling and the current target geometry at release, with a marker
capture and phase-specific order diagnostics. Accessible row movement remains tested separately.

Follow-up qualification adds real Android InputConnection newline/Backspace operations and caret
position assertions, not only direct Editable mutation. It also adds attached-view motion assertions:
API36 enables the system animator scale for app-owned transitions; the other three profiles retain
reduced motion. Native OS window transitions remain disabled for deterministic harness navigation.

Haptic calls honor Android settings and do not bypass disabled feedback. A screenshot or emulator
cannot establish whether the vibration feels right on the user's phone. Physical haptic feel,
OEM keyboard behavior and novice understanding remain device/user acceptance work.

## Scope boundaries

The new content-object pen/overflow/clipboard grammar is the rich **Doc** editor's grammar for all
its inserted kinds (table, checklist, image, drawing and diagram). Simple Notes keeps its smaller
attachment/editor model; its checklist hold feedback, shared sheet transitions, row alignment and
typed numbering were reviewed/updated without converting existing Notes to Docs or flattening data.
Calculator, unrelated Archive work and Wall were not redesigned.

## Final qualification

Pending the final source-pinned native run. This document is not a release approval or a claim of
zero remaining bugs. Final run IDs, counts, inspected revision captures and APK are recorded in the
revision ledger and development handoff only after the jobs actually complete.
