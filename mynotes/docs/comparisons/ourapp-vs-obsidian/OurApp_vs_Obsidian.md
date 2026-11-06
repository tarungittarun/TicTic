# OurApp_vs_Obsidian

Manual expert inspection of retained real screenshots plus bounded source inspection; no new live device session

Phase 1: two modules only. No new live/physical test or measured speed ranking. Not a feature-count or E2E-pass comparison.

## N01 — The floating create action covers preview text
P1 | Visible defect in retained frame

**Task:** Read the last visible note preview, then create a note.

**Observed:** In M01, New note sits over words in the orange note card. This is a visible obstruction, not just an aesthetic preference.

**Comparison:** O01 keeps file-choice rows and its search input distinct. It is a different retrieval surface, not proof that every Obsidian overlay avoids content.

**Impact:** The same small region must support reading and tapping; confidence in the card preview is reduced.

**Direction:** Keep the requested floating New note button. Reserve a readable safe area or adapt its placement; do not restore the rejected bottom create strip.

**Manual acceptance:** Recreate the three-card state, then test long titles, last-row scroll, list/grid, keyboard and large text. No required preview line or row action should be covered at rest.

**Source:** NotesController.renderHome: floating createRow; board bottom padding.

**Limit:** Not newly reproduced on a device. This does not prove the card is impossible to open or scroll.

Evidence: M01, O01

## N02 — The search invitation is clipped
P2 | Visible presentation limitation

**Task:** Understand what the home search will search.

**Observed:** M01 shows the hint clipped after Search all content & s.... The full source hint is Search all content & settings.

**Comparison:** O01 uses Find or create a note; O03 presents a separate vault-search surface. Obsidian also has icon-learning costs.

**Impact:** The promise of a broad search is hidden exactly where a first-time user needs scope clarity.

**Direction:** Use a shorter visible invitation and an explicit scope explanation in the expanded search state, within the existing single bottom search.

**Manual acceptance:** At small width and large text, the primary search invitation remains understandable without guessing the clipped suffix.

**Source:** NotesController.renderHome: search.setHint and fixed filter control.

**Limit:** Clipping is visible; comprehension impact remains an expert judgment, not a user-study result.

Evidence: M01, O01

## N03 — Library mixes destinations with creation and intake
P1 | Expert UX inference from visible UI

**Task:** Choose between browsing existing work, creating content and managing the app.

**Observed:** M02 mixes Archive, Trash, Voice inbox, Open file, Viewed files, + Checklist, Folders and Settings in one undivided list.

**Comparison:** O02 is a file-browsing surface; O04 separately exposes actions. This is a concept-boundary comparison, not equivalent menu inventories.

**Impact:** Users must infer which entries open places and which start operations. The app can feel like a collection of tools rather than one workspace.

**Direction:** Group the existing menu by purpose and make creation/intake roles consistent. Retain requested Folders/Settings ordering and do not add duplicate Notes/Doc entries.

**Manual acceptance:** Ask new users where to find a saved note versus create a checklist; record wrong turns before and after grouping.

**Source:** NotesController.renderHome: navigation/menuRows.

**Limit:** No measured wrong-turn rate is available. Obsidian also distributes actions across several surfaces.

Evidence: M02, O02

## N04 — Notes and Doc ask for an early classification decision
P1 | Navigation hypothesis requiring hands-on confirmation

**Task:** Retrieve yesterday's content without remembering its creation mode.

**Observed:** M01 and M12 prominently separate Notes and Doc before showing content. Source implements independent workspace filtering.

**Comparison:** O01 presents several file kinds in one chooser. That does not mean MyNotes lacks cross-workspace search: its universal search already crosses Notes and Docs.

**Impact:** Mode recall can become an extra burden when users remember subject matter, not whether something was a Note or Doc.

**Direction:** Preserve both data models. Make retrieval scope and the current workspace obvious; use the existing global search as a reliable bridge rather than duplicating home controls.

**Manual acceptance:** Use mixed Notes/Docs with similar titles. Compare successful retrieval, workspace switches and backtracking, without instructing the creation mode.

