# Physical viewer feedback: next-source work, not delivery approval

The user's 0.30/code33 feedback supersedes previous synthetic viewer usability
confidence. The private diagnostics remain outside Git and are not uploaded to CI.
They show a Word render failure at the oversized-line/image fit branch. This does
not establish a corrupt Word source. Earlier retained spreadsheet extraction errors
must not be mistaken for a new grid-parser failure in the current version.

## Prepared changes

- Universal search hint: **Search all content & settings** in both workspaces.
- Standalone Activity retains its live UI for orientation/screen geometry changes.
  Parsed workbooks, current query/sheet/scroll and PDF resources are not deliberately
  discarded on rotation. Font-scale and theme changes still use normal recreation.
  This is not process-death restoration. PDF page sizing responds to available width.
- Preview Back targets capture the actual opening context: Home, viewed history,
  an editor or Storage. PDF/Word/spreadsheets no longer unconditionally enter Storage.
- Oversized DOCX inline lines/images scale to the available empty paper body, preserving
  their source text and subsequent paragraphs instead of rejecting the preview.
  Approximate layout warnings remain. Legacy binary .doc still needs another reader.
- PDF pinch zoom (1–5x), panning while zoomed and double-tap reset. This magnifies the
  bounded raster; it is not arbitrary-resolution/vector zoom. Normal unzoomed dragging
  remains RecyclerView scrolling. Recycled pages reset zoom.
- Visible Select page text and Share original controls use the existing 48dp icon design.
  Long-pressing a page opens selectable embedded text; direct glyph selection/highlights
  over the raster are NOT implemented. Scanned pages still need OCR. Sharing grants
  read-only original access, not write access, and invokes the Android chooser.
  Spreadsheet sharing uses the same host operation.
- OCR selection opens immediately after recognition. Tap words/drag an area/pinch and
  choose selected text or an image crop. Selection alone does not save or alter notes;
  existing review/explicit-save fences and original pixels remain authoritative.

## Qualification

Added Android regressions for actual two-pointer page zoom/double-tap/reset,
oversized Word images followed by text, real SAF spreadsheet rotation with query
retention/original bytes/no import, caller-aware PDF Back, and selection-first review.
Current inventory:381 owned native methods and294 JVM cases. Added five host-side
safety contracts; these are source checks, not Android or physical acceptance.

Candidate version is0.31/code34 with unchanged package/signing, no Internet permission
and no data clearing. Actions-only compilation/native qualification is required before
handoff. No new physical speech/playback/emoji accuracy claim is made. The trained
speech engine, Vosk default and OCR weights are unchanged by this feedback round.
