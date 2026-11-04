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

## Follow-up implementation (unqualified until native pilot completes)
- Source decode now preserves a 1200x2670 screenshot (3.204 MP) rather than
  halving both axes solely because its height exceeds 2400; 4MP remains the cap.
- Selected-area inference runs freshly on source pixels with bounded worker queue,
  source retain/release, cancellation on dismissal, and an editable confirmation.
  It does not mutate stored notes or replace full-image word coordinates.
- Preview selected text before using it; simplify help; remove entry translation
  and drag scale changes while preserving system animation preferences/haptics.
- Separate speech install/verification, model load and decode timings; add
  cancellation checks within model hashing/unpacking without weakening hashes.
- Byte audit reports compressed model/ABI contributions. No added model weights.
- Strict pilot inventory gate rejects missing/duplicate/failed tests even if adb
  returned zero. Missing output is a failure, not a pass.
- Retrieved original missing landscape job logs: sdkmanager emulator download
  failed with `Error on ZipFile unknown archive` before launch. New provisioning
  validates the runner-provided emulator and skips only unnecessary emulator
  upgrades, forwarding all other SDK installations normally.

A private local desktop English-only WASM Tesseract experiment found that full
image enlargement alone still missed the dialog while a tight region read it.
Those private pixels/results are NOT committed or uploaded and are not Android
qualification, model training or evidence of general accuracy. This motivated the
area re-read implementation rather than a claim that scaling solved the issue.

## Observed red native evidence and follow-up
Strict pilot 41764909944 / light-landscape job 126362188800 executed 45
methods: 43 passed, two failed. Kept exact assertions:
- `mixedStyleDialogScreenshotKeepsTitleAndActions`: full-image recognition empty.
- `automaticBundledRecognitionNeedsNoLanguageChoiceAndKeepsSymbols`: the retained
  tilted Kannada document helper regressed after raster enlargement.

Do NOT call earlier process-exit-only success qualification. Subsequent strict
pilot reports are authoritative. Repair preserves documents whose short edge is
already at least 1000px; does not shrink test expectations. An adaptive threshold
pass is tried ONLY if all ordinary passes returned empty, never to globally
rewrite already-recognized document text. These changes await native evidence.

Speech quantization study 41765677553 uses exactly the existing adapted export,
verified original artifact and source hashes, desktop whisper.cpp at the same
pinned revision, fixed B0033-B0064 public prompts on KSP/SLT (64 utterances), f16
versus q8_0 versus q5_1, identical greedy decoding/two threads. This is model
quantization parity research, not new training, Android timing, default Vosk
comparison or unseen-Indian-speaker evidence. Outputs retain individual hypotheses,
WER and regressions. No quantized file is installed in the app by this work.

## Candidate61 qualification boundary
Version 0.61.0/code61 marks the actual source changes, not a renamed old APK.
The full module workflow must run all 425 native methods across four profiles,
original OCR/voice benchmarks, JVM coverage, lint and module-ordered reporting.
Release APK growth is gated at 156,213,948 bytes (the previous 155,165,372-byte
qualified release plus 1MiB). No model weights were added or replaced in61.
The Q8 candidate remains research-only despite its promising parity/size results.
Physical acceptance and broad real-scene/unseen-speaker accuracy remain open.

## Nonshipping Q8 Android compatibility probe

`notes-q8-native-smoke.yml` reuses candidate61's exact compiled debug/test foundation
(run 41767304608, source a3833061, artifact 13292844719) rather than rebuilding or
changing the user APK. It quantizes the existing F16 asset with the pinned desktop
quantizer and rejects any Q8 bytes other than the independently retained study's
43,550,778-byte SHA256 65fb6de21bcb73d3ff8f86ea5d8a3da65a25df913f731af60e5b7b952c426d3c.
Only the model and its install checksum/length manifest may change. All Java,
native libraries, resources, package/version and test payloads must remain byte-identical.
The workflow accepts debug packages only and re-signs the three research APKs with
one ephemeral key; these are **not user release APKs**.

Two fresh emulator guests each run the original three TrainedWhisperTest methods
once for F16, then install Q8 over it without uninstalling/clearing data. A public
marker must survive; the existing public recording-preservation, cancellation and
worker-death assertions remain mandatory for both models even if adb reports zero.
This is only a compatibility/safety smoke, not a broad paired accuracy benchmark.
Its public-fixture timing includes first installation/verification and is neither
isolated decode latency nor a physical-device speed claim.

