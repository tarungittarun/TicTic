# Saved typing styles and PDF export — code19

## Save and reuse a style

1. Open a document and select text whose appearance you want to reuse.
2. Choose **Format → Save selection as style**. Give it a short, recognisable name and tap **Apply**.
3. In this or another document, choose **Format → Saved styles → your style → Apply style**.
   - With text selected, it changes the selected text's supported formatting. A heading applies to its paragraph.
   - With only a blinking cursor, it sets the appearance of **subsequent typing** in that field. It does not change the existing text.

You can also choose Bold, Italic, Underline, Strike, a heading, Font or Size directly at a blinking cursor. To type without those formats, choose **Clear selection formatting** at the cursor. **Stop pending typing style** stops the explicit typing override without changing existing text; normal formatting at the cursor can still be inherited. Tapping another position follows its formatting. To change the field's existing text, select it first.

Styles capture common supported appearance, not the selected words, URLs or paragraph numbers. They can include font, size, emphasis and a heading. Applying a preset does not replace a selected hyperlink destination or paragraph numbering. Normal/unset preset properties clear those supported properties in the selected range; a preset is not an additive-only brush.

Keep up to **16 named styles**, with names of **1–48 characters**. Names are case-insensitively unique. To replace a name, delete that preset first, then save it again. Deleting a preset does **not** strip formatting from existing notes. A success message follows the disk-write acknowledgement; an error must not be treated as a successful save.

### What is retained

- Note text and formatting are saved in the existing note/checkpoint format.
- Named styles are app-private, device-local settings, separate from notes. **Note backups do not include named styles.** Do not uninstall or clear app data to update.
- Pending typing intent is scoped to the current note/field and restored with the Android editor session. It is **not** a promise of persistence after process restart or persistent Undo history.

## Export clickable links

1. Select a label and use **Format → Hyperlink** to set an explicit `http://` or `https://` destination.
2. Use **Export PDF** and choose a PDF destination through Android's file picker.
3. Open the resulting PDF in a link-capable PDF viewer. Tap/click the label to follow its destination; the viewer may ask for confirmation.

The PDF contains real URI link annotations, including supported wrapped text, page breaks and table panels. It does not merely print a URL beside the label. Plain URL-looking text is not automatically made into a hyperlink. MyNotes does not fetch destinations during export; following a link in another app can access the network and that site's normal privacy rules apply.

## Include printed writing guides, only when wanted

**Writing lines** in a document controls its on-screen writing guides. PDF printing is a separate, optional choice:

1. Turn on **Writing lines** for the document if you want its guides to be eligible for printing.
2. Choose **Export PDF**.
3. In **PDF export options**, select **Print writing guides where Writing lines is on**.
4. Tap **Choose PDF file** and select the destination.

The PDF option starts **off** in a fresh activity session. Leaving it off preserves the ordinary, unruled PDF appearance. Turning it on does not add guides to documents whose Writing lines setting is off. Cancelling the options dialog does not change the document.

## Safety and qualification limits

Export preparation is local and uses a private staging file before copying to the chosen destination. The annotation updater accepts only fresh PDFs produced internally by Android, not arbitrary imported PDFs. Unsupported producer structures or safety limits fail the export rather than silently dropping links. Limits include 200 pages, 8,192 link rectangles and 1 MiB total encoded URI data; a long wrapped link can require multiple rectangles.

Back up notes before installing a development update. Keep the same package and signer, install over the existing app, and never clear data as an update step. Automated Android and independent PDF checks are recorded separately in the job ledger and source-pinned evidence. They do not replace physical Motorola acceptance or constitute public-release approval.
