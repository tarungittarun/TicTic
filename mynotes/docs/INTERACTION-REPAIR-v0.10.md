# MyNotes 0.10 — interaction repair

**Published: MyNotes 0.10.0 (11).** [Download APK](https://github.com/tarungittarun/TicTic/releases/download/mynotes-v0.10.0-11/MyNotes-v0.10.0-11.apk). [Final publication run40642752968](https://github.com/tarungittarun/TicTic/actions/runs/40642752968) passed all required gates and all 21 Android lanes. Earlier pending/failure statements below are the investigation history; none was waived.

## Device report and evidence boundary

The user supplied physical-device captures of empty Library / Filter sheets and a diagnostic export after restarting. The export reports Android16, dark mode and 0.85 font scale. It records repeated menu taps but does not establish the precise rendering failure or a crash/memory-leak diagnosis. Personal attachments are not committed to the repository.

Previous green emulator totals did not cover this longer navigation sequence or demonstrate reliable selection on this device. This repair must be checked on the reported interaction paths, not presented as OEM certification.

## Information architecture

| Location | Purpose and actions |
| --- | --- |
| Editor **+ / Add to note** | Scan image, record voice, diagram, reminder. |
| Editor **A / Text formatting** | Paragraph Text/H1/H2 and inline formatting only. |
| Editor **palette / Colour** | Opens colour picker directly; no labels or reminder hidden here. |
| Editor **tag / Labels** | Opens labels directly with its own icon/name. |
| Editor **More** | Share, export PDF, conversion and checklist-specific editing. No Copy or Trash. |
| Notes list selection | Long-press a card, select more cards if needed, then Pin/Archive/Trash or More → Copy/Labels/Colour/Share/Merge. Copy preserves originals and their structured contents. |
| Library | Notes, Archive, Trash, Voice inbox, Settings. Diagnostics stay default-off in Settings. |

A saved reminder is outside the editable note body: a rounded surface with bell, accent date/time, **Edit reminder** accessible name and separate Clear action. Tapping its date opens the date/time chooser. It is not editable text or an external web/calendar hyperlink.

## Rendering and selection changes

- Sheet content no longer depends on measuring a stack of hidden sibling panels. Each opening mounts only the chosen panel in a fresh ScrollView, resetting transient scroll/focus state. Inactive panels remain outside that measurement path. Page reset disposes panel contents.
- Diagnostics record sheet row count and measured/viewport heights, not note content. This makes another blank-sheet report more actionable than a tap-only log.
- Editing explicitly disables select-all-on-focus. Native text selection remains in charge; selection drags are protected from enclosing document scroll interception. Ordinary drag scrolling without a selection remains available.
- New real-pointer long presses require exactly `bravo` in a multiword note and exactly `03` in a multiline numeric note, not a full-note selection.
- A repeated native sequence alternates Library, Filter, Settings, editor tools and colour, including switching sheets without rebuilding the page. After 12 cycles it must still reach Settings without restarting.

## Reminder delivery defect

The old reconciliation treated only future dates as scheduled. A save after an inexact alarm's due time could therefore cancel that still-undelivered alarm. Repeated saves also re-registered unchanged future alarms.

The candidate keeps unchanged future schedules, catches up due reminders from the durable snapshot rather than cancelling them during a save, deduplicates notifications, and rebuilds schedules at startup/boot/update. Deleting, clearing or changing the reminder still invalidates the old notification. New tests cover schedule timestamps, a save across the due boundary, notification permission and duplicate suppression.

Blocked app/channel notifications now produce an explicit explanation with a Notification settings action. Diagnostics distinguish scheduling, delivery and blocked-channel outcomes. Android inexact-alarm delivery, Doze/OEM restrictions and force-stop remain real limits: this is not an exact-to-the-minute alarm guarantee.

## Qualification

Source run40637486745 found two test/fixture assumptions that needed correction: an obsolete assertion required Labels to be hidden (it is deliberately a direct icon now), and the release-upgrade seeder expected a landscape title while the previously shipped app intentionally hid it behind an open keyboard. The new fixture closes the real keyboard when required; it still enters and verifies the seed through the real release UI.

Run40638337811 qualifies the follow-up, including 75 JVM cases /123 unique Android cases across 21 lanes. The API36 lane now explicitly uses the reported dark/0.85-font configuration; light/default-font and large-font variants remain in the other lanes. Native captures and full results are required before approval. Local gates before pushing: 505 root checks and 115 Notes checks passed.

Calculator/MySpace remain untouched. Package/signing identity are unchanged, and the candidate versionCode rises to11. Publication stays gated until actual new captures and required runtime checks are accepted.

## Reviewed follow-up outcome

[Run40638337811](https://github.com/tarungittarun/TicTic/actions/runs/40638337811) completed with all 21 Android lanes passing, including **123/123 methods in all six complete resource variants**: light, dark, 200% font, landscape, tablet, and API36 dark/0.85 font. All required source/domain/lint/APK audits passed. Its overall summary correctly withheld publication while review approval was pending.

The new captures were inspected, including direct Labels/Colour controls, Plus → Reminder, readable heading layout, landscape editor, and Settings after repeated navigation in dark mode and Android16/0.85 font. Native exact-word/numeric-line long presses, repeated sheets and list-only Copy/Trash assertions pass. Reminder reconciliation boundary/permission/deduplication checks pass. The runtime PDF now uses the source-pinned app version and adds the actual Add/reminder and post-navigation evidence page.

Approval pins source `57990d0d0cd794cc7f2fc5b051eb3b658235b91a` and this reviewed run. A separate complete gated run must succeed before an immutable APK is shared. The precise original Motorola rendering cause was not established by the export; this repair and its passing sequence are not a claim of physical-device certification.

## Publication guard caught automatic selection expansion

Run40639953784 blocked publication because API26 selected `Alpha bravo` when the real long press targeted `bravo`. This is a meaningful selection failure, not a test to weaken or retry blindly. The follow-up disables Android TextClassifier smart expansion on editable fields (`TextClassifier.NO_OP`); native selection handles, manual Select All and clipboard editing remain enabled. Prior visual approval has been cleared again. Fresh qualification is required; there is no published v0.10 APK yet.

## Fresh classifier-corrected qualification

[Run40641156198](https://github.com/tarungittarun/TicTic/actions/runs/40641156198), source `019459a9db566a5df646c315062e1a9f7c90e59f`, passed all 21 Android lanes and required source/domain/lint/APK gates. All six full resource profiles passed **123/123**, including the exact-word/numeric-line native selection checks and API36 dark/0.85 font. New source-pinned captures were inspected and the 17-page runtime PDF regenerated. Approval now pins only this corrected source and run. The failed publication was not bypassed; a fresh complete gated publication run is required.

## Immutable delivery

Final source `07fb72edff13aca5eca49ab6e410634bdcd052e0`; 66522692 bytes; SHA-256 `2ffc9457ee3ac0f6508d26981e60245cff9afebcfb1bd94e0db9475adb2aeb03`. Publication and acceptance jobs succeeded. Fresh classifier-corrected qualification and final publication both pass the strict native word/numeric-line selection checks and 123/123 cases in all six full-resource profiles. Existing package/certificate and schema are unchanged; versionCode11 installs over code10 without clearing data. Physical Motorola confirmation and exact alarm timing are not certified.
