# Paper & Ink — MyNotes 0.9 UX review

**Candidate 0.9.0 (10), not published.** The user rejected the 0.8 interface despite its green functional CI. That feedback supersedes the earlier “accepted” UX wording. This redesign must earn acceptance from practical use and rendered evidence, not test totals.

The requested refinement window is 28 October 2026, **00:03:08–03:03:08 UTC** (05:33:08–08:33:08 IST). This document is a live review record, not a claim that the window has finished.

## The design decision

Borrow interaction principles, not a brand skin: Keep's quick capture and quiet cards, contextual tools in leading note editors, and Word-style paragraph formatting. No sign-in, cloud service, or feature expansion is being introduced to disguise the UX problem.

- **Content first:** the editor is a writing surface, not a column of command buttons. An optional title leads into the body; save-status chatter is hidden in normal use. Heading, body and surrounding paragraphs occupy native text line boxes.
- **One signature:** warm paper / charcoal ink, restrained honey accents, consistent outline icons, shared typography and spacing. Home, editor, diagrams, Library, Settings, storage, colour and label pages share the same primitives.
- **Only the current task:** persistent editor chrome is Back, Pin, Archive and Undo above; Capture, Formatting, Organise and More below. Secondary commands appear in dismissible, scrollable sheets instead of permanent disclosure/button stacks. The formatting deck replaces the bottom dock while open.
- **Readable states:** 12 labelled colour swatches, selected-colour outline, checked-list state, actual Undo availability, plain-language capture/recovery messages. No “0 checked” counter on an untouched list.
- **Small and large screens:** centred 720dp maximum writing column, 640dp formatting deck, 560dp maximum sheet width; adaptive note-card columns; scrollable constrained pages. Large text wraps rather than being truncated into single-line actions. Recording keeps Stop outside its scrolling explanation.
- **Native editing:** selection handles, caret, composition and keyboard remain Android's controls. Metadata changes and recreation preserve the body selection. Text inputs request no fullscreen/extract IME: landscape must not replace the app with the keyboard's separate editor.

“Cleaner” does not mean removing capabilities. Existing capture, offline OCR/ASR, labels, colours, reminders, diagrams, sharing, conversion, local storage/recovery, widget, Archive, seven-day Trash and Undo remain accessible. Calculator and MySpace code stay frozen.

## First-use route

1. Home → **New note** (accessible name: New text note), or **Checklist**.
2. Write directly in the body; a title is optional. There is no Save button to discover.
3. **A / Formatting** opens Text / H1 / H2 and inline styles. Selecting any portion of a paragraph applies the heading to that whole paragraph, without consuming neighbouring paragraphs.
4. **Plus / Capture** offers Diagram, Export PDF, Scan image, Record voice and Voice inbox. Permissions are contextual; denial does not disable ordinary notes.
5. **Organise** contains colour, labels and reminders. **More** contains share, copy, Trash and type-specific actions, plus creation/edit dates.
6. **Library → Settings** contains storage/recovery and the diagnostic switch. Diagnostics are **off by default**; enable before reproduction, export deliberately, and delete locally when finished.

## The reported overlapping-heading bug

The fix is not extra fixed whitespace. H1/H2 are normalized to complete paragraph ranges, use native metric-affecting spans with deliberate line-height allocation, and explicitly remeasure after formatting. The typing watcher updates the same Editable during input composition; it does not replace the whole editing buffer on every keystroke. Enter ends the current heading and starts ordinary text.

Practical checks cover `01` through `09` with H1/H2 on the first line, preceding/following paragraphs, a caret and reversed selection, wrapped text, mixed scripts, clear/Undo/reopen, composing a word and then Enter, and colour/recreation selection retention. Native layouts and glyph rasters are checked separately from screenshot inspection. The raster probe temporarily colours test glyphs to identify their ink bounds; the persisted numeric screenshots are captured **before** that mutation.

## What the earlier rendered evidence actually found

Evidence directory [`ux-review/40612414633`](ux-review/40612414633/), source `349ebaacdbc02e7e02d445088717d4077d9c8ce2`, is **pre-final**, not approval of the current source.

