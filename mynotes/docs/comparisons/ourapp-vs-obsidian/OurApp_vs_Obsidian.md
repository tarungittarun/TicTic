# OurApp_vs_Obsidian — all-module evidence review

All identified MyNotes functional families and all 29 internal Obsidian modules in the retained pinned-version registry; not all possible third-party extensions or full functional acceptance.

Visual expert inspection of retained native frames (additional contact-sheet review) plus bounded implementation and provenance review. No new live/device/participant session.

**No fresh live usability testing has been completed. All chapter tasks are NOT RUN. No speed/jank/crash-frequency ranking. No app fixes.**

## Scope
37 chapters; all 29 internal pinned Obsidian definitions, 28 exposed controls; 205 commands and 20 settings sections routed. These are coverage references, not feature-count scores. Main PDF: 95 pages, including the historical 39-page phase-one appendix.

## X01 — Find, organise, label and reorder
P1 | Visible issue + source-supported candidates

**Scope:** Search, folders, labels/tags, list/grid, sorting and manual order

**Evidence:** The retained home has a floating New note overlapping preview words; Library combines destinations and creation actions. Current source makes a nonempty home query global, bypassing the current folder/workspace/label filters.

**Comparator/fairness:** Quick switcher, file explorer, tags and full-content search are distinct Obsidian surfaces. Its empty search frame does not demonstrate better relevance or speed.

**Judgment:** Priority remains content visibility and a clearly stated search contract. Detailed findings N01–N10 are retained in the phase-one appendix.

**Direction, not implemented:** Keep floating creation and cross-workspace search, but state scope and protect preview space; separate browsing from administration.

**Manual task — NOT RUN:** Find an archived body-only match while a folder filter is active; reorder a distant card, then return.

**Acceptance / remaining evidence:** Record actual scope, result passage, selected filters and persisted order; repeat list/grid, keyboard and large text. Measure drag continuity only in a new live session.

Frames: M01, O01

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:renderHome/filterNow/renderUniversalResults; mynotes/runtime/src/main/java/app/space/notes/runtime/NotesBoard.java; mynotes/runtime/src/main/java/app/space/notes/runtime/LibraryOrderStore.java

## X02 — Writing, reading and formatting
P1 | Visible viewport constraint

**Scope:** Notes/Doc, selected text, new typing, headings and Markdown modes

**Evidence:** Format plus keyboard leaves only a short strip of writing context. The source already has selected-text/new-typing semantics and accessible selected controls; formatting is not declared broken.

**Comparator/fairness:** Obsidian keeps a compact keyboard toolbar but exposes Markdown syntax and icon-learning costs. It is not automatically simpler for a novice.

**Judgment:** E01–E10 in the appendix explain context, hierarchy and feedback problems. More controls are not a substitute for confident writing.

**Direction, not implemented:** Adapt the existing single Format panel to the available viewport; preserve labels, selection, fifth Clear and direct editing.

**Manual task — NOT RUN:** Format the middle of a paragraph, type new text, undo, dismiss the keyboard and reopen the note.

**Acceptance / remaining evidence:** Retain surrounding prose, correct style target, caret and saved result. Repeat with large text; compare actual task completion and mistakes, not assertion counts.

Frames: M09, O06

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:renderEditor; mynotes/runtime/src/main/java/app/space/notes/runtime/FormatControls.java; mynotes/runtime/src/main/java/app/space/notes/runtime/DocumentEditorView.java

## X03 — Checklists, numbering and list editing
P2 | Expert inference

**Scope:** Complete, indent, reorder, convert selected text and hide checked

**Evidence:** Our row sheet labels its target List item 1 and exposes Indent/Outdent/Remove; checklist-level actions occupy another sheet. That helps discoverability but requires understanding row versus whole-list scope.

**Comparator/fairness:** Obsidian renders bullets, numbered items and checkboxes inside prose. The retained transformation readbacks establish narrow Markdown edits, not equal drag usability.

**Judgment:** Candidate friction is scope switching, not an established inability to edit lists. Do not remove our visible text labels just to imitate Markdown.

**Direction, not implemented:** Keep row grips, direct item insertion, named lists and a non-drag Move route; make the affected list/item unmistakable.

**Manual task — NOT RUN:** Insert an item in the middle, indent it, move it past the viewport, hide completed rows and undo.

**Acceptance / remaining evidence:** No lost items, wrong-list operations or accidental workspace swipes. Verify numbering after insertion and Backspace exit at an empty item.

Frames: AM01, AO01

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:convertNote; mynotes/runtime/src/main/java/app/space/notes/runtime/DocumentEditorView.java; mynotes/domain/src/main/java/app/space/notes/domain/AutoListContinuation.java; mynotes/domain/src/main/java/app/space/notes/domain/ParagraphNumbering.java

## X04 — Tables and direct cell editing
P1 | Expert inference

**Scope:** Cell/row/column operations, header role and wide content

**Evidence:** Our table is embedded with a named-content header and nearby pen/overflow actions. The captured compact table does not establish behavior for many columns, large fonts or long cells.

**Comparator/fairness:** Obsidian shows a readable rendered Markdown table; that is not evidence its cell editor, paste or column operations are faster.

**Judgment:** Scope ambiguity and narrow-screen navigation need validation. E08 already flags multiple neighboring action scopes.

**Direction, not implemented:** Preserve direct cell editing, header toggles, boundaries, overflow cues and undoable confirmed deletion.

**Manual task — NOT RUN:** Edit a long cell in a wide table; insert a column beside it, toggle header role, delete a row and undo.

**Acceptance / remaining evidence:** Keep the active cell visible above the IME; no unexpected horizontal jump; restore content and header meaning after undo. Compare the same table, not these unequal fixtures.

Frames: M14, O08

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/DocumentEditorView.java; mynotes/runtime/src/main/java/app/space/notes/runtime/TableWidths.java; mynotes/domain/src/main/java/app/space/notes/domain/TextTable.java

## X05 — Images, attachments and inline media
P2 | Expert inference

**Scope:** Insert, name, select, crop/edit, remove and return to text

**Evidence:** Our image selection sheet presents Use text, Keep image, Read this area and selection controls together. This is a specialised workflow with several possible outcomes to interpret.

