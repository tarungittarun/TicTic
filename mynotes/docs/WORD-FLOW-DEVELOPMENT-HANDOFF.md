# Word-flow revision — v0.22.0 / code25 development handoff

**Implemented; final build and all four native jobs PASS.** This is a signed development APK,
not public-release approval or physical-device/novice acceptance.

## Exact installable artifact

- **MyNotes-v0.22.0-25-development.apk** (raw APK, not ZIP):
  https://github.com/tarungittarun/TicTic/actions/runs/41235123128/artifacts/13054959507
- Source `1d7462c694df4735fa771ab126b708846d6f63d0`; successful run `41235123128`.
- 66,717,828 bytes. GitHub artifact metadata SHA256:
  `7c08643316fb1706319cdac43ff0452a1c625b0ad431fed5c0853652ca9cb76f`.
  Metadata retrieved through GitHub; no claim of a separate local download/hash verification.
- Expires 2026-11-07 19:39:59 UTC; repository access may be required.
- Package `app.space.notes`; stable signing identity unchanged; min26/target34/compile35.

Back up important notes and original recordings first. **Install over the existing app; do not
uninstall or clear data.** Check About shows0.22.0/code25, not the older0.20 or0.21.
Non-default table widths use schema9; older APKs reject unsupported content rather than strip it.
Do not downgrade after saving these features without a compatible backup/recovery plan.

## Requested interaction changes

- Ordinary lines are writing fields. No Text option in Insert; Enter remains ordinary writing.
- Tables have a named header, right-side edit control and overflow with one Rows and columns
  entry, header toggle, Fit to content and fixed column width. No writing-paper ruling or native
  EditText underline inside cells; intended table borders remain. Live styled fit does not rebuild
  the writing field; fixed/fit proportions also reach PDF export.
- Table handle offers only Rename, Copy, Cut and Delete. Cut is deferred until successful Paste
  at a writing caret in the same Doc, preserving rich cells, name, header and width with Undo.
  Copy can paste into another Doc. The structured clipboard is app-memory only, not durable or a
  general system table clipboard; a process restart does not silently delete the source table.
- Doc top actions are not duplicated in its overflow. Colours/Background share one fifth bottom
  Background tool with tabs; Notes uses the same combined appearance surface. Labels live in
  overflow, not the bottom dock. Notes also has a direct Voice shortcut.
- Formatting has an X close control, visible section/option separation, Font/Size dropdowns,
  font previews and grouped More options. No visible New typing heading; accessible selection/
  subsequent-typing descriptions remain. Large system fonts use measured toolbar rows.
- Hyperlinks show label and URL together, render blue, preserve mixed styles for unchanged labels,
  and offer Open link / Copy link on long-press. Opening an external browser requires confirmation.
  Link-only touch hold avoids API26 native selected-word dragging; other text keeps native selection.
- Image viewing reaches3x; drawing/diagram viewing reaches5x, with pinch/pan/reset. Drawing
  creation zoom also reaches5x. Viewing does not alter original pixels or drawing identity.
- Explicit destructive operations, including diagnostic-log deletion, request confirmation.
  Existing seven-day Trash retention and bounded automatic cache/history maintenance are separate.
- Calculator unchanged; Archive and unrelated Wall work remain deferred. Local processing and
  existing rich content/attachments remain intact; no arbitrary Word import or cloud dependency.

## Final qualification

| Gate | Result |
|---|---|
| Local repository QA |615 PASS (includes MyNotes checks; not an additional unique total)|
| Local MyNotes QA |225 PASS|
| Debug/release compilation, lint, exact JVM gate |PASS;247 JVM inventory|
| API26 full native |280/280 PASS|
| API36 full native |280/280 PASS|
| API34 dark200% focused |144/144 PASS|
| API34 landscape focused |144/144 PASS|
| Independently parsed real exported PDF features/geometry |All4 profiles PASS|
| Whole GitHub Actions run |SUCCESS; all5 jobs completed successfully|

No skipped, waived or removed native data assertions. Original failed attempts remain recorded in
[the job ledger](WORD-FLOW-REVISION-AND-JOBS.md). The last run also verifies repeated long-press
Copy/Open/Cancel, rich table movement/Undo, saved-style typing/recreation and image preservation.

Source-bound receipts and synthetic screenshots:
`design-2026-10-29/native/41235123128/` (77 bounded JPEG previews per profile; cap84, not84 required).
All four profiles retain word-appearance, word-link-form, word-table-options, word-zoom,
document-dragged and document-typing-style. Reviewed final API26 style rendering, large-font
500% zoom and landscape table captures; these are not a hands-on novice trial.

## Honest remaining validation and limits

Physical Motorola edge50neo/Android16 install-over and novice usability walkthrough remain pending,
as do physical power-loss/provider/OEM testing and the combined200%-font-plus-landscape scenario.
The automated large-font and landscape profiles are separate, not that combined scenario.
Existing backup/recording omissions and bounded history retention still apply; back up original
recordings separately. No public/Play-release or billing approval is claimed.

Fresh40 authorization: **35 individual jobs executed/reserved;5 unused**. Earlier allowances are
separate. The requested150-minute uninterrupted streak is **not certified**: elapsed wall time
and CI waits are not proof of uninterrupted productive work. No idle padding or hours claim.
