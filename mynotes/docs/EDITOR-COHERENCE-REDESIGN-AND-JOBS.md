# Notes / Doc coherence redesign — fresh 35-job authorization

User feedback on delivered code23: Find, Aa and Insert feel complicated and disconnected;
content-level versus whole-note/document actions are unclear; icons and app identity are inconsistent.
This is a rejected usability outcome despite green automation. Do not relabel it as accepted.

## Design contract
- Notes remains a quick, simple note/checklist workspace. Rich Doc stays separate in the same APK.
- One Paper & Ink vocabulary: existing app palette, outline icons, 48dp hit areas, visible tool names.
  No font-dependent arrow/more/drag glyphs standing in for drawn icons in Doc's main chrome.
- Find is a mode of the current editor: query, up/down, count/preview, Close. The page stays visible.
  No modal results screen, no persistent highlight formatting. Search remains bounded/off-thread.
- Insert adds content at the writing position; Format changes selected text or subsequent typing.
  Basic Doc formatting docks to the page, matching Notes' inline formatter. Advanced choices may
  use a bottom tray, never pretend to be another document. Scope is explicit.
- Manage-note/document menus are distinct from selected-content menus. Content menus identify their
  scope; Rename/Edit should be discoverable; removal is scoped and undoable, not a note deletion.
- App-owned menus use branded bottom trays. Real Android date/time pickers remain native.
- No migration, flattening, storage-format change, attachment mutation or removed rich capability.

## References and honesty
Keep's simple writing-first separation and the previously linked official Keep formatting/checklist
help inform Notes; Word/Google Docs mobile's document editing/ribbon/context patterns inform Doc.
These are design references, not a claim that this sandbox ran or user-tested those products.
Current sources reviewed:
- Google Material Android bottom sheets: https://github.com/material-components/material-components-android/blob/master/docs/components/BottomSheet.md
  Secondary content is bottom-anchored; modal versus nonmodal surfaces differ in scrim, not vocabulary.
- Microsoft Fluent UI Android: https://github.com/microsoft/fluentui-android
  Official mobile components include menus, drawers, top app bars and coherent shared icon/list systems.
- Earlier product/accessibility references and accepted constraints remain in
  `EDITOR-USABILITY-RESEARCH-AND-REDESIGN.md`. This new critique supersedes any claim that code23's
  mere feature availability establishes usability acceptance.
No new UI framework, copied app branding, cloud dependency or third-party SDK introduced.

## Execution and job ledger
Fresh **35 individual Actions jobs**, separate from previous30 (26 conservatively reserved/4 unused)
and older allowances. Cap is not a target or a billing promise. Android builds Actions-only.

| Reservation | Jobs | Total | Run/source | Status |
|---|---:|---:|---|---|
|1: inline Find, named vector tools, docked Doc formatting and scoped branded trays|5|5/35|41198034897 /94a641f8|241 JVM PASS;260/261 full,126/127 focused: old Aa-name assertion failed on all4; all4 PDFs PASS|
|2: measured label fit, scope, persistent ribbon and same-page metadata trays|5|10/35|41199283231 /79cb44cd|241 JVM PASS;264/265 API26/API36,131/131 dark200%,129/131 landscape; all4 PDFs PASS|
|3: fit palette/Insert, anchored folder chooser and safe Create/Cancel/Move/Undo|5|15/35|41200568121 /7166d341|build failed: missing Gravity qualification;1 executed, native skipped|
|4: fully qualify Android Gravity; same266/132 native inventory|5|20/35|41200715192 /479ee9da|241 JVM;132/132 both focused profiles;265/266 API36;264/266 API26|
|5: temporary-panel release, editor dialog identity and correctly scoped provider test|5|25/35|41202239736 /783726ba|241 JVM;132/132 dark;131/132 land;265/266 both full profiles|
|6: target the actual async folder tray and await native accessibility cache updates|5|30/35|41203529856 /58238865|ALL5 PASS:241 JVM,266 API26,266 API36,132 dark,132 landscape; all4 PDF probes PASS|
|7: live Find tracks editable writing and cleans up observers|5|35/35|launching|596 root/206 Notes + Java syntax PASS;268/134 native pending|