**Source:** NotesController.filterNow; Workspace.includes; renderUniversalResults.

**Limit:** Not a claim that tabs are intrinsically bad or that the two models must be merged.

Evidence: M12, O01

## N05 — Folder browsing carries storage-management weight
P2 | Visible structure plus UX inference

**Task:** Open a folder during ordinary note organisation.

**Observed:** M03 repeats Folders / Your folders, explains private storage and originals, then includes Backups; source also appends Storage settings.

**Comparison:** O02 focuses on a collapsed fixture-folder tree. We did not execute a matched folder-navigation task in both apps.

**Impact:** Safety explanations are useful, but they compete with the everyday hierarchy and contribute to a settings-page feeling.

**Direction:** Keep accurate storage disclosures available, but progressively disclose management details away from the main folder list. Do not remove recovery routes.

**Manual acceptance:** With ten folders, the main task is immediately recognisable as browsing. Users can still locate backup and storage details without ambiguity.

**Source:** NotesController.renderFolders.

**Limit:** The sparse fixture is not evidence that real users have no folders.

Evidence: M03, O02

## N06 — Creating a folder adds a second sheet over Move to folder
P2 | Visible layering; behaviour unmeasured

**Task:** Create a destination while moving the current item.

**Observed:** M11 contains the dimmed editor, Move to folder and a second Create folder surface. Multiple dismiss/return levels are visible.

**Comparison:** No matched Obsidian create-while-moving capture is available. This is a directly observed MyNotes issue, not a claimed competitor speed win.

**Impact:** Users have to predict whether Create also moves the item, which layer closes, and where Back returns.

**Direction:** Use one clear staged flow inside the existing move surface, retaining the note and returning to the chosen destination. Keep the actual move outcome explicit.

**Manual acceptance:** Exercise Create, Cancel, system Back, invalid names and keyboard dismissal. Verify the correct destination and unchanged note contents after each branch.

**Source:** NotesController.renderMoveFolderSheet and renderFolders.

**Limit:** No claim of data loss or broken Back behaviour is established by this image.

Evidence: M11

## N07 — Global queries silently change the filtering contract
P1 | Source-supported scope mismatch risk

**Task:** Search while a folder or label filter is active.

**Observed:** filterNow makes every nonempty query on page Notes global: folder, workspace and label filters are bypassed; nondeleted archived notes are included. This is deliberate code behaviour, not a measured bug.

**Comparison:** Obsidian separates quick switching, vault search and tags in O01/O03/O05. Their combined filtering semantics were not tested.

**Impact:** A user can think the query narrows the current list while the implementation broadens it. That undermines predictability.

**Direction:** Keep global search if intended. State All notes & Docs / includes archived explicitly and make switching to current-scope search understandable.

**Manual acceptance:** Populate two folders, two labels, Notes/Docs and an archive item. For each scope/query combination, verify the displayed scope and returned IDs against the stated contract.

**Source:** NotesController.filterNow: global; included; global || filter expression; renderUniversalResults.

**Limit:** Source establishes the branch. Its actual presentation with a populated global query needs fresh live capture.

Evidence: M05, O03

## N08 — Universal results do not explain a body-text match
P1 | Source-supported presentation gap

**Task:** Identify the right note when the search phrase is only in its body.

**Observed:** renderUniversalResults builds title buttons plus Note/Doc, archive and folder metadata. That renderer does not add a matching body passage or query highlight.

**Comparison:** O03 is an empty-query capture, so no claim about superior Obsidian snippets or search accuracy is made here.

**Impact:** Similar titles require opening candidates to discover why they matched; finding information is not the same as merely returning an item.

**Direction:** Add a compact, safe matching passage with highlighted terms and retain the already useful type/folder metadata. Preserve original text and bound long snippets.

**Manual acceptance:** Create five identically titled items with different body phrases. Identify the intended item from the result context, then verify the correct open target.

**Source:** NotesController.renderUniversalResults: title button and type/folder line.

**Limit:** M06 shows a focused search entry, not a matched global query. Source evidence supports this finding; comparative relevance is unmeasured.

Evidence: M06, O03

