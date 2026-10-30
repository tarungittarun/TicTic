# Writing-first editor: usability audit and redesign proposal

## Status and research boundary

Research/proposal only. No production UI, note schema, migration, APK or CI workflow changed. No Actions jobs used. This is an expert walkthrough of code20 and its saved native screenshots plus public product/accessibility documentation—not interviews, hands-on competitor testing or a completed usability study. The user's report is direct evidence that feature availability did not produce an acceptable experience.

Reviewed: `DocumentEditorView.java` (`render`, `renderBlock`, `renderChecklist`, `renderTable`, `insertMenu`, `blockMenu`, `formatMenu`, drag/selection handling); controller document actions; code20 user guide; source-pinned API26 `document-mixed.jpg`. Existing automated tests establish behaviour, not novice comprehension or satisfaction. Recommendations below are design judgments, not measured improvements.

## 1. What went wrong in the current editor

| Observed implementation | User consequence | Recommended change |
|---|---|---|
| Every text/checklist block gets a header and 48dp handle row. | A note looks like a form or block-management tool; writing is interrupted by implementation labels. | Hide Text/Checklist headers in ordinary writing. Preserve internal block identities and semantics. Show a subtle selected-content affordance only when relevant. |
| Separate top controls, horizontally scrolling tools, saved-status row and bottom Document row. | Too much interface competes with the page; some actions are offscreen. | One compact top bar and one adaptive bottom tool strip; compact save status in the top bar. No essential action discoverable only by horizontal scrolling. |
| Format offers 16 entries, nested Font choices and a numeric-size prompt. | Users must remember menu paths and cannot easily see active style. | A formatting sheet with visible active toggles, paragraph styles, font sample, size controls and advanced presets. |
| Insert-at-cursor versus Insert-after-section depends on focus/context. | The user can be unsure where content will go. | Preserve and visibly indicate the insertion point; never silently append because focus was lost. |
| Table editing uses Table structure → operation → numbered position. | Users translate the cell they see into a row/column number. | Act relative to the selected cell: row above/below; column left/right. |
| Images/drawings use a fixed-height preview and a separate Edit button. Diagrams are represented by an Edit/name/shapes button. | Visually uneven document; diagram contents are not visible inline in that rendering path. | Aspect-ratio-aware image/drawing previews and a bounded inline diagram preview; tap content to select/edit. |
| Drag handles exist, but the inspected block drop path chooses before/after using the target's half-height; no explicit insertion-line rendering is present there. | Destination is difficult to predict, especially around tall visuals. | Visible insertion line, selected-content outline, scroll feedback, cancel and accessible destination selection. |
| Rename is reached through generic Content controls. | A name looks like another unexplained button rather than an editable caption. | Selected visual shows its editable name and a clearly labelled Rename action. |
| Hide checked occupies a separate row, and the checklist also has a section header. | Small checklists consume excessive height. | Keep the requested item grips and + List item; move Hide checked to contextual checklist tools. |

Reducing visible clutter must not mean removing requested features, shrinking touch targets or making everything a long-press secret.

## 2. Relevant external patterns

