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
| 3 | `1012100e50a034cc7dcb2b23847906ec7e34c8fa` | 41389829243 | **SUCCESS, 9/9 jobs**, 294 JVM / 1184 native outcomes, 355 unique identities. All 340 v0.28 baseline identities preserved; source-pinned complete native reports are in Git. Latest landscape/reduced-viewport changes remain round 4, not silently covered by this run. |
| 4 | `590c9d10daf52ca6a1bdc15b7b088f6d7259f396` | 41390921561 | **FAILED, not qualification**: 1131/1192 native outcomes; large-font shard A had 61 Core fixture focus failures. Its other 64 methods passed, and the seven other profile inventories completed. Min-API native methods passed but the documentation-publishing step failed after local gates. Full original reports, diagnostics and frames recovered by 41392814864. |
| 5 | `37476912f375b5fe7da17a3b421f619d43090993` | 41393215902 | **FAILED, not qualification**: 713/1192 native outcomes. All four API-34 profiles stopped in the new preflight (478 missing); full saved window snapshots prove launcher focus existed, but the queried `dumpsys window windows` subcommand omitted global focus fields. Separately, one API-26 baseline format-size accessibility action failed; its other 183 methods passed. No assertion is removed or relabeled as a pass. |
| 6 | `3239bc7815a90fc568f4be2fc61e4546dbaf8c40` | 41394658554 | **FAILED, not qualification**: 1092/1192 native outcomes. Seven native shards completed; current-API-36 shard A stopped after 84/184 with exit 124. Case 85, `CoreJourneysTest#selectedNotesTextConvertsOnlySelectedRangeAndStaysInNotes`, started but never finished. Activity dump also timed out; exact cause unknown. All API-34 focus preflights and API-26 size actions passed this time; this does not prove a runtime fix for the previous intermittent size failure. |

Round 7 is being prepared with **no production runtime change and no removed assertion**.
A debug-test-only JUnit observer records code-only Java thread stacks after 45 and 120 seconds
without a test finishing, independently of the app main thread. A host watchdog fails with
exit 124 after 240 seconds without instrumentation output (the existing per-test timeout is
180 seconds); the 1500-second overall cap remains. These are diagnostic observations, not
passes or an explanation of the hang. Bounded progress/stack files are collected before
potentially blocked `dumpsys` calls. No root, reset, model swap or user-data upload is used.

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
| 6 | `03db68fda36c8f2a4cbff921b893dd68fe380a9b` | 41390127020 | **SUCCESS, 32 corrected AdamW steps**, 2,365,816 of 2,365,824 trainable elements changed; 103.6315 seconds of training audio; 23.119812 seconds measured experiment duration. Complete reports/licenses retained in `speech-pilot/41390127020/` by `106cad2f`; adopted=false. |

These independent completed pilots optimize **only the final Whisper decoder block**; the encoder and
all other parameters are frozen. They are not full acoustic-encoder training and are not
Vosk-model adaptation. Base Whisper SHA256:
`d3dd57d32accea0b295c96e26691aa14d8822fac7d9d27d5dc00b4ca2826dd03`.
Corrected pilot checkpoint SHA256 from its check annotation:
`cffd01fab0f5fbafc6342413f6088cf206739918e70df0fe4f6797a57e1a86f8`
(9,471,790 bytes; artifact contains trained decoder-block weights, not a standalone app model).

The 16 held-out, different-prompt **SLT US-English** control utterances scored 4.8611% WER
before versus 4.1667% after in the completed experiments (7 versus 6 edits / 144 reference words). **This is not an Indian-accent WER result**, a
statistical generalization claim, or a comparison against production Vosk. There is no unseen
Indian speaker, spontaneous dictation, regional/code-switching, noise/silence or phone-runtime
acceptance in this small experiment. No optimization or model quality is inferred from a
successful metadata/download step. The 32 losses involve different utterances; first/last loss
must not be presented as a controlled convergence comparison.

The original and corrected experimental artifacts expire after 14 days; the round-6 complete
source-pinned reports/licenses and primary roster are now preserved in Git. Their actual file
hashes match the report (data manifest, both original CMU grants, Whisper MIT grant and roster).
Round-6 checkpoint: 9,471,790 bytes, SHA256
`9220868123336171babe0224e07106edc7acbd04adbd448b73a291eb7fe917d0`.
The same 103.6315 seconds of source audio was reused across the small decoder pilots; repeated runs are
not additional unique training hours or cumulative steps in one checkpoint. Production Vosk US weights remain unchanged
because replacing them without those missing gates would violate the no-regression priority.
See `INDIAN-SPEECH-ADAPTATION-PROTOCOL.md` for the broader adoption criteria.

## Additional primary-source voice audit — not training

Run **41391004242 SUCCESS**, source `ce6aa1caac64b00fb650accf799c79e6d1a5a49f`,
checked AUP, AXB, GKA and SLP against the original FestVox roster, checksum-verified original
archives and attempted per-voice primary grant paths. The roster verifies Indian-English
metadata for these voices, but **zero additional per-voice grants were verified** at the checked
paths or in the archive metadata. All four remain blocked for training, not silently admitted
from a public download or a secondary accent citation. This is not proof that permission
cannot be obtained elsewhere. Full bounded public evidence: `speech-candidates/41391004242/`.
Only KSP has been used for Indian training. This audit performed **zero optimizer steps**.

