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
|3: fit palette/Insert, anchored folder chooser and safe Create/Cancel/Move/Undo|5|15/35|launching|593 root/203 Notes + Java syntax PASS; Android pending|

20/35 unreserved. No other run authorized/reserved in this ledger.
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
