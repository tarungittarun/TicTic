# Quality follow-up — 2026-11-01

Baseline: delivered 0.25/code28, closure 7b516081. User is testing that APK;
this follow-up must not silently replace it. New candidate 0.26/code29.
User requested all speech/OCR/files work, research locally, and removed the
individual Actions job cap for this follow-up. No paid model services, private
uploads, public releases, or promise of uninterrupted training are authorized.
Prior 18/60 device/capture jobs and adaptive40/40 remain historical/separate.

## Actual research read (upstream via GitHub API)
- https://github.com/tesseract-ocr/tessdoc/blob/main/tess5/TrainingTesseract-5.md
  warns scratch training without representative text lines overfits; recommends
  supported Python tesstrain tools rather than abandoned shell training. Fine-tuning
  is different from running a recognition test repeatedly. Current integer fast
  language assets are not being pretended to be freshly trained weights.
- https://github.com/ggml-org/whisper.cpp/tree/master/examples/whisper.android
  recommends tiny/base for its Android example. This is an implementation candidate,
  not proof of latency/memory/accuracy on the user's phone or a drop-in Vosk change.
- https://github.com/SYSTRAN/faster-whisper : CTranslate2/quantized CPU/GPU inference;
  published desktop/CUDA benchmark comparisons are not Android guarantees. No Python
  service/network upload or desktop dependency will be inserted into the Android app.

## Implemented first measured hardening batch (not yet Android-qualified)
- Bounded Unicode-codepoint CER and explicitly normalized WER evaluation. Punctuation,
  arithmetic symbols and emoji substitutions count as errors, not normalized away.
  Nine domain tests cover normalization, insertions, empty conventions and work caps.
- Actual Vosk inference on the existing licensed public fixture plus deterministic
  lower-amplitude and seeded-noise variants. Threshold is relative WER <=0.6 against
  clean model output; explicitly NOT ground-truth accuracy, accent or microphone proof.
  Tests verify original/degraded WAV bytes are unchanged by recognition and persist
  source hash, measurements and elapsed times.
- Actual OCR inference on light/dark/transparent common-text variants with strict
  CER <=0.20 and explicit +/= preservation; persisted reports expose every output.
- Fixed dark-frame heuristic: do not invert light document interiors merely because
  top/bottom borders are black. Added exact source/prepared-pixel regression.
- DOCX aggregate decompression budget now includes document.xml, regardless of ZIP
  order; public helper also rejects oversized compressed input. Tests cover exact
  aggregate boundary plus tabs, breaks, Kannada, emoji, symbols and source immutability.
- CI preserves quantitative OCR/speech JSON with the native evidence, not just tests.

## Qualification ledger
First follow-up round reserved: build + 8 existing exhaustive profile shards =9 jobs.
0 follow-up jobs launched before commit; record actual run/results below. Gate local
root/Notes checks before every push. Android build/inference remains Actions-only.
No model weight changes or training runs claimed. Representative licensed held-out
noisy/accent/emoji corpora and physically measured Android alternatives remain needed
before promising a recognition model replacement or broad accuracy improvement.

## Parallel nonshipping model research
Android qualification run 41329486926 launched all 9 jobs, source 394bc049;
279 JVM methods / 309 Android methods (160 focused); planned 938 native outcomes.
While it runs, one separate CPU research job is authorized/reserved, without Android
source or dependency changes. Compare pinned small Vosk and sherpa-onnx Whisper tiny.en
on the public whisper.cpp JFK fixture (git blob identity checked), its attenuation/noise
variants, and silence. Emit lexical WER against a manually stated public-speech reference,
load/inference elapsed time, process peak RSS, exact transcripts and model receipts.
This single-fixture desktop experiment is neither training nor Android/held-out accuracy.
Whisper release asset has no publisher digest: observed hashes are research receipts,
not independent authenticity verification, and are insufficient for product adoption.

Also read https://github.com/alphacep/vosk-space/blob/master/adaptation.md : diagnose
mismatch first; runtime vocabulary restriction, language-model adaptation and acoustic
fine-tuning are distinct, and training from scratch is rarely the best first solution.
https://github.com/k2-fsa/sherpa-onnx provides Android support and a Whisper tiny.en
example; that establishes an integration candidate, not that it should replace Vosk.

## First execution results and collection fix
Run 41329486926: 9 jobs executed. Exact 279 JVM and **938/938 native outcomes passed**,
but eight native jobs failed after testing because the emulator action executes script
lines separately: the multi-line shell `for` used for report collection was incomplete.
Evidence e2814c22 preserves the passing method inventories, not missing quantitative
reports. Changed collection to one complete command and added a local shell-syntax
regression check. Do not call that run fully qualified. A second exhaustive 9-job round
is reserved to verify collection and preserve actual quantitative outputs.

