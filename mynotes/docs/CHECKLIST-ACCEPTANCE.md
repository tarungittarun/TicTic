# MyNotes checklist backlog acceptance ledger

This ledger covers the requested K039–K052 slices. Each slice is specified before its
implementation begins. A passing source test is not Android runtime evidence, a built APK,
physical-device acceptance, or publication. Record those gates separately when they happen.

## AT-K039 — Convert text ⇄ checklist

- **AT-K039.1 — Selection and surrounding rich text:** Given a canonical writing section
  containing prefix, selected multi-line text, suffix, inline marks, and a supplementary
  Unicode character, when only the selected range is converted to a checklist, then the
  prefix and suffix remain in order with their original marks, each selected line becomes
  one unchecked row, selected row marks are retained, and a split surrogate is rejected
  without changing the source.
- **AT-K039.2 — Plain-text round trip:** Given ordered checklist rows with checked and
  unchecked states, indentation, links/styles, when the checklist is converted to text and
  its row lines are converted back, then visible row text, checked state, depth, order, and
  row marks are retained. The interoperable line prefixes are `  [ ] ` and `  [x] ` (two
  spaces per nesting level); unmarked text lines remain ordinary unchecked row text.
- **AT-K039.3 — Undo and persistence:** Given a saved canonical checklist, when the user
  converts it to text, saves/reopens, and taps Undo, then the complete original checklist
  (including checks and formatting) is restored; the converted text uses the same visible
  status prefixes as search/share/export projections.

Implementation evidence: `DocumentContentTest` and `NoteTest`; Android journey:
`CoreJourneysTest#docChecklistConvertsWithoutMarkersAndUndoRestoresRows` (the historical
method name is retained for the qualified baseline; its assertions now require markers).
Execution status: **not run**. Runtime/status evidence: **pending**.

## AT-K040 — Hide checked rows with an expandable count

- **AT-K040.1 — Per-list collapse/expand:** Given two checklists, one containing checked and
  unchecked rows, when Hide checked is enabled for only that list, then its checked rows are
  omitted and one accessible `N checked items` footer appears; tapping the footer reveals
  the checked rows without changing their text, order, marks, or completion state.
- **AT-K040.2 — Durable preference and count:** Given a collapsed checklist, when a row is
  completed/uncompleted and the editor is saved/reopened, then the footer count reflects
  the current checked rows and the per-list hidden/expanded preference is retained.
- **AT-K040.3 — Empty and read-only states:** Given no checked rows, then no count/expansion
  footer appears. Given a read-only trashed note, checked rows remain visible without an
  expansion control. Merely opening or rendering either list does not mutate its rows.

Implementation evidence: `DocumentContentTest#checkedVisibilityPreferenceIsPerChecklistAndDoesNotChangeRows`, `CoreJourneysTest#canonicalHideCheckedFooterExpandsAndPersistsPerList`, and `CoreJourneysTest#trashedChecklistsShowCheckedRowsWithoutAnExpansionControl` (Android cases authored; not run).
Source QA: the focused 35-test run passed, including editor-coherence 18/18, find/name 2/2, core contracts 6/6, and device-capture 9/9; Java syntax parsing accepted 19 changed source/test files. JUnit/Android execution: **not run**. Runtime and APK evidence: **pending**.

## AT-K041 — Per-list bullets and theme colour

- **AT-K041.1 — Isolated appearance choices:** Given two checklists and an unchanged note colour,
  when Star and Teal are chosen for only one list, then that list's row markers and board-card
  preview use Star/Teal while the sibling list and note background/colour stay unchanged.
  The picker offers Check, Dot, Square, Diamond, and Star, plus twelve theme choices: the current Default accent and eleven named colours (Red, Orange, Yellow, Green, Teal, Blue, Dark blue, Purple, Pink, Brown, Gray). Each choice has a distinct accessible name and uses the existing dark-pill picker treatment.
- **AT-K041.2 — Persistence and plain-text boundary:** Given a styled checklist, when the user
  saves and reopens it, then the per-list glyph and theme are retained. The note's plain-text
  projection, search matches, share text, PDF/export text, and backup text remain independent
  of the visual glyph/colour; checklist rows keep their `[ ]`/`[x]` status prefixes.
  Previously supported schema6 and schema7–10 libraries decode with the default appearance
  and retain all existing content.
