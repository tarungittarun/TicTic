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

### Candidate62 measured result and remaining API26 failure

Run 41771337080 built the genuine 0.62.0/code62 development APK, 155169768 bytes
(788 bytes over v61, 4192 over v60), under the unchanged cap. All 50 focused native
methods passed in all four API34 UI profiles (200 outcomes). Historical audit
41772789535 found **zero improved and zero regressed observations versus v60**:
mean CER 0.014457963659729964 and 1134/1152 targets met. Against v61 it repaired
51 observations but lost v61's 45 improvements; that second comparison correctly
keeps the audit workflow red. Thus this restores the measured paper baseline,
not an across-the-board OCR/model accuracy improvement. Historical median times
(341.5/442/335ms for v60/v61/v62) are not causal phone speed evidence.

Separate v61 refinement 41767304688 failed on API26: 217/218 in the min-api26 shard,
with the original automatic-Kannada assertion reading a conjunct incorrectly
(`ಕನ್ಹಡ` instead of `ಕನ್ನಡ`). API34/native-theme success does not resolve that.
The original failing assertion remains unchanged. Candidate62's paper guard does
not alter that 2400×1000 native-resolution fixture, so this remains an open
qualification issue rather than a reason to relax the assertion.

`notes-kan-best-native.yml` is an isolated **nonshipping** Android weight study:
reuse exact candidate62 debug/test APKs, replace only Kannada fast weights plus
its explicit manifest override, retain the original Apache-2.0 notice from the
best repository, and verify all Java/native/test/permission payloads unchanged.
Best model blob 8f2e98e1d36ee516a1638522e569d2c4d11ac86d is 10233763 bytes;
repository revision e12c65a915945e4c28e237a9b52bc4a8f39a0cec, exact license blob
d9a10c0d8e868ebf8da0b3dc95bb0be634c34bfe. It runs the same 21 original OCR
assertions once per weight set on API26 and API34, no uninstall/clear-data or
assertion retries, and retains failures for BOTH phases. A failing fast baseline
keeps the research workflow red even if best improves it. This is not training,
shipping adoption, a varied-scene accuracy corpus, or peak-RAM/phone-speed proof.
The larger model may be slower and exceed the user APK budget; it is not added
to the actual user APK. Shipping weights/defaults and the 156213948-byte cap stay
unchanged. Research run 41773501883 failed; see the retained outcome below.


### Retained full62, rejected larger weights, and actual API26 probes

Full62 run **41771337080 succeeded**: 426 native methods × four profiles = 1704
outcomes, plus 200 focused outcomes; 4752/4752 inference rows retained. The
1097-page report is `../module-e2e/41771337080/MyNotes-Module-E2E-Report.pdf`.
Operational completion does **not** mean all accuracy targets met: that field is
false, physical acceptance is false, training steps zero, shipping models unchanged.

Larger-weight study **41773501883 failed**. API34 reported 20/21 with the paper
assertion reading `ಕನ್ನಡ ಭಾಷೆ\n\nಭಾರತ ಸಮಸ್ಕಾರ`; API26 exited 137 before native
methods. Per-phase raw artifacts have not yet been audited, so this is not a
claim about individual fast/best phase counts or a diagnosis of exit 137. No
larger model was adopted.

Diagnostic 41775792629 compiled successfully but failed before tests because its
API26 setup incorrectly assumed a Pixel launcher. Native-only **41776598360**
reused that compiled test APK, removed that assumption, and used the established
root → radio provisioning → unroot handoff. It reached the original strict
assertion, reproduced `ಕನ್ಹಡ` versus `ಕನ್ನಡ`, and retained all 12 raw-source probes.
Fixtures executed as uid 2000, not root; no assertion retry or reference substitution.

Those probes demonstrate why confidence cannot select output alone. Combined
languages with adaptive thresholding scored 95–96 yet omitted the first word.
Kannada-only ordinary and adaptive passes agreed on the full phrase (ordinary
confidence 70, adaptive 96 for PSM3). These are raw-source diagnostics, not a
controlled end-to-end accuracy comparison or a prediction for arbitrary photos.

An **unqualified source experiment** adds a bounded consensus fallback for
already-native-resolution, readable-paper, multi-line Kannada blocks only.
Both isolated ordinary/adaptive outputs must agree, retain every word position,
align source boxes, and preserve digits/punctuation; stale boxes and mixed-script
text are rejected. The displayed text and selectable boxes come from the same
chosen pass. There is no vocabulary/reference substitution, weight addition,
training, new release APK, or demonstrated speech/phone-speed improvement.
The original API26 assertion is unchanged. Run **41777249420** stopped at its JVM gate (348/349): a synthetic single-line
input with two-line boxes exposed a missing text-line check. The policy now
requires both multi-line text and geometry. Native inference did not run there.
Broad regression/size/latency gates remain required
before adoption. Root Python coverage is not a substitute for its JVM/native gates.


Revision 52e8f19f / run **41777570980** passed the JVM/build/lint gate but failed
its native assertion at **source preservation**, after text and word-selection
assertions passed. Source generation changed 272 → 278. This does not establish
which bytes changed or qualify the full fixture loop. The extra native passes
had been handed the original bitmap; the repair gives them a bounded disposable
ARGB copy instead. The original generation/pixel assertion is retained. Downstream
full-class gate 41777705341 correctly refuses that failed prerequisite. Probe
annotations are now emitted as one bounded report to avoid GitHub's ten-notice
limit; the older retained annotation export explicitly says it has only ten of
twelve cases and points to the complete original artifact.
