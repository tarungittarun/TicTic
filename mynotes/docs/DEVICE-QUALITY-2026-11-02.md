# Device-quality follow-up — active work ledger

Started 2026-11-01 21:49 UTC (2026-11-02 local). Requested window: 360 minutes;
this ledger records actual work/results, never an invented elapsed-time/training claim.
Previous App-folder qualification (30 jobs) and speech/OCR research (40 jobs) are closed,
separate scopes. Current follow-up: public corpus audit run41354950288 SUCCESS (1 job). Qualification
round1 run41354950129 superseded while its build was running: an over-broad RTF-fixture
edit accidentally removed pre-existing methods, caught by inventory comparison before
native qualification. All methods restored and a pinned baseline-preservation gate added.
Manual cancellation returned insufficient integration permission; workflow concurrency
will supersede the old run. Count its final actual job outcomes, not planned slots.
Round2 expects 282 JVM and 1116 native outcomes; final results pending.

Priorities: startup/Undo failure and independent diagnostics, home layout, simpler storage,
automatic OCR, common-file previews, speech quality and useful local intelligence.

The supplied launcher screenshot shows a truncated history-mismatch notice, **not a crash
stack trace**. It does not establish the exception or prove lost current notes. Reproduce
Undo with a removed App folder and add diagnostic access independent of library loading.
Never clear user notes or silently reinitialize an unavailable selected folder.

## Research already inspected in this follow-up
- AI4Bharat/Svarah GitHub README: Indian-accented English benchmark, not a training split;
  audio distribution links to Hugging Face (not an allowed sandbox download host).
- AI4Bharat/NPTEL2020-Indian-English-Speech-Dataset: educational-domain data with transcript
  quality caveats, large external downloads, not a substitute for spontaneous phone speech.
- AI4Bharat/IndicVoices: spontaneous Indian-language corpus; data/model downloads are on
  external hosts. Do not report training until licensed data and trainable weights are
  actually available, split without speaker leakage, trained and independently evaluated.

No new Indian-accent training or model adoption has occurred. User authorization permits
training, but does not make unavailable data/models accessible or establish their licensing.

## Follow-up safety and review changes (round 3)
- Private-first persistence converts memory pressure to a reported incomplete save, rather
  than losing the writer thread. Synthetic backend-pressure coverage checks private content.
- Text import offers **View only**, with Find, without creating a note or touching the source.
- XLSX cached-value text preserves sparse column gaps and explicit row numbers; invalid,
  duplicate/out-of-order or out-of-range coordinates are rejected rather than shifted.
- Speech retains uncertain candidates in Voice inbox with an explicit **Review transcript**
  action, instead of automatically inserting low/unknown native word-confidence results.
  The threshold is a conservative review heuristic, **not a calibrated accuracy estimate**;
  confidently wrong recognitions remain possible. This does not replace Indian evaluation.
- Native PDF preview closes descriptors on allocation failure and skips stale queued pages.
- Inspection found a Java-escaped RTF fixture error (\r/Unicode preprocessing rather than
  literal RTF control words); corrected to literal backslashes before round 3.
- Current inventory: 282 JVM; 339 unique native methods (all 327 previous qualified methods
  preserved), exhaustive shards 175/164 and focused 117/107: **1126** expected native outcomes.
  These are planned counts, not executed/passing results. Round 2 built successfully and
  started all eight native jobs; its final outcomes must be tallied after supersession.
- Local pre-push gates: 667 root checks, 277 Notes checks passed; all Java parsed. Actual
  Java symbol compilation and Android execution remain Actions-only and pending for round 3.

## Final qualification candidate (round 4)
- Round 1: one runner-executed cancelled build; the cancelled native placeholder had no
  runner and is **not** counted as an executed job.
- Round 2 (41355053789): one successful build and eight runner-executed cancelled native
  jobs; no successful native qualification. Concurrency cancellation completed.
- Round 3 (41355595977): build SUCCESS (Java app/test compilation, 282 JVM tests, lint and
  release assembly). Native work is superseded by round 4, not reported as passing.
- Round 4 adds an actual failed-library/diagnostic-button test and scrollable standalone
  safe-start checks. Existing real-SAF TXT coverage now exercises View only and Find before
  creating an editable copy, proving no note is created merely by viewing.
- Ordinary storage tasks appear before repair; file descriptions include the ten formats.
  Home Undo adds an accessible last-note-action/not-storage explanation. Pending permission
  callbacks cannot invoke a missing controller while the host is in recovery mode.
- Inventory: **282 JVM**, **340 unique native**; exhaustive 175/165, focused 118/107;
  **1130 expected native outcomes** across API26/API36 and API34 large-font-dark/landscape.
  All 327 previously qualified native identities remain. Nine jobs planned, not yet a result.
