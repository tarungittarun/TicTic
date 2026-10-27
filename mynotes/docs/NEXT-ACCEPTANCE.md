# Next MyNotes acceptance slices (planned, not shipped)

This is the continuation plan after accepted **0.7.0(8)**, not a claim that the full Keep inventory is complete. Current outcomes and qualifications are in [REPORT-CARD.md](REPORT-CARD.md); the original permanent requirement IDs remain in [REQUIREMENTS.md](REQUIREMENTS.md).

## 1. Portable local attachments and recovery — next large product slice

Covers photo/file capture and owned-media foundations (K018–019, K024, K053–056), followed by richer voice-note presentation (K017, K058–059). Existing OCR review does **not** implement image attachments, and private voice WAVs are **not** included in JSON/folder generations today.

Before adding media buttons:

- Specify a versioned attachment/reference model; preserve schema 1–5 reads and monotonically increase the public versionCode for runtime changes. Clones/import merges must not overwrite a different note's media.
- Own immutable local copies. A moved original or expired picker grant must not break saved content. Stream bounded chunks rather than loading entire recordings/images/backups into heap.
- Design transactional portable-media export/import with a manifest, byte lengths and digests. Do not silently label a text-only JSON export a complete media backup. Reject traversal, duplicate archive names, inconsistent manifests and incomplete archives before replacing live data.
- Define retention independently from note visibility. Seven-day Trash and explicit permanent deletion must not delete a blob shared by another live/restorable note. Garbage collection needs a checked reference set and a crash-safe grace period, not an age-only directory sweep.
- Preserve the current permanent voice-deletion guard through media/schema migration. A late ASR completion must not revive a purged note. Explicit inbox recovery must remain an intentional separate action.
- Treat provider cancellation, readonly/removed SD cards, low disk and process death during copy/commit as normal failure paths; retain the original source and last good generation. Local-only users must choose on-device/SD SAF destinations, not a cloud-backed provider. Review current OCR's three-authority allowlist explicitly: Downloads/OEM providers need offline provenance/capability checks and Android tests before adding support, not an unreviewed blanket content-URI allowance.

Acceptance: real Android provider-stream tests on API26/current APIs, rotation/cancellation/death boundaries, duplicate/merge/reference retention, malformed and large streamed archives, original-file deletion, write denial and exact bytes after export/reimport. Physical SAF/SD/camera/photo-picker/EXIF and external share apps still need actual devices; injected providers do not substitute for those outcomes.

## 2. Reminder/widget completion, not just extra controls

K031–032/K073–074/K093 remain partial: current reminders are future-only, inexact and open the original note. Specify recurrence, explicit snooze/complete and missed-power-off policy first. A large catalogue should reconcile against platform alarm quotas; consider scheduling the next due event instead of one unbounded alarm per note. Keep optional notification permission and avoid promising exact delivery without the corresponding permission/policy.

K020/K089–092 remain partial or pending: the current widget is a four-title projection, not quick-capture, selected-note content, live checklist interaction or per-instance reconfiguration. Persist configuration per widget ID, clean it on deletion, validate immutable action identities, and make writes use the same authoritative repository transaction. Multiple placed instances must not share accidental state.

Acceptance: stale notification actions after edits/Trash/import, duplicate delivery, rearm/reboot/clock/timezone changes, denied permission, catalogue scale, independent widget instances and idempotent checkbox actions. Add real launcher place/remove/re-place/resize/configure, Doze and OEM background-delivery trials before physical reliability claims.

## 3. Complete editor/search/Keep interactions with explicit boundaries

Priorities include full undo/redo, markdown/list triggers (K028–029), hashtag labels (K068), checklist gestures/nesting (K046–050), rich formatting/card fidelity (K027), drawing/stylus capture (K016/K061–062) and visual/interaction polish (K094–099). Editable vector diagrams are a distinct delivered feature, not a substitute for a freehand drawing canvas.

Search currently runs off-thread; qualify a large representative index, cancellation and stable selection/scroll/IME behavior before an unlimited-scale/no-lag claim. Define startup corruption recovery and generation compaction without rolling back a valid newer save. Test non-Latin text, emoji/surrogates, composing input, TalkBack focus and dark/light contrast on every new surface.

**Wall drag ordering K004 stays last**, after lifecycle, storage and other product interactions. Do not spend this stage on wall movement instead of missing capture/data features.

## 4. Pipeline/evidence policy for every slice

- Keep the independent `mynotes/` build; do not rebuild the frozen calculator/MySpace. Use the current branch and Actions-only Android builds.
- Retain the **30-expanded-job ceiling** of the main workflow. Extend test discovery and per-lane groups rather than accidentally adding matrix jobs beyond the budget; a new class must be covered by discovery, exact method accounting and explicit lane selection.
- Preserve API26 minimum coverage, current Android, real Activity/display resource checks and optional permission positive/negative cases. Keep genuine prior-release seeded update and exact installed-byte checks in every release lane.
- Keep native component soaks separate and nonpublishing; report actual elapsed time/cycles, source identity and sampled retained-memory metrics. A component/debug soak is not final-release UI, microphone, battery, thermal, concurrent OCR/ASR or physical ARM/16-KiB qualification.
- Local Notes/root gates and whitespace checks precede commit/push. Publish only after every required source/JVM/build/audit/emulator gate is green, with a higher versionCode for changed runtime inputs. Never replace previous APK assets.
- Record findings **and outcomes**, including failed qualification and untested boundaries. Bigger case counts alone are not a speedup, accuracy score or full-product completion percentage.

## Physical acceptance priority

1. Real English speech, Indian accents/noise/silence, mic cancellation and audio route changes; verify original WAV playback/export even on inference failure.
2. Hindi/Kannada/English camera and rotated low-contrast pages, not only generated system-font images.
3. Local SAF/SD provider durability and external sharing, then launcher and notification/reboot/Doze behavior.
4. Physical ARM low-memory/thermal and 16-KiB-page devices; static ELF alignment is only preliminary evidence.
5. TalkBack, hardware/software IME, 200% font and lifecycle stress on the final release bytes.

Play publication and eventual MySpace integration are separate later gates, not completed by a passing standalone workflow.