**Comparator/fairness:** Obsidian captures links and an embedded note. This is not a matched image crop or OCR interface, and media/PDF embed correctness was not functionally accepted.

**Judgment:** Candidate mental-model friction: selecting an image region, extracting words and editing an attachment must not appear to be the same destructive operation.

**Direction, not implemented:** Explain what each action inserts or retains, preserve originals, and keep image management subordinate to reading.

**Manual task — NOT RUN:** Insert a photo, select two recognized words, keep the image, edit its crop, then remove only the intended attachment.

**Acceptance / remaining evidence:** Keep source bytes, unrelated blocks and names intact; make copy versus replacement clear. Verify offscreen word selection and explicit outside-tap dismissal.

Frames: AM02, AO02

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/ImageTextSelectionDialog.java; mynotes/runtime/src/main/java/app/space/notes/runtime/ImageTextSelectionView.java; mynotes/runtime/src/main/java/app/space/notes/runtime/ImageEditView.java; mynotes/runtime/src/main/java/app/space/notes/runtime/NoteImageFiles.java

## X06 — Web hyperlinks and external opening
P2 | Expert inference

**Scope:** Display text, safe URL validation, edit/remove and browser consent

**Evidence:** Our Hyperlink form separates display text and address; the captured invalid-address state shows an error affordance. Source limits web links to HTTP/HTTPS and confirms external opening.

**Comparator/fairness:** Obsidian internal links and embeds are not the same operation as adding a web URL. Do not award a graph/navigation win for a web-link form.

**Judgment:** Validation visibility and multiline entry deserve a matched test; no claim of broken link handling follows from this captured fixture.

**Direction, not implemented:** Keep visible URL errors and original words on Remove; identify when another app will open.

**Manual task — NOT RUN:** Add a long URL to selected words, correct a malformed address, reopen, edit and remove the link.

**Acceptance / remaining evidence:** No duplicate URL per line, changed neighboring prose or unnoticed external launch. Check keyboard visibility, wrapped fields and TalkBack error announcement.

Frames: AM03, AO02

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:noteHyperlink; mynotes/domain/src/main/java/app/space/notes/domain/RichLinks.java; mynotes/domain/src/main/java/app/space/notes/domain/SafeLinks.java

## X07 — Internal links, backlinks, graph and previews
P1 | Comparator reference / scoped capability unknown

**Scope:** Connected-note navigation and local/global context

**Evidence:** The scoped MyNotes source inspection establishes web links; it does not establish an equivalent vault-wide internal-link/backlink/graph workflow. This is a candidate scope gap, not a proved app-wide absence.

**Comparator/fairness:** Obsidian shows linked mentions with a passage, outgoing links, global/local graphs and an embedded note. A tiny graph with a large options panel also has cognitive and viewport costs.

**Judgment:** Potential effectiveness gap for connected research notes, not a reason to add a decorative graph to every simple note.

**Direction, not implemented:** Validate whether users need stable internal-note references first. Prefer a small reliable related-note route before copying complex graph controls.

**Manual task — NOT RUN:** Link A to a heading in B, rename B, return through backlinks and preview the target without losing A.

**Acceptance / remaining evidence:** Target identity and rename behavior must be verified live; count wrong destinations and recovery steps. No graph speed or link-integrity winner has been measured.

Frames: O12, AO03

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java; mynotes/domain/src/main/java/app/space/notes/domain/RichLinks.java

## X08 — Long-note orientation, tags, footnotes and word count
P1 | Scoped gap + explicit comparator blocker

**Scope:** Headings, jump/navigation, find and document context

**Evidence:** MyNotes supports headings and highlighted find return. The reviewed source/captures do not establish a heading-outline jump path equivalent to the captured Obsidian outline.

**Comparator/fairness:** Obsidian tags and outline are visible; the Footnotes pane says No footnotes found even though fixture text contains a footnote. Rendered detection was explicitly blocked, not accepted.

**Judgment:** Long-note orientation is a candidate gap; footnote and word-count completeness remain untested in our matching scope.

**Direction, not implemented:** Keep fast highlighted Find, and validate a compact heading route rather than adding permanent chrome.

**Manual task — NOT RUN:** Locate a middle heading, find a repeated phrase, follow a footnote and return to the exact reading location.

**Acceptance / remaining evidence:** Verify target and return anchors, heading levels, counts and generated text treatment; do not infer footnote success from a registered command.

Frames: O09, AO04

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NoteFindDialog.java; mynotes/domain/src/main/java/app/space/notes/domain/FindInNote.java; mynotes/runtime/src/main/java/app/space/notes/runtime/DocumentEditorView.java

## X09 — Properties and Bases
Not ranked | Different product scope

**Scope:** Structured metadata, records and filtered views

**Evidence:** Our note properties include labels, color, folder and reminder state; arbitrary typed-property and database-view editing is not established by this inspection.

**Comparator/fairness:** Obsidian shows typed properties and a Bases table of vault files. The canonical standalone Properties attempt was obscured; visible Properties is a layout reference, not proof of a successful properties interaction.

**Judgment:** Not a fair row-for-row comparison with our document tables. A Bases database is a different information architecture, not just more columns.

**Direction, not implemented:** Treat structured-record management as a separate product decision. Do not burden everyday notes with unused metadata fields.

**Manual task — NOT RUN:** For users who need records, edit one typed field, filter a view, rename a file and reopen the view.

**Acceptance / remaining evidence:** Validate data types, retained filters, note identity and error recovery. No Bases performance, formulas or equivalent MyNotes capability is certified here.

Frames: O11, AO05

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:renderLabels/renderColours; mynotes/domain/src/main/java/app/space/notes/domain/Note.java

## X10 — Quick capture, daily notes, templates and unique notes
P2 | Shared capture task / optional workflows unknown

**Scope:** Start a useful note with low setup cost

**Evidence:** Our floating create action is direct, but a sparse fixture cannot establish first-use usefulness. The reviewed slice does not establish equivalent daily-note/template workflows.

**Comparator/fairness:** Obsidian daily notes and timestamp names are visible. Template insertion failed because no template folder was configured; random-note invocation does not certify selection distribution.

**Judgment:** Capture efficiency is a shared job; daily notes/templates are optional aids, not automatic points. Setup failures must not be presented as competitor success.

