# User map and refinement handoff — 29 October 2026

Main deliverable: [`MYNOTES-USER-MAP-AND-REFINEMENT.pdf`](../../../MYNOTES-USER-MAP-AND-REFINEMENT.pdf), 37 bookmarked landscape slides.

The PDF distinguishes current published 0.12 functionality from proposed refinements. It is not runtime proof or a release approval. The SVG frames in this folder can be imported into Figma; they are editable vector handoff assets, **not** a hosted Figma document. Rebuild with `python tools/build_notes_user_walkthrough.py` (PyMuPDF). Raw device diagnostics and personal screenshots are deliberately not embedded or committed.

## Requested 180-minute work session

Start: 2026-10-29 06:30 UTC / 12:00 IST. Target interval ends 09:30 UTC / 15:00 IST. Elapsed time must be reported honestly; this timestamp is not a claim that 180 minutes have elapsed.

- [x] Review supplied screenshots and begin diagnostics triage.
- [x] Deliver detailed slide-style PDF, app glossary, walkthrough and proposed vector layouts.
## Working implementation and evidence — 07:05 UTC checkpoint

Implemented on this working branch, **not published as a replacement APK**:
- Adaptive drawing paper and uniform legacy-image scaling; full available drawing viewport.
- Icon-and-label tool strip, tinted/bordered selected state and a separate compact control dock.
- Accessible top Save immediately before More; grid, PNG clipboard, help, clear and scoped deletion.
- Typed Drawing/Image cards with compact icons; Read image naming.
- Fixed-opposite-corner diagram resize handles, preview/CANCEL/one-commit semantics.
- Grouped Add sheet (Visual content / Capture text / Schedule this note), properties near the title.
- Signature appearance/choice sheets, rounded themed native dialogs and settings-route restoration.

Qualification is distinct from implementation:
- Run **40831654435**, source **8567768**: build/lint and all **148 JVM** methods passed.
- Android 14 / 200% font / dark: all **48 selected native** methods passed.
- Android 8 / normal font / light: **47/48** passed; clipboard fixture created its service on a thread without a Looper. The actual app publishes on the main thread. The fixture is corrected locally; rerun pending.
- Newer grouping, dialog and selection polish **postdates that run**. Three additional native UI cases are authored, not yet passed.
- Artifact URLs failed in this environment. A bounded fixture-only JPEG annotation bridge is being added for real rendered review; original PNGs remain normal artifacts.
- [ ] Newer-source build/lint and exact native rerun.
- [ ] Actual rendered review in both profiles; wider landscape/tablet coverage.
- [ ] Broader regression qualification and candidate APK.
- [ ] Motorola Android 16 physical-device acceptance.

The PDF remains a walkthrough/specification, not runtime proof. See this timestamped checkpoint for implementation status rather than interpreting its proposed layouts as shipped UI.

Audio archive development is paused for this feedback. Its earlier qualification is not evidence for this redesign. Published 0.12 remains immutable.

## Second qualification / source 5af264c

Run **40833264747** compiled debug app/tests, lint and all 148 JVM methods successfully. It executed all 51 selected native methods per profile, with **49 passing on API26** and **50 on API34**. The three new grouping/tool/appearance-route methods passed. The clipboard Looper fixture fix passed.

Remaining failures are not waived: closing a sheet left its active panel VISIBLE inside a hidden overlay, so dispatch-first test disclosure could not reopen Merge. Close now parks the panel GONE, with its launcher retained. API26 also rejected one input injection; the tap helper now captures the active window package and a real screenshot rather than silently retrying.

The annotation image relay was experimentally unusable: GitHub truncates messages at 4 KiB and limits annotations per step. The replacement writes bounded synthetic JPEG fixtures and provenance for a same-branch documentation relay; no private screenshots or release APKs are involved.

Further local changes awaiting the next native pass: explicit disabled Undo/Redo states, current ink swatch and selected colour, readable checklist commands, semantic menu icons, bounded extreme-aspect drawing migration, and native shape/picker/mixed-content review cases.

## Rendered findings addressed after inspecting run 40833264747

The actual normal-font Add screenshot hid Reminder below the fold. It now uses adaptive visual/capture tiles instead of six tall rows; the title is shortened and the normal-font portrait test requires the full Reminder target to be visible. Large text retains scrolling and word-sized columns.

Library is restored to the lower dock (with a top fallback in Trash, where creation controls are hidden). Its swipe threshold is the requested left 20%, and its sheet measures content rather than forcing a half-screen clipping window.

Diagram uses a single compact Add / Undo / Redo / Edit / More icon dock; Group shapes and Edit arrows live in More rather than beneath the canvas. Trash diagrams are view-only. Labels/OCR/merge receive a visually primary confirmation; host palette pairs remain authoritative. Voice cards omit seconds and bound transcript previews to four lines so playback controls remain findable. These changes are still awaiting native qualification.

