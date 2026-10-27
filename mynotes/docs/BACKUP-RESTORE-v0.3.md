# MyNotes 0.3.0 (4) — library reopen, backup and restore

Implementation/testing outcome is recorded in M0-STATUS.md. This release does not
complete the rich-content/ML/diagram scope.

## Open library after reinstall or folder selection

Storage > Open library > select the existing local MyNotes folder. Android re-grants
read/write permission. Notes validates generations/checksums, prepares a merge preview
and shows counts. Until Confirm merge, the current library is unchanged. Cancel does
not import anything or switch the destination.

Confirm combines the current and selected libraries and uses that folder as the active
destination. Current notes are never discarded. A same-ID note with different text or
metadata is kept as a separate, deterministically identified copy. Reopening/re-importing
the same versions does not grow duplicate conflict copies. A target changed by another
writer after preview is rejected. Forked/corrupt chains are still refused, not repaired
by silently selecting a branch.

A private before-restore checkpoint is written before applying a merge. Folder history
is retained; prior generations/source folders are not deleted. This does not remove the
need for a separately verified portable backup. Only one app should write a folder.

## Portable backup

Storage > Backups > Export backup. Choose a file using Android's local Internal storage
or SD-card provider. The JSON envelope has format/version, count and SHA-256. The worker
closes, reopens and verifies all bytes before reporting success. Failed/interrupted
exports can leave an incomplete file; do not rely on a file without the success outcome.
Choosing an existing file can overwrite that backup (Android picker prompts); it cannot
change the current note library. Prefer a new timestamped backup each time.

Import backup validates format, schema, checksum, duplicate IDs and count before preview.
Confirm is additive: existing notes stay, exact duplicates skip, conflicts keep both.
All text-note metadata (IDs except necessary conflict copies, creation/edit dates,
colours, pin/archive and Trash timestamps) is preserved. Seven-day Trash policy still
applies when the library loads; a restored expired Trash item is not made permanent.

Previous snapshot previews the checkpoint from the last confirmed import/open-library.
It too merges without deleting current notes. It is not a destructive time-travel/replace
operation, and it is removed on uninstall. A failed folder update after import retains
the merged snapshot privately and reports Pending; future retries preserve the imported
notes rather than overwriting them with the old UI state.

## Current bounds and privacy

- Text data only: media/diagrams/rich content are not implemented yet. Do not describe
  this format as a future complete media backup.
- Portable backups are capped at 8 MiB for both export and import. This is an explicit
  milestone safety bound, not silent truncation. Streaming large/media backups remain
  required later. Folder libraries use the existing generation format, not this envelope.
- Only the standard local ExternalStorageProvider is accepted. Cloud/other providers
  are refused. Files are plain JSON and NOT encrypted.
- Normal APK updates should never uninstall the app. Reinstall recovery is possible
  only from an existing chosen folder or verified backup, not deleted private data.
- Real SAF-provider/SD removal, permission-revocation and picker journeys still need
  physical-device qualification. In-memory backend tests do not prove device durability.
- Preview is deliberately cancelled on process destruction; there is no hidden deferred
  import. A new picker result arriving during initial load is queued until load completes.
