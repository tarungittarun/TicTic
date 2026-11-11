# MyNotes71 vs official Obsidian1.15: comparison execution evidence

**Comparison phase remains open. Production fixes are on hold.** A page visit,
74 attempt records, native inventory totals or a green observer process must not
be described as completion of the original37 functional/UX acceptance tasks.

## Targeted emulator trace: prior guest loss not reproduced

[Reviewed diagnostic PDF:42 pages,43 clickable links](https://github.com/tarungittarun/TicTic/raw/c7744955d/mynotes/docs/comparisons/comparison37/43083262273/Comparison37_Runtime_Probes.pdf) · [Actions run43083262273](https://github.com/tarungittarun/TicTic/actions/runs/43083262273).

This was a **single fresh dark-landscape guest**, restricted to areas31–37 after the failed full-grid run. It produced14/14 attempt records and14/14 complete original captures, with0 capture errors and14 target-app captures. Statuses:7 interaction observations,3 counterpart-not-established,2 blocked and2 diagnostic-only; **whole requested tasks accepted0**. It cannot fill or replace any row in the separate full-grid cohort above.

The bounded symbolic-stack tracer attached to QEMU PID4191. At harness completion the tracer receipt reported the leader still running; the host-after snapshot confirms qemu-system-x86 was alive. The retained244-line trace contains ordinary worker-thread exits only—no terminating signal or exit_group. No minidump or kernel OOM/killed-QEMU line was retained. Therefore this is a **non-reproduction, not an emulator fix or long-session reliability proof**. Trace instrumentation and single-job load may affect timing.

Manual review (CMP37-H23): X33 shows MyNotes’ notes-list home versus Obsidian with the fixture note open; X34 shows MyNotes Appearance/Speech-engine settings versus Obsidian Core plugins. These are real screenshots but unmatched tasks/states, not a visual or feature winner.

- [Coverage summary](https://github.com/tarungittarun/TicTic/blob/f2d00d017612aa18c012ab1fd516693ae29af209/mynotes/docs/comparisons/comparison37/43083262273/summary.json)
- [Original trace receipt](https://github.com/tarungittarun/TicTic/blob/f2d00d017612aa18c012ab1fd516693ae29af209/mynotes/docs/comparisons/comparison37/43083262273/comparison37-dark-landscape-guest-loss-diagnostic/comparison37-evidence/emulator-exit-trace-receipt.json)
- [Original bounded trace](https://github.com/tarungittarun/TicTic/blob/f2d00d017612aa18c012ab1fd516693ae29af209/mynotes/docs/comparisons/comparison37/43083262273/comparison37-dark-landscape-guest-loss-diagnostic/comparison37-evidence/emulator-exit-trace.txt)

A separate fresh **20-job parallel full grid, run43086479017, is currently in progress**. It is a new cohort; pending jobs are not evidence and will not be pooled with either result above.

## Latest audited cohort: comparison still incomplete

[Download latest reviewed PDF: 150 pages, 584 clickable links](https://github.com/tarungittarun/TicTic/raw/2458cad4e07031f544ac8beff730266d9396a208/mynotes/docs/comparisons/comparison37/42617793054/Comparison37_Runtime_Probes.pdf).
[Original latest run42617793054](https://github.com/tarungittarun/TicTic/actions/runs/42617793054).

| Profile | Attempt records (planned74) | Complete original snapshot records | Capture errors | Target app present in capture |
| --- | ---: | ---: | ---: | ---: |
| Light portrait | 74 | 74 | 0 | 67 |
| Dark portrait | 74 | 74 | 0 | 69 |
| Light landscape | 74 | 74 | 0 | 68 |
| Dark landscape | 66 | 65 | 1 | 58 |

This cohort contains **288 attempts and287 complete original captures**, not296 clean comparisons. Each profile spans5 fresh guests. Snapshot completeness is not functional acceptance: **original requested tasks accepted remains0**. External/system surfaces and blocked routes remain distinguishable from app-owned UI; no feature-absence or comparative score is inferred.

- **Host failure mode established in one traced reproducer:** run42608740772 attached to QEMU PID3296, observed SIGSEGV/SEGV_MAPERR and process death before tracer cleanup. It does not establish an Android app fault or the underlying emulator code defect. Code-only recheck42610424422 returned only an unnamed heap frame; no usable component attribution. No raw memory or environment collected/uploaded by these diagnostic tools.
- **Requested guest rendering was not applied:** run42611730935 explicitly fell back to host lavapipe/swangle because this image does not support in-guest rendering. It was not a successful bypass of the host graphics path.
- **Host Mesa/Xvfb improved usable coverage, not proven stability:** run42612858441 completed14/14 captures for areas31–37 in light portrait, and logs verified actual Mesa llvmpipe. The first broad cohort42614548550 had296 attempts/295 complete captures and one observer failure with QEMU still alive. A CDP reconnect-on-error cleanup fix preserves the original sampling error;6 unit tests cover its failure paths.
- **Latest verification still loses a guest:** in42617793054 dark-landscape Obsidian X33, an adb force-stop command timed out and capture later reported emulator-5554 missing. QEMU was absent in the postfailure snapshot. Areas34–37 in that shard were not executed in either app. This untraced run supplies no termination signal or app fault attribution. Older successful frames are not used to fill these missing originals.
- **Search fairness is not uniformly resolved:** earlier cohort42614548550 supplied a live keyboard-open pair, but latest light-landscape Obsidian X01 has an empty query and no keyboard while MyNotes has cobalt42 and its keyboard open. Both original rows are blocked. Latest frames cannot establish a comparative search win; CMP37-H21 documents the exact mismatch.

[Separate earlier broader-coverage report](https://github.com/tarungittarun/TicTic/raw/a2d9431a953e774007f86df6205877364cc29a9d/mynotes/docs/comparisons/comparison37/42614548550/Comparison37_Runtime_Probes.pdf) —296 attempts/295 captures, never pooled into the latest cohort. All originals, failures and hashes remain in their run-specific directories. Reviewed observations CMP37-H14–H23 explain the diagnostic and visual limits.

The targeted diagnostic and its evidence publication have completed; the separate full parallel grid is pending, not accepted. No automatic retry is running. **1149 root QA tests passed**, with report/Office dependencies installed. No production app changes or new APK. Comparison-first ordering remains: app fixes are still parked. Remaining work includes reliable complete capture, matched user routes, full-workflow outcome assertions, actual OCR/audio/document fidelity, animation/efficiency/time measurements and separately disclosed physical/human/novice verification. This PDF is evidence and shortcomings, not acceptance of those tasks.

## Retained executions

| Run | What actually happened | Scope |
| --- | --- | --- |
| [42342628990](https://github.com/tarungittarun/TicTic/actions/runs/42342628990) | Portrait:74 records per theme, each3 bounded outcomes,16 interactions,1 diagnostic inspection,54 blockers. Landscape:0 module probes; fixture setup failed. | Partial runtime evidence only; many blockers are harness or guest loss, not app findings. |
| [42343158423](https://github.com/tarungittarun/TicTic/actions/runs/42343158423) | All four profiles:0 module probes; keyboard-observer guard aborts setup. Complete generated Office input bytes and fatal frames retained. | Failed provisioning, no comparison coverage inferred. |
| [42344599145](https://github.com/tarungittarun/TicTic/actions/runs/42344599145) | Completed failure. Portrait:44 records each; light landscape:0; dark landscape:74, including37 blocked Obsidian profile assertions. | Original guest-loss/permission/picker/profile failures retained; no whole-task acceptance. |
| [42345604329](https://github.com/tarungittarun/TicTic/actions/runs/42345604329) | Completed: both landscape jobs green, both portraits failed. Portrait:44 records each; landscape:74 each. All37 Obsidian rows in each landscape profile were blocked because the actual display was portrait. | Green jobs prove capture/attempt completion, not requested-profile or functional acceptance. |
| [42346486192](https://github.com/tarungittarun/TicTic/actions/runs/42346486192) | Completed:13 green/7 red short-session jobs. Actual records: light portrait60, dark portrait48, light landscape42, dark landscape74. | Five independent guests per profile, not a stable long session; no profile reached whole-task acceptance. |

## Earlier audited deliverable (before host-Mesa studies)

[Reviewed comparison PDF:150 pages,373 clickable links](https://github.com/tarungittarun/TicTic/raw/b4b747b0a1db89f298eec6cb3fb75dd64102f430/mynotes/docs/comparisons/comparison37/42408161677/Comparison37_Runtime_Probes.pdf). This report includes independently reviewed search frames, not just automated outcome counts. It is explicitly **not completion of the original37 full workflows**.

- Native light/dark landscape search frames show the query and header above the software keyboard, without a visible result card. Obsidian's corresponding retained frame has no keyboard, so no controlled comparative win is assigned. The later live helper rechecks are reviewed separately as CMP37-H19 and CMP37-H21; their outcomes must not be substituted into this older cohort.
- Singleton recovery correctly retains four rows and the real host snapshot from [42485530023](https://github.com/tarungittarun/TicTic/actions/runs/42485530023), rather than the previous empty observer report. The emulator process was absent after failure; the retained kernel excerpt supplies no OOM/segfault attribution. After-failure free memory does not establish conditions before failure.
- [42486608340](https://github.com/tarungittarun/TicTic/actions/runs/42486608340) actually used emulator37.2.12.0/build16428233, compared with37.2.13.0/build16740729. The pinned prior build also lost the guest after four records. This environment change did not establish a fix.
- [Pinned-build original report](https://github.com/tarungittarun/TicTic/raw/b4b747b0a1db89f298eec6cb3fb75dd64102f430/mynotes/docs/comparisons/comparison37/42486608340/Comparison37_Runtime_Probes.pdf).

At that earlier publication, launched jobs had completed and root QA passed1126 tests. The later studies and current QA total are reported above. Original full-workflow, physical/human/novice requirements remain open; no production fix or new APK was introduced.

## Later comparison-route and guest-loss studies

The full comparison is **still incomplete**; none of these small studies establishes whole-task acceptance.

| Run | Actual evidence | Interpretation |
| --- | --- | --- |
| [42408161677](https://github.com/tarungittarun/TicTic/actions/runs/42408161677) | Scroll-aware recheck:9 green/11 red jobs; retained records56/60/54/16 for light portrait/dark portrait/light landscape/dark landscape. | Several native landscape tools became reachable through actual scrolling. PDF/TXT directory selection and guest losses remain unresolved. |
| [42484167089](https://github.com/tarungittarun/TicTic/actions/runs/42484167089) | Extra CDP screenshot fence disabled; four final-module jobs all failed. Retained records4/4/0/0. | Removing that extra observer operation did not eliminate guest loss. Not a proven fix or cause. |
| [42484719290](https://github.com/tarungittarun/TicTic/actions/runs/42484719290) | Direct software-renderer recheck:four jobs failed;4 retained records in every profile. | Renderer change did not establish stability. The host-diagnostic heredoc also failed because the emulator action runs each line in its own shell; `import: not found` was a harness error, not an app error. No host resource snapshot was produced by that attempt. |

- [Scroll-aware recheck PDF](https://github.com/tarungittarun/TicTic/raw/1c3ffb916cad6d3c5b230b7961492dea9c4a7ec4/mynotes/docs/comparisons/comparison37/42408161677/Comparison37_Runtime_Probes.pdf)
- [Observer-isolation PDF](https://github.com/tarungittarun/TicTic/raw/1c3ffb916cad6d3c5b230b7961492dea9c4a7ec4/mynotes/docs/comparisons/comparison37/42484167089/Comparison37_Runtime_Probes.pdf)
- [Direct-renderer recheck PDF](https://github.com/tarungittarun/TicTic/raw/1c3ffb916cad6d3c5b230b7961492dea9c4a7ec4/mynotes/docs/comparisons/comparison37/42484719290/Comparison37_Runtime_Probes.pdf)

The diagnostic runner is now a single Python process with host collection in `finally`, preserving the comparison's original failure exit code. A separate DocumentsUI search fallback retains the unsuccessful directory-route frame and labels search as an independent route; it has unit coverage but is **not yet device-verified**. Original failed records and reports remain unchanged.

## Earlier recheck:42404795321

The notification isolation, provider settling and per-launch rotation changes were tested in20 jobs on the pinned installed apps. **10 jobs succeeded and10 failed; the comparison is not complete.** Retained records per profile are56 light portrait,56 dark portrait,44 light landscape and44 dark landscape. No whole requested task is marked accepted.

- [Latest recheck PDF](https://github.com/tarungittarun/TicTic/raw/d773e3e32200034b0d0c9483ac8e018369eb000a/mynotes/docs/comparisons/comparison37/42404795321/Comparison37_Runtime_Probes.pdf).
- [Actual counts and scope](https://github.com/tarungittarun/TicTic/blob/d773e3e32200034b0d0c9483ac8e018369eb000a/mynotes/docs/comparisons/comparison37/42404795321/summary.json).
- [Recovered original host-log manifest](https://github.com/tarungittarun/TicTic/blob/d773e3e32200034b0d0c9483ac8e018369eb000a/mynotes/docs/comparisons/comparison37/42404795321/host-logs/lookup-42406196610/manifest.json).

Verified improvements:44 Obsidian records achieve landscape, with WebView and PNG dimensions retained. Native X21 captures belong to MyNotes in all four profiles rather than the earlier notification prompt. Neither observation proves complete landscape workflows or widget placement.

Remaining failures: one job could not download the pinned artifact (`other side closed`); others lost the guest connection. PDF/TXT selection still failed, despite the provider-settling change. Landscape library/tools sheets expose only part of their actions; exact-label lookup did not reach several offscreen controls. These remain comparison route blockers, not automatically new app defects.

All20 original host logs were recovered after fixing the GitHub CLI escape-sequence refusal. Some failed guests show graphics ColorBuffer errors and the emulator console port is refused during subsequent cleanup. This does **not** establish graphics as the cause, nor OOM, nor an app crash; cleanup termination must not be mislabelled the initiating failure. Both publication runs completed; no emulator retry is running.

## Latest completed-cohort reports

- [Short-session report (PDF)](https://github.com/tarungittarun/TicTic/raw/4b77fb466187663f4804f2fab1ece2014263efbb/mynotes/docs/comparisons/comparison37/42346486192/Comparison37_Runtime_Probes.pdf).
- [Long-session recheck report (PDF)](https://github.com/tarungittarun/TicTic/raw/4b77fb466187663f4804f2fab1ece2014263efbb/mynotes/docs/comparisons/comparison37/42345604329/Comparison37_Runtime_Probes.pdf).
- [Short-session original counts and scope (JSON)](https://github.com/tarungittarun/TicTic/blob/4b77fb466187663f4804f2fab1ece2014263efbb/mynotes/docs/comparisons/comparison37/42346486192/summary.json).

Publication run [42348177659](https://github.com/tarungittarun/TicTic/actions/runs/42348177659) completed successfully. Both original cohorts remain separate; their rows are not pooled into a fictional complete execution.

## Earlier report downloads

- [First partial runtime report,77 pages (PDF)](https://github.com/tarungittarun/TicTic/raw/81768402a8c442297e5c7cbb4d158660cd7c2893/mynotes/docs/comparisons/comparison37/Comparison37_Runtime_Probes.pdf).
- [Second-run provisioning-blocker report (PDF)](https://github.com/tarungittarun/TicTic/raw/260d5e0f282db64d1628d1bd6484f6261db6c180/mynotes/docs/comparisons/comparison37/42343158423/Comparison37_Runtime_Probes.pdf).
- [Original first-run results and scope (JSON)](https://github.com/tarungittarun/TicTic/blob/81768402a8c442297e5c7cbb4d158660cd7c2893/mynotes/docs/comparisons/comparison37/42342628990/summary.json).
- [Original second-run results and scope (JSON)](https://github.com/tarungittarun/TicTic/blob/260d5e0f282db64d1628d1bd6484f6261db6c180/mynotes/docs/comparisons/comparison37/42343158423/summary.json).

Raw PNG/XML/DOM/session/fixture evidence is stored in each run directory. Its
`retention.json` lists original byte counts/SHA256 plus retained paths. New large text/XML/JSON files use verified lossless deterministic gzip; decompress before checking their original hashes. Public fixture bytes can be content-addressed across sessions. PNGs and module records remain directly readable. The originals are not
replaced with clean reruns. Completed-job log retrieval failed; `host-logs/manifest.json`
retains those retrieval failures. No host-crash/root-cause claim is made from missing logs.

## Independently reviewed limitations

`reviewed-observations.json` links exact immutable source evidence for independently checked
harness/corpus/infrastructure observations, including the later permission, picker and actual-profile failures. In particular:

- MyNotes X05 left a system image picker on top; subsequent native screenshots
  show DocumentsUI, not the requested note/settings/viewer pages.
- Obsidian's installed search ID was present, but the original regex matched ID
  plus a human name, so its anchored ID lookup failed.
- Obsidian DOCX produced a system Share chooser; this is not native DOCX fidelity
  proof. Initial Office fixture containers were invalid and cannot indict either app.
- Original guest connections disappeared after X22. Cause is unknown. Cases
  recorded after losing the guest are not executed functional comparisons.
- The second guard used IME-view state as keyboard visibility and repeatedly
  navigated Back out of the editor. This is an observer failure, not a new app defect.

## Concrete blockers still requiring comparison-harness work

- Native reminder X20 leaves the notification permission request open. Subsequent X21+ captures can belong to PermissionController, not MyNotes. This is not evidence that all those app features fail.
- PDF/TXT picker captures show Downloads with the Comparison37 folder; selection was not established. The valid DOCX did open and render its public marker, but fidelity/search/selection remain unverified.
- Actual landscape assertions fail for all Obsidian rows in run4 and many run5 sessions. PNG dimensions and WebView dimensions both confirm portrait; a few run5 light-landscape sessions really are landscape. The reason for that session difference is unproven.
- Some fresh guests also went offline; shortening sessions did not eliminate loss. Settings targets not visible to exact-label lookup and persistent tool-sheet surfaces need route/scroll investigation, not automatic app-defect labels.

## Still not accepted

The original task text is printed separately from the actual smaller probe in
all reports. Layout/icons must be reviewed from real frames; animations,
human efficiency/time, smoothness, accessibility/TalkBack, realistic varied
OCR/audio/document corpora, account services and physical-phone outcomes are
not established by these probes. Account/community-plugin boundaries remain
explicit. API navigation/fixture seeding is not touch-route efficiency evidence.
No extra-feature score advantage, paid training, cloud recognition, private
library upload, new APK, re-signing, public release or production fix is introduced.
