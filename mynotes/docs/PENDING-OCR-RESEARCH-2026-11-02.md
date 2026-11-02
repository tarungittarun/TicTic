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


## Indian-English speech data follow-up (no training/download)

Inspected90 public `resources/*/info.txt` blobs in the historical OpenSLR web
source [danpovey/openslr at91932a90](https://github.com/danpovey/openslr/tree/91932a90b4b94d8f8629cda4fd10b405b113ab30).
This is a limited historical repository snapshot, not a claim about today's whole
OpenSLR catalog. SLR83 is explicitly UK/Ireland dialect data, NOT Indian English
(`info.txt` blob25b0a6872b8aedb33fcd1153c04eb59dd349a0aa). SLR37 is Bengali
TTS, not evidence for broad Indian-English ASR. Neither was silently substituted
for an Indian-English training corpus. Only small metadata files were read.

Rechecked primary [Svarah README](https://github.com/AI4Bharat/Svarah/blob/2c644dc4f67da2aae63a0bd9ceb2c6f8679ff05c/README.md):
9.6h/117 speakers/19 states, a useful *evaluation* benchmark; its linked download
is still Hugging Face. The README itself supplies no dataset license grant. Prior
gated-access/license audit is not resolved by the code repository or paper.
Do not bypass gates, use benchmark data as training or claim its published model
WER numbers belong to MyNotes.

[NPTEL2020 README](https://github.com/AI4Bharat/NPTEL2020-Indian-English-Speech-Dataset/blob/64f8d5ef7c2346563779c0bd7e8487f7739be482/README.md)
explicitly describes noisy transcripts, education-domain South-Asian English,
random splits,15,700h/1.1TB. It links a public1000-example manually annotated
Pure-Set GitHub release asset (`v0.1`, asset37231585,
`nptel-pure-set.tar.gz`,188815224bytes). **Audio was not downloaded** and individual
recording rights/speaker metadata are not established here. Generic YouTube
Creative Commons linking is not a complete per-recording rights manifest. This
is a possible bounded *evaluation* lead once provenance/license are established,
not approval to train/redistribute weights. Unknown base-model pretraining overlap
and random/non-speaker-disjoint splits must be disclosed in any eventual report.

No new speech experiment, training step, broad-accent score or physical playback
acceptance occurred in this increment. Existing one-speaker KSP pilot remains
exactly that, not broad Indian-English coverage.

## Actual public PNG-byte audit and queued CPU feasibility experiment

Downloaded only the pinned10,682,087-byte public Twemoji source archive, verified
SHA256d91bdab1e9ac66f2935425e539f5ac1cd83a491fa27b2021c77630a0ba8e09ff, graphics
license Git blob and all3944 required PNG Git blobs against the pinned complete
tree. No private content. PNG-byte identity report is
`design-2026-10-29/emoji-bitmap-identity-2026-11-02.json`.
There are3941 distinct PNG blobs for3944 sequences: France/Clipperton/St Martin
share byte-identical flags, and Norway/Svalbard-Jan Mayen share another. Thus exact
country-code recovery from these pixels alone is inherently ambiguous; a truthful
recognizer needs alias choices/context, not an invented single answer.

One separate CPU-only public-graphics feasibility job is requested, not yet a
success: all3941 alias classes, Twemoji-only fitting, capped three epochs with
measured steps/changed weights/loss;64 separate-art-style OpenMoji glyphs never
used for fitting. CC-BY4 training and CC-BY-SA4 evaluation attribution/full licenses
are retained. No private images/recordings, GPU/paid training service, runtime
weights change, public release or universal-accuracy claim. Experimental weights
remain an Actions artifact only. These cropped-glyph results cannot qualify
detection, cluttered photos, unknown rejection or phone performance.

## Measured pilot outcome — run41457619334 (not approved)

The earlier metadata-only/queued statements describe those earlier audits, not
this completed experiment. Exact public report, aliases and full graphics notices
are now retained in `emoji-pilot/41457619334/`, collected by run41458393371.
Source: `c1da367f982688f22f9bff3e740529847d1b1d14`.

Actual CPU optimization:741 steps,47292 augmented training instances,3 epochs,
686261/686261 parameters changed,59.739144297 training CPU seconds. Losses:
8.3026415690,8.2892703345,8.2888207146. **This is a negative learning result, not a
useful recognizer:** clean *seen-style, not held-out* top1=1/3941, top5=5/3941;
separate-style OpenMoji top1=0/64, top5=0/64. Successful workflow execution and
changed parameters do not establish useful learning or improved accuracy. The
64 examples are only a small separate-style sample, not universal evaluation.

Do not ship/adopt these weights, increase confidence labels, or change the shipped
OCR based on this run. Before another optimization experiment, establish useful
baselines and inspect feature/optimization collapse; then require meaningful
seen-style learning, family-disjoint generalization, explicit unknown/hard-negative
calibration, bounded mobile inference, and photo/detection tests. More steps alone
are not evidence. Pixel-identical flag aliases still need ambiguity/context handling
regardless of learning quality. No private data or runtime model changed.
