# Storage & recovery — 0.2.0 (2)

Historical milestone notes. Current library reopen/backup functionality supersedes the
old import limitations below; see BACKUP-RESTORE-v0.3.md and STANDALONE-TEST-GUIDE.md.

Implementation written; Android/provider qualification status must be checked in the
execution report. Do not interpret this design as a universal durability guarantee.

## Flow

Home > Storage > Choose folder. Choose a NEW EMPTY local folder via Android's standard
Internal storage or SD-card provider. Picker cancellation leaves the destination alone.
The host retains read/write URI permission. Cloud providers are rejected, not silently
used online. Android-restricted roots/system directories cannot be selected.

The serial worker copies the complete text library, closes and reopens the created
files, validates checksums/content, then persists the selected destination. Previous
files are retained. Failed/partial generations are ignored when a prior valid chain
exists. Missing parents, unknown formats or forks block writes instead of choosing a
winner. Existing libraries are rejected as migration targets; opening/importing those
libraries is a future feature, not an implicit overwrite or merge.

## Recovery

Every external save first writes an app-private AtomicFile journal. If folder access
is revoked, full, disconnected or provider writes fail, the UI reports pending recovery
rather than Saved-to-folder. Retry save attempts the same destination; it does not
silently relocate the library. Reopen can recover the private snapshot. A successful
external write with a lost acknowledgement is recognised by matching content on retry.
Foreign updates are not overwritten with an older recovery branch.

Use device storage is an explicit choice to switch back to private local storage;
the previous external folder is retained. Choosing a different empty folder can preserve
recovered changes when the old tree cannot be reconnected. External files are plain JSON
and not encrypted. Private cache/recovery is intentionally retained and disclosed.

## Limits / pending tests

- Only text documents in this release. Media/blob/structured rich schemas are pending.
- No cloud, encryption, import-existing-library UI or automatic cross-app sharing.
- Uninstall deletes private recovery/settings and folder grants. Chosen files remain,
  but this version has no reopen-existing-library import; do not uninstall as an update.
- Full immutable snapshots retain their ancestor chain. Large-library incremental
  commits/compaction and bounded-memory provider indexing are not implemented yet.
  This must be addressed before the full scale/performance acceptance gate.
- Preflight head comparison is not a cross-process lock. Concurrent writers may fork;
  the protocol detects this and refuses subsequent writes. Use only one app writer.
- SAF provider flush/close/readback cannot guarantee power-loss durability on every
  device/provider. Android-provider fault injection, SD removal, actual persisted grant
  restart and picker cancellation/revocation still require device qualification.
- A local disk failure can also prevent the private journal; UI must not claim success.
- Debounced autosave still has a short keystroke window before persistence. Lifecycle
  flush helps; forced process death within that window is not yet a passed guarantee.
