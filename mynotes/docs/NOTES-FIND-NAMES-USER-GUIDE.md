# Find and content names — Notes and Doc

## Find inside a created note

Open a note → **More actions (⋮) → Find in note**.
Type the words you want. Matching is literal and case-insensitive, not a regular expression.
Results include title, writing, checklist items and displayed content captions. Doc also
searches its editable table cells through **Document (⋯) → Find in document**.

Tap a result to jump to it. Writing matches are selected in the editor; caption matches
scroll to their caption and are announced for accessibility. Find does not replace text or
add permanent highlight formatting. Close Find with **×** or Android Back.

If the keyboard leaves little room, press its **Search** action to hide it and see the
results. At most100 results are shown; refine the query when it says100+. Queries are
limited to256 characters. This searches stored/displayed text, not pixels/OCR inside images,
audio or labels hidden inside a diagram editor. It is not a Replace tool.

## Checklist back to text

In Notes: **Checklist options → Convert to text**.
Generated checkboxes disappear: an item `Milk` becomes `Milk`, not `[ ] Milk` or `[x] Milk`.
Indentation and line boundaries stay. Brackets actually typed into an item are your text
and are not deleted. **Undo** restores the checklist, check states, order and indentation.

In Doc: tap the checklist name → **Convert to text**. Its caption becomes a text line;
indentation and rich row formatting survive. In a short keyboard viewport use
**Document (⋯) → Undo** if the top Undo icon is hidden.

Sharing/exporting a checklist as plain text may still include checkbox notation. That is
separate from this explicit conversion, which removes generated checkbox notation.

## Rename inserted content in Notes

- Image: **Image actions (⋮) → Rename image**.
- Drawing: **Drawing actions (⋮) → Rename drawing**.
- Diagram: **Rename diagram**, by the diagram preview.
- Reminder: **Rename reminder** (the pencil beside its scheduled date).

Enter a name → **Save name**. Names are up to200 characters. Clear a name to return to the
default. Cancel leaves it unchanged. Saving an unchanged name does not create an Undo step.

Names change the caption, not the attachment's identity, pixels, drawing type, diagram
shapes or reminder time. They survive reopening, local/folder storage, backup and copy.
Long visual captions use up to two lines with an ellipsis; the full name is still available
in Rename, accessibility, Find, storage and backups. PDF captions wrap the custom name.
Undo reverses a rename. New reminder notifications use the reminder's custom name; Android
permission, battery policy and inexact-delivery limitations still apply.

In Doc, tap a visual or its caption → **Rename**. Reminder names can be edited through
**Document → Rename reminder** when a reminder is scheduled. The name/date is also visible
below the document title. Named visual captions remain available in read-only documents.

## Upgrade/data precautions

This update keeps simple Notes and rich Doc separate in the same APK. No automatic
conversion or flattening is introduced. Install over the existing app; never uninstall or
clear app data. Keep an exported backup and separate copies of important voice recordings.

Real content names use schema8. Unnamed notes retain the previous schema output. Older APKs
reject schema8 rather than silently losing names: **do not downgrade** after renaming.
Backup wrapper format is unchanged; names are included in its document payload. Other backup
omissions remain: original recordings, private empty folders, edit history and saved styles.

Automated screenshots/native checks do not replace a novice/physical-device walkthrough.
See `NOTES-FIND-NAMES-AND-DOC-FOLLOWUP.md` for exact qualification and remaining acceptance.
