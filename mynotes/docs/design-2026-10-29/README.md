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