**Direction, not implemented:** Preserve immediate blank-note creation. Evaluate templates only against actual repeated writing tasks and explain required configuration.

**Manual task — NOT RUN:** From a cold start, capture a short thought; then create a repeated meeting structure and return tomorrow.

**Acceptance / remaining evidence:** Observe time to first saved words, setup mistakes, duplicate daily notes and return continuity. Do not populate fake user content to make the app feel busy.

Frames: M01, AO06

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:renderHome; mynotes/domain/src/main/java/app/space/notes/domain/Note.java

## X11 — Freehand drawing and Canvas
P2 | Partially overlapping tools

**Scope:** Visual thinking, embedded drawings versus spatial boards

**Evidence:** Our dedicated drawing screen exposes labelled Pen/Pencil/Highlighter, width/color and undo tools; the frame alone does not measure pen latency, palm rejection or save reliability.

**Comparator/fairness:** Obsidian Canvas shows connected cards on a spatial board. It is not a native freehand drawing benchmark, and the requested landscape captures stayed portrait.

**Judgment:** These tools overlap in visual thinking but are not interchangeable. Do not score our pen tools against a card-board feature count.

**Direction, not implemented:** Keep accessible drawing tools and clear draft/save meaning; make the transition back to the containing note predictable.

**Manual task — NOT RUN:** Draw a diagram annotation, leave and reopen it; in Canvas connect two cards, pan/zoom and reopen the board.

**Acceptance / remaining evidence:** Evaluate each tool against its own output goal. For common save/return work, preserve content and location; latency needs real stroke traces on matched hardware.

Frames: AM04, AO07

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/DrawingPad.java; mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:stashDrawing/renderDrawing; mynotes/runtime/src/main/java/app/space/notes/runtime/DrawingClipboard.java

## X12 — Diagrams, shapes, arrows and grouping
P2 | Expert inference

**Scope:** Create/edit a bounded structured visual

**Evidence:** Our diagram editor has shape selection plus a separate options sheet for Group shapes, Edit arrows and Split arrow. A large empty canvas in a one-shape fixture is not evidence of missing content.

**Comparator/fairness:** Canvas is the nearest retained Obsidian visual reference, not a matched shape-and-arrow editor. No successful comparator arrow/group gesture is established.

**Judgment:** Candidate discoverability issue: users must find contextual grouping and connection actions without confusing selection, editing and export.

**Direction, not implemented:** Preserve colorless diagram options, pen-scoped names and direct arrow editing. Improve guidance at the relevant selection state, not with a permanent tutorial overlay.

**Manual task — NOT RUN:** Add two shapes, connect them, group, resize, split the arrow, undo and reopen.

**Acceptance / remaining evidence:** Check actual anchor geometry, target selection, retained names and no accidental duplication. Run long-press/context-menu actions with large text and TalkBack.

Frames: AM05, AM06

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/DiagramView.java; mynotes/runtime/src/main/java/app/space/notes/runtime/DiagramRenderer.java; mynotes/domain/src/main/java/app/space/notes/domain/Diagram.java; mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:renderDiagram

## X13 — PDF reading, selection, search and markup
P1 | Current-state reference / comparator workflow unverified

**Scope:** Open, scroll, zoom, find, annotate and export a marked copy

**Evidence:** Current retained frames show a compact filename toolbar, keyboard-adjacent find field and separate markup rail. Earlier rejected viewer duplication should not be reported as still present.

**Comparator/fairness:** The Obsidian audit did not certify PDF embed/viewer workflows. No matched PDF search, annotation or original-preservation run exists for it.

**Judgment:** Current design has the requested shape; gesture precision, search latency, scrubber timeout and zoom continuity remain acceptance work, not proved polish.

**Direction, not implemented:** Retain real file-wide zoom, selection-clearing outside tap, compact controls, loader and the hiding page scrubber. Do not reintroduce Go to page or duplicate counts.

**Manual task — NOT RUN:** Open a large scanned/mixed PDF, zoom a distant page, select offscreen text, find a hit with IME, mark a rotated page and export.

**Acceptance / remaining evidence:** Compare the original bytes; inspect hit position, highlight geometry and rendered export. Exercise cancellation and a partial destination; private markup drafts are outside Notes backups.

Frames: AM07, AM08

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/LocalPdfPreview.java; mynotes/runtime/src/main/java/app/space/notes/runtime/PdfTextLayer.java; mynotes/runtime/src/main/java/app/space/notes/runtime/PdfPageOcr.java; mynotes/runtime/src/main/java/app/space/notes/runtime/PdfMarkup.java; mynotes/runtime/src/main/java/app/space/notes/runtime/PdfMarkedCopy.java

## X14 — Word files and original-layout expectations
P1 | Different file-format scope

**Scope:** DOCX preview, selection, zoom, import versus original reader

**Evidence:** Our filename toolbar is compact. Source explicitly says MyNotes pages are an approximate local preview and offers an original-layout reader or a separate editable copy.

**Comparator/fairness:** Obsidian is not a retained Word-layout comparator; its rendered Markdown cannot establish DOCX fidelity.

**Judgment:** Effectiveness depends on honest mode boundaries. A readable preview must not be mistaken for a faithful Office renderer or editable original.

**Direction, not implemented:** Keep the original-only opening flow and clear disclosure. Separate page appearance controls from edits to imported content.

**Manual task — NOT RUN:** Open a DOCX with headers, inline images, wide tables and hard page breaks; select text, zoom and create an editable copy.

**Acceptance / remaining evidence:** Check original bytes and document return position, compare rendered pages with a suitable Word reference, and disclose any dropped layout/media. Do not invent fidelity from a synthetic one-page frame.

Frames: AM09, AM10

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/WordPageFile.java; mynotes/runtime/src/main/java/app/space/notes/runtime/StructuredWord.java; mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:openFile/openWordPages

## X15 — Large TXT and read-only text viewing
P2 | Current viewer reference / performance unmeasured

**Scope:** Pagination, selecting, find and creating an explicit editable copy

**Evidence:** Our retained TXT view shows real text, a 1/17 page indicator and page arrows rather than the note editor. It is an opening state, not a measured large-file completion test.

