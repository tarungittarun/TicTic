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
|1: inline Find, named vector tools, docked Doc formatting and scoped branded trays|5|5/35|not yet launched|584 root/194 Notes PASS; Android pending|

30/35 unreserved. No other run authorized/reserved in this ledger.
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
