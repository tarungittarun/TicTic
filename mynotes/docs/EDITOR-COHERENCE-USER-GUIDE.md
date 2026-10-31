# One writing page, two workspaces

This is the redesigned development interface, not a claim of novice or physical-device acceptance.
Notes and Doc remain separate choices in the same MyNotes APK. Existing rich documents are not
converted into simple notes. **Copy to Doc** makes a separate copy and preserves the original.

## Notes: write first

The lower tools name what they do:

- **Insert** adds an image, drawing, diagram, scan, voice recording, reminder or checklist.
- **Format** changes writing. Select words for Bold/Italic/Underline. Headings apply to the
  current paragraph. Close the formatting tray to recover writing space.
- **Colour** changes the note colour; it does not recolour selected words.
- **Labels** organises the whole note. Tick labels, then **Save**. **Cancel** changes nothing.
- **More** contains whole-note options, including Find, Background, Move to folder and undo/redo.

Use the magnifying glass in the upper bar for **Find in note**. It is also available under More.
Type a word, use the previous/next arrows, and tap the current match preview to reveal it.
**Close find** returns to the same editor. Find never writes highlight formatting into your note.
With the keyboard open the Find controls become compact; the writing page remains underneath.

Colour, Labels, Background and Move to folder open temporary bottom trays, not new writing pages.
Cancel or Android Back closes the tray. Choosing a colour, background or destination applies it;
use Undo to reverse an applied change. Creating a folder creates the folder only: choose its row
if you also want to move the note there. An explicitly created empty folder is not removed by
cancelling the move.

On a narrow screen with large text the lower tools use two short rows rather than splitting
words or reducing your chosen text size. In a wide landscape window they remain one row when
all the labels fit. A crowded keyboard viewport prioritises the active writing field.

## Doc: text tools versus document tools

There are three distinct places to look:

1. **Format** changes selected words or, with just a cursor, the style of subsequent typing.
   The small ribbon stays open: combine **B**, *I*, underline and strike without opening a
   new menu for every choice. Paragraph, font, size and advanced formatting remain available.
   The down arrow closes formatting, not the document.
2. **Insert** adds content at the current writing position. **List** deals with lists and
   conversion of selected writing. Text, checklists, tables, images, drawings and diagrams keep
   their own identities; inserting something does not flatten the document.
3. **Document** contains whole-document choices: Find, note colour/background, labels,
   folder, writing lines, PDF and other document-level actions. It is not a selected-image menu.

For a single image, drawing, diagram, checklist or table, tap its caption/controls. The menu
identifies that content. **Edit** and **Rename** are near the beginning; remove is at the end.
Those actions affect the chosen content, not the whole document. Rename changes the caption,
not the image pixels, diagram shapes, checklist items or table data. Undo remains available.

Doc Find also searches editable table cells and displayed captions. Literal text only: it does
not search image pixels, audio or the inside of an unopened diagram. Use the query and arrows,
then Close find. There is no Replace operation. Queries are bounded to256 characters and100
matches; refine the query if it reports100+.

## Checklist to ordinary text

Notes: open **List → Convert to text** while viewing a checklist. Doc: tap the checklist caption,
then **Convert to text**. Generated checkbox marks disappear; characters you actually typed
are retained. Indentation, row order, content names and rich row formatting are preserved.
**Undo** restores the checklist and check states. Plain-text sharing is a different operation
and may still include checkbox notation.

## Safe upgrade

Install the development APK over the existing app. Do not uninstall or clear data. Export a
backup first and keep separate copies of important original recordings: note backups do not
include original audio, private empty folders, edit history or saved styles.

This redesign does not change the data schema. Content names introduced earlier still use
schema8; older APKs reject those named notes rather than silently dropping their names. Do not
downgrade after using content names. Processing remains local; this work adds no network service.

Native tests and rendered screenshots are evidence, not novice approval. A physical-device
walkthrough still needs to check keyboard/selection, large text, accessibility, gestures,
permissions, recording, file providers and reminder delivery under the actual OEM policies.
