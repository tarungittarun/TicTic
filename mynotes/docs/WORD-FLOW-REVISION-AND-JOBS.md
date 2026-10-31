# Word-like editing revision — fresh 40-job authorization

Started 2026-10-31 approximately17:02 UTC. Requested effort150 minutes; actual duration must be
reported truthfully, never padded or inferred from CI waiting. Fresh cap40 individual Actions jobs,
independent of every earlier allowance. Reservation1 now books5 jobs (build + four native profiles);35/40 unreserved.

## Requested contract
- Ordinary writing is text by default; no Text choice in Insert. Preserve existing rich blocks.
- Table header: name, edit at right, overflow for cell/row/column/header and layout options.
  One Rows and columns entry, not duplicate structure menus. No native underline inside cells.
- Table handle menu only Rename, Copy, Cut, Delete. Paste at a text caret, preserving rich cells,
  header and name. Cut must not lose data on failed placement; Undo remains available.
- Fit table to content or fixed column width. Display preference must not silently mutate data.
- Top-only Undo/Redo/Find, not duplicate overflow actions. Remove Colour/Background from overflow.
  Fifth bottom tool combines Colours/Background tabs. Labels only in overflow.
- Formatting: section/option separation, close X, no visible New typing heading. Font and Size
  look like dropdown selectors, with accessible descriptions retaining selection/typing scope.
- Hyperlink visible text and URL in one form; blue links; long-press Open/Copy link.
- Local visual viewer supports image3x, drawing/diagram5x; no pixel mutation from viewing.
- Confirm user-triggered permanent deletion, including diagnostic logs. Preserve automatic bounded
  cache/history maintenance and documented Trash retention separately from explicit delete actions.
- Same design language in Notes where applicable; Calculator unchanged; no cloud dependency.

## Qualification plan
Root and Notes tests before every push; Android builds only in authorized Actions workflow.
Retain API26/API36 full suites, dark200% and landscape focused suites, PDF probes and existing data
preservation tests. Update obsolete UI expectations, never remove data assertions to obtain green.
Physical novice/OEM approval remains separate. New install must clearly identify a newer version
than rejected0.20 and prior0.21. No public-release approval claim.

| Reservation | Jobs | Cumulative reserved | Source/run | Result |
|---|---:|---:|---|---|
|1: full implementation and new data/UI safety coverage|5|5/40|8daa3baa /41223351324|Build245 JVM PASS; API26/API36 271/276 each, large-font/landscape135/140 each; all4 PDF PASS. Same5 UI failures in each profile; not qualified.|
|2: live table fit, mixed links, visual/UI polish, lifecycle safety and updated native navigation|5|10/40|c52e49ca /41224792310|Build PASS. Focused profiles142/144 each: native link toolbar both, dock label clipping large-font, drag viewport landscape. Full profiles cancelled at48/280, not passes. Both completed PDF probes PASS.|
|3: measured dock row capacity, link action refresh/priority, drag focus and caret isolation|5|15/40|03e04617 /41228015699|Build PASS; large-font143/144, landscape141/144. Full profiles timed out at48/280 each. Not qualified.|
|4: direct link long-press, stable compact threshold and measured native drop target|5|20/40|51321a63 /41230888109|Build PASS; API36 279/280; API26 277/280; large-font/landscape143/144 each. Common failure: section drop consumed by ordinary blank text field. API26 additionally: More text options and second link long-press. Four PDF probes PASS.|
|5: internal-drag-safe ordinary writing placeholders|5|25/40|0b78a88b /41232258716|Build247 JVM PASS; API36 280/280; large-font/landscape144/144 each; API26 279/280, second link long-press fails. Four PDF probes PASS.|
|6: repeat link long-press and native dialog focus restoration|5|30/40|launching|Local615 root/225 Notes gates; full qualification required|

No previous unused slots are combined with this cap. Failed/cancelled executions count.

First candidate:0.22.0/code25, schema9 only when a table has non-default sizing. Reads1–9.
Default-width tables and ordinary existing notes retain prior schema output. No downgrade after
using saved table sizing; backups preserve rich cells, names, header and width. Table Cut is
deferred until successful Paste inside the source document; Copy supports another open Doc in
the same app process. The structured table clipboard is app-memory only, not a public system clip.

Round2: no paper ruling within table cells; bounded live styled fit with per-column debounce and
matching PDF width proportions. Preserve mixed styles when editing link URLs and inherit style
when inserting a link.5x drawing creation/viewing and focus-anchored zoom. Grouped More, font
previews, real icons, large-font Appearance layout; fifth Background tool in Notes and Doc.
Guard stale dialogs and changed hyperlink forms before applying edits. Source1 failures audited:
row menu test did not focus its cell; Labels and palette expectations obsolete; bulk-label helper
opened wrong overflow; table menu required native-window settling/scroll; visual locator required
non-TextView accessibility matching. Data assertions retained. New real native long-press regression
and word-zoom screenshot remain unqualified until reservation2. No150-minute claim.

Reservation3 remediation: measured row capacity replaces hardcoded2+3 Notes dock split.
Native link actions notify ActionMode on prepare and are prioritized; test waits for actual
action readiness instead of assuming overflow presence. Native drag takes container focus,
hides IME and suppresses caret reveal until drop/end. No data assertions removed. Notes
formatting close now uses X as requested. Cancelled full lanes remain unqualified.

Reservation4: direct two-action link long-press menu keyed to touched character; plain writing
retains native selection. ActionMode prepare reports only actual menu changes. Native drag test
uses a visible lower-half target, and always releases its pointer even on assertion failure.
Disclosure test closes Format with X before navigating Back in a compact viewport. Full-lane
hang repeated immediately after48 methods: audited compact-layout padding feedback, now computes
threshold using stable normal padding, not its own changing dock padding. Instrumentation bounded
at20min within30min job so a future hang can retain partial diagnostics. No retries or ignored
exit codes, and no weakening of full inventory.30min source3 cancellations are not passes.

Reservation5 keeps the default writing appearance, but uses the existing WritingField subclass
for blank lines so native internal drags bubble to the containing section instead of being
consumed by Android EditText. Same native reorder/Undo/data assertions retained. Source4
completed all methods in both full lanes; no hang remains in that evidence.

Reservation6 collapses the selected link after its menu action, preserving text/marks while
allowing API26 repeat long-press instead of dragging an already-selected native word. Dialog
selection restoration waits for the document window to regain focus and restores original
field focus; transient title focus cannot close a locked formatting tray. Same real native
Copy/Open/Cancel and data-preservation tests retained. API26 saved-style failure in source4
was absent in source5; the explicit focus guard nevertheless covers the audited transition.
