# MyNotes 0.10 — interaction repair

Status: candidate 0.10.0 (11), not yet published. Prior 0.9 approval was deliberately cleared.

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
