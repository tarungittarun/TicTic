# Real-world recognition work — 4 November 2026

Status: development changes, NOT qualified or published as a new APK.

## User-observed failures
- Mixed-style, dark launcher screenshot: missing crash-dialog title and actions.
- Tiny status-bar selection: nonsense substituted for icons and text.
- Zooming the viewer does not rerun OCR at a larger recognition resolution.
- Speech accuracy/latency and word-heavy confusing interface need improvement.
- Existing APK approximately 155 MB; do not add another large bundled model blindly.

User attachments remain outside Git and CI. No private-image upload, training or
cloud inference. CI fixture is an own-authored dialog recreation, not the attachment.

## First bounded preprocessing change
Replace longest-edge-under-1000 enlargement with short-edge-aware enlargement:
up to 2x, no more than 4 million recognition pixels, target short edge 1200.
Floor raster dimensions to preserve the pixel budget. Preserve source pixels and
normalized word-box coordinates. A 705x1568 screenshot now gets recognition pixels
rather than only visual zoom. This does not recover information absent from the
source or distinguish arbitrary status-bar icons. No accuracy gain claimed yet.

Tests: five JVM scale/budget tests; native raster bound test; native mixed-style
crash-dialog title/action recognition test. Native inference must run in Actions.

## Next gates before shipping
1. Execute native tests before claiming this fixes the reported case; measure time
   and memory. Retain existing blank/script/arithmetic/selection regression tests.
2. Evaluate local crop re-recognition (not just crop of existing word boxes), dark
   local backgrounds, and uncertain icon/text rejection without deleting valid text.
3. Freeze varied screenshots, photos, mixed fonts, blur/glare/rotation, independent
   heldout scenes and per-region references before model selection. Do not equate
   more synthetic system-font samples with real-world accuracy.
4. Audit the APK ZIP by model, ABI and assets. Compare compact quantized replacement
   candidates with existing weights; never stack models without a measured benefit.
5. Speech: measure cold load, file verification, decode and total time separately;
   compare raw WER on independently referenced Indian-English/noisy/quiet speech.
   Preserve audio, model integrity and cancellation; do not replace acoustic errors
   with invented punctuation or dictionary-based guesses.
6. Interface: concise image-led reading surface, obvious area selection/retry/copy,
   plain-language uncertainty, stable controls and reduced movement. Preserve
   accessible labels, 48dp targets, selection and landscape support.
7. Close the existing missing landscape OCR lane and v59-to-v60 historical
   comparison separately; previous report is not all-green evidence.

No new trained/quantized weights, speech accuracy improvement, physical acceptance,
new APK size or completed interface redesign is claimed by this document.
