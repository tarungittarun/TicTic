# Separate module E2E / recognition performance baseline

## Execution and ordered report

Workflow: `.github/workflows/notes-module-e2e.yml`. It does not modify `tests.yml`, publish a replacement APK, train a model or consume private attachments. Production runtime/domain/app main source remains the candidate57 baseline. The benchmark harness and corpus are opt-in **androidTest only**.

PDF chapters follow this explicit order:

1. Notes and checklists
2. Rich Doc and tables
3. Diagrams
4. Read image / OCR
5. Voice inbox and transcripts
6. Images and drawing
7. File viewers
8. Home, search and Library
9. Folders and storage
10. Backup, history and Trash
11. Reminders
12. Settings and diagnostics

`module-e2e-scene-order.json` explicitly places every126 known native screenshot scenes in a module and workflow order. A shared whole-app capture method does not dump diagrams, Voice inbox or drawings into Settings. Portrait light/dark comparisons and landscape light/dark comparisons stay together, under their owning module. Each named method retains exact passed/failed shard results and source line. Uncaptured states remain labelled missing; no old screenshot is relabelled as a new profile.

## Planned work, not yet a completion claim

- Native:419 methods ×4 complete API34 configurations =1,676 outcomes, split into8 disjoint shards.
- Four profiles: light portrait, dark portrait, light landscape, dark landscape; actual Android resource assertions plus captured environment.
- OCR:384 generated conditions:8 content templates ×4 font families ×3 font sizes ×2 polarities ×2 size/rotation settings. English, receipt, symbols, URL/email, Hindi, Kannada, mixed script and blank controls;1.12M/2.4M input pixels. These are384 conditions, **not384 independent photographed documents**.
- Speech:200 distinct public CMU utterances (100 KSP,100 SLT), each clean/quarter amplitude/seeded10dB uniform-noise target/gain3 clipping, plus4 silence controls =804 inputs. Two speakers, **not804 independent speakers**. PCM16 mono16kHz. KSP and SLT source archives are checksum pinned; original primary-source permission is verified. Some archives omit COPYING; a previously retrieved hash-bound original CMU grant is preserved, not assumed.
- Recognition:384+804=1,188 input conditions repeated in4 profiles =4,752 planned observations. Default bundled Vosk is measured. Large experimental Whisper-corpus, spontaneous dictation, unseen Indian speakers and multilingual ASR remain outside this baseline.

## Accuracy, performance and preservation

Ground truth is source text / original public transcripts, not clean recognizer output. Store reference, hypothesis, strict Unicode CER, strict WER, separately documented English punctuation/case-normalized WER, and operational errors. Independently re-score in Python; reject forged score or pass flags. Predeclared descriptive targets: OCR CER≤0.20; normalized speech WER≤0.35; silence must remain empty. An execution-success flag **does not mean every accuracy target passed**. Missing rows prevent a complete-corpus accuracy pass.

Record actual elapsed milliseconds, compressed input bytes, pixel dimensions/font/polarity/rotation or audio duration/condition, real-time factor, and sampled heaps. Timing includes recognition plus scoring and, for speech, transcript codec/dedup/source-integrity checks; excludes fixture synthesis and report writes. Cold initialization is included on first invocation. Sampled heap is not peak RSS. Emulator host/load results are not physical phone latency or causal theme-speed comparisons.

For speech, verify unchanged input bytes and transcript-source identity/dedup/backup-codec roundtrip. Existing native Voice inbox transport/recovery/UI methods are independently selected in all4 configurations; the800 inference conditions are not silently described as800 physical card-tap or microphone tests.

Reports retain every actual result, including failures, and provide PDF, JSON, CSV, performance summaries, specimen hashes and source/license provenance. Raw public audio and generated PNGs remain external Actions artifacts, not Git or production APK assets. Physical OCR/microphone/loudness, arbitrary vendor/provider states, complex Word equivalence and physical drag precision remain unqualified.

## Initial failure history

Run41649423992 failed before Android compilation/inference because the original CMU archive omitted COPYING. No model test passed or failed in that attempt; all planned inference remained unexecuted. The failed report is retained as failed history. Preparation was corrected to require the exact retained primary-source grant bound to the same archive hash; no license requirement, app assertion or accuracy criterion was removed.

## Executed orientation finding and strict scheduling

The original full run41650596888 completed1,676/1,676 native outcomes successfully and recorded4,752 model measurements. Its three dark-landscape benchmark guests failed the original orientation assertion (1080×1920 portrait), so that requested profile was **not qualified**. All model methods themselves executed. Recheck41652746046 reused the exact original build artifacts and passed all four checks after applying rotation **after** settled HOME and actual Notes Activity focus. Two speech shards require the same observed setup correction; their conclusions must be recorded separately. Failed original rows stay retained, not relabelled.

`notes-module-ordered-native.yml` additionally executes each complete profile with **strict sequential barriers between12 modules**, rather than relying on mixed parallel native shards. Its419-method explicit placement manifest is also the report ledger placement. The initial full run remains evidence of all-method four-profile execution, not a claim that parallel shards ran in strict chapter order. No new model, app code or APK is produced by this schedule audit.

Actual early OCR has324 distinct image hashes across384 rendering conditions and8 source templates; duplicate fallback glyphs and blank controls are not independent documents. Speech has200 source clip hashes but100 distinct prompts read by2 speakers. The report exports those distinct-count limits. Report-only OFL Noto Devanagari/Kannada static fonts are hash-pinned with original grants; report rendering requires ReportLab4.4.4 and uharfbuzz0.52.0. They are not app fonts or trained weights.