Known adoption blocker: model installation uses checksum-named files. An upgrade
can leave the old 77MB installed model alongside Q8. The probe explicitly retains
model-directory disk usage; APK shrinkage alone is not an installed-storage win.
No cleanup policy, model adoption, default-engine change or new training is made
here. Independent Android accuracy, paired decode latency/peak RAM, cache migration
and physical acceptance remain required. Results are pending until the workflow
has actually executed; root derivative/identity tests are not Android evidence.

### Proven host disk exhaustion, not an OCR assertion failure

Read-only diagnostic 41769079870 retained the original fatal lines for jobs
126365615277 and 126366665061: available 6533.30 / 6061.37 MB versus 7372.80 MB
required to create the default emulator userdata partition. Both executed zero
native tests. CI now requests an explicit 4G userdata disk (rather than the larger
image default); this changes only the disposable emulator, not app storage rules,
APK bytes or assertions. Existing running jobs retain their original config.
A native-only rerun can reuse the exact candidate61 foundation without rebuilding
another user APK. Full qualification still requires actual completed results.

### Executed compatibility result (not adoption)

Run https://github.com/tarungittarun/TicTic/actions/runs/41769080104 completed
successfully: prepare job 126374272768, native dark-landscape job 126374923615,
native light-portrait job 126374923706. Each native job independently required all
three existing TrainedWhisperTest assertions for F16 and Q8: **12 successful test
executions total**. This establishes that the exact previously studied Q8 model
can load/infer through the existing Android x86_64 private worker and pass the
public original-audio, cancellation and worker-death smoke assertions. It is NOT
12 independent speech samples, an Android paired accuracy corpus, ARM/physical
qualification, decode-latency proof, or an adopted/shipped model reduction.

Supplemental native replay 41769343840 reuses original candidate61 foundation with
explicit 4G disk and sparse source checkout. Voice-only replay 41769511821 repeats
only the light-portrait voice0 lane blocked in job 126370468181; the diagnostic
also confirmed its fatal 6061.23MB-available / 7372.80MB-required disk error.
Original full run 41767304608 remains failed in that lane even if supplemental
runs later pass. The other 11 original benchmark lanes finished successfully;
operational success does not imply every accuracy target was met. Supplemental
results remain pending here until independently verified completion.

## Candidate62: repair measured interpolation regressions, not a tuned score gate

Historical audit 41770347277 independently rescored 1152 matched case/profile
observations from v60 and v61. Operational errors: zero for each; accuracy-target
passes: 1134 for each. Mean CER worsened from 0.014457963659729964 to
0.015758643781741608; 45 observations improved and **51 regressed** (17 input IDs,
repeated across the three available v60 profiles). The audit correctly FAILED.
The missing fourth historical profile was excluded for absent artifact, not
accuracy. This is synthetic historical evidence, not fresh same-guest A/B or
1152 independent photos. v61 is not an established overall accuracy improvement.

Every regression was on a native 1600×700 paper-style input that the new
short-edge rule interpolated unnecessarily. One lost the first Kannada line
(ocr-0268); other failures changed Kannada letters, arithmetic symbols/spacing or
inserted a space in an email address. The source-preserving decode for actual tall
screenshots is retained. Candidate62 adds a bounded, source-only paper/row-ink
estimate: already readable ink stays native, while tiny ink and unknown scenes
keep the bounded enlargement fallback. This uses neither case IDs, reference
text, language predictions nor dictionaries. It samples 1024 background pixels,
then uses one reusable source row plus a bounded ink-row projection; no extra
full-image copy or model assets. The estimate is a preprocessing heuristic, not
an accurate scene-text detector or universal quality guarantee.

A new native test reproduces the exact public italic-monospace Kannada paper
case in both polarities, requiring native raster dimensions and BOTH lines.
Original popup, small/tall-image, native inference, symbol and region assertions
remain intact. The module inventory increases from 425 to 426; the pure JVM test
adds readable/tiny/solid/noisy/diacritic row cases. Candidate62 needs its own actual
Android build, original-corpus comparison and full qualification; root tests
alone do not prove the repair. APK cap stays 156213948 bytes; model weights and
default speech engine remain unchanged.

Executed v61 evidence: all 425 native methods in all four profiles completed
(1700 outcomes); supplemental 41769343840 passed 49×4=196 focused outcomes.
Supplemental voice run 41769511821 completed all 402 measurements, with only
312 meeting their accuracy targets. Together with the original eleven lanes,
all 4752 planned observations exist across explicitly separate runs. The original
full run and its 1097-page report still correctly retain their missing 402-case
lane and failed/unqualified status; no historical result is relabelled green.
