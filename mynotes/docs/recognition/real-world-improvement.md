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


Source-copy revision **41778572752** passed build/JVM/lint, but still failed the
original conjunct assertion. Earlier text/selection success was not completion
of the fixture loop. No fixture/font-specific correction is introduced. The
next experiment separates the near-uniform background check from the old
8–128px projected-ink-band heuristic, which can exclude larger/rotated glyph
bands. The exact cause is not established by the available probes. All two-pass
agreement, source-copy, word coverage, digit/punctuation and geometry checks stay
in place. Its native gate now executes the full original 21-method OCR class
plus 384 varied and 128 frozen-reference inferences on each API, with an API34
historical same-input regression audit. These remain unqualified until executed.


### Full experimental r4: actual native results, not wrapper colour

Run **41779636217**, compiled source e1a7f9b4, passed JVM/build/lint. Its 384-case
API34 historical comparison measured **1 improved / 0 regressed / 0 operational
errors**. This is one same-input synthetic profile, not broad real-photo accuracy
or causal phone speed. Native logs recovered read-only in **41781104001** prove
**API34 23/23**; its job was incorrectly red because optional absent probe data
was not JSON. API26 was **22/23**, with the original conjunct assertion still
failing. Both APIs completed 384 varied + 128 frozen-reference inferences with
zero operational errors: API34 targets 378/384 and 124/128, API26 373/384 and
123/128. Failed assertions are retained; no test was rerun to recover these counts.

Diagnostic **41781175749** compiled and reached the same original error. Its
baseline has confidence 84, complete four-word boxes and two text lines. All
entry gates are true, including native dimensions 2400×1000, script eligibility,
and near-uniform background. Thus neither a missing newline nor the paper-entry
gate explains this remaining rejection. Candidate word geometry is being
recovered separately from the immutable artifact (GitHub truncates long check
annotations to 4096 characters). No accuracy/physical acceptance, audio speed gain,
model training, release, or completed task is claimed.


### Original API26 failure repaired and independently regression-gated

Run **41782476953**, source **5b0891a1**, is **SUCCESS**: build/JVM/lint and both
API26/API34 native jobs. Each API passed all original 21 OCR methods plus both
corpus methods (**23/23**); 384 varied and 128 frozen-reference inferences per API
completed. The original strict conjunct, word-selection and source-generation
assertions remain unchanged and now pass. API34 historical comparison retained
**1 improved / 0 regressed / 0 operational errors** across 384 hash-matched cases.

Read-only geometry recovery **41782226457** established the remaining rejection:
ordinary Kannada word boxes matched the baseline exactly, while adaptive boxes
were vertically padded. Both native texts agreed. The repair retains the tight
ordinary text/boxes together, and accepts bounded adaptive corroboration only
when it substantially overlaps the tight word, has at most three times its area,
and does not swallow another word centre. Strict original/ordinary alignment,
complete word coverage, script, digit/punctuation and source-copy guards remain.
No expected-word substitution or confidence-only selection is used.

Development **0.63.0/code63** is now the full-suite candidate. Release APK signing,
size cap 156213948 bytes, four-theme/orientation native/focused/recognition runs
and module-ordered PDF remain required. No new weights, training or public release.
The whole real-world/ASR/interface task is not declared complete by this fix.

A separate **private local desktop** experiment became possible using the binary
`tesserocr 2.12.0` wheel (Tesseract 5.5.3, Leptonica 1.87.0) and exact existing
shipping weights. Twelve probes of the supplied crash-popup screenshot stayed
outside Git/CI; no images or extracted text were uploaded. Three whole-image
adaptive probes recovered all three manually referenced popup strings; ordinary
whole-image probes did not. The initial manual-crop coordinates were taken from the resized viewer instead
of the original 1200×2670 source, so those off-target crop results are INVALID
as dialog evidence. Corrected source-mapped crops produced all three strings in
3/6 unnormalised probes and 6/6 dark-normalised probes (including all three
ordinary-threshold modes). This corrects the local experiment, not an app mapping
change. It uses Pillow approximation, not Android JNI/app selection, and does not
prove the reported popup fixed on a phone. Whole-image automatic reading still
needs qualification; global adaptive replacement loses other existing text.
Private outputs remain local; no phone-speed, physical acceptance or speech
improvement is inferred.


