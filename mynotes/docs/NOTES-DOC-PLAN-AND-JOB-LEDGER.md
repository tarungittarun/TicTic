# Notes and Doc — implementation and qualification ledger

## Approved scope
Same APK, two distinct workspaces: simple Notes/checklists as before, and rich Doc.
Existing canonical documents are presented in Doc without rewriting storage. No automatic
conversion, flattening, deletion, or downgrade. Explicit **Copy to Doc** creates a new
identity and preserves the original plus the existing verified recovery checkpoint.
The former global canonical-default preference is no longer read or written.

Doc retains the agreed formatting, named mixed checklists, rich table cells, images,
drawings, diagrams, inline hyperlinks, reminders, labels, colour/background, folder
storage, local backups, PDF, history and privacy boundaries. This stage also implements
header roles, cell-relative row/column actions, inline vector diagram previews,
tap-to-edit visuals, a single + content-insertion menu, and line-position placement
for all whole content sections. Text handles remain on demand to keep writing clean.
Inline word hyperlinks remain word links, not separate draggable link-card objects.

### Safety details
- Workspaces filter the same library. Archive, Trash, folders and backups remain shared;
  Doc cards are identified. Existing rich documents are never silently made simple.
- Header is an optional strict boolean inside schema7 table data. Default false is omitted,
  preserving old encodings/fingerprints. Older APKs reject nonempty new table metadata
  rather than silently dropping it. Do not downgrade an updated installation.
- Header roles survive editing, column changes, history and storage; deleting the first row
  removes its header role. Inserting above a header requires disabling the role first,
  rather than silently demoting a populated header. PDF header emphasis is presentation
  only; no bold marks are injected into the user's stored cells.
- Movement retains object identity/data and splits rich text at a validated UTF-16 boundary.
  Original data/security, file-size bounds and opt-in diagnostics requirements remain.

## New job allowance (distinct from historical exhausted 35)
User explicitly approved **up to 24 additional individual Actions jobs**.
Reserve before launch. Four jobs per full checkpoint. No tests.yml or other build workflows.
Cap is not a target or billing authorization; no claim to have checked charges.

| Reservation | Jobs | Cumulative reserved | Run/source | Outcome |
|---|---:|---:|---|---|
| 1: initial Notes/Doc qualification | 4 | 4/24 | 41118729220 / 953daf0f | build PASS; dark115/116 and landscape95/116 FAIL (home viewport); API26 reporting pending at next checkpoint |
| 2: complete Doc usability + home viewport repairs | 4 | 8/24 | 41122179331 / 7e849b9f | build232 PASS; API26 248/249, dark117/118, land116/118; preview-helper discovery failure plus land Doc focus/viewport failure |
| 3: final flow and native dispatch repairs | 4 | 12/24 | 41124708672 / 15515e93 | build232 PASS, API26 251/251 PASS, dark120/120 PASS, land119/120 FAIL (actual writing line clipped) |
| 4: measured IME caret correction | 4 | 16/24 | 41126842332 / d79f8b68 | ALL PASS:232 JVM,251/251 API26,120/120 dark,120/120 landscape; all3 independent PDFs and complete fixtures |
| 5: exact table identity and post-settling regression | 4 | 20/24 | 41128791793 / e9fcf0d0 | build232 PASS, API26 252/252 PASS, dark121/121 PASS, land120/121 FAIL (journey tried hidden Back before dismissing IME) |
| 6: final grid clarity + native keyboard-aware Back | 4 | 24/24 | pending launch | pending |

**0 jobs remain unreserved. The new24-job allowance is fully reserved; do not launch further jobs without new approval.** Skipped/cancelled jobs remain conservatively reserved.

## Current validation
Local Python root **565 PASS**, standalone Notes **175 PASS** before workflow/version
metadata edits; rerun is mandatory before committing/pushing. Android builds are Actions-only.
No Java executable is installed locally: attempted pure-domain javac/JUnit did not run;
new JVM and Android tests are authored, **not yet executed**. Prior APK/code21 evidence is
historical and does not qualify this source. Device/novice usability and independent PDF
results must not be claimed from source contracts alone.

## Qualification checklist
- Build/JVM/debug + release lint; stable package/signer and increasing code22.
- Full API26 native inventory; focused dark2x and landscape inventories.
- Notes/Doc separation after old opt-in, original-preserving copy and checkpoint.
- Table header strict round-trip, contextual edits, Undo/Redo/reopen, PDF links/header.
- Regression inventory for earlier Notes data/storage/security/capture/history workflows.
- Preserve source-pinned screenshots/reports; inspect visuals and independent PDF checks.
- Deliver raw development APK with exact source/hash and honest completed/pending status.

## Checkpoint 2 implementation and evidence

- Complete adaptive Doc tools: no sideways toolbar; compact top save/history/overflow,
  two-row large-text tools, compact overflow fallback, document-wide writing lines.
- Writing continuation reuses an existing following field; insertion with no anchor asks
  the user to choose a location instead of silently appending.
- Caret link insertion is one immutable history transaction, with display text and safe URL;
  existing links expose Open/Edit/Remove without deleting words or other styling.
