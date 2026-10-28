# Paper & Ink — MyNotes 0.9 UX review

**Candidate 0.9.0 (10): all 21 Android lanes passed; rendered review approved for gated publication. Not yet published.** The user rejected 0.8's UI despite green functional CI. This redesign must earn acceptance from practical use and rendered evidence, not totals.

Requested refinement window: **28 October 2026, 00:03:08–03:03:08 UTC** (05:33:08–08:33:08 IST). Final qualification is a separate release gate; reaching the end of the window does not authorize bypassing it.

## Design and first-use decisions

Borrow interaction principles, not a brand skin: Keep's quick capture and quiet cards, contextual tools in leading note editors, and Word-style paragraph formatting. No sign-in, cloud service or feature expansion is being used to disguise the UX problem.

- **Content first:** optional title, direct body editing, native selection and caret. No persistent column of command buttons or routine save-status chatter.
- **One signature:** warm paper / charcoal ink, restrained honey accents, outline icons, shared typography and spacing across cards, editor, diagrams, Library, Settings, storage, colour and labels.
- **Contextual controls:** Back / Pin / Archive / Undo above the editor; Capture / Formatting / Organise / More below. Secondary commands live in dismissible sheets. The formatting deck replaces the normal dock.
- **Readable states:** labelled colour swatches and selected outline; actual Undo availability; no empty checklist progress counter; plain-language storage and capture messages.
- **Adaptive layout:** centred 720dp maximum writing column, 640dp formatting deck, 560dp sheets, adaptive card columns and scrolling constrained pages. Large text wraps. Recording keeps Stop separate from its scrolling explanation.
- **Native editing:** no fullscreen/extract IME in landscape. Metadata changes and recreation retain selection. Compact keyboard layouts collapse unnecessary chrome rather than sacrificing the writing/search viewport.

### First-use route

1. Home → **New note** (accessible name: New text note), or Checklist. Write directly; title is optional and there is no Save button to discover.
2. **A / Formatting** → Text / H1 / H2 and inline styles. A heading applies to the selected paragraph, not its neighbours.
3. **Plus / Capture** → Diagram, Export PDF, Scan image, Record voice, Voice inbox. Permissions are contextual; denial leaves ordinary notes usable.
4. **Organise** → Colour, Labels, Reminder. **More** → Share, Copy, Trash, type-specific actions and dates.
5. **Library → Settings** → Storage & recovery and diagnostics. Diagnostics are **off by default**; enable before reproduction, export deliberately, delete locally.

Cleaner presentation does not remove existing capture, offline OCR/ASR, labels, colours, reminders, diagrams, sharing, conversion, local storage/recovery, widget, Archive, seven-day Trash or Undo. Calculator and MySpace remain frozen.

## Heading overlap: implementation and practical evidence

This is not fixed whitespace added between sample lines. H1/H2 normalize to complete paragraphs, use native metric-affecting spans and deliberate line-height allocation, and remeasure after formatting. The input watcher updates the existing Editable through composition instead of replacing the buffer. Enter ends a heading and starts ordinary text.

Checks include `01`–`09` with H1/H2 on `01`, paragraphs before/after, wrapped and mixed-script text, caret and reversed selection, clear/Undo/reopen, composing a word then Enter, and selection after colour/recreation. Native line boxes and glyph raster bounds are checked separately. Numeric screenshots precede the temporary diagnostic glyph-colour mutation.

