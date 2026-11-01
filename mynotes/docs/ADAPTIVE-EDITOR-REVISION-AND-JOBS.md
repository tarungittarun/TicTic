# Adaptive editors, full-resolution previews — fresh40-job authorization

Request 2026-11-01; baseline931e681c (delivered0.23.0/code26).
New40 individual Actions-job cap, separate from the closed prior20/40 allocation.

## Acceptance
- Zoom image/drawing previews decode full stored resolution, not inline thumbnails. Diagrams redraw
  vectors at zoom scale. Never invent lost source pixels; existing imports were bounded/re-encoded.
- Doc Insert adopts the grouped, labelled Notes Insert surface; no duplicate Hyperlink bottom tool.
- Notes Format adopts Doc's format controls (font, size, clear, paragraph and numbering). Hyperlink
  lives in Insert in both editors and supports selected multi-line text with one destination URL.
- No visible Write text hints in Doc; empty writing lines remain editable by default.
- Notes overflow moves from the bottom dock to the top-right corner.
- Landscape editors move top/bottom controls into left/right vertical rails, keeping the document
  between them and frequent actions low. Preserve focus, selections, Undo and keyboard reachability.
- Inspect surface depth, separation, accessibility, portrait/landscape and large text across the app.
- Split exhaustive native inventories into more parallel jobs without omitting methods; count every
  individual job conservatively against40. Monitor through completed qualification, not pending.
- Calculator/tests.yml frozen; Android builds only in Actions; root QA gates every push.

## Attachments
User diagnostics(7) and six-page PDF inspected locally only; do not commit private attachments or text.
Diagnostics include history from older builds; inspect final environment before attributing events.
Full-resolution display cannot reconstruct pixels discarded by older imports.

## Jobs
| Reservation | Jobs | Allocated | Source/run | Result |
|---|---:|---:|---|---|
| Candidate1 | 7 | 7/40 | ba42bd08 / 41286387638 | failure |
| Candidate2 | 9 | 16/40 | a8e5fc8b / 41287670974 | failure |
| Candidate3 | 9 | 25/40 | abfa0ba8 / 41288909525 | failure |
| Candidate4 | 9 | 34/40 | 7fd06c8e / 41290068899 | failure |

40 allocated; 34 expanded at final reservation; 0 unallocated. Prior allowances are not added.
No public release or billing approval inferred. Historical reservation-time counts below are retained.

## Candidate 1 — reservation before launch
7/40 slots reserved (build + API26 a/b + API36 a/b + dark200% + landscape).
0 executed at reservation; 33 remain unallocated. No results claimed yet.
Full API inventories are method-sharded (151 + 140 = 291), disjoint and exhaustive,
including CoreJourneys on both halves. The 11 PDF tests stay together on shard a;
focused profiles retain all155 methods and PDF verification. JVM inventory260.
Local root631 tests PASS; changed/new Java parses; Calculator/tests.yml unchanged.
Candidate version0.24.0/code27, same package/signing policy. No public release authorization.

Implemented: shared FormatControls in both data-model-independent editors; grouped shared InsertPalette;
Notes top-right overflow (Pin/Archive retained inside it to avoid clipping the new corner action);
Insert-only multiline Hyperlink, Notes font/size/number controls and pending inline typing;
scrollable actual left/right controls in landscape with a side Format panel; raised/bordered tool surfaces;
full-stored-resolution zoom decode and uncached vector redraw; no Doc Write text hints.
Native authored checks cover shared Notes size/number persistence, multiline hyperlink/mixed styles in
both editors, actual rail geometry, and1600px stored preview decoding. Qualification still pending.

Private attachment review (metadata/stack categories only): six A4 PDF pages; embedded images
937x1600 and244x455. Vector/text pages remain vector/text; smaller old raster sources cannot be
made original-resolution by zoom. Both historical uncaught stacks point to selectionToChecklist;
retain the shipped collapsed-caret List regression and exact content/Undo tests. Attachments stay private.

## Candidate 1 result / candidate 2 reservation
Source ba42bd08; run41286387638 completed FAILURE. All7 jobs executed; build succeeded
(260 JVM tests and lint), native API26 a150/151 b138/140; API36 a149/151 b139/140;
dark153/155; landscape143/155. Four PDF verification steps succeeded. Not qualified;
do not distribute that APK as the finished revision. Fixture documentation commit failed;
new gate diagnostics will annotate failures rather than leaving only inaccessible runner logs.

Corrections prepared from source review/results: Notes writing column now measures against the
actual centre viewport (including a side Format panel), not the full display width; Find attaches
to the vertical owner above rails. Keep Notes top actions reachable when Format makes portrait
compact. Pin journey opens its new overflow location; native soak uses Normal paragraph, not the
obsolete Text label. Pending inline spans coalesce, candidate link/format changes validate before
changing the native field, explicit accessible blue hyperlinks, multiline rail labels and outlined
shared dialogs. One additional native pending-typing test preserves earlier words and Clear scope.
API26's one caret/format visibility failure remains under requalification, not silently called fixed.

