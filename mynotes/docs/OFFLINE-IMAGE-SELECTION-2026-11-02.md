# Follow-up ledger — offline image selection, playback, Indian training

Status: **in progress; no 0.29 APK recommendation yet**. Work is on
`arena/3caca636-tictic` only; the already delivered 0.28 qualification is unchanged.
This is a separate scope from its 48-job / approximately 90-minute ledger. No claim of
360 minutes, physical acceptance, universal image understanding or completed Indian ASR.

## Implemented candidate

- 0.29.0 / code 32; same package, minimum API and sideload signing identity.
- Retain bounded EXIF-oriented source preview; extract normalized word regions from the
  *winning* Tesseract recognition pass without changing the bundled OCR weights.
- Full-screen text-region selection: tap/toggle, rectangle, 8× pinch zoom/two-finger pan,
  native accessible word checkboxes, select all/clear and explicit reviewed text replacement.
- Explicit visual copy of any selected area, including emoji/pictures not understood by OCR.
  Source pixels/file are not mutated; copies follow existing embedded-image/backup bounds.
- Reference-counted bitmap leases, pending-result closure/cancellation, modal ownership,
  low-memory cleanup and storage-operation guards. Crop validation runs off-thread;
  existing main-thread image insertion is reused. No UI work in a storage-thread callback.
- Optional, default-off session-local Android +6 dB playback boost. Stored/exported WAV and
  ASR input are untouched. Unsupported effects and player state errors retain normal playback
  or explain failure. Gain is not noise removal or physical clarity qualification.
- No Internet permission, new cloud service, private-data upload, data migration or production
  speech-model replacement. Notes/Docs/storage/Home/Undo/safe-start baseline is retained.

User instructions and physical checklist: `OFFLINE-IMAGE-SELECTION-029.md`.

## Android qualification

| Round | Runtime source | Run | Result |
|---|---|---|---|
| 1 | `4029f1a3b27c4d919f8d4e63904b1357da3ba231` | 41387972692 | Build failed: new test referred to DocumentCodec in the wrong package. App/domain compiled; no native execution. Fixed. |
| 2 | `9fcc7ccfe42d0094203d5b4f2c004cff21cc0b9a` | 41388219515 | Superseded/cancelled for source-review hardening. 1116 recorded native passes, 60 missing in interrupted landscape shard; **not full qualification**. Seven other native reports complete. Evidence `bf2bff93`. |
| 3 | `1012100e50a034cc7dcb2b23847906ec7e34c8fa` | 41389829243 | Running: 9 jobs planned, 294 JVM tests, 355 unique native methods / 1184 profile outcomes expected. All 327 preserved baseline identities remain authored. No outcome is assumed. |

Round 3 additionally covers transparent-image white compositing, empty-text visual copying,
source-pixel preservation, real Tesseract normalized word boxes, crop-lease lifetime,
controller save/cancel, persisted-revision completion and unchanged WAV bytes with boost.
It collects native image-selection screenshots as well as existing baseline captures.
Local source gates at the round-3 push: root 677 PASS; MyNotes 287 PASS; Java grammar and
`git diff --check` PASS. Local source checks are not Android builds or physical tests.

## Genuine training, separate from production

All input is public CMU ARCTIC speech, not user recordings. Primary source metadata identifies
KSP as an Indian-English male speaker. Original CMU grants allow commercial/noncommercial
use subject to retaining notices/authors and marking modifications. Corpus archives are
verified against hashes published in pinned PyTorch Audio source; Whisper tiny.en base bytes
are checked against the hash embedded in its upstream URL. No gated data is bypassed.

| Attempt | Source | Run | What actually happened |
|---|---|---|---|
| 1 | `ab6a301f` | 41388436823 | Workflow validation failed before any runner job; zero training. Runner-context placement fixed. |
| 2 | `3c462350` | 41388497449 | One preflight job failed while resolving primary license paths; zero optimization. |
| 3 | `32257ba4` | 41388779563 | One preflight job failed on missing packaged COPYING; zero optimization. Keep verified primary web grants when not packed; normalize `./` archive paths. |
| 4 | `96e991b71bcc57f2fefa2b1e92b69d97fbbb63ec` | 41388946222 | **SUCCESS: 32 real AdamW steps**, 32 KSP clips / 103.6315 seconds; 2,365,816 parameter elements changed. Preliminary prefix subsequently aligned with inference. No app adoption. |
| 5 | `d5900e7880cc31254bd8f25ed1422d9826f9c55d` | 41389504274 | **SUCCESS: corrected 32 steps**, same 103.6315 seconds; 2,365,814 elements changed of 2,365,824 trainable. Actual model/data/evaluation duration 17.6897 seconds, not hours of training. Full notice fields truncated at 4096 characters, so whole-report persistence is being added. |
| 6 | Complete-report persistence follow-up | Pending run ID | Same bounded corrected experiment plus source-pinned full reports/licenses in Git; checkpoint/audio deliberately excluded from Git and APK. Do not assume completion. |

Both completed pilots optimize **only the final Whisper decoder block**; the encoder and
all other parameters are frozen. They are not full acoustic-encoder training and are not
Vosk-model adaptation. Base Whisper SHA256:
`d3dd57d32accea0b295c96e26691aa14d8822fac7d9d27d5dc00b4ca2826dd03`.
Corrected pilot checkpoint SHA256 from its check annotation:
`cffd01fab0f5fbafc6342413f6088cf206739918e70df0fe4f6797a57e1a86f8`
(9,471,790 bytes; artifact is experimental decoder deltas, not a standalone app model).

The 16 held-out, different-prompt **SLT US-English** control utterances scored 4.8611% WER
before versus 4.1667% after in both runs. **This is not an Indian-accent WER result**, a
statistical generalization claim, or a comparison against production Vosk. There is no unseen
Indian speaker, spontaneous dictation, regional/code-switching, noise/silence or phone-runtime
acceptance in this small experiment. No optimization or model quality is inferred from a
successful metadata/download step. The 32 losses involve different utterances; first/last loss
must not be presented as a controlled convergence comparison.

The original and corrected experimental artifacts expire after 14 days; full source-pinned
reports/licenses are being preserved separately. Production Vosk US weights remain unchanged
because replacing them without those missing gates would violate the no-regression priority.
See `INDIAN-SPEECH-ADAPTATION-PROTOCOL.md` for the broader adoption criteria.

## Explicitly unfinished

- Universal emoji-to-Unicode conversion / unrestricted object understanding.
- Representative, unseen-speaker Indian dictation evaluation and a qualified replacement
  speech model actually packaged for Android. The real pilot is progress, not completion.
- Motorola playback/microphone quality, physical photo selection/OCR acceptance, and any real
  crash root-cause diagnosis without a device trace. Guarded error paths do not prove all
  crashes are impossible.
- Final 0.29 qualification, raw APK metadata/link, exact final job count and actual elapsed
  follow-up duration. Do not install an intermediate artifact as if it had passed all gates.
