# TicTic v1.11.0 focused device QA

**Status: NOT RUN.** This is a test plan, not evidence of Android runtime behavior. Run on a physical device and a supported emulator where possible. Record model, Android/API level, OEM, theme, glass configuration, permission state, steps, actual result and evidence for each case. The workspace SDK/emulator has to be provisioned again; no device is attached here.

## A. Ringing controls, notifications and volume keys

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| A01 | Clean install and upgrade from v1.10.0 | VersionName 1.11.0 / versionCode 21; alarm data and unrelated settings remain intact. | NOT RUN |
| A02 | Upgrade with legacy shared Volume Up + Down = Snooze | Both independent volume-key settings migrate to Snooze. | NOT RUN |
| A03 | Upgrade with legacy shared Volume Up + Down = Stop/Dismiss or retired Turn off | Both independent settings migrate to Stop; no migration disables a repeating alarm. | NOT RUN |
| A04 | Settings → Alarms → While an alarm is ringing | Dedicated card separates screen/notification buttons from independent volume-key shortcuts; choices are Snooze + Stop or Stop only. Explanations match where actions apply; Stop is always present and no active-ring Turn off choice exists. | NOT RUN |
| A05 | Ring with Snooze + Stop selected, screen awake | Full-screen alarm UI has Snooze and Stop only. Snooze uses the configured interval; Stop ends the current ring. | NOT RUN |
| A06 | Ring with Stop only selected, screen awake | Full-screen UI has Stop only; Stop ends the ring. | NOT RUN |
| A07 | Inspect active ringing notification and lock-screen actions in both action modes | Snooze + Stop mode shows at most Snooze and Stop; Stop-only mode shows Stop. No action disables the repeating schedule. Check behavior when notification permission is denied or channel/lock-screen content is restricted. | NOT RUN |
| A08 | Ring a repeating alarm; tap Stop from screen and notification | Current ring ends, repeating alarm remains enabled and next scheduled occurrence stays queued. | NOT RUN |
| A09 | Ring once; tap Stop | Current ring ends; no unintended recurring schedule is created. | NOT RUN |
| A10 | Set Volume Up = Snooze, Volume Down = Stop; ring and press each key separately | Up snoozes once; Down stops only the current ring. No framework volume change while the active ringing screen consumes the key. | NOT RUN |
| A11 | Reverse per-key mappings; test key repeat and key-up | Each key consistently performs only its selected action once. | NOT RUN |
| A12 | Set both keys to Snooze, then both to Stop | Both mappings act as selected, without accidentally turning off a repeating schedule. | NOT RUN |
| A13 | Wake from screen-off/keyguard and test lock-screen notification | Android/OEM behavior is recorded honestly; Power key remains system-owned and is not remapped. | NOT RUN |
| A14 | Inspect separate early pre-alarm reminder | Its separately documented Turn off alarm action remains distinct from the active ringing controls. | NOT RUN |

## B. Usage report and timeline

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| B01 | Open Usage | Today / 7 days / 30 days are presented as one segmented selector with a clear selected state and date-range caption, visually distinct from generic action buttons. | NOT RUN |
| B02 | Switch among all periods | Summary totals and Recent history update to the selected local-day window; counts/dates agree with stored samples. | NOT RUN |
| B03 | Select Today | Window starts at local midnight and caption identifies today. | NOT RUN |
| B04 | Select 7 days / 30 days across a date boundary | Window includes selected calendar days through today; date caption and filtered history match. | NOT RUN |
| B05 | Tap Save usage report · CSV; cancel picker | No file is written and the app remains usable. | NOT RUN |
| B06 | Save CSV to local Documents | File opens in a spreadsheet/text editor; includes selected date range, generated time, metric status, usage totals, widget refreshes, event aggregates and retained activity rows. CSV quoting/newlines are valid. | NOT RUN |
| B07 | Export with history saving paused and with metrics paused | Report marks statuses accurately and does not imply paused metrics are zero activity. Existing retained history is handled consistently with the Usage screen. | NOT RUN |
| B08 | Export with quotes/newlines or non-ASCII labels/details | CSV escapes embedded quotes and emits valid UTF-8, preserving readable text. | NOT RUN |
| B09 | Inspect report for privacy | Labels/details can be present, as disclosed before export. File is created only after user selects a destination; no network upload/share occurs automatically. | NOT RUN |

## C. Coordinated glass design and contrast

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| C01 | Enable Glass; compare Frosted, Luminous and Sculpted | Three distinct but coherent surface characters; preview changes immediately and does not cover text. | NOT RUN |
| C02 | Sweep opacity (15–100%), Matte-to-Glossy finish and Subtle-to-Floating depth from low to high | Live preview visibly changes at both extremes and through the range; the same settings affect app surfaces consistently and persist after page changes/restart. | NOT RUN |
| C03 | Visit Clock, Reminders, Timer, Stopwatch, Routines, Usage and every Settings group | Shared glass surfaces, background tint, edge/highlight and corner treatment are consistent. Primary accent actions remain clear. | NOT RUN |
| C04 | Open alarm editor, dialogs, Widget Studio and widget catalog | App-owned editor chrome/dialog surfaces use the same material; Widget Studio’s actual widget preview remains faithful to the selected widget, not overlaid with app styling. | NOT RUN |
| C05 | Trigger alarm in Light and Dark themes with Glass enabled | Ring screen follows the shared glass character while keeping Snooze/Stop labels and surfaces legible; opacity does not undermine critical action contrast. | NOT RUN |
| C06 | Try minimum opacity, maximum Glossy/Depth and all accents in Light/Dark | Main text, labels, selected/unselected controls, fields, popups and alarm actions remain readable; record any contrast failure. | NOT RUN |
| C07 | Toggle Standard/Glass, switch themes, restart app | Standard restores solid app panels; Glass settings/character persist and apply consistently. | NOT RUN |
| C08 | Inspect statement about live blur/screenshots | UI accurately describes a drawn layered effect; no screenshot or live blur claim is made. | NOT RUN |
| C09 | Tap and hold buttons/cards on Clock, Settings, alarm screen and Widget Studio | Surface gently widens/compresses while pressed, gives clear contact feedback and springs back without obscuring labels, triggering twice or blocking accessibility. | NOT RUN |
| C10 | Drag and fling vertically to both ends of the main and Settings screens | Scroll gives a restrained elastic response; pulling past the top/bottom stretches the content and returns smoothly without clipping, layout drift or scroll-position loss. | NOT RUN |

## D. Regression and compatibility

- Re-run the v1.10.0 manual cases in [`qa/QA-v1.10.0-manual.md`](QA-v1.10.0-manual.md) and the 58-case base regression workbook.
- Recheck alarms/reminders, sound routing, exact-alarm and notification permission paths, notification channels, screen-off/keyguard/full-screen behavior, timer/stopwatch, widget placement/reconfiguration, offline core, optional manual-city weather and reboot/time-change handling.
- Include at least one target handset, Android 12+ and Android 14+ permission behavior, and a major OEM skin. Separate OS/launcher-dependent outcomes from app defects.
- Report actual executed/pass/fail/flaky/blocked counts. No timing-accuracy percentage or same-device Google Clock parity is inferred from build/static checks.
