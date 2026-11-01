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
