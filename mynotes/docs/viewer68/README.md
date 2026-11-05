# Candidate68 viewer guide and acceptance scope

## Using the viewers

- **PDF/Word:** one filename toolbar contains Back, filename, Search and file options. Word rendering remains an **approximate preview**, not a claim of complete Word layout fidelity.
- **Selection:** a deliberate tap elsewhere releases selected text. Scrolling is not treated as a release. A search-result highlight is separate from a manual text selection.
- **Zoom:** choose page or whole-file scope in file options. Whole-file pinch changes document geometry across pages, not just a crop of the touched page. Raster allocation remains independently bounded.
- **Page position:** the current/total pill appears during interaction and hides after about one second of inactivity. Dragging and ordinary scrolling use the same pixel range, including within-page progress. Dragging to the bottom reaches document end; a mostly blank tail of a source page stays blank rather than being reconstructed.
- **Search:** tap Search, type and use the keyboard Search/Enter action. Previous/next move between embedded-text matches. The dock stays above the keyboard; a found glyph range is highlighted near the top of the reading viewport. Scanned pages without embedded text require explicit page recognition instead.
- **Offline recognition:** Recognize page offline shows indeterminate progress and Cancel before queued recognition work starts. English, Hindi and Kannada are supported by the existing on-device models; recognition can be wrong and must be reviewed. It does not silently create a Note/Doc or modify the PDF.
- **Markup:** pen, highlighter, eraser and undo/redo operate in marking mode. Done retains a private content-keyed draft. **Save marked PDF copy** exports separately; the original is not edited. Private PDF annotation drafts are not part of Notes backups: retain an explicit marked PDF copy when it matters.
- **Details:** View details reads actual file/provider metadata. Unavailable provider fields remain unavailable rather than being guessed.
- **TXT/CSV:** original-preserving reading, background work/status, compact filename chrome and themed readable content remain separate from explicitly saving a copy. TXT page navigation uses matching vertical previous/next controls.

## What the evidence means

`41959810821/` contains 55 public-fixture window thumbnails from the latest focused execution: 11 scene IDs in five profiles. Original PNG SHA-256 values are retained. These are actual Android window/composed-display captures, not reconstructed mockups. The native gate verifies painted IME pixel variation instead of accepting a visible-insets flag alone.

Focused execution is **not** complete module E2E, universal format fidelity or physical-device acceptance. The matching full run, exact signed-APK install-over checks, report and visual inspection must finish before APK delivery. Historical sibling folders remain labelled with their actual limitations and superseded sources.

## Physical acceptance still required

Install over the existing same-signer app; do not uninstall or clear data. Check a real long PDF, your keyboard and font scale, deliberate deselection versus scrolling, whole-file zoom on a distant page, both directions of the scrubber, cold page recognition/cancellation, marked-copy export and original-file bytes. Check actual Word/table/font layout where fidelity matters. Rotation/crop annotation export and interrupted export still need dedicated real-file coverage.

The prior supplied diagnostic log contained pending optional App-folder copies. Private-save completion and external-copy completion are different; the user's selected external folder has not been physically verified. Do not remove conflict guards, assume that copy is current or clear private data based only on emulator evidence.
