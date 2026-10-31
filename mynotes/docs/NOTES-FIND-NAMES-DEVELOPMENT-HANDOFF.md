# MyNotes 0.20.0 (23) — Find, clean conversion and content names

## Candidate / qualification

Source `7e46d59e6a948c3a88b25391b0ea08a3d0e27173`.
[Qualification41189599678](https://github.com/tarungittarun/TicTic/actions/runs/41189599678).
**All software qualification gates PASS. Development APK, not physical/novice/public-release approval.**

[Raw signed development APK — MyNotes-v0.20.0-23-development.apk](https://github.com/tarungittarun/TicTic/actions/runs/41189599678/artifacts/13038830015)

- Raw APK, not a ZIP; GitHub sign-in may be required. Expires2026-11-07T09:42:21Z.
- 66,698,248 bytes. GitHub raw artifact SHA-256:
  `c79b6edebfff3bfbecca930b026758683b30f3ef46e7cd6c14e0d9b193ed86fd`.
- Package `app.space.notes`; version0.20.0/code23; min26/target34/compile35.
- CI verifies retained signer SHA-256:
  `08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`.
- Artifact digest is from GitHub; no separately downloaded sandbox copy/hash is claimed.

**Export a backup first, then install over the existing app. Never uninstall or clear data.**
Keep original recordings separately. **Do not downgrade:** real names require schema8;
older APKs reject it rather than losing names. Unnamed data retains existing schema6/7 output.
Backup wrapper format is unchanged; names are preserved in its document payload.
This is development qualification, not public-release/Play approval.

## Requested Notes changes

1. **More actions → Find in note.** Off-thread literal, case-insensitive search of title,
   writing, checklist items and displayed captions. Tap a result to select/jump; no saved
   highlight marks, replacement or content mutation.100-result/256-character query bounds.
   Compact accessible query/Close row, adaptive results and IME Search to dismiss the keyboard.
2. **Checklist options → Convert to text.** Generated checkbox markers disappear; indentation
   and line boundaries remain. User-authored brackets remain text. Undo restores check states.
   Sharing/export notation is intentionally separate from explicit conversion.
3. **Editable content names:** image/drawing actions → Rename; Rename diagram; reminder pencil
   → Rename reminder. Durable, bounded and undoable; preserved by storage/backups/copy/history.
   Blank resets the default; saving unchanged names is a no-op. Renaming does not change
   drawing kind, image bytes/identity, diagram shapes or reminder time.
   Reminder notification titles use their custom name while retaining private visibility.

## Doc continuation

- **Document → Find in document:** writing/title/checklist/table fields and displayed captions.
- Checklist caption → **Convert to text:** retains caption, indentation and rich row marks.
- Contextual reminder naming and visible name/date below the title.
- Read-only content captions remain visible/searchable; existing visual Rename preserved.
- Custom legacy image/diagram names are rendered as PDF captions, without rewriting pixels.

The prior rich Doc scope remains implemented. This is additional software work, not a claim
that physical novice acceptance was performed. Unrelated deferred Archive/Wall/Play/MySpace
work is not silently declared done. Calculator is unchanged; no cloud service was added.

[User guide](NOTES-FIND-NAMES-USER-GUIDE.md) ·
[Complete job/failure ledger](NOTES-FIND-NAMES-AND-DOC-FOLLOWUP.md) ·
[Original Doc acceptance](EDITOR-USABILITY-RESEARCH-AND-REDESIGN.md).

## Exact-source checks

| Gate | Result |
|---|---|
| Repository Python |579 PASS |
| Standalone Notes Python |189 PASS (subset, not additive) |
| Compilation |Android debug/release/instrumentation PASS; not just syntax parsing |
| Android debug/release/instrumentation compilation, lint/JVM |PASS;241 JVM |
| API26 portrait/100% full native |259/259 PASS |
| API36/Android16 portrait/100% full native |259/259 PASS |
| API34 dark portrait/200% font |125/125 PASS |
| API34 landscape/100% font |125/125 PASS |
| Independent PDF checks / complete provenance |all4 PASS; no missing profiles |
| Final-source visual inspection |Android16 writing and Find screenshots inspected |

No failure waived. Checkpoint1 passed257/257 API26 but dark/landscape123/124 failed the
reminder's accessible state-description mismatch; its full equality assertion stays.
Checkpoint2 failed instrumentation compilation on my missing assertion import; native
was skipped. The helper was corrected and a source import guard now covers all MyNotes
native/JVM tests. Both prior checkpoint evidence and job reservations remain in the ledger.

## Remaining acceptance and limits

- Physical Motorola/Android16 install-over-existing-data, restart, OEM/IME and novice trial.
- Novice tasks: Find a word and return to writing; convert checked/indented rows and Undo;
  name image/drawing/diagram/reminder and reopen; verify old Notes stay simple; use Doc Find,
  reverse-convert a rich checklist, edit/move content, Undo/reopen and inspect an exported PDF.
- Real reminder delivery, native selection/reflow, providers, ML accuracy, low storage,
  power-loss, performance and lifecycle behavior require real-device testing.
- Find is not image/audio OCR, a diagram-editor shape-label search or Replace tool.
- Backups still omit original recordings, private empty folders, history and saved styles.
  Private writes/external mirrors are not one cross-filesystem transaction. Doc is not Word import.

**26/30 newly approved job slots reserved**, including skipped native slots conservatively;
**4 remain unreserved**. Earlier unused6 from the previous10 are not silently added.
The189-minute uninterrupted streak is **not claimed or guaranteed**; no measured-work-hours
or billing claim is made. Automated passing alone cannot prove novice excellence.

### Final hardening
The preceding source8fc28925/run41187174320 was fully green:240 JVM,258/258 API26,
125/125 dark,125/125 landscape and all3 independent PDFs, with complete provenance.
Final source avoids copying complete canonical image JSON during metadata validation.
Long captions use at most2 display lines (full names remain stored/searchable/accessible);
the native drawing journey uses a139-character name and full-target native action taps.
PDF custom image/diagram captions wrap rather than truncating the name. Search shows “1 match”.
Checkpoint4 also passed all3 native/PDF profiles. The recording-aware Notes restore review
then found and fixed a missing metadata copy. New JVM/native tests exercise names through
legacy/canonical backup codec and recording-link conflicts. Checkpoint5 was cancelled by
workflow concurrency; all5 slots remain counted. The current source requires4 native/PDF
profiles, adding full Android16; preceding green evidence is not substituted.


## Final receipt
All4 profiles and independent PDF results were inspected from
[exact-source evidence](design-2026-10-29/native/41189599678/provenance.json).
The4 profiles above are separate; this does not qualify combined200%-font landscape.
Actual Android16 writing and Find JPEGs were opened and inspected. The compact Find
sheet visibly shows its query,48dp Close, “1 match” and “Writing” result; writing remains
above the keyboard with insertion/format/list/Labels/Colour controls.
The new restore regression passes on both API26 and API36; no failure was waived.
No further qualification run is reserved.26/30 conservative slots,4 unreserved.
