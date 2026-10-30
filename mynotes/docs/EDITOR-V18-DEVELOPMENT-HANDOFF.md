# MyNotes v18 development handoff — implementation and automated qualification complete

## Installable APK

[Download MyNotes-v0.15.0-18-development.apk](https://github.com/tarungittarun/TicTic/actions/runs/41024603014/artifacts/12962923344)

This artifact is the **raw APK, not a ZIP**. GitHub access to the repository is required. It is a development build, **not a public-release approval**.

- Version: **0.15.0 / versionCode18**, package `app.space.notes`.
- App/instrumentation source: `8d225ec405ab1773d14140b3778ec26087b5e240`.
- Size:66,659,248 bytes.
- SHA256: `c555c5a630685844c0b3c2b18f4804a84eb762aa3d5c726486bfdb9470e527db`.
- Build verifies the existing signer SHA256: `08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`.
- Artifact expiry: **6 November2026,08:11:36 UTC /13:41:36 IST**.

**Export a backup before updating. Install over the existing app; do not uninstall or clear its data.** Once notes use schema7, older APKs cannot read them. Keep an independently exported backup of important originals.

## Where the new editor is

For an existing legacy note: **More actions → Document editor → Enable editor**. This is deliberately an explicit, verified migration, not a silent rewrite. The first original-library checkpoint is available under **Backup & restore → Before document editor**. After a successful first upgrade, newly created notes use the document editor. Other existing notes remain unchanged until explicitly upgraded.

Tap a writing position and use **Insert**. Use a section's left-hand grip to move it; tap the grip/name for contextual actions. Select only the desired writing, then use **Selection → checklist**. **Format** controls the selected body, list or table-cell text; note titles remain plain. In narrow or2×-text layouts, contextual controls scroll horizontally.

## The13 requested feature areas

| # | Implemented behavior |
|---|---|
|1|Ordered text, checklists, tables, images, drawings and diagrams in one document. Actual caret-position visual/table insertion; editable writing before/between/after visuals. Section handles support native dragging and edge autoscroll without flattening content.|
|2|Editable rich-text table cells; insert/delete rows and columns with Undo. HTTP/HTTPS hyperlinks retain their selected labels and require confirmation before opening an external browser.|
|3|Editable visual/section captions, with new Image01/Image02, Drawing01 and Diagram01-style defaults. Existing captions are retained during migration.|
|4|Independent persisted writing-lines switch; it does not replace or erase the selected paper background.|
|5|Bold, italic, underline, strike, heading1/2, normal paragraphs, four platform font families and selected-text custom sizes8–72sp. Formatting persists in text, checklist rows and table cells.|
|6|Session text/structure Undo and Redo, including native text-menu integration, caret restoration, typing groups and separate style/newline operations. History is bounded to50 steps and a12Mi-character sharing-aware budget; it is not permanent history across reopening the app.|
|7|Text and checklist sections coexist. Only selected text converts; surrounding writing, formatting, row identity, checked state and indentation are retained.|
|8|Checklist grips precede checkboxes. Redundant Add item/Move up/Move down controls are absent; **+ List item** remains. A grip tap exposes contextual indent/outdent/remove actions.|
|9|The bottom **Document** action is document-wide; insertion, formatting, section/row and visual editing are contextual rather than mixed into that overflow. Labels and Colour also have contextual-toolbar access.|
|10|Numbered paragraphs, with tested numbered Enter, newline deletion/joining and heading-to-normal continuation.|
|11|**File location** explains the actual folder copy or app-private multi-note library file; it does not pretend that app-only notes each have a separate visible file. Location details are not diagnostic logs.|
|12|Open with/document-level drop for compatible, bounded, permission-granted local MyNotes files. Editable writing fields reject URI/Intent coercion. This is **not arbitrary Word/DOCX import**, and not every third-party/cloud URI provider is accepted.|
|13|Discoverable Open note/Open folder routes and clearer folder setup/settings, retaining real selected-folder moves and recovery behavior.|

This is a structured Android note editor, not a desktop Word engine. Named custom style presets, caret-only pending font presets, clickable PDF link annotations and printing the optional writing guides are not claimed. PDF export preserves mixed-content ordering and rich text/table pagination, with printed link destinations.

## Actual qualification results

Run: [41024603014](https://github.com/tarungittarun/TicTic/actions/runs/41024603014).

| Gate | Result |
|---|---|
|Root Python checks|552/552 PASS|
|MyNotes Python checks|162/162 PASS|
|Exact domain JVM inventory|204/204 PASS|
|Debug/release compilation, lint, APK signing/signer gate|PASS|
|API26, full native inventory|**220/220 PASS**|
|API34 landscape, focused native inventory|**86/86 PASS**|
|API34 dark theme,2× system text|**86/86 PASS** in authorized replay41033973969, using unchanged v18 app/instrumentation sources|
|Three-profile synthetic evidence preservation|PASS, integrated into API26 job; source-pinned reports/screenshots committed as `1c2619c`|
|Motorola edge50neo / Android16 physical acceptance|Not performed; still required|

The first dark/2× attempt failed36/86 because a **Pixel Launcher ANR** overlay stole window focus. The original failed report, screenshot and environment diagnostics remain preserved, not relabelled as passes. The user authorized exactly one extra job. [Replay41033973969](https://github.com/tarungittarun/TicTic/actions/runs/41033973969) reused the existing compiled APKs, verified zero non-documentation MyNotes source differences from8d225ec, preserved preflight diagnostics and force-stopped the external Pixel Launcher. Its exact inventory passed **86/86**, with no missing, unexpected, duplicate or failed methods. Evidence is committed as `9c1d7a5`; provenance separately identifies app/APK source8d225ec and harness source6cb80a2. Representative dark/2× keyboard and formatting captures were visually inspected. No app code, data permission or test assertion was changed for the replay.

**All three planned automated profiles now pass for the same application/test sources.** API26/full and landscape results remain source-pinned to41024603014; dark/2× results are source-pinned to41033973969. This completes automated qualification, not Motorola physical-device acceptance or public-release approval.

Actual native tests now verify table/image caret insertion, fonts/size/links, rich-row and section dragging, reverse anchor/focus before Document/after Colour/after controller recreation, focused landscape writing above the IME, numbering, separate diagrams and rich mixed-content persistence.

## Data and security work

- Canonical IMAGE/DRAWING pixels now use the existing immutable, content-addressed, hash-verified media blobs, including cross-kind deduplication. They are no longer copied inline into every new generation. Earlier inline canonical format2 development records remain readable; no format/version substitution strands them.
- Native tests verify real granted-SAF PNG intake, actual folder moves, two generations/one shared media blob, rich table/link/checklist/order preservation, corruption rejection, older inline snapshots and explicit OOME recovery failure.
- Migration verifies a source-pinned round trip and keeps the first original checkpoint before schema7 saving. Local recovery precedes external-folder writes. No old generations or user files are pruned to make this pass.
- Strict UTF8/Unicode, structural/schema/type/mark bounds, nested diagram validation and local URI gates remain. Unsupported canonical fields are rejected rather than silently discarded. No cloud/LLM/Internet permission was added; existing offline OCR/ASR models were not replaced.
- Source-file OOME conditions become explicit recovery errors, **not empty-library initialization**. This does **not** prove bounded total selected-folder history, private-file reads or total I/O. Duplicate JSON-key rejection is not independently enforced. Backup intake/export remains8MiB; oversized exports are refused rather than truncated.
- Document bounds include512 sections,2048 editable fields,16384 marks and a2Mi text/style budget; diagrams additionally have1024 nodes/4096 edges per document,4096-character labels and256-character identities. Visual/blob limits remain enforced. Normal limits are not claims of arbitrary-size document support.

Calculator is unchanged; Archive remains paused and Wall drag remains last. No180-minute actual-work total or physical-device acceptance is being claimed.

## Remaining acceptance and final job count

The authorized single-job replay has completed successfully. Final count: **26 individual GitHub job IDs:20 started and6 skipped**, including the one explicitly approved extension beyond25. The replay used exactly one job, without a rebuild or separate evidence runner. No further job has been launched or assumed authorized.

The APK download, version, SHA256 and signature above are **unchanged**: no reinstall is needed if this exact v18 build is already installed. All13 feature areas and planned automated checks are complete within the documented scope and limits. Physical acceptance on the Motorola edge50neo/Android16 remains unperformed and decisive; this remains a development handoff, not public release approval.

For phone acceptance: back up before upgrading; enable the document editor on a copied/sample legacy note; insert a visual/table between text; style a selection; convert only selected lines to a checklist; reorder rows/sections; test Undo/Redo, rotation, keyboard visibility and reopen; then verify Open note/Open folder, File location and an exported/restored backup. Do not uninstall or clear app data to test this.

The single-profile replay workflow is retained for reproducibility, not authorization to launch another job. The full four-job/three-profile workflow remains archived at `mynotes/docs/ci/notes-editor-three-profiles-template.yml`.
