# Device files, home and capture revision — 2026-11-01

Baseline144fec0b / delivered0.24.0-code27. New user authorization: **up to60 individual
Actions jobs**, separate from the CLOSED previous40/40. **Round 1 reserved: 9 jobs / 60; 0 launched before this push.** Build plus eight native shards; skipped jobs do not count as executed.
No Android build outside Actions, no public Release, no billing promise. Calculator and
tests.yml frozen. Root QA gates before every push; default [skip ci] unless launching
an explicitly reserved qualification run. Continue monitoring qualification to completion.

## Requested scope
Folders in Filter & view; real device folder selection/creation/moves; Open file for
TXT/DOCX/PDF/legacy Word; home left/right workspace swipes; bottom full-width search and
Filters with create above; Doc OCR/voice insertion; selected-only Notes checklist;
more useful OCR and clearer audio/readable transcription; strict native cases.

## Research / design boundaries
- https://github.com/tesseract-ocr/tessdoc/blob/main/ImproveQuality.md : rescaling,
  binarization, inversion, deskew and appropriate page segmentation matter. Do not
  claim Tesseract language models understand arbitrary emoji or all decorative fonts.
  Implement bounded low-confidence sparse-layout fallback, with genuine inference tests.
- https://github.com/openai/whisper : multilingual encoder-decoder ASR is a different
  model family from the current small Vosk recognizer. Readable learned punctuation is
  not obtained merely by changing audio volume. A model switch requires verified model
  distribution, Android integration, latency/memory/privacy and real audio evaluation.
- https://github.com/openai/whisper/discussions/813 : maintainers/users discuss silence
  hallucinations and word timestamp issues. This is discussion evidence, not a guarantee.
  Do not delete pauses from the retained original audio or infer speakers from silence.
- https://github.com/m-bain/whisperX : alignment/diarization are separate processing
  stages; overlapping speech and speaker assignment need dedicated evaluation.

No online transcription/OCR or automatic upload is authorized. Preserve original audio,
raw recognition and reviewable text. Pause-based punctuation/paragraphs are explicitly
heuristic, not semantic topic/speaker detection or a claim to match commercial models.

## Implementation notes (unqualified work in progress)
Home search moved below quick actions; create remains above/right. Filter & view links
to real folder browser and device main-folder picker. Existing verified folder storage
continues to autosave editable MyNotes files; choosing a root copies/verifies the library,
not silently relocating arbitrary unrelated files. Folder create/move remains available.
Doc Insert now routes Read image and Record voice. OCR appends reviewed text to Doc.
Open file retains bounded MyNotes JSON review, adds bounded local TXT/DOCX text-copy
review; DOCX rich layout/media are explicitly not imported. PDF and binary DOC open in
an installed reader after consent; no compatible reader is an honest failure, not editable
import. Cloud providers remain rejected. Original files are never overwritten.

Selected-only Notes conversion now uses verified migration/checkpointing and schema 10
mixed Notes. Independent Save copy to device folder writes/verifies a complete portable
Copy file without changing the active root, source note or original ID. Doc voice recording
has an explicit review-and-insert action: appended text plus recording association, while
original audio/raw recognition and the automatic transcript note are retained. No automatic
speaker/topic inference. Optional platform NoiseSuppressor, PCM peak meter, conservative
image preparation and sparse OCR fallback require real-device evaluation.

## Qualification round 1 (reserved 9 jobs)
Local gates: 643 root tests + 253 Notes source-contract tests PASS; Java syntax parsing
PASS (not type checking); diff whitespace PASS. Android builds remain Actions-only.
Discovered: 270 JVM methods, 304 Android methods; focused 160. Native plan:
API26 all 157+147, API36 all 157+147 (granted microphone), API34 large font
85+75, API34 landscape 85+75 = 928 native outcomes. Compilation, lint and all
native outcomes pending. No new APK handoff until results inspected.

