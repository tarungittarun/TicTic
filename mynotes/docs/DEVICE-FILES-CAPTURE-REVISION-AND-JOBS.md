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


## Round 1 inspected; round 2 reserved
Run 41302182697, source 0cc03e0f: all 9 jobs executed. Build/JVM/lint succeeded;
908/928 native outcomes passed, 20 failed. Six distinct failure methods: missing accessible
search name, filter-tab test's obsolete hierarchy, blank RecyclerView swipe routing,
Doc Insert incorrectly requiring a selection before capture, and two future-schema test
fixtures still using now-supported version 10. All six addressed; picker result 104 guard
also corrected, with new source-contract regression. No skipped/duplicate outcomes.
Evidence auto-preserved in e0e54035. Round 2 reserves another 9 jobs: 18/60 total
reserved, 9/60 already executed. Local gates 644 root + 254 Notes PASS.

### Additional upstream research actually inspected
Whisper README model table distinguishes A100-based relative speed from device performance;
its VRAM figures are not Android RAM benchmarks. whisper.cpp lists tiny 75 MiB/~273 MB
and base 142 MiB/~388 MB as approximate disk/memory; its Android example recommends tiny
or base (https://github.com/ggml-org/whisper.cpp/tree/master/examples/whisper.android).
WhisperX README limitations explicitly identify imperfect diarization/overlapping speech
and dictionary-dependent alignment. No whisper/whisper.cpp/WhisperX dependency, model,
network service or accuracy claim has been added; a measured Android replacement remains
separate work, not a silent backend change. Current Vosk remains pinned and offline.

## Qualification complete — delivered candidate 0.25.0 / code 28
Round 2 run **41303449656**, source **880fe1076371c16f8bc1926622cacf1c1bdb15af**:
**all 9 jobs SUCCESS**, build/debug+release lint and exact 270 JVM-method gate PASS.
**928/928 native outcomes PASS**, no missing/unexpected/duplicate outcomes across
API26, API36, API34 large-font/dark and API34 landscape. Native evidence and 352 JPEG
previews preserved by **6b10bff3**, under `design-2026-10-29/native/41303449656`.
Inspected folder navigation, landscape Doc capture chooser and large-font mixed Notes
screenshots. Real-provider independent copies/TXT opening, arithmetic-symbol OCR,
source-preserving reviewed transcript append, selection conversion/Undo and workspace
swipes are included in the passing native inventory. API36 microphone is granted;
emulator capture/meter evidence is not physical microphone clarity evidence.

New allowance usage: **18 individual jobs actually executed / 60 authorized**;
**42 unused**. No pending jobs, no public release, no billing authorization. Previous
adaptive 40/40 remains closed and separate. Calculator and tests.yml unchanged.

Raw installable development APK (not ZIP):
https://github.com/tarungittarun/TicTic/actions/runs/41303449656/artifacts/13081708721
`MyNotes-v0.25.0-28-development.apk`, 66,739,424 bytes.
GitHub-reported SHA256: `c3a64d94b5e07b12ca5a68c1154f30f54c3d2215f83d5e2e512b190a18797807`.
Expiry: 2026-11-08T11:49:50Z. This hash is GitHub's artifact metadata, not a local
redownload verification; sandbox artifact download follows a blocked Azure host.
The build verifies the unchanged signing certificate. Install over the current app;
do not uninstall or clear data. Keep an independent backup; original WAV audio needs
separate export and is not included in note backups.

### Finding the new functions
- Home: swipe the note board left for Doc and right for Notes (leading-edge Library
  gesture remains separate). Search and Filters are below the creation controls.
- Filters → Folders: browse/create/manage folders or choose the device main folder.
  Main-folder selection is the library-wide verified storage operation, not a one-note move.
- Open a Note/Doc → its actions → Save copy to device folder for an independent complete
  portable MyNotes copy in a chosen existing folder, without changing the main folder.
- Library → Open file: TXT/DOCX offer reviewed text copies; DOCX rich layout/media are
  not imported. PDF/legacy DOC require consent and an installed external reader.
- Select only the desired Notes text, then convert to checklist; surrounding text/styles
  stay intact and Undo restores the original note.
- Doc → Insert → Read image / Record voice. In the recording inbox, choose Review and
  insert transcript, correct text/paragraphs/speaker labels, then explicitly insert.

### Deliberate limitations / remaining acceptance
No universal emoji/decorative OCR guarantee; review recognized symbols manually.
Pause-based punctuation and paragraph breaks are heuristics, not semantic topic detection
or speaker diarization. No unbenchmarked Whisper replacement was shipped. Real noisy
speech, accents, handwriting/photos, physical phone/SD-card behavior and novice usability
still require user/device evaluation. Automation does not establish those guarantees.
