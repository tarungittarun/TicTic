# MyNotes 0.1.0 (1) — early text slice test guide

**Build status: GitHub Actions build 40561897413 succeeded.** Domain JVM tests passed;
release APK and Android test APK compiled. The five instrumented persistence tests
have NOT run on an emulator/device; compilation is not a runtime pass.

- [Download MyNotes 0.1.0 (1)](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/mynotes/releases/MyNotes-v0.1.0-1.apk)
- [SHA-256](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/mynotes/releases/MyNotes-v0.1.0-1.apk.sha256)
- [Build](https://github.com/tarungittarun/TicTic/actions/runs/40561897413)

This early text build uses app.space.notes and installs beside MySpace/MyCalc.
It is not the final design or complete feature set. Keep important data backed up;
folder selection and durable failed-save recovery are not implemented yet.

## Manual device journeys — awaiting results

1. Create a text note, type Unicode title/body, wait for Saved locally, Back and reopen.
2. Background/resume and rotate mid-edit; check title/body/caret and note identity.
3. Search a title/body fragment; type quickly and clear query; keyboard/focus stays stable,
   results follow latest query. Search is currently page-local, not final global filters.
4. Pin a note; check it sorts before unpinned notes. Archive hides it from Notes; Archive
   opens/edits it; Unarchive returns it.
5. Trash a note; open Trash; Restore returns it to its prior archive state. Undo on Home
   reverses the most recent pin/archive/trash state change. No permanent-delete UI yet.
6. Reopen after seven days; expired trashed notes purge at load. Use controlled clock
   fixtures, not destructive time experiments on a real user's library.
7. Simulate save failure: confirm no Saved acknowledgement and Retry save remains usable.
   Do not force-stop with unsaved data: durable recovery journal is not implemented yet.
8. Test dark/light, large fonts and small screens; log clipping and touch failures.

## Written automated tests

- :notes-domain:test — seven Java tests; CI task passed.
- NotesPersistenceTest — five Android tests; compiled, emulator/device execution pending.
- python3 -m unittest discover -s qa — 420 tests passed; source/protocol only.

## Explicitly unavailable

Folder selection, final Home masonry, list/rich-text editors, colours/labels UI, media,
recording/transcription, OCR, drawings, diagrams, reminders, widgets, import/export and
final motion/snackbar design. No microphone request exists in this text-only APK.