- **AT-K041.3 — Cancel, accessibility, and read-only:** Given the picker is open, Cancel leaves
  style and content unchanged. Each choice is operable by name at large font scale and in dark
  mode. A read-only trashed list keeps its stored appearance visible but exposes no editable
  appearance control and is not mutated by viewing it.

Planned evidence: domain model/codec tests and an Android `CoreJourneysTest` covering picker,
card rendering, list isolation, persistence, and plain-text projection. Implementation and tests
are authored. Focused source QA passed 35/35, and Java syntax parsing accepted 19 changed files;
JVM/JUnit/Android execution, runtime, build, and APK remain **pending**.

## AT-K042 — Grab image text (offline OCR)

- **AT-K042.1 — Local image and privacy:** Given a writable note, when the user chooses
  Insert → Read image and selects an image from a supported on-device provider, then OCR runs
  locally and opens an editable, reviewable result. A remote URL or cloud-only provider is
  refused; cancel, decode failure, and recognition failure leave the source image and saved
  note unchanged. No image is uploaded or added to the note implicitly.
- **AT-K042.2 — Save and search:** Given reviewed recognized text, when the user edits it and
  appends it to an existing note or saves it as a new note, then only the reviewed text is
  persisted, it appears in the note's plain-text projection, and a search for a distinctive
  recognized phrase finds the saved note after reload. Empty/whitespace text cannot be saved.
- **AT-K042.3 — Unsupported and read-only states:** Given a note in Trash, there is no OCR
  editing action. Given a blank recognition result, append/new-note actions stay disabled
  until the user types nonblank text. Cancelling review never writes recognized text or changes
  note content; the selected original image remains unchanged.

Implementation note: the existing MyNotes runtime uses bundled CPU Tesseract4Android with
English, Hindi, and Kannada models, rather than the ML Kit engine named in the historical
inventory. This slice reuses that offline implementation; this is not evidence of ML Kit parity.

Evidence authored: native OCR tests in `OfflineOcrTest`, editor/review and save/search journeys
in `CoreJourneysTest#ocrReviewRequiresNonblankTextAndCancelNeverChangesTheOriginal`, and local
provider/cancel flows in `ImageSelectionTest`. The latest focused Python source QA passed
38/38, including local/review/search and timestamp contract checks plus the corrected Java
newline-escape guard. JVM/JUnit and Android execution, live OCR journey, APK build,
physical-device acceptance, and publication are **not run / pending**.

## AT-K043 — Show the edited timestamp in the editor

- **AT-K043.1 — Visible, localized footer:** Given an existing legacy note or canonical Doc,
  when it opens, then a single subdued, readable `Edited [localized date and time]` line is
  present after the note content at the bottom of its scrollable editor. Its value comes from
  the saved `updatedAt`; opening or reading the note does not change that value.
- **AT-K043.2 — Edit, save, and reopen:** Given a saved note or Doc, when its content is
  changed and the local save completes, then the line reflects that persisted update time.
  After reopening the editor it still matches the stored timestamp; the feature does not
  change sorting, content, or the creation time.
- **AT-K043.3 — Trash and accessibility:** Given a read-only trashed note/Doc, the same saved
  edited-time line is visible and opening it leaves the stored object unchanged. The line is
  exposed as readable text to accessibility services, wraps at large text/short widths, and
  remains legible in dark mode; it must not truncate or cover editing controls.

The existing editor currently exposes Edited/Created timestamps only inside More actions;
this acceptance specifically requires an always-readable editor-bottom line. Implementation and
`CoreJourneysTest#editedTimestampFooterTracksSavedEditsAcrossNotesDocsAndTrash` are authored.
The latest focused Python source QA passed 46/46. Android/JUnit execution, large-font/dark-mode
runtime review, build, and APK evidence remain **pending**.

## AT-K044 — App-wide Small / Medium / Large text