## N09 — A filtered-empty library can look like a new empty library
P1 | Source-supported defect candidate; not live reproduced

**Task:** Understand why existing notes are not visible.

**Observed:** The emptyHint expression has a special no-labels-selected branch, but an ordinary nonempty label filter with zero matches and an empty query falls through to A little space for your ideas / Create a note below.

**Comparison:** O05 provides a tag-specific browsing context and counts. Its empty-tag state was not captured.

**Impact:** Users may create duplicate content or worry that saved notes disappeared when only the current view is empty.

**Direction:** Distinguish first-use empty, no results in this folder/filter, and loading/error states. Offer Clear filters without erasing actual content or preferences unexpectedly.

**Manual acceptance:** Keep a Work note, select a Personal-only filter, and clear the query. Confirm an accurate filtered-empty explanation and reversible clear action.

**Source:** NotesController.renderHome: emptyHint ternary; filterNow.

**Limit:** This follows a source branch. Reproduce on the signed build before assigning a confirmed runtime defect.

Evidence: M05, O05

## N10 — A broad search is not yet a clear action-discovery route
P2 | Action-discovery hypothesis

**Task:** Find a less-frequent action without knowing its menu location.

**Observed:** Our Library and editor divide operations across menus and panels. Universal search source includes a small fixed set of settings destinations, not all editing actions.

**Comparison:** O04 visibly lists commands with a dedicated Select a command input. Execution of every action is not certified by that screenshot.

**Impact:** New users may know what they want to do but not the correct menu, especially after moving between Notes and Doc.

**Direction:** First improve grouping and wording. If testing still exposes discovery failures, add lightweight action help within existing navigation; do not copy a 205-command interface wholesale.

**Manual acceptance:** Give unprompted tasks such as change heading, move note and find within note. Record menu detours and help requests, not just eventual completion.

**Source:** NotesController.renderUniversalResults: fixed settings array; renderEditor menus.

**Limit:** No measured task-time advantage or missing-basic-search claim is made.

Evidence: M02, O04

## E01 — Formatting plus keyboard squeezes out the writing context
P1 | Visible viewport constraint

**Task:** Format a phrase while retaining the surrounding paragraph.

**Observed:** M09 leaves only a short band of the note above the Format panel and keyboard. This directly shows how little prose remains visible in that captured state.

**Comparison:** O06 uses a compact toolbar above the keyboard while more of the note remains visible. Different fixtures prevent a precise productivity measurement.

**Impact:** Users may lose the sentence context, then need repeated panel closing and reopening to continue writing.

**Direction:** Adapt the existing single Format panel to available height; keep common controls compact and advanced options reachable. Preserve readable labels and the requested single A entry.

**Manual acceptance:** On the same device/font/IME, format the middle of a paragraph. Target at least three readable context lines at normal font, with a sensible large-text fallback and reachable caret.

**Source:** NotesController.installFormatting: formattingPanel; FormatControls; adaptEditorViewport.

**Limit:** The proposed context-line target is an acceptance proposal, not a measured current value for all devices.

Evidence: M09, O06

## E02 — Selection versus future typing is not explicit enough visually
P1 | Expert UX inference; existing accessible states acknowledged

**Task:** Know whether Bold will affect selected words or the next typed words.

**Observed:** M17 and M18 show similar Format surfaces with different selection/caret states. Source has accessibility state descriptions, but the visible panel heading is simply Format.

**Comparison:** O06 shows an active selection near a compact toolbar. It does not establish that Obsidian solves every mixed-style state.

**Impact:** A styling operation can feel unpredictable even when the underlying span logic is correct.

**Direction:** Clarify the target inside the existing panel heading or concise context text. Preserve current selection, typing state, accessibility descriptions and Clear as the fifth inline action.

**Manual acceptance:** Alternate selected text, collapsed caret and mixed formatting. Users should predict the target before tapping and observe only the intended text change.

**Source:** FormatControls.refresh; NotesController.noteTypingStyle and formatNoteValue.

**Limit:** This is a comprehension risk, not a claim that formatting currently edits the wrong range.

