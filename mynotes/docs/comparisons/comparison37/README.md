# MyNotes71 vs official Obsidian1.15: comparison execution evidence

**Comparison phase remains open. Production fixes are on hold.** A page visit,
74 attempt records, native inventory totals or a green observer process must not
be described as completion of the original37 functional/UX acceptance tasks.

## Retained executions

| Run | What actually happened | Scope |
| --- | --- | --- |
| [42342628990](https://github.com/tarungittarun/TicTic/actions/runs/42342628990) | Portrait:74 records per theme, each3 bounded outcomes,16 interactions,1 diagnostic inspection,54 blockers. Landscape:0 module probes; fixture setup failed. | Partial runtime evidence only; many blockers are harness or guest loss, not app findings. |
| [42343158423](https://github.com/tarungittarun/TicTic/actions/runs/42343158423) | All four profiles:0 module probes; keyboard-observer guard aborts setup. Complete generated Office input bytes and fatal frames retained. | Failed provisioning, no comparison coverage inferred. |
| [42344599145](https://github.com/tarungittarun/TicTic/actions/runs/42344599145) | Recheck executing at this publication: separate command-ID matching, recognized chooser cancellation, exact Insert/About routes, manager-only keyboard state, full IME dumps and capture-health gating. | Outcome not yet known; not completion or app-fix evidence. |

## Actual report downloads

- [First partial runtime report,77 pages (PDF)](https://github.com/tarungittarun/TicTic/raw/81768402a8c442297e5c7cbb4d158660cd7c2893/mynotes/docs/comparisons/comparison37/Comparison37_Runtime_Probes.pdf).
- [Second-run provisioning-blocker report (PDF)](https://github.com/tarungittarun/TicTic/raw/260d5e0f282db64d1628d1bd6484f6261db6c180/mynotes/docs/comparisons/comparison37/42343158423/Comparison37_Runtime_Probes.pdf).
- [Original first-run results and scope (JSON)](https://github.com/tarungittarun/TicTic/blob/81768402a8c442297e5c7cbb4d158660cd7c2893/mynotes/docs/comparisons/comparison37/42342628990/summary.json).
- [Original second-run results and scope (JSON)](https://github.com/tarungittarun/TicTic/blob/260d5e0f282db64d1628d1bd6484f6261db6c180/mynotes/docs/comparisons/comparison37/42343158423/summary.json).

Raw PNG/XML/DOM/session/fixture evidence is stored in each run directory. Its
`retention.json` lists exact byte counts and SHA256 values. The originals are not
replaced with clean reruns. Completed-job log retrieval failed; `host-logs/manifest.json`
retains those retrieval failures. No host-crash/root-cause claim is made from missing logs.

## Independently reviewed limitations

`reviewed-observations.json` links exact immutable source evidence for seven
harness/corpus/infrastructure observations. In particular:

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

## Still not accepted

The original task text is printed separately from the actual smaller probe in
all reports. Layout/icons must be reviewed from real frames; animations,
human efficiency/time, smoothness, accessibility/TalkBack, realistic varied
OCR/audio/document corpora, account services and physical-phone outcomes are
not established by these probes. Account/community-plugin boundaries remain
explicit. API navigation/fixture seeding is not touch-route efficiency evidence.
No extra-feature score advantage, paid training, cloud recognition, private
library upload, new APK, re-signing, public release or production fix is introduced.