CPU research run 41330011385: 1 job SUCCESS, eight fresh-process inference measurements;
full rows/receipts saved in RECOGNITION-CPU-PILOT-41330011385.json from check annotations.
Clean/attenuated JFK lexical WER: Vosk 1/22, Whisper tiny.en 0/22; seeded noise both 2/22.
Measured process peaks roughly 225 MiB Vosk vs 308–310 MiB Whisper, including runtime
imports. Desktop inference timings do not establish phone performance. Whisper emits
[BLANK_AUDIO] on silence, a control marker that must not be inserted as spoken text.
This single 22-word sample cannot justify a model replacement or general accuracy claim.

Follow-up actual jobs so far: **10** (9 qualification +1 research), all terminal.
Second qualification round reservation +9, prospective total19, no arbitrary cap target.
Local checks before second push: 649 root /259 Notes, all passing.

## OCR weight experiment reservation
Second Android qualification is running as 41330808145, source 0a9d1071. In parallel,
reserve one CPU-only job comparing the app's exact fast language weights with official
Tesseract best 4.1.0 weights for English/Hindi/Kannada. Six phrases × clean/low-contrast
× two weight families =24 actual inferences, with pinned Git blob verification, font/image
hashes, Unicode CER and process timings. Require complex-script shaping support; do not
mistake missing glyphs for recognition failures. Desktop fonts/PSM7 are a controlled pilot,
not handwriting/emoji/photo accuracy or Android performance; no APK model changes.
Prospective total follow-up jobs: **20** (18 native/build +1 speech research +1 OCR research).
No new model training is claimed; fast integer weights are not fine-tunable float models.

