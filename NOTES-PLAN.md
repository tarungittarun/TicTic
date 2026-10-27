# NOTES-PLAN — MySpace Notes, referenced to Google Keep

**Owner's instruction (2026-10-27):** *"Now we took most of our features for our calculator app from Google calculator. So similarly, I need to get started on notes by reference to Google Keep notes. So, prepare top 100 features from Google Keep notes, apart from Google backup, Google sign-in. Take everything from UI design, working, user experience, everything. Share the selected features, then we can start. Keep in mind, development of these separate apps like calculator, notes should not affect other apps or working features. It should be like microservices."*

This document is the **reference sheet** (like the calculator's spec sheet was: REFERENCE ONLY — the owner's shipped taste wins wherever they differ). It contains:

- §1 — what is excluded and why (account/cloud features)
- §2 — the **microservice isolation guarantee** (how Notes work cannot touch any other app)
- §3 — honest inventory of the existing `:feature-notes` (what already ships)
- §4 — **the top 100 Google Keep features**, in 10 categories, each with: what Keep does (UI + working + UX detail), our local-first adaptation where needed, and status (`✔` shipped · `◐` partial · `✖` new)
- §5 — decision points that need the owner's call before building
- §6 — proposed build phases and the test plan (MASTERCLASS §13 applied)

Research basis: Google Keep Help pages, the Play Store listing, and the 2023–2025 Keep changelog as reported by 9to5Google / Android Police / The Verge / Workspace Updates blog (rich text formatting Oct 2023, customizable lists 2024, sorting + Material 3 Expressive Aug 2025, reminders→Tasks migration and location-reminder discontinuation Oct–Nov 2025). Verified 2026-10-27.

---

## 1. Excluded (owner-directed + account-dependent)

| Excluded | Why |
|---|---|
| Google backup / sync | Owner excluded. Ours is local-first by standing rule (offline, no network). |
| Google sign-in / account avatar / account switching | Owner excluded. |
| Real-time collaboration (shared notes, collaborator management, "shared with" search filter) | Requires Google accounts and sync — dead without sign-in. A local "share as text/image" path covers the practical need (§4 G). |
| Reminders → Google Tasks migration, Calendar/Tasks integration | Keep itself moved time-reminders into Tasks (late 2025) and killed location reminders. **We do the opposite: reminders stay native, local notifications, no account** — which is now an advantage, not a gap. |
| "Copy to Google Docs" / Keep-in-Docs-Sheets-Slides / Gmail integration | Account features. Adaptation: Send/share/copy as plain text (§4 G). |
| Web app, iOS app, Chrome extension, Wear OS tiles | Other platforms. (Wear listed once in §4 I as out-of-scope for completeness.) |
| Cloud voice transcription | Keep transcribes via Google servers. Offline equivalent needs heavy on-device ML — decision point D3. |

---

## 2. Microservice isolation guarantee (owner's hard requirement)

*"Development of these separate apps like calculator, notes should not affect other apps or working features. It should be like microservices."*

The module graph already enforces this; here is the guarantee made concrete for the Notes rework:

1. **Dependency rule (existing, now pinned harder by qa contracts):** `:feature-notes` may import only `:core-*` — never `:app`, never `:shell`, never another `:feature-*`. No other module may import `:feature-notes` except the `:app` registry (and a standalone host). New contracts will grep every source file both ways and fail the gate on any violation.
2. **Data isolation:** notes live under their own prefs keys (`notes_items` mirror) and their own Room tables (`NoteEntity` in `:core-data`'s database — the established cross-module data seam). Images/audio/drawings get their own app-private subfolder (`files/notes-media/`). No other app reads or writes any of it. The JSON mirror remains the exchange format so **data outlives the module** (SUITE-PLAN §3.4) — a `lite` build without Notes still holds the data.
3. **The calculator stays frozen.** `feature-calculator/`, `calc/` and their suites are not touched by Notes work (owner's freeze, 2026-10-27). Their tests keep running in CI as regression proof that Notes work broke nothing.
4. **Development happens behind a standalone host, like MyCalc:** a new `:notes` module (one activity implementing `ScreenHost`, applicationId `app.space.notes`, debug suffix `.debug`, minimal permissions) hosts the exact same `:feature-notes`. The instrumented end-user suite runs against the standalone — **MySpace's APK does not change at all during development**. The owner perfects MyNotes on his phone first; integration into MySpace is one deliberate final round (registry already exists; the swap replaces the old screen with the new one) with the full suite regression-tested, per his standing rule.
5. **Widgets and the share-sheet target are host-level surfaces** — they land only in the integration round, in `:app`/`:core-widgets`, never inside the feature module (same reason `ScreenHost.toast` exists: a feature has no business constructing host surfaces).
6. **Blast radius of any Notes commit:** `:feature-notes` + `:notes` + `qa/` + docs. If a CI run shows a red outside that radius, it is treated as a Notes defect regardless of what it looks like.

---

## 3. What `:feature-notes` already is (honest inventory, 813 lines)

From the Sprint-2 rebuild (build 3.9.0, after the owner judged Notes "not comparable to Google Notes"):

- **Shipped ✔:** two-column coloured card wall; pins held on top; archive (drawer toggle, files instead of deleting); checklists stored as plain text (so search and export keep working) with line-toggle rules (`isChecklistLine`, `toggleChecklistLineAt`); search box that **submits** instead of rebuilding per keystroke (that rebuild-per-keystroke was itself a fixed defect); title+body editor with Pin/Archive/Delete pills and a delete-confirm dialog; `NotesStore` = Room as store-of-record + JSON mirror in `AppPrefs` (`notes_items`, cap 500); `NotesRules` with preview/displayTitle/sort comparators; dark mode via the suite palette; JVM rules tests.
- **Not there at all ✖:** labels, reminders, trash/restore, images, audio, drawing, rich text formatting, list customization (nesting/drag/hide-checked), multi-select bulk actions, merge/duplicate/share, search filters, grid/list toggle, sorting UI, drag-and-drop note order, widgets, share-sheet target, undo snackbars, timestamps display, OCR.
- **Known deltas from Keep to settle in §5:** submit-search vs Keep's instant search; delete-with-confirm vs Keep's trash+undo; no section headers ("Pinned"/"Others"); archive as a drawer toggle vs a nav page.

This is the starting line. The 100 below are the finish line.

---

## 4. The top 100 Google Keep features

Status legend: **✔** already ships in `:feature-notes` · **◐** partial foundation exists · **✖** new work.
Every item is Keep-derived; local-first adaptations are marked *(adapt)*; items that are ours-by-necessity (no account) are marked *(ours)*.

### A. Home screen & navigation (1–12)

1. **Staggered two-column card wall** ✖→✔ (exists; needs Keep's auto-height masonry rhythm) — cards size to content, colours and pins visible at a glance; reverse-chronological under the pinned section.
2. **"Pinned" / "Others" section headers** ◐ — pinned notes always first, under a small caps header; the rest under "Others"; headers disappear when a section is empty.
3. **New notes land at the top** ◐ — a saved note appears first (under pinned), not at the bottom (Keep behaviour; matches the owner's calculator-history instinct: newest on top).
4. **Long-press drag-and-drop note reordering** ✖ — manual order on the home board (Keep Android, confirmed 2023+); dragged card lifts, others reflow; order persists. *Decision D6.*
5. **Grid ↔ single-column list view toggle** ✖ — toggle in the top bar; the choice persists; single-column is Keep's accessibility/reading mode.
6. **Sorting: date created / last modified** ✖ — Keep added this Aug 2025 (menu → Sort); complements manual order (D6 picks which governs).
7. **Navigation drawer**: Notes · Reminders · Labels… · Archive · Trash · Settings ◐ — hamburger at top-left; current page highlighted; labels listed individually under a "Labels" head.
8. **Label pages from the drawer** ✖ — tapping a label opens its page: every note carrying it, full card wall, searchable within.
9. **Archive page** ✔ — archived notes hidden from home but searchable and editable forever; Unarchive returns them to the top of Others.
10. **Trash page with 7-day auto-purge** ✖ — deleted notes rest in Trash 7 days (Keep Help's official window; some 2025 web reports say 30 — *D4*), then vanish; Restore, Delete forever, and "Empty trash" live here. Purge runs locally on app open *(ours — no server needed)*.
11. **Reminders page** ✖ — every note with an upcoming reminder, soonest first; tap opens the note; reminder editable/dismissable from here.
12. **Friendly empty states** ✖ — each page says what belongs there ("Notes with upcoming reminders appear here", "Notes you archive appear here") — Keep's exact pattern, and the cure for dead-end screens this project has been burned by.

### B. Note creation & capture (13–22)

13. **FAB "+" → instant new-note editor** ◐ — bottom-right FAB; the editor opens with focus in the body and the keyboard up; back with text = saved (no save button, ever).
14. **Text note: title + body, autosave on exit** ✔ — Keep has no explicit save; leaving the editor persists. Title optional; untitled notes show body preview as their card face.
15. **New checklist note** ◐ — dedicated capture (home quick-action + widget button): opens the editor already in list mode with the first checkbox waiting.
16. **New drawing note** ✖ — opens straight to the drawing canvas; save makes the drawing the note's card image.
17. **New voice note** ✖ — opens recording immediately; card shows the audio chip + duration *(needs D1)*.
18. **New photo note** ✖ — system camera intent (no permission needed) or photo picker; the image becomes the card, text optional beneath.
19. **Share-sheet target: capture from ANY app** ✖ — MySpace/MyNotes appears in Android's share sheet for text and images (`ACTION_SEND`); shared content becomes a new note instantly *(host-level: integration round)*. This is Keep's most-used capture path ("quickly capture text, links, or images from any mobile app").
20. **Quick-capture home-screen widget** ✖ — Keep's signature widget: four buttons (note · checklist · voice · camera) that open the matching capture directly *(integration round)*.
21. **Undo snackbar after every destructive/quiet action** ✖ — "Note archived · Undo", "Moved to trash · Undo", "Note deleted · Undo" — Keep's safety net that replaces confirm dialogs *(ours: replaces the current delete-confirm, D5-adjacent)*.
22. **Tap card → full editor** ✔ — opens in place with Keep's expand transition; tapping outside a quick-note dismisses without saving empty content.

### C. The editor (23–44)

23. **Editor chrome** ◐ — top bar: back arrow (left), **Pin** (right). Body scrolls independently. Bottom action bar pinned. No toolbar clutter while typing.
24. **Bottom bar, left: "+ Add"** ✖ — opens the attachment row: Image (camera/gallery) · Drawing · Audio recording.
25. **Bottom bar: colour palette button** ◐ — opens the 12-swatch row **inside the editor**; picking repaints title, body and background live; default swatch = theme surface.
26. **Bottom bar: Format button (underlined "A")** ✖ — Keep's Oct-2023 addition; opens the formatting toolbar.
27. **Formatting toolbar: H1 · H2 · Aa · Bold · Italic · Underline · Strikethrough · Clear formatting** ✖ — selection-based standard rich text; renders on cards too (H1/H2 visibly larger, bold/italic/underline/strike preserved in the wall).
28. **Markdown-style instant triggers** ✖ — typing `*`, `-` or `+` then space converts the line to a bullet; `1.` then space starts a numbered list (Keep's documented behaviour); Tab indents, Shift+Tab outdents (hardware keyboards).
29. **Bulleted & numbered lists inside text notes** ✖ — distinct from checkbox lists: formatting, not tasks; auto-continues the bullet/number on Enter.
30. **Clickable links** ✖ — URLs in bodies are live links; long-press offers copy/open *(ours: no Keep web-preview cards, but links must work)*.
31. **Reminder bell in the editor** ✖ — date + time picker (Keep's tomorrow-morning / later-today / pick-date-time shortcuts); saved reminder shows as a **chip under the note body** with an × to remove.
32. **Reminder chips on cards** ✖ — any note with a reminder shows the bell chip (with "Tomorrow, 8:00 AM"-style text) on its card in the wall — Keep's at-a-glance reminder visibility.
33. **Archive from the editor** ✔ — bottom-bar/3-dot archive; note leaves home for the Archive page; Undo snackbar.
34. **3-dot ▸ Delete → trash, not oblivion** ◐ — editor delete moves to Trash (7-day) with Undo snackbar; current confirm-dialog behaviour replaced *(D5)*.
35. **3-dot ▸ Make a copy** ✖ — duplicates the note (content, colour, labels; not reminders) as a new note at the top.
36. **3-dot ▸ Send / Share** ✖ — share the note as plain text (title + body) through the Android sheet; image notes share the image; Keep's "Send to other apps" minus the Docs target *(adapt)*.
37. **3-dot ▸ Labels** ✖ — checkbox dialog of all labels + "Create new label" inline; multi-select; applied labels show as chips at the card bottom.
38. **Merge notes** ✖ — from multi-select 3-dot: concatenates selected notes (titles become first lines) into one new note; sources go to Trash with Undo.
39. **Convert text ⇄ checklist** ✖ — Keep can turn a text note's lines into checkboxes (and back); the plain-text storage rule (already ours) makes this lossless.
40. **Hide checked items (list notes)** ✖ — per-note toggle; checked items collapse into an expandable "N checked items" footer row — Keep's grocery-list declutterer.
41. **Customize list: themes & bullets (Keep, 2024)** ✖ — per-list picker of bullet glyph (check, dot, square, diamond, star…) and theme colour; renders on the card *(our dark-pill picker design per the calculator's round-32 rule: match the app's signature, never stock dialogs)*.
42. **3-dot ▸ Grab image text (OCR)** ✖ — Keep extracts an image's text into the note body so search finds it; on-device via bundled ML Kit *(decision D2)*.
43. **Timestamps: "Edited …" line** ◐ — Keep sorts by last-modified and surfaces edit time; show a subtle "Edited 27 Oct, 12:40" row at the editor bottom *(data already stored; display new)*.
44. **Adjustable font size (Small / Medium / Large)** ✖ — Keep's teased-then-shipped reading comfort; app-wide setting affecting editor + cards.

### D. Checklist working details (45–52)

45. **Checkboxes tick instantly; item text stays plain** ◐ — Keep does NOT strikethrough by default; checked items simply dim/hide per setting 40.
46. **Drag-to-reorder list items** ✖ — grip-and-move within the list; works on unchecked and checked alike.
47. **Nested / indented list items** ✖ — sub-bullets under any item (Keep's "nested items" for multi-step lists); indent/outdent controls; nesting depth shown by inset.
48. **Enter adds the next checkbox; empty trailing item auto-dropped** ◐ — the rhythm that makes Keep lists feel alive; saving a list with a blank last line drops it silently.
49. **"Hide checked items" footer** ✖ — (same mechanism as 40, listed here as the list-UX half: the collapsed row shows the count and expands on tap).
50. **Swipe-to-delete a checked item** ✖ — Keep Android lets a checked item be swiped away; with Undo snackbar *(verify against current Keep during build; ship behind the same gesture rules as the calculator: outcomes logged)*.
51. **Links and plain text as list items** ✔◐ — list items are ordinary text (URLs clickable); no rich formatting inside checkbox items (Keep's actual limit — formatting lives in text notes).
52. **Checklists stored as plain text** ✔ — our existing rule and Keep-compatible: search, export, share and the JSON mirror all keep working; checkbox state is a line prefix.

### E. Media: images, audio, drawings (53–62)

53. **Camera capture into a note** ✖ — system-camera intent, zero permissions *(ours: same pattern as the Scanner)*.
54. **Photo-picker / gallery attach** ✖ — Android photo picker (no permission on 33+; the suite's capped media-read covers below 33); copies into `files/notes-media/` so notes never break when originals move *(adapt — Keep references Drive photos; we own our copies)*.
55. **Multiple images per note** ✖ — Keep allows several; they stack in the note, thumbnails on the card.
56. **Image-first cards + full-screen viewer** ✖ — image on top, text below; tap → immersive viewer with swipe between a note's images.
57. **OCR: grab image text** ✖ — (see 42) recognized text appended to the body and indexed by search; on-device only *(D2)*.
58. **Voice memo recording in notes** ✖ — record button → waveform/timer UI → stop saves an audio chip *(D1: RECORD_AUDIO, the one permission Notes would declare)*.
59. **Audio playback with seek + card chip** ✖ — play/pause/scrub inside the note; card shows 🎙 + duration; recordings live in app-private storage (Recorder app's proven pattern).
60. **Automatic transcription of voice memos** ✖ — Keep's headline feature ("record a voice memo and have it automatically transcribed"). Cloud-based for Keep; offline needs on-device speech ML — **propose deferring** *(D3)*; the audio + manual text path ships regardless.
61. **Drawing canvas: finger + stylus** ✖ — Keep's drawing notes: pen and highlighter strokes, several ink colours, stroke widths, undo; saves as the note's image. Stylus pressure optional.
62. **Drawing = first-class card** ✖ — drawings render as image cards, searchable by label/colour like any note; "Save a drawing as a note" is Keep's own help-page flow.

### F. Organization: pins, colours, labels, archive, trash, reminders (63–74)

63. **Pin / unpin** ✔ — pin icon top-right in editor; pinned section on home; Keep's rule: pinned always first regardless of date.
64. **12 background colours + default** ◐ — Keep's palette (white/default, red, orange, yellow, green, teal, blue, dark-blue, purple, pink, brown, gray — 12 with default per current Keep); colour paints card, editor and viewer; **every colour contrast-guarded for dark AND light** (the calculator's round-20/33 lesson applied from day one) *(ours)*.
65. **Colour from multi-select** ✖ — bulk recolour several cards at once (see 86).
66. **Labels: create / rename / delete** ✖ — drawer → "Create new label" (Keep caps at 50; we keep the cap as a sanity limit); rename/delete from the labels editor; deleting a label removes it from notes but never touches the notes.
67. **Multiple labels per note** ✖ — Keep's folders-that-aren't-folders: one note, many labels; label chips at the card bottom; tapping a chip opens the label page.
68. **`#hashtag` quick labelling** ✖ — typing `#` in the body offers matching labels inline and creates new ones on the fly (documented Keep behaviour).
69. **Label pages with in-label search** ✖ — (pairs with 8) each label page is a full wall scoped to that label, with its own search field.
70. **Archive: out of sight, never lost** ✔ — archived notes stay searchable and editable; the Archive page lists them; this is Keep's "filing vs shredding" distinction.
71. **Unarchive** ◐ — one tap (editor or card 3-dot) returns the note to the top of Others, with Undo.
72. **Trash: 7-day window, restore, delete forever, empty trash** ✖ — (pairs with 10) the full Keep trash lifecycle, enforced locally: purge stamp checked on every app open; "Empty trash" asks once, then Undo-able for the session *(D4)*.
73. **Time & date reminders, native notifications** ✖ *(adapt)* — Keep moved these to Google Tasks and killed location reminders (Oct 2025); **we keep them fully in-app**: local reminder store, exact-alarm permission flow explained with consequences (the Clock app's proven pattern), notification with Open/Snooze/Complete actions. No account, no migration, works offline.
74. **Snooze & complete from the notification** ✖ — snooze re-arms (+10 min / +1 h / tomorrow shortcuts); complete clears the reminder chip; both logged as outcomes *(ours)*.

### G. Search (75–80)

75. **Instant search-as-you-type over titles + bodies** ◐ — Keep filters while you type. *Conflict to settle (D5):* the owner's earlier instruction made our search submit-based to kill per-keystroke rebuilds; Keep-grade instant search returns **only if** filtering happens off the UI thread and cards update in place — the recorder-blink lesson pinned by contract.
76. **Search sees OCR text and transcripts** ✖ — Keep's "grab the text from an image to help you quickly find that note again through search" — image text and (if D3 ships) audio transcripts are indexed like typed text.
77. **Filter chips: type** ✖ — Keep's search filters: Lists · Voice memos · Images · Drawings (documented filter set).
78. **Filter chips: colour** ✖ — restrict results to a background colour.
79. **Filter chips: label** ✖ — restrict to a label (plus the `label:`-style quick entry from the search field).
80. **Search-bar UX** ◐ — full-width rounded bar at the top of home; tap → search page with filter row; clear ✖ inside; back collapses to home; recent queries *(Keep shows recent searches on focus — include)*.

### H. Multi-select & bulk actions (81–88)

81. **Long-press a card → selection mode** ✖ — card lifts and shows a check; other cards selectable by tap; back or ✖ exits.
82. **Selection top bar** ✖ — "N selected" + close; action icons replace the normal top bar exactly like Keep's.
83. **Bulk archive** ✖ — with one Undo snackbar for the batch.
84. **Bulk delete → trash** ✖ — one Undo; the batch purges together after 7 days.
85. **Bulk label** ✖ — the label dialog applies to every selected note (adds, never replaces).
86. **Bulk colour** ✖ — palette row recolours all selected cards at once.
87. **Bulk share/send** ✖ — selected notes concatenated (title + body separators) into one share payload; image notes attach their images.
88. **Merge** ✖ — (pairs with 38) available whenever ≥2 notes are selected; the merged note opens immediately for editing.

### I. Widgets & system surfaces (89–93) — *integration round, host-level*

89. **Quick-capture widget** ✖ — (pairs with 20) resizable; four direct buttons; the suite's widget engine renders it (owner's standing rule: widgets re-placeable, resize-aware, 150×84dp min hints).
90. **Single-note widget** ✖ — pin one note to the home screen showing its real content (Keep's 2023 single-note widget); tap opens the note; colour and pin state reflected.
91. **List widget with live checkboxes** ✖ — tick list items from the home screen without opening the app; ticks write through to the note.
92. **Widget reconfiguration** ✖ — launcher long-press ▸ Configure re-enters Widget Studio for that instance (the suite's existing re-edit pattern).
93. **Reminder notifications as a proper channel** ✖ — named channel, exact-alarm flow with consequences explained, snooze/complete actions, survives reboot (the Clock's `EventReminderReceiver` re-arm pattern reused, not reinvented). *(Wear OS tiles: out of scope — no Wear target.)*

### J. Design language & UX polish (94–100)

94. **Material 3 Expressive look (Keep's Aug-2025 redesign)** ✖ — bolder typography, expressive shapes and buttons, springy motion — as a *design reference*, rendered in **our** signature: dark-pill popups, glass surfaces, GelMotion press feels (the calculator's round-32 rule: pickers and menus match the app's design, never stock dialogs).
95. **True dark mode on every surface** ◐ — editor, viewer, drawer, trash, widgets; palette-resolved like the rest of the suite; no white flashes (Keep's dark mode is exact-dark, not gray).
96. **Motion with outcomes** ✖ — card expand-from-position into the editor, staggered card entrance, FAB morph, snackbar slides; every animation logged where it gates behaviour (calculator lesson: animations must never swallow a state change).
97. **Contrast-guarded inks everywhere** ✖ — the calculator's 0.35-luminance guard applied to all 12 note colours × dark/light text: a colour cannot ship unreadable text (round-33 rule, reused wholesale).
98. **Honeycomb-family icon** ✖ *(ours)* — the standalone MyNotes and the in-suite Notes tile carry the honeycomb mark the owner chose forever, in Notes' accent.
99. **Locale-correct timestamps** ✖ *(ours)* — "Edited 27 Oct, 12:40" / "Tomorrow, 8:00 AM" rendered Indian-locale-correct (the calculator's grouping lesson: locale is a feature, not an afterthought).
100. **Offline-first, zero-account guarantee** ✖ *(ours — the advantage)* — every one of the 99 above works with no network and no sign-in, forever. Keep itself lost this in 2025 (reminders exiled to Tasks, location reminders killed, collaboration account-bound). Our pitch line: **all of Keep that a phone can do alone.**

**Tally:** ✔ 11 · ◐ 18 · ✖ 71. (Statuses re-verified against the 813 lines of existing code, §3.)

---

## 5. Decision points — the owner's call before building starts

| # | Question | Proposal |
|---|---|---|
| **D1** | Voice notes need `RECORD_AUDIO` — the first permission Notes would declare (repo policy is minimal permissions; Recorder already declares it, so suite precedent exists) | Declare it in MySpace (already granted for Recorder) and in standalone MyNotes only if voice notes are wanted there |
| **D2** | "Grab image text" (OCR): bundled on-device ML Kit text recognition (~a few MB, offline, Google/AndroidX-family — the repo's no-third-party rule needs this one exception) vs shipping without OCR | Take the exception — APK size is explicitly not a constraint (owner's rule), and OCR is half of Keep's search story (76) |
| **D3** | Automatic voice transcription offline: heavy on-device speech ML, poor quality/size tradeoff; Keep does it in the cloud | **Defer.** Ship recording + playback now; revisit if a good offline model emerges |
| **D4** | Trash retention: Keep Help says 7 days; some 2025 reports say the web moved to 30 | Pin **7 days** (the documented Android behaviour), stored in one constant so it can move in a line |
| **D5** | Instant search vs the owner's earlier submit-search order: that order fixed a real lag (per-keystroke view rebuilds) | Instant search **with** off-UI-thread filtering + in-place card updates, pinned by a contract that fails any rebuild-per-keystroke; if it ever lags, it reverts to submit automatically — his old order stays honoured in spirit (no lag) |
| **D6** | Manual drag-and-drop note order (4) vs pure chronological + sort menu (6) | Ship both, Keep-style: manual order default, Sort menu overrides — but manual drag is the single riskiest gesture in the list (the calculator's handle-drag saga is the cautionary tale); it goes last in its phase |
| **D7** | Standalone **MyNotes** host APK during development (like MyCalc) | **Yes** — it is the microservice guarantee made physical (§2.4): MySpace untouched until the integration round, and the owner perfects Notes on his phone first |
| **D8** | Delete-confirm dialog → Keep's trash+Undo model (34, 21) | Adopt trash+Undo (safer than a dialog, and it is what Keep does); the dialog goes away |

---

## 6. Build phases & test plan (MASTERCLASS §13 applied from day one)

**Phase 0 — Skeleton (isolation first):** `:notes` standalone host + qa isolation contracts (§2.1–2.2) + AppLog outcomes wiring + build stamp (`notes screen build X`) + geometry/state log lines + CI story-count gate wired for the Notes suite. *Proof: standalone APK installs beside MySpace; MySpace APK byte-identical.*

**Phase 1 — Keep's core loop:** items 1–3, 5, 7, 9–10, 12–15, 21–25, 33–34, 63–64, 70–72, 81–86, 95 (home wall, sections, drawer pages incl. Trash lifecycle, editor chrome + palette, pins/colours/archive/trash, multi-select bulk, undo snackbars, dark mode). *This phase alone makes it "Keep-shaped".*

**Phase 2 — Lists & text power:** 26–29, 39–41, 44–52, 30, 35–36, 87–88, 43, 99 (formatting toolbar + triggers, checklist working set incl. nesting/drag/hide-checked/customize-list, convert, links, copy/share, merge, timestamps).

**Phase 3 — Media:** 16–18, 24, 53–59, 61–62 (+57/42 OCR per D2, +76 search indexing of OCR text). Voice transcription (60) deferred per D3.

**Phase 4 — Reminders & search depth:** 11, 31–32, 73–74, 75, 77–80, 6, 4, 66–69 (labels full set, native reminders, instant search + filters, sorting, drag-order last per D6).

**Phase 5 — Integration round (host-level, one deliberate round):** 8, 19–20, 89–93, 98 + swap the new screen into MySpace's registry + full-suite regression across ALL apps (calculator stories must still pass unchanged — the freeze proof).

**Test machine (copied from the calculator, mistakes pre-paid):**
- **Layer 1:** qa source contracts — isolation greps (§2.1), menu/action counts, the 12-colour palette pin, trash-window constant, no-rebuild-per-keystroke pin (D5), version/stamp agreement, helper-import + brace + assert sentinels (already global).
- **Layer 2:** JVM journeys — `NotesRules` fixtures for every rule (checklist prefixes, nesting, conversion, merge order, hashtag parsing, purge arithmetic), ported to Python and hammered before UI exists (the calculator's port habit).
- **Layer 3:** instrumented end-user stories on the **standalone host**, written as outcomes (`aCheckedItemHidesBehindTheFooterRow`, `trashRestoresWithinSevenDaysAndPurgesAfter`), with the full helper glossary from day one: hold timers via injected dwell, text-center holds, **dispatch-first taps/drags** (the farm playbook §10 — window injection is presumed guilty until proven innocent), transcript-on-failure riding `AppLog.tail`, pixel census for every "visible" claim (colours, checkboxes, chips), debug-door pattern ready for any floating window.
- **Farm playbook pre-applied:** Nexus-5 profile pin, live logcat stream, two-door transcript pull, retry-once, annotations 1..6, story-count gate, and the two-reruns-then-instrument rule (MASTERCLASS §9.1).
- **Owner loop cadence:** every phase ships an APK link + TEST-GUIDE section table + candid status; every device log he shares gets read line-by-line with his own recent choices convicted first (rounds 27/28 posture).

---

*Prepared 2026-10-27 on branch `arena/3caca636-tictic`. The calculator remains frozen at MySpace 3.12.39 (76) / MyCalc 1.0.37 (38). Nothing in this document changes any code; it is the reference sheet the build starts from, and the owner's selections in §5 decide the deviations.*
