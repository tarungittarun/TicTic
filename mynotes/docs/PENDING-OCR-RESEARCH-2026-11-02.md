# Offline OCR pending work: inspected source and safety plan

This is research/preparation, NOT a trained or shipped emoji recognizer. No private
image, document or recording was read/uploaded. Runtime weights are unchanged.

## Public emoji asset candidates (GitHub primary sources)

The reproducible `mynotes.tools.ci.emoji_asset_audit` reads complete pinned tree
metadata and the already pinned Unicode17 emoji-test Git blob, not image pixels.
Result:4009 SVG filenames;3944/3944 fully-qualified Unicode17 sequences each have
one presentation-selector-normalized filename candidate,0 missing,0 ambiguous.
**This is not OCR coverage/accuracy.** No training steps or inference were run.
No SVG/image/font payload was downloaded by this audit. Merely mapping names does
not verify the graphic contents or license of every individual graphic.

- Candidate: [jdecked/twemoji](https://github.com/jdecked/twemoji/tree/b6b55fef1e8636b540a6d016a4729ca8cdf2e60b),
  commit `b6b55fef1e8636b540a6d016a4729ca8cdf2e60b`, root tree
  `09b56fba51275e05f0f1d2002efd069ed57298ba`.
- Inspected [graphics license](https://github.com/jdecked/twemoji/blob/b6b55fef1e8636b540a6d016a4729ca8cdf2e60b/LICENSE-GRAPHICS),
  blob `dc8853a70cfa9d7e8933c080caa147815c659fab`: **CC-BY4.0**, not the
  repository software's MIT license. Preserve creator/source/license attribution
  and transformation details in any future derived dataset/model/app notice.
- [OpenMoji](https://github.com/hfg-gmuend/openmoji/tree/626bdc684b85738ebdba73ad83c499c2eb88c5dd)
  graphics are CC-BY-SA4.0; code is LGPL3.0 per README. Share-alike/derived-weight
  obligations need resolution before mixing into a redistributable dataset/model.
- [Noto Emoji](https://github.com/googlefonts/noto-emoji/tree/99701e07d3d1fe1bdbcdb14964547f0fd592966f)
  root LICENSE at this revision is OFL1.1 (blob
  `d09d3d0e09169f6747754e6b25faf5ba5ef6402d`), while README still describes most
  images/tools as Apache2.0. Do not blanket-classify all assets as Apache2 from
  README alone; inspect exact per-directory/per-asset provenance before use.

Candidate report: `design-2026-10-29/emoji-asset-candidates-2026-11-02.json`.
Presentation normalization drops only FE0F for *candidate matching*. ZWJ, skin tone,
gender, tag and keycap components remain distinct. Asset identity remains a Git
blob, not a filename alone. Audit fails on incomplete/unpinned tree/Unicode input.

### Next gates before any emoji training

1. Pin downloaded asset blob bytes, attribution and applicable license; rasterize
   locally in a resource-limited non-network renderer, preserve sequence labels.
2. Separate emoji detection from recognition; ordinary text/symbols/non-emoji
   icons are hard negatives. Never replace existing OCR with guessed object names.
3. Split by font/art family, not random augmented copies of the same glyph. One
   style's complete filename list is not evidence for other styles or real photos.
4. Include ZWJ/skin-tone/flags/keycaps, monochrome forms, blur, scaling, clutter and
   transparent/dark backgrounds. Calibrate an explicit unknown/review outcome.
5. Publish true held-out detection/sequence-error results and CPU/memory evidence
   before integration. Current shipped Tesseract repertoire cannot emit these
   sequences; a metadata audit does not change that limitation.

## Scanned PDF implementation plan from actual runtime APIs

`LocalPdfPreview` owns a serialized renderer executor and separate text executor.
`OfflineOcr.recognizeBitmap` already requires a non-UI thread, bounds source pixels
at4MP and offers cancellable eng+hin+kan recognition. Reuse these APIs; no new model
or network permission is necessary.

- Explicit **Recognize this page offline** action only, initially one selected page.
  Never silently rasterize/OCR all1000 supported pages or scan a private library.
- Queue a bounded raster on the existing renderer executor. Use a white background,
  capped long edge and at most4MP, close `PdfRenderer.Page` before recognizing.
  Do not read the renderer from the text thread or race queued document close.
- Keep OCR distinct from embedded text: reviewed/possibly wrong OCR is not a real
  PDF text layer. Original file remains byte-identical and shareable.
- Review/select before copy/insertion. No automatic Note/Doc creation or edits.
  Cancel/Back/close must stop Tess and recycle every bitmap exactly once.
- Ownership trap: current image-dialog Keep callback runs *after dismiss*. An OCR
  scan cannot be closed on dismiss while that callback still needs its pixels;
  retain/transfer ownership before dismiss, then release after copying/async work.
- Tests needed: scanned synthetic PDF with real Tesseract result, empty/uncertain
  page, cancellation, close during render/OCR, page-rotation geometry, crop bytes,
  unchanged original, no implicit save and no leaked temporary files/bitmaps.

Direct PDF glyph selection is separate work: PDFBox text coordinates, rotated
pages, bidi, ligatures/surrogates and zoom/pan need verified mappings. Never fake
an exact glyph overlay by evenly dividing a line's text; retain the truthful
page-text-panel fallback when geometry cannot be established.
