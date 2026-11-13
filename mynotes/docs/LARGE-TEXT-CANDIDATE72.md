# Candidate72 — targeted large-text, image-selection, and workspace qualification

Candidate `0.72.0` / versionCode `72` passed the targeted Android-emulator qualification described below. The source and instrumentation changes are on the active branch; this record does not approve a public release or physical-device acceptance.

## Candidate changes

- **Library sheet:** portrait at font scale 1.5× or above can use the available screen height rather than the previous 560dp cap. Settings remains discoverable without reducing text or touch-target sizes; overflow remains scrollable.
- **Image text selection:** portrait selection details and secondary tools use a bounded scroll area. “Use selected text”, “Keep selected image”, and “Read this area” remain outside the scroller, and the layout budgets for the image preview.
- **Repeated Notes/Doc navigation:** workspace/card settling is diagnosed at each stage. The repeated creation journey now checks that each new note or Doc is still persisted in its intended workspace after Back, in addition to its workspace, content-kind, and IME-readiness checks.

## Targeted GitHub Actions result

Run [43404767000](https://github.com/tarungittarun/TicTic/actions/runs/43404767000) tested source commit `0f85176b704122cc785aef0273d9ff447cf4e7fe`. The build job and all four configured native-emulator jobs completed successfully:

| Profile | Configuration | Result |
| --- | --- | --- |
| `api26-portrait` | API 26, portrait, font scale 1.0 | Pass |
| `api34-large` | API 34, portrait, font scale 2.0, night mode off | Pass |
| `api34-landscape` | API 34, landscape, font scale 1.0, night mode on | Pass |
| `api36-portrait` | API 36, portrait, font scale 1.0 | Pass |

Each native job also passed **“Reject absent or partial assertion results.”** That step runs `verify_recognition_pilot` against the instrumentation output. Its exact-result parser rejects missing, duplicate, unexpected, or failed methods; the workflow selects the methods below on every profile:

- `CoreJourneysTest#notesAndDocCreationRemainSeparateEvenAfterAnOldOptIn` (FQ-0003)
- `ImageSelectionTest#selectionViewportRetainsUsefulAreaInPortraitAndLandscape` (FQ-0004)
- `CoreJourneysTest#wholeAppLibraryTraversalRecordsEachVisitedPage` (FQ-0005)

The workflow also selects its other named search, formatting, adaptive-editor, and environment-qualification cases. This is a targeted regression set, not the full native inventory.

### Acceptance covered by the selected cases

- **FQ-0003:** repeated Notes↔Doc navigation and creation remained separate, workspace/card and IME readiness checks passed, and the test verified saved notes and Docs after each fast Back. For Docs it checks saved document content; for notes it checks the saved note remains non-document content with its title and body.
- **FQ-0004:** the existing requirement that the image preview occupy at least **35%** of the dialog height remains in the test and passed. In API34 portrait at font scale 2.0, the test also requires bounded secondary details, fully visible primary actions, 48dp targets, and unellipsized action labels.
- **FQ-0005:** in the same API34 large-text portrait profile, the existing initial-Library assertion that the full Settings row is visible passed; Settings remains last and Folders penultimate in the panel.

The API34 font-scale-2.0 profile ran with night mode off. The historical FQ-0004/FQ-0005 observations were recorded in dark mode, so this run does not claim an exact dark-theme reproduction; the layout assertions passed in the configured Candidate72 profile.

Machine-readable run/job/artifact metadata and the receipt-verification scope are recorded in [`full-qualification/43404767000/summary.json`](full-qualification/43404767000/summary.json). The per-profile assertion artifacts are retained by Actions. Direct ZIP retrieval of the API34-large artifact with `gh run download` returned EOF in this environment, so its JSON payload was not independently opened locally; the recorded per-profile assertion scope is established by each successful exact-result gate and the checked-in workflow/verifier, not by a claim of local artifact extraction.

## Limits

This result qualifies the targeted changed-source checks on the four configured emulators only. It is not full-inventory or physical-device acceptance. The workflow has no publication job, and no release APK was published.
