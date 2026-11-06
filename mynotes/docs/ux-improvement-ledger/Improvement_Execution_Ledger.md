# MyNotes — improvement execution ledger

**Status: ongoing, incomplete. No whole-app or physical-device acceptance. No app fixes implemented.**

This document records fresh bounded tests in both apps before a comparative improvement is promoted. Earlier screenshot/source audits remain hypotheses, not new tests. Each named finding has its own scope, evidence, confidence and proposed acceptance task.

## Improvement execution ledger

**Document contract:** Do not treat this document as a completed comparison of every control. Untested pages/features/aspects stay not tested. Knowing our source helps form a mechanism hypothesis; it does not prove the exclusive root cause or that a proposed fix works.

**Pinned apps:** MyNotes 0.69.0/code69, signed development APK SHA256 03d839d58b828ac72e211b69a1904e00e5f44f9f5c2b37704d4312501431d167; official Obsidian 1.15.0/code396 SHA256 2d92966fba5441a8976ae86b39cf138c0a845d3dcd318e6f0d29d304950bca5d.

**Method:** Fresh scripted interactions on one disposable Actions emulator, followed by visual evidence review and source tracing. This is not a human participant study. UI inspection and command/API setup shortcuts are identified separately. No E2E pass total is interpreted as UX quality.

**Known measurement limits:** Activity launch TotalTime is not interactive-ready time. Host command elapsed durations include inspection/adb overhead, not touch latency or human efficiency. Gfx/memory dumps are diagnostics, not a cross-engine smoothness rank. Animations, frame jank, battery and physical playback are not accepted without separate experiments.

**Scope still outstanding:** physical devices, participant usability, precise touch latency, animation quality, frame jank, battery, all unexecuted plan tasks

## Actual runs and provenance