**Latest completed evidence:** [run 40622598745](https://github.com/tarungittarun/TicTic/actions/runs/40622598745), production source `d5cf717327883798bb150562bfefb5a9957e7342`, [provenance and captures](ux-review/40622598745/).

| Review profile | Android methods | Actual review outcome |
| --- | ---: | --- |
| Light / API34 | 118/118 | Quiet cards, editor and contextual tools; numeric heading lines visibly separated. |
| Dark / API34 | 118/118 | Readable ink/surfaces and labelled palette; consistent editor/settings styling. |
| 200% font / API34 | 118/118 | Heading/body separation readable; **colour names break mid-word** in the three-column palette. Not approved. |
| Landscape / API34 | 118/118 | Actual keyboard leaves readable editor lines; actual search result now visible above the keyboard. |
| Tablet / API34 | 116/118 | Centred editor and formatting visible, but two native tap journeys fail to reveal Heading 1 / Colour. Not approved. |

All other Android lanes in that run succeeded: **20/21 lanes passed**, not a full pass. Lint, domain, source and APK audits also passed. Publication was skipped. The remaining failures are not dismissed as environmental.

## What visual inspection changed after green counts

1. Run40614442937 passed all 21 lanes, but its actual landscape keyboard capture left no useful note body. It was not accepted.
2. Removing fullscreen IME and hiding only the top bar was insufficient: later instrumentation measured **5 visible body pixels versus a 77px native line**. Caret-only scrolling did not fix the measured layout.
3. Editor title/dock adaptation restored readable lines. Run40621098598 then exposed a separate search defect: hidden header/dock retained their old measured blank space, leaving no result viewport.
4. Source `d5cf717` posts a changed-only forced root remeasurement after traversal. Run40622598745 and its actual search image now show the result title and body above the landscape keyboard.
5. A separate API29 save/Undo race was corrected by reporting Saved only for the actual persisted revision. API29 passed in the last two completed qualifications.
6. Tablet failures exposed a harness readiness problem worth testing: `tap()` asserted visibility before waiting for UI idle and did not require stable target geometry. Source `de1d671` adds bounded visibility and stable, fully visible geometry waits; it still injects actual down/up pointer events and retains clipping and minimum-target assertions. **This is a proposed synchronization correction, not a confirmed diagnosis or a passed tablet result.**
7. Inspecting the actual 200% palette found split names despite 118/118. Source `934c40980ff264d1c030f29d4efdea79b789a631` uses two columns at large system text and adds a native assertion against splitting single-word colour names. **New screenshots and Android results are required before calling it fixed.**

## Current qualification and deliverables

[Run 40623972980](https://github.com/tarungittarun/TicTic/actions/runs/40623972980) qualifies source `934c40980ff264d1c030f29d4efdea79b789a631`, including both pending corrections. It supersedes run40623790926, which was automatically cancelled by the workflow's newer-source concurrency policy; that cancelled run is not qualification evidence.

Local gates before the latest production push: **505 root checks + 115 Notes checks passed**, with a clean whitespace diff. Android builds run only in GitHub Actions, not locally.

[`../../NOTES-UI-OVERVIEW.pdf`](../../NOTES-UI-OVERVIEW.pdf) is now a **16-page actual-runtime review**, replacing the old concept-only overview. It pins run40622598745/source d5cf717 on every page and is explicitly **Pending review / not release approval**. It does not depict or qualify the later large-font palette correction. Screenshots are synthetic-fixture emulator captures; the flowchart page is explicitly a schematic guide to the six implemented shape types. The historical concept generator now writes `NOTES-UI-CONCEPT-HISTORY.pdf` so it cannot overwrite the runtime overview.

`ux-review/approval.json` remains fail-closed. No new APK download is represented as accepted or published.

## Completed long component measurements

Both 70-minute runs completed successfully. They exercise an attached debug controller's paragraph reflow/raster, native English OCR/ASR, PCM recovery, atomic private repository and backup. They are **not** 70 minutes of human gestures or exact-final-UI navigation.

| Run / source | API | Elapsed ms | Cycles | Warm / peak PSS KiB |
| --- | ---: | ---: | ---: | ---: |
| 40610708876 / 9114bec | 26 | 4,200,310 | 3,137 | 205,664 / 217,561 |
| 40610708876 / 9114bec | 35 | 4,201,116 | 2,082 | 87,504 / 137,861 |
| 40614747195 / 15ae085 | 26 | 4,201,313 | 2,167 | 197,906 / 208,263 |
| 40614747195 / 15ae085 | 35 | 4,200,905 | 1,903 | 87,590 / 136,246 |

Original WAV remained unchanged: SHA-256 `dcfea5712c43a43ba7ae8083afb39d36993e5a69c46e88b68aaa72b65cb615bb`. These measurements are not a leak-free, frame-smoothness, battery, thermal or acoustic-accuracy claim.

## Release acceptance — still pending

- [ ] Newest-source light, dark, 200% font, landscape and tablet captures inspected, including the corrected palette.
- [ ] Numeric paragraph, native typing/composition, caret and colour/recreation regressions pass on all required profiles.
- [ ] Primary actions and content remain visible/understandable in the reviewed constrained scenes.
- [ ] Full Android matrix, lint, APK identity/privacy/update audits and source gates pass.
- [ ] PDF/report regenerated from the accepted evidence, with honest scope boundaries.
- [ ] Approval pins equivalent production inputs and the actual reviewed run before immutable publication.

Physical OEM/SAF/SD providers, TalkBack, microphone hardware, accent/noise, battery/thermal, ARM and 16KiB-device execution remain unqualified. Emulator screenshots and component soaks do not certify these. User/device experience remains decisive; “world class” is a goal, not a test-derived certification.

## Follow-up resolution and publication approval

Run [40623972980](https://github.com/tarungittarun/TicTic/actions/runs/40623972980), source `934c40980ff264d1c030f29d4efdea79b789a631`, completed with **all 21 Android lanes passing**, including **118/118 methods in each of the five full review profiles**. Its overall summary was intentionally red because approval was still withheld, not because an Android test failed.

The new captures were inspected: large-font palette names now stay whole in two columns; tablet native H1/H2 and colour/recreation journeys pass without replacing real pointer input with direct clicks. Light/dark home/editor, numeric heading reflow, checklist, Settings/recovery and contextual tools remain consistent. Landscape actual-keyboard editor and search have readable content. The two previously blocking findings are resolved within these tested emulator configurations.

Approval now pins this source and run, and the canonical 16-page PDF has been regenerated from its actual captures. A new full gated publication run is required; no failed lane is skipped or overridden. Earlier pending statements above are the historical investigation record. Device/user acceptance remains separate from this bounded release approval.
