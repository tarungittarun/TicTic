# MyNotes improvement execution ledger

**Owner request:** test the relevant thing in **both installed apps** before recording a comparative improvement. Cover pages, features, icons, presentation, layout, animation, efficiency, smoothness, timing and failure/recovery. Keep the evidence and use these stable items for later fixes.

**This is a living execution record, not a claim that the whole app has been tested.** The previous comparison PDFs are historical visual/source audits. They are hypothesis inputs, not fresh paired-task results. Creating the initial ledger did not authorise app fixes. On 2026-11-06 the user explicitly authorised starting implementation; candidate70 changes now require separate changed-build qualification.

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

## Implementation authorised — candidate71 current, candidate70 history

User requested applying fixes on 2026-11-06. IMP-0001 and IMP-0002 are implemented in `be8643bb9967ff5f3cda0093fdf89e5736703609`, **not yet accepted as fixed**. Changed-build JVM/lint and four-profile native results are tracked separately. No production APK is being published by this nonpublishing qualification. Existing baseline frames/source hashes and all failed attempts remain unchanged.

**Current result:** candidate71 passed Actions build/lint, 366 JVM tests and exactly eight focused native methods per four profiles. See `implementations/42231478498/`. Candidate70 API26 failure and preflight/schema/retention corrections remain in the execution history. Both items are `implemented_partially_verified`: not closed or whole-feature/physical accepted. No public APK release or install-over qualification is implied.

| New implementation term | Meaning |
|---|---|
| Implemented, partially verified | Code committed and named changed-build checks passed; explicit wider acceptance gaps remain. This is not a closed/fixed whole feature. |

Native qualification screenshots were resized to 540px by the existing instrumentation capture helper; retention did not alter their pixels. They use public seeded fixtures, not the exact paired-session input/caret/scroll state. Do not infer a percentage viewport, speed or usability gain by comparing these frames to baseline screenshots.
