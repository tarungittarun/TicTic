# Candidate 68 viewer audit — work in progress, not acceptance

## Evidence boundaries

No candidate68 APK is delivered by this document. Full matching E2E, migration checks and signed delivery remain gated on actual passing runs. Emulator assertions are not physical-phone approval. Source contracts are not Android execution.

Initial Android run **41913118570**, source `1f69bcbad27d8801dcf0d70685c961e2980d0a06`, compiled debug/instrumentation/release and passed lint and the existing release-size gate. Its native qualification failed and is retained as failed evidence. One completed dark-landscape lane reported **51/55** passing methods: two legacy tests assumed the RecyclerView was a direct root child; actual file-width measurement and search/highlight also failed. Do not describe that run as qualified. Later source changes need their own results.

## Prior report visual inventory

Reviewed the retained report inventory at `module-e2e/41892075423/provenance.json` and all **132 distinct non-OCR-input scene IDs** as primary-profile contact-sheet layouts, with full-size inspection of the Word renderer image, spreadsheet landscape, large TXT portrait and OCR review. This is a broad layout triage, **not individual full-resolution review of all four profiles**.

The old inventory contains **912 images**: **384 `ocr-NNNN` benchmark input images**, plus **528 other image records** across the four profiles. Benchmark input pixels must not be counted as visited app layouts. Four `word-synthetic-paper-page` records show rendered paper bitmaps, not attached viewer windows. Their report description now explicitly excludes toolbar, keyboard and navigation qualification.

Previously identified exact duplicate groups are Library entry-sheet captures: they can legitimately share pixels before different destination pages. Duplicate hashes alone do not establish stale navigation. The original receipt is immutable.

### Findings and source changes

| Surface | Observation in retained images/source | Current change / limit |
|---|---|---|
| PDF | No attached continuous PDF-viewer state in the old 11-scene File viewers screenshot group; PDF export options are not PDF viewing. | Seven new declared native viewer scenes cover the actual attached PDF window, search, rail, recognition loader and marking; passing execution is still required. |
| Large TXT | Generic “File preview” heading plus a second action row; pagination controls scroll with the file body. | Filename/Search/options in the header, persistent bottom pagination, no editable copy until explicitly chosen. Current-page Find remains a documented limitation. |
| Word | Raw prepared-page image cannot expose viewer chrome problems. | Shared compact PDF-derived viewer; source is unchanged and approximate Word rendering is explicitly identified in metadata. An attached Word screenshot is still needed. |
| Spreadsheet | Old fixture uses an artificial heading and its own grid controls, not the actual controller navigation. | Filename toolbar with native Back, sheet selection, Search/options; controller duplicate header removed; IME Search supported. Keep full heading plus a complete grid row visible. |
| File details | Help/limitations were mixed with “details.” | Shared asynchronous original-provider metadata with actual name/location/type/size/modified and counts. Absent fields say Unavailable; Word preview cache size is not presented as original size. Visible metadata loader. |
| PDF extracted text | Static heading/count and selectable body were insufficiently distinguished. | Nonselectable type/count line and divider; selectable body separated from title; copy remains body-only. |
| Voice inbox and folders | Background reads began with a plain text placeholder. | Compact indeterminate progress rows while loading; existing result/error replacement and safe Back remain. |
| Viewed files | Background index query initially had text only. | Small real progress indicator, removed on success/failure; originals remain read-only. |
| Other rare pages | Trash/restore confirmations, diagram/group prerequisites, image removal, filter/library, reminder pickers, table/checklist options and diagnostic dialogs were included in the layout triage. | Preserve established native controls and data-safety confirmations rather than redesign unrelated workflows speculatively. Full current four-profile screenshots still require review. |

## Whole-file zoom cause

The initial native test expected doubled measured width but observed the original width. Merely changing a RecyclerView's layout width inside HorizontalScrollView does not force its auto-measured width under an unspecified horizontal measure spec. The correction sets the required minimum width as well as updating page geometry. Resizing retains existing holders/rasters instead of rebinding every page during pinch. Raster memory remains independently bounded. The updated test sends actual two-pointer window input and checks a distant page's measured geometry; a direct scale callback is no longer its acceptance evidence.

## Markup safety and coverage

Private drafts are content-keyed, bounded, atomically written and explicitly outside Notes backups. Malformed drafts must remain retained, not silently become empty drafts. Source PDF bytes are never edited. Export creates a separate private marked PDF; copying to SAF requires a new empty local regular destination, fsync and SHA-256 readback. Existing files are not truncated. Native coverage includes export text/ink checks and the actual SAF copy writer; latest declarations also inject pen/highlighter input into the attached viewer. Rotation/crop export coverage, interrupted export and physical usability remain explicit qualification gaps.

## Latest diagnostic file: complete structural review

Reviewed all **7,606 JSON records** after the header in the latest supplied diagnostic file: **0 malformed JSON records**, **33 event types**, spanning reported version-code slices31 through67. The raw file and identifying device context are not copied into the repository or CI.

- Across the entire retained file: **933 `folder.copy.pending`**, **176 historical `save.failure`-labelled records in version52**, **9 `open-file.failure`**, **1 `word.preview.failure`**, and **16 picker cancellations**. Historical event labels alone do not establish private-data loss.
- The latest reported67 slice contains **421 events**, including **61 `folder.copy.pending`**, **58 `save`**, **10 `save.coalesced`**, **58 `memory.trim`**, **48 `lifecycle.flush`**, and a completed startup. No event name in that slice contains failure/error/slow. That is a log observation, not proof that no unlogged UI issue exists.
- The latest startup phase entries report elapsed1–15ms for their recorded subphases; these are **not** PDF/Word/OCR opening benchmarks.
- All recorded file-open failures are in older slices31–64; the Word preview failure is in33. Do not attribute them to67 or claim the new viewer has repaired every historical file without reproducing those inputs.
- Current source durability writes the private AtomicFile before the optional folder copy. The external generation guard refuses overwriting a folder changed since inspection. The supplied redacted exception traces are insufficient to conclusively identify each latest pending-copy cause. **External-copy completion on the user's folder remains unresolved.** Do not disable that guard, claim the external copy is current, or advise uninstalling/clearing data.
- Memory trim events are signals, not crashes. Large-file bounds, lazy first-page opening, a32-page/2M-character text cache and independent raster bounds address foreseeable pressure, but need execution and device observation.

## Remaining delivery gates

1. Diagnose and pass updated native viewer lanes, including actual IME/Enter, two-finger zoom, direct selection release, annotations, original-byte preservation and SAF readback.
2. Inspect attached Word and current rare-page screenshots; distinguish fixture-render evidence from actual windows and direct callbacks from injected input.
3. Run complete matching module-ordered E2E in all four theme/orientation profiles; update descriptions, retain all failures and review actual captures.
4. Run exact-source install-over/migration and retention checks without uninstalling or clearing the user's data.
5. Only then deliver the next signed raw development APK and matching report with exact results and remaining physical/recognition limitations.
