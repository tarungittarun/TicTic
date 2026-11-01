# App folder / Open file qualification ledger

Independent from the previous closed 40-job speech/OCR research follow-up. No new model
weights or training. Android builds remain Actions-only. The user lifted the job cap;
count actual executed jobs, not skipped jobs or hypothetical matrix slots.

| Round | Source | Run | Actual jobs | Result |
|---|---|---|---:|---|
| 1 | dc024ab1 | 41344031614 | 1 build | Failed runtime compilation: lambda parameter shadowed a local name. 279 JVM tests passed; 8 native jobs skipped. No qualified APK. |
| 2 | 86975fbc | 41345739995 | 1 build | Failed runtime compilation: staging space-check cursor declaration missing after formatting. 279 JVM tests passed; 8 native jobs skipped. No qualified APK. |
| 3 | next source | pending | 9 planned | Build + 8 native shards; 279 expected JVM tests and 1082 expected native method outcomes. Not evidence until complete. |

Executed so far: **2 failed build jobs**, all terminal; 16 native jobs skipped, not counted
as executed. Planned round 3 is not yet a result. Root source/compatibility checks 666 and
MyNotes checks 276 passed before the second push; all gates rerun before the next push.

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
