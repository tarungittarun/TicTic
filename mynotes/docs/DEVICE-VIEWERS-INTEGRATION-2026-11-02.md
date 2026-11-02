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

## Startup and full-row filter changes prepared during native qualification

- Verified private-journal startup path stays on the existing serial writer queue and
  preserves recovery/head/pending fences; with an existing valid private copy it does not
  call a potentially slow/revoked external provider. Full folder protocol still checks its
  expected head on save. Missing private storage takes the existing full load path; corrupt
  private data is reported, never replaced with an empty library or external rollback.
- Loading now has a progress indicator, elapsed time and fixed-name phase breadcrumbs;
  diagnostic export appears after five seconds rather than looking like the normal home
  screen. Nonessential OS reminder refresh follows the queued UI-ready update.
- Radio/checkbox options fill their labeled rows with 48 dp minimum height. A native test
  injects an actual far-edge tap and checks the selected choice survives Apply/reopen.
- Three new native repository contracts check provider-free startup, corruption retention,
  and staged-recovery fences. Local gate 706 tests PASS; native qualification of these
  additional changes is pending, not included in the older integration build source.

## Round 9 failure and corrected model export

- Run **41406961762**, source c0140302: FAILED. Native loader SIGSEGV on both
  API26 and API36 full-inventory B shards. Exact synthetic reports are retained in
  `design-2026-10-29/native/41406961762/`; never confuse these with passing evidence.
- Concrete cause: trained optimizer matrices were float32. The upstream F16 exporter
  labels matrix payloads F16 but assumes input matrices are already half precision.
  Float32 bytes therefore corrupted subsequent dimension records in the GGML stream.
  Native backtraces show model-loader `read_safe<int>` overwriting its stack.
- Export now explicitly casts serialized matrices to F16 and checks the complete
  bounded GGML stream, including all 167 tensor records. Unit fixtures deliberately
  reproduce mismatched F32-payload/F16-header and truncation failures. No base-weight
  substitution or additional optimizer training took place.
- Corrected export **41409413622 SUCCESS**, source2373ce10c71223f210fd4e1536b6ff1118c99afd,
  artifact **13124627967**, 85,852,249 ZIP bytes, digest
  `86899e08eafa46c167c06a88e2d5428b38dff14897d656e6824f2715d5a54d50`;
  expires2027-01-31T07:55:28Z. This is export validation, not phone/inference acceptance.
- Experimental inference now uses a private same-UID `:offline_whisper` bound process.
  No exported component or Internet permission. Binder death returns an explicit error;
  original recordings remain authoritative and the default Vosk engine is unchanged.
  A new native test will deliberately kill only that worker, check recoverable error,
  original bytes, surviving Notes PID, and responsive main looper. NOT YET QUALIFIED.
- Local root gate **710 PASS**. Startup/filter and this model/process fix still require
  exhaustive latest-source Android qualification before any replacement APK handoff.
- Shared-toolchain edits also started standalone run41406961751 (28 jobs rather than the
  dedicated nine-job refinement). Cancellation was denied by integration permissions
  (HTTP403). Its publication remains subject to unchanged source-bound UX approval gates;
  no public release is authorized. Future shared setup edits are pushed with skip-ci.

## Viewer/search working tree, not candidate delivery

Continuous native PDF pages and offline PDFBox text search/count/copy, source-byte copy,
private bounded viewed-file FTS history, and saved-content-first global search have
initial implementations but remain uncommitted and Android-uncompiled. They are NOT in
round10 integration qualification. Excel spreadsheet view, Word layout fidelity, and
rotating/filterable in-app diagnostics are still pending. Physical startup, playback,
emoji OCR and Indian-speaker accuracy are not accepted by emulator evidence.
