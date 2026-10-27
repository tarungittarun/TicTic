# MyNotes 0.3.0 (4) — update, backup and restore guide

**Published:** run 40566162164 passed all jobs. 430 local checks, 14 JVM tests,
28 Android tests, signature/version gate and install-over-0.1.0 smoke passed. Published
APK checksum verified. This is an early text/storage milestone; real SAF/SD and physical-
device qualification is distinct from the injected-backend emulator suite.

## Update

Install the release APK over the existing MyNotes release. **Do not uninstall or clear
app data for a normal update.** app.space.notes and signing certificate stay unchanged;
versionCode increases to 4. Debug is a separate app and not the release update channel.

- [MyNotes 0.3.0 (4)](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/mynotes/releases/MyNotes-v0.3.0-4.apk)
- [SHA-256](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/mynotes/releases/MyNotes-v0.3.0-4.apk.sha256)
- [CI](https://github.com/tarungittarun/TicTic/actions/runs/40566162164)

## New journeys — use disposable notes first

1. Storage > Backups > Export backup. Save a NEW timestamped JSON file using local
   Internal storage/SD card. Wait for Backup verified; inspect that a non-empty file
   exists. Cloud/other providers are refused. A failed export is not a usable backup.
2. Edit a test note, then Import backup. Check preview counts. Cancel: current text
   stays unchanged. Preview again and Confirm merge: changed same-ID text appears as
   a separate note, not an overwrite; unchanged entries are skipped.
3. Re-import that same backup: no additional conflict copies should be created. Check
   title/body, pin/archive, colour and Trash timestamp preservation.
4. Previous snapshot previews the state before the last confirmed import/open-library.
   Confirm adds missing/different versions; it does NOT delete newer current notes.
5. Existing library: choose a second known text library with Storage > Open library.
   Review counts and cancel once; confirm current destination unchanged. Then confirm
   a fresh preview; both sets survive and saving now goes to the selected folder.
6. Reinstall recovery is available ONLY if a chosen local folder or verified backup
   survived. Re-grant folder access and Open library, or Import backup. Do not perform
   uninstall experiments on your only copy of important notes.
7. Negative cases: corrupt/truncate a disposable backup, alter its checksum, use an
   unsupported version or choose a non-local provider. Validation must fail before
   preview/import. 8 MiB is the explicit portable text-backup limit on both directions.
8. Folder failure during restore: merged data should remain in private recovery with
   Pending status; restart, reconnect and Retry save. Real permission/SD fault checks
   still need device results; emulator transaction tests use an injected backend.
9. Regression: create/edit/search, pins, archive, seven-day Trash/restore, Undo,
   dark/light, large font, rotation and small-screen action accessibility.

## Automated scope

Passed current suite: 430 local checks (40 Notes + 390 legacy), 14 Java domain tests,
28 Android tests. Android includes private AtomicFile, protocol, backup codec, injected
repository transfer/recovery and storage layout checks. It is not certification of all
physical providers or proof of seeded release-data migration on every prior version.

## Still unfinished

Streaming large/media backup, encryption, generation compaction/index scaling, rich
text/lists, labels/colours UI, images/OCR/voice/ASR, diagrams, reminders/widgets, final
masonry/motion and full MySpace integration. Private checkpoints disappear on uninstall.
Debounced edits still have a brief forced-kill window before persistence. A corrupt
private library can still block startup and needs a future dedicated recovery screen;
do not clear data without a separately verified surviving folder/backup.
