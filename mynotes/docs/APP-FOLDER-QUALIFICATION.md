# App folder / Open file qualification ledger

Independent from the previous closed 40-job speech/OCR research follow-up. No new model
weights or training. Android builds remain Actions-only. The user lifted the job cap;
count actual executed jobs, not skipped jobs or hypothetical matrix slots.

| Round | Source | Run | Actual jobs | Result |
|---|---|---|---:|---|
| 1 | dc024ab1 | 41344031614 | 1 build | Failed runtime compilation: lambda parameter shadowed a local name. 279 JVM tests passed; 8 native jobs skipped. No qualified APK. |
| 2 | 86975fbc | 41345739995 | 1 build | Failed runtime compilation: staging space-check cursor declaration missing after formatting. 279 JVM tests passed; 8 native jobs skipped. No qualified APK. |
| 3 | 168ff446 | 41345956856 | 9 executed | Build/lint and 279 JVM passed; all 8 native jobs failed, 1074/1082 passed. Two new test defects (four profiles each): wrong Find label and reading preview before merge completion. Source-pinned reports preserved by 533bb11c. No qualified APK. |
| 4 | a8dd1d8d | 41347397975 | 9 executed | Build/lint and 279 JVM passed; 7 native jobs passed, 1 failed. 1085/1086 methods passed. New category journey passed API26/API36/2x-font; landscape asserted 48dp on an accessibility rectangle clipped by scrolling before revealing the full row. Evidence 3b16150b. |
| 5 | cb556543 | 41348954464 | 1 build | Test compilation failed: SHOW_ON_SCREEN is an AccessibilityAction, not a legacy AccessibilityNodeInfo integer constant. 279 JVM passed; 8 native jobs skipped. Product app source unchanged from round 4. |
| 6 | 17febd34 | 41349116962 | 9 executed | SUCCESS 9/9 jobs: debug/release build and lint, 279 JVM, 1086/1086 native methods, exact inventories with no missing/duplicate/failing outcomes. All four category-dialog journeys passed; full visible 48dp targets and save/cancel/consent semantics verified. Evidence e25a167d. |

**Closed: 30 actual jobs**, independently counted from the previous closed research follow-up:
6 builds (3 passed, 3 failed), 24 native jobs (15 passed, 9 failed). No cancelled jobs;
24 native jobs skipped in rounds 1/2/5 are not counted as executed. No training or public
release. Final source/compatibility gates: 666 root and 276 MyNotes checks pass.

## Qualified development APK
- Source: `17febd34da64cf9070544e7558f800e45691dca1`; evidence commit `e25a167d`.
- Run: https://github.com/tarungittarun/TicTic/actions/runs/41349116962 — **SUCCESS 9/9**.
- `MyNotes-v0.27.0-30-development.apk`, 66,768,176 bytes, raw APK upload (no ZIP wrapper).
- Download: https://github.com/tarungittarun/TicTic/actions/runs/41349116962/artifacts/13100660856
- GitHub-reported SHA256: `98df20c2e3adf8b8df75b7142b2de025df01ccce604a41927ab6ebb0da52c130`.
  Artifact metadata verified through GitHub API; APK was not downloaded/hash-verified locally
  because the artifact blob host is unavailable in this sandbox. Build verified the pinned
  existing package signer `08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`.
- Expires 2026-11-08 20:54:49 UTC. Development artifact, not a public Release.
- Install over the existing app; do not uninstall/clear private data to test this update.

| Native configuration | Shard A | Shard B | Total |
|---|---:|---:|---:|
| API26 portrait | 169/169 | 158/158 | 327/327 |
| API36 portrait | 169/169 | 158/158 | 327/327 |
| API34 2x-font/dark | 113/113 | 103/103 | 216/216 |
| API34 landscape | 113/113 | 103/103 | 216/216 |

356 synthetic JPEG previews plus exact raw reports/provenance are preserved under
`mynotes/docs/design-2026-10-29/native/41349116962/`. Independently inspected the new
category-dialog screenshots in all four configurations: native checkboxes/labels and
Save/Cancel remain legible/reachable; the disclosure body scrolls in constrained windows.
This targeted inspection is not a claim of reviewing every old fixture or physical acceptance.

Acceptance covers new local-provider intake, structured DOCX round-trip, editable/searchable
TXT intake without source mutation, real-provider empty-root refusal, full checkpoint recovery
of notes/audio/settings/styles/history/empty folders into isolated fresh storage, excluded
categories, corrupt object refusal, unsafe manifest destinations, READY replay after interrupted
installation, source-preserving note-only import, old-controller flush fencing and recovery of
the last complete checkpoint when note-only generations advanced. New low-level regression
cases also prevent external rollback/foreign writers silently replacing private content.
Fresh-context emulator recovery is **not** a claim of physical uninstall/reinstall, removable
SD-card reliability, vendor Files by Google UI acceptance, large-library scalability or
universal Word fidelity. Original sources/checkpoints remain local and untouched by imports.

The category dialog now uses one custom ScrollView containing its privacy/pending disclosure
and four native CheckBoxes (48dp minimum), instead of conflicting AlertDialog message/list
APIs. The native journey requires visibly reachable checkboxes, toggles all four, saves,
checks persisted preferences, reopens and cancels a change, and verifies diagnostic-copy
selection does not enable log collection. All four profiles include this regression.
App-local diagnostic deletion explicitly excludes older App-folder checkpoints and exports.