- [42217004191](https://github.com/tarungittarun/TicTic/actions/runs/42217004191): provisioning_failed_before_apps — APK retrieval/verification step failed; emulator interactions did not run. The exact CLI failure is not inferred from its exit code. Raw-artifact-aware download used for the next attempt.

- [42217122419](https://github.com/tarungittarun/TicTic/actions/runs/42217122419): fresh_session_retained — Both signed apps installed; bounded interactions retained. W01 checks were unequal and Obsidian body had extra leading #. Wave-one S01 screenshots show actual matches in both. Obsidian Bold selector and MyNotes Appearance route blocked. Initial connectivity receipt shows Active default network: 100; offline isolation NOT established. Workflow success is not UX acceptance.

- [42217446693](https://github.com/tarungittarun/TicTic/actions/runs/42217446693): evidence_publication_gate_failed — Source session was complete, but the retention root gate failed; no app regression is inferred.

- [42218137421](https://github.com/tarungittarun/TicTic/actions/runs/42218137421): evidence_publication_gate_failed — Individual host-gate diagnostics were added; the initial annotation was truncated by GitHub. Assertions were not disabled.

- [42218403144](https://github.com/tarungittarun/TicTic/actions/runs/42218403144): evidence_retained_after_root_gate — Same retained original session saved after adding the report dependency set used by the local gate. No app or assertion change. This is a publication outcome, not an app UX pass.

- [42218900341](https://github.com/tarungittarun/TicTic/actions/runs/42218900341): fresh_session_retained_with_review_override — Repeat completed and originals retained. MyNotes W01/S01 visible outcomes and P01 entry observed. Obsidian W01 exact-body check blocked on empty readback; W02 shows **** in an otherwise empty body, not comparable formatted prose. Obsidian S01 raw pass is INVALIDATED by visual/DOM review: actual pane says 0 results / No matches found; title was in background editor. Initial default network none, not pre-install isolation proof.

- [42219043546](https://github.com/tarungittarun/TicTic/actions/runs/42219043546): evidence_retained_after_root_gate — Second-session original bytes and CDP command trace retained; publication success does not validate the raw false-positive search assertion.

**OBS-0001 / W01 / create a note containing the shared agenda:** Wave one created the agenda words in both apps, but Obsidian saved source had an extra leading #. MyNotes accessibility body and home title versus Obsidian editor/vault readback are unequal checks, not crash-recovery proof. Wave two did NOT correct the fixture: its exact Obsidian body assertion blocked on empty readback. No creation success or data-loss conclusion is drawn from that wave.

Evidence: [sessions/42217122419/mynotes-W01-typed.png](sessions/42217122419/mynotes-W01-typed.png), [sessions/42217122419/mynotes-W01-home-saved.png](sessions/42217122419/mynotes-W01-home-saved.png), [sessions/42217122419/obsidian-W01-typed.png](sessions/42217122419/obsidian-W01-typed.png), [sessions/42217122419/session.json](sessions/42217122419/session.json)

**OBS-0002 / S01 / find the note by body-only marker cobalt42:** Both apps found UX Meeting. MyNotes showed title/type with no body passage; Obsidian showed a passage with highlighted cobalt42. Global search pane setup used the built-in Obsidian command API, so touch-route efficiency is not compared. The extra leading # does not change this body-only query criterion. This is wave-one evidence only. The second-wave raw Obsidian search pass is invalidated: the actual pane has zero results after the body-entry blocker. It is not a successful independent reproduction.

Evidence: [sessions/42217122419/mynotes-S01-body-match-results.png](sessions/42217122419/mynotes-S01-body-match-results.png), [sessions/42217122419/obsidian-S01-body-match-results.png](sessions/42217122419/obsidian-S01-body-match-results.png), [sessions/42217122419/session.json](sessions/42217122419/session.json)

**OBS-0003 / W02 / inspect writing context while keyboard and formatting affordances are present:** Our Format panel opened and Bold selected state changed false to true. Our body node bounds shrank from 506px to 176px high. Obsidian keyboard and visible B toolbar were captured, but the wave-one selector could not activate B; no successful paired bold-formatting outcome is claimed. Viewport comparison is bounded to the captured states, not equal caret/default font or productivity.

Evidence: [sessions/42217122419/mynotes-W02-keyboard-before-format.png](sessions/42217122419/mynotes-W02-keyboard-before-format.png), [sessions/42217122419/mynotes-W02-format-keyboard.png](sessions/42217122419/mynotes-W02-format-keyboard.png), [sessions/42217122419/mynotes-W02-bold-state.png](sessions/42217122419/mynotes-W02-bold-state.png), [sessions/42217122419/obsidian-W02-keyboard-before-format.png](sessions/42217122419/obsidian-W02-keyboard-before-format.png), [sessions/42217122419/obsidian-W02-blocked.png](sessions/42217122419/obsidian-W02-blocked.png), [sessions/42217122419/session.json](sessions/42217122419/session.json)

**OBS-0004 / P01 / appearance entry:** MyNotes route was blocked because the harness sent two Back presses and landed on the launcher before looking for Library. This is a harness precondition error, not proof of missing Library. Obsidian Appearance was opened by settings API, not a measured touch route. Values, contrast and assistive use were not tested. Wave two reached MyNotes Settings and Appearance through the corrected one-Back touch route; Obsidian used settings API again. This establishes entry visibility only, not theme switching or equal route efficiency.

Evidence: [sessions/42217122419/mynotes-P01-blocked.png](sessions/42217122419/mynotes-P01-blocked.png), [sessions/42217122419/obsidian-P01-appearance.png](sessions/42217122419/obsidian-P01-appearance.png), [sessions/42217122419/session.json](sessions/42217122419/session.json), [sessions/42218900341/mynotes-P01-appearance.png](sessions/42218900341/mynotes-P01-appearance.png), [sessions/42218900341/obsidian-P01-appearance.png](sessions/42218900341/obsidian-P01-appearance.png)

## Evidence review and corrected claims

**REVIEW-NETWORK:** Wave one reports Active default network: 100 despite disable commands. Offline was NOT established. Wave two initial receipt reports none, but lacks consecutive pre-install checks. Neither proves absence of background traffic. Public dummy data only; no login, sync credentials or intentional upload actions. Do not infer no network transfer.

Evidence: [sessions/42217122419/environment.json](sessions/42217122419/environment.json), [sessions/42218900341/environment.json](sessions/42218900341/environment.json)

**REVIEW-WAVE2-S01:** INVALIDATED RAW PASS: Obsidian S01 recorded visible_outcome_verified, but native screenshot and DOM explicitly show 0 results / No matches found. The assertion searched the whole document for UX Meeting and accepted a background-editor title. Reviewed status: invalidated_harness_false_positive; no successful paired search in this wave. Raw retained files remain unchanged. W01 fixture readback was empty; creation/body entry is blocked, not evidence of app data loss.

Evidence: [sessions/42218900341/obsidian-S01-body-match-results.png](sessions/42218900341/obsidian-S01-body-match-results.png), [sessions/42218900341/obsidian-S01-body-match-results-dom.json](sessions/42218900341/obsidian-S01-body-match-results-dom.json), [sessions/42218900341/obsidian-W01-typed.png](sessions/42218900341/obsidian-W01-typed.png), [sessions/42218900341/session.json](sessions/42218900341/session.json)

**REVIEW-WAVE3-METHOD:** Next bounded repeat requires fresh-emulator offline provisioning before any APK installation, keeping production apps unmodified. Obsidian interactive W01 remains independently tested and may stay blocked. An explicit separate fixture setup uses the official vault/file API to seed/read back exact public body for downstream W02/S01 state comparison; it cannot count as note creation, typing efficiency, durability, or a W01 pass. Search must inspect visible result-passage and result-title containers rather than background text. This is a fixture/harness correction, NOT an app fix.

Evidence: 

## Measured quantities — narrowly defined

**MET-L01-MYNOTES-WAVE1:** Android am start -W TotalTime after am force-stop; persistent app data retained, three process-cold launches.

**App / samples / units:** mynotes | [2102, 2341, 2606] | milliseconds

**Interpretation limit:** First activity-launch milestone only; not time to interactive editor. App order not counterbalanced, restored content/start state differs, host/software GPU is not a phone. No general speed ranking, p95, significance or APK-size causality is inferred. Network conditions are not equivalently established: wave-one initial default network 100, wave-two initial none without pre-install isolation proof.

Source: [sessions/42217122419/session.json](sessions/42217122419/session.json)

**MET-L01-OBSIDIAN-WAVE1:** Android am start -W TotalTime after am force-stop; persistent app data retained, three process-cold launches.

**App / samples / units:** obsidian | [3310, 3559, 3894] | milliseconds

**Interpretation limit:** First activity-launch milestone only; not time to interactive editor. App order not counterbalanced, restored content/start state differs, host/software GPU is not a phone. No general speed ranking, p95, significance or APK-size causality is inferred. Network conditions are not equivalently established: wave-one initial default network 100, wave-two initial none without pre-install isolation proof.

Source: [sessions/42217122419/session.json](sessions/42217122419/session.json)

**MET-W02-MYNOTES-NODE-HEIGHT-WAVE1:** Height of the live body EditText bounds reported by UIAutomator before opening Format and after opening it, with the same keyboard state.

**App / samples / units:** mynotes | [506, 176] | physical pixels in UI tree bounds, before then after

**Interpretation limit:** 1080x1920 display, 480 dpi (3 px/dp), font scale1.0. This is clipped node geometry, not glyph-line count, touch latency, readability score or a comparable Obsidian node-height measurement. One activation sequence; not two independent trials.

Source: [sessions/42217122419/session.json](sessions/42217122419/session.json)

**MET-L01-MYNOTES-WAVE2:** Android am start -W TotalTime after am force-stop; persistent app data retained, three process-cold launches.

**App / samples / units:** mynotes | [3188, 4211, 2838] | milliseconds

**Interpretation limit:** First activity-launch milestone only; not time to interactive editor. App order not counterbalanced, restored content/start state differs, host/software GPU is not a phone. No general speed ranking, p95, significance or APK-size causality is inferred. Network conditions are not equivalently established: wave-one initial default network 100, wave-two initial none without pre-install isolation proof.

Source: [sessions/42218900341/session.json](sessions/42218900341/session.json)

**MET-L01-OBSIDIAN-WAVE2:** Android am start -W TotalTime after am force-stop; persistent app data retained, three process-cold launches.

**App / samples / units:** obsidian | [4136, 3212, 3426] | milliseconds

**Interpretation limit:** First activity-launch milestone only; not time to interactive editor. App order not counterbalanced, restored content/start state differs, host/software GPU is not a phone. No general speed ranking, p95, significance or APK-size causality is inferred. Network conditions are not equivalently established: wave-one initial default network 100, wave-two initial none without pre-install isolation proof.

Source: [sessions/42218900341/session.json](sessions/42218900341/session.json)

## Actionable improvements — evidence-gated

### IMP-0001 — Show why a body-search result matched

**Status:** proposed_not_implemented

**Owner / implementation not started:** Arena coding agent; implementation not started

**Acceptance on changed build:** not_run_on_changed_build

**Bounded task / expected result:** Find UX Meeting using cobalt42, a term present only in the body, and identify the relevant passage without opening candidate notes.

**Preconditions and exact actions:** Fresh API36 emulator, 1080x1920/density480, font1.0, same Gboard, animations1.0, offline guest. Create the recorded public agenda. MyNotes: return home, touch global search, type cobalt42. Obsidian: use global-search command only to open its pane, touch its query field, type cobalt42. Inspect result UI. Full action code and traces are retained; pane setup is not an equivalent navigation-efficiency experiment.

**MyNotes actual result:** One result is returned: UX Meeting followed by Note. No body passage or in-result highlighting of cobalt42 is shown in the captured result. The query field/keyboard containing cobalt42 are not a result snippet.

**Obsidian actual result:** One result is returned with an agenda passage and the cobalt42 occurrence visibly highlighted. Wave-one source has an additional leading #; this does not alter the body-only query criterion. No relevance ranking claim follows from one result.

**User consequence:** Without match context, users must open candidates to understand why they matched. Extra effort with multiple results is an expert inference, not measured user completion time in this one-note case.

**Reproduction and severity:** P1 expert triage for information retrieval; one independent initial paired session. No crash/data-loss claim, no population incidence estimate.

**Root-cause confidence:** source_supported_mechanism_not_exclusive_cause

**Source-supported mechanism:** NotesController.filterNow uses Note.matches to include body matches, but renderUniversalResults constructs a title button plus type/archive/folder metadata only. It does not create a matched-body passage or highlighted result range. This renderer design is consistent with the fresh visible result; no causal patch experiment has yet been run.

**Alternative explanations / limits:** The single-match corpus does not measure relevance, search speed, task failure, or user burden at scale. A future configured view could differ. Leading # and default typography differ between fixtures. The installed source/byte identity and raw results are retained; no blanket claim about every search surface is made.

**Proposed change — not implemented:** Build bounded match metadata on the existing search worker, preserving the current matching semantics: note identity, matched source (title/body/label/content name/diagram), and a short passage/range where available. Render a readable snippet/highlight for body hits and an honest source label for other hits. Reuse the query-generation guard; do not scan full bodies again on the UI thread. Keep grouped Saved notes & Docs, Viewed files and Settings and the 200-result disclosure.

**Protected requirements / risks:** Preserve archive/global-scope behavior until explicitly redesigned, plain/rich note identity, native accessible labels, Unicode correctness and the user-required floating New note. Do not introduce stale snippets, reveal private content in logs, or silently change search ranking/scope. Position mapping across case-normalised Unicode needs explicit tests.

**Acceptance task:** Retest the same body-only marker on both apps; add same/similar titles, two body occurrences, rich tables/checklists, label-only hits and multilingual/emoji boundaries. Each returned body hit must show an accurate bounded passage and visible accessible match treatment. No false source label, stale-query result or data mutation. Measure correction/navigation effort separately before claiming an efficiency gain.

**Next discriminating test:** Require exact downstream fixture independent of W01 input test, and visible result-container assertions; retain prior failed runs. Then repeat with two similarly named notes matching different passages, inspect differentiation and opening. Implementation only later after approval; retest on changed signed build.

**Source reference:** mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:398 filterNow | sha256=4fb7c955f8f2bc9477e3d7f0415253652fb1509e48fb7816f68321241fdfb3a9

[View immutable source](https://github.com/tarungittarun/TicTic/blob/a014e015731103a041b092a0e3c2802ac2805d3a/mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java#L398)

**Source reference:** mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:424 renderUniversalResults | sha256=4fb7c955f8f2bc9477e3d7f0415253652fb1509e48fb7816f68321241fdfb3a9

[View immutable source](https://github.com/tarungittarun/TicTic/blob/a014e015731103a041b092a0e3c2802ac2805d3a/mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java#L424)

Evidence: [sessions/42217122419/mynotes-S01-body-match-results.png](sessions/42217122419/mynotes-S01-body-match-results.png), [sessions/42217122419/obsidian-S01-body-match-results.png](sessions/42217122419/obsidian-S01-body-match-results.png), [sessions/42217122419/mynotes-S01-body-match-results.xml](sessions/42217122419/mynotes-S01-body-match-results.xml), [sessions/42217122419/obsidian-S01-body-match-results-dom.json](sessions/42217122419/obsidian-S01-body-match-results-dom.json), [sessions/42217122419/session.json](sessions/42217122419/session.json)

### IMP-0002 — Reserve writing context when the Format panel opens with the keyboard

**Status:** proposed_not_implemented

**Owner / implementation not started:** Arena coding agent; implementation not started

**Acceptance on changed build:** not_run_on_changed_build

**Bounded task / expected result:** Keep nearby note prose visible while choosing inline formatting with the keyboard open. This item concerns viewport allocation, not whether bold formatting saves correctly.

**Preconditions and exact actions:** Use the same recorded agenda on the same portrait emulator. MyNotes: reopen UX Meeting, focus its body, capture keyboard state, open Format, capture again, activate Bold and inspect selected state. Obsidian: focus the recorded editor and inspect its keyboard/B toolbar state; its first-run Bold selector failed, so activation correctness is not compared.

**MyNotes actual result:** Fresh screenshots show only a short strip of prose above the Format panel. Body node bounds change from [60,277][1020,783] (506px high) to [60,264][1020,440] (176px); panel bounds are [60,476][1020,999]. Bold control selected state changes false to true. These are geometry/state observations, not a failed formatting assertion.

**Obsidian actual result:** The captured keyboard/editor state leaves a larger visible prose region above a compact B/other-action toolbar. Its content reaches the top system-bar area, so it is not treated as a flawless reading layout. Default font, caret and scroll position are not matched; no productivity ratio is inferred. Actual Bold activation remained blocked by the first-run harness.

**User consequence:** Losing surrounding prose can require closing/reopening controls to retain sentence context. That consequence is an expert inference; no novice task-time or satisfaction study was performed.

**Reproduction and severity:** P1 expert triage for a core writing state; one independent initial paired-state inspection and one MyNotes before/after activation. Not an established incidence rate or font-size-independent defect.

**Root-cause confidence:** source_supported_mechanism_not_exclusive_cause

**Source-supported mechanism:** In portrait toggleFormatting allocates a height clamped between112dp and360dp based on55% of shell height. buildFormatting supplies its own header/options layout; adaptEditorViewport can hide/compact other chrome but does not express an explicit readable-body budget in that allocation. This mechanism is consistent with the observed shrinkage. Which allocation/header contribution dominates has not been isolated by a patch experiment.

**Alternative explanations / limits:** Caret/scroll, IME height, window insets, font and title visibility affect context. UI tree bounds are not rendered glyph counts. The larger Obsidian visible region does not prove its whole formatting workflow is better; the first activation attempt was blocked. Landscape rails and large text were not tested in this pair.

**Proposed change — not implemented:** Prototype an IME/inset-aware portrait layout that reserves a defined readable body area before allocating panel height. Keep the common five inline controls compact and advanced controls reachable through scroll/progressive disclosure; use an explicit large-text/very-short-viewport fallback rather than silently shrinking targets. Treat the three-readable-lines idea as a proposed normal-font target to validate, not an existing measurement or universal promise.

**Protected requirements / risks:** Keep single A Format, fifth inline Clear, selected-text/new-typing semantics, accessible selected state, 48dp targets, Undo/Redo and safe Back. Do not remove fonts/size/paragraphs, steal typing focus, reopen a dismissed keyboard or undo existing landscape rails. Preserve the actual selected text and caret.

**Acceptance task:** Repeat the same before/after task with fixed caret and equalised note content; record viewport and actual task success. Exercise selected text/new typing, keyboard show/hide, portrait/landscape and font1.0/1.5. Verify reachability, focus, correct formatting, persistence and unchanged note content. Measure interaction time/jank separately before claiming a smoothness improvement.

**Next discriminating test:** Wave-two SVG Bold target worked but body fixture was empty, so it cannot establish paired prose/formatting acceptance. Repeat on a known exact downstream fixture, disclose API setup, then test selection/new typing and persisted formatting. Isolate our panel budget/header contributions only in a later approved prototype.

**Source reference:** mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:554 buildFormatting | sha256=4fb7c955f8f2bc9477e3d7f0415253652fb1509e48fb7816f68321241fdfb3a9

[View immutable source](https://github.com/tarungittarun/TicTic/blob/a014e015731103a041b092a0e3c2802ac2805d3a/mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java#L554)

**Source reference:** mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:600 toggleFormatting | sha256=4fb7c955f8f2bc9477e3d7f0415253652fb1509e48fb7816f68321241fdfb3a9

[View immutable source](https://github.com/tarungittarun/TicTic/blob/a014e015731103a041b092a0e3c2802ac2805d3a/mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java#L600)

**Source reference:** mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java:587 adaptEditorViewport | sha256=4fb7c955f8f2bc9477e3d7f0415253652fb1509e48fb7816f68321241fdfb3a9

[View immutable source](https://github.com/tarungittarun/TicTic/blob/a014e015731103a041b092a0e3c2802ac2805d3a/mynotes/runtime/src/main/java/app/space/notes/runtime/NotesController.java#L587)

Evidence: [sessions/42217122419/mynotes-W02-format-keyboard.png](sessions/42217122419/mynotes-W02-format-keyboard.png), [sessions/42217122419/obsidian-W02-keyboard-before-format.png](sessions/42217122419/obsidian-W02-keyboard-before-format.png), [sessions/42217122419/mynotes-W02-keyboard-before-format.png](sessions/42217122419/mynotes-W02-keyboard-before-format.png), [sessions/42217122419/mynotes-W02-bold-state.xml](sessions/42217122419/mynotes-W02-bold-state.xml), [sessions/42217122419/obsidian-W02-blocked.png](sessions/42217122419/obsidian-W02-blocked.png), [sessions/42217122419/session.json](sessions/42217122419/session.json)

## Pending module task inventory — not findings

**Coverage rule:** A bounded test on one page does not mark its whole module or all its aspects complete. 37 module plans and 231 literal source control definitions are starting inventories, not 268 completed tests or an exhaustive inventory of generated/OS states.

**Per-aspect scope:** Pages/navigation, feature outcomes, icon discovery, visual hierarchy, layout/keyboard, animation, efficiency, smoothness, timing, accessibility and save/recovery are tracked separately. A screenshot does not pass any performance/gesture/assistive task.

**PLAN-X01 / Find, organise, label and reorder:** partially_probed_not_module_accepted

**Task still required:** Find an archived body-only match while a folder filter is active; reorder a distant card, then return.

**Acceptance:** Record actual scope, result passage, selected filters and persisted order; repeat list/grid, keyboard and large text. Measure drag continuity only in a new live session.

**Aspect statuses:** page navigation: not_tested; feature outcome: partially_observed: single body-only match in both apps; other search/organisation tasks pending; icon discovery: not_tested; visual hierarchy: partially_reviewed: S01 result-passage presentation only; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X02 / Writing, reading and formatting:** partially_probed_not_module_accepted

**Task still required:** Format the middle of a paragraph, type new text, undo, dismiss the keyboard and reopen the note.

**Acceptance:** Retain surrounding prose, correct style target, caret and saved result. Repeat with large text; compare actual task completion and mistakes, not assertion counts.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: partially_observed: W02 portrait viewport; paired bold activation blocked in wave one; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X03 / Checklists, numbering and list editing:** not_tested_in_fresh_paired_study

**Task still required:** Insert an item in the middle, indent it, move it past the viewport, hide completed rows and undo.

**Acceptance:** No lost items, wrong-list operations or accidental workspace swipes. Verify numbering after insertion and Backspace exit at an empty item.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X04 / Tables and direct cell editing:** not_tested_in_fresh_paired_study

**Task still required:** Edit a long cell in a wide table; insert a column beside it, toggle header role, delete a row and undo.

**Acceptance:** Keep the active cell visible above the IME; no unexpected horizontal jump; restore content and header meaning after undo. Compare the same table, not these unequal fixtures.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X05 / Images, attachments and inline media:** not_tested_in_fresh_paired_study

**Task still required:** Insert a photo, select two recognized words, keep the image, edit its crop, then remove only the intended attachment.

**Acceptance:** Keep source bytes, unrelated blocks and names intact; make copy versus replacement clear. Verify offscreen word selection and explicit outside-tap dismissal.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X06 / Web hyperlinks and external opening:** not_tested_in_fresh_paired_study

**Task still required:** Add a long URL to selected words, correct a malformed address, reopen, edit and remove the link.

**Acceptance:** No duplicate URL per line, changed neighboring prose or unnoticed external launch. Check keyboard visibility, wrapped fields and TalkBack error announcement.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X07 / Internal links, backlinks, graph and previews:** not_tested_in_fresh_paired_study

**Task still required:** Link A to a heading in B, rename B, return through backlinks and preview the target without losing A.

**Acceptance:** Target identity and rename behavior must be verified live; count wrong destinations and recovery steps. No graph speed or link-integrity winner has been measured.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X08 / Long-note orientation, tags, footnotes and word count:** not_tested_in_fresh_paired_study

**Task still required:** Locate a middle heading, find a repeated phrase, follow a footnote and return to the exact reading location.

**Acceptance:** Verify target and return anchors, heading levels, counts and generated text treatment; do not infer footnote success from a registered command.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X09 / Properties and Bases:** not_tested_in_fresh_paired_study

**Task still required:** For users who need records, edit one typed field, filter a view, rename a file and reopen the view.

**Acceptance:** Validate data types, retained filters, note identity and error recovery. No Bases performance, formulas or equivalent MyNotes capability is certified here.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X10 / Quick capture, daily notes, templates and unique notes:** not_tested_in_fresh_paired_study

**Task still required:** From a cold start, capture a short thought; then create a repeated meeting structure and return tomorrow.

**Acceptance:** Observe time to first saved words, setup mistakes, duplicate daily notes and return continuity. Do not populate fake user content to make the app feel busy.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X11 / Freehand drawing and Canvas:** not_tested_in_fresh_paired_study

**Task still required:** Draw a diagram annotation, leave and reopen it; in Canvas connect two cards, pan/zoom and reopen the board.

**Acceptance:** Evaluate each tool against its own output goal. For common save/return work, preserve content and location; latency needs real stroke traces on matched hardware.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X12 / Diagrams, shapes, arrows and grouping:** not_tested_in_fresh_paired_study

**Task still required:** Add two shapes, connect them, group, resize, split the arrow, undo and reopen.

**Acceptance:** Check actual anchor geometry, target selection, retained names and no accidental duplication. Run long-press/context-menu actions with large text and TalkBack.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X13 / PDF reading, selection, search and markup:** not_tested_in_fresh_paired_study

**Task still required:** Open a large scanned/mixed PDF, zoom a distant page, select offscreen text, find a hit with IME, mark a rotated page and export.

**Acceptance:** Compare the original bytes; inspect hit position, highlight geometry and rendered export. Exercise cancellation and a partial destination; private markup drafts are outside Notes backups.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X14 / Word files and original-layout expectations:** not_tested_in_fresh_paired_study

**Task still required:** Open a DOCX with headers, inline images, wide tables and hard page breaks; select text, zoom and create an editable copy.

**Acceptance:** Check original bytes and document return position, compare rendered pages with a suitable Word reference, and disclose any dropped layout/media. Do not invent fidelity from a synthetic one-page frame.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X15 / Large TXT and read-only text viewing:** not_tested_in_fresh_paired_study

**Task still required:** Open a large multilingual TXT, find a repeated phrase near the end, select across the viewport and return to the prior file list.

**Acceptance:** Measure open/find latency separately from rendering; inspect Unicode integrity, clear selection on deliberate tap, retained page and unchanged original.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X16 / Spreadsheet previews and cached values:** not_tested_in_fresh_paired_study

**Task still required:** Open quoted CSV and a multi-sheet XLSX with formulas, missing cached results and wide cells; locate a value and copy it.

**Acceptance:** Compare visible rows/columns and Unicode with source values, disclose cached-only results, retain sheet/scroll position and never execute macros or scripts.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X17 / Markdown, HTML, RTF, ODT and slide imports:** not_tested_in_fresh_paired_study

**Task still required:** Open one realistic file of each advertised type, inspect text order and unsupported elements, then cancel or create a copy.

**Acceptance:** Record per-format fidelity and actionable errors; preserve originals, reject malformed/oversized archives safely and distinguish external-reader delegation from in-app support.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X18 / OCR, image-region text and scanned-page recognition:** not_tested_in_fresh_paired_study

**Task still required:** Read tiny/oblique/multilingual public scenes, blank controls and selected image regions; compare exact expected text and corrected output.

**Acceptance:** Retain source hashes, full hypotheses, CER, omissions/false words, cold/warm latency and correction time. No new accuracy study or training occurred in this report.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X19 / Voice recording, playback and transcription:** not_tested_in_fresh_paired_study

**Task still required:** Record varied Indian speakers, pause/resume, deny/regrant permission, play through a headset and correct names/numbers while listening.

**Acceptance:** Measure word errors, boundary losses, time-to-review, seek accuracy and actual audible playback on physical hardware. Verify originals export separately from note backups.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X20 / Reminders and notifications:** not_tested_in_fresh_paired_study

**Task still required:** Set, rename and reschedule a reminder, deny notifications, reboot and then tap the delivered notification.

**Acceptance:** Use disposable notes; verify no stale/duplicate notification, correct note identity and sensible blocked-state feedback. Measure actual delivery under power restrictions separately.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X21 / Home widget and Android entry points:** not_tested_in_fresh_paired_study

**Task still required:** Place/resize the widget, pin and archive notes, share text from another app and open a reminder deep link.

**Acceptance:** Verify labels, intended note, no duplicates and return context on real launchers, including large system text. Do not restore widget/notification consent silently during recovery.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X22 / Appearance, backgrounds, fonts and accessibility:** partially_probed_not_module_accepted

**Task still required:** Switch system theme, increase font scale, navigate with TalkBack and format selected text over a patterned background.

**Acceptance:** Check clipping, announced scope/selection, contrast and focus order across orientations; screenshots alone do not establish 48dp targets or a successful screen-reader task.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X23 / First-use storage, private folders and optional App folder:** not_tested_in_fresh_paired_study

**Task still required:** Create notes offline, choose an empty optional folder, revoke access, save again and reconnect without clearing app data.

**Acceptance:** Inspect exact copied categories and pending status; test reinstall/reselection only on disposable fixtures. Never tell the user to uninstall or clear their real library for this review.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X24 / Portable backup, import merge and full recovery:** not_tested_in_fresh_paired_study

**Task still required:** Export a near-limit rich library, exceed the limit safely, import identical and same-ID changed notes, then verify every supported content type.

**Acceptance:** No silent partial success or overwrite; disclose omitted categories and readable/non-encrypted JSON. A complete App-folder checkpoint is a separate opt-in recovery mechanism, not this portable file.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X25 / Trash, permanent deletion and recovery:** not_tested_in_fresh_paired_study

**Task still required:** Delete/restore a rich note, inspect what happens to its audio and exports, then purge only a disposable selected note.

**Acceptance:** Verify the exact affected IDs, Undo boundaries, expiry policy and retained separate data. Never infer crash or power-loss recovery from a confirmation screenshot.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X26 / Archive, pinning and bookmarks:** not_tested_in_fresh_paired_study

**Task still required:** Pin a note, archive it, find it globally, restore it and compare a saved Obsidian bookmark task.

**Acceptance:** Check persisted identity/order, scope labels and no accidental duplicate. Bookmark persistence must be newly verified rather than inferred from its empty pane.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X27 / Undo, redo, editing history and save feedback:** not_tested_in_fresh_paired_study

**Task still required:** Edit text/table/image, undo and redo, leave and reopen; repeat after controlled process restart on disposable data.

**Acceptance:** Verify exact rich payload and history matching the saved revision; preserve current content when history is invalid or trimmed. Never run destructive migration on the user library.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X28 / Workspaces, tabs and mobile navigation:** not_tested_in_fresh_paired_study

**Task still required:** Switch between two notes, a PDF and Settings; reorder content near an edge, then return to the initial editing location.

**Acceptance:** Log wrong destinations, lost context, accidental switches and state after restart. Workspace restoration is outstanding until a matching live task succeeds.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X29 / Settings, commands, quick actions and help discovery:** not_tested_in_fresh_paired_study

**Task still required:** Without a walkthrough, find move, undo, export, a setting and one uncommon editor action using touch alone.

**Acceptance:** Observe search terms, wrong taps and help requests; distinguish disabled/contextual/unconfigured actions. Do not use all 205 inventoried commands as a usability success count.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X30 / Export, sharing, duplication and note composition:** not_tested_in_fresh_paired_study

**Task still required:** Share a mixed note, export PDF and JSON, duplicate it, merge selected notes and undo the merge.

**Acceptance:** Check text/media/table fidelity for each promised format, exact source IDs and retained source copies; destination failure must not silently destroy content.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X31 / Local summary and text assistance:** not_tested_in_fresh_paired_study

**Task still required:** Summarize varied notes with negation, dates and multilingual sentences; check the retained source and decide whether the result is useful.

**Acceptance:** Verify each sentence comes from the source, essential cautions are not lost and limits are clear. No summary-quality test was executed in this report.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X32 / Privacy, permission boundaries and diagnostic logs:** not_tested_in_fresh_paired_study

**Task still required:** Deny microphone, use notes normally, enable logs for a dummy issue, filter/export/delete and inspect the resulting local file.

**Acceptance:** Verify opt-out, readable levels/time range, explicit delete confirmation and exported content minimisation. Security of all native dependencies was not certified here.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X33 / Smoothness, speed, footprint and reliability:** partially_probed_not_module_accepted

**Task still required:** On matched hardware, cold start, create/type, search a large library, drag a distant card, open a large PDF and recover from a controlled failure.

**Acceptance:** Collect touch-to-visible timings, frame trace, dropped frames, memory and exact failure logs. Distinguish timeouts/socket failures from app crashes and do not publish a fabricated global score.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: Android activity-start milestone sampled only; interactive-ready time not measured; accessibility: not_tested; save and recovery: not_tested

**PLAN-X34 / Core modules, community plugins and extensibility:** not_tested_in_fresh_paired_study

**Task still required:** Find a core module, understand its description, enable/disable it and identify changed controls; inspect restricted/plugin trust text without installing unknown code.

**Acceptance:** Verify reversible configuration and a stable default note workflow. No community code was installed or executed; unknown plugins and future modules are outside this pinned-version inventory.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X35 / Sync, Publish, account and service boundaries:** not_tested_in_fresh_paired_study

**Task still required:** For a future explicitly authorised service study, evaluate offline edits, conflicts, account loss, export and deliberate public visibility with disposable data.

**Acceptance:** No login, purchase, upload or publishing was performed. Conflict resolution, encryption, recovery and cost/value remain untested; an entry screen is not a passed sync task.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X36 / Slides, presentation and reading projections:** not_tested_in_fresh_paired_study

**Task still required:** Present a multi-slide disposable note, navigate, dismiss the keyboard, rotate and return to editing; compare PDF reading only as a separate output route.

**Acceptance:** Verify every slide, media, controls and return location. Obsidian landscape was not qualified by its retained audit; no physical presentation run occurred here.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

**PLAN-X37 / About, models, licences and honest help:** not_tested_in_fresh_paired_study

**Task still required:** Ask a novice to identify where notes are saved, how to protect audio and how to export useful diagnostics without sending private notes.

**Acceptance:** Observe whether they can explain the result in their own words. Do not substitute long notices or a generated guide for successful comprehension.

**Aspect statuses:** page navigation: not_tested; feature outcome: not_tested; icon discovery: not_tested; visual hierarchy: not_tested; layout and keyboard: not_tested; animation: not_tested; efficiency: not_tested; smoothness: not_tested; timing: not_tested; accessibility: not_tested; save and recovery: not_tested

## How this ledger must drive later fixes

**Stable IDs:** Do not renumber or delete items to make the record look clean. Add dated evidence, affected builds and changed confidence. Preserve blockers and failed attempts.

**Before implementation:** Confirm actual reproduction and source mechanism, isolate alternatives, define a minimal change and protect earlier requested behavior. Do not copy Obsidian architecture by default.

**After implementation:** Record the implementation commit, run the same task on the new signed app, retain before/after evidence, verify persistence and regressions, then change the item status. Proposed is not fixed.

**Source inventory:** source-surfaces.json inventories literal first-party definitions only. Dynamic names, generated controls, OS/provider dialogs and states not visited are not exhaustively enumerated.

**No unsupported certainty:** Never report general speed, smoothness, reliability, accessibility, user satisfaction or exclusive causality from the current bounded emulator evidence. Unknowns require named next tests, not confident filler.

## Supporting records
- [Machine-readable ledger](ledger.json)
- [Literal source-surface inventory](source-surfaces.json)
- [Status and confidence definitions](README.md)