0/35 unreserved. No other run authorized/reserved in this ledger.
Original4 profiles retained: API26 full, Android16/API36 full, dark200% portrait focused,
landscape100% focused. Exact inventory must include all existing cases plus new native Find cases.
Every source push is gated by root unittest, Notes unittest and diff checks. No tests.yml changes.

## Acceptance, not just automation
Inspect actual Notes writing/Find/Format/Insert and Doc writing/Format/content-menu captures.
Exercise direct Find, next/previous, return-to-writing, Back, no data/mark changes, fonts/selection,
large text/IME, contextual action scope, reopen/Undo and PDF/backup safety. Do not remove a failing
assertion to make the redesign pass. Native helper changes must follow real visible user controls.
Physical novice/Motorola testing, OEM behavior and real reminder delivery remain unperformed here.

Session began approximately11:34 UTC2026-10-31 from tool clock. Requested180 minutes is an effort
target, not a verified uninterrupted-work claim. Waiting on CI is not invented active work.

Checkpoint1 expected inventory:241 JVM,261 full native,127 focused; no inventory shrink.

## Reference follow-up and staged second pass (not yet another job reservation)
Official product help reviewed, not just framework examples:
- Keep Android: https://support.google.com/keep/answer/2888246?hl=en&co=GENIE.Platform%3DAndroid
  Select words, Format, then B/I/U; heading removal operates on paragraphs. Keep Notes simple.
- Docs Android Find: https://support.google.com/docs/answer/62754?hl=en&co=GENIE.Platform%3DAndroid
  Query/Search, top-right arrows through occurrences, Done returns to document. We implement Find,
  not its additional Replace feature, and add a direct search shortcut without removing More fallback.
- Docs Android formatting: https://support.google.com/docs/answer/1663349?hl=en&co=GENIE.Platform%3DAndroid
  Select text using native handles, Format, then text/font/size choices. Do not replace native selection.
- Word Android ribbon: https://support.microsoft.com/en-us/office/show-or-hide-the-ribbon-in-word-for-mobile-devices-b8a5b4c3-a187-45b7-a945-718e0524bfe1
  Show/hide editing tools to preserve reading/writing space; do not transplant desktop ribbon density.
Microsoft's forum reports of Android16 toolbar occlusion are not authoritative product guidance;
we do not copy a reported broken layout or recommend their workaround of inserting unwanted text.

Staged improvements: combine B/I/U/heading/size toggles in one persistent docked ribbon with live
scope and state; contextual content Edit/Rename first, Remove last; whole-Note conversion under
Insert/List, not note management; compact Find count beside query during a short IME viewport;
read-only match preview scrolls without stealing query focus; explicit controller-close cleanup.
Colour, Labels and Background become bottom trays over the same editor, not page replacements.
Labels Save/Cancel and palette Cancel are fixed outside scrolling content. Native tests must prove
Cancel retains the very same writing View/cursor and data. Native keyboard Find must retain a real
writing line and a full48dp Close target. One old Aa visibility assertion must follow the new Format
control name, retaining its actual-visible-control assertion rather than deleting it.

## Checkpoint1 actual visual review
Evidence bot0ca8a48d imported; complete4 profiles,4 PDFs PASS. Only native failure is the
assertNotNull(text("Aa")) in the keyboard journey; change to Text formatting, retain its visibility
and native-line/caret assertions. No app failure is waived as a label mismatch.
Opened actual API36 Find, selected-word result and Doc selected-text ribbon, plus dark200% Notes
keyboard. Find stays in-page; ribbon is docked, but old spacing and word labels waste room.
Critically, the200% footer splits Format/Colour/Labels mid-word. Source2 measures real scaled
label widths, reflows Notes into writing-versus-note tool rows only where needed, and forbids
multiline/ellipsized tool labels in a native regression. No text-scale reduction. On wide landscape
it stays one row. Native actual keyboard Find, same-writing-view property Cancel and two style
choices in one open ribbon are added; expected265 full/131 focused,241 JVM.
3 necessary synthetic scenes added (Doc Find, keyboard Find, contextual options); bounded cap77.