### Nonshipping speech speed investigation (release/default unchanged)

`CompactSpeechBenchmark` is opt-in benchmark source, not production code. The
new debug-only CPU attention study keeps the exact existing F16 weights and all
Java/resources fixed; APK payload proof permits only the three ABI native-wrapper
entries to differ. Default and release attention remain OFF, no GPU, extra model,
training, release adoption or speed gain claimed. It evaluates CMU KSP/SLT
B0065–B0100: 36 prompts × two speakers × four deterministic acoustic conditions,
plus four silence controls, across API26/API34 with balanced phase order. KSP is
not an unseen speaker. Per-row source preservation, independent raw-hypothesis
WER, partial results, separate warmups and observable wall time are retained.
Any WER regression blocks adoption; emulator timing is not phone-speed proof.

Initial research runs 41784821798 / 41784916890 stopped during preparation;
no native accuracy outcome is inferred. A repository-root package-import mistake
was reproduced locally and repaired. A missing progress heartbeat was also fixed
before the corrected native study. The integration denied cancelling the initial
queued run (403), so it is not described as cancelled. These failures remain
visible; no assertion is weakened or retried to choose a favourable transcript.

### Build63 missing-lane provisioning and split-capture provenance

The original full run `41784134085` remains failed: two OCR emulators timed out
before any tests. All four native UI lanes and all voice benchmark lanes passed.
Its incomplete report is retained, not relabelled green.

The first exact-APK supplement `41787131044` passed dark-landscape OCR. Its
light-portrait emulator booted but failed **before Notes tests**, because the
external Pixel HOME never obtained stable focus. Read-only cause recovery
`41788213312` retains that traceback. The failed attempt has a diagnostics-only
artifact; it is not missing and is not corpus evidence.

The narrow follow-up `41788513984` executes only light-portrait OCR, against the
same pinned compiled foundation `13300687412`. Provisioning starts the actual
Notes activity and requires three consecutive actual focused-window observations,
including after orientation setup. It does not dismiss app errors/ANRs, tap dialogs,
force-stop Notes or bypass the four original environment/corpus assertions.
Root tests cover real focus, interruption resetting stability, and rejecting a
system ANR dialog. Passing dark-landscape tests are not executed again.

Composite receipts explicitly map dark-landscape measurements to `41787131044`,
light-portrait to `41788513984`, and all other measurements/native UI captures to
`41784134085`. Synthetic OCR input specimens belong to the light-portrait capture,
not the native UI suite. Capture provenance tests prevent the new report run from
being incorrectly printed as the original screenshots' capture run. The report-only
correction workflow waits for the existing capture; it neither compiles nor runs
app tests and corrects the same report directory rather than duplicating the full
bundle. A passing capture/report is not broad scene accuracy, a phone-speed result,
physical acceptance, model training or shipping qualification.

The corrected report-only run `41789450818` succeeded after explicitly fetching
the original source tree for the source-pinned inventory (the shallow checkout
caused the two earlier report attempts to fail with `fatal: not a tree object`).
The complete composite is under `module-e2e/41788513984`: 1,704 native outcomes,
4,752/4,752 inferences, 1,097 PDF pages. Both supplemental OCR lanes passed all
four assertions. The original failed full/supplement/report attempts stay visible.

`build63-composite-triage.json` independently compares identical hashed inputs
and profile/module/case IDs with full62. OCR: 1,532 equal, four improved, zero
regressions. Those four rows are repeated profiles, not four independently new
real-world successes. Current target misses: 24 OCR rows (20 Kannada, four mixed
script) and 685 voice rows. Voice hash-matched comparison: 3,033 equal, 87 gains, 80 regressions;
16 unhashed silence controls are excluded rather than claimed identical. The
unchanged shipping speech weights and emulator variation do not justify a causal
speech improvement claim. Broad scene-text and phone audio/UI acceptance remain
unfinished. The separate exact-weight attention study is not shipping adoption.