Evidence: M17, O06

## E03 — Routine formatting shares space with advanced controls
P2 | Visible density plus source-confirmed panel contents

**Task:** Apply a common style without navigating typography settings.

**Observed:** M10/M17 place paragraph choices, font family, Android size values and other options in a tall panel. FormatControls also provides Number paragraphs further down.

**Comparison:** O10 demonstrates configurable mobile toolbar actions. Customisation is not proof of a better default for novices.

**Impact:** More controls do not automatically make editing more effective; frequent actions compete with less-frequent configuration.

**Direction:** Prioritise frequent actions within the same panel and progressively disclose advanced options. Do not add another formatting strip or remove existing capabilities.

**Manual acceptance:** Observe five common formatting tasks and one advanced task. The common tasks should not require scanning advanced controls; the advanced task must remain discoverable.

**Source:** FormatControls constructor and panel ordering.

**Limit:** Action frequency is not measured yet. Validate the prioritisation with actual users.

Evidence: M17, O10

## E04 — Text-size controls expose implementation units
P2 | Visible terminology issue

**Task:** Choose a comfortable reading/writing size.

**Observed:** M17 shows 18 sp. This is an Android implementation unit rather than a familiar task description for most note-taking users.

**Comparison:** O13 also exposes pixels in Appearance. Obsidian is not a plain-language ideal here; its font-size configuration is a different workflow, not numerical equivalence or a proved competitor advantage.

**Impact:** The control adds translation work for users who just want larger or smaller text.

**Direction:** Use understandable size wording and preview the effect while retaining precise accessible adjustment and the existing allowed range.

**Manual acceptance:** Ask users to enlarge text one step and restore it without explaining sp. Verify large-text layout, mixed selections and saved sizes remain correct.

**Source:** FormatControls: sizeChoice, Size (8–72 sp), smaller/bigger.

**Limit:** Terminology judgment only; no unsupported contrast, physical-size or accessibility-failure claim.

Evidence: M17, O13

## E05 — The persistent editor toolbar gives decoration equal weight
P2 | Priority hypothesis from visible UI

**Task:** Keep the main writing actions immediately understandable.

**Observed:** M07/M08 show Insert, Format, Voice and Background as equal persistent destinations. Background is always at the same hierarchy as writing operations.

**Comparison:** O10 allows toolbar configuration, but its icon density also increases learning effort. We should not imitate that density indiscriminately.

**Impact:** For a typing-centred workflow, attention may be spent on decoration while structural actions remain inside other menus.

**Direction:** Keep the useful labels and required actions. Test contextual prominence or configurable order inside the existing toolbar instead of simply adding more buttons.

**Manual acceptance:** Run typing, checklist and reorganisation tasks. Determine whether the toolbar order matches user intent before changing defaults.

**Source:** NotesController.renderEditor writing toolbar.

**Limit:** Voice may be a primary workflow for some users. This is not an argument to remove or devalue it.

Evidence: M08, O10

## E06 — Long notes lack an established heading-jump counterpart
P1 | Candidate navigation gap in reviewed scope

**Task:** Return to a named section in a long note.

**Observed:** M19 demonstrates a long scrollable note. No heading-outline surface was established in the reviewed MyNotes captures; source inspection did not establish a heading navigator.

**Comparison:** O09 visibly lists the note heading hierarchy in an Outline pane. Exact jump latency and every tap outcome were not measured.

**Impact:** Scrolling and text search are weak substitutes when users remember document structure rather than an exact phrase.

**Direction:** Evaluate a lightweight heading navigator that reuses existing heading formatting. Preserve scroll position and make return-to-edit straightforward; avoid introducing graph-style complexity.

**Manual acceptance:** Use a note with twenty headings and repeated words. Find a named section, edit it, and return to the previous location without losing selection or text.

**Source:** Reviewed NotesController and runtime Java navigation surfaces; no absence claim beyond this scope.

**Limit:** Potential missing interaction, not proof that all versions or hidden paths lack it.

Evidence: M19, O09

## E07 — Mixed content looks like editable machinery even while reading
P1 | Visible presentation gap

