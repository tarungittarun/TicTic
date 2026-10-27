# Notes requirement and evidence ledger

Permanent IDs, 2026-10-27. Original scope snapshots below retain historical markers for the OLD MySpace implementation. They are NOT evidence for new MyNotes. Owner-approved development baseline overrides historical proposals.

All new-runtime requirements start Planned. AT-* are reserved acceptance scenario IDs, not executable tests. Before each slice, specify Given/When/Then cases and link executable tests plus runtime evidence. No item becomes Verified from a source grep alone.

## Original inventory

### K001
- Scope: **Staggered two-column card wall** ✖→✔ (exists; needs Keep's auto-height masonry rhythm) — cards size to content, colours and pins visible at a glance; reverse-chronological under the pinned section.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K001`; scenarios due before implementation.
- Evidence: none.

### K002
- Scope: **"Pinned" / "Others" section headers** ◐ — pinned notes always first, under a small caps header; the rest under "Others"; headers disappear when a section is empty.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K002`; scenarios due before implementation.
- Evidence: none.

### K003
- Scope: **New notes land at the top** ◐ — a saved note appears first (under pinned), not at the bottom (Keep behaviour; matches the owner's calculator-history instinct: newest on top).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K003`; scenarios due before implementation.
- Evidence: none.

### K004
- Scope: **Long-press drag-and-drop note reordering** ✖ — manual order on the home board (Keep Android, confirmed 2023+); dragged card lifts, others reflow; order persists. *Decision D6.*
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K004`; scenarios due before implementation.
- Evidence: none.

### K005
- Scope: **Grid ↔ single-column list view toggle** ✖ — toggle in the top bar; the choice persists; single-column is Keep's accessibility/reading mode.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K005`; scenarios due before implementation.
- Evidence: none.

### K006
- Scope: **Sorting: date created / last modified** ✖ — Keep added this Aug 2025 (menu → Sort); complements manual order (D6 picks which governs).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K006`; scenarios due before implementation.
- Evidence: none.

### K007
- Scope: **Navigation drawer**: Notes · Reminders · Labels… · Archive · Trash · Settings ◐ — hamburger at top-left; current page highlighted; labels listed individually under a "Labels" head.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K007`; scenarios due before implementation.
- Evidence: none.

### K008
- Scope: **Label pages from the drawer** ✖ — tapping a label opens its page: every note carrying it, full card wall, searchable within.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K008`; scenarios due before implementation.
- Evidence: none.

### K009
- Scope: **Archive page** ✔ — archived notes hidden from home but searchable and editable forever; Unarchive returns them to the top of Others.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K009`; scenarios due before implementation.
- Evidence: none.

### K010
- Scope: **Trash page with 7-day auto-purge** ✖ — deleted notes rest in Trash 7 days (Keep Help's official window; some 2025 web reports say 30 — *D4*), then vanish; Restore, Delete forever, and "Empty trash" live here. Purge runs locally on app open *(ours — no server needed)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K010`; scenarios due before implementation.
- Evidence: none.

### K011
- Scope: **Reminders page** ✖ — every note with an upcoming reminder, soonest first; tap opens the note; reminder editable/dismissable from here.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K011`; scenarios due before implementation.
- Evidence: none.

### K012
- Scope: **Friendly empty states** ✖ — each page says what belongs there ("Notes with upcoming reminders appear here", "Notes you archive appear here") — Keep's exact pattern, and the cure for dead-end screens this project has been burned by.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K012`; scenarios due before implementation.
- Evidence: none.

### K013
- Scope: **FAB "+" → instant new-note editor** ◐ — bottom-right FAB; the editor opens with focus in the body and the keyboard up; back with text = saved (no save button, ever).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K013`; scenarios due before implementation.
- Evidence: none.

### K014
- Scope: **Text note: title + body, autosave on exit** ✔ — Keep has no explicit save; leaving the editor persists. Title optional; untitled notes show body preview as their card face.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K014`; scenarios due before implementation.
- Evidence: none.

### K015
- Scope: **New checklist note** ◐ — dedicated capture (home quick-action + widget button): opens the editor already in list mode with the first checkbox waiting.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K015`; scenarios due before implementation.
- Evidence: none.

### K016
- Scope: **New drawing note** ✖ — opens straight to the drawing canvas; save makes the drawing the note's card image.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K016`; scenarios due before implementation.
- Evidence: none.

### K017
- Scope: **New voice note** ✖ — opens recording immediately; card shows the audio chip + duration *(needs D1)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K017`; scenarios due before implementation.
- Evidence: none.

### K018
- Scope: **New photo note** ✖ — system camera intent (no permission needed) or photo picker; the image becomes the card, text optional beneath.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K018`; scenarios due before implementation.
- Evidence: none.

### K019
- Scope: **Share-sheet target: capture from ANY app** ✖ — MySpace/MyNotes appears in Android's share sheet for text and images (`ACTION_SEND`); shared content becomes a new note instantly *(host-level: integration round)*. This is Keep's most-used capture path ("quickly capture text, links, or images from any mobile app").
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K019`; scenarios due before implementation.
- Evidence: none.

### K020
- Scope: **Quick-capture home-screen widget** ✖ — Keep's signature widget: four buttons (note · checklist · voice · camera) that open the matching capture directly *(integration round)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K020`; scenarios due before implementation.
- Evidence: none.

### K021
- Scope: **Undo snackbar after every destructive/quiet action** ✖ — "Note archived · Undo", "Moved to trash · Undo", "Note deleted · Undo" — Keep's safety net that replaces confirm dialogs *(ours: replaces the current delete-confirm, D5-adjacent)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K021`; scenarios due before implementation.
- Evidence: none.

### K022
- Scope: **Tap card → full editor** ✔ — opens in place with Keep's expand transition; tapping outside a quick-note dismisses without saving empty content.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K022`; scenarios due before implementation.
- Evidence: none.

### K023
- Scope: **Editor chrome** ◐ — top bar: back arrow (left), **Pin** (right). Body scrolls independently. Bottom action bar pinned. No toolbar clutter while typing.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K023`; scenarios due before implementation.
- Evidence: none.

### K024
- Scope: **Bottom bar, left: "+ Add"** ✖ — opens the attachment row: Image (camera/gallery) · Drawing · Audio recording.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K024`; scenarios due before implementation.
- Evidence: none.

### K025
- Scope: **Bottom bar: colour palette button** ◐ — opens the 12-swatch row **inside the editor**; picking repaints title, body and background live; default swatch = theme surface.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K025`; scenarios due before implementation.
- Evidence: none.

### K026
- Scope: **Bottom bar: Format button (underlined "A")** ✖ — Keep's Oct-2023 addition; opens the formatting toolbar.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K026`; scenarios due before implementation.
- Evidence: none.

### K027
- Scope: **Formatting toolbar: H1 · H2 · Aa · Bold · Italic · Underline · Strikethrough · Clear formatting** ✖ — selection-based standard rich text; renders on cards too (H1/H2 visibly larger, bold/italic/underline/strike preserved in the wall).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K027`; scenarios due before implementation.
- Evidence: none.

### K028
- Scope: **Markdown-style instant triggers** ✖ — typing `*`, `-` or `+` then space converts the line to a bullet; `1.` then space starts a numbered list (Keep's documented behaviour); Tab indents, Shift+Tab outdents (hardware keyboards).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K028`; scenarios due before implementation.
- Evidence: none.

### K029
- Scope: **Bulleted & numbered lists inside text notes** ✖ — distinct from checkbox lists: formatting, not tasks; auto-continues the bullet/number on Enter.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K029`; scenarios due before implementation.
- Evidence: none.

### K030
- Scope: **Clickable links** ✖ — URLs in bodies are live links; long-press offers copy/open *(ours: no Keep web-preview cards, but links must work)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K030`; scenarios due before implementation.
- Evidence: none.

### K031
- Scope: **Reminder bell in the editor** ✖ — date + time picker (Keep's tomorrow-morning / later-today / pick-date-time shortcuts); saved reminder shows as a **chip under the note body** with an × to remove.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K031`; scenarios due before implementation.
- Evidence: none.

### K032
- Scope: **Reminder chips on cards** ✖ — any note with a reminder shows the bell chip (with "Tomorrow, 8:00 AM"-style text) on its card in the wall — Keep's at-a-glance reminder visibility.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K032`; scenarios due before implementation.
- Evidence: none.

### K033
- Scope: **Archive from the editor** ✔ — bottom-bar/3-dot archive; note leaves home for the Archive page; Undo snackbar.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K033`; scenarios due before implementation.
- Evidence: none.

### K034
- Scope: **3-dot ▸ Delete → trash, not oblivion** ◐ — editor delete moves to Trash (7-day) with Undo snackbar; current confirm-dialog behaviour replaced *(D5)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K034`; scenarios due before implementation.
- Evidence: none.

### K035
- Scope: **3-dot ▸ Make a copy** ✖ — duplicates the note (content, colour, labels; not reminders) as a new note at the top.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K035`; scenarios due before implementation.
- Evidence: none.

### K036
- Scope: **3-dot ▸ Send / Share** ✖ — share the note as plain text (title + body) through the Android sheet; image notes share the image; Keep's "Send to other apps" minus the Docs target *(adapt)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K036`; scenarios due before implementation.
- Evidence: none.

### K037
- Scope: **3-dot ▸ Labels** ✖ — checkbox dialog of all labels + "Create new label" inline; multi-select; applied labels show as chips at the card bottom.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K037`; scenarios due before implementation.
- Evidence: none.

### K038
- Scope: **Merge notes** ✖ — from multi-select 3-dot: concatenates selected notes (titles become first lines) into one new note; sources go to Trash with Undo.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K038`; scenarios due before implementation.
- Evidence: none.

### K039
- Scope: **Convert text ⇄ checklist** ✖ — Keep can turn a text note's lines into checkboxes (and back); the plain-text storage rule (already ours) makes this lossless.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K039`; scenarios due before implementation.
- Evidence: none.

### K040
- Scope: **Hide checked items (list notes)** ✖ — per-note toggle; checked items collapse into an expandable "N checked items" footer row — Keep's grocery-list declutterer.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K040`; scenarios due before implementation.
- Evidence: none.

### K041
- Scope: **Customize list: themes & bullets (Keep, 2024)** ✖ — per-list picker of bullet glyph (check, dot, square, diamond, star…) and theme colour; renders on the card *(our dark-pill picker design per the calculator's round-32 rule: match the app's signature, never stock dialogs)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K041`; scenarios due before implementation.
- Evidence: none.

### K042
- Scope: **3-dot ▸ Grab image text (OCR)** ✖ — Keep extracts an image's text into the note body so search finds it; on-device via bundled ML Kit *(decision D2)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K042`; scenarios due before implementation.
- Evidence: none.

### K043
- Scope: **Timestamps: "Edited …" line** ◐ — Keep sorts by last-modified and surfaces edit time; show a subtle "Edited 27 Oct, 12:40" row at the editor bottom *(data already stored; display new)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K043`; scenarios due before implementation.
- Evidence: none.

### K044
- Scope: **Adjustable font size (Small / Medium / Large)** ✖ — Keep's teased-then-shipped reading comfort; app-wide setting affecting editor + cards.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K044`; scenarios due before implementation.
- Evidence: none.

### K045
- Scope: **Checkboxes tick instantly; item text stays plain** ◐ — Keep does NOT strikethrough by default; checked items simply dim/hide per setting 40.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K045`; scenarios due before implementation.
- Evidence: none.

### K046
- Scope: **Drag-to-reorder list items** ✖ — grip-and-move within the list; works on unchecked and checked alike.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K046`; scenarios due before implementation.
- Evidence: none.

### K047
- Scope: **Nested / indented list items** ✖ — sub-bullets under any item (Keep's "nested items" for multi-step lists); indent/outdent controls; nesting depth shown by inset.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K047`; scenarios due before implementation.
- Evidence: none.

### K048
- Scope: **Enter adds the next checkbox; empty trailing item auto-dropped** ◐ — the rhythm that makes Keep lists feel alive; saving a list with a blank last line drops it silently.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K048`; scenarios due before implementation.
- Evidence: none.

### K049
- Scope: **"Hide checked items" footer** ✖ — (same mechanism as 40, listed here as the list-UX half: the collapsed row shows the count and expands on tap).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K049`; scenarios due before implementation.
- Evidence: none.

### K050
- Scope: **Swipe-to-delete a checked item** ✖ — Keep Android lets a checked item be swiped away; with Undo snackbar *(verify against current Keep during build; ship behind the same gesture rules as the calculator: outcomes logged)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K050`; scenarios due before implementation.
- Evidence: none.

### K051
- Scope: **Links and plain text as list items** ✔◐ — list items are ordinary text (URLs clickable); no rich formatting inside checkbox items (Keep's actual limit — formatting lives in text notes).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K051`; scenarios due before implementation.
- Evidence: none.

### K052
- Scope: **Checklists stored as plain text** ✔ — our existing rule and Keep-compatible: search, export, share and the JSON mirror all keep working; checkbox state is a line prefix.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K052`; scenarios due before implementation.
- Evidence: none.

### K053
- Scope: **Camera capture into a note** ✖ — system-camera intent, zero permissions *(ours: same pattern as the Scanner)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K053`; scenarios due before implementation.
- Evidence: none.

### K054
- Scope: **Photo-picker / gallery attach** ✖ — Android photo picker (no permission on 33+; the suite's capped media-read covers below 33); copies into `files/notes-media/` so notes never break when originals move *(adapt — Keep references Drive photos; we own our copies)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K054`; scenarios due before implementation.
- Evidence: none.

### K055
- Scope: **Multiple images per note** ✖ — Keep allows several; they stack in the note, thumbnails on the card.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K055`; scenarios due before implementation.
- Evidence: none.

### K056
- Scope: **Image-first cards + full-screen viewer** ✖ — image on top, text below; tap → immersive viewer with swipe between a note's images.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K056`; scenarios due before implementation.
- Evidence: none.

### K057
- Scope: **OCR: grab image text** ✖ — (see 42) recognized text appended to the body and indexed by search; on-device only *(D2)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K057`; scenarios due before implementation.
- Evidence: none.

### K058
- Scope: **Voice memo recording in notes** ✖ — record button → waveform/timer UI → stop saves an audio chip *(D1: RECORD_AUDIO, the one permission Notes would declare)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K058`; scenarios due before implementation.
- Evidence: none.

### K059
- Scope: **Audio playback with seek + card chip** ✖ — play/pause/scrub inside the note; card shows 🎙 + duration; recordings live in app-private storage (Recorder app's proven pattern).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K059`; scenarios due before implementation.
- Evidence: none.

### K060
- Scope: **Automatic transcription of voice memos** ✖ — Keep's headline feature ("record a voice memo and have it automatically transcribed"). Cloud-based for Keep; offline needs on-device speech ML — **required, offline only** per owner approval; preserve original audio and allow editing the transcript.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K060`; scenarios due before implementation.
- Evidence: none.

### K061
- Scope: **Drawing canvas: finger + stylus** ✖ — Keep's drawing notes: pen and highlighter strokes, several ink colours, stroke widths, undo; saves as the note's image. Stylus pressure optional.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K061`; scenarios due before implementation.
- Evidence: none.

### K062
- Scope: **Drawing = first-class card** ✖ — drawings render as image cards, searchable by label/colour like any note; "Save a drawing as a note" is Keep's own help-page flow.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K062`; scenarios due before implementation.
- Evidence: none.

### K063
- Scope: **Pin / unpin** ✔ — pin icon top-right in editor; pinned section on home; Keep's rule: pinned always first regardless of date.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K063`; scenarios due before implementation.
- Evidence: none.

### K064
- Scope: **12 background colours + default** ◐ — Keep's palette (white/default, red, orange, yellow, green, teal, blue, dark-blue, purple, pink, brown, gray — 12 with default per current Keep); colour paints card, editor and viewer; **every colour contrast-guarded for dark AND light** (the calculator's round-20/33 lesson applied from day one) *(ours)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K064`; scenarios due before implementation.
- Evidence: none.

### K065
- Scope: **Colour from multi-select** ✖ — bulk recolour several cards at once (see 86).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K065`; scenarios due before implementation.
- Evidence: none.

### K066
- Scope: **Labels: create / rename / delete** ✖ — drawer → "Create new label" (Keep caps at 50; we keep the cap as a sanity limit); rename/delete from the labels editor; deleting a label removes it from notes but never touches the notes.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K066`; scenarios due before implementation.
- Evidence: none.

### K067
- Scope: **Multiple labels per note** ✖ — Keep's folders-that-aren't-folders: one note, many labels; label chips at the card bottom; tapping a chip opens the label page.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K067`; scenarios due before implementation.
- Evidence: none.

### K068
- Scope: **`#hashtag` quick labelling** ✖ — typing `#` in the body offers matching labels inline and creates new ones on the fly (documented Keep behaviour).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K068`; scenarios due before implementation.
- Evidence: none.

### K069
- Scope: **Label pages with in-label search** ✖ — (pairs with 8) each label page is a full wall scoped to that label, with its own search field.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K069`; scenarios due before implementation.
- Evidence: none.

### K070
- Scope: **Archive: out of sight, never lost** ✔ — archived notes stay searchable and editable; the Archive page lists them; this is Keep's "filing vs shredding" distinction.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K070`; scenarios due before implementation.
- Evidence: none.

### K071
- Scope: **Unarchive** ◐ — one tap (editor or card 3-dot) returns the note to the top of Others, with Undo.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K071`; scenarios due before implementation.
- Evidence: none.

### K072
- Scope: **Trash: 7-day window, restore, delete forever, empty trash** ✖ — (pairs with 10) the full Keep trash lifecycle, enforced locally: purge stamp checked on every app open; "Empty trash" asks once, then Undo-able for the session *(D4)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K072`; scenarios due before implementation.
- Evidence: none.

### K073
- Scope: **Time & date reminders, native notifications** ✖ *(adapt)* — Keep moved these to Google Tasks and killed location reminders (Oct 2025); **we keep them fully in-app**: local reminder store, exact-alarm permission flow explained with consequences (the Clock app's proven pattern), notification with Open/Snooze/Complete actions. No account, no migration, works offline.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K073`; scenarios due before implementation.
- Evidence: none.

### K074
- Scope: **Snooze & complete from the notification** ✖ — snooze re-arms (+10 min / +1 h / tomorrow shortcuts); complete clears the reminder chip; both logged as outcomes *(ours)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K074`; scenarios due before implementation.
- Evidence: none.

### K075
- Scope: **Instant search-as-you-type over titles + bodies** ◐ — Keep filters while you type. *Conflict to settle (D5):* the owner's earlier instruction made our search submit-based to kill per-keystroke rebuilds; Keep-grade instant search returns **only if** filtering happens off the UI thread and cards update in place — the recorder-blink lesson pinned by contract.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K075`; scenarios due before implementation.
- Evidence: none.

### K076
- Scope: **Search sees OCR text and transcripts** ✖ — Keep's "grab the text from an image to help you quickly find that note again through search" — image text and audio transcripts are indexed like typed text.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K076`; scenarios due before implementation.
- Evidence: none.

### K077
- Scope: **Filter chips: type** ✖ — Keep's search filters: Lists · Voice memos · Images · Drawings (documented filter set).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K077`; scenarios due before implementation.
- Evidence: none.

### K078
- Scope: **Filter chips: colour** ✖ — restrict results to a background colour.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K078`; scenarios due before implementation.
- Evidence: none.

### K079
- Scope: **Filter chips: label** ✖ — restrict to a label (plus the `label:`-style quick entry from the search field).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K079`; scenarios due before implementation.
- Evidence: none.

### K080
- Scope: **Search-bar UX** ◐ — full-width rounded bar at the top of home; tap → search page with filter row; clear ✖ inside; back collapses to home; recent queries *(Keep shows recent searches on focus — include)*.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K080`; scenarios due before implementation.
- Evidence: none.

### K081
- Scope: **Long-press a card → selection mode** ✖ — card lifts and shows a check; other cards selectable by tap; back or ✖ exits.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K081`; scenarios due before implementation.
- Evidence: none.

### K082
- Scope: **Selection top bar** ✖ — "N selected" + close; action icons replace the normal top bar exactly like Keep's.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K082`; scenarios due before implementation.
- Evidence: none.

### K083
- Scope: **Bulk archive** ✖ — with one Undo snackbar for the batch.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K083`; scenarios due before implementation.
- Evidence: none.

### K084
- Scope: **Bulk delete → trash** ✖ — one Undo; the batch purges together after 7 days.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K084`; scenarios due before implementation.
- Evidence: none.

### K085
- Scope: **Bulk label** ✖ — the label dialog applies to every selected note (adds, never replaces).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K085`; scenarios due before implementation.
- Evidence: none.

### K086
- Scope: **Bulk colour** ✖ — palette row recolours all selected cards at once.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K086`; scenarios due before implementation.
- Evidence: none.

### K087
- Scope: **Bulk share/send** ✖ — selected notes concatenated (title + body separators) into one share payload; image notes attach their images.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K087`; scenarios due before implementation.
- Evidence: none.

### K088
- Scope: **Merge** ✖ — (pairs with 38) available whenever ≥2 notes are selected; the merged note opens immediately for editing.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K088`; scenarios due before implementation.
- Evidence: none.

### K089
- Scope: **Quick-capture widget** ✖ — (pairs with 20) resizable; four direct buttons; the suite's widget engine renders it (owner's standing rule: widgets re-placeable, resize-aware, 150×84dp min hints).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K089`; scenarios due before implementation.
- Evidence: none.

### K090
- Scope: **Single-note widget** ✖ — pin one note to the home screen showing its real content (Keep's 2023 single-note widget); tap opens the note; colour and pin state reflected.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K090`; scenarios due before implementation.
- Evidence: none.

### K091
- Scope: **List widget with live checkboxes** ✖ — tick list items from the home screen without opening the app; ticks write through to the note.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K091`; scenarios due before implementation.
- Evidence: none.

### K092
- Scope: **Widget reconfiguration** ✖ — launcher long-press ▸ Configure re-enters Widget Studio for that instance (the suite's existing re-edit pattern).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K092`; scenarios due before implementation.
- Evidence: none.

### K093
- Scope: **Reminder notifications as a proper channel** ✖ — named channel, exact-alarm flow with consequences explained, snooze/complete actions, survives reboot (the Clock's `EventReminderReceiver` re-arm pattern reused, not reinvented). *(Wear OS tiles: out of scope — no Wear target.)*
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K093`; scenarios due before implementation.
- Evidence: none.

### K094
- Scope: **Material 3 Expressive look (Keep's Aug-2025 redesign)** ✖ — bolder typography, expressive shapes and buttons, springy motion — as a *design reference*, rendered in **our** signature: dark-pill popups, glass surfaces, GelMotion press feels (the calculator's round-32 rule: pickers and menus match the app's design, never stock dialogs).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K094`; scenarios due before implementation.
- Evidence: none.

### K095
- Scope: **True dark mode on every surface** ◐ — editor, viewer, drawer, trash, widgets; palette-resolved like the rest of the suite; no white flashes (Keep's dark mode is exact-dark, not gray).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K095`; scenarios due before implementation.
- Evidence: none.

### K096
- Scope: **Motion with outcomes** ✖ — card expand-from-position into the editor, staggered card entrance, FAB morph, snackbar slides; every animation logged where it gates behaviour (calculator lesson: animations must never swallow a state change).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K096`; scenarios due before implementation.
- Evidence: none.

### K097
- Scope: **Contrast-guarded inks everywhere** ✖ — the calculator's 0.35-luminance guard applied to all 12 note colours × dark/light text: a colour cannot ship unreadable text (round-33 rule, reused wholesale).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K097`; scenarios due before implementation.
- Evidence: none.

### K098
- Scope: **Honeycomb-family icon** ✖ *(ours)* — the standalone MyNotes and the in-suite Notes tile carry the honeycomb mark the owner chose forever, in Notes' accent.
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K098`; scenarios due before implementation.
- Evidence: none.

### K099
- Scope: **Locale-correct timestamps** ✖ *(ours)* — "Edited 27 Oct, 12:40" / "Tomorrow, 8:00 AM" rendered Indian-locale-correct (the calculator's grouping lesson: locale is a feature, not an afterthought).
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K099`; scenarios due before implementation.
- Evidence: none.

### K100
- Scope: **Offline-first, zero-account guarantee** ✖ *(ours — the advantage)* — every one of the 99 above works with no network and no sign-in, forever. Keep itself lost this in 2025 (reminders exiled to Tasks, location reminders killed, collaboration account-bound). Our pitch line: **all of Keep that a phone can do alone.**
- State: **Planned** for new runtime.
- Acceptance reservation: `AT-K100`; scenarios due before implementation.
- Evidence: none.

## Expanded scope

| ID | Acceptance obligation | State / evidence |
|---|---|---|
| DG-01 | Basic and expanded shapes serialize/reopen without geometry loss. | Planned; no runtime evidence |
| DG-02 | Attached straight/elbow/curved connectors follow moved shapes; labels and heads persist. | Planned; no runtime evidence |
| DG-03 | Free and block text remains editable, styled, searchable and readable. | Planned; no runtime evidence |
| DG-04 | Grouping, layers, lock, duplicate, copy/paste, align and distribute retain relationships. | Planned; no runtime evidence |
| DG-05 | Zoom/pan/grid/guides, templates, undo/redo and full-content PNG pass both themes. | Planned; no runtime evidence |
| ST-01 | Chosen supported local tree holds the actual authoritative library; cancel preserves destination. | Planned; no runtime evidence |
| ST-02 | Interrupted writes recover without false Saved feedback. | Protocol probe only: qa/test_notes_storage_probe.py; Android/SAF pending |
| ST-03 | Folder move verifies documents/media before switching and preserves source on failure. | Planned; no runtime evidence |
| ST-04 | Revoked/full/unplugged/provider-failed storage exposes recovery without silent relocation. | Planned; no runtime evidence |
| ST-05 | Schema migration, backup/import and duplicate-ID handling retain documents and assets. | Planned; no runtime evidence |
| ST-06 | Stale writes and forks are blocked, not silently overwritten. | Protocol probe only: qa/test_notes_storage_probe.py; Android/SAF pending |
| ML-01 | Bundled OCR works from clean offline install; script coverage and quality are documented. | Planned; no runtime evidence |
| ML-02 | Bundled automatic offline ASR preserves audio on failure, supports progress/cancel/retry. | Planned; no runtime evidence |
| ML-03 | Microphone deny/permanent deny/revoke leaves other features and imported-audio ASR usable. | Planned; no runtime evidence |
| ML-04 | Engine/model hashes/licenses, ABIs and real-device RAM/latency/quality results are documented. | Planned; no runtime evidence |
| HX-01 | New runtime stays standalone-only until deliberate integration; no old feature consumes it. | Planned; no runtime evidence |
| HX-02 | Both host adapters pass route/theme/permission/share/notification/widget contracts. | Planned; no runtime evidence |
| HX-03 | Receiving host acquires folder grant and migrates data without loss/duplicate IDs. | Planned; no runtime evidence |
| NF-01 | 10,000-note search is off-thread, latest query wins and input focus does not churn. | Planned; no runtime evidence |
| NF-02 | Both themes, large text, TalkBack, lifecycle and device/API matrix pass. | Planned; no runtime evidence |
| NF-03 | Diagnostics record outcomes, omit content by default and share only explicitly. | Planned; no runtime evidence |

## Slice 2 evidence update (2026-10-27)

The version-0.1.0 text vertical slice is written in :notes-domain/:notes-runtime/:notes.
Relevant K requirements have **partial implementation, unverified on Android**; their
full original acceptance scope is NOT complete. In particular, the initial Home is a
single-column list, not the final masonry/card design; delete Undo is a Home control,
not the final snackbar; there is no colour picker, rich content or SAF adapter yet.

- Text create/edit/autosave: partial implementation; Android verification pending.
- Search: debounce and background filtering with stale-query rejection; no 10,000-note
  performance result, FTS, filters or Unicode-normalization qualification yet.
- Pins/archive/Trash/restore/Undo: implemented in the first text slice; UI/end-user
  stories pending. Trash expiry runs at library load. Permanent delete UI is pending.
- ST-02: app-private AtomicFile repository with readback; SAF implementation still pending.
- HX-01: eight additional source/build contracts included in 416 passing Python checks.
- Domain: seven JUnit tests written, NOT run (no local JDK).
- Android persistence: five instrumented tests written, NOT compiled or run.
- ML and diagram requirements remain Planned. No new-runtime requirement is Verified
  from these source checks alone.


## Slice 4 evidence — 0.2.1 (3)

- ST-01/ST-03/ST-04: local-only SAF backend, picker/grants, verified folder change,
  private recovery and storage controls implemented; **actual SAF/provider/device
  acceptance remains unverified**, not marked complete.
- ST-02/ST-06: Java generation protocol now additionally exercised by Android tests
  with an in-memory fault backend. This does not prove every physical provider.
- HX-01: changes remained inside mynotes/ and its dedicated workflow. Old apps untouched.
- Update policy: release identity/certificate/code gates and emulator install-over-old
  passed in run 40564058421. Old v1 document format remains readable in tests.
- Android suite 14/14 passed; JVM rules tests passed; 425 Python checks passed.
  See M0-STATUS.md and STORAGE-v0.2.md for limits. ML/diagram/full feature items remain open.


## Slice 5 evidence — 0.3.0 (4)

- ST-05 partial implementation: existing local library reopen with re-grant, explicit
  non-destructive preview/confirm merge, checksum-verified portable text backups,
  duplicate/conflict handling and pre-restore checkpoint. 8 MiB portable format bound;
  complete streaming media backup and physical provider acceptance remain open.
- ST-04 repository recovery: restart/retry and import into unavailable destination
  now exercised with real AtomicFile + injected backend. This is NOT physical SAF proof.
- HX-03 groundwork: selected-folder reopen and portable IDs/merge are reusable by a
  future receiving host. Actual MySpace integration and grants/migration remain pending.
- CI 40566162164: 14 JVM and 28 Android tests passed; install/signature/version gates
  passed. 430 Python checks passed. Current guide links the published 0.3.0 (4) APK.
- Full UI preview/cancel/document-picker journeys remain for on-device acceptance.


## Slice 6 evidence — 0.4.0 (5)

Core implementation expanded substantially; the original K criteria remain binding.
Scope-specific runtime evidence in CI 40569713346:

- K001/K002/K005/K006: virtualised two-column masonry/list, pinned sections and sort
  preferences implemented. Wall manual order/drag remains pending; performance/device
  matrix is not inferred from RecyclerView.
- K026–K029/K039–K052: structured checklist and selected-range formatting foundations
  implemented and exercised, but NOT all original criteria complete. Sticky typing,
  markdown, row drag, custom bullets/themes and full undo stack remain open.
- K063–K074: per-note/bulk labels, colour swatches and label filtering implemented;
  global label manager and hashtag/autocomplete remain open.
- K081–K088: multi-select metadata actions, share, duplicate, merge/sources-to-Trash and
  basic Undo implemented. Share host is faked in the controller test; full external
  destination behaviour and final snackbar UX remain for device qualification.
- ST-05: version-1 migration and schema-2 backup/restore for marks, rows, labels tested;
  legacy plain conflict-copy IDs preserved across the upgrade.
- 29 JVM + 40 Android + 440 local checks passed. No full-feature completion percentage
  asserted. Remaining system/media/ML/diagram/integration scope is unchanged.

## Slice 7 evidence — 0.5.0 (6)

CI40578488274: 51 JVM; 63 unique Android tests / 441 executions, 13 emulator lanes;
independent lint, APK audit, immutable inventory and final acceptance all green.

- Diagram requirement: six common flowchart shapes/blocks/text boxes, editable geometry,
  directed labeled arrows, drag/button movement, color, resize and basic Undo delivered.
  Backup/copy retain editable objects; Merge refuses diagram flattening. Fixed single
  logical canvas, not freehand, auto-layout, snapping or multi-page diagram editing.
- PDF: native paginated text/checklist and shared vector diagram renderer; PDFRenderer
  reopens/renders generated pages in tests. Local destination copy/readback is implemented;
  physical provider failures and external reader/language fidelity need device review.
- K063–K074: global rename/combine/remove and additive bulk assignment now implemented;
  unused global label definitions and hashtag autocomplete remain open.
- Incoming plain-text Android sharing now implemented. Other media is not implied.
- Real font/orientation/night resource assertions plus controller cases pass for 200%
  font, landscape, night and tablet profiles, after fixing a board viewport collapse.
  Physical taps/IME/TalkBack are not universally certified by dispatched view tests.
- Required offline OCR and voice transcription remain OPEN at this release checkpoint.