## Expanded acoustic experiment — SUCCESS, not production adoption

Source `10549e851edb62306993cb1ddde6aa8dee7ad605`, run **41393453222 SUCCESS**.
Complete small reports, original grants, primary roster and modification notice were retained
by `5305f665` in `speech-acoustic/41393453222/`. The independent local evidence cross-check
verifies the actual manifest/license/roster hashes, 593 unique training WAV hashes and prompts,
657 unique WAV hashes including controls, disjoint training/evaluation prompts and 32 paired
heldout reference prompts. It does not claim to rerun inference or rehash the checkpoint locally.

- **593 actual AdamW steps**, one pass over 593 KSP A recordings, **1884.8339375 seconds
  (31.4139 minutes) of unique Indian read speech**. The earlier pilot's A clips are a subset;
  do not add their repeated duration as new unique speech.
- Final **acoustic encoder AND final decoder** blocks were trained. **1,774,076 encoder** and
  **2,365,817 decoder** elements changed: **4,139,893 of 4,139,904 trainable elements** total.
  The pipeline hashes all other frozen state before/after and requires identical hashes.
- **311.9592 seconds measured model/training/evaluation experiment duration**, excluding the
  preceding data preparation and dependency installation; not 31 minutes of compute or six
  hours of productive work. Recomputed current-encoder features are used during both training
  and after-training scoring; frozen baseline-feature reuse would invalidate this experiment.
- Partial trained-block checkpoint **16,573,120 bytes**, SHA256
  `84dcc55f0c1bcdc53e62d90a50c4dbe284669222f13ecbcbf458d62af42bc338`.
  It requires the exact original base model; it is not a standalone Vosk/Android model.
  Research artifact **13119337497**, expiry **2026-11-16T05:46:44Z**; no WAV/checkpoint in Git.

| Control, 32 recordings each | Before | After | Meaning |
|---|---|---|---|
| KSP B, **SAME Indian training speaker**, different prompts | 34 edits / 278 words, 12.2302% WER | 22 / 278, 7.9137% | Seen-speaker prompt-heldout diagnostic only; **not unseen-speaker Indian accuracy** |
| SLT B, US speaker absent from training | 13 / 278, 4.6763% WER | 10 / 278, 3.5971% | Small US regression control, not a production Vosk comparison |

The two control voices read the same 32 heldout prompts: 64 distinct audio recordings, not
64 independent reference prompts. All 593 training prompts are disjoint from those prompts.
These descriptive results are not statistical significance, spontaneous dictation, Hindi/
Kannada code-switching, noise/silence robustness or physical-phone qualification.
**Adopted=false; production Vosk remains unchanged.** A qualified Android Whisper runtime,
production-baseline comparison and representative Indian speaker-held-out/physical evidence
are still required. Universal emoji OCR is a separate unfinished task.

The first expanded preparation used repeated random access in BZip2 archives. Forward-only
preparation and bounded future failure diagnostics are committed without dispatching duplicate
training; this completed source-pinned run is unchanged. A six-member synthetic BZip2 test
proves identical member bytes with one backward seek instead of six; this is a structural
curation regression test, not an actual-corpus timing claim. A cancellation request during
preparation returned HTTP 403 (integration permissions); no cancellation/duplicate replacement
occurred, and the original run subsequently completed successfully.

## Explicitly unfinished

- Universal emoji-to-Unicode conversion / unrestricted object understanding.
- Representative, unseen-speaker Indian dictation evaluation and a qualified replacement
  speech model actually packaged for Android. The real pilot is progress, not completion.
- Motorola playback/microphone quality, physical photo selection/OCR acceptance, and any real
  crash root-cause diagnosis without a device trace. Guarded error paths do not prove all
  crashes are impossible.
- Final 0.29 qualification, raw APK metadata/link, exact final job count and actual elapsed
  follow-up duration. Do not install an intermediate artifact as if it had passed all gates.


## Visual evidence boundaries

The round-4 selection screenshots were captured as JPEGs and skipped by the legacy PNG-only
baseline collector. Supplementary source-pinned job 41391729004 retained those four actual
synthetic frames; 41392814864 additionally recovered the original failed-profile reports,
window diagnostics and PNG previews after the native evidence-publishing step failed.
Native source `37476912` now captures PNG after two animation frames, avoiding stale labels
in the earlier screenshot. This changes test capture, not recognized text or model accuracy.
The screenshot words/boxes (including the emoji) are **injected synthetic OCR results** for
controller/selection testing. They are not proof that Tesseract recognizes emoji. A separate
native Tesseract fixture checks actual text-word-region extraction.

The failed large-font Core frame is a black system/home surface, with an attached but
unfocused app fixture; configured preflight had a null focused window. This does not establish
a product crash root cause. Preserve it rather than blaming or silently ignoring app errors.
The new HOME settlement only relaunches the external emulator launcher and requires three
stable focused samples; it never force-stops the app or dismisses its ANR/crash dialogs.
All fixture focus assertions remain hard failures, with first-failure-time system dumps added.
