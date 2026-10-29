# Local AI: privacy-first decision record

Date: 30 October 2026. This is an assessment, **not an announcement of a shipped generative assistant**.

## What the app already does locally

The runtime declares Tesseract4Android for OCR and Vosk for speech recognition. OCR languages and the speech model are bundled through the existing asset pipeline, not downloaded by a runtime assistant. Recognition can be inaccurate; editable review and preservation of the original audio remain essential. These are on-device machine-learning features, not a conversational language model.

The standalone app manifest currently has no INTERNET permission and disables Android application backup. The runtime manifest declares a non-exported, temporary-grant image FileProvider. These are valuable boundaries, not a guarantee that every possible host/provider is private. An embedding app can have different permissions. A user-selected Android document provider or share destination can synchronize externally; choosing a destination is not equivalent to the app uploading notes itself. Backups/exported files need their own protection, and the current app must not be described as end-to-end encrypted or as an encrypted vault.

## Recommended order

1. Finish reliable editing, undo, selection and recovery first. AI must not cover up missing edit tools.
2. Improve the existing local OCR/transcript review without silently changing recognized content. No automated whole-note rewriting, background indexing or voice processing beyond the explicitly selected recording.
3. Only then prototype an optional offline text model behind a genuine local engine integration. Do not add an AI action that only shows a coming-soon notice or disguises deterministic capitalization/cleanup as generated reasoning.

## A possible useful first generative workflow

- User deliberately selects text and chooses an offline action such as Summarize or Rewrite.
- Before the first run, show exactly which text will be passed to the local model. Default scope is the selection, never the library, labels, attachments, recordings or unrelated notes.
- Model import is an explicit local-file operation with size, compatible format, expected identity/hash and license validation. No automatic download, cloud fallback or remote inference. A hash proves file identity, not that a model is safe or trustworthy; the manifest must have a trusted provenance.
- Cancel must stop inference, release engine resources and leave the note unchanged. Clearly show running/stopping/error states and protect against duplicate Apply.
- Show generated output in an editable preview beside a clear source reference. Mark it as AI-generated and potentially wrong. Apply is explicit, checks that the source selection/document revision has not changed, and creates an Undo checkpoint. Cancel/Back does not overwrite the note.
- Do not automatically follow instructions found inside note text. Output is inert text, not executable commands, URLs to auto-open, filesystem actions or reminder scheduling.

## Engine evaluation requirements (not yet implemented)

A quantized CPU-capable engine such as a suitably audited llama.cpp-based integration could be evaluated. No engine version or model is approved here. Choice depends on Android API/ABI support, license, native memory ownership, bounded context length, cancellation, threading, reproducible native builds and actual device measurements. APK-size allowance does not remove RAM, battery, thermal or latency limits. Do not promise a particular memory footprint or response time without measuring the exact model and device.

Start with small context and one active session; refuse oversized inputs clearly instead of truncating silently. Handle low memory, unavailable acceleration, unsupported model formats, corrupt files and app backgrounding without losing edits. Do not run inference on the UI thread. Keep model files out of ordinary note backups and out of this Git repository.

## Mandatory privacy and qualification gates

- Inspect the **merged manifest for both standalone and embedding host**, not just the source manifest. Audit engine dependencies and runtime sockets; no analytics, crash payloads containing notes, prompts, inference output or microphone content.
- Explicitly test airplane-mode operation and denied network access. Keep log/diagnostic events free of user text and model tokens. Existing opt-in diagnostics must not become an AI telemetry channel.
- No prompt history by default, no training/fine-tuning on notes, no embeddings/index database unless separately explained and explicitly enabled. Remove cached sessions when the user clears them; disclose that guaranteed forensic erasure on flash storage is not promised.
- Verify model/source integrity, local path traversal resistance, oversized/corrupt input handling, native memory safety, cancellation and concurrent activity recreation. Native engines process untrusted model files and require a real security review.
- Measure cold load, warm inference, peak memory, responsiveness, thermal/battery effects and failure recovery on the target Motorola and supported low-memory devices. Test relevant languages with representative text; model availability is not an accuracy guarantee.
- Require the usual source/build/lint/native/release gates and user acceptance. Existing CI allowance is exhausted; this assessment authorizes no new jobs or release.

## Current outcome

**Local AI is feasible to investigate, but not yet integrated or qualified.** No generative engine, model downloader, cloud service, prompt collection, note indexing or new AI permission has been added in this refinement batch. The new image editor is deterministic geometry and is deliberately not labelled AI.
