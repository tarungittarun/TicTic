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