Candidate2 reserves9 further individual jobs BEFORE launch: build + two disjoint shards of each
of the four profiles. Total16/40 reserved,7 executed at reservation;24 unallocated remain.
Inventories292 exhaustive and156 focused; each pair's union is exact and disjoint. Root632 PASS.

## Candidate 2 result / candidate 3 reservation
Source a8e5fc8b; run41287670974 completed FAILURE. All9 further jobs executed; cumulative16.
Build/JVM260/lint passed. API26 a150/151 b139/141; API36 a150/151 b140/141;
dark a83/83 b72/73; landscape a83/83 b71/73. PDF verification passed on all four a shards.
Rail geometry, image controls and long-press regressions from candidate1 now passed.
Remaining failures: old Pin-icon description assertion after the action became a menu row;
API26 fixture's Back-based keyboard preparation dismissed Format/finished an activity during stale
insets; short landscape Find left its title ahead of the writing line; an immediate API36 accessibility
percentage assertion. Keep all original visible-line, formatting, pixel/zoom bounds and persistence
assertions. Prep now hides only the IME through Android's input service; Pin is tested by opening
its overflow and reading its real visible name; short rails hide the nonfocused title (Find can still
reveal/focus a matching title); zoom assertions await bounded accessibility event propagation.

Fixture commit's root632 + Notes242 gates PASSED, then the SHA-pinned checkout had no named local
branch to push. Candidate3 binds only the approved session branch before the gated fixture commit;
all native checkouts remain pinned to the tested source SHA. No other branch is created or pushed.

Candidate3 reserves9 more jobs BEFORE launch: cumulative25/40 reserved,16 executed at reservation;
15 remain unallocated. Inventories unchanged292 exhaustive /156 focused /260 JVM. Root634 PASS.
This is not a claim that the pending candidate passed, nor authorization for a public release.

## Candidate 3 result / candidate 4 reservation
Source abfa0ba8; run41288909525 completed FAILURE with8/9 green jobs. API26 292/292,
API36 291/292 (a151/151,b140/141), dark156/156, landscape156/156; JVM260/lint passed.
PDF checks passed on four a shards. Fixture commit997f8514 successfully preserved337 synthetic
JPEGs, original PNG hashes, native reports and provenance; no missing profile. Corrected layout,
Find visible-line, multiline link, size/numbering/pending-style tests passed. Only failure was an
API36 motion-enabled native tap: Drawing remained hidden after Insert was tapped during IME
hide. It is not called a pass. Production code stays unchanged for candidate4.

Test preparation now observes Android WindowInsetsAnimation completion and twelve stable layout
observations after hiding IME, not only the requested invisible inset; no Back navigation and no
retry of a missed tap. Native touch dispatch, drawing edit/pixel/identity assertions stay intact.
Existing native-report uploads explicitly overwrite their same-named artifact on a deliberate job
rerun, rather than leaving stale reports; no rerun has yet been invoked.

Candidate4 reserves9 further slots BEFORE launch: cumulative34/40,25 executed at reservation;
6 unallocated remain. Same292/156/260 inventories; root634 PASS. Review of candidate3 previews
has started; final candidate results and final visual review remain pending.


## Candidate 4 result / final candidate 5 reservation
Source7fd06c8e/run41290068899 FAILURE; evidence8654e028 published all eight profiles.
API26 291/292 (a150/151,b141/141), API36 292/292, dark156/156, landscape156/156;
JVM260/lint and four PDF verification steps passed. API36 drawing test now passed.
Only API26 nativeCaretAndReverseSelectionStyleWholeParagraphOnly did not open Format.
That paragraph-metrics fixture still edited text while IME was visible: it never called
closeKeyboard, so the prior helper improvement did not apply there. Explicitly settle IME
BEFORE setting that fixture's text/caret. Native Format/Heading taps, reverse-selection,
mark-boundary and glyph pixel assertions all remain; dedicated keyboard journeys stay intact.
Production code unchanged. This is preparation, not a claim of a resolved production bug.

Reserve final6 jobs BEFORE launch: cumulative40/40,34 executed; zero unallocated.
Build plus API26a151/API26b141/API36all292/darkall156/landscapeall156. This retains
all896 native outcomes and260 JVM tests without duplication/omission; reduced parallelism
fits the six remaining authorized slots. Coordinator requires all five native artifacts.
Full-profile instrumentation has a bounded1500s deadline rather than split-profile1200s.
Root/Notes/diff gates required before push. No extra job or public release authorization.
Visual review144 scenes recorded in ADAPTIVE-EDITOR-VISUAL-REVIEW.md.
