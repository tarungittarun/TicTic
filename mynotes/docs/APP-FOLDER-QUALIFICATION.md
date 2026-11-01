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
| 5 | next source | pending | 9 planned | Product app source unchanged from round 4. Test first requests native ACTION_SHOW_ON_SCREEN and requires the full 48dp row before interacting, rather than asserting on a partially visible clipped accessibility rectangle. Expected 279 JVM and 1086 native outcomes. |

Executed so far: **20 actual jobs** (4 builds: 2 passed, 2 failed; native: 7 passed, 9 failed).
No cancelled jobs; 16 native jobs skipped in rounds 1/2, not counted as executed.
Round 5 adds 9 planned jobs, not yet results. Root source/compatibility checks 666 and
MyNotes checks 276 pass before push. No APK is qualified until every required check passes.

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