- **AT-K044.1 — Choice affects reading surfaces:** Given the app font-size setting, when the
  user selects Small, Medium, or Large, then the choice is saved and applied across note
  editor text and board cards (and other app text rendered in scalable units). Default is
  Medium. The user’s note contents, saved rich-text sizes, search results, timestamps, and
  ordering are not rewritten by changing the presentation scale.
- **AT-K044.2 — Persistence and system accessibility:** Given a selected app size, when
  MyNotes is reopened, the selection is retained. The app-specific scale combines with the
  current Android system font scale, without changing the device’s system setting.
- **AT-K044.3 — Cancel and responsive layouts:** Given the chooser is open, Cancel keeps the
  prior choice. At large Android font scale, in landscape, and in dark mode, the chosen size
  remains readable; labels wrap/controls reflow rather than truncating or shrinking touch
  targets. Note content and editor cursor remain unchanged.

The source implementation and instrumented Android journey are authored. Focused Python source QA
passed 46/46; a Java syntax parser accepted the eight changed Java files (not a compile). Android/JUnit
execution, large-scale/landscape/dark-mode rendered review, APK build, and device qualification
remain **pending**.

## AT-K045 — Toggling checklist rows keeps text plain

- **AT-K045.1 — Immediate, non-striking toggle:** Given an editable legacy Notes checklist or
  canonical Doc checklist, when a row is checked or unchecked, then its checkbox/status changes
  immediately and saves. Checked row text in the editor is subdued (or hidden by the existing
  Hide checked choice), never automatically struck through; unchecking restores the normal text
  appearance. Explicitly authored rich-text marks and row text are preserved.
- **AT-K045.2 — Persistence and identity:** After save and reopen, checked state and its display
  remain correct in both checklist types. Toggling changes only the checked flag: item IDs, text,
  rich-text marks, depth, list ordering, and note content outside that flag remain unchanged.
- **AT-K045.3 — Hide checked and read-only:** With Hide checked off, completed rows remain visible
  in the subdued style. With it on, completed rows collapse under the existing per-list count/footer
  and can be shown again. In read-only Trash, checked state remains visible and subdued, checkboxes
  are disabled, and opening the row does not mutate the saved note.

The source implementation, Android journey, and Python source guard are authored. The latest
focused Python source QA passed 46/46; a Java syntax parser accepted all eight changed Java files
(not a compile). Android/JUnit execution, rendered contrast review, build, and APK evidence remain
**pending**.

## AT-K046 — Reorder checked and unchecked rows by dragging

- **AT-K046.1 — Native drag for either state:** Given an editable legacy Notes or canonical Doc
  checklist with checked and unchecked rows visible, when the user long-presses a row grip and
  drops it before or after another row in that same list, then row order updates immediately.
  Either checked or unchecked rows can be the source; the checkbox state is unchanged.
- **AT-K046.2 — Persistence and Undo:** After save and reopen, the reordered item IDs and order
  persist. Undo restores the prior ordering. Reordering preserves each row’s text, checked flag,
  rich-text marks, and nesting depth, and does not change other note content.
- **AT-K046.3 — Boundaries and accessibility:** Dragging cannot move a row into a different
  checklist or alter read-only Trash. Read-only rows offer no drag grip. A non-drag accessible
  move-earlier/move-later action provides the equivalent ordering operation.

The source implementation, domain tests, and Android journeys for both checklist types are
authored. Focused Python source QA passed 46/46, and a Java syntax parser accepted the eight
changed Java files (not a compile). JVM/Android execution, device drag review, build, and APK
evidence remain **pending**.

## AT-K047 — Nest checklist rows with indent and outdent

- **AT-K047.1 — Adjust one row's nesting:** Given an editable legacy Notes or canonical Doc
  checklist, when the user chooses Indent or Outdent for a row, then only that row moves one
  nesting level. Its text, checked state, rich-text marks, identity, sibling order, and other
  note content stay unchanged. Nesting levels are 0 through 3.
- **AT-K047.2 — Visible inset, persistence, and Undo:** Each nesting level adds a visible 16 dp
  inset in both editors. After save and reopen, the row retains its nesting level and inset;
  Undo restores the prior level without changing other row data.
