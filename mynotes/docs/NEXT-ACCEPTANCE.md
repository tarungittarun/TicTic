# Next MyNotes acceptance slices (planned, not shipped)

This is the continuation plan alongside the **0.11.0(12) candidate**, not a claim that the full Keep inventory is complete. Current outcomes and qualifications are in [REPORT-CARD.md](REPORT-CARD.md); the original permanent requirement IDs remain in [REQUIREMENTS.md](REQUIREMENTS.md).

## 1. Media foundations implemented; remaining portability boundaries

The 0.11 candidate implements owned image attachments, freehand drawing with durable drafts, 16 backgrounds, physical local SAF folders/moves, schema6 with reads1–6, self-contained bounded image backups and SHA-verified format2 selected-folder media generations (format1 remains readable). See [organisation/media outcomes](ORGANISATION-AND-MEDIA-v0.11.md) for qualification status and exact limits. Earlier notes saying there are no image attachments are superseded.

Still not delivered: embedding original voice recordings in library backups, arbitrary file attachments, media garbage collection/generation compaction, advanced stylus tools, or universal provider/power-loss atomicity. Original audio must be exported separately. Do not delete shared image blobs by age or visibility alone; future GC needs checked live/restorable references and a crash-safe grace period.

Retain bounds, digest/path validation, non-destructive merge preview, late-ASR deletion guards and last-good-generation fallback. Broader OEM/SD/Downloads providers need offline provenance/capability checks and physical evidence, not a blanket URI allowance. Physical SAF/SD/camera/photo-picker/EXIF and external share apps still need actual devices; emulator/provider tests do not certify those outcomes.

## 2. Reminder/widget completion, not just extra controls

K031–032/K073–074/K093 remain partial: current reminders are future-only, inexact and open the original note. Specify recurrence, explicit snooze/complete and missed-power-off policy first. A large catalogue should reconcile against platform alarm quotas; consider scheduling the next due event instead of one unbounded alarm per note. Keep optional notification permission and avoid promising exact delivery without the corresponding permission/policy.

K020/K089–092 remain partial or pending: the current widget is a four-title projection, not quick-capture, selected-note content, live checklist interaction or per-instance reconfiguration. Persist configuration per widget ID, clean it on deletion, validate immutable action identities, and make writes use the same authoritative repository transaction. Multiple placed instances must not share accidental state.

Acceptance: stale notification actions after edits/Trash/import, duplicate delivery, rearm/reboot/clock/timezone changes, denied permission, catalogue scale, independent widget instances and idempotent checkbox actions. Add real launcher place/remove/re-place/resize/configure, Doze and OEM background-delivery trials before physical reliability claims.

## 3. Complete editor/search/Keep interactions with explicit boundaries

Priorities include full undo/redo, markdown/list triggers (K028–029), hashtag labels (K068), checklist gestures/nesting (K046–050), rich formatting/card fidelity (K027), advanced drawing/stylus tools beyond the new freehand foundation (K016/K061–062) and visual/interaction polish (K094–099). Editable vector diagrams are a distinct delivered feature, not a substitute for a freehand drawing canvas.

Search currently runs off-thread; qualify a large representative index, cancellation and stable selection/scroll/IME behavior before an unlimited-scale/no-lag claim. Define startup corruption recovery and generation compaction without rolling back a valid newer save. Test non-Latin text, emoji/surrogates, composing input, TalkBack focus and dark/light contrast on every new surface.

**Wall drag ordering K004 stays last**, after lifecycle, storage and other product interactions. Do not spend this stage on wall movement instead of missing capture/data features.

## 4. Pipeline/evidence policy for every slice

- Keep the independent `mynotes/` build; do not rebuild the frozen calculator/MySpace. Use the current branch and Actions-only Android builds.
- Retain the **45-expanded-job ceiling** of the main workflow. Extend test discovery and per-lane groups rather than accidentally adding matrix jobs beyond the budget; a new class must be covered by discovery, exact method accounting and explicit lane selection.
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
