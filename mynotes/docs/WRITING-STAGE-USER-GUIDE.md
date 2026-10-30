# Aa and named checklist boxes — scoped code21 guide

**Implemented and native-tested. The final workflow has an unresolved follow-on failure/evidence gap; see the development handoff. This is not final physical-device usability or public-release acceptance.**

## Which editor changes?

These changes apply to the existing **document editor**. The title keeps its existing title style; Aa formats writing, checklist rows and table-cell text. Previously enabled document notes get them on opening; the app does not automatically convert older simple notes. The older editor and its image/drawing/table/storage workflows are not replaced by the broad redesign proposal.

If you deliberately want the document features for an old note, use its existing **Enable document editor** action, review the explanation and back up important notes first. That existing operation verifies the conversion and retains a private first-upgrade recovery snapshot. New-note defaults follow the app's existing opt-in setting.

## Write first

Open your document note and tap the writing area. The permanent Text header/handle row has been removed from ordinary writing; your title and text remain editable. Existing Insert, Labels, Colour, Writing lines and Document actions remain in their previous toolbar/destinations; only Format is replaced by **Aa**.

If you need the existing whole-text-section actions, select text and use Android's selection toolbar → **Text section options** (possibly under its overflow) → **Show move handle**. Regular Cut/Copy/Paste and selection remain native. Nothing is automatically merged or flattened to produce the cleaner appearance.

## Aa: style without a long list

1. Select words, or place your cursor where you will type.
2. Tap **Aa**.
3. Read the scope at the top:
   - **Selected text:** styles change the selected words. Headings apply to the paragraphs they touch.
   - **New typing:** styles apply to what you type next; earlier text remains unchanged.
4. Choose Bold, Italic, Underline, Strike, Normal paragraph, Heading1/2, Font or Size. Each applied choice returns you to writing. Reopen Aa for another choice.

The selected styling controls have a highlighted state. An unhighlighted control can mean off or mixed formatting across your selection. Font offers Sans serif, Serif, Monospace and Cursive. Size accepts8–72sp; the minus/plus controls adjust the base size by one within those bounds. For mixed selections, the displayed fallback is not a promise that every character has the same size/font. The panel scrolls on small screens/large system text. At large text sizes its style controls use two columns and size steppers have a separate row to avoid clipping.

**More text options** retains the existing hyperlinks, numbering, clear formatting and saved-style operations. Links still require selected words and a safe HTTP/HTTPS address. Saved styles remain local and text-free. Tapping another writing position follows its formatting under the existing typing-style behaviour.

## Make a checklist from selected writing

1. Write your note normally.
2. Select the exact lines/words you want in a checklist.
3. Tap the existing **Selection → checklist** toolbar control, or the native selected-text action **Make checklist**.

Only that selection converts. Each selected newline creates a checklist row. Surrounding writing and formatting stay in place. The list appears in a bordered box with a default name such as **Checklist01**. Existing lists without saved names show **Checklist** until renamed.

- Tap the box name or corner grip → **Rename** to give it a useful name.
- **+ List item** adds a row; individual row grips still reorder rows inside that box.
- The **box's** grip moves the whole checklist, not one item.
- Tapping or holding the box grip opens its options. Drag by moving your finger from the grip rather than holding still.
- **Remove checklist** asks for confirmation and is undoable. Removing it does not remove surrounding writing.

## Move a checklist between writing lines

**Drag:** drag its corner grip into a writing area. A coloured insertion line shows where it will be placed before you release. Holding near the page edge scrolls. Drop before the required line; dropping below the final line places it at that writing area's end. Existing whole-section boundary drops also work.

**Without dragging:** tap its name/grip → **Move checklist**. A banner explains placement. Tap the writing line you want the checklist to appear before, or choose **Cancel move**. To place at the text's end, tap below its final line where there is room; otherwise use a following writing area or drag to the boundary.

The destination is saved as a text position. Soft-wrapped screen lines can change with font size, screen width and orientation; a drop inside a paragraph splits the rich text at the indicated line start without erasing its words or formatting. Undo reverses the move as one operation. Names, row contents, checked states and indentation stay with the box.

## Saving and recovery

Names are included in the note's saved document, portable MyNotes backup, merged notes and text/PDF exports. They participate in note history and survive reopening when saved. The existing history limits, safe recovery behaviour and backup exclusions remain; no unlimited-history promise is added.

Back up important notes/recordings and install a newer qualified APK over the existing app. **Do not uninstall or clear data. Do not downgrade after using named boxes:** older APKs reject these previously unsupported checklist names rather than silently stripping them.

## Not included in this stage

No new table-header workflow, new image/drawing/diagram editor, unified + redesign, floating Word layout, arbitrary Word import or broad whole-note toolbar replacement. Physical Motorola/Android16 acceptance and public-release approval are separate from automated qualification.
