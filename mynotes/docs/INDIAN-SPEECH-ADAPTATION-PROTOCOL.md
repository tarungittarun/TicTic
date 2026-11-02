# Indian-English speech: adaptation and acceptance protocol

Status: **a small genuine decoder-training pilot has completed; production adaptation is
not qualified or adopted**. Corrected run 41389504274 performed 32 AdamW updates on 32 public,
licensed CMU ARCTIC KSP Indian-English clips (103.6315 seconds of speech). Its separate SLT
US-English control is not unseen-speaker Indian-ASR evidence. Full details, exact steps and
limitations: `OFFLINE-IMAGE-SELECTION-2026-11-02.md`. The current app retains its pinned Vosk
small English-US weights. Microphone processing and confidence-based review are separate
from acoustic-model adaptation, and must be evaluated separately.

## Current blockers and permitted next steps

| Input | Current evidence | Decision |
|---|---|---|
| IndicVoices | Audit revision `c96f9088f138cf89d419da7e8e643e1f05c00a87`; gated access; declared CC-BY-4.0 | No audio downloaded; obtain authorized access and inspect English/code-switching suitability before selecting training data |
| Svarah | Audit revision `ebbf7777fe771490696a3f7b007097606fa8c924`; gated access; license absent in audited metadata | Do not assume a license; reserve as held-out evaluation if authorized, never train on it |
| NPTEL2020 Indian-English | Repository describes a large educational-speech corpus and transcript caveats | Inspect actual data/weight licensing and availability; lectures alone are not spontaneous phone dictation |
| CMU ARCTIC KSP | Primary Indian-English speaker metadata, permissive original CMU grant, checksum-pinned audio; 32 real final-decoder-block optimization steps | Quarantined Whisper research only. Obtain representative unseen Indian speakers/phone dictation before any adoption; a US control does not close that gate |
| Existing bundled US Vosk | Existing pinned offline model; previously tested on a small non-Indian fixture | Retain as baseline; that fixture does not establish Indian-accent accuracy |
| Alternative Indian Vosk / Whisper / sherpa-based runtimes | Research candidates, not newly qualified app models | Separate model license, runtime compatibility, APK size and real accuracy before adopting |

Public repository references inspected during this task:
- https://github.com/AI4Bharat/IndicVoices
- https://github.com/AI4Bharat/Svarah
- https://github.com/AI4Bharat/NPTEL2020-Indian-English-Speech-Dataset
- https://github.com/alphacep/vosk-space/blob/master/models.md
- https://github.com/alphacep/vosk-space/blob/master/adaptation.md

Do not request credentials in chat, bypass access controls, treat an unspecified license as
permission, upload private notes/audio, or count metadata retrieval as training. Authorized,
accessible, appropriately licensed data and trainable weights are required before starting.

## Reproducibility record required for every genuine run

Record corpus revision/license/access terms, selected file hashes, transcript provenance,
speaker IDs, split manifest hashes, normalization rules, model base/weight hashes, code
revision, training seed/hyperparameters, hardware, actual start/end times and checkpoints.
Keep large audio/weight/checkpoint artifacts outside Git. A completed job must state whether
it performed metadata inspection, preprocessing, inference evaluation or actual optimization.

Split by **speaker**, not random audio chunks. Keep duplicates and neighboring segments
from one speaker/session out of different splits. Use development data for model selection;
do not tune on Svarah or another final held-out benchmark. Respect the conditions of the
underlying audio, transcripts and model separately.

## Test matrix

Evaluate the existing model first, then every proposed adaptation on the identical pinned
held-out data and normalization policy:

- Indian-English read and spontaneous speech; varied accents, speaking rates, slang and
  proper names. Report underrepresented groups instead of presenting a universal claim.
- Code-switching with Hindi/Kannada as a separate capability; English-only performance
  does not imply support for those languages.
- Clean recordings, actual environmental noise and transparent synthetic-noise conditions.
  Preserve clean originals and record the noise source/license, SNR and augmentation seed.
- Microphone path A/B on actual phones: baseline versus supported speech processing.
  Use matched phrases/distance/room/device; listen for clipping, muffling and suppression
  of the intended speaker. Do not confuse playback clarity with ASR accuracy.
- Latency/real-time factor, initialization time, peak Java/native memory, sustained CPU,
  thermal behavior, battery and APK/storage cost across supported low/mid-range phones.
  Emulator timing is engineering evidence, not physical power/audio acceptance.

Report corpus-weighted WER (substitutions + deletions + insertions divided by reference
words), clip/speaker distributions and failure rates; separately report punctuation/readability.
Show original and normalized transcripts when explaining results. Unsupported emoji intent,
unknown proper names and silence/noise hallucinations need explicit review, not invented text.

## Adoption gate

Adopt only after a documented, reproducible held-out improvement with acceptable regressions,
resource costs and physical-phone behavior. Select thresholds in advance once a legitimate
baseline exists; do not invent a “PRO” accuracy percentage or retrofit a pass criterion.
Keep the original WAV independently playable/exportable when inference fails or is rejected.
Low-confidence review remains useful but is not a calibrated measurement and cannot detect
all confidently wrong recognitions. Keep the previous working model until the replacement
passes the app's offline/device qualification and license/packaging checks.