OCR pilot run 41330941921 completed successfully with 24 inferences, but GitHub capped
individual step notices: only 9/24 per-case annotations were exposed to this workspace
(the complete artifact is behind the sandbox's blocked Azure download host). Do not
summarize incomplete rows as a complete comparison. Group all rows in one bounded
annotation and repeat the one-job pilot for complete inspectable evidence. Follow-up
prospective count becomes21 jobs; this is a report-preservation correction, not training.
OCR repeat 41331260229 completed, but a single grouped notice hit GitHub's observed
4096-byte annotation-message truncation (JSON became incomplete). Corrected to six
language/family groups under3500 bytes each, below both per-message and per-step caps;
added deterministic local regression for payload sizes. One further one-job pilot
is reserved so all 24 outputs are actually inspectable; prospective follow-up total22.
These evidence transport corrections do not change APK recognition models.

## Training/adoption protocol (not an executed training claim)
Longer running is not a stopping criterion or quality guarantee. Do not fine-tune on
these six OCR phrases / one speech sample and then use them as the accuracy test.
1. Establish licensed, representative paired data: phone photos/screenshots (three
   supported scripts, fonts, symbols, blur/skew/lighting) and transcribed speech
   (speakers/accents, microphone distance, noise, conversation and silence). No private
   notes/recordings may be uploaded or used for training without separate consent.
2. Freeze manifest/hashes and source/speaker/font-separated train, validation and final
   held-out splits before adaptation. Synthetic variants of one source stay in one split.
3. Prefer the least intrusive adaptation that solves a measured mismatch. Generic
   notes must not be constrained to a tiny Vosk vocabulary to inflate benchmark scores.
   OCR fine-tuning needs compatible float checkpoints; integer fast weights are not
   silently treated as trainable networks. Emoji needs appropriate character coverage
   and data, not an invented replacement character or text-generation guess.
4. Select checkpoints on validation, retain incumbent and all raw inputs/recognition,
   and use early stopping rather than idle-duration padding. Report dataset size,
   actual training steps/time, losses and held-out CER/WER, digit/symbol errors, silence
   false insertions, punctuation/paragraph errors separately. Diarization needs its
   own annotated speakers and overlap evaluation; pause gaps are not speaker labels.
5. Compare on Android min26/current/large-font/landscape and a physical phone: cold/warm
   latency, peak memory, battery/thermal effects and privacy/data-preservation controls.
   Desktop RSS/timing and printed font fixtures cannot substitute for those measurements.
6. Adopt only with reproducible held-out gains and no unacceptable common-case or
   data-loss regression. Record model/runtime licenses, verified immutable hashes,
   artifact provenance and rollback/format compatibility before changing default weights.

Currently no representative training/held-out corpus or new trained checkpoint exists.
The actual completed work is hardening, strict evaluation and upstream-model pilots;
no claim of general speech/emoji accuracy or full Word-format fidelity is justified.

## Second qualification inspected; third round reserved
Run 41330808145 finished FAIL: seven profile method inventories passed; landscape-api34-b
hit the 1500s instrumentation timeout with 50/75 outcomes and no completion marker.
Do not discard or call that a pass. First-finisher's 15-minute peer wait also expired,
so evidence 071d7dd6 is explicitly missing that profile. Native APK source unchanged.
Quantitative reports were collected on API36 (OCR 0 CER on all three controlled variants;
relative speech WER 0 / 1-of-15), but API26 `run-as ... test -f` was not portable.

Third round fixes evidence infrastructure, not model accuracy claims: use portable ls,
require selected reports and independently recompute their scores on the host, preserve
raw instrumentation text, recover the prior timeout tail, add 180s per-test watchdog
while retaining 1500s whole-instrumentation bound. Give first-finisher aggregation a
longer bounded window than the slowest peer; only that aggregator job has 45min limit.
Reserve 9 further jobs: prospective follow-up total31 (27 build/native +1 ASR +3 OCR pilots).

OCR final complete pilot is 41331604142: all24 outputs inspected and saved in
OCR-WEIGHT-PILOT-41331604142.json. Fast/best: English 0/66 vs8/66 codepoint errors
(best removed arithmetic spaces); Hindi both2/60 (digit1 lost twice); Kannada both0/62.
These six system-font phrases show no measured benefit from a blind best-weight swap.
No broad language accuracy, handwriting or emoji coverage is established by this pilot.

## Third qualification inspected; fourth round reserved
Run 41333491242 source a6685417: all9 jobs ran, but full qualification FAIL.
API26/API36 quantitative reports are now collected and independently verified. The
previously timed-out landscape shard passed75/75 with the watchdog. Other failures:
- API36-b: 1 geometry assertion expected local height192, got enclosing translated
  global rectangle193. This is outward pixel rounding, not clipping. Correct test now
  requires full visible height AND width (>=local dimensions); still forbids clipping.
- Large-font-b: captured black frame and attached-but-unfocused activity from the first
  UI fixture onward; assertions correctly refused interactions instead of false passes.
  Add explicit device wake/keyguard dismissal/stay-awake setup and test-only keep-screen-on;
  retain every real window-focus assertion. This is fixture/device precondition handling,
  not an app claim to override normal user screen timeout. Root cause beyond these
  observed symptoms remains unproven; capture bounded window/power/input/logcat diagnostics
  on every future run and preserve the original instrumentation exit code.
- Old landscape timeout tail identified selectedNotesTextConvertsOnlySelectedRangeAndStaysInNotes
  as unfinished. It subsequently passed on the same app source, so do not invent a
  production-model cause or claim a proven deadlock fix. Keep watchdog and diagnostics.

Fourth exhaustive round reserves9 jobs: prospective follow-up total40 (36 build/native,
1 speech research,3 OCR research). Local gates657 root /267 Notes PASS before push.
No recognition model replacement, long training claim, or public release.

## Completed follow-up batch — 0.26.0 / code29
Final run **41335226486**, source **29c64d0807773d7be3a460defc96e39515490d17**:
all9 jobs SUCCESS, 279 executed JVM methods, debug/release build and lint PASS.
**938/938 native outcomes PASS**, exact inventories, no skips/missing/duplicates:
API26 160+149; API36 160+149; API34 large-font/dark85+75; landscape85+75.
Native evidence **d08df26d**, 352 JPEGs plus raw instrumentation and bounded diagnostics.
Inspected final large-font home and API36 capture sheet; controls remain visible.

All four quantitative reports are present, independently re-scored both in CI and
locally: API26 and API36 OCR CER0 on three controlled common-text variants each;
relative speech WER0 on both perturbations in this run. This is NOT ground-truth
speech accuracy; the raw clean Vosk number transcription itself has recognition errors.
Prior recorded relative WER1/15 demonstrates why this tiny synthetic probe is only a
bounded-regression check, not proof of real-world robustness or perfectly stable decoding.

Final local checks: **657 root /267 Notes PASS**. Calculator and tests.yml unchanged.
One-off old-run diagnostic recovery is removed in the closure (otherwise its expiring
artifact would break future qualification); ongoing per-run diagnostics, watchdogs,
wake/focus preconditions and strict report gates remain. No APK/runtime changes in closure.

**Actual follow-up jobs:40**, all terminal: four qualification rounds ×9, one ASR CPU
pilot, three OCR CPU pilots. Earlier failed/partial runs and report transport problems
remain recorded above, not reclassified as successes. Prior device/capture18 and old
adaptive40 remain separate historical allowances. User removed follow-up job cap;
unused capacity is not a spending target, and no public release or paid model service ran.

Optional newer raw development APK, for after the user's existing0.25 test:
https://github.com/tarungittarun/TicTic/actions/runs/41335226486/artifacts/13094322909
`MyNotes-v0.26.0-29-development.apk`,66,740,712bytes; GitHub-reported SHA256
`5f92fe82004112fc4a6fc47c42781c2b7e08a4a4df51963bfb60f4609bf44072`.
Expiry2026-11-08T18:12:41Z. Hash comes from GitHub metadata, not a local re-download.
Unchanged signer verified in build. Install-over only; don't uninstall/clear data.
Original audio still requires separate export/backup. No public Release was created.

### Exact done / still pending
Done: dark-frame OCR preprocessing correction; strict Unicode/symbol scoring and real
inference probes; safe DOCX compressed/aggregate budget correction; source immutability
checks; pinned offline-model CPU comparisons; reproducible receipts and native evidence;
stronger CI preconditions, score validation and bounded diagnostics.
Still pending: representative licensed training/held-out corpora, actual fine-tuning,
physical noisy/accented speech assessment, universal emoji/handwriting OCR, semantic
punctuation/topics/diarization, full editable Word-layout/media fidelity, and measured
Android integration of any replacement recognizer. No new model weights were trained
or shipped. Small pilots do not justify silently replacing the default models.
