# MyNotes 0.2.1 (3) — update and storage test guide

**Published and verified by CI run 40564058421:** seven JVM tests, 14 Android tests,
install-over-0.1.0 smoke, signing/package/version gates and APK checksum passed.
Actual SAF/SD device qualification is still pending. This is an early text/storage
milestone, not the full PDF design or complete feature set.

## Safe update rules

Install the new **release** APK over the existing MyNotes release. Do not uninstall or
clear app data. Package remains app.space.notes with the same certificate. Debug is a
separate .debug package and is not the release update channel. Android downgrades are
not supported. CI checks certificate/package/version compatibility before publishing.

- [MyNotes 0.2.1 (3) APK](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/mynotes/releases/MyNotes-v0.2.1-3.apk)
- [SHA-256](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/mynotes/releases/MyNotes-v0.2.1-3.apk.sha256)
- [CI](https://github.com/tarungittarun/TicTic/actions/runs/40564058421)

APK published successfully; checksum verified after fetching the published artifact.

## Manual device journeys — real-provider qualification pending

1. Before updating, create a named note in 0.1.0/0.2.0. Update without uninstalling and
   confirm title/body/pin/archive/Trash metadata are retained. CI installs old -> new
   and tests the old format, but does not seed a real release note before upgrading.
2. Home > Storage > Choose folder. Use a NEW EMPTY folder under local Internal storage
   or SD card. Confirm success, add/edit a note and inspect mynotes-gen-*.json files.
   No cloud provider is accepted. Android disallows some roots/system folders.
3. Cancel the picker: current location/data must not change. Pick a cloud provider or
   a folder containing a library: it must be refused without deleting existing files.
4. Change to another empty folder: all notes copy; original files remain. Restart and
   confirm edits come from the selected library, not an obsolete snapshot.
5. Revoke directory access/remove SD card, edit, and observe private-recovery/pending
   status instead of Saved-to-folder. Reopen, confirm recovery text; reconnect then
   Retry save. Device/provider fault cases have not yet been certified.
6. Use device storage: confirm private saving continues and old external files remain.
   Opening/importing an existing library after reinstall is not available yet; do not
   uninstall as an update strategy.
7. Small screen/large font: storage explanation scrolls; Choose folder, Retry save,
   Use device storage and Back must remain accessible. Test landscape separately.
8. Regression: edit Unicode text, search rapidly, pin/archive/trash/restore and use
   one-level state Undo. Repeat across restart and dark/light themes.

## Automated coverage

- Local source/protocol/release-policy suite: 425 tests (35 Notes + 390 legacy).
- Pure Java document tests: seven.
- Android suite: five private persistence + eight provider-independent generation/format
  tests + one actual-layout visibility test = 14. Actual SAF and physical SD tests are
  separate; passing the in-memory backend does not certify every storage provider.
- APK signature equality and strictly increasing code; adb install-over-old smoke.

## Limits

Text-only; final masonry, rich text/lists, colour/labels UI, media, recording/ASR/OCR,
diagrams, reminders, widgets and import/export remain unfinished. Full snapshots retain
ancestor history; compaction and scale qualification are pending. No encryption. Private
recovery/settings are deleted on uninstall; selected files remain but import-existing UI
is pending. Debounced saves still leave a small forced-kill window before persistence.