- **AT-K047.3 — Depth limits and read-only boundary:** At depth 0, Outdent is disabled; at depth
  3, Indent is disabled. Neither action can exceed the supported range. Read-only Trash has no
  row-action controls and remains unchanged when opened.

The source implementation, domain tests, Android journeys for legacy Notes and canonical Docs,
and Python source guard are authored. The focused Python source selection passed 46/46, and a
Java syntax parser accepted the eight changed Java files (not a compile). The wider 48-test run also
errored in `test_android16_is_full_and_all_four_profiles_are_required` because
`.github/workflows/notes-refinement-native.yml` is absent, and failed
`test_named_documents_use_new_schema_without_changing_unnamed_output` on its codec assertion; neither
exercises K047. JVM/Android execution, rendered inset review, build, and APK evidence remain **pending**.

## AT-K048 — Enter continues a checklist; trailing blanks are omitted

- **AT-K048.1 — Split at the caret:** Given an editable legacy Notes or canonical Doc checklist
  row, when Enter is pressed at a caret or selected range, then text before it stays in that row
  and text after it moves to a new row inserted immediately after. The original row keeps its
  identity, checked state, depth, and rich-text marks on the retained text; the new row has a
  new identity, is unchecked, inherits depth, and keeps marks on the moved text. Focus moves to
  the start of the new row.
- **AT-K048.2 — Save, reopen, and Undo:** The split and row order persist after save and reopen.
  Undo restores the original row text, marks, and checklist state without leaving a duplicate.
- **AT-K048.3 — Trailing blanks and Trash:** When the editor is left, trailing rows with only
  whitespace are silently removed; blank rows between nonblank rows remain. Read-only Trash
  does not add, split, or prune rows when Enter is attempted.

The source implementation, domain tests, Android journeys for both editors, and Python source guard
are authored. The helper rejects stale rows/invalid selections, carries rich-text spans across the
split, and keeps trailing-row pruning separate from the draft autosave so a new empty row remains
available while editing. Focused Python editor-coherence QA passed **22/22**; `javalang` accepted the
six changed Java files as syntax (not compilation), and the changed files have no trailing whitespace.
Android/JUnit execution, IME/device interaction, runtime review, build, and APK evidence remain **pending**.

## AT-K049 — The collapsed footer reports the count and expands checked rows

- **AT-K049.1 — Accurate accessible count:** Given a collapsed checklist with two checked rows,
  then the footer reads “2 checked items” and exposes “Show 2 checked items” to accessibility in
  legacy Notes and canonical Docs. A single checked row uses the singular form; zero checked rows
  has no footer.
- **AT-K049.2 — Expand without changing list data:** When the footer is tapped, checked rows in
  that checklist become visible in their existing order with text, checked state, depth, marks,
  and row identity unchanged. Sibling checklists retain their own collapse preferences. Expansion
  persists after save/reopen.
- **AT-K049.3 — Read-only Trash:** Checked rows remain visible and no expansion footer is offered;
  opening the note does not rewrite checklist state.

This is the already-present footer half of K040, now tracked as a separate list-UX acceptance item.
The implementation, legacy/canonical journeys, per-list persistence, empty state, and Trash boundary
are authored; `hiddenCheckedFooterUsesAccuratePluralCountInBothEditors` adds plural-count coverage.
Focused Python editor-coherence QA passed **22/22**; `javalang` accepted the six changed Java files
as syntax (not compilation), and the changed files have no trailing whitespace. Android/JUnit
execution, runtime/device review, build, and APK evidence are **pending**.

## AT-K050 — Swipe checked checklist rows away with an Undo snackbar

- **AT-K050.1 — Checked rows only and gesture boundaries:** Given an editable legacy Notes or
  canonical Doc checklist, swiping a checked row horizontally in either direction reveals the
  Delete affordance and removes only that row. Unchecked rows, vertical scrolling, and reorder
  handles remain unaffected; hidden checked rows must first be expanded. The feature is disabled
  in read-only Trash.