### 08:07 UTC refinement checkpoint — not release approval

Run [40837054318](https://github.com/tarungittarun/TicTic/actions/runs/40837054318), source `1e79302`, compiled debug/test and signed release APKs, passed 148 JVM cases and both release/debug lint. Native exact inventories: API26 **182/182**, API34 light **182/182**, API34 dark **59/59**, API34 dark/200% font **59/59**. Landscape **58/59** failed reverse-selection restoration; tablet **28/59** is unqualified: its real configuration stayed landscape and the actual screenshots show a **Pixel Launcher ANR blocking app focus**. Retained previews/reports are evidence inputs, never approval. The full current-source drawing, primary labels, compact diagram dock and adaptive Add now have rendered evidence on the four passing phone profiles. Additional large-screen work is pending.

Submitted next: one-shot post-layout restoration of saved native anchor/focus (not continuous selection forcing); selection saved even while a property page has temporarily removed the body view; compact intrinsic Filter & view pages and primary Apply; consistent primary/secondary native-dialog buttons. IME fullscreen prevention already existed in `NotesDesign.entry`; it is **not** being claimed as the new landscape fix. Added a palette-stage regression probe to distinguish selection capture from restoration.

The next environment setup computes rotation from the emulator's **natural physical aspect**, preserves pre/post window and last-ANR diagnostics, and stops only the external Pixel Launcher on the tablet after retaining diagnostics. It does not hide app crash/ANR dialogs. Also adds Android 16, dark, 0.85-font coverage; this is not a Motorola hardware qualification. Those changes are unqualified until their new run completes. Archive work stays paused; 0.12/code13 remains the public APK.

The first follow-up build (`40839866334`) caught a test-fixture compile error: `NotesAppearance.dark` requires the host argument. Production runtime compiled, but no native jobs ran. Corrected the fixture call; this is not a passing qualification.

### 08:26 UTC diagnosis, before the next qualification

Run `40840191909` / source `ae8aee4`: 148 JVM/build/debug+release lint passed. Native results are **not green**: API26 182/184, API34 light 183/184, Android16 dark/0.85 font 183/184, dark 60/61, 200% font 60/61, landscape 57/61, tablet 59/61. All seven encountered a new regression test's incorrect launcher name (`Filter & view` instead of the real `Filters`). The test also mistakenly called the editor-only `laidOut()` helper on a filter sheet; both fixture errors are corrected, not waived.

The tablet's real orientation now passes and retained pre/post diagnostics contain no ANR; foreground clipboard also passes. Its remaining swipe is being instrumented at DOWN/candidate and MOVE/claimed, with actual measured-view readiness. The landscape probe proves anchor/focus changed **before property capture**; the speculative one-shot post-layout restoration is removed. Keyboard/window readiness is now an explicit precondition before synthetic reverse-selection setup. Hidden landscape header controls are reached by a real Android Back to dismiss the IME, not by making views visible. Native input checks are sampled after accessibility idle and rechecked against the current visible window immediately before dispatch; no injected tap retry or permission bypass is added. API26's recurring injection failure remains unqualified until this check passes.

Further rendered review caught an orphan AM/PM in a reminder chip: its date and time now occupy deliberate separate lines, with the complete schedule exposed as an accessibility state. Added synthetic Folders onboarding, Diagnostics and Group shapes dialog captures, raising the bounded preview allowance from 40 to 48 to retain failure screenshots too. These new changes still need fresh Android execution and visual review. Public APK remains 0.12/code13; the archive UI remains paused.

Follow-up `40842177367` stopped at test compilation: the app test module does not export the runtime module’s AndroidX Core implementation dependency. The test now reads the native API30+ accessibility state, while runtime keeps its existing Core backport for older Android. No native methods ran in that build. Native picker headers now inherit the app surface/ink rather than the stock grey block; initial Folders recovery has a clearly primary Choose main folder. These are pending qualification, not claimed passing.

### 08:35 UTC build boundary and pipeline correction

`40842602541` stopped at Android resource linking: `headerTextColor` is a private framework attribute. Removed the speculative native-picker header override entirely rather than accessing private IDs or leaving light headers unreadable. Signature dialog containers and primary/secondary buttons remain; calendar/clock internals stay native. No native jobs ran for that source.

Source pushes also unintentionally started the full publishing workflow in parallel with refinement. The integration cannot cancel runs directly (403). The scoped `40842884241` Actions cleanup succeeded, stopping overlapping full attempts; the publishing trigger now requires a deliberate change to its workflow/setup, with **all publishing gates unchanged**. Refinement pushes no longer start duplicate 21-lane publishing farms. No calculator workflow was requested and no new public APK was published. This overlap is recorded as wasted CI work, not extra qualification.