**Comparator/fairness:** Obsidian native Markdown reading is a text-oriented reference, but arbitrary TXT opening, pagination and selection were not matched.

**Judgment:** The shared job is locating and copying information without accidentally importing or overwriting the source. Input size alone is not proof of low latency.

**Direction, not implemented:** Preserve read-only original viewing until explicit copy/edit, and stable search/selection across page boundaries.

**Manual task — NOT RUN:** Open a large multilingual TXT, find a repeated phrase near the end, select across the viewport and return to the prior file list.

**Acceptance / remaining evidence:** Measure open/find latency separately from rendering; inspect Unicode integrity, clear selection on deliberate tap, retained page and unchanged original.

Frames: AM11, O07

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:renderFileTextPreview; mynotes/runtime/src/main/java/app/space/notes/runtime/LocalOpenFile.java; mynotes/domain/src/main/java/app/space/notes/domain/TextPages.java

## X16 — Spreadsheet previews and cached values
P2 | Different data-model scope

**Scope:** CSV/XLSX grids, viewport navigation and data expectations

**Evidence:** Our preview exposes a 3-row, 2-column fixture with row/column coordinates. The inspected source opens CSV/XLSX through SpreadsheetFile and SpreadsheetPreview.

**Comparator/fairness:** Obsidian Bases is a vault-record view, not an Excel engine. Its table cannot establish XLSX formula execution, workbook fidelity or CSV compatibility.

**Judgment:** Do not imply a preview is a full spreadsheet editor. Cached-value handling and sheet ordering must be explained when creating text copies.

**Direction, not implemented:** Keep grid context and explicit preview mode; provide readable wide-cell navigation without competing zoom/scroll gestures.

**Manual task — NOT RUN:** Open quoted CSV and a multi-sheet XLSX with formulas, missing cached results and wide cells; locate a value and copy it.

**Acceptance / remaining evidence:** Compare visible rows/columns and Unicode with source values, disclose cached-only results, retain sheet/scroll position and never execute macros or scripts.

Frames: AM12, AO05

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/SpreadsheetFile.java; mynotes/runtime/src/main/java/app/space/notes/runtime/SpreadsheetPreview.java; mynotes/runtime/src/main/java/app/space/notes/runtime/CommonFileText.java

## X17 — Markdown, HTML, RTF, ODT and slide imports
P1 | Source-supported fidelity limitation

**Scope:** Safe text extraction versus native rich rendering

**Evidence:** CommonFileText supports CSV/Markdown/HTML/RTF/ODT/XLSX/PPTX extraction. HTML strips script/style blocks; Office extraction does not promise layout, media or live formulas.

**Comparator/fairness:** Obsidian natively renders Markdown with code/callouts/math. This is not proof of equivalent import behavior for every other file type.

**Judgment:** Our supported-extension breadth should not hide a lower-fidelity text-copy contract. This is a source-supported limitation, not a reproduced corruption bug.

**Direction, not implemented:** Explain exactly what a viewer preserves and what an editable copy drops before confirmation; keep other-reader privacy boundaries explicit.

**Manual task — NOT RUN:** Open one realistic file of each advertised type, inspect text order and unsupported elements, then cancel or create a copy.

**Acceptance / remaining evidence:** Record per-format fidelity and actionable errors; preserve originals, reject malformed/oversized archives safely and distinguish external-reader delegation from in-app support.

Frames: AM13, AO08

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/CommonFileText.java; mynotes/runtime/src/main/java/app/space/notes/runtime/LocalOpenFile.java; mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:openFile

## X18 — OCR, image-region text and scanned-page recognition
P1 | Retained measured limitation, not new live reproduction

**Scope:** English/Hindi/Kannada, correction, provenance and loaders

**Evidence:** The retained review screen requires checking text before saving. The delivered qualification receipt says OCR targets were met for 1512/1536 main observations; eight extra public scenes executed with zero exact outputs.

**Comparator/fairness:** No equivalent built-in Obsidian OCR outcome was established. A denied/absent comparator workflow is not an accuracy win for us.

**Judgment:** Known recognition-quality limit, not merely a styling concern. Generated repeated-reference observations do not establish field accuracy on varied posters, tiny fonts or emoji.

**Direction, not implemented:** Keep original images, explicit user-triggered OCR, visible loading, editable review and a clear insertion destination. Do not silently retry duplicates or invent unsupported-language text.

**Manual task — NOT RUN:** Read tiny/oblique/multilingual public scenes, blank controls and selected image regions; compare exact expected text and corrected output.

**Acceptance / remaining evidence:** Retain source hashes, full hypotheses, CER, omissions/false words, cold/warm latency and correction time. No new accuracy study or training occurred in this report.

Frames: AM02, AM14

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/OfflineOcr.java; mynotes/runtime/src/main/java/app/space/notes/runtime/ImageRegionText.java; mynotes/runtime/src/main/java/app/space/notes/runtime/PdfPageOcr.java; mynotes/docs/candidate69-markup-followup.md

## X19 — Voice recording, playback and transcription
P1 | Retained accuracy limitation / comparator recording blocked

**Scope:** Permission, pause/resume, originals, review and speech engines

**Evidence:** Our inbox displays transcript state, playback controls and a progress rail. Existing qualification met speech targets for 2528/3216 observations, not all. The optional engine is labelled experimental.

**Comparator/fairness:** Obsidian audio recorder reached permission denial in its audit; successful recording, playback and quality were not certified. Recording audio is not equivalent to transcription.

**Judgment:** Known effectiveness limits must not be buried behind a polished inbox. Neither a playback icon nor an aggregate processing rate proves physical listening quality.

**Direction, not implemented:** Preserve original audio independently of transcripts, honest engine labels, readable progress and review-before-use. Do not claim general Indian-accent competence from one adapted speaker.

**Manual task — NOT RUN:** Record varied Indian speakers, pause/resume, deny/regrant permission, play through a headset and correct names/numbers while listening.

**Acceptance / remaining evidence:** Measure word errors, boundary losses, time-to-review, seek accuracy and actual audible playback on physical hardware. Verify originals export separately from note backups.

