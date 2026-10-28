# MyNotes 0.11 / code 12 — candidate, not published

This work follows the user's real-device 0.10 feedback. Implementation is not an acceptance claim. Only a fully green, exact-source Android qualification plus rendered review can authorize publication; install the existing 0.10 release until that happens.

## Changed interactions

- Pin changes to the selected **Unpin** control. Native text selection, direct Colour/Labels, Reminder under Plus, and list-only note Copy/Trash remain.
- Left-edge home swipe opens the lower-screen Library. Filters have swipeable **View / Sort / Labels** tabs and independent label checkboxes (all, none, one, or a subset).
- Folders are actual local SAF directories below the chosen main directory, not labels. Browse into one to create notes there, or move selected notes. Portable `.mynote.json` copies carry images and can be imported with the non-destructive preview/merge route. Backup envelopes still require their checksum.
- Voice has separate transcript cards, seek/progress, play/pause and icon record/refresh actions. Technical model, audio and backup explanations belong in About. Deterministic pause-based comma/period insertion improves readability; it does **not** reconstruct semantic punctuation perfectly.
- Diagram metadata is distinct from reminder metadata. The picker offers 20 named shapes plus text annotation. **Edit shape**, drag-to-move, diagram Undo/Redo and basic grouping replace the old confusing controls. Nested groups, layers and smart guides are not claimed.
- Notes accept locally copied images and freehand drawings, independently of note colour and **16 backgrounds**. Image imports sample pixels, normalize EXIF orientation, strip source metadata, and enforce byte/dimension/library-backup bounds. Drawings have stroke Undo/Redo and private draft recovery.
- Settings and Storage & recovery use focused rows/actions and collapsed details. Standalone appearance supports System / Light / Dark; a host can supply appearance/design tokens through `NotesHost`. This is an embedding contract, not certification of the future MySpace lifecycle or migration.

## Durability work, not just controls

Selected-folder generations use format 2: verified SHA-256 image references point into reserved `.mynotes-media-v1`. Identical images are stored once instead of copied into every autosave generation. Format 1 remains readable. Private and portable note payloads and backups remain self-contained. The hydration cache is bounded to 8 MiB. History and old image variants are retained; this is **not** generation compaction or media garbage collection.

Physical note updates write and read back a new temporary copy **before** renaming the old current file. The previous copy is removed only after the replacement's name and exact bytes are verified. A failure retains copies and the private pending journal. Named pending/previous files also let reconciliation retry an interrupted root placement. This is not a claim of universal provider atomicity or physical power-loss qualification.

Queued immutable snapshots coalesce before disk work, avoiding a long queue of obsolete full-image saves. Drawing Save is guarded against repeated taps, draft loading verifies dimensions, and draft cleanup occurs only after a durable note save and matching draft bytes. A newer/different draft is not deleted. Folder/background Undo restores those metadata fields without replacing subsequent body edits.

## Qualification record

- Local source gates: root **505**, Notes **115**, whitespace check pass at the implementation boundary. These are not Android compilation or execution.
- Android plan: **114 JVM / 144 unique Android** methods, partitioned across 21 emulator lanes; Actions is the only Android build path.
- Run `40694075712` (c2c681e): failed. The real SAF operations passed on API26 but cleanup released a grant already revoked by directory deletion. API29 did not complete the picker; API-specific button matching and bounded UI-state failure diagnostics were added. A background test tried a hidden menu item without opening More actions. These harness corrections do not count as successful reruns.
- Latest runtime source `75394e11d3b87de367c7c080cab21a31427c05f8`, qualification `40696288757`: **in progress**, not approved. Later synthetic-scene capture changes are test-only and still need execution.
- Publication, the final rendered overview PDF, and real-device confirmation remain pending. The previous 0.10 artifact is unchanged.

## Important limits

JSON backups include embedded note images, not original voice recordings; export original audio separately. Images are limited to eight per note, 1600 pixels on the long edge and approximately 1 MiB encoded each; the complete verified backup has an 8 MiB cap. Input image files are bounded to 20 MiB. Drawings are 1024 × 768. No cloud provider or Internet permission is added.

Emulator proof does not certify physical OEM/SD-provider durability, microphone/accent/noise quality, TalkBack, thermal behavior, ARM/16-KiB devices, or future MySpace integration. Do not uninstall or clear data when updating. Keep the stable package/signing identity and increase versionCode for future runtime changes.
