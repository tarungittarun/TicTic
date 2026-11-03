# Candidate53 — code52 physical feedback remediation

2026-11-03. MyNotes 0.50.0 / versionCode53. **Not yet Android-qualified or
Motorola-accepted.** Candidate52's green automation did not cover the subsequently
reported physical crash/save conflict; its report is historical, not a clean bill
of health for these issues. Private attachments are not included here or in CI.

## Findings and implemented changes

| Physical report | Candidate53 change | Remaining verification |
|---|---|---|
| Repeated incomplete-save notices | A separate typed outcome is emitted only after the latest private snapshot is written and read back. Private saves advance their acknowledgement and retain edit history. Folder pending stays visible; its explanatory notice is once per unresolved episode. Automatic provider retry backs off for 60 seconds while private saves continue; explicit Retry bypasses backoff, not the head fence. JSON-null folder heads are restored as Java null. | Actual device's folder divergence origin is not proven. No blind reset/overwrite. |
| Rearrangement crash | ItemTouchHelper completion posts outside RecyclerView layout, waits for a safe frame, fences newer data/gestures/detachment, and only then rolls back or saves the order once. Existing target latch retained. | New real-Android layout-pass regression plus existing actual drag journeys must pass. |
| No Home route from Archive/other library lists | Visible Back added to Archive, Trash, filtered label/folder lists. Android Back from Archive/Trash returns Home. Existing page headers retain Back. | Native Archive/Trash regression and broader profile navigation. |
| Larger TXT fails to open | Separate viewing bound: up to 8 MiB encoded input and 4,194,304 UTF-16 characters; 24,000-character native selectable pages without surrogate splitting. No truncation. Editable copies keep the existing 100,000-character limit. | Real SAF large TXT regression, Unicode/page round-trip tests. |
| Slow file opening | Read-only TXT/PDF/Word/spreadsheet parsing uses a separate reader queue, not the serial library writer/provider retry queue. Text layout is page-bounded; no main-thread whole-file word-array allocation. | No physical latency benchmark claimed. A currently running slow document provider still cannot be made instantaneous. |
| Viewed files appearing on Home | TXT and other supported text formats default to view-only. Only explicit Create editable copy adds a Note; Word import remains an explicitly selected copy action. PDF/CSV/Word previews remain separate from Home. | Real SAF small/large TXT no-mutation/copy tests. |
| Colourless diagram shapes | Existing valid colour zero now means no fill (not white/paper); editor labels it Colourless. No schema change; border/text retained. | Pixel-level Android drawing test, existing codec/diagram journeys. |
| Embedded diagram written controls | Legacy Note diagram uses Edit icon and overflow actions, matching image cards. Zoom/Rename remain available in overflow. Canonical content already uses its shared content controls. | Physical discoverability/appearance pending. |
| Reminder rename text hidden at left | Rename content sheet uses explicit side margins, start gravity and a wrapping field capped at four visible lines rather than horizontally scrolled single-line input. | Physical keyboard/font appearance pending. |
| Changing Filter/view height | Measure tallest tab at construction, reuse that viewport across tabs, cap the filter sheet at half the actual shell height, and scroll overflowing tab content internally. Other sheet sizing unchanged. | Native all-tab/half-screen regression plus original large-font/landscape assertions. |
| Laggy native selection autoscroll | Selection rectangle requests use immediate native scroll rather than animated chasing; remove unnecessary legacy undo rescans on every selection change. Still native handles/selection, no replacement recognizer. | Motorola offscreen selection smoothness is not yet measured/accepted. |

## Safe folder-difference repair

Settings → Storage & recovery → More options & repair → **Review selected folder
difference** previews both libraries without changing either. Confirmation merges
differing versions as conflict copies, writes the previous private state checkpoint,
and commits only against the exact head reviewed. If another writer changes the
folder after preview, the expected-head check still refuses the write.

Selecting another complete App folder remains notes-only import; it does not become
a replacement destination. Only reviewing the *already selected* folder can repair
that selected destination through the existing confirmed merge protocol. No
uninstall, clear-data, external-generation deletion or private diagnostic upload.

## Qualification status

Authored inventory: 310 JVM tests; 414 distinct Android tests. Planned eight native
profiles/shards: 212/202 (API26), 148/138 (API34 landscape), 148/138 (API34 large-font
night), 212/202 (API36), totaling **1,400 native outcomes**. These are planned counts,
not execution results. Root safety gate **803/803 passed** before source push (7.705 seconds). Source
`ebf4a1d2bbfa8fdcfa9d953fdf1ef9a1681ea9ad`. Actions qualification is running at
https://github.com/tarungittarun/TicTic/actions/runs/41578780795 . No Android result
is claimed yet. Android compile/lint/JVM/native results must be recorded before
calling candidate53 qualified. The source workflow uploads a development APK, not a
public Release. Stable signing/package and install-over upgrade constraints remain.
