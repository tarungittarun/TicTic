# Content controls development handoff — MyNotes0.23.0 / code26

## Installable APK

- Download: https://github.com/tarungittarun/TicTic/actions/runs/41280119585/artifacts/13073121489
- Filename: `MyNotes-v0.23.0-26-development.apk` — uploaded as a raw APK (`archive: false`), not a ZIP.
- Size:66,721,736 bytes.
- GitHub artifact-metadata SHA256: `c519c731e715f1313db1d65e000e92cc1649af6c5d960c1b7ccf8daf93153d3d`.
  This is the GitHub-reported digest, not a claimed local download/hash verification.
- Artifact expiry:2026-11-08T06:18:04Z. Download before expiry; this is not a permanent public Release.
- Package `app.space.notes`; min26/target34/compile35 unchanged. Stable development signer verified
  by the Actions build: certificate SHA256 `08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`.
- **Install over the existing app. Do not uninstall or clear storage.** Export a backup first and export
  original voice recordings separately; document backups do not contain original audio or edit history.

## Qualified source and actual final results

- Source: `90319c2a406afd28b8c243f8d69383582ad8977a`.
- Run: https://github.com/tarungittarun/TicTic/actions/runs/41280119585 — **all5 jobs SUCCESS**.
- Source-pinned evidence commit: `252b1a74`; directory `design-2026-10-29/native/41280119585/`.
- JVM260/260; Android compile and debug/release lint passed.
- Full API26:286/286. Full API36:286/286, with app-owned animator scale1.
- Dark/200% API34:150/150. Landscape API34:150/150, both animator scale0.
- Four independent PDF export/URI/page-geometry validations passed.
- Every native report was independently parsed after completion: no missing/failing methods;
  new attached-view motion test and real text-input Enter/Backspace tests present in all profiles.
-81 source-pinned synthetic screenshots per profile; main app surfaces and revision controls reviewed.
  Exact inspected scenes and review boundaries are in `CONTENT-CONTROLS-VISUAL-REVIEW.md`.
- Local root QA625 passed, **including**235 Notes checks (do not add these counts).
- Calculator and `.github/workflows/tests.yml` unchanged from the previous delivered revision.
- Production inputs are unchanged from candidate3, which also independently passed all four profiles;
  candidate4 strengthened the input/motion tests and harness, without reverting any assertions.

## What changed

- Actual reported code25 List crash fixed at its cause: collapsed text selections insert a checklist
  at the caret; selected text still converts, with surrounding text and marks preserved. Domain
  selection conversion still rejects empty ranges; the UI selects the correct operation.
- Rich Doc content pens manage Rename/Copy/Cut/Delete, and open visual editing where appropriate.
  Table cells remain directly editable; its pen does not focus the first cell. Overflows own scoped
  table/list structure and placement, not duplicated clipboard management. This applies to all
  rich Doc inserted content kinds, not just tables.
- Whole-checklist/content Copy regenerates block, row and visual-reference identities without
  flattening rich payloads. Cut keeps its live source until a valid same-document paste commits.
  The structured clipboard is private app-process memory, not a durable or system rich clipboard;
  Copy can be pasted into another Doc. Cut across documents is explicitly refused without deletion.
- Four-dot item grips versus six-dot whole-content grips; hold-to-drag items, respectful haptic calls,
  lift, insertion marker and settlement feedback. No clicked Move item menu. Native tests exercise
  both real pointer movement and non-drag accessible row actions with exact rich-row/Undo assertions.
- One raised, outlined A Format dock: Clear fifth after Strike, Size/dropdown/steppers on one row,
  no Styles/More text options/duplicate Link subsection. Stored preset files and existing formatting
  are not deleted. Paragraph/font/size controls remain, with scroll/reflow for larger text.
- Literal typed numbering continues on Enter, with empty-entry escape and one-Backspace removal
  of the prefix. Both simple Notes and Doc are tested through actual Android InputConnection calls,
  including exact generated-prefix caret placement. Tables/checklist rows do not unexpectedly
  turn cell/item text into document numbering.
- Shared Notes sheets get short preference-aware entry transitions; control rows align icons and
  labels centrally. Simple Notes checklist hold feedback and numbering were updated while retaining
  its smaller editor model, rather than silently migrating Notes into Docs.

## Allowance and limits

The fresh40 allowance is closed at20 conservatively allocated slots:16 actual runner jobs executed;
4 reserved matrix slots never expanded after candidate1's build failure and were not recycled.
20 slots remain unused. Earlier allowances are separate and were not added.

This is a qualified **development artifact**, not a public-release approval, billing assurance,
novice usability sign-off, or a guarantee of zero bugs. Physical Motorola haptic feel, OEM keyboard
behavior, real-provider failure/power-loss cases and combined200%-landscape remain unclaimed.
Install-over testing on the user's phone is still decisive, especially the caret List action,
long-hold item drag and numbered-prefix Backspace that motivated this revision.