- **AT-K050.2 — Immediate Undo and exact restoration:** A successful removal shows an accessible
  “Checked item deleted” snackbar with an Undo action. Undo restores the same row ID at its prior
  position with text, checked state, nesting depth, and rich-text marks unchanged. The deletion
  saves when not undone and survives reopening; Undo also saves and survives reopening.
- **AT-K050.3 — Accessibility and boundaries:** An accessible “Delete checked item” action is
  available for an editable checked row. A stale, unchecked, or no-longer-editable row cannot be
  removed by a delayed gesture/action; Trash never exposes the swipe or delete action and does not
  mutate stored rows.

The dual-editor source implementation and `checkedChecklistRowsSwipeAwayWithUndoInBothEditors`
Android journey are authored. Focused Python source QA passed **23/23**; `javalang` accepted the
eight changed Java files as syntax (not compilation), and the changed files have no trailing
whitespace. Native swipe/Undo execution, runtime/device review, build, and APK evidence remain
**pending**.

## AT-K051 — Safe links and selected-text formatting in checklist rows

- **AT-K051.1 — Safe URL taps in both checklist editors:** Given an editable legacy Notes
  checklist or canonical Doc checklist row containing an ordinary `http://` or `https://`
  address, when the user taps the address, then MyNotes presents an external-site confirmation
  with the validated address. Cancel leaves row text, completion, order, and depth unchanged;
  no external app is launched without explicit confirmation. Invalid/non-web schemes are not
  made actionable, and automatic link affordances are not stored as rich-text marks.
- **AT-K051.2 — Format a selected checklist range:** Given a selected range inside a checklist
  row, when the user applies text formatting or adds a selected-text hyperlink, then the chosen
  marks apply only to that range. Row ID, text, order, checked state, depth, list name, and
  hide-checked preference remain unchanged. Canonical Docs retain the row in place; formatting
  a legacy Notes checklist migrates to canonical Notes-workspace content without turning it into
  a Doc or flattening other rows. Rich pasted formatting is retained. A URL's transient clickable
  span does not become a persisted mark unless the user explicitly adds a hyperlink.
- **AT-K051.3 — Writing text remains formattable:** Given an ordinary writing note/section,
  when the user applies a text style or inserts a rich hyperlink, then its text and marks still
  save/reopen as before. Checklist-row formatting must not disable or alter writing fields.

`CoreJourneysTest#checklistFormattingAndSafeLinksPreserveRowsInBothEditors` now covers safe URL
confirmation/cancellation, non-web schemes, transient automatic links, selected-range formatting
in Docs, explicit row hyperlinks, rich paste, legacy checklist migration to Notes-workspace
content, and formatting in an ordinary writing section. This scenario is **authored, not yet
executed**. Static QA, Android/JUnit execution, accessibility/tap review, compilation, build, and
APK evidence for this revision remain **pending**.

## AT-K052 — Plain-text checklist projections and JSON round-trip

These scenarios were recorded **before K052 implementation**.

- **AT-K052.1 — Stable line-prefix projection:** Given legacy Notes and canonical Doc checklists
  with checked and unchecked rows at several nesting depths, the plain-text projection emits
  each row in order with exactly two leading spaces per depth followed by `[x] ` for checked
  or `[ ] ` for unchecked, then the row text. A canonical checklist name remains a separate
  preceding line. Marker glyph/colour and the Hide checked presentation preference do not
  change the projection. Search matches the row text and its status prefix in both models.
- **AT-K052.2 — Share and export use the same projection:** Given either checklist type,
  sharing and PDF/text export include every stored row with the same status/depth prefixes,
  including rows hidden only by the editor's collapse preference. UI-only marker glyphs and
  colours are not inserted into the text; ordinary writing-section text and formatting remain
  independent.
- **AT-K052.3 — JSON mirror restores state, not a divergent text copy:** Given legacy and
  canonical checklists, checksum'd JSON backup encode/decode retains row identity, text, order,
  checked state, depth, and per-list metadata. Its reconstructed plain-text projection matches
  the prefixes above. Converting a checklist to text and parsing those explicit prefixes back
  restores checked state and nesting; unmarked lines remain unchecked ordinary text.

