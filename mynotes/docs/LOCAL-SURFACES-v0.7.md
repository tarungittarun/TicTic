# MyNotes 0.7 candidate — local reminders and home-screen widget

Qualification is pending until the candidate's complete Actions gates pass. The accepted 0.6 APK remains available unchanged.

## Implemented scope

- Reminder date/time per note; Clear reminder and existing Undo. Schema 5 carries the reminder epoch in local journals, selected-folder generations and portable JSON backups. Schemas 1–4 migrate with no reminder. Import conflicts include reminder differences; duplicating a note does not silently duplicate its alert.
- Optional, contextual Android 13+ notification permission. Denial does not disable notes, OCR/voice, storage, PDF or widgets. No Internet or exact-alarm permission is requested.
- Android inexact `setAndAllowWhileIdle` alarms, immutable per-note PendingIntents, private notification visibility, tap to open the existing note, stale/deleted/changed alarm validation and repeat-delivery markers. Trash/clear/purge cancel pending alerts; restore can reschedule a still-future reminder. Boot/package update reconciles future alarms from the atomic private snapshot without reading a potentially unavailable document provider.
- Re-placeable/resizable home-screen widget: up to four active notes, pinned first, most recently changed next; archive and Trash excluded. Header opens the library; each row opens its existing note. Library “Home widget” invokes the launcher's supported pin request or gives standard manual placement instructions. Removing a widget never deletes a note. Titles on the home screen are visible to anyone who can see it; do not place the widget if that is inappropriate.
- Receivers use `goAsync`, worker threads and `finally` completion; OS surfaces never read selected-folder providers on the main thread. Saved private snapshots remain authoritative for these bounded projections.

## Qualification design

The main workflow expands to **21 Android lanes / 30 expanded jobs**, with independent source, inventory, JVM, lint and APK gates. A dedicated API34 local-surfaces lane grants notifications; normal modern lanes leave that optional permission denied. RemoteViews inflation, immutable Intent identities, scheduling/cancellation, actual notification policy/deduplication, schema migration, non-destructive import, private snapshot isolation, clear/Undo and existing-note routing are executable tests.

Every lane now installs the **highest-version accepted prior APK**, creates a uniquely named real release note through the UI, cold-opens it, updates without uninstalling, and requires that same title after update. Before/after UI XML and baseline identity/hash are retained. This tests seeded note survival, not audio-attachment migration or arbitrary old-device state.

A separate read-only, nonpublishing workflow runs a requested **70-minute continuous native/durable soak on API26 and API35**. It cycles real English Tesseract and Vosk inference, interrupted PCM repair, atomic private repository save/readback and portable backup with a bounded 32-note working set. It pins source WAV bytes, records elapsed time/iterations/PSS/native allocations, and fails on a gross post-warmup PSS increase above 128 MiB. This generous gross-growth guard is not proof of zero leaks. Normal CI executes the same test with four cycles; only the dedicated reports establish long-run duration. No soak success is claimed before completion.

## Explicit limits

- **Not exact alarms.** Android battery saving, Doze, OEM policy and notification/channel settings may delay or suppress delivery. Force-stop disables alarms until reopening. Future alarms are restored on boot/update; power-off missed deadlines are not promised catch-up notifications.
- Notification denial keeps data intact, but cannot produce notifications. The notification is generic except for the title; note bodies are not copied into it.
- Reminder time is an absolute epoch chosen in the current timezone, not a recurring/floating-time calendar rule. No recurring/location reminders, snooze, exact-alarm exemptions or calendar sync yet.
- Widget is a simple bounded title list, not a scrollable collection or an interactive checklist. It is resizable, but short placements may show fewer rows. It reflects the latest private snapshot, not externally edited folder contents before the app reconciles them.
- No physical launcher, reboot, OEM battery/thermal, TalkBack or permission-dialog corpus is claimed by component/emulator checks. Real launcher pinning/resizing and timed delivery under device Doze still need device trials.

OS projections and delivery share a latest-snapshot lock; private AtomicFile reads/writes and voice transcript reads/writes are protected across worker instances. Saving unrelated edits does not dismiss an already delivered reminder; clear/Trash/time changes invalidate it. Hindi/Kannada controlled-font image diagnostics retain source PNGs, raw recognized text, confidence and token matches separately from the strict English recognition gate. These diagnostics do not assert general language accuracy.

Automatic late/retried voice results also consult the durable creation marker before creating a missing note. Explicit Voice inbox “Open note” remains a user-requested recovery action; automatic recognition must not undo a deliberate purge.