**Task:** Read prose and a checklist as one coherent note.

**Observed:** M13 exposes block borders, a block header, grip, pencil/menu controls and per-row controls between Before and After. The fixture name Checklist01 is test data, not proven default product copy.

**Comparison:** O07 integrates rendered headings, lists and callouts into note content. Obsidian metadata and its own toolbar still consume space.

**Impact:** Management chrome interrupts the reading hierarchy and can make a small amount of content feel more complicated than it is.

**Direction:** Offer calmer nonselected presentation and reveal object actions contextually without losing direct editing, grip behaviour, pen-scoped actions or accessibility.

**Manual acceptance:** Read a mixed note without editing, then edit one row and reorder one block. Compare comprehension and accidental edits while ensuring every existing action is still reachable.

**Source:** NotesController.renderEditor and content block renderers.

**Limit:** This does not justify merging all object types or removing the existing safety controls.

Evidence: M13, O07

## E08 — Several nearby menus require users to infer their scope
P1 | Control-scope comprehension risk

**Task:** Rename a block versus change or delete an individual list item.

**Observed:** M13 has a block-level pencil/overflow and multiple row overflows. The visual relationship between each action and its object must be learned.

**Comparison:** O08 illustrates integrated reading, not a verified equivalent list-management menu. No competitor interaction advantage is assumed.

**Impact:** Choosing the wrong scope risks unwanted structural changes and increases hesitation around editing.

**Direction:** Make selected-object boundaries and action wording explicit while retaining per-content actions, per-cell operations and confirmation before permanent deletion.

**Manual acceptance:** Ask users to change one row, rename its block and remove only that row. Record wrong-menu openings and verify unaffected content remains byte/semantically unchanged.

**Source:** NotesController content headers and item action menus.

**Limit:** Existing content descriptions are not assumed missing; TalkBack interaction was not tested in this review.

Evidence: M13

## E09 — Save/location confidence differs between note surfaces
P1 | Visible feedback inconsistency

**Task:** Leave the editor knowing where the work is and whether it is saved.

**Observed:** M07 has no visible saved-status text in its captured top bar. M14/M11 show a status string truncated to Saved · internal · D... or Saved · on this devi....

**Comparison:** Obsidian exposes a file/path context on several views, but we did not inspect a comparable failure/recovery indicator. It is not a proven save-reliability winner.

**Impact:** The user has to infer safety differently across surfaces; truncation removes meaning from already compact status text.

**Direction:** Define concise shared saving/saved/error feedback and a readable location detail on demand. Keep one compact top row; do not add permanent duplicate status bars.

**Manual acceptance:** Edit, wait for save, leave, reopen and inject a controlled write failure on disposable data. Status must match persisted content and keep recovery/Back safe.

**Source:** NotesController.renderEditor; document header/status surfaces.

**Limit:** No lost save or data corruption was reproduced. This is feedback design, not a persistence-failure assertion.

Evidence: M14, O07

## E10 — Find-result context uses implementation-oriented wording
P2 | Visible copy/presentation concern

**Task:** Locate a phrase inside the current note and understand the hit.

**Observed:** M15 displays 1 match with Writing and a short passage; M16 correctly retains a highlighted hit after returning. The useful highlight should be preserved.

**Comparison:** A matched Obsidian in-note-find capture is unavailable in this evidence set. O03 is vault search and is not a valid substitute.

**Impact:** Writing is less precise than Body text or a user-facing content label in a mixed-content note.

**Direction:** Use concise content labels, accurate current/total hit feedback and consistent highlight visibility. Preserve the existing keyboard-aware field and successful highlighted return.

**Manual acceptance:** Search repeated text across body, checklist and table. Verify labels, next/previous order, close, keyboard placement and retention without changing content.

**Source:** NotesController.showNoteFind and content match labels.

**Limit:** Do not relabel the existing highlighted return as broken; no comparative search speed was measured.

Evidence: M15, M16

## Live testing remains outstanding
T01–T08 and smoothness/latency metrics are proposed in the PDF, not executed. The PDF contains the full method, caveats, prioritisation and module roadmap.