- Whole-object and row destination choices supplement drag; object boundaries have preview
  lines. Object previews open contextual Edit/Rename/Move controls; redundant Edit rows are
  removed. Image ratios are bounded, check visibility moves into checklist context.
- Home content/empty state shares one bounded weighted viewport, so tabs cannot push the
  dock outside landscape. Tabs hide during search IME to retain the earlier search space.
- Initial failing exact annotations retained in notes-doc-evidence/41118729220. No failures
  waived: dark search-title and landscape home-control clipping are the defects repaired.
- Manual cancel was denied with integration permission403. Reservation1 remains counted;
  the next deliberate workflow checkpoint may supersede it via existing concurrency.
- Peer reporting now observes source check-runs rather than stale jobs statuses; bounded
  GitHub lookup/pull retries retain genuine failure exit codes. No test inventories reduced.
- Current local root566/Notes176 PASS; all Java files parsed with tree-sitter (syntax only).
  Authored next inventory:232 JVM,249 full native; Android execution still pending.

## Checkpoint 3

Checkpoint2 preserved all three native reports, screenshots and independent PDF reports
in design-2026-10-29/native/41122179331 (source7e849b9f). Home Notes search and landscape
control regressions no longer appear. Its remaining failures are not waived:

- The native tap helper searched TextViews only and could not discover DiagramPreview.
  It now discovers named Views too, still sends actual Android DOWN/UP events and retains
  complete, stable, unclipped rectangles for toolbar/control targets. Scrollable image/
  diagram content can exceed viewport height; native taps require a visible48dp rectangle
  inside the actual focused app window and inject at that visible rectangle's center.
- Landscape Doc keyboard case retained a keyboard but failed focus/line visibility. Wait
  for modal/layout restoration before requesting IME. The measured-shrink recovery also
  restores its remembered writer when focus fell to a non-EditText, not just null; it never
  steals focus from another text field or a modal window. Diagnostic failures now identify
  actual field geometry/focus/visible line, not a generic timeout alone.
- Explicit Start writing recovers documents whose last content section was removed.
- Tapping a previously focused text field after object controls restores that text anchor
  and clears the object outline. New native journeys cover this and empty-document recovery.
- Evidence collector waits for all three uploaded profile names and reports missing profiles
  explicitly rather than silently accepting a partial fixture set.

Local gates: root568/Notes178 PASS, Java syntax parsed. Authored inventory232 JVM/251 full
native/120 focused. Builds/native checks for these latest fixes remain pending.

## Checkpoint 4 — measured caret correction, not a relaxed assertion

The new diagnostic established that the writer **was focused**. In the tiny landscape
IME viewport, ScrollView was123px tall, field144px, scroll69px: only75px of the field
remained visible versus a77px line. More importantly, Android's late rectangle request
clipped the top of the actual glyph line. The previous focus-recovery change alone did
not solve that. No check is waived.

Reapply the exact caret-line reveal after native scroll changes during a bounded1000ms
IME/focus-settling window. Explicit scroll/touch/drag cancels the window, so normal reading
is not pulled back to the caret. No polling/timer loop is added; correction from scroll notifications is limited to that settling window.
Native assertion is stronger: the **actual line top and bottom** must both lie inside the
field's visible rectangle, not merely some field area at least one line high. Controls
retain48dp targets; no font or touch target was reduced to make the check pass.

Source15515e93 evidence (all three profiles, PDFs, screenshots) is preserved under
`design-2026-10-29/native/41124708672/`. Checkpoint4 retains the232/251/120 inventory,
local root568/Notes178 gates, and requires a new exact-source Android qualification.

## Checkpoint 5 — final data-isolation guard

Manual source review identified that a selected cell's string key could share a prefix
with another imported table ID (e.g. `a` and `a:child`). Cell actions now require exact
selected-block identity as well as a cell key. New native journey exercises both IDs,
rejects the wrong table's context, edits the correct table and checks Undo isolation.
The native caret check also waits beyond the1000ms correction window and rechecks the
actual line, preventing a merely transient visibility pass. No production controls or
text sizes are reduced. Authored final inventory232 JVM/252 full/121 focused.
Local root569/Notes179 PASS before launch; all Java source syntax parsed.

## Checkpoint 6 — final reservation

Checkpoint5's landscape failure was a real timing-dependent **journey error**: it called
programmatic `click(Back)` while the existing legacy Notes header was hidden by its tiny
IME viewport. The same journey now uses the existing **native** `tap(Back)` helper, which
dismisses IME when the named header is hidden, waits for its real unclipped rectangle and
injects DOWN/UP. No visibility is forced and no assertion is removed. The strengthened
post-settling caret case passed; the table-prefix isolation case passed.

Visual review of the actual large-font table fixture also exposed insufficient body-cell
boundaries. Add visible theme-aware1dp borders,6dp horizontal text padding, header display
emphasis (not stored bold marks), and uniform full-row cell heights for wrapped text.
The existing native header/edit/Undo/reopen case now uses a multiline value and asserts
matching sibling heights/padding, in addition to unchanged data checks. No inventory shrinks:
232 JVM,252 full,121 focused. Local root570/Notes180 and Java syntax must pass before push.
This is the sixth/final4-job reservation;24/24 conservatively counted.
