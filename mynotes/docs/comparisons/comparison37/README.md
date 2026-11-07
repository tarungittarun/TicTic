# MyNotes71 vs official Obsidian1.15: comparison execution evidence

**Comparison phase remains open. Production fixes are on hold.** A page visit,
74 attempt records, native inventory totals or a green observer process must not
be described as completion of the original37 functional/UX acceptance tasks.

## Retained executions

| Run | What actually happened | Scope |
| --- | --- | --- |
| [42342628990](https://github.com/tarungittarun/TicTic/actions/runs/42342628990) | Portrait:74 records per theme, each3 bounded outcomes,16 interactions,1 diagnostic inspection,54 blockers. Landscape:0 module probes; fixture setup failed. | Partial runtime evidence only; many blockers are harness or guest loss, not app findings. |
| [42343158423](https://github.com/tarungittarun/TicTic/actions/runs/42343158423) | All four profiles:0 module probes; keyboard-observer guard aborts setup. Complete generated Office input bytes and fatal frames retained. | Failed provisioning, no comparison coverage inferred. |
| [42344599145](https://github.com/tarungittarun/TicTic/actions/runs/42344599145) | Completed failure. Portrait:44 records each; light landscape:0; dark landscape:74, including37 blocked Obsidian profile assertions. | Original guest-loss/permission/picker/profile failures retained; no whole-task acceptance. |
| [42345604329](https://github.com/tarungittarun/TicTic/actions/runs/42345604329) | Completed: both landscape jobs green, both portraits failed. Portrait:44 records each; landscape:74 each. All37 Obsidian rows in each landscape profile were blocked because the actual display was portrait. | Green jobs prove capture/attempt completion, not requested-profile or functional acceptance. |
| [42346486192](https://github.com/tarungittarun/TicTic/actions/runs/42346486192) | Completed:13 green/7 red short-session jobs. Actual records: light portrait60, dark portrait48, light landscape42, dark landscape74. | Five independent guests per profile, not a stable long session; no profile reached whole-task acceptance. |

## Latest recheck:42404795321

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
