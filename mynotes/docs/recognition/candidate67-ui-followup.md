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
