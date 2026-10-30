# Code20 development handoff — search, local folders and restart history

**Development-qualified: final three-profile replay PASS. Not public release approval.**

## Requested changes

1. Home bottom-bar Search is immediately after Record voice. Tap focuses search; double-tap opens the keyboard; holding the second tap opens Filters. Standard click/long-click, the existing editable search field and Filters button remain accessible alternatives. Moved/cancelled holds do not activate Filters.
2. Folders now presents organisation, not storage-migration errors: private folders work without connecting external storage; connected real local folders still work. Create, browse and move notes without changing storage implicitly. Backups and Storage settings are separate destinations; the normal Folders page no longer mixes in main-storage setup controls. Local-only picker flags supplement the enforced local-provider/readback-verified backup boundary.
3. Per-note Undo and Redo can survive reopening a controller/app through a bounded private AtomicFile sidecar, with checksum verification and exact saved-note anchors. Both legacy and document editors participate. New typing invalidates Redo immediately. Corrupt/unknown history is retained and blocked until explicit Reset edit history; it cannot silently rewind or overwrite current notes.

[Full user instructions, limits and recovery steps](SEARCH-FOLDERS-HISTORY-USER-GUIDE.md).

## Raw signed development APK

- **MyNotes-v0.17.0-20-development.apk** — raw APK, not ZIP: https://github.com/tarungittarun/TicTic/actions/runs/41058786300/artifacts/12977978378
- Version0.17.0/code20, package`app.space.notes`, min26/target34/compile35.
- Size66,678,664 bytes. Raw artifact SHA-256:`1fcd83148c010a11194f11bcbbc6fc59d8582c5bb3f163a19534492dff72f0fb`.
- Stable signing certificate SHA-256:`08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`; mandatory signing verification passed.
- Expires2026-11-06 12:55:20UTC. GitHub repository access may be required.
- APK/test source`009e92f0620d7f65b69fdc856cab929d47101f1b`. Production app trees and app/build.gradle are unchanged since`0a7b51205c7765cb06079378d9ab7798a48ebcfd`; later changes correct test navigation routes and documentation only.

## Exact qualification

| Gate | Result |
|---|---|
| Local root QA |558 PASS|
| Local MyNotes QA |168 PASS; overlaps root discovery, not an additional168 unique tests|
| Executed JVM inventory |217, exact mandatory gate PASS|
| Compilation, debug/release lint, stable signing |PASS; build124072943776|
| Full API26 |240/240 PASS, exact inventory, no skips; job124073945271|
| API34 dark2× |111/111 PASS, exact inventory, no skips; job124073945284|
| API34 landscape |111/111 PASS, exact inventory, no skips; job124073945475|
| Actual exported PDF independent parser gate |PASS independently on all three final profiles|

Final source-pinned reports and synthetic screenshots are preserved in evidence commit`94b7348`, under`docs/design-2026-10-29/native/41058786300/`. All profiles require every selected method exactly once, without skips/duplicates. The final exported PDF fixtures pass independent URI annotation and page-geometry checks.

Original failures remain recorded, not waived: run41054984154/sourceab1d4fb passed237/238 and caught delayed Redo invalidation, fixed in production with a new domain regression. Run41056664304/source0a7b512 passed238/240 full and110/111 in each focused profile; remaining tests still expected storage setup on the old Folders screen. Their readiness/route changes retain all navigation, real-file, note-body and move assertions. Their source-pinned reports/screenshots remain under`docs/design-2026-10-29/native/`. No failed method was skipped.

Native checks include independent-store Undo/Redo, exact-note anchor mismatch, missing/purged notes, corrupt checksum with blocked writes, explicit reset preserving notes, interrupted AtomicFile recovery, oversized-file rejection,64-history retention, private-folder duplicates/path rejection, legacy/document cold-controller restart journeys, native keyboard/search gestures and private-folder create/move/reopen. These do not claim a physical Android force-stop/power-loss test.

## Data and remaining acceptance

Back up notes and important recordings, then install over the existing app. **Do not uninstall or clear data.** Note/checkpoint schemas are unchanged; history starts with new edits under this update, not reconstruction of pre-update edits.

History is per-note, up to50 steps per direction/64 recent histories, with global12Mi-character memory and16MiB encoded-file ceilings plus a conservative unique-payload budget. Large notes may retain fewer steps. Current notes are saved first; mismatching history is skipped after interrupted commits. History is not an unbounded backup, persistent navigation/Home-batch stack, individual drawing-stroke archive, or resurrection mechanism for permanently purged notes. Exported note backups exclude history, named presets, empty private folders and original recordings. Local backups remain readable JSON and retain the existing8MiB size limit.

Diagnostics attachment was reviewed locally and was not committed: it showed7 folder-open failures and3 rejected imports on older code16. The new private-folder backend addresses the former; unknown rejected file contents were not guessed at or silently accepted.

Budget:10 jobs reserved/started for this request,19/35 cumulative including the prior9;16 remain unused/unreserved. [Ledger](SEARCH-FOLDERS-HISTORY-JOB-LEDGER.md). No new35-job allowance is claimed. No measured-hours claim is made.

**Motorola edge50neo Android16 physical acceptance and public-release/Play approval remain pending.** Calculator unchanged; Archive paused; Wall dragging deferred.