Frames: AM15, AM16

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/VoiceCapture.java; mynotes/runtime/src/main/java/app/space/notes/runtime/VoicePlaybackCard.java; mynotes/runtime/src/main/java/app/space/notes/runtime/VoiceFiles.java; mynotes/runtime/src/main/java/app/space/notes/runtime/OfflineSpeech.java; mynotes/docs/module-e2e/42021523354/voice-speaker-summary.json

## X20 — Reminders and notifications
P1 | Source-supported delivery boundary

**Scope:** Schedule, rename, deliver, cancel and reopen the intended note

**Evidence:** A native time picker is visible. LocalReminders schedules inexact alarms and checks notification permission/channel state; this is not proof that a reminder arrived on a physical device.

**Comparator/fairness:** Daily-note creation in Obsidian is not equivalent to notification scheduling. No built-in matched reminder-delivery workflow is established.

**Judgment:** Delivery expectation is the key effectiveness risk: a selected time must not imply an exact-alarm guarantee the implementation does not provide.

**Direction, not implemented:** Keep reminders optional, renameable and local; explain approximate timing and permission blocks at the point they matter.

**Manual task — NOT RUN:** Set, rename and reschedule a reminder, deny notifications, reboot and then tap the delivered notification.

**Acceptance / remaining evidence:** Use disposable notes; verify no stale/duplicate notification, correct note identity and sensible blocked-state feedback. Measure actual delivery under power restrictions separately.

Frames: AM17, AM18

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/LocalReminders.java; mynotes/runtime/src/main/java/app/space/notes/runtime/ReminderReceiver.java; mynotes/domain/src/main/java/app/space/notes/domain/LocalSurfaces.java

## X21 — Home widget and Android entry points
P2 | Source inspection only for launcher workflow

**Scope:** Pinned/recent projection, resizing, share-in and deep-link continuity

**Evidence:** Settings offers a Home widget. NotesWidget source renders up to four projected note titles and a first-use empty message; no launcher widget screenshot is used as visual acceptance evidence.

**Comparator/fairness:** The retained Obsidian mobile quick action is an in-app gesture, not proof of an equivalent Android home widget.

**Judgment:** Source presence is not launcher usability. Actual widget placement, refresh and accessibility are outstanding.

**Direction, not implemented:** Preserve optional widget consent, pinned-first content and clear launch destinations; do not expose trashed notes through stale OS surfaces.

**Manual task — NOT RUN:** Place/resize the widget, pin and archive notes, share text from another app and open a reminder deep link.

**Acceptance / remaining evidence:** Verify labels, intended note, no duplicates and return context on real launchers, including large system text. Do not restore widget/notification consent silently during recovery.

Frames: AM19, M01

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesWidget.java; mynotes/runtime/src/main/java/app/space/notes/runtime/LocalSystemSurfaces.java; mynotes/app/src/main/AndroidManifest.xml

## X22 — Appearance, backgrounds, fonts and accessibility
P1 | Expert inference

**Scope:** Theme, paper, writing lines, large text and assistive input

**Evidence:** Our theme sheet has System/Light/Dark choices, while note paper backgrounds and text formatting are separate controls. E04 already notes implementation-facing sp units; Obsidian also exposes pixels.

**Comparator/fairness:** Obsidian offers base scheme, accent, themes, fonts and CSS snippets. More appearance settings do not prove better contrast, legibility or accessible interaction.

**Judgment:** Potential confusion comes from several appearance scopes and cramped large-text editing, not a lack of decorative options.

**Direction, not implemented:** Preserve system appearance, legible labels, writing-line independence and generous targets; clarify app theme versus note paper versus selected-text color.

**Manual task — NOT RUN:** Switch system theme, increase font scale, navigate with TalkBack and format selected text over a patterned background.

**Acceptance / remaining evidence:** Check clipping, announced scope/selection, contrast and focus order across orientations; screenshots alone do not establish 48dp targets or a successful screen-reader task.

Frames: AM20, AO09

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesAppearance.java; mynotes/runtime/src/main/java/app/space/notes/runtime/NotesDesign.java; mynotes/runtime/src/main/java/app/space/notes/runtime/NoteBackground.java; mynotes/runtime/src/main/java/app/space/notes/runtime/FormatControls.java; mynotes/runtime/src/main/java/app/space/notes/runtime/EditorRails.java

## X23 — First-use storage, private folders and optional App folder
P1 | Source-supported model complexity

**Scope:** Where notes live, extra copies, reconnect and recovery expectations

**Evidence:** Our Storage & recovery page explains on-phone saving plus optional extra copies. A second dialog lists selectable categories and says active recording remains pending until stopped.

**Comparator/fairness:** Obsidian audit used private app storage; its vault is not an automatic cross-device backup. Private app storage is vulnerable to uninstall in both workflows.

**Judgment:** Our architecture asks users to distinguish private notes, organisational folders, extra App-folder copies and portable backup. This is a real mental-model burden, not evidence of current data loss.

**Direction, not implemented:** Keep truthful copy/permission warnings but use progressive disclosure and one understandable status: saved privately, extra copy ready/pending, recovery needs verification.

**Manual task — NOT RUN:** Create notes offline, choose an empty optional folder, revoke access, save again and reconnect without clearing app data.

**Acceptance / remaining evidence:** Inspect exact copied categories and pending status; test reinstall/reselection only on disposable fixtures. Never tell the user to uninstall or clear their real library for this review.

Frames: AM21, AM22

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:renderStorage; mynotes/runtime/src/main/java/app/space/notes/runtime/AppFolder.java; mynotes/runtime/src/main/java/app/space/notes/runtime/AppFolderTasks.java; mynotes/runtime/src/main/java/app/space/notes/runtime/NotesRepository.java

## X24 — Portable backup, import merge and full recovery
P1 | Documented/source-confirmed capability limitation

**Scope:** Verify output, disclose exclusions, restore without overwriting newer work

**Evidence:** The backup page visibly lists exclusions; restore previews added/skipped/conflicting notes before Confirm merge. BackupCodec and the UI explicitly cap portable backup at 8 MiB.

**Comparator/fairness:** Obsidian file recovery is per-file history, not a certified full-vault backup/restore path. Sync is not used here as a backup benchmark.