- **Google Keep:** its Android documentation separates writing from a dedicated formatting action and describes direct Bold/Italic/Underline and paragraph style controls. Borrow the simple entry point, not its whole-note-only checklist model: MyNotes must retain mixed text/checklists and selected conversion. [3](https://support.google.com/keep/answer/2888246?hl=en&co=GENIE.Platform%3DAndroid)
- **Google Keep checklist movement:** left-side movement handles provide an established pattern. Retain the user's requested grip before the checkbox, with a non-drag alternative. [2](https://support.google.com/keep/answer/6395451?hl=en&co=GENIE.Platform%3DAndroid)
- **Samsung Notes:** official instructions use a single Insert (+) entry point for adding items/images and separate More options for sharing/export-related actions. Borrow that separation; do not add its cloud/AI dependency to this local app. [1](https://www.samsung.com/us/support/answer/ANS10001384/)
- **Apple Notes on iCloud:** its documented table flow inserts at the chosen position, creates a basic 2×2 table and lets users tap cells to type. Borrow immediate editability, without claiming this web workflow proves Android usability. [5](https://support.apple.com/en-ca/guide/icloud/mm1aa627ae18/icloud)
- **Android accessibility:** use at least 48dp touch targets, scalable text, meaningful labels and alternatives to gesture-only actions. Compact visual chrome must still have generous hit areas. [3](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility)

These sources support interaction patterns, not a claim that any competitor satisfies all MyNotes requirements.

## 3. Proposed user experience

### A. Opening and writing

Show a quiet page: optional title, note text, inline visuals/tables, and space to continue typing. Do not ask the user to create a Text section before writing. Do not show block-type headers above every paragraph.

Top bar: Back, compact save state, Undo, Redo, whole-note overflow. Bottom strip: Add (+), Text style (Aa), Checklist, Labels, Colour. Use labelled/accessibly named compact controls; at narrow widths or 2× text use a deliberately designed two-row layout rather than hiding essential controls offscreen. Existing direct Labels/Colour access remains. The strip stays reachable above the keyboard without obscuring the caret.

The overflow remains document-wide: share/export, file location, move note to folder, background/writing lines and existing whole-note metadata actions. No row/column/section editing there. Writing lines remain independent of background.

### B. Adding content

Place the cursor → Add (+) → Table/Image/Drawing/Diagram/Link. Checklist also has its direct toolbar action. Reminder remains reachable from Add. For a new empty note, insertion begins in its writing area. For selected visuals/tables, indicate an insertion point immediately after that object.

On leaving an image picker or drawing editor, restore the saved insertion anchor, not whichever field happens to have focus. After insertion, offer an obvious writing space immediately below; tapping it begins typing without finding + Text. Above/below visual boundaries must be easy to tap. Do not add unlimited empty blocks just because the user taps repeatedly.

Hyperlink: select words → Add link → labelled URL field with selected text shown. At a caret without selected words, offer Text to display + Web address. Selecting an existing link exposes Edit/Open/Remove. Keep the external-browser confirmation and safe HTTP/HTTPS restriction.

### C. Formatting without menu hunting

Aa opens a compact sheet above/resizing with the keyboard, not an additional stack of dialogs:

- Normal / Heading 1 / Heading 2 with visual samples.
- Bold / Italic / Underline / Strike with selected states.
- Font preview and named choices; size stepper plus direct numeric entry within existing bounds.
- Numbering and checklist conversion actions.
- Saved styles and Clear formatting in a secondary area.

A persistent in-sheet label says either **Selected text** or **New typing**. Mixed selection styles must not show a misleading uniformly active toggle. Applying formatting preserves the selection, scroll and keyboard. Do not require users to understand 'pending typing style'; use 'Use normal text' instead. Selection → checklist remains selected-only, never a silent whole-note conversion.

### D. Tables

Add → Table inserts an immediately editable 2×2 table and focuses its first cell. Show clear cell boundaries. With a cell selected, show contextual Row/Column controls: Add row above/below, Add column left/right, and destructive deletion with confirmation. No positional-number guessing.

New tables should fit a normal phone viewport when practical; wider tables scroll horizontally with a visible overflow cue. Do not compress cells into unreadable text. Cell text uses the same Aa tools. Provide a reachable way to leave the table and continue writing below it. Preserve all existing rich-cell marks, row/column limits and Undo grouping.

### E. Images, drawings and diagrams

Display the actual content inline. Names default to Image01, Drawing01, Diagram01 etc.; existing names survive. An understated caption does not become a large management header.

Tap the object to reveal a selection outline and a small contextual sheet: Edit, Rename, Move, Remove. Tapping blank writing space exits object selection. Drawing/image operations still use their dedicated editor, preserving audio/OCR and other existing workflows where applicable; no accidental drag should launch editing.

### F. Moving content

Keep direct checklist grips before checkboxes. For other content, reveal a handle when selected; Move enters an explicit placement state showing valid boundaries and an insertion line. Drag and edge-scroll provide a preview; dropping commits one undoable move, never copies or flattens content.

For TalkBack or difficult touch interaction, Move → Choose destination exposes labelled insertion boundaries → Move here. Cancel makes no change. This is not a return to the redundant Add item/Move up/Move down checklist menus the user removed. Text movement must mean the selected paragraph/section explicitly—not an ambiguous substring or every paragraph in the note.

## 4. Safe implementation order

1. Restore writing-first hierarchy and visible inline content; preserve storage/content/history models. No automatic destructive merging of blocks or legacy editor rollback that loses rich content.
2. Replace long Format and table-position menus with contextual tools; preserve exact selection and IME behaviour.
3. Unify insertion anchors, writing after objects, captions, movement preview and accessible destinations.
4. Qualify all 13 existing feature areas and the new workflows together, including export/reopen/Undo/Redo. Test layout at every phase rather than adding controls until the end.

Do not mix this with changes to Calculator, Archive, Wall, backup format, external-file trust rules or cloud services. No new job allowance is assumed. Current cumulative accounting remains19/35,16 unreserved; reserve individual jobs before any future build launch.

## 5. Acceptance: completing tasks, not finding menu entries

Proposed targets—not results already achieved:

| Task | Observable success |
|---|---|
| New plain note | Start typing directly, with no section setup or content-management header. |
| Mixed note | Type → insert image → type below → insert table, with no lost text or unexpected bottom insertion. |
| Selected checklist | Select two middle lines → Checklist; surrounding text remains ordinary text. |
| Style text | Select words → Aa → Bold/size; selection remains and active state is visible. Apply a style to new typing without restyling earlier content. |
| Edit table | Tap cell → add row below with one contextual operation; continue typing after the table. |
| Move content | Select diagram → Move; see exact destination before committing; Undo restores order and content. Repeat without dragging using accessibility actions. |
| Rename visual | Select Image01 → Rename; edited name persists after reopening. |
| Read note | No persistent Text/Checklist header rows, global editing panels or edit buttons between every content item. |
| Small viewport | In landscape with IME and at 2× system text, caret and required controls remain reachable; no essential action depends on horizontal toolbar discovery. |
| Data safety | Rich text, check state, indentation, table cells, image data, drawing/diagram data, history and exports survive edits/reopen/migration. Interrupted operations retain current notes. |

Follow with a real-device novice walkthrough: ask the person to perform these tasks without explanation; record success, mistaken taps, help needed and perceived difficulty. Any unresolved core-task failure blocks a usability-complete claim even if all automated tests pass.

## Decision

Keep the richer document capability, but stop exposing its block-management machinery during normal writing. Restore the earlier editor's simplicity at the interaction layer. Validate this design before declaring another feature checklist complete.