- Light, dark and 200% font: all 115 discovered Android methods passed in each full review lane. Native numeric screenshots show a larger first heading followed by naturally separated normal lines; the 200% example shows wrapped heading text without colliding with the following paragraph.
- **Landscape was red: 106/115.** Looking at `editor-keyboard.png` revealed the actual fullscreen/extract IME covering the editor. The resulting native taps hit the keyboard's system-owned window. Changing coordinate helpers alone did not fix that UX. The candidate now requests `IME_FLAG_NO_EXTRACT_UI | IME_FLAG_NO_FULLSCREEN`, and opens formatting without the keyboard in a constrained viewport. Native EditorInfo flags are asserted as part of the composition case.
- **Tablet did not reach instrumentation.** UIAutomator returned a null root during the baseline-release taskbar/window transition; the harness aborted on the subsequent missing XML pull. The bounded selector readiness check now handles an absent tree and records window state on each failed lookup. Missing instrumentation is still a hard failure, never a pass.

Run [40613541005](https://github.com/tarungittarun/TicTic/actions/runs/40613541005) qualifies source `63f63c29968bfb8072b8b4132499bed9cb875bf3` including these changes. The full API34, landscape and 200% font lanes each passed all **116** methods, including selection/recreation. Landscape no longer fails on the fullscreen keyboard. Four other lanes failed **before emulator launch** on GitHub artifact-service `ECONNRESET`; their absence is not attributed to app UI. A separate API29 storage case hit its 16MiB Java heap while constructing and reading an 8MiB oversize fixture. The follow-up avoids allocating the entire source fixture, and converts backup memory exhaustion into a recoverable IOException instead of an uncaught app error.

The next qualification uses current Node24-native artifact actions, limits the download burst to eight concurrent lanes and retains artifact-service inventory on failure; it does not add emulator/test rerun loops. **Final five-variant review and full green matrix are pending.** Publication remains fail-closed in `ux-review/approval.json`.

## A second visual finding after the full matrix passed

Run [40614442937](https://github.com/tarungittarun/TicTic/actions/runs/40614442937), source `1f8be9b184e2014c78e28658e6e303234f4cf5ac`, passed **all 21 Android lanes**, including all 117 methods in each full resource variant. Its workflow remained red **only because manual rendered approval was pending**. Seventy provenance-pinned screenshots were exported.

Inspection still found a real problem: `landscape-api34/editor-keyboard.png` displayed top and bottom toolbars, but essentially no readable note body. Not using fullscreen IME was necessary, but not sufficient. The top editor chrome now collapses only below a 240dp available height, leaving the native writing viewport above the keyboard. A screenshot-stage assertion requires the actual body visible rectangle to contain at least one native line; it does not merely assert that toolbar buttons can be tapped. The formatting keyboard-dismiss threshold is also 240dp, so ordinary portrait editing does not dismiss it unnecessarily.

Run [40615854257](https://github.com/tarungittarun/TicTic/actions/runs/40615854257) qualifies this final viewport change, source `e4fb726b373958866919ee6342ce0ce9415ce0ba`. **Approval is still pending its actual captures.** The contrast-token checks now also verify 4.5:1 normal-text contrast for light/dark ink, hints and accents on shared surfaces, and for text on all 12 note-card colours. Numeric token tests are not TalkBack or physical-display qualification.

Two 70-minute component soaks are tracked separately: original redesigned text/layout source `9114bec` (run40610708876), and source `15ae085` (run40614747195). The latter has equivalent TextStyles/native exercise/storage/ML component inputs, but precedes the final toolbar viewport adjustment. Neither is described as 70 minutes of physical gestures or exact-final-UI navigation.

## Approval checklist — not automatically satisfied by green CI

- [ ] Final-source light, dark, 200% font, landscape and tablet images inspected.
- [ ] Numeric heading, surrounding paragraphs, actual typing/composition and caret regressions pass.
- [ ] First-use creation, contextual tools, colour/labels, checklist, diagram and recovery routes remain understandable.
- [ ] No hidden or clipped primary action in reviewed constrained scenes; actual landscape keyboard no longer owns the whole screen.
- [ ] Full Android matrix, lint, signed APK audit, upgrade seed preservation, source/domain/privacy gates pass.
- [ ] Report/PDF accurately separate rendered screenshots, schematic design, emulator tests and physical-device limitations.
- [ ] Approval pins equivalent production inputs and an actual reviewed run before immutable publication.

## Qualification boundary

These are synthetic fixtures in an isolated debug host, plus separate signed-release install/identity/update checks. Screenshots are real emulator captures, not marketing mockups or physical-phone proof. Component soak cycles are not human typing, TalkBack, OEM/SD-provider, accent/noise, microphone hardware, thermal, ARM or 16KiB-device qualification. User/device experience remains decisive; “world class” is a goal, not a test-derived certification.
