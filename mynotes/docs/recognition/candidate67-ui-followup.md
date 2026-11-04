# Follow-up to pre66 physical feedback — in progress

No next APK has been offered. User has not tested66. Intermediate diagnostic
builds retain version66; the distributable version will advance only for the
final qualification source. Development diagnostics are not release acceptance.

## Implementation and acceptance matrix

1. Home create controls now overlay the board, transparent container; bottom
   padding keeps final cards reachable. Needs four-profile visual acceptance.
2. Reorder edge-scroll is frame-paced/bounded and drops stale-target suppression
   during scrolling. Existing native drag retained. Long-distance bottom-to-top
   journey and animations-enabled qualification still required.
3. Library Settings last/Folders penultimate, including Checklist; glyphs now
   follow action names instead of obsolete indices. Needs native ordering check.
4–6. Image reader primary actions no longer horizontally scroll/clip; shortened
   labels retain full accessibility names; redundant Reset zoom removed (pinch
   remains). Small-font OCR, startup speed and broader varied-image accuracy
   investigation NOT complete. No model/recognition-output change yet.
7. Shared Notes/Doc SelectionScrollView defers unselected-area down events,
   directly scrolls/flings without collapsing selection; actual taps and long
   holds forward to native editor, native endpoints remain native. Added actual
   Android dispatch regression; attached real editor/handle extension still due.
8. Discovered PDF hard cap of1,000 pages independently of32MiB. Raised bounded
   visual preview caps to10,000 pages/128MiB; retained 2MP raster and bounded text
   scratch. Automatic Viewed indexing stops at64 pages and marks partial, while
   explicit Find still scans all pages. Added >18MiB/1097-page public fixture and
   last-page rendering test; original private PDF not available/tested.
9. Word page glyph geometry comes directly from the same shaped StaticLayout
   painted to PDF, with Unicode cluster boundaries, image-span exclusion and
   finite50k total/5k per-page geometry. Uses direct PdfSelectionImage rather than
   long-hold opening Page text. Explicit Page text remains fallback and source
   Unicode retained. Added source/geometry/direct-hold regression.
10. PDF highlight bands now include font ascender/descender and join adjacent
   glyphs on a line, rather than sparse separate ink rectangles. Outside-area
   page scrolling retains selection; existing hold/drag/floating Copy retained.
   Screenshot comparison against recreated public checklist/table still due.
11. Overflow Zoom scope chooses Each page versus Whole file; normalized zoom
   state propagates across visible/recycled pages without allocating larger
   rasters. Preference persisted and viewport restores scope. Native scope
   propagation/rotation regression still due.
12–13. Full page audit and broad latency pass NOT complete. Do not infer those
   from the targeted changes or structural Python tests.

## Diagnostic execution

New workflow notes-candidate67-interactions.yml compiles/lints on Actions only,
then runs ViewedFileJourneysTest, ImageSelectionTest and the existing actual
Home long-hold drag in light/dark × portrait/landscape. No distributable APK
publication in this workflow. It is a focused diagnostic, NOT full module/corpus
qualification. New three native tests are included in strict ordered inventory
(432/profile) for eventual full qualification. Python structural guardrails are
not substituted for Android execution. Native results currently pending.

Never upload the supplied private screenshots; CI fixtures are generated public
text/shapes. Stable package/signing and Calculator untouched. Root gate required
before every push. Final module-ordered E2E, varied OCR/performance evidence,
install-over proof and honestly recorded failures are required before sharing67.

## First executed diagnostic:41827950030, sourceaffcadbb

Actions compilation/JVM/lint/size gate passed. All20 viewer tests, including the
new >18MiB/1097-page last-page render, Word geometry/direct hold, selection scroll
and actual PDF long-hold/endpoint/floating Copy passed in every profile.
Native verification: dark portrait38/38, light portrait38/38, dark landscape37/38,
light landscape37/38. Both landscape failures are the unchanged image dialog
rotation visibility assertion: Keep selected image measured174px but only163px
visible. Light portrait job additionally exited255 after its native verification
passed; exact driver-stage reason is pending original job-log retention, not
labelled an app assertion failure or silently passed.

Repair: create primary image buttons with short visible labels and48dp minimum
BEFORE the first layout, rather than changing52dp to48dp inside a post-layout
rotation callback. Full accessibility labels and original strict visibility tests
remain unchanged. This avoids a transient old-height measurement during reflow.
Second diagnostic includes two real bottom-to-top36-card hold/edge-scroll cases
(Notes and Doc), actual Home overlay/Library-order assertions, Settings traversal
captures and normalized zoom-scope propagation. Drag targets use finger position,
not an assumed centered grab, retaining scroll-adjusted target hysteresis.
435 native methods/profile are now in the strict eventual full module inventory.

