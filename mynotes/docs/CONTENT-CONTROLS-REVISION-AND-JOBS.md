# Content controls, typing and motion — fresh40 authorization

User request 2026-11-01; new cap40 individual Actions jobs, separate from prior35/40 closed.
Work in one continuing session through qualification; no invented uninterrupted-time claim.

## Contract
- Fix actual List crash on code25: diagnostics show uncaught IllegalArgumentException from
  DocumentContent.selectionToChecklist when selection is collapsed. No private diagnostics copied into Git.
- Pen opens content management (Rename/Copy/Cut/Delete); grips move, not open management menus.
  Checklist/options-specific controls under overflow; same hierarchy for inserted visuals.
- Four-dot item grip, distinct from whole-content grip; long-hold movement with respectful haptics,
  lift, insertion marker and settlement feedback. Remove Move item from item menu; preserve accessible movement.
- Copy/Cut/Paste whole checklists and inserted content without flattening, losing rows or deleting source early.
- One visibly elevated Format panel: fifth Clear control, Size/dropdown/steppers on one row,
  no Links/Styles/More submenus; existing saved preset files and rich content must not be deleted.
- Continue typed numbered paragraphs; Backspace/empty-entry exit remains editable and local.
- Audit app sections for clarity, transitions, haptics and reduced-motion behavior. Calculator frozen.

## Gates and accounting
Local root QA before every push, Notes checks, no Android builds outside Actions, tests.yml untouched.
Retain full API26/API36, focused dark200% and landscape, actual PDF checks, data preservation and
new crash/gesture/typing regressions. Automated qualification is not physical OEM or novice approval.

| Reservation | Jobs | Cumulative | Source/run | Result |
|---|---:|---:|---|---|

| 1 | 5 | 5/40 | `7e30daf0`; run41277605164 | Build failed: new visual-copy test used a non-UUID image fixture. 259/260 JVM passed; native matrix never expanded. |
| 2 | 5 | 10/40 | Candidate2; run assigned after gated push | Reserved |

10/40 slots conservatively reserved/consumed; candidate1 executed1 build, four native slots unexecuted after build failure (not recycled). Candidate2 reserves5. Failed/cancelled individual jobs count; unused earlier allowances are not added.

## Candidate1 implementation and test scope
Collapsed List inserts at caret with surrounding rich slices intact. Pen owns Rename/Copy/Cut/Delete
(and editable visual content); overflow owns structure/placement. Copy regenerates block/row/image
identities; Cut remains same-document and deferred until immutable paste validation succeeds.
Four-dot item grips require a hold, with source lift, target boundary and settling cue. Accessible
row actions remain available without a clicked Move item menu. Format is one elevated outlined dock
with B/I/U/strike/Clear, paragraph controls, font, Size/−/+ row and paragraph numbering. Preset
files/storage classes are retained; removed preset submenus are not silently reintroduced.
Literal typed numbering continues only on a single newline edit, with whole-prefix Backspace and
empty-entry exit in both simple Notes and Doc. System animation/haptic settings are respected.
New JVM cases cover numbering boundaries and rich clipboard identities; five new native journeys
cover the reported caret crash, pen/cell scope, whole-checklist copy/deferred Cut, and both numbering
editors. Existing typing/persistence/Undo assertions are retained after the explicitly removed Styles
UI was replaced with direct Bold. Three new synthetic scenes expand the hard archive cap84→88
(the allowlist is86), not an unbounded collection or a data-assertion waiver.

Candidate2 fixes the invalid test UUID without relaxing model validation or rich assertions.
New pen tests inspect the actual dialog accessibility tree, rather than the underlying editor.
Native item drag also exercises its accessible Move item later action and Undo. Shared Notes/Doc
control rows disable text-baseline alignment so icon/caption and Size/steppers are truly centered.
