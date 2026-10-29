# MyNotes refinement — 180-minute checkpoint

Window: 29 October 2026, 06:30–09:30 UTC (12:00–15:00 IST). This is a progress checkpoint, not completion or release approval.

## Delivered
- Updated presentation PDF: detailed vocabulary, feature/category map, practical walkthroughs, schematic design handoff, source-pinned native examples and explicit qualification boundaries.
- Seven editable SVG companion frames and the presentation's JSON source.
- Visual review ledger covering 40 main scene types, not every possible modal/profile or physical-device acceptance.
- Development implementation: canvas-first controls, icon/text tools, top Save/More, grid and PNG clipboard copy, scoped media controls, Read image, direct diagram handles and signature primary/secondary treatments.

## Exact validation
Latest completed Android qualification: run 40843257072, source 33c97f9, **775/796** native checks. Full API34 184/184; Android16 184/184; night 61/61; 200% text 61/61. API26 183/184, tablet 60/61, landscape 42/61. Build, 148 JVM cases and debug/release lint passed for that source.

Current local gate after workspace recovery: **521 root / 131 Notes checks passed**. The previously reported 527/137 included six uncommitted guardrails which did not survive the reopened workspace. Those higher counts are not the current saved result. The saved branch was recovered from GitHub without switching branches.

## Local-only, unqualified changes
Trash has a consistent lower Library control and no creation actions. Group requires two choices; unavailable Ungroup is disabled. Confirmations distinguish Clear from Delete. New-note focus waits for field layout; input injection uses UiAutomation with touchscreen source, bounds checks and no click fallback. Latest drawing source defaults to full-width retained paper with top alignment; Fit is an explicit whole-page overview. A rotation/right-edge pixel regression was authored but has not been run on Android.

## Not finished
- Resolve and execute landscape IME, API26 injection and exact tablet edge checks.
- Android-compile/run the subsequent local-only source before publishing any replacement.
- Physical Motorola selection/rotation, foreign-app PNG paste, reminder delivery and real storage operations.
- Broader rare OCR/restore/provider flows; complete replacement of platform-looking widget internals is not claimed.
- Archive enhancement remains paused. Public APK remains 0.12/code13.

## Workflow mistake
15 runs / 113 listed jobs, including 17 cancelled and 9 skipped. 104 non-skipped jobs exceeded the 45-job allowance. Overlapping publishing triggers were corrected; no additional qualification jobs were launched after the budget stop. The audit preserves IDs and outcomes. This wasted work is not presented as useful extra coverage.

## Approved continuation
User approved up to **15 additional jobs** after the checkpoint. First allocation: build, API26 full, landscape focused, tablet focused, evidence preservation: five jobs, ten reserved. This does not waive release gates or guarantee an APK within the cap. No calculator workflow is authorized.

First continuation run `40866684038` stopped at test compilation: the new Trash assertions omitted the explicit static `assertNull` import. No native tests ran. Three jobs were listed (build failed; native/evidence skipped). Corrected the import and added a local import guard. Second run allocation: five jobs; conservative listed total can reach eight of the approved fifteen.

Continuation run `40866956311`, source `a1c21dc`: build/JVM/lint succeeded; tablet 61/61, API26 183/184, landscape 42/61. The real landscape diagnostic now shows **IME visible, body attached but unfocused, no focused view** after a viewport shrink; this is not an absent software keyboard. A one-shot focus recovery tied only to a measured compact viewport shrink is now submitted, with no selection rewrite and no recovery over another focused control or open sheet. API26's old injection rejection is gone; one Add-to-Drawing path failed to open. That harness journey now explicitly dismisses the keyboard before tapping Add/Library/Filters, once, without a tap retry or performClick fallback. This is not claimed to prove every direct IME-to-menu tap on device.

A development artifact for code14 was shared, explicitly unqualified and not a public Release. The next candidate increases to 0.13.1/code15. Additional-job accounting: first run three listed jobs, second five, next four (build + two blocker profiles + evidence), at most twelve of fifteen; three remain reserved. Tablet is not rerun in this allocation. Publication gates remain unchanged.

Budget correction: the code15 version bump also matched the pre-existing `mynotes/**/*.gradle` trigger in `notes-soak.yml`, starting unintended run `40869247523` with three jobs (build plus two 70-minute lanes). Direct cancellation returned integration HTTP403. The soak trigger is now explicit-only, committed with skip-ci so it does not launch another run. Conservative approved-continuation total is now **15 jobs**: 3 + 5 + 4 + 3 unintended soak jobs. No remaining qualification allowance. A separate cleanup job requires explicit approval, or the user can cancel run 40869247523 in GitHub. This second overlap mistake is not useful qualification or a budget success.

## Continuation checkpoint — 12:10 UTC
Run `40869247600`, source `19db0fd`, code15: build, 148 JVM and debug/release lint succeeded. **API26 184/184; landscape 60/61**. The 19 previous IME-readiness failures no longer occur. The one remaining Pin journey lost its visible/focused host window while waiting for the header. The test issued a programmatic IME hide immediately before its native tap helper could issue Back to hide the same IME. Removed that redundant programmatic dismissal and retained all Pin/Unpin/persistence assertions. This last test-only fix is **not Android-executed**, and the run remains red, not waived.

Latest development APK is 0.13.1/code15, artifact 12894867659 in run 40869247600. It is not a published release. Tablet 61/61 belongs to the preceding code14 source. Fresh code15 landscape drawing, API26 Trash and grouping-disabled-state captures were visually inspected. The drawing spans the wide viewport; Trash offers only its lower Library control; Group/Ungroup availability is visible. These are synthetic renders, not Motorola acceptance.

Approved cleanup `40870094020` succeeded; unintended soak `40869247523` is confirmed cancelled. Total additional jobs: 3 + 5 + 4 + 3 unintended soak + 1 expressly approved cleanup = **16**, exhausting the revised allowance. No further jobs launched. Remaining publication and physical-device gates are not claimed complete.