## Third candidate corrections
Move to folder was still a full-page editor replacement. Its editor/list selection entry now
uses a bounded bottom tray with fixed Cancel, off-thread folder reads, explicit folder choice,
and a scoped Create folder dialog. No note moves on Cancel or on Create alone. A created folder
is an explicit filesystem operation, not undone by cancelling the subsequent move. Late reads
and creation completions cannot reopen a closed/stale panel. Storage errors keep the writing
page and offer retry; they never replace the selected storage root. The Library Folders page
and its storage/recovery navigation are unchanged. Native coverage checks Notes and Doc view
identity and selection on Cancel, plus exact rich-content fingerprint and folder after save.
One additional synthetic folder-context capture raises bounded scene cap to78.

Source2 evidence bot67f49e32 imported. Opened dark200% editor-keyboard and keyboard Find,
API36 contextual checklist menu and selected-text ribbon. Whole labels and retained writer now
look coherent;131/131 dark200% checks passed. Exact remaining failures: normal portrait API26
and API36 addMenuExplainsVisualContentCaptureAndScheduleCategories Reminder visible-height
156px vs4px/56px; landscape canonicalReverseSelectionSurvivesColourAndControllerRecreation
and nativeCaretSurvivesColourPageAndControllerRecreation palette Blue312px target exceeds293px
scroll viewport. These are actual reachability defects, not stale-label assertions. Preserve the
full Reminder height assertion and actual-tap unclipped assertions. Fix palette to72dp minimum
cells/32dp swatches/unchanged13sp text and4dp padding; regroup conversion and Reminder together
under Lists & reminders instead of adding a tall descriptive List section. All existing actions
remain. Native folder case also exercises Create-without-moving, Cancel, explicit Move and Undo
with exact rich-content fingerprints. Creating a draft label then Cancel must not persist it.
Source3 inventory266 full/132 focused;241 JVM. No physical/novice or180-minute acceptance claim.

Third run build124535378981 failed at NotesController871: unimported Gravity in the new
Create folder dialog placement. Java-parser only checked grammar, not Android symbol resolution.
Correct to android.view.Gravity.BOTTOM, consistent with this controller. No native job executed;
keep its5-slot reservation conservatively counted (1 actual execution, remaining native skipped).
After reservation4 there are20/35 conservatively reserved,15 unreserved; actual executed before
run4 is11. No root/Notes red gate was pushed; local gates do not claim Android compilation.

## Fourth results and final corrective candidate
Evidence botbb59d725 imported.132/132 dark200% and132/132 landscape passed, including actual
Blue colour taps and Create/Cancel/Move/Undo with exact rich fingerprints. API36 only failure
RealFolderStorageTest.folderBrowserCreationAndMoveFlowWritesRealVisibleFiles; API26 same plus
CoreJourneysTest.selectedTrashDeletionLeavesOtherTrashedNotesAndOriginalAudio (Library not shown,
window_focus=false, its frame-dependent failure screenshot also timed out). Do not waive either.
RealFolderStorageTest's name lookup can select the still-visible, scrim-covered **Archive action**
in the underlying selection bar while the async destination list is loading. Old full-page picker
removed that bar. Make its lookup respect NO_HIDE_DESCENDANTS modal scope; keep real-provider
source-empty/destination-one-file/body/folder assertions intact, improve their failure messages.
The isolated Core failure's cause is unproven: require attached/focused live activity at fixture
readiness (stronger precondition, not auto-dismiss or retry), preserve all native-tap checks, and
capture faults directly from UiAutomation even if the fixture no longer posts frames. Add bounded
control-failure capture for diagnosis; require the case to pass again, never exclude it.

