# Active device feedback / integration streak

User priorities: finish real on-device trained-model integration, then startup/viewers,
universal sectioned search, full filter row targets, 1 MB diagnostic rotation + viewer,
and saved-versus-opened file separation. No Internet permission, private input upload,
data clearing or silent replacement of the working speech engine.

## Evidence / boundaries

- User supplied a physical-device startup screenshot and private diagnostic report.
  These files remain outside Git; do not copy phone-identifying context or raw logs into CI.
- Screenshot is the existing asynchronous loading placeholder, not a log viewer.
  Export remains available while startup is pending. The report confirms spreadsheet
  extraction failed at the text budget. It does NOT include startup phase durations;
  exact startup bottleneck is not established from this report alone.
- Source has a shared serial disk executor for load/save/import. The supplied report
  contains many coalesced saves and long gaps between completed saves. This is evidence
  to investigate queueing and provider I/O, not proof that private data is corrupt.
- Previous 0.29 emulator qualification is not physical acceptance of these new requests.

## Work ledger

1. Trained Whisper export: preparing exact base + actual 593-step blocks for CPU inference.
   Checks both pinned SHA-256 values and exact tensor keys/shapes; never substitutes the
   untrained model if download/export fails. Export run 41406351456 SUCCESS, source
   e9cd60d2; trained-model artifact 13124080289, digest
   d9d46992f99c0d0bb3d4fcc2e1c46ade00249a199df9c88859cb6f508c51dcf9,
   retained until 2027-01-31. Android CPU integration source now includes pinned whisper.cpp
   v1.8.2 / 4979e04f, cancellable JNI, bounded 30-second PCM windows, checksummed private
   installation, unchanged audio, conservative low-RAM rejection, explicit experimental
   selection, mandatory transcript review and unchanged Vosk default. Native build/inference
   qualification is NEXT, not yet passed. Code 33 / 0.30 development build is not delivered.
   This is not universal Indian accuracy or physical acceptance. Build requires Actions
   artifact read access; expired exports fail closed rather than substituting base weights.
2. Startup: investigate shared disk queue and folder/provider I/O; preserve authoritative
   internal content and pending edits. Do not bypass migration/recovery fences.
3. PDF: continuous, lazy page rendering; page/occurrence counts, search, selectable text,
   copy and byte-preserving Save copy. No requirement that all pages stay in memory.
4. Word: paginated/layout view, embedded assets and a truthful fidelity limitation;
   preserve original bytes; no claim of exact layout without a compatible layout engine.
5. Excel: sheet/grid viewer independent of editable-note character limit; lazy/windowed
   cells, search/copy/count/Save copy; retain explicit archive/decompression bounds.
6. Search: content and viewed files first across workspaces; settings as a separate section.
7. Filters: whole labeled row is a 48 dp accessible selection target, not just the radio.
8. Diagnostics: rotate at 1 MB; bounded retention explained; in-app time/severity filtering
   (presets/custom) and export; no private note/URI/error-message leakage.
9. Distinguish owned notes/Docs from external read-only file history. Opening alone must
   not silently import it into the saved library or copy private bytes without consent.
