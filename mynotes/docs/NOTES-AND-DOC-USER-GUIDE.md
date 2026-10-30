# Notes and Doc

## Two places, one app

**Notes** is the simple notes/checklist workspace. **Doc** is the rich-document workspace.
Use the two tabs on the home screen, or choose either destination in Library.

- **Notes → New note:** start typing immediately in the familiar editor.
- **Notes → + Checklist:** create an ordinary checklist.
- **Doc → New Doc:** start a rich document, with its writing area ready.
- Existing rich documents appear in Doc. They have not been flattened or rewritten.
- Using Doc never changes the way the next simple note/checklist is created.

Folders, Archive, Trash and backups remain part of the same local library. Doc cards are
marked **Doc**. Search on the Notes/Doc home screen searches the current workspace.

## Keep a note and make a rich copy

Open a simple note → **More actions → Copy to Doc → Create Doc copy**.
The original stays in Notes with its original identity/content. The separate Doc copy
can be edited independently. This is not a destructive upgrade or a conversion of all notes.
Backups retains the verified first document-editor recovery checkpoint; it is not a new
per-copy version archive. Export your own backup before major edits or installing updates.

## Write in Doc

The page contains your title and writing—not a management header above every text section.
The toolbar exposes **+**, **Aa**, **☑**, **Labels** and **Colour**. At larger text sizes it
uses two rows rather than hiding tools sideways. The top bar provides Back, save state,
Undo/Redo and **⋯ Document**. In a very short keyboard viewport, the lower **⋯** keeps
Document actions reachable, including Undo/Redo.

- Tap in text, then **Aa**. The sheet says **Selected text** or **New typing**.
  Bold, italic, underline, strike, headings, font and size remain available.
- Select lines, then **☑** to make a named checklist without converting surrounding text.
- Tap a checklist caption for Rename, Move, insertion, removal or Hide/Show checked.
  Item grips stay before checkboxes; **+ List item** remains direct.
- For content insertion, first place your cursor, then **+**. Table/image/drawing/diagram
  insertion retains that anchor. Reminders and hyperlinks are also available here.
- Tap **Continue writing…** below an object where no writing area exists. A repeated tap
  reuses the following text area instead of repeatedly adding empty sections.
- If every section was removed, **Start writing** creates a fresh writing area without changing your title.
- **Document → Writing lines** changes ruling independently of the background.

## Tables

**+ → Table** starts with an editable 2×2 table and focuses its first cell.
Tap a cell → **Cell actions** to add a row above/below or a column left/right.
Deletion asks for confirmation and remains undoable. **Header row · Off/On** changes the
first row's role without injecting bold formatting into stored cell text. PDF displays
header emphasis. Header repetition on every PDF page is not promised.

Deleting the first row removes its header role. To insert above a header, first turn the
header role off. Wider tables scroll sideways; the page provides an overflow cue.
The position-based table structure controls remain as a secondary fallback.

## Visuals, links and movement

Images/drawings have bounded aspect-ratio previews. Diagrams show a bounded inline vector
preview instead of only a shape-count button. Tap a preview to select it and open content controls; choose **Edit** for its dedicated
editor. Its caption/handle also exposes naming, insertion and movement controls.

**+ → Hyperlink:** selected words get a web address. At an ordinary caret, enter **Text to
display** and a web address. At an existing link, choose Open, Edit or Remove. Removing a
link keeps the words. Only safe HTTP/HTTPS addresses are accepted; opening an external
browser asks for confirmation.

Drag a content handle to a boundary, or onto a writing line. An insertion line previews
the destination; edge scrolling helps with long documents. For a non-drag route, choose
**Move content** (or **Move checklist**) → a labelled destination → confirm. The writing-line
placement route also has **Cancel move**. Text section controls are available through text
selection; normal text does not permanently show its handle. Row handles offer a Move item
destination list as well as indentation/removal. Moving retains the original object and
its formatting/attachments; it does not create a flattened copy.

## Data and qualification boundary

Do not uninstall/clear app data to install a development update. Do not downgrade after
saving newer rich data: older APKs reject metadata they cannot preserve. Keep an exported
backup outside the app. Processing and existing permission/diagnostics boundaries remain
local/opt-in; no cloud service has been added for this separation.

This guide describes the implementation under qualification, not proof of usability.
See `NOTES-DOC-PLAN-AND-JOB-LEDGER.md` for exact source/run results and remaining checks.
A real-device novice walkthrough is still required before calling the experience excellent.
