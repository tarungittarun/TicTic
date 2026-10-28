# Audio-inclusive archive: next implementation contract (not shipped)

The 0.12 repair does **not** include original recordings in JSON backups. Export important Voice inbox WAVs separately. This document defines the next slice; it is neither a working feature nor a substitute for native and physical acceptance.

## Compatibility and user actions

- Keep existing JSON format1/2 import/export and bounded, image-inclusive note generations unchanged. No silent schema migration or replacement of old backups.
- Add a clearly named **Full backup (notes + recordings)** action and a separate archive import/merge preview. Explain that ordinary JSON excludes audio, and display recording count/size before starting.
- Full backup contains the same authoritative note snapshot, drawings/images already embedded in that snapshot, completed original WAVs, and bounded recording/transcript metadata. Exclude incomplete captures, models, caches, diagnostic logs, platform permissions and arbitrary external files.
- Export locally with the existing reviewed document-provider policy. No cloud exception, Internet permission, automatic upload or storage-wide permission. Do not hold a complete WAV/archive in memory.

## Container and trust boundaries

Use a versioned ZIP container with one manifest, one library JSON document, and exactly enumerated audio/metadata entries. Freeze the exact manifest encoding/version and limits alongside executable fixtures before wiring UI. SHA-256 detects corruption; it does not authenticate an untrusted archive.

- Manifest identifies each entry by a canonical relative path, byte length and SHA-256. Reject duplicate manifest/ZIP entries, unknown required versions, missing or surplus entries, absolute paths, separators in IDs, dot components, symlinks and unsupported compression/encryption.
- Validate IDs with `VoiceFiles`' actual ownership rules rather than inventing a second filename authority. Never extract using a ZIP name resolved directly against the filesystem.
- Bound manifest size, item count, per-entry bytes and **actual aggregate uncompressed bytes**, independent of ZIP header claims. Check available disk space and retain headroom; compression ratio alone is not a safety check. Freeze practical limits after measuring real long recordings and expose the limits before export.
- Stream using a fixed-size buffer, account every read/write, check cancellation and handle interrupted/short/zero-progress streams. Validate WAV headers and full digests, not merely filename or a successful stream close.
- Reject malformed or conflicting same-ID content. Existing identical verified recordings may be reused; never overwrite a different local recording just because an imported name matches.

## Snapshot and restore transaction

1. Serialize export against the authoritative repository revision. Snapshot completed recordings and stable metadata; do not race an active capture, transcription completion or metadata replacement. Check source stability while streaming; fail rather than publishing a mixed snapshot.
2. Stage and verify archive import in app-private temporary storage. No live library/audio mutation during validation or merge preview.
3. Show note/recording additions and conflicts. Recheck the base revision after confirmation; recompute the preview or reject a stale confirmation rather than losing intervening edits.
4. Publish only verified, uniquely owned new recording files, copy/verify-first. Commit the merged note library through the same repository/generation protocol as ordinary saves. Keep a durable journal sufficient to resume/abort the multi-file operation after process death.
5. If the library commit fails, never delete pre-existing recordings, notes or provider files. Orphaned newly staged/imported audio must not trigger placeholder recreation or late-ASR resurrection. Define journal recovery and source-application/deletion guards together; retaining audio alone is not sufficient.
6. Mark completion only after verified commit. Discarding an import must leave the live library unchanged. Cleanup must be exact-owned, retryable and separate from speculative media garbage collection.

Original recordings must remain playable/exportable if inference fails or imported model versions differ. Import must not automatically rerun recognition, unexpectedly replace reviewed transcript text, or recreate a manually purged note. Existing backups and recovery generations are not forensic-erasure targets.

## Required tests before publication

- Round-trip empty, image/diagram/checklist-rich, multilingual and large-recording libraries; format1/2 JSON compatibility unchanged.
- Corrupt/truncated ZIP, CRC/digest/header mismatch, duplicate/extra/missing entries, traversal/absolute names, invalid voice IDs, oversized declared and streamed data, compression bombs, zero-progress reads and cancellation.
- Equal-ID/equal-content reuse versus unequal-content conflict; corrupt existing local audio must not be overwritten automatically.
- Active capture and concurrent ASR/edit/Trash/purge/import races, stale preview confirmation, late source application and durable note-created/deletion guards.
- Process death and injected failures at each staging/publication/journal/generation boundary; retry idempotence, last-good library and all previously existing audio preserved.
- Real SAF local export/import, provider revocation, full disk and partial writes on API26 and modern Android. Assert bounded memory with long real files; do not substitute an in-memory toy stream for the size path.
- User-visible progress/cancel/error recovery, no blank after-use sheets, large-font/dark/landscape layouts, final-source installed-byte update checks, then physical Motorola/SD-provider trials.

Reminder recurrence/snooze/missed-event semantics and per-instance interactive widgets remain separate slices. Wall drag ordering stays last.
