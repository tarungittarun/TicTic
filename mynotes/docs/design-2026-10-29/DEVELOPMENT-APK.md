# Updated development APK — not release approval

Build source: `19db0fd`, run [40869247600](https://github.com/tarungittarun/TicTic/actions/runs/40869247600).

[Download the build ZIP](https://github.com/tarungittarun/TicTic/actions/runs/40869247600/artifacts/12894867659)

Extract **`release/notes-release.apk`**. Do not install the debug or Android-test APKs. This is version **0.13.1 / code15**, package `app.space.notes`. GitHub authentication may be required. Artifact retention is one day; this is not a permanent Release asset. This replaces the earlier code14 development link.

## Qualification boundary

Compilation, 148 JVM tests and debug/release lint succeeded. API26 passed 184/184; landscape passed 60/61, with the Pin journey still red. Tablet passed 61/61 on the preceding code14 source, not this code15 source. Full publication qualification has not cleared. The artifact is an explicitly unqualified development candidate, not a production-ready recommendation. The published Release remains 0.12/code13.

## Protect existing data

- Make and verify a local note backup before testing a development candidate. Export important original voice recordings separately; they are not included in the note backup.
- Install as an update over the existing app. **Do not uninstall to force installation.** If Android reports a signing/package conflict, stop and report the exact error rather than deleting the installation.
- Retain the old APK, backups and chosen main folder. Do not assume that an older APK can safely reopen all data written by a newer development version.

## Highest-value physical checks

1. Open a new drawing in portrait. Check that the drawing tools have icons and names, the selected tool is obvious, and Save is immediately left of More.
2. Save a drawing, reopen it, then rotate to landscape. Draw near the right edge. Pan a tall page with two fingers; use Fit to see the whole page. Verify old and new marks remain after saving and reopening.
3. Enable grid, copy the drawing as an image and paste into another image-capable app. Only artwork should be copied—not controls or the guide grid. Some recipient apps may not support image clipboard pastes.
4. In a mixed note, distinguish an imported Image from a Drawing. Edit the drawing without adding a duplicate attachment. Remove only the intended item, then test Undo.
5. Select a diagram shape and resize using its corner handles. Confirm grouping becomes available only with two choices and existing groups can be ungrouped.
6. Write and precisely select text in landscape with the keyboard visible. Apply a heading, change Colour/Labels, return and verify the selected range and following paragraph layout.
7. Open Trash. Library should remain in its lower location, with no New note, Checklist or Record voice actions offered there.

These checks supplement automation; they do not imply that storage, reminder delivery, OCR or voice accuracy have already been accepted on the Motorola device.
