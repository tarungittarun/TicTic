# Novice-screen review — 29 October 2026

## Evidence boundary

This ledger is a visual/source review, **not release approval**. The rendered inputs are synthetic Android debug-host fixtures, not personal notes or the user's uploaded screenshots. All **37 original captured scene types** have now been examined across `40837054318` (source `1e79302`) and `40840191909` (source `ae8aee4`). This does **not** mean every image/profile was individually approved. The latter run is red, including a newly authored test's wrong launcher locator. Neither run establishes physical Motorola acceptance.

Source `33c97f9` completed seven-profile qualification in run `40843257072`: 775/796 checks, four passing profiles and three red. Its new Folders onboarding, Diagnostics, Group shapes dialog and changed reminder timestamp have now been visually examined in full-API34 renders. That is **40 main scene types** examined in total. Subsequent local-only edits are not Android-qualified.

## Scene coverage

| Area | Actual scene types examined | Novice assessment |
|---|---|---|
| Browse and navigate | home, library, search-keyboard | Lower Library entry is clear on Notes. Search retains a readable result above the IME. The Library sheet has distinct destination icons. |
| Filter and view | filters-view, filters-sort, filters-labels | Intrinsic height avoids View's former empty half-screen. Gold outlined tabs distinguish selection; primary Apply stays at the top. All three sort choices are visible at 200% text. Labels can scroll independently. |
| Write and format | editor, editor-keyboard, format, format-keyboard, numeric-h1, numeric-h2 | Title/body hierarchy is distinct; heading lines reflow without glyph collisions in the reviewed examples. Native selection remains visible. Large-font writing retains an actual body viewport above the keyboard. |
| Add and schedule | capture, add-reminder, mixed-note-header | Visual content / Capture text / Schedule categories separate unlike actions. Normal-font portrait shows the entire Reminder action. Existing reminder's AM/PM could become orphaned: changed source uses deliberate date/time lines and accessible schedule state; the fresh API34 render now shows the date and complete time on separate lines. |
| Ink workspace | drawing, drawing-tools, drawing-options | Paper fills the space between top/bottom controls. Pen/Pencil have real glyphs and obvious selected state. Save is immediately before More. Tools scroll rather than shrink at 200% text. Help is on demand; it is not persistent canvas clutter. |
| Visual content | image-editor, remove-image-confirmation | A Drawing card has an edit glyph; an imported Image does not pretend to be an editable drawing. Each has its own More/removal scope. Removal copy explicitly preserves the rest of the note and offers Undo. Its confirmation action now has a stronger primary treatment in local-only source; fresh native validation is pending. |
| Diagram | diagram, diagram-options, shape-dialog | Four visible corner handles replace width/height command buttons. Add/Undo/Redo/Edit/More occupy one compact dock. Edit shape has a primary Save. The page is fixed-width artwork, not a zoomable editor; small diagram label legibility remains a refinement opportunity. |
| Checklists and appearance | checklist, palette, backgrounds, background-editor, labels | Checklist rows are native editable text with separate completion targets. Colour names/swatches are explicit. Background previews show the pattern before choosing. Save labels is clearly primary; Cancel is secondary. |
| Voice and app services | voice-inbox, settings, settings-after-navigation, storage, about | Voice timestamps omit seconds; bounded previews keep playback controls visible. Settings separates appearance, files and help. Storage explains app-only versus Files-visible locations. About contains model/technical details rather than burdening the voice inbox. |
| Native time selection | reminder-date, reminder-time | Rounded containers and warm primary OK/Save distinguish positive actions. Calendar/clock internals remain native; they are not claimed as a wholly custom calendar. These picker fixtures do not prove permission or notification delivery. |
| Trash | trash, trash-confirmation | Scope and irreversible deletion warning are explicit, including separately retained backups/original audio. Found a real inconsistency: viewport adaptation makes the creation dock reappear in Trash despite its initial hidden state. Local-only source now constructs only the lower Library entry in Trash, with no creation actions; fresh native validation is pending. |

## Remaining review, not silently waived

- Current-source landscape anchor/focus preservation, API26 input dispatch and exact tablet leading-edge swipe must pass, not merely be explained away.
- Reviewed fresh Folders (explicit primary Choose main folder), Diagnostics (off by default, local-only scope) and Group/reminder fixtures. Fresh renders of subsequent local-only refinements are still pending.
- Local-only grouping now disables Group until two choices and dims unavailable Ungroup. Android execution/render review of that refinement remains pending.
- Read image's language selector, OCR review, backup/merge previews and provider-owned/system permission flows still need broader rendered/physical walkthroughs. Main-scene coverage is not a claim that every possible modal has been reviewed.
- Foreign-app PNG paste, Motorola rotation/IME behaviour, actual reminder delivery, physical folder operations and voice accuracy need device acceptance.
- Full publication qualification remains separate from these nonpublishing review runs. Public APK remains 0.12/code13. Archive UI/transactional restore work remains paused.

## Presentation handoff

The PDF contains the vocabulary, category map, practical journeys, acceptance boundaries and source-pinned runtime examples. SVGs are an importable Figma-style handoff, **not** a hosted Figma project. Native examples are explicitly labelled fixtures and must not be mistaken for a production-device screenshot or visual approval.
