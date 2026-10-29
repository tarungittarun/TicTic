# Additional qualification allocation

User approved **up to 15 additional GitHub Actions jobs** after the proposal to build/test/fix the unfinished APK. This is a job-count ceiling, not a monetary budget or a six-hour background-work commitment.

## Attempt 1

- Trigger: deliberate workflow-path update on `arena/3caca636-tictic`.
- Scope: current code16 source, including image editor, checklist actions, grouping/OCR improvements and the unresolved Pin journey.
- Reserve: **4 jobs** — build/JVM/lint, full API26 instrumentation, focused API34 landscape instrumentation, evidence preservation.
- Remaining unreserved: **11 jobs**. Actual started/listed/skipped job accounting and run ID will be recorded once GitHub creates the run. No unrelated soak or publication workflow is requested.
- No public release unless required gates pass; physical-device acceptance remains distinct.

The earlier 15+1 allocation is historical and is not counted again against this newly approved cap.

## Attempt 1 results — run 40959615761

Source `e27afd12eff2b33a91723ba83b5192dc56961046`. **4 jobs started, 4 completed; 4/15 used.** Build, JVM suite and debug/release lint passed. API26 189/190; landscape 66/67. Sole failed case on both profiles: OCR review journey. Pin and the three new image/checklist cases passed. Reports/captures preserved by evidence commit `09410bf`.

The OCR case used the legacy `click` helper on a note title: it performs a click on the TextView, while NotesBoard opens the note through its card container. The following Add action then was absent. No language/review screenshot was reached. Correct this case to native coordinate taps (including card entry), preserving all OCR mutation/blank/cancel assertions; add missing-control names to legacy helper diagnostics. This is a test-entry correction, not a waived OCR failure or claim of recognition accuracy.

The built APK artifact exists (12935431340), but local download failed with EOF at the artifact storage host. No direct APK is delivered or Release published.

## Attempt 2

Reserve another **4 jobs** for rebuild, API26 full, API34 landscape focused, evidence. Maximum cumulative usage after this attempt: **8/15**, leaving **7**. Deliberate workflow-path trigger only.

## Attempt 2 results — run 40961354160

**4 further jobs completed; cumulative 8/15 used.** Build/JVM/lint passed; landscape **67/67 passed**, API26 **189/190**. The corrected OCR test now enters the review; native dispatch exposes an actual baseline-layout clipping issue on API26: Append button top offset 17px inside a row whose height already equals the button's height. No assertion was waived. Source fix disables baseline alignment and centers controls vertically in shared action rows.

## Attempt 3 — revised reservation

Reserve **5 jobs**: build/JVM/lint, API26 full regression, API34 2.0-font dark focused, API34 tablet focused, evidence. Maximum cumulative **13/15**, leaving **2**. Do not launch an unrelated workflow. Add explicit synthetic crop-preview and checklist-item-action captures (registry 54, bounded cap remains 56).

A separate raw-file upload uses the official upload-artifact v7 `archive: false` support. This will provide an authenticated **installable APK artifact, not a ZIP**, if the upload succeeds. Verify the established signing-certificate SHA256 before uploading; retain file SHA256 and signer receipt. It is a development artifact, not a public Release or full acceptance.

## Attempt 3 final results — run 40962976726

Source `9ed1ca4c5474d8505d6390ba45b8d616254fe4b0`; all **5 jobs succeeded**. Cumulative new allocation **13/15 jobs used, 2 unused**. No further job is started by this handoff.

- Build, JVM inventory, debug/release lint and established signing-certificate verification passed.
- API26: **190/190**; API34 dark/2.0-font: **67/67**; API34 tablet: **67/67**.
- Source-pinned synthetic reports/captures preserved by commit `65c6446`. Newly captured crop preview and checklist actions are present on all three profiles.
- Raw development APK artifact **12937000778** successfully uploaded with `archive: false`, named `MyNotes-v0.13.2-16-development.apk`, 66,605,116 bytes, expiry 2026-11-05T22:06:03Z. No public Release. See DEVELOPMENT-APK-0.13.2-16.md.
- Latest-source landscape is not represented by the prior source's 67/67 pass. Full matrix, physical acceptance, remaining typing history and broad refinement remain open.

Local handoff gates: **543 root / 153 Notes Python checks passed**. These counts are separate from native execution.