- No new trained model, Indian WER measurement or physical-phone audio/crash acceptance.
  Guide: `DEVICE-QUALITY-028-GUIDE.md`. Requested 360-minute window has not elapsed.

## Preserved partial results and category-navigation correction
The workflow preserved round-2 partial native reports in evidence commit `490752e1`, even
though its jobs concluded cancelled. They show **135 passing and 6 failing test outcomes**,
plus eight incomplete-instrumentation completion errors. The six failures were two
StorageLayout target checks, three private-folder journeys and one settings-parent journey.
Do not turn cancelled jobs into passing qualification or hide these earlier test failures.

The new category disclosure was real: old journeys attempted to tap Folders/copy controls
without first opening their category, and the geometry test asserted inside the same UI
pass that expanded a previously unlaid-out panel. Round 5 explicitly opens the real
Organise notes / More options & repair controls and lets Android lay out the category
before checking it. The complete target-size/window assertions remain unchanged; tests
never force hidden controls visible. Also keep the repair disclosure button beside its
moved panel, rather than moving just the panel. Inventory remains 282 JVM / 1130 native.

Round-4 push initially encountered an automated evidence commit. Fetched and merged it
on the session branch (no force push), reran the root gate, and pushed `5dcaee4c`, run
41356235141. Round 5 supersedes that source with the observed category-navigation fixes.

Round-3 preserved reports (`823bbcd8`) additionally exposed an API26 test-setup issue:
Android had already revoked the deleted folder's grant, so unconditionally releasing it
threw before the test could reach Undo. Release only grants actually still present;
keep the removal/Undo/private durability/reopen assertions. This is not a reproduced app
crash. Round-3 partial totals: 150 passing, seven failing test outcomes, eight incomplete
completion errors; final job conclusions were cancellation, not native success.

## Round 5 completed, not qualified; round 6 correction
Run41356383842: **1 successful build +8 failed native jobs**; all1130 native methods ran,
**1116 PASS /14 FAIL**, no skips/duplicates. Preserved evidence `049b803e`. This is not the
same as the round-2 planned1116 count and is not a passing candidate.

Failures: missing New-note tooltip (4 profiles), old OCR-language-dialog Cancel expectation
(4 profiles), old greater-than-half-screen Search width expectation (3 profiles), landscape
pinned-card visibility (2 journeys), landscape IME title clipping (1 journey).

Round 6 fixes the actual accessibility and landscape space defects: New Note/Doc has a
matching tooltip, redundant Notes/Doc brand chrome is hidden in landscape while workspace
tabs remain, and compact Home/IME padding is reduced without shrinking48dp controls.
The OCR journey now verifies `auto` directly and still checks blank gating, review Back
cancellation and explicit append against unchanged source bytes. The old width assertion
is replaced with full visibility and48dp targets for Search/Filters in the **remaining**
dock space after the microphone, matching the requested placement. All method identities
remain. New automatic-OCR, file, removed-folder Undo/private reopening, memory-pressure,
confidence-review, summary and failed-start/diagnostic cases passed in this failed round.

Added actual auto-flow screenshot collection (`read-image-auto`) alongside legacy whitelist
entries. Inspected the preserved portrait Home and landscape keyboard image; the latter
confirmed a genuinely clipped result title, not merely an incorrect expected string.
Round 6 expected inventory stays282JVM /1130native,9jobs. No training/physical acceptance.

## Round 6 completed: 1128 PASS /2 FAIL, no qualified APK yet
Run41358196950/source d5f323b8: build and six native jobs SUCCESS, two native jobs FAIL;
all1130 outcomes present, no skips/duplicates. Evidence d3298043. All14 round-5 failures
were resolved, including landscape pinned cards, readable IME results, accessibility and
automatic OCR review. Remaining failures:
- API36 existing Doc→Home→New Note journey: focused/attached body but keyboard hidden.
  A previous Doc keyboard-hide transition can race a single early show request. Round7
  uses a bounded initial readiness handoff (at most1s), respects focus/page changes and
  stops once IME visibility is observed; no persistent keyboard-forcing loop.
- Landscape removed-folder test: the real DocumentsProvider could not delete a directory
  while the synthetic App-folder mirror was adding children. Round7 waits for that mirror
  to finish, then deletes externally and retains all Undo/private reopen assertions. It
  does not catch/ignore failed deletion or turn an incomplete test into a pass.

Round7 inventory remains282JVM /1130native,9jobs planned. Local gates and Java grammar
must pass before pushing; no local Android builds, new trained weights or physical claims.
