# OurApp_vs_Obsidian — all-module evidence review

## Results

**[Download/open OurApp_vs_Obsidian.pdf](OurApp_vs_Obsidian.pdf) — 95 pages.**

The expanded report contains **37 functional-area chapters**, covering the identified standalone **MyNotes 0.69.0/code69** surfaces and **all 29 internal Obsidian Android 1.15.0/code396 module definitions** in the retained registry (28 exposed core controls). It also preserves the earlier 39-page, 20-finding comparison of finding/organising and editing/reading as a clearly labelled appendix.

**All-module inclusion is not complete fresh hands-on testing.** This is visual expert inspection of retained native screenshots plus bounded source/provenance review. No new live/device/participant session, matched latency/jank study or runtime-bug reproduction was performed. Chapter tasks are explicitly **NOT RUN**. This does **not** complete the user's request for proper fresh hands-on testing. Automated app passes and document checks are not UX scores. No runtime app fixes, Android builds, installs, training, logins or uploads occurred.

### Scope

- Find/organise/search/reorder; Notes/Doc writing; lists/checklists/numbering; tables; images/attachments; web and internal links; backlinks/graphs/previews; heading/tag/footnote/count context; properties/Bases; daily notes/templates/unique/random notes.
- Drawing/Canvas; diagrams/shapes/arrows; PDF navigation/search/selection/markup; Word; large TXT; CSV/XLSX; safe extraction of other supported formats; OCR; recording/playback/transcription.
- Reminders; home widget/share-in/OS entry points; appearance/accessibility; storage/onboarding/optional App folder; backup/restore/import; Trash; Archive/pinning/bookmarks; Undo/history/saving; mobile tabs/workspaces; Settings/command discovery.
- Export/share/merge/copy; local summary; privacy/permissions/diagnostics; performance/footprint/reliability; core/community plugins; Sync/Publish/account boundaries; Slides; About/models/licences/help.

Shared user jobs are compared, not feature totals. Non-equivalent specialised capabilities are identified and receive no extra-feature credit. Account/payment-dependent services, community-plugin execution, desktop-only/documentation-only capabilities, future versions and physical-device acceptance are not silently passed. All 205 retained commands and 20 settings-section overviews are routed for traceability, **not claimed as individually accepted UX workflows**.

## Supporting results

- [Text edition](OurApp_vs_Obsidian.md): all 37 module chapters with evidence, trade-offs, direction and unexecuted manual acceptance tasks.
- [Structured all-module review](all-modules-review.json): module chapters, source anchors, exact retained core registry and command/settings routing.
- [Evidence manifest](all-modules-evidence-manifest.json): 104 catalogued source frames, original paths/dimensions/SHA-256 and referenced implementation-source hashes. Native image files are reused in place, not duplicated into this directory.
- [Document integrity](report-integrity.json): continuous page count, module page locations, chapter completeness, text bounds and PDF SHA-256. These checks do not establish app usability.
- [Original phase-one PDF](OurApp_vs_Obsidian_Phase1.pdf) and [text](OurApp_vs_Obsidian_Phase1.md): retained separately; `review-data.json`, `evidence-manifest.json` and `phase1-report-integrity.json` describe that historical installment.

## Regeneration

Dependencies: Pillow, ReportLab, PyMuPDF. From repository root:

```sh
python mynotes/tools/comparisons/ourapp_all_modules_report.py
```

The builder checks all source paths/anchors, hashes inputs, routes the complete retained core registry and verifies every module chapter fits on one complete page. It appends the retained phase-one PDF. To rebuild that historical standalone report first, use `ourapp_ux_report.py`; it now writes the `_Phase1` filenames rather than overwriting the expanded deliverable. Phase-one PDF bytes may change on regeneration due to PDF timestamps; its retained historical integrity receipt describes the published original.

Source/provenance inspection baseline for expansion: `95471afb28704ee084e3cbd7c82f54aeb648425a`. MyNotes native capture run42021523354, app sourcea014e015731103a041b092a0e3c2802ac2805d3a. Obsidian native capture42179340885 with retained command continuation42182162974. Different fixture content, environments and keyboards prevent a defensible live speed or task-success ranking.