Test-only small-font study added:72 authored script/font/size/polarity inputs,
144 counterbalanced baseline versus4x-crop measurements planned, no production
OCR change. Added three SHA/Git-blob verified original public iOS screenshots,
five frozen inputs including two no-text nature-photo negative controls. These
are not positive street-sign photos or five independent source scenes. All
hypotheses/misses/inventions/timing must be retained before any adoption.

Original proof retention initially failed in41830346993: the earlier artifact
pattern refinement-native.* omits the verifier's refinement-native-verified.json
(hyphen rather than dot). Separate corrected retainer reconstructs exactly the
38 original expected methods fromaffcadbb and parses the original retained raw
logs; it does not substitute today's435-method inventory or repeat any device
case. Explicit PYTHONPATH covers Actions temporary Python script context. The
current diagnostic's independent build/native/study jobs can continue unaffected.

Reader41830910204 confirmed both original retention causes: missing verifier JSON
in job126575628645, and empty capture TAR in corrected job126576722854. It also
confirmed light portrait's255: emulator-5554 disappeared AFTER native38/38 and
before adb TAR capture. This is not a native assertion failure; screenshot proof
for that profile is unavailable, not fabricated/relabelled. Retainer now records
missing/empty/corrupt capture explicitly while retaining original raw native
outcomes. Four actual Python file-I/O tests cover empty/corrupt/named/traversal
archives. Device tests are not repeated for proof-retention repairs.

Reader41831443088 confirmed the third retainer was blocked by gh CLI's terminal
escape-sequence guard when reading the original job log. Added the same explicit
--allow-escape-sequences already used by the proven failure reader, with URL-query
redaction before public retention. No device/test repetition. Small-font study
in41830346993 completed its two test methods (149 measurements planned), but an
accidentally copied43-case interaction verifier then rejected those two study
identities. Raw study JSON is retained by the artifact step; inspect/recover the
actual data, do not relabel this workflow green or rerun inference merely to fix
that reporting gate. No OCR production change has been adopted.

## Expanded diagnostic41830346993 — executed, not full acceptance

Build/JVM/lint/size passed. Native:43/43 both portraits;42/43 both landscapes.
Both new36-card bottom-to-top Notes/Doc gestures, actual Home overlay/Settings-last
ordering/Library/Settings traversals, and new independent/whole-file zoom state
propagation passed in all four profiles. The SAME strict rotation assertion still
failed in both landscapes (174px vs163px): initial short labels/min-height alone
were insufficient. Do not call that earlier repair qualified.

Next repair moves responsive reflow from a post-layout callback into the root's
onMeasure BEFORE children are measured, eliminating stale child bounds rather
than relaxing the unchanged visibility assertion. The next pass also includes
attached real native Notes/Doc selection-ActionMode scrolling and foreground-first
PDF raster/deferred64-page history indexing. Inventory437/profile. No OCR model
or production recognition-output change yet; study results awaiting retention.
Original150/152 diagnostic outcomes now retained successfully by41832126874,
bot230dd997. Missing/empty capture profiles remain explicit; dark portrait has
three actual public UI captures. The original driver255 is confirmed disappeared
emulator after38 native passes, not an app assertion failure.

## Recovered study and portrait visual audit (2026-11-04)

- Recovery41833783470 SUCCESS, bot7f6e6a78. No native inference repeated. Existing149 measurements independently rescored from raw hypotheses with Java-matching NFC/whitespace and bounded codepoint Levenshtein. Both native study methods passed; the original job's43-method reporting gate was wrong.
- **Do not adopt global4x enlargement:**72 paired inputs; baseline66 exact, candidate63 exact;5 improved/7 worsened/60 tied; mean CER0.012668→0.021938; median inference211.5→273ms. Counterbalanced emulator timing, not physical total cold-open latency. Raw outputs retained for all pairs.
- Three positive public inputs: one exact, mean CER0.024176. Two nature-photo negative controls both hallucinated text. Non-Latin letters in2 outputs; supported Indic glyphs in3 outputs (one positive screenshot misreads a status-bar symbol as a Kannada digit). These are real retained accuracy failures, **not solved**. Empty-reference edit counts must not be presented as a normal accuracy percentage.
- Viewed both27-page portrait capture sets (light/dark) as labelled contact sheets; checked model/about, appearance, diagnostic viewer/filter/delete confirmation, Home Notes/Doc, Library, empty Archive/Trash/Folders/Voice inbox/Viewed files, Settings, speech-engine choice, storage/repair/copy choices and Viewed help/options. Does not cover landscape: old capture absent on failure. Empty states, background/dock colour, 48dp tools, destructive confirmations and cards coherent; storage copy-choice explanation is dense, not a redesigned visual proof of all populated features.
- Found a genuine discoverability issue in captures: Settings last in Library but hidden just below the initial portrait viewport. Library-only portrait cap now88% (measured content still shrinks smaller panels); landscape/default menus stay72%, Filters remain exactly half height. Native traversal now asserts the entire final Settings row visible before scrolling. **Not yet Android-qualified on this newer source.**
- Native selection-scroll cases additionally check an actual deliberate tap collapses the range without changing text. These additions await full-profile execution; endpoint-extension beyond viewport and physical acceptance remain distinct, unproven claims.

