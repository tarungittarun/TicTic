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
| 2: complete Doc usability + home viewport repairs | 4 | 8/24 | pending launch | pending |

16 jobs remain unreserved. Skipped/cancelled jobs remain conservatively reserved.

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