A lifecycle review also found dynamically recreated property trays stayed parked with cancelled
inputs and fixed footers until leaving the note. Null-launcher panels are now one-shot: close
unregisters panel and footer; opening another panel closes/releases the previous one. Button-owned
cached tool menus remain reusable. Native Cancel coverage compares the entire retained view count
before/after Colour, edited-but-cancelled Labels and Folder, alongside exact writer/cursor/data.
Create folder now uses the existing shared editor-sheet helper (width, shape, dim and IME mode),
not a one-off gravity placement. Add its synthetic capture. Doc colour/folder scope says document,
not note. Normal corpus79 + one conditional fault image gives bounded cap80. Data/domain and
Calculator diff against delivered61b10ab0 is empty. No180-minute or novice acceptance claim.
Reservation5 means25/35 conservatively reserved; after5 actual jobs,21 executed/4 skipped slots
retained in reservations.10 slots remain unreserved. Never count a skipped job as an execution.

## Fifth result; no case excluded
Evidence botaec4ac4c imported. All release/view-count, same-view Cancel, draft-label Cancel,
Create-without-moving, rich Move/Undo and the API26 selected-Trash case passed. Dark132/132.
Both full profiles only failed the real folder UI flow: a hidden **parked Library menu** also
contains Archive. Ignoring the scrim-covered main view alone still allows its hidden fallback.
Use an explicit active Move-to-folder panel locator for async waiting and clicking, assert that
the destination is shown and belongs to that panel. Preserve every real-file/data assertion.
Landscape sole failure: aaPanelDistinguishesSelectionFromTypingAndKeepsRichText's immediate
accessibility-tree New typing assertion at535. Add a stronger synchronous assertion on the
actual aa-scope TextView and then a bounded awaitDialogText before the unchanged native tree
assertion. If the real label does not update, it must fail; no production refresh or automatic
selection rewrite is introduced merely to make a test pass. Root595/Notes205 remain PASS.
Reservation6:30/35 conservative slots,5 unreserved;26 actual jobs if all5 execute. No production
change since783726ba in this sixth source; test corrections still require full Android gates.

## Sixth result and final live-writing audit
Evidence botf56e0c91 imported. All5 jobs green:241 JVM,266/266 API26 and API36,132/132 dark200%
and landscape. All4 pypdf profiles ok. Provider source/destination/body assertions passed, as did
the synchronous real-ribbon label and native accessibility label checks. Qualified intermediate
APK source58238865/artifact13043421221,66,706,580 bytes, metadata SHA256
25cd0185c062120a1e7a2c63688d8f1867637a9d1f6c718ee93a983aeb3678da; not delivered as final.

Final audit: because Find is now in-page, writing can be edited without leaving it. Its initial
snapshot was static: counts/new queries could stay stale, and tapping a stale preview closed Find
before jump() could refresh it. This did not rewrite note text, but is not a coherent live editor.
Observe writer fields while Find is attached, debounce120ms, mark cached text dirty, refresh only
changed snapshots, cancel old work/generations, and remove observers/callbacks on close. Content
refresh updates matches without scrolling away from the writing caret; query/navigation still
reveals the chosen match. A stale preview refreshes rather than closing prematurely. Keep literal
256-query/100-match bounds and no persisted highlight marks. Doc's preview says Writing, and its
initial scope says document, not note. Add2 native cases: mutate Notes/Doc writing while Find is
open, expect2 live matches, retain caret, navigate exact second range, close, continue editing and
verify exact text plus unchanged original bold mark/no new marks on the found words.
Inventory241 JVM/268 full/134 focused. Normal/compact counters represent the same count; the new
checks accept2 matches or1/2, then verify the actual7..13 selected range. No case removed.
Reservation7 consumes final planned5 slots:35/35 conservatively reserved,31 actual executions if
all5 run,4 skipped slots retained in the conservative ledger; no unreserved planned slots remain.
No180-minute, novice, physical-device or public-release approval claim.