**Judgment:** The 8 MiB bound is a concrete portability limitation for rich libraries. Original audio, empty folders, saved named presets and edit history are excluded; private PDF drafts need separate export.

**Direction, not implemented:** Make backup scope and actual readiness legible before users rely on it. Preserve checksum/readback, non-destructive merge and checkpoints; plan larger-library portability only after comparison approval.

**Manual task — NOT RUN:** Export a near-limit rich library, exceed the limit safely, import identical and same-ID changed notes, then verify every supported content type.

**Acceptance / remaining evidence:** No silent partial success or overwrite; disclose omitted categories and readable/non-encrypted JSON. A complete App-folder checkpoint is a separate opt-in recovery mechanism, not this portable file.

Frames: AM23, AM24

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/BackupCodec.java; mynotes/runtime/src/main/java/app/space/notes/runtime/LocalBackupFiles.java; mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:renderBackups/renderPreview; mynotes/runtime/src/main/java/app/space/notes/runtime/AppFolder.java

## X25 — Trash, permanent deletion and recovery
P1 | Visible safety affordance / recovery execution unverified

**Scope:** Restore, seven-day retention policy, purge scope and safety

**Evidence:** Our confirmation explicitly says permanent deletion cannot be undone and that backups/original voice recordings are kept separately. The ordinary Trash view lists deleted note cards.

**Comparator/fairness:** Obsidian local recovery shows source text with Copy/Restore actions. It is not a matched Trash purge or successful restoration test.

**Judgment:** The visible warning is a strength to preserve. Recovery confidence still depends on explaining different lifetimes for a note, recording, backup and annotation draft.

**Direction, not implemented:** Keep seven-day Trash/Undo and explicit destructive confirmation. Make restoration and separately retained copies understandable without alarming first-use notes.

**Manual task — NOT RUN:** Delete/restore a rich note, inspect what happens to its audio and exports, then purge only a disposable selected note.

**Acceptance / remaining evidence:** Verify the exact affected IDs, Undo boundaries, expiry policy and retained separate data. Never infer crash or power-loss recovery from a confirmation screenshot.

Frames: AM25, AO10

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:confirmPermanentDelete/bulkState; mynotes/runtime/src/main/java/app/space/notes/runtime/NotesRepository.java; mynotes/runtime/src/main/java/app/space/notes/runtime/VoiceFiles.java

## X26 — Archive, pinning and bookmarks
P2 | Expert inference

**Scope:** Keep important content reachable without duplicating it

**Evidence:** Our Library offers Archive, and home distinguishes Pinned/Others. Source toggles archive/pin on note identities rather than making content copies.

**Comparator/fairness:** Obsidian Bookmarks capture is empty. It establishes an entry point, not successful bookmark creation, folder organisation or persistence.

**Judgment:** Archive, pin, tab pin and bookmark are different concepts. The shared criterion is reliable return to important content, not equal label counts.

**Direction, not implemented:** Keep pinning and Archive; state when global search includes archived content, and avoid teaching a second path that duplicates the same action.

**Manual task — NOT RUN:** Pin a note, archive it, find it globally, restore it and compare a saved Obsidian bookmark task.

**Acceptance / remaining evidence:** Check persisted identity/order, scope labels and no accidental duplicate. Bookmark persistence must be newly verified rather than inferred from its empty pane.

Frames: AM26, AO11

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:bulkState/filterNow; mynotes/domain/src/main/java/app/space/notes/domain/LibraryFilter.java

## X27 — Undo, redo, editing history and save feedback
P1 | Expert inference

**Scope:** Session recovery, retained history and draft durability

**Evidence:** Our top bar exposes Undo/Redo. Source explains retained history bounds: up to 50 steps per note, 64 recently edited notes, 16 MiB on disk; it is not exported in portable backups.

**Comparator/fairness:** Obsidian snapshots provide a source-text recovery view; not an equivalent guarantee for every rich-object undo or process interruption.

**Judgment:** Distinguish undo history, saved note, recovery snapshot and backup. A visible Saved label cannot prove every external copy is verified.

**Direction, not implemented:** Keep clear private-save versus copy-pending feedback and safe Back. Do not fix perceived empty space by hiding durability warnings.

**Manual task — NOT RUN:** Edit text/table/image, undo and redo, leave and reopen; repeat after controlled process restart on disposable data.

**Acceptance / remaining evidence:** Verify exact rich payload and history matching the saved revision; preserve current content when history is invalid or trimmed. Never run destructive migration on the user library.

Frames: AM27, AO10

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/TextEditHistory.java; mynotes/runtime/src/main/java/app/space/notes/runtime/PersistentHistoryStore.java; mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:queueSave/undo; mynotes/domain/src/main/java/app/space/notes/domain/DocumentHistory.java

## X28 — Workspaces, tabs and mobile navigation
P1 | Expert inference

**Scope:** Notes/Doc, switching contexts, Back and layout restoration

**Evidence:** Our Library and Notes/Doc switcher support different destinations; workspace recall is an expert concern, not evidence two workspaces are inherently wrong.

**Comparator/fairness:** Obsidian tab overview and Manage workspace layouts are visible. Saving/restoring a layout was not semantically certified, and mobile settings expose additional navigation variants.

**Judgment:** Two different navigation architectures both need a simple return path. Do not demand a desktop-style tab model for our app.

**Direction, not implemented:** Preserve deliberate Notes/Doc switching and block accidental workspace swipes during drag. Keep route and cursor/scroll context on Back.

**Manual task — NOT RUN:** Switch between two notes, a PDF and Settings; reorder content near an edge, then return to the initial editing location.

**Acceptance / remaining evidence:** Log wrong destinations, lost context, accidental switches and state after restart. Workspace restoration is outstanding until a matching live task succeeds.

Frames: M02, AO12

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:openWorkspace/fileReturnTarget; mynotes/runtime/src/main/java/app/space/notes/runtime/LibraryGestureLayout.java; mynotes/runtime/src/main/java/app/space/notes/runtime/EditorRails.java

## X29 — Settings, commands, quick actions and help discovery
P2 | Expert inference

**Scope:** Find an action without memorising hidden gestures

**Evidence:** Our settings are grouped and globally searchable. The inspected scope does not establish a full action-command palette equivalent; this is not a claim settings search is missing.

