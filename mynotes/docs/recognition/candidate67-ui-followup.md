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

## Full candidate67 qualification plan, not a pass claim

Coordinated version67/0.67.0. The full workflow's four recognition-focus profiles now include every new reader/navigation/reorder/selection case as an early barrier, in addition to OCR/speech/motion/region cases. Thus a remaining real or harness failure blocks the expensive main corpus and ordered full-native lanes; an intermediate diagnostic cannot be mistaken for final acceptance.

Planned exact inventory:437 native methods/profile=1748 ordered outcomes; original4752 recognition observations; additional2 public sources×4 profiles=8 separate scene measurements (4760 across these two cohorts, not including historical149-study observations or native-test internal model calls). Additional scene methods verify real resource orientation/night/font scale; raw text and CER shown in the Read image chapter, not conflated with the main4752 or French support. All public eval assets stay instrumentation-only. APK growth limit/signing/package/minSdk unchanged. Corpus/scene completion is not a claim all accuracy targets pass.

Root/report parser smoke uses explicitly fabricated temporary unit inputs only, never device proof; generated PDF was discarded. Per-module public CPU/progress diagnostics now survive wrapper overwrite; no measured causal latency improvement is asserted. Exact signed66→67 install-over on API26/34 and source-pinned security/raw-APK receipt remain pending until the new candidate artifact exists. Do not share the next APK before final qualification.

## Current13-request acceptance ledger (not completion)

| Request | Implemented / observed | Still required / limitation |
|---|---|---|
| Floating Home creation | New note/Doc overlay the board, not a separate solid strip. Native hierarchy + both theme/orientation capture sets reviewed. | Full-source ordered regression pending. |
| Long reorder |36-card Notes and Doc native bottom-to-top holds reach first position; contents unchanged; all four profiles pass. | Standard profiles disable animations. This is not physical Google Keep-equivalent animation/feel approval. |
| Library order | Settings last, Folders next-last; final full Settings row visible in ordinary portrait after Library-only cap change. Four-profile checks pass. | Small landscape remains scrollable by design; full-source regression pending. |
| Read image presentation / start | Short primary actions, real48dp rails, no redundant Zoom tool, responsive layout; strict retained-rotation visibility passes all profiles. | Whole cold-open improvement has not been measured/proven. |
| Tiny OCR | Paired study retained144 measurements; unsafe global4x rejected (5 better/7 worse; slower median). | No new production OCR accuracy policy adopted; tiny-text/missing-symbol/false-script failures remain. |
| Varied images | UI/photo dialogs, selected-row crop, no-text beach/heron negatives scored with all hypotheses; two new poster/outdoor-sign originals frozen for four-profile measurement. | New8 observations pending. French signs are an unsupported-language limit, not French support. Both old nature negatives hallucinate text. |
| Long selections | Attached real ActionMode scroll preserves anchors/focus/content, both Notes and Doc. Normal-tap check passes three round5 profiles; one landscape drag geometry failure addressed separately. | Round6/all-source final checks pending. Native handle extension across viewport and physical IME variants not yet proven. |
|18MB PDF | Synthetic >18MiB1097-page document open + final raster passed four profiles;128MiB/10000-page bound and64-page background index budget, no arbitrary small18MB cap. | Does not prove the user's unavailable private SAF original or every huge/hostile PDF. First-raster index deferral lacks causal first-paint benchmark. |
| Direct Word selection | Prepared page geometry accepts direct native longpress and real floating Copy; contents/source unchanged in four-profile viewer cases. | Unsupported layouts still need page-text fallback; not perfect Word format fidelity certification. |
| PDF highlights | Line-coalesced native glyph boxes/direct Copy tested; avoids per-letter visual gaps. | Rotated writing/pages retain explicit page-text fallback; physical visual precision not certified. |
| Zoom scope | Overflow lets user choose per-page or whole-file state, with propagation and retained scope tests across profiles. | Full-source regression pending; not a removed option. |
| Rare pages | Four-profile captures now retained, including Settings/diagnostics/storage/repair/choice dialogs, empty library destinations and help. Portrait and selected landscape frames inspected. | Populated variants and complete module-ordered regression/PDF still pending; this is not an all-pages usability certification. |
| Latency | Foreground-first PDF text indexing, retained per-module public phase logs; rejected slower OCR candidate. | No broad measured speed-up or audio-accuracy improvement claim. Physical start/tap/recording latency unqualified. |

Active full-source qualification41839360685/source92f51576 and signed66→67 rich upgrade41839619874. The migration run inherited an old64→66 display title; workflow display name corrected without rerunning its current native work. Its pinned inputs/CLI codes are66→67, which must be checked in actual proof rather than inferred from its title. Root gate969 passed before this documentation-only change. No APK shared.

### Measured bounded PDF fixture and candidate byte budget

Recovered round5 PUBLIC CPU logs record the generated18,994,387-byte1097-page document constructor plus120×170 final-page raster: light portrait22ms, dark portrait23ms, light landscape26ms, dark landscape25ms. Raw per-profile logs and a scoped receipt are retained. These numbers **exclude fixture generation, SAF copying, full-resolution page raster, first visible UI, text indexing and physical latency**; cache may be warm. They demonstrate no small18MB hard cap in that tested path, not a measured cold-open speed-up.

Full-source candidate raw APK metadata:155,176,860 bytes, only3,548 bytes above66; remains1,037,088 bytes below the156,213,948-byte cap. Source/run/digest/expiry retained in `candidate67-artifact-receipt.json`. This is a metadata receipt, **not** actual byte/aapt/signer verification or final qualification. The running signed-update jobs must independently check raw bytes, manifest/noINTERNET/min26 and stable signer. No APK offered.

