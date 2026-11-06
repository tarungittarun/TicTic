# Obsidian Android 1.15.0 — screenshot reference and explicit coverage gaps

## Open the reports

- [Features, briefs and actual screenshots](Obsidian-Features-and-Screenshots.pdf): **657 pages**, 656 evidence entries; PDF bookmarks group walkthrough, commands, settings and variants, with individual entry bookmarks.
- [Command coverage matrix](Obsidian-Coverage-Matrix.pdf): **6 pages**, every one of the **205 registered command IDs** accounted for.
- [Presentation/integrity review](presentation-review.json), [individual results](feature-results.json), [command ledger](command-coverage.json), [capture provenance](coverage-summary.json).

**The requested 100% functional coverage has NOT been achieved.** This is a substantial UI reference and a complete census of the inspected command registry, not a complete inventory/acceptance test of every behavior the app can offer. Counts of screenshots, hotkey rows and command entries must not be presented as counts of distinct fully tested features.

## Exact target and method

Official, unmodified `md.obsidian`, **1.15.0 / version code 396**, Android API 36 emulator. APK SHA-256: `2d92966fba5441a8976ae86b39cf138c0a845d3dcd318e6f0d29d304950bca5d` from the [publisher's release](https://github.com/obsidianmd/obsidian-releases/releases/tag/v1.15.0). No APK redistribution. Only public dummy notes in private app storage; no credentials, purchases, cloud uploads or community-plugin execution.

The inventory records **29 internal module definitions**, **28 exposed module controls**, and **20 settings-section overviews / 377 settings entries**, including 205 hotkey entries. Seven optional local modules were enabled through the real settings UI. The extra internal `editor-status` definition is not a separate exposed core-plugin toggle.

App commands, real CDP input and the app's file/editor APIs exercise the installed application. These are our inspection scripts, not an official Obsidian-owned test suite. Every evidence image originates in `adb exec-out screencap -p`; CDP compositor snapshots only synchronize painting and are discarded. Retained JPEG images were preserved byte-exactly during PDF review. No app screenshot was reconstructed, restyled or rotated to imitate a passing profile.

## Results — do not combine the categories into a pass percentage

| Evidence-entry classification | Count |
|---|---:|
| Narrow assertions passed | 31 |
| UI/control/entry inspected; semantic outcome not fully certified | 566 |
| Blocked or unverified attempted state | 59 |
| Total evidence entries | 656 |

The 31 narrow passes comprise vault availability, initial corpus file count, 27 saved-Markdown transformations, original-source preservation and cold-restart note retention. They do not prove every editor gesture or workflow. No failed-status entry was emitted; that is **not** proof the app has no defects. N/A is not separately assigned in this runtime ledger; desktop-only documentation and third-party extensions are outside this study, not silently passed.

The separate **205-command ledger** contains **27 functional readback passes, 129 UI-only inspections and 49 blocked/unverified entries**, with zero inventoried-only command IDs remaining. A readback can come from the independently captured formatting walkthrough; that does not certify the generic command invocation's entire follow-on workflow.

## Explicit gaps and blockers

- Paid/account-dependent Sync and Publish, external integrations, community plugins, physical devices and all settings-value combinations are not functionally accepted.
- Landscape requests remained **360×640 portrait**. Both requested-landscape note captures and their Canvas counterparts are blocked. Light/dark portrait views were captured.
- Template insertion encountered an unconfigured template folder; it is blocked, not successful insertion.
- Recording reached the real Android microphone-permission prompt and subsequent permission denial. Both recorder actions are unverified/blocked, not successful recording or audio-quality proof.
- The standalone properties-panel attempt remained obscured by tab overview and is blocked. Independent, unobscured **all-properties** and **file-properties** command screenshots are also included.
- Rendered footnote-selector detection and slash-command invocation were not established by this harness. Other actions needing different selections/context remain individually blocked.
- Several local workflows still lack semantic assertions, including search-result correctness, saved workspace restoration, bookmark persistence, recovery restoration, media/PDF embeds and diagram syntax. Their absence is a coverage gap, not evidence that the app lacks those capabilities.

## Honest run provenance

Walkthrough, settings and variants completed in run **42179340885**. Its command session lost its debug socket after **192 captured entries**; its native execution remains explicitly **incomplete** in the segment receipts. No app-crash/OOM cause is inferred from this socket error or post-teardown adb state.

Run **42182162974** collected the remaining **13** command entries in an independent fresh emulator. Composition validates the exact ordered registry, pinned APK digest, no duplicate IDs and all 205 IDs represented before declaring the **catalogue collection** complete. This is **not one uninterrupted 205-command execution**. The interrupted segment's final source-preservation check was not reached and is not inferred from the continuation. Original error/diagnostic receipts remain alongside the report.

Presentation review checked 140 representative native frames, not all 656 individually. Automated checks verified all 656 referenced images and one native image per screenshot page. The PDFs were regenerated locally from retained native JPEGs for conservative captions; this did not constitute another Android test run. Full PDF hashes and review scope are in `presentation-review.json`.