K052 source correction: canonical PDF export no longer treats the editor-only Hide checked
preference as a filter; every stored row is emitted with its `[x]`/`[ ]` prefix. Added executable
coverage: `NoteTest#checklistPlainTextPrefixesDriveSearchIndependentOfVisibilityAndAppearance`,
`CoreJourneysTest#checklistPlainTextProjectionSurvivesSearchAndShareInBothEditors`,
`PdfExportTest#checklistRowsExportAsPlainTextPrefixes`, and
`BackupCodecTest#checklistJsonRoundTripPreservesStatesAndPlainProjection`, alongside the existing
`DocumentContentTest#checklistTextRoundTripRetainsMarkersDepthAndRowMarks` and
`NoteTest#checklistTextConversionRoundTripsStateAndKeepsOrdinaryIndentation`. Focused Python
editor-coherence QA passed **24/24**; `javalang` accepted the five changed Java files as syntax
(not compilation), and seven K052 files have no trailing whitespace. Android/JUnit journeys are
authored, not executed. Runtime review, compilation, build, and APK evidence remain **pending**.

## Requested MyNotes editor updates — 2026-10-17

These additional regressions track the current user request. They are not release qualification
or physical-device evidence.

- **Checklist deletion:** For both Notes and Docs, Backspace on a whitespace-only row removes it
  immediately without a warning. The item menu's Remove item action is immediate too. Surviving
  row IDs, marks, completion, depth, order, and checklist metadata are unchanged; Undo can restore
  deliberate removals.
- **Uncheck all:** The checklist-level menu exposes Uncheck all in both editors. Every checked
  row becomes unchecked without replacing row IDs or changing text, marks, order, depth, or the
  Hide checked preference.
- **Drag-only checklist movement:** Checklist options do not offer “Move checklist”; the native
  block handle remains available. Dragging a checklist into a writing line or to a new section
  preserves its name, row identity/state/formatting, surrounding text, and a writable trailing
  section. Undo restores the earlier structure.
- **Writable continuation and focus:** In a fresh Doc, inserting a checklist or a table leaves an
  empty writing section after the inserted object. The first checklist row and newly added rows
  receive focus, a visible cursor at the expected caret, and the keyboard. Checklist conversion
  in Notes and copied/moved Doc checklists also retain a writable tail.
- **In-editor search:** Notes and Docs expose the query, Previous match, and Next match in a
  non-modal bar immediately above the visible keyboard while the query is focused.
- **Removed summary action:** The Notes and Doc action menus do not expose Quick summary.
- **Image annotations:** Highlight and shape annotations work through the shared image editor for
  Notes and Docs. Undo/Redo and controller-state restoration retain annotation state; saving
  explicitly changes only the original attachment (identity retained), and note Undo restores
  the original image bytes.
- **Transient notices:** Repeated identical notices are coalesced and a new notice cancels the
  preceding toast, so rapid notices replace rather than stack over editor content. Source QA
  checks the coalescing/replacement boundary; live Android toast behavior still needs runtime
  confirmation.

Regression journeys authored in `CoreJourneysTest` include
`emptyChecklistBackspaceAndMenuRemovalAreImmediateInBothEditors`,
`checklistOptionsUncheckAllAndOfferDraggingInsteadOfMoveMenu`,
`insertedChecklistAndTableKeepWritableTailAndCaretOnNewRows`,
`findQueryAndNavigationControlsStayAboveKeyboardInNotesAndDocs`,
`noteAndDocumentMenusDoNotOfferQuickSummary`,
`imageAnnotationsUndoRedoRestoreAndOnlyChangeTheSavedAttachmentInNotesAndDocs`,
`namedChecklistOptionsOmitMoveAndKeepTheDragHandle`, and the existing
`nativeChecklistBoxDragShowsLineAndDropsInsideRichWriting`. JVM domain coverage includes
`WorkspaceTest#copiedChecklistKeepsRowIdentityAndGetsWritableTail`. These are **authored, not
executed** in this revision. Static QA, Android/JUnit execution, compilation, APK build, Android
runtime, physical-device testing, and publication remain separate pending evidence gates.
