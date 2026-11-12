# MyNotes improvement execution ledger

**Current source follow-up:** candidate72 / 0.72.0 (code72) contains a code-only large-text UI pass for the previously recorded Library and image-selection viewport failures. GitHub build and focused Android qualification are pending; no finding is marked fixed. See [candidate72 scope and acceptance status](../LARGE-TEXT-CANDIDATE72.md). Candidate71 run history below remains historical and unchanged.

**Owner request:** test the relevant thing in **both installed apps** before recording a comparative improvement. Cover pages, features, icons, presentation, layout, animation, efficiency, smoothness, timing and failure/recovery. Keep the evidence and use these stable items for later fixes.

**This is a living execution record, not a claim that the whole app has been tested.** The previous comparison PDFs are historical visual/source audits. They are hypothesis inputs, not fresh paired-task results. Creating the initial ledger did not authorise app fixes. On 2026-11-06 the user explicitly authorised starting implementation; candidate70 changes now require separate changed-build qualification.

## Latest expanded check — completed, not all green

The full candidate71 run [42277337133](https://github.com/tarungittarun/TicTic/actions/runs/42277337133) executed all **446 discovered native methods across six profiles: 2,671 passed / 2,676 executions, five failures**. Fresh domain JVM **366/366**, debug/release build and lint, offline/size/signing audit, and signed **69→71** public-rich install-over checks on API26/34 passed.

The completed [recheck 42282354356](https://github.com/tarungittarun/TicTic/actions/runs/42282354356) passed the entire **218-method API26 owning shard** after correcting an API30-only instrumentation observer. The original spreadsheet query mismatch did not reproduce; it is **not called fixed**. Three unchanged navigation/large-text assertions failed again in isolation. This is not a production fix, all-profile pass, public release approval, physical acceptance or completed Obsidian comparison.

- [Detailed full check PDF](../full-qualification/Full_Qualification.pdf)
- [Full check and five-failure triage](../full-qualification/Full_Qualification.md)
- [Original full-run receipts and SHA256 manifest](../full-qualification/42277337133/summary.json)
- [Stable native-failure IDs, classifications and next tests](../full-qualification/findings.json)

## Main document and machine-readable record

- [Improvement execution document](Improvement_Execution_Ledger.md)
- [Stable task inventory, observations and future work items](ledger.json)
- [Source-surface inventory](source-surfaces.json): inventoried first-party labels/control definitions, not runtime acceptance.
- Fresh native evidence is stored under `sessions/<run-id>/`; original artifact/run links, image hashes, environment, exact APK digests, inputs and command traces accompany observations.

## Exact wording and status rules

| Term | Required meaning |
|---|---|
| Planned / not tested | No fresh outcome. A source label, screenshot or command registry is not execution. |
| Interaction observed | Named controls were exercised and resulting real UI recorded; not necessarily verified content persistence. |
| Visible outcome verified | Stated observable result checked. The description must name the check, and must not imply stronger save/recovery guarantees. |
| Paired evidence reviewed | Same bounded task attempted in both pinned apps and retained results reviewed. This is not every feature on those pages. |
| Blocked | Setup, harness, permissions, account or task could not complete; exact reason retained. Not automatically an app defect. |
| Root cause hypothesised | Mechanism plausible from source; not isolated by a causal experiment. |
| Root mechanism source-supported | Observed outcome is consistent with a named code branch. Still not a demonstrated exclusive cause or a verified fix. |
| Cause confirmed | An isolating experiment rules out the stated alternatives. Source familiarity alone does not qualify. |
| Proposed fix | A design/code change to evaluate later, with risks and a retest. Not implemented or known to work. |
| Fixed / verified | Separate implementation commit and actual acceptance retest on that build, with retained evidence. No such status is earned by this audit. |

A finding must say **which app, version, fixture, state, action, observed outcome, scope and confidence**. Never write “slow”, “smooth”, “broken”, “missing”, “fixed”, “always”, “never” or “100%” as a measured conclusion without a definition and supporting evidence. “Not established in this scope” is not “does not exist”.

## Required record for every actionable improvement

Stable ID; task/surface; both app identities; environment; preconditions; exact actions; expected outcome; actual outcomes separately; evidence paths/hashes; metric definition and samples (or null with reason); user consequence; reproduction count; severity rationale; root-cause confidence; source path/symbol/line and hash; alternative explanations; proposed change; protected requirements; regression risks; acceptance tasks; fix owner/status; eventual implementation/retest references.

Do not write a cause/fix into the findings section merely to fill a template. Unknown fields stay **unknown**, with the next discriminating test named. Non-equivalent app-specific tools are recorded as such rather than inventing a competitor workflow.

## Testing constraints

- Exact existing, unmodified signed MyNotes **0.69.0/code69** APK and official Obsidian **1.15.0/code396**; SHA-256 checked before install. No new APK build or re-signing.
- Fresh disposable Actions emulator; never the user's data. No uninstall/clear/downgrade of the user's app; no accounts, payments, cloud uploads or community-plugin execution.
- Both apps on the same emulator/API/display/font/keyboard. Animations enabled at 1.0. Match fixtures and state as far as the task permits; document differences and setup shortcuts.
- Scripted live interactions are **not participant usability research or physical-device acceptance**. They can supply actual outcome evidence, followed by visual review; E2E pass totals do not supply UX quality.
- `am start -W TotalTime` is an Android activity-start milestone, **not time to an interactive app**. ADB/inspection elapsed durations include harness overhead and are **not touch latency or human efficiency**.
- Keep raw `gfxinfo`/memory diagnostics as diagnostic samples, not a cross-engine smoothness ranking. No animation, jank or battery claim without an appropriate measured trace and defined interpretation.
- Historical baseline source bytes remain pinned under `source-baselines/`. Current implementation refers to stable work-item IDs; earlier screenshots cannot verify the changed build.

## Current run bookkeeping

The ledger records failed provisioning separately from actual app sessions. The initial CLI retrieval of the raw APK did not reach app interaction; the replacement uses the raw-artifact-aware download action. Neither event is an app crash or UX finding. Refer to the main document for exact run states and the bounded results actually obtained.

## Reviewed corrections — do not count raw harness statuses blindly

- **Wave one (42217122419):** initial connectivity receipt reports `Active default network: 100`. Offline isolation was not established.
- **Wave two (42218900341):** initial default network `none` is not a pre-install isolation proof or evidence of no background traffic. The exact Obsidian body readback blocked on empty text. Its S01 raw `visible_outcome_verified` is **invalidated**: native screenshot and DOM show **0 results / No matches found**; the harness found a title in the background editor. Original bytes/status are preserved, not rewritten.
- A separate API-seeded fixture for downstream visual state comparison is setup, not a note-creation, typing-efficiency, save-durability or W01 pass. An input blocker is not proof of app data loss.
- No private libraries, sync credentials or intentional upload actions were used. Absence of background transfer has not been measured. See the document's evidence-review section.

- **Wave three (42220127426):** offline bootstrap verified before installation (three consecutive checks; shell unrooted to UID2000); initial/final default network `none`. Obsidian W01 independently passed exact editor/vault readback **before** the separate explicit API fixture setup. Visible S01 result title/passage assertions and native screenshot agree. No continuous packet capture or reliability guarantee; the earlier input failure remains unisolated. All three sessions' originals remain retained.

## Implementation status — candidate71 history, candidate72 pending

User requested applying fixes on 2026-11-06. IMP-0001 and IMP-0002 were implemented in `be8643bb9967ff5f3cda0093fdf89e5736703609`, **not accepted as fully fixed**. Their candidate71 changed-build JVM/lint and four-profile native results are tracked separately. Existing baseline frames/source hashes and all failed attempts remain unchanged.

**Historical candidate71 result:** Actions build/lint, 366 JVM tests and exactly eight focused native methods per four profiles passed. See `implementations/42231478498/`. Candidate70 API26 failure and preflight/schema/retention corrections remain in the execution history. Both prior items are `implemented_partially_verified`: not closed or whole-feature/physical accepted. No public APK release or install-over qualification is implied.

**Candidate72 result:** large-text Library and image-selection viewport source changes are now present in 0.72.0/code72. The source-scoped workflow is prepared but has not yet run; no candidate72 build or native result exists, and no finding is marked fixed.

| New implementation term | Meaning |
|---|---|
| Implemented, partially verified | Code committed and named changed-build checks passed; explicit wider acceptance gaps remain. This is not a closed/fixed whole feature. |

Native qualification screenshots were resized to 540px by the existing instrumentation capture helper; retention did not alter their pixels. They use public seeded fixtures, not the exact paired-session input/caret/scroll state. Do not infer a percentage viewport, speed or usability gain by comparing these frames to baseline screenshots.