### Actual Doc scroll interaction found; fix before qualification

Round6's light portrait still timed out at the initial scroll delta, despite the settled, window-constrained gesture. Inspection found a production interaction: `SelectionScrollView.dispatchTouchEvent` intentionally consumes selected-range drags, so Doc's existing ScrollView/field onTouch listeners never run to cancel `caretRevealUntil`. Its active automatic-caret lease can therefore scroll the viewport back toward the original selection. Added an explicit user-gesture callback at the beginning of dispatch (DOWN/MOVE), before deferral/scrolling; Doc cancels caret reveal there. Existing native scroll test now also checks that callback is delivered. Strict attached scroll/range/tap/text checks remain. This is a source change requiring a new full run, not a waiver/retry into green.

Signed update41839619874 failed on both APIs during **create**, before any upgrade: the Java CI-only probe still contained hardcoded64/66 assertions although the Python runner had explicit66/67 codes. Both jobs independently passed raw APK byte/digest/manifest/signer verification. Native probe now checks the explicit old/candidate pair monotonically and the runner passes both values. Original failed create logs must be retained; no migration success claim. No app data-clearing/uninstall.

The direct cancellation API for the superseded full run returned403 resource-not-accessible-by-integration; no retry/authentication workaround. The replacement workflow retains the same branch concurrency group so Actions can cancel the old-source qualification itself. Source92f's raw APK remains unqualified and unshared.

Further inspection before source approval found two **unconditional** adaptViewport posts of `revealCaret`, independent of its short lease. A dispatch callback alone does not suppress those already queued layout corrections. Doc now remembers manual selected-range viewport intent and the reveal method honors it while that range remains active; a real selection/caret change clears it (so deliberate taps, Find and handle extension keep normal behavior). Editor re-render resets the transient flag. The attached native case explicitly requests relayout and then checks the viewport stays scrolled, as well as unchanged anchor/focus/content and normal tap collapse. This strengthens the assertion; no pre-scroll assignment or retry is used. Another exact-source full qualification is required; intermediate sources remain unapproved.

Original migration failure evidence is now retained verbatim at `mynotes/docs/migration/41839619874/` (retention41841867804 SUCCESS, botdd30103f). Both raw create traces say `expected:<64> but was:<66>`; both phases arrays are empty, so no rich-data upgrade was attempted. Source92f identity verification is no longer metadata-only; its receipt explicitly remains superseded/unapproved. Full92f was cancelled by the branch concurrency guard before corpus inference (retained report: native0, inference0; early focus4/4 succeeded but did not expose the intermittent callback path). Current strengthened full source is abdd7caa / run41842482778; exact-source rich update pins that run. Intermediate e391 is superseded and cannot authorize delivery.

### Notes relayout defect found by strengthened native assertion

Full41842482778: build passed; early focus light portrait and light landscape passed. Dark landscape80/81 failed **Notes** after-layout viewport retention (not content mutation or endpoint loss); dark portrait cancelled by fail-fast. Main native and benchmark lanes skipped, report generation is not acceptance. Failed job annotations retained at `interactions/41842482778/failed-focus-receipt.json`.

Notes' automatic caret reveal also ran unconditionally on relayout/IME changes. Apply the same bounded manual-range intent to Notes: capture deliberate gestures only for the focused selected body; suppress automatic reveal while that range is active; clear on real selection/caret changes and editor recreation. Title focus and ordinary caret typing remain unaffected. Preserve the failing after-relayout assertion unchanged. A fresh exact-source full run and migration proof are required before APK delivery. Prior exact-source migration41842774948 succeeded on API26/34 but does not qualify the next changed APK.

### f97 exact-source qualification exposed native caret ownership, not just app helper

41871176226 early light-landscape focus80/81 failed the unchanged **Doc after-relayout viewport** assertion; expensive full native/corpus lanes therefore remain blocked. Boolean reset-on-selection-callback guards are insufficient: Android can issue same-range callbacks and its ScrollView performs its own child-focus/rectangle/layout correction independently of the app helper. Move ownership into SelectionScrollView: record the exact focused field and anchor/focus when an actual deferred drag crosses touch slop, compare identity/endpoints rather than callback occurrence, preserve its viewport through native focus/rectangle/layout requests, and let genuine endpoint/caret changes release ownership. Both Notes and Doc caret helpers consult that shared state. Explicit Find releases it before navigating (including finding the same original range). Added direct native rectangle/layout and real endpoint-change checks to the existing dispatch test. Attached strict viewport/range/content/tap assertions are unchanged.

Do not run another full corpus until the smaller four-profile diagnostic passes. API34 migration41871240310 passed; API26 failed before producing any offline/native probe evidence (raw APK identity checks passed). Original CI log diagnosis41873345903 ran without device/test repeats; logs were truncated by per-annotation limits, so chunked retention is being used before any API26 retry. No migration/data-loss conclusion from missing execution.

API26 original diagnosis is now concrete (chunked no-device retention41874322477): after emulator boot, `adb root` returned1 with `unable to connect for root: closed`; offline provisioning failed before any APK installation/fixture seed. API34 executed all three migration phases successfully. Preserve original annotations at `updates/41871240310/original-failure-annotations.txt`. CI helper now captures stderr and accepts only that exact adbd restart transport message, still independently requiring actual UID0 via the existing bounded verifier before broadcasting airplane mode, three consecutive real offline observations and final UID2000. Real root denial or an unverified UID transition remains fatal. Added tests for all three cases. No app permission/security/data change, no fixture/test retry into green, and no claim API26 migration has passed until another exact-source execution does so.
