# Home search, local folders and restart-safe Undo/Redo — code20

## Bottom search button

The magnifying-glass button is immediately after **Record voice** in the Home-page bottom bar.

- **Tap:** focus the existing search box.
- **Double-tap:** focus search and show the keyboard.
- **Tap, then hold the second tap:** open **Filters** instead of showing the keyboard.
- A normal long-press also opens Filters. Moving/cancelling the touch cancels a held gesture.

You can still tap the search box directly to type, and use its existing **Filters** button. Those are the alternatives when using accessibility tools or when gestures are inconvenient. Search still runs locally; no query is uploaded.

## Folders: organisation, not a storage migration

Open **Library → Folders**, or **Settings → Storage & recovery → Folders**.

- **All notes** returns to the normal notes list.
- **Main folder** shows notes not placed in a subfolder.
- Tap a folder name to see its notes.
- **Create folder** makes a named folder without asking you to connect external storage.
- To move notes, select them in the list, then choose **Selection actions → Move to folder** and select the destination. A document's Move to folder action uses the same destination chooser.

With private storage, folders organise the library inside MyNotes. They do not make individual files visible in Android Files. With an already-connected local main folder, real subfolders/portable note files continue to work. **Storage settings** is the separate, optional place for changing where the library is saved. Moving storage copies and verifies the library first; previous files are retained.

Private folder names survive restarting the app. Used folder placement is included in note backups; empty private folders are not. There is a limit of128 private folders. This update does not add recursive folder deletion or rename operations.

## Make a local backup

From Folders, choose **Backups**, or use **Settings → Storage & recovery → Backups → Export backup**.

Choose **Internal storage**, **Downloads**, or a local **SD card** through Android's local storage provider. The picker is marked local-only, and the app independently rejects cloud-provider destinations. Export reports success only after writing and reopening the complete backup to verify it.

Backups include current note text, checklists, diagrams, images/drawings, note formatting and used folder placement. They **do not** include original voice recordings, named style presets, empty private folders, or Undo/Redo history. Export important recordings separately from Voice inbox. The existing8MiB portable-backup limit remains. Backup JSON is readable, not encrypted: choose where to keep/share it carefully.

Use **Import backup** for a MyNotes backup or exported `.mynote.json` document. Review the preview before accepting. A PDF, Word document or another app's arbitrary JSON is not a MyNotes backup. The attached diagnostics reported rejected imports but did not reveal their contents; no unsupported file has been guessed at or silently accepted.

## Undo/Redo after restarting

For note editing, saved Undo and Redo steps can now be loaded when reopening the app. This works for both the existing text/checklist editor and the document editor. Note snapshots can retain formatting, structure, visuals and metadata; document cursor information is also recorded when available. A new edit invalidates Redo immediately, not only after autosave.

History **starts being collected with this update**. It cannot recreate edits that an older APK kept only in memory. This is per-note edit history, not a persistent app-navigation stack or a way to resurrect permanently purged notes. Existing Home/batch-action Undo retains its session behaviour; the retained note edit snapshots are used from the note's Undo/Redo controls.

### Bounds and recovery

- At most50 steps per direction per note, for up to64 recently used note histories.
- A conservative global12Mi-character in-memory budget; a16MiB maximum encoded private history file with a smaller unique-snapshot payload budget. Older steps can be trimmed sooner, especially for large image-heavy notes. These are upper limits, not a promise that every note retains50 steps.
- History is saved privately on the device, separately from current notes and portable backups. It is not cloud-synchronised.
- Current notes are written first. History is then written atomically with checksums. Only history whose anchor exactly matches the current saved note is restored. Mismatched history is skipped; it never rewinds a newer note automatically.
- Interrupted atomic history writes can recover the preceding complete history. A crash between note and history commits can lose the latest history, but mismatching history is not applied to current notes. Sudden termination can also lose typing still marked unsaved; this is not a guarantee against every power-loss window.
- If saved history is corrupt or unsupported, MyNotes keeps your current notes, warns you, and retains the unreadable history without overwriting it. New history persistence stays paused until you explicitly reset it.

If that happens, first make a local note backup if possible. Then use **Settings → Storage & recovery → Reset edit history → Reset history**. This removes edit-history steps, **not current notes, attachments or existing backups**. Do not uninstall or clear app data as a recovery step.

## Updating safely

Install the newer signed APK over the existing app. Keep the same package/signer. Back up notes and important recordings first. **Do not uninstall or clear data.** Automated checks and the source-pinned development handoff are separate from physical Motorola acceptance and public-release approval.
