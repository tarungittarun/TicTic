# MyNotes 0.6 — offline OCR and automatic voice transcription

## Accepted checkpoint

[Run 40582488414](https://github.com/tarungittarun/TicTic/actions/runs/40582488414) passed all 20 Android lanes: **64 JVM tests, 80 unique Android tests, 662 Android executions**. The native APK is **66,490,360 bytes**, SHA-256 `751c02483ffa7900229bc8a936f8370dbb70f88358588401b8faf842b150484b`, source `fcb8830a2c4bf904bb9cfb59200a07cf30806aad`. [Direct APK](https://github.com/tarungittarun/TicTic/releases/download/mynotes-v0.6.0-7/MyNotes-v0.6.0-7.apk).

The previous attempt exposed a real landscape-fixture problem: launcher orientation could override the rotation setting. The harness now locks display rotation and ignores activity orientation requests in that lane; the original strict landscape assertion passed unchanged. Corrupt transcript metadata is isolated per recording, durable note-created markers prevent purged voice notes returning, and controller recreation journeys cover recovery/deduplication. These are emulator results, not universal device or language-accuracy claims.

## Acceptance criteria

- **OCR-01** Bundled English/Hindi/Kannada model bytes match pinned Apache-2 provenance. No model download occurs on device and no network permission exists.
- **OCR-02** A clean generated English image runs real native Tesseract inference with radios disabled, producing expected words. Blank images do not invent text; Hindi/Kannada model initialization is exercised, not claimed to establish their recognition accuracy.
- **OCR-03** User picks a local image, selects a language, reviews editable recognized text and explicitly appends it or creates a note. Cancel/failure does not replace note content or the source image. Huge/invalid/cloud inputs are refused with an explanation.
- **VOICE-01** Microphone is requested only from Record voice; denial leaves other features operational. Granted emulator lanes create real PCM WAV bytes and finalize a readable header. The app is foreground-capture-only: leaving the app stops capture.
- **VOICE-02** WAV is durably checkpointed/finalized **before** bundled Vosk inference starts. Transcription begins automatically after Stop. A public speech sample runs real native offline inference and produces expected words.
- **VOICE-03** Native failure, cancellation or process interruption never deletes the original. Incomplete PCM framing is recovered from actual durable bytes; an active locked recording is not renamed by an inbox scan.
- **VOICE-04** Original WAVs can be played or exported to a local Documents destination and verified by readback. Transcripts are stored independently and applied idempotently to stable voice-note IDs. Editing/Trash are not silently overwritten/resurrected by recognition.

## Engines, models and licensing

**OCR:** Tesseract4Android 4.9.0, upstream tag commit `15c534717b1cb58261b58d4e4c1200c7f81f668c`; Tesseract 5.5.1, Leptonica 1.85.0, libjpeg v9f and libpng 1.6.48. Version-pinned standard single-threaded AAR from its documented scoped JitPack repository. tessdata_fast 4.1.0 commit `65727574dfcd264acbb0c3e07860e4e9e9b22185`, English/Hindi/Kannada models packaged in the APK, each SHA-256 pinned in `runtime/src/main/assets/ocr/models.json`.

**Speech:** Vosk Android 0.3.75 and JNA 5.18.1, version-pinned Maven artifacts. Official `vosk-model-small-en-us-0.15` English CPU model, Apache-2 per upstream's pinned model inventory. Archive **41,205,931 bytes**, SHA-256 `30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498`; every uncompressed member is size/hash pinned in `runtime/src/main/assets/voice/model.json`. It is downloaded/verified **at build time only**, not by the app. Acquisition [40578093243](https://github.com/tarungittarun/TicTic/actions/runs/40578093243) inspected the official TLS source; these hashes pin repeatable bytes, not independent authorship attestation. The large archive is not stored in Git; the build cache/generated assets are disposable. Runtime verifies archive and member bytes before private extraction.

Bundled licenses and upstream notices are available under **Storage → Offline models & licenses**, including Apache-2, Leptonica, IJG, libpng and JNA's Apache-2 option. Native AARs/models are unmodified. This is not a transitive CVE audit or blanket legal opinion. Supported packaged ABIs are ARMv7, ARM64 and x86_64; other ABIs are not promised. Compressed JNI packaging avoids reliance on old-AGP uncompressed ZIP alignment, but 16-KiB-page physical-device compatibility still requires separate qualification.

## Device/data behavior

No INTERNET permission, Android SpeechRecognizer, cloud API, telemetry uploader or application-side model downloader. **Only RECORD_AUDIO** is declared; microphone hardware is optional and permission is contextual. No camera/storage permission is requested; OCR uses a local document URI grant. Sources from cloud/unknown providers are rejected.

OCR copies the selected image to private temporary storage, checks compressed input bounds (32 MiB), downsamples to a bounded working bitmap (4 million pixels / 2400 longest edge), applies EXIF orientation, and runs a single CPU worker. This bounds the recognition operation, not note/library size. English/Hindi/Kannada are explicit language choices; handwriting, mixed scripts, low light, skew, tables, numbers and unfamiliar fonts can fail or be inaccurate. Native confidence is not an accuracy guarantee. OCR text is reviewed before insertion; full original text/marks/checklist rows/diagrams are preserved when appending. Original image is unchanged, but is not an image attachment or backed up by MyNotes.

Voice capture is mono **16 kHz PCM16 WAV**, without an artificial note-size limit (RIFF's actual 32-bit container limit applies). It checkpoints length/fsync during capture; Stop finishes source bytes before starting CPU inference. Activity pause stops recording. No background microphone service is implemented. Automatic transcription is **English**; accents/noise/other languages may be wrong. Source audio remains in the independent **Voice inbox**, even for failed/empty recognition. Voice originals are never automatically purged with note Trash. Source IDs in schema 4 make transcript application idempotent through rotation/recovery/backup conflicts. Notes write schema 4 and read 1–4; stable package/signing and versionCode 7 preserve install updates.

**Important:** JSON backups/selected-folder note generations include transcript text/source IDs, **not WAV originals**. Audio lives in private Voice inbox until explicitly exported to Internal storage or SD via Export WAV; its export is readback-verified. Export important audio separately before uninstall/clear-data or moving phones. This is a candid boundary, not a full attachment/streaming-media backup format. Voice recovery/metadata errors must leave ordinary notes usable and audio retained. Process death during capture can retain a recoverable partial WAV; successful background transcripts are reconciled on next load. Failed/unfinished recognition may need explicit Retry from the inbox. No automatic cloud fallback exists.

## Qualification scope

Pipeline grows to **20 emulator lanes / 29 expanded jobs**, below the 30-job ceiling: existing aspects/resource variants plus OCR and speech on API26/34, full API35, and separate microphone-granted lanes on API26/34. Default lanes retain denied microphone access. Each verifies actual permission/configuration and exact signed release bytes separately from debug-host instrumentation. Real bitmap OCR, native model inference, WAV/header/locking recovery, corpus speech and positive/negative mic policy are automated. Native model inference is off the UI/disk-save queues.

Physical photo picker/EXIF camera corpus, physical microphone quality, noisy/Indian-accent speech, full language accuracy, audio routing/Bluetooth, memory/battery/thermal limits, screen readers and process-kill timing are not certified by these fixtures. Models need private disk space and native RAM; inability to initialize must preserve sources and other features. A green corpus is not universal compatibility.