**Comparator/fairness:** Obsidian command palette, quick action and mobile-toolbar configuration are visible. Slash-command invocation was blocked and keyboard shortcuts do not guarantee touch reachability.

**Judgment:** The gap is discovering the right action at the right time, not simply matching the number of command rows.

**Direction, not implemented:** Make common actions predictable in context, preserve readable labels and teach rare gestures progressively; evaluate command search only if tasks justify it.

**Manual task — NOT RUN:** Without a walkthrough, find move, undo, export, a setting and one uncommon editor action using touch alone.

**Acceptance / remaining evidence:** Observe search terms, wrong taps and help requests; distinguish disabled/contextual/unconfigured actions. Do not use all 205 inventoried commands as a usability success count.

Frames: AM19, O04

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:renderSettings/renderUniversalResults; mynotes/runtime/src/main/java/app/space/notes/runtime/EditorActions.java; mynotes/runtime/src/main/java/app/space/notes/runtime/InsertPalette.java

## X30 — Export, sharing, duplication and note composition
P1 | Source-supported format/scope boundaries

**Scope:** PDF/JSON/text, content copy versus flattening, merge safety

**Evidence:** Our PDF sheet exposes writing-guide export choice. Source Android text sharing uses plainText with a 100,000-character cap; merging moves sources to Trash and provides Undo.

**Comparator/fairness:** Obsidian note-composer commands are inventoried, not a matched full compose/export/share acceptance run. Do not equate a command entry with a successful destination write.

**Judgment:** Different export routes preserve different things. Users need to know when they are sending plain text, a visual PDF, an editable note file or a recoverable backup.

**Direction, not implemented:** State preservation scope before export, keep originals unchanged, retain per-content copy semantics and verify destination writing rather than reporting chooser launch as completion.

**Manual task — NOT RUN:** Share a mixed note, export PDF and JSON, duplicate it, merge selected notes and undo the merge.

**Acceptance / remaining evidence:** Check text/media/table fidelity for each promised format, exact source IDs and retained source copies; destination failure must not silently destroy content.

Frames: AM28, O04

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:exportPdf/shareSelected/mergeSelected; mynotes/runtime/src/main/java/app/space/notes/runtime/PdfExporter.java; mynotes/domain/src/main/java/app/space/notes/domain/ContentCopies.java

## X31 — Local summary and text assistance
Not ranked | Source-only / app-specific / not comparatively ranked

**Scope:** Extractive summary with source context, not generative AI

**Evidence:** NotesController.quickSummary uses a local sentence ranker and displays a warning that it may miss important details. The selected editor frame is context only, not a captured summary result.

**Comparator/fairness:** No equivalent built-in summarizer was established in the Obsidian audit. Community AI plugins are outside the installed-core comparison.

**Judgment:** This app-specific tool receives no comparative feature credit. Effectiveness must be judged against faithful extraction and useful coverage, not generated-looking text.

**Direction, not implemented:** Keep local-only, non-generative labelling, source context and explicit user review. Do not claim a language model or training where none is used.

**Manual task — NOT RUN:** Summarize varied notes with negation, dates and multilingual sentences; check the retained source and decide whether the result is useful.

**Acceptance / remaining evidence:** Verify each sentence comes from the source, essential cautions are not lost and limits are clear. No summary-quality test was executed in this report.

Frames: M07

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:quickSummary; mynotes/domain/src/main/java/app/space/notes/domain/LocalSummary.java

## X32 — Privacy, permission boundaries and diagnostic logs
P1 | Expert inference

**Scope:** Offline processing, opt-in collection, export and delete

**Evidence:** Diagnostics collection is visibly off in the retained frame; the log viewer states there are no matching records. The app manifest declares microphone, notifications and boot reception, not INTERNET.

**Comparator/fairness:** Obsidian has accounts/Sync and community-plugin trust boundaries. This does not imply ordinary local editing uploads content or that it is insecure.

**Judgment:** Our local/opt-in stance is worth preserving. A friendly sentence is not a complete privacy/security audit, especially when handing files to another reader.

**Direction, not implemented:** Keep collection off until opted in; distinguish local save/export from upload and explain external-reader control. Never collect private content merely to make a diagnostic report fuller.

**Manual task — NOT RUN:** Deny microphone, use notes normally, enable logs for a dummy issue, filter/export/delete and inspect the resulting local file.

**Acceptance / remaining evidence:** Verify opt-out, readable levels/time range, explicit delete confirmation and exported content minimisation. Security of all native dependencies was not certified here.

Frames: AM29, AM30

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/Diagnostics.java; mynotes/runtime/src/main/java/app/space/notes/runtime/DiagnosticLogView.java; mynotes/app/src/main/AndroidManifest.xml

## X33 — Smoothness, speed, footprint and reliability
P1 | Measured APK payload only / live performance unknown

**Scope:** Cold start, typing, drag, file open, search, crashes and memory

**Evidence:** The signed APK receipt is 155,195,444 bytes; the verified Obsidian APK is 14,511,986 bytes. Our retained report explicitly has physical_acceptance=false and recognition targets not all met.

**Comparator/fairness:** APK size ratio is about 10.7:1, not installed storage, memory, startup latency or a causal explanation of bugs. A static Obsidian reading frame cannot establish a speed winner.

**Judgment:** Your complaints of sluggishness/bugs remain unresolved inputs. No new app-crash count, task latency, jank trace or matched user completion rate was measured.

**Direction, not implemented:** First define common fixtures and success criteria. Separate bundled-model payload from startup/interaction work; do not trade away offline features solely to chase a tiny APK.

**Manual task — NOT RUN:** On matched hardware, cold start, create/type, search a large library, drag a distant card, open a large PDF and recover from a controlled failure.

**Acceptance / remaining evidence:** Collect touch-to-visible timings, frame trace, dropped frames, memory and exact failure logs. Distinguish timeouts/socket failures from app crashes and do not publish a fabricated global score.

Frames: AM31, O07

Sources: mynotes/docs/candidate69-markup-followup.md; mynotes/docs/module-e2e/42021523354/report-summary.json; mynotes/docs/comparisons/obsidian/audit/42182162974/README.md

## X34 — Core modules, community plugins and extensibility
Not ranked | Version-pinned module census / no plugin execution