## Third strict diagnostic, still not qualification

41832709872/source5b248b69: build/lint/size PASS; native178/180 (45/45 each portrait,44/45 each landscape). Both new real ActionMode selection-scroll cases passed in all four profiles. The pre-measure reflow alone **did not fix** retained image actions: same174px/163px clipping. No assertion weakened and no final run/APK published.

Fourth repair makes the two short primary actions use explicit48dp layout height, single-line ellipsis, zero vertical text padding and both TextView/View minimums (not just setMinHeight). Rotation's exact-height assertion now reports the entire ancestor geometry and pending-layout flags if it fails again. This remains a candidate repair, not proven. Round4 also includes the portrait Library and normal-tap deselection changes. Original round3 logs/captures retained independently, no repeats to rewrite outcomes.

Round3 retention41835080716 SUCCESS/bot9a3beff7:34 public PNGs per profile, including both native selection-scroll captures and long-distance drags. Unlike earlier runs, the wrapper now preserved landscape captures despite the native assertion failure. Individually inspected landscape Settings/Folders and image selector: scrollable cards retain readable themed chrome and content; selector's primary actions visible in this saved frame, secondary tools scroll. These are not a capture of the exact failing rotation frame. The next strict assertion includes pending-layout/ancestor coordinates to disambiguate it.

Read-image capability help now explicitly warns that photo textures/status icons may be mistaken for letters even when no writing exists, based on actual recovered negative-control outputs. This disclosure is not a model/accuracy fix. Prepared rich migration runner accepts explicit increasing old/candidate codes for66→67; its local mock contract is not executed Android migration proof.

## Round4 result and valid tap-target correction

41835080389/source185e3d93: build/JVM/lint/size PASS, native176/180 (44/45 every profile). Image rotation strict visibility now PASSES all four profiles with explicit48dp action rail. Portrait Library's full final Settings row also passes. All failures are the expanded Doc selection case's *new deliberate-tap stage*: portrait does not collapse; landscape replaces selected bravo with k. Initial selection-scroll invariants passed before this stage. Do not call that proof of app data loss: the added tap reused pre-IME drag coordinates after native long-press could open/resize the keyboard. The one-character replacement is consistent with tapping the soft keyboard, not a text-area tap. Round5 recomputes a current field∩ScrollView∩visible-window target immediately before injection; the same deselection and byte-for-byte unchanged-text assertions remain strict. This target correction awaits execution.

Additional two-source public scene assets/harness are frozen but not yet executed: an illustrated English poster and real oblique French direction signs. French is explicitly unsupported, a capability-limit challenge only. No private content/model addition/training. Their planned eight observations remain separate from the original4752 corpus and144 paired-font observations.

## Round5, a different stage fails in one profile

41837142840/source2c9a3d36: build PASS;179/180 native.45/45 in both portraits and dark landscape. Light landscape fails **before the tap**, at the required viewport-scroll delta. The remapped deliberate tap and unchanged-content assertion passed in the other three profiles; do not conflate the old k-key injection with this new failure. Image rotation and Library assertions remain green everywhere.

Inspected retained round4 landscape Doc selection screenshot: keyboard occupies the lower area; only a short writing viewport remains, with the floating ActionMode above it. The original24dp bottom inset plus30%-height destination can leave less than touch slop of actual travel in that viewport. Round6 waits for native IME/layout to settle (a real start screenshot), constrains drag geometry to field∩viewport∩visible window, and uses proportional10%/90% vertical positions. It still requires meaningful travel beyond touch slop, actual scroll>30px, unchanged range/text, and normal-tap deselection. No retry or weakened assertion.
