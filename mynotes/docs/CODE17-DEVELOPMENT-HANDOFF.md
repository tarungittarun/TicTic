# MyNotes 0.14.0 (code17) — partial development handoff

**Not completion of the 13-item upgrade and not public-release approval.**

## Installable artifact

- Raw APK: [MyNotes-v0.14.0-17-development.apk](https://github.com/tarungittarun/TicTic/actions/runs/41008698148/artifacts/12955898791) — GitHub sign-in/access required; not a ZIP.
- 66,614,556 bytes; artifact expiry 2026-11-06 05:31:12 UTC.
- SHA-256: `e45d33aa6e0ed00383e01b634c0b40eab476f4e6b1bd5704e519f5da30f4c039`.
- APK source: `260eac00946ab383951277960966d7a8104ca4e5`.
- Package `app.space.notes`, stable signing checked by the build; higher versionCode17. Schema6 unchanged.

**Back up first. Install as an update over the existing signed MyNotes app. Never uninstall or clear app data to install this candidate.** If Android reports a signature conflict, stop; do not remove the old app. A selected notebook folder is not a substitute for a separate verified backup.

## Request-by-request status

| # | Requested change | This candidate |
|---|---|---|
| 1 | Word-like ordered text/visual content | Pending. Legacy body/checklist/media separation remains. |
| 2 | Tables and hyperlinks | Pending. |
| 3 | Editable visual names and numbered defaults | Pending; legacy Drawing dispatch must be migrated safely first. |
| 4 | Optional writing lines | Partial: Background → Ruled now follows native text baselines in the editor, not a fixed grid through images. No separate new writing-lines switch; PDF alignment is not qualified. |
| 5 | Font, size and richer/custom formatting | Pending; existing formatting remains. |
| 6 | Proper text Undo/Redo | Partial: per-field session history for title/body/checklist text, native context actions, formatting/selection snapshots, bounded 50 steps/500k retained characters per history. Resets on navigation/recreation; not persistent document-wide history. |
| 7 | Mixed text/checklists; convert selected text only | Pending. Conversion is still whole-note. |
| 8 | Handles before checkboxes; remove redundant row commands | Implemented and native-tested: real handle drag and accessible position chooser; redundant Add item/Move up/down removed; + List item retained. |
| 9 | Document-only overflow/contextual content controls | Partial: checklist commands moved to the left Checklist options control; item indent/outdent/remove are contextual. Whole-note conversion remains in Note actions; not a completed mixed-content toolbar redesign. |
| 10 | Numbered paragraphs/content | Pending. |
| 11 | Saved note's file location | Implemented: private multi-note library versus verified provider document; missing/stale copy disclosed, no file creation to display a location. |
| 12 | File-manager drop/Open with | Implemented bounded compatible-MyNotes-document intake and explicit review/merge; not Word import or editing arbitrary files in place. Provider/file-manager physical-device acceptance remains pending. |
| 13 | Discoverable opening and simpler folders | Partial: Library → Open note file, clearer Open library/Choose folder explanation and app-only Folders setup path. Full storage UX/device-provider qualification remains pending. |

## Data and security boundary

No schema migration, attachment rewrite, added Internet permission, cloud/LLM service or model downloader in this batch. External intake is permission-granted content-URI only, with provider allowlisting, bounded reads and existing non-destructive merge/recovery. Opening does not directly overwrite the source file. External drops are routed before editable children can insert their URI as text. Busy/review/visual/OCR/recording drafts block intake. Temporary drag grants are released after reading or on close. File-location data and raw uploaded diagnostics are not added to logs or committed.

These controls are not a claim that every provider or malformed-input case has been independently penetration-tested. Use compatible local MyNotes exports, preserve backups, and complete physical acceptance before relying on external document handling.

## Qualification

Final [run 41008698148](https://github.com/tarungittarun/TicTic/actions/runs/41008698148): **5/5 jobs passed**. Build/JVM/lint/signing passed; API26 **197/197**, API34 dark at 2× font **74/74**, API34 landscape **74/74**. Exact inventories checked, no skipped/duplicate outcomes. Actual input-dispatched row drag now passes, including text/checked/depth/identity preservation and Undo; earlier failed probes are retained in the upgrade ledger, not reclassified as passes.

Local source suites before runtime push: root **551**, Notes **161** passed. Native source-pinned reports/synthetic screenshots: `design-2026-10-29/native/41008698148/`, evidence commit `2ec10d0`. Synthetic scene filenames such as `mixed-note-header` do not imply ordered mixed-document support.

**17/17 authorized Actions jobs used. No further qualification jobs or public release are authorized by this allocation.** Documentation-only handoff uses `[skip ci]`. Physical Motorola acceptance, actual third-party file-manager drop/Open with, PDF baseline alignment and process-recreation behavior are not proven by this matrix.

## Suggested physical acceptance (backup first)

1. Update without uninstalling; inspect existing long notes, checklist checked states/depth, images, drawings, diagrams, labels, folders, Trash and voice recordings.
2. Type and format, use text Undo/Redo, then leave/reopen to verify saved content. History reset on navigation is expected; lost saved text is not.
3. Drag the handle before a checkbox through several rows; verify checked state/depth/text remain attached to the item. Undo the move. Test the accessible position chooser too.
4. Select Background → Ruled; inspect wrapped lines with the keyboard and current device font scaling.
5. Open a compatible local exported document via Library and then file-manager Open with. Review and cancel once before confirming a merge; original source must stay unchanged. Unsupported cloud, oversized and arbitrary non-note files must not silently import.
6. Inspect File location. App-only storage must clearly describe a shared private library, not invent a separate note file. With no selected folder, Folders should explain setup rather than show a misleading read error.

## Remaining work and time accounting

See `DOCUMENT-FORMAT-MIGRATION-PLAN.md` for the unimplemented canonical block-model migration and its persistence/search/export/merge/recovery gates. A design document is not implementation. Existing notes must not be flattened to enable a new toolbar.

The requested **180 minutes of actual work has not been met or claimed**. Passive CI waits are not counted as actual work; no promise of after-turn/background development is made. This is an honestly bounded partial checkpoint with an installable candidate, not the full requested delivery.
