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

## Round 10 findings; next-source implementation (not qualified delivery)

Run **41409690954/source5c3fb6ad** FAILED: build passed; four focused shards
passed (125/125,115/115 for both landscape and 200% font). API26 A186/187:
far-edge radio test did not select. Both full B shards176/177: actual trained
inference timed out180s. Intentional worker death/cancellation passed on both;
there was no recurrence of the malformed-model loader SIGSEGV. API36 A had zero
outcomes/incomplete instrumentation, so it provides no runtime acceptance.
Exact immutable reports are retained under that run's native evidence directory.

Next-source changes, pending Actions compilation/native qualification:
- CPU Debug C/C++ gets `-O3` while retaining debug assertions/symbols. O0 kernels
  are an identified CPU issue, not yet proof that the entire timeout is resolved.
  Content-free model-verification/load/ready/decode phase messages go through IPC;
  app-private diagnostics remain opt-in and extra logcat markers are debug-only.
- Far-edge test now uses actual screen coordinates and SOURCE_TOUCHSCREEN on both
  injected events; all width/selection/Apply/reopen assertions remain intact.
- XLSX/CSV open directly in a sparse, virtualized vertical/horizontal cell grid,
  bypassing the100k editable-note import. Workbook relationships preserve tab order
  and cell references preserve gaps. Fixed column headings, cell read/copy,
  go-to-cell, current-sheet value/formula search, counts, original-byte copy/reader.
  **Not Excel fidelity:** raw cached values only; no number/date formatting,
  merges, charts/images, recalculation or macro execution.32MiB input,250k populated
  cells,8m characters, bounded archive expansion. Legacy XLS still needs a reader.
- PDF is continuous RecyclerView pages, bounded rasterization and offline embedded
  text extraction/search/count/navigation/copy, automatic bounded viewed indexing,
  original-byte copy and optional external reader. Separate renderer/text ownership
  retains temporary files until both close; PDFBox scratch capped64MiB. No scanned
  PDF OCR, visual match rectangles, pinch zoom or configuration-state restoration yet.
- Saved Notes/Docs (including archived, not deleted) precede viewed files and then
  settings in global search. Empty-query workspace/filter behavior remains separate.
  Viewed index is private, last32files/up to2Mi characters each, explicitly partial
  when interrupted/capped. Unicode61 keeps Indic combining marks; real FTS4 prefix
  syntax corrected. Emoji/punctuation uses escaped literal LIKE, not wildcards.
  Clearing the index never deletes original files or saved content.
- Byte copy rejects same URI/document and matching physical inode/device before
  any writable descriptor;64MiB cap and explicit partial-destination failure. Cannot
  verify pipe providers: reject safely rather than risk source alias overwrite.
- Diagnostics now rotates at1MiB per file (current+previous, at most2MiB), stores
  inferred DEBUG/INFO/WARN/ERROR, and offers a native parsed on-device list, presets,
  custom local date/time range, levels, record selection and filtered local export.
  View/export newest5,000 matching records; all-log export remains separate. No note
  content, exception messages or automatic upload added. Existing Java/OS kill gaps
  and bounded queue omissions remain; this is not system-wide logcat capture.
- DOCX now offers the unchanged original-layout reader BEFORE limited text import,
  so the import cap no longer blocks that option. External reader privacy/network
  control is disclosed. **Internal high-fidelity paginated Word rendering remains
  unimplemented**, including imported logos/headers/images/inherited style fidelity.
- Added native PDF resource/text/vertical-container, large spreadsheet/sparse/order,
  quoted CSV, hostile archive, English/Indic/emoji FTS, copy rejection, universal
  saved-before-viewed-before-settings and diagnostic parsed-range/level tests.
  These are authored coverage, not passing results. Local gate721 PASS; all changed
  Java parses with a syntax-only analyzer, which is NOT Android compilation.

No replacement APK, physical startup/playback/emoji/Indian-speaker acceptance or
public release has been delivered/approved. Last delivered APK remains0.29/code32.

### Round 11 compile gate and follow-up

Run41413721256/source13edccb4 stopped at owned instrumentation compilation;
no emulator shards executed. Runtime Debug sources compiled, but test code used
PdfDocument as AutoCloseable (it is not), and directly referenced implementation-only
RecyclerView/PDFBox classes absent from the app test compile classpath. Fixed with
explicit PdfDocument try/finally and pinned androidTest-only dependencies matching
runtime versions. No assertions removed; full qualification is still required.

Additional correctness review fixes: cancellation has a unique request ID so a late
cancel cannot stop a later request; late binding after caller exit is ignored;
process-wide gate prevents overlap with a service instance still stopping. Copy now
also rejects every nonempty destination before opening for writing. Filtered diagnostic
export reports success only after stream close. These are source-reviewed/root-tested,
not qualified Android results. Local gate722 PASS; native inventory372 methods.

### Round 12: actual trained CPU inference passes; full qualification still fails

Run41414920566/sourcefcda199a, immutable evidence6d8e459c:
- API26 A191/191; API36 A191/191; landscape A126/126.
- API26 B180/181 and API36 B180/181. Both include PASS for
  `actualTrainedCpuModelRunsOfflineAndPreservesPublicAudio` and private-worker death.
  This is actual CPU inference with the bundled trained model, not a structural-only
  check. No measured native WER, broad accent, ARM physical-device or latency claim.
- Landscape B114/115, large-font B114/115: each fails the stale PDF Next-page button
  expectation. Source now uses real accessible vertical RecyclerView scrolling and
  keeps the original-byte/no-import assertions; no assertion-count reduction.
- Large-font A64/126, no completion: retained main/worker stacks prove a lock inversion.
  Main held LocalSystemSurfaces' class monitor and waited for SNAPSHOT_LOCK; schema
  upgrade writer held SNAPSHOT_LOCK and waited for the surface monitor. Source now
  uses a single reentrant snapshot monitor for coherent projection and writing.
  This is a real diagnosed deadlock; whether it explains the user's physical startup
  screenshot remains unproven. No blind IME retries were added. API36 A completed.

### Next-source implementation awaiting Android qualification

Internal DOCX prepared-paper preview now renders stored paper geometry, explicit/cached
breaks, inline PNG/JPEG/etc images and default header/footer logos into a private native
PDF, reused by the continuous viewer. XML page-source text preserves Unicode for
search/count/copy rather than depending on PDF glyph reverse mapping. Original DOCX
bytes are never written. It is **explicitly approximate**, not a full Word engine:
fonts substituted; tables linearized; floating drawings, advanced styles/fields,
merged cells and multiple-section layouts may differ. Original-reader and separate
editable-copy options remain. No external conversion service, macros or URL fetching.

Caps:32MiB input/retained parts/rendered PDF,64MiB decompression,4096 entries,
20,000 paragraphs,50,000 characters per shaped paragraph/1,000,000 total,
4MP per decoded image/8MP unique total,1000 pages. Unsupported images get visible
placeholders and warning details. Renderer failure leaves source and notes unchanged.

Viewer actions moved into a 48dp accessible options control; title limited to two
lines. Current-page detection uses the most visible page, not the first sliver (which
could otherwise keep page1 indicated even at the end of a short two-page document).
Copy now opens one nontruncating `rw` descriptor and verifies that exact descriptor's
inode/device/empty size before writing—no check/reopen pathname race.

Authored native regression coverage includes rendered red header-logo pixels and
stored page breaks, Unicode page source, hostile Word XML/paths, cancellation before
late binding, coherent projection/schema upgrade checkpoint retention, and real SAF
copy success/nonempty rejection. Authored tests are not yet passing Android evidence.
