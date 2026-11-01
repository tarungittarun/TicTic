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