**Scope:** Enable/disable, configuration burden and trust

**Evidence:** Our inspected app bundles first-party tools; an Obsidian-equivalent user-installable plugin marketplace is not established in this scope.

**Comparator/fairness:** Obsidian core toggles and community-plugin warning are visible. The audit records 29 internal module definitions, 28 exposed controls; editor-status is not an extra exposed toggle.

**Judgment:** Plugins change functionality and risk, not automatically UX quality. Missing a third-party extension is not a core-app defect in this comparison.

**Direction, not implemented:** Keep default flows usable without setup. Treat optional complexity and code execution as separate consent decisions, not onboarding requirements.

**Manual task — NOT RUN:** Find a core module, understand its description, enable/disable it and identify changed controls; inspect restricted/plugin trust text without installing unknown code.

**Acceptance / remaining evidence:** Verify reversible configuration and a stable default note workflow. No community code was installed or executed; unknown plugins and future modules are outside this pinned-version inventory.

Frames: AO13, AO14

Sources: mynotes/docs/comparisons/obsidian/audit/42182162974/walkthrough-feature-inventory.json

## X35 — Sync, Publish, account and service boundaries
Not ranked | Account/paid/disabled workflow not tested

**Scope:** Local-first use versus optional cross-device/public services

**Evidence:** MyNotes explicitly presents itself as local/no-account. No matching built-in multi-device sync or publishing service was established.

**Comparator/fairness:** Obsidian Sync sign-in is visible; Publish was disabled in the retained module inventory. Account/payment-dependent behavior was not executed or accepted.

**Judgment:** These are optional service-scope differences, not evidence that Obsidian synchronises perfectly or our local notes are empty/defective.

**Direction, not implemented:** Do not bolt on cloud services to win a feature table. Explain local storage, backup and transfer clearly; assess a service only if the product requirement changes.

**Manual task — NOT RUN:** For a future explicitly authorised service study, evaluate offline edits, conflicts, account loss, export and deliberate public visibility with disposable data.

**Acceptance / remaining evidence:** No login, purchase, upload or publishing was performed. Conflict resolution, encryption, recovery and cost/value remain untested; an entry screen is not a passed sync task.

Frames: AO15, AO16

Sources: mynotes/docs/comparisons/obsidian/audit/42182162974/README.md; mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:renderModelInfo

## X36 — Slides, presentation and reading projections
P2 | Different output scope / visible comparator viewport constraint

**Scope:** View a note as a presentation without changing its source

**Evidence:** Our PDF export and PPTX text extraction are different operations from presenting a Markdown note. No equivalent built-in slide-show workflow is established.

**Comparator/fairness:** Obsidian shows a Slides title screen, but the software keyboard occupies much of the viewport. This captured constraint prevents claiming a polished full-screen presentation session.

**Judgment:** Presentation is a separate output job. Do not count PPTX extension support as presentation editing or a title screen as successful end-to-end presenting.

**Direction, not implemented:** For current shared output tasks prioritise readable exports. If presentation becomes required, validate full-screen entry, controls, source preservation and return state.

**Manual task — NOT RUN:** Present a multi-slide disposable note, navigate, dismiss the keyboard, rotate and return to editing; compare PDF reading only as a separate output route.

**Acceptance / remaining evidence:** Verify every slide, media, controls and return location. Obsidian landscape was not qualified by its retained audit; no physical presentation run occurred here.

Frames: AO17, AM28

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/CommonFileText.java; mynotes/runtime/src/main/java/app/space/notes/runtime/PdfExporter.java; mynotes/docs/comparisons/obsidian/audit/42182162974/walkthrough-feature-results.json

## X37 — About, models, licences and honest help
P2 | Expert inference

**Scope:** Version identity, offline model claims and troubleshooting guidance

**Evidence:** Our About/model source describes bundled OCR/speech, optional experimental adaptation, disk/RAM costs and separate audio export. This is implementation disclosure, not a new training or accuracy result.

**Comparator/fairness:** Obsidian General/settings exposes version/help/account and licences. The inspected screenshot reference pins 1.15.0/code396 rather than claiming all future versions behave alike.

**Judgment:** Help should answer how to do a task before presenting internal engine detail. Existing limitations must stay available, not disappear in a cosmetic simplification.

**Direction, not implemented:** Keep concise task-first help and progressive technical/licence detail; show exact version and actionable permission/storage recovery routes.

**Manual task — NOT RUN:** Ask a novice to identify where notes are saved, how to protect audio and how to export useful diagnostics without sending private notes.

**Acceptance / remaining evidence:** Observe whether they can explain the result in their own words. Do not substitute long notices or a generated guide for successful comprehension.

Frames: AM32, AM33

Sources: mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:renderModelInfo; mynotes/docs/candidate69-markup-followup.md

## Retained Obsidian modules

- file-explorer → X01; historical enabled=True
- global-search → X01; historical enabled=True
- switcher → X01; historical enabled=True
- graph → X07; historical enabled=True
- backlink → X07; historical enabled=True
- canvas → X11; historical enabled=True
- outgoing-link → X07; historical enabled=True
- tag-pane → X08; historical enabled=True
- footnotes → X08; historical enabled=True
- properties → X09; historical enabled=True
- page-preview → X07; historical enabled=True
- daily-notes → X10; historical enabled=True
- templates → X10; historical enabled=True
- note-composer → X30; historical enabled=True
- command-palette → X29; historical enabled=True
- slash-command → X29; historical enabled=True
- editor-status → X02; historical enabled=True
- bookmarks → X26; historical enabled=True
- zk-prefixer → X10; historical enabled=True
- random-note → X10; historical enabled=True
- outline → X08; historical enabled=True
- word-count → X08; historical enabled=True
- slides → X36; historical enabled=True
- audio-recorder → X19; historical enabled=True
- workspaces → X28; historical enabled=True
- file-recovery → X25; historical enabled=True
- publish → X35; historical enabled=False
- sync → X35; historical enabled=True
- bases → X09; historical enabled=True

## Detailed first-two-module findings
See OurApp_vs_Obsidian_Phase1.md and the PDF appendix. Companion manifests contain full source paths and SHA-256 values. Module inclusion is not functional acceptance.