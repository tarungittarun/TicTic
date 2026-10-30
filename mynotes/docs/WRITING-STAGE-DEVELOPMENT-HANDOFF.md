# Scoped writing stage — code21 development handoff

**Implemented and exact native gates PASS. Overall CI/evidence preservation remains incomplete; signed development candidate, not public-release approval or complete pipeline sign-off.**

## Scope implemented

This stage changes only the existing opt-in document editor's text styling, ordinary-writing presentation and checklist boxes. It is not the broad editor redesign. [Stage plan and runner ledger](WRITING-STAGE-PLAN-AND-JOB-LEDGER.md); [step-by-step user guide](WRITING-STAGE-USER-GUIDE.md).

- **Aa** replaces Format. Scrollable visible Bold/Italic/Underline, Strike, Normal/Heading1/Heading2, font and8–72sp size controls, active/off-or-mixed state, and explicit **Selected text** versus **New typing** explanations. Common choices apply and return to writing. At large system text, inline styles use two columns, headings get more space and size steppers have their own row; labels preview bold/italic/underline/strike. Existing safe links, numbering and named presets remain under More text options.
- Ordinary text no longer has a permanent Text/handle header. Title/writing remain native editable fields; no content flattening or automatic block merging. Text section options can reveal the existing handle on demand.
- Selected text converts into a bordered checklist box with an editable name; new defaults Checklist01/02. Surrounding text and inline marks survive. Existing row grips and + List item remain; box grip/name opens contextual Rename/Move/Insert after/Remove. Long-pressing its grip opens the same options.
- Whole checklist boxes can move into a specific writing-field line position. Native local drag has a coloured insertion marker and edge scrolling; Move checklist offers tap-to-place and Cancel move. Drops split rich text without copying/dropping box contents. A committed move is one Undo step.
- Names survive row editing/checked state/add/reorder/hide, note merges, saved library/portable backup, history, exported text/PDF and reopening. Existing empty names decode unchanged. Schema7's existing name field is used; older APKs reject named boxes rather than silently stripping them. Do not downgrade.

## Deliberately unchanged/deferred

NotesController, DrawingPad, DiagramView and ImageEditView production sources are byte-for-byte unchanged from code20. Table row/column operations, visual editors, diagrams, Insert entry points, file intake/trust rules, Home/Folders and local backup/history bounds are not redesigned. Only checklist caption printing is added to the PDF flow. Calculator is unchanged, Archive paused, Wall dragging deferred.

The older simple editor is not replaced or automatically migrated. Existing deliberate Enable document editor conversion remains the route for an old note. The wider unified +, table-header workflow, new visual previews and general object movement redesign are deferred.

## Qualification

Source:`9e7547b3055071fc22eb5e18a693100aa09fc879`; run41098898803.

| Gate | Result |
|---|---|
| Local root source/QA |562 PASS; includes the172 MyNotes checks, not734 unique cases|
| JVM inventory |221 PASS in build124211784854|
| Compilation, debug/release lint, stable signing |PASS|
| Full API26/124213217727 |244/244 PASS, exact inventory|
| Dark2×API34/124213217814 |114/114 PASS, exact inventory|
| LandscapeAPI34/124213217684 |114/114 PASS, exact inventory|
| Independent actual exported PDF checks |Prior65062be3 checkpoint: all3 PASS. Final modern jobs succeed; final API26 independent result unrecovered.|

First candidate5363548/run41091891301 passed242/244 full and112/114 each focused. All existing journeys and actual native checklist-to-text drag/marker tests passed. Two new journeys failed because an accessibility helper did not handle null node text and row descriptions duplicated their parent box name. The replay fixes null handling and gives each row a distinct `box name, list item N` accessibility label, retaining all assertions and adding real merge name-preservation assertions. Original reports/screenshots remain under`design-2026-10-29/native/41091891301/`; evidence commit65340331. No skipped/waived cases.

Replayb1e1d3f/run41093980631 passed244/244 full and114/114 dark, but113/114 landscape: pointer coordinates shifted to the next line during edge scrolling. Adaptive viewport-relative edge zones plus an exact pre-drop marker-offset assertion qualified in65062be3/run41096558875:244/244,114/114,114/114; all PDFs PASS, evidence399feab7. Final9e7547b3 adds responsive Aa layout after actual2× screenshot review and tests size-increase for subsequent typing. It does not expand the scope. See the ledger for every reservation and original failure.

## Final run limitation — do not conceal the red workflow

All three native exact-inventory gate annotations are confirmed against the final source. Dark2× and landscape jobs succeed. The API26 job nonetheless fails, with generic exit128/1 annotations after its244/244 native gate; the overall run is **failure**, not green. Native job-step detail is absent from the recovered API response. GitHub502/504 status errors and signed-download EOF prevented recovering full final reports/screenshots. Exact failed-step attribution is not known; do not assert that a specific step or every PDF gate passed.

[Source-bound manually captured check summary](writing-stage-evidence/41098898803/README.md) preserves job outcomes, exact source-linked gate notices and artifact metadata. It is **not** the usual full fixture bundle. Final Aa screenshots have not been visually reviewed; the responsive layout was reviewed in code and exercised by native tests. Prior checkpoint screenshots are labelled with their true source and do not represent the final Aa layout. Physical novice testing is still required. No extra runner was launched after the allowance was exhausted.

## Signed raw development APK

- Name:`MyNotes-v0.18.0-21-development.apk`; version0.18.0/code21.
- Raw APK, not ZIP: https://github.com/tarungittarun/TicTic/actions/runs/41098898803/artifacts/12996819517
- 66,682,092 bytes; raw artifact SHA-256:`821da2ae1b00470e6c32da0faae978bfbde98b13ac3ba27b34a5d423dee34a6f`.
- Package`app.space.notes`; min26/target34/compile35.
- Stable signer SHA-256:`08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca` verified by mandatory build gate.
- Artifact expires2026-11-06 17:28:59UTC; GitHub repository access may be needed.

Install over the existing app after backing up important notes and recordings. **Do not uninstall or clear data.** Notes must show saved before relying on retained history. Existing per-note retention/file/backup limits remain. A screen-wrapped line is not a permanent document line: placement commits a text offset, and subsequent wrapping changes with font/width. Individual visual/drawing workflows are not newly qualified features of this stage; their existing regression journeys remain mandatory.

Runner accounting:16 jobs for this stage,35/35 cumulative,0 unused/unreserved. No further jobs will be launched under this allowance. No additional allowance assumed; no measured-hours claim. **Physical Motorola edge50neo/Android16 usability acceptance and public/Play release approval remain pending.** Automated passes do not establish that novice users find the editor understandable.
