# MyNotes: local files and the optional App folder

## Keep your current installation
Install development updates **over the existing app**. Do not uninstall or clear app data
just to test recovery. Emulator qualification is not a physical-device reinstall guarantee.
Keep an independent backup, especially before changing storage or removing the app.

## Where your work lives
- By default, everything stays in MyNotes' private storage on your device. Android removes
  that private storage on uninstall.
- An optional App folder adds a second local copy; it does not replace private storage.
- Open **Library → Settings → Storage & recovery → Choose folder** and select an **empty**
  local folder. An existing recognized App folder is a recovery source, not a new destination;
  unrelated nonempty folders are refused rather than overwritten.
- Use **App-folder copy choices** for original recordings/transcripts, settings/saved styles,
  edit history/recovery and diagnostic copies. Notes/Docs and their attachments are always
  included while connected. Diagnostic copies do **not** enable diagnostic collection.

The external folder contains:

```
MyNotes-App.json        Layout identity/version: leave intact
README.txt             Local recovery/privacy instructions
Content/
  Notes/               Editable MyNotes Notes/Docs and note subfolders
  Voice/               Completed original recording copies
Settings/              Portable settings references
Profile/               Reserved local-profile namespace; no account required
History/               History/checkpoint references
Diagnostics/           Diagnostic copies, only if selected
Recovery/
  Library/             Verified native note generations
  Objects/             Checksummed auxiliary data
  Checkpoints/         Complete dated recovery manifests
```

Use MyNotes **Folders** to create note subfolders and move notes. Current folder controls
support individual named subfolders, not an unlimited nested directory tree. Do not manually
rearrange recovery/system directories. This is not a general filesystem synchronizer:
arbitrary files dropped into the directory are not automatically imported as notes.

## Know when the second copy is ready
Saves remain internal first; external copying and verification run in the background.
Use **Refresh checkpoint status** to read the current result, and **Retry save** after
reconnecting unavailable storage. A verified checkpoint is a dated recovery point, not a
promise that later changes have already been copied. Active recordings, pending writes,
revoked access, a removed SD card or insufficient space can leave newer data private only.
Finish recording and wait for verification before relying on the external copy.

## Recover an existing App folder
On an empty installation, open **Storage & recovery → Recover App folder**, select the
existing folder and grant Android access again. Review the checkpoint date and included
categories before confirming. MyNotes verifies the recovery objects before installation.
Existing notes, recordings, saved styles or private folders block full recovery rather than
being silently replaced. Notification/widget permissions require fresh Android consent.

If newer note generations differ from the last complete app checkpoint, MyNotes can recover
the dated complete checkpoint privately without reconnecting that source as a save target.
The warning explains this. Review newer notes separately using **Open library**, then choose
a new empty App folder for continued copies. **Open library** on a complete App folder imports
notes only; it does not import recordings/settings/history or change that source folder.

## Open files usefully
- **Open file** accepts supported local MyNotes JSON, TXT, DOCX and PDF files.
- A matching unchanged native note reopens in the editor; other native content gets a merge
  preview, with changed-ID conflicts preserved rather than silently overwriting your work.
- TXT offers an editable imported copy: you can edit it, use Find, and save it as a note.
- DOCX imports supported text formatting and simple tables into an editable Doc. This is not
  a full Word layout engine: media/layout/inherited styles may not survive; merged or nested
  tables are refused. Legacy binary `.doc` is not supported. Keep the original Word file.
- PDF remains a preview/source, not a promise of fully editable PDF reconstruction.
- Editing an imported copy does not write back to the original selected file.

## Backups, deletion and privacy
Portable JSON backups are **note-only**: original recordings, settings and edit history are
not included. Full App-folder checkpoints are separate and depend on your copy choices.
Turning off a category stops future copies; it does not erase earlier external copies.
Deleting app-local diagnostics likewise does not erase exported files or older checkpoints.
Old recovery objects are retained; no automatic garbage collection or external permanent
purge is provided, so the folder can grow. Move to a new empty destination when appropriate,
and manage old independent copies deliberately.

Everything here is local, but external copies are readable plaintext/native data, **not
encrypted**. Anyone or any app with access to that folder may read them. Private caches,
model installations, Android folder grants and diagnostic-collection consent are not portable.
Physical uninstall/reinstall, vendor file providers, removable-media failures and large-library
performance still need device-specific acceptance; do not treat a passing emulator as proof
that every phone/provider has the same durability guarantees.
