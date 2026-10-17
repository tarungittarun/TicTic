# TicTic v1.10.0 focused device QA

**Status: NOT RUN.** This is a test plan, not evidence of Android runtime behavior. No booted emulator or physical handset is available in this workspace; the earlier API 34 emulator attempt exited for insufficient host RAM and unavailable hardware acceleration. Record device model, Android/API, OEM skin, theme, steps, expected/actual, and evidence for every executed scenario.

## A. Upgrade and volume-key migration

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| A01 | Install v1.10.0 clean | App launches; versionName 1.10.0 / versionCode 20; no crash. | NOT RUN |
| A02 | Upgrade v1.9.9 with stored Control volume preference | Settings now selects Snooze. Either volume key on the active ringing screen snoozes; it never changes ring volume. | NOT RUN |
| A03 | Upgrade with stored Turn off alarm key preference | Settings selects Dismiss. A volume key stops only the current ring; it does not disable a repeating alarm. | NOT RUN |
| A04 | Upgrade with existing Snooze or Dismiss preference | That choice is retained. | NOT RUN |
| A05 | Inspect Volume Up + Down settings | Only Snooze and Dismiss are offered; selected choice is clear. No Control volume or Turn off alarm preference remains. | NOT RUN |
| A06 | Active ring with Snooze chosen; press Up and Down separately | Each key triggers one snooze, stops the current ring, and uses this alarm’s configured snooze interval. | NOT RUN |
| A07 | Active repeating alarm with Dismiss chosen; press Up and Down separately | Either key stops this ring; repeating schedule stays enabled for its next occurrence. | NOT RUN |
| A08 | Check notification and on-screen controls | The separate notification Turn off action and explicit ringing-screen controls remain available as appropriate; changing key preference does not remove them. | NOT RUN |
| A09 | Repeat with screen off, keyguard, app background, notifications allowed/blocked | Only claim key behavior on the active ringing screen. After waking, notification/on-screen actions work where Android permits. Power key is not remapped. | NOT RUN |

## B. Timer and stopwatch settings

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| B01 | Open Settings → Timer and Stopwatch | Timer exposes hundredths, button haptics, completion alerts and completion vibration. Stopwatch exposes hundredths, button haptics and lap haptics. | NOT RUN |
| B02 | Timer hundredths off/on while running | Off uses the compact whole-second display; on shows `MM:SS.hh` or `HH:MM:SS.hh`; countdown remains monotonic and responsive. | NOT RUN |
| B03 | Toggle timer button haptics and use preset, Start, Pause, Reset | Haptic feedback follows the setting; timer timing/persistence is unchanged. | NOT RUN |
| B04 | Test timer completion alert/vibration combinations | Notification and vibration switches work independently, subject to Android notification/channel/DND settings. | NOT RUN |
| B05 | Stopwatch hundredths off/on during a run | Display changes only; elapsed time and lap/split storage remain precise to hundredths. | NOT RUN |
| B06 | Toggle stopwatch button haptics and lap haptics independently | Start/Pause/Reset and Lap feedback follow the relevant independent settings. | NOT RUN |
| B07 | Pause/resume/reset stopwatch; record laps | Timing and lap/split values remain stable, no delayed visible refresh or unexpected reset. | NOT RUN |

## C. Settings layout, elevation and glass material

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| C01 | Scroll Design settings down; change Compact/Balanced/Airy, Soft/Crisp corners, accent, font | Settings rerenders at the same scroll position; no jump to the top. | NOT RUN |
| C02 | Change Settings navigation group | New group intentionally opens at its top; switching back starts at the top of that group. | NOT RUN |
| C03 | Compare Flat, Soft and Raised card elevation in Light and Dark | Flat has no added lift; Soft has subtle shadow/contrast; Raised has clearly stronger shadow and edge separation. Text remains legible. | NOT RUN |
| C04 | Select Glass material and adjust opacity from high to low | Live sample and app surfaces update; transparency is visible and text remains readable. | NOT RUN |
| C05 | Move Finish from Matte to Glossy | Matte is quieter; Glossy adds a restrained highlight without obscuring content. | NOT RUN |
| C06 | Move Depth from Subtle to Floating | Surface edge/shadow changes visibly; no clipped shadows or layout overflow while scrolling. | NOT RUN |
| C07 | Leave Design, return, restart app, switch light/dark | Material and slider settings persist and apply consistently across pages/dialogs; no text/background contrast failures. | NOT RUN |
| C08 | Choose Standard mode | App returns to the solid-surface appearance; theme, accent and layout preferences remain intact. | NOT RUN |

## D. Optional routine and reminder names

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| D01 | Save a reminder with a blank title | Saves successfully as the next unused label, e.g. `Reminder 01`. | NOT RUN |
| D02 | Existing `Reminder 01` and `Reminder 03`; save a blank reminder | Generates `Reminder 02`, avoiding a duplicate sequence label. | NOT RUN |
| D03 | Save a routine with a blank name | Saves successfully as the next unused `Routine NN` label. | NOT RUN |
| D04 | Edit an existing unnamed reminder/routine and leave name blank | Saves with a free sequence label while preserving the same record ID and schedule. | NOT RUN |
| D05 | Provide a user-defined name | User's non-empty name is retained exactly; no automatic renaming. | NOT RUN |

## E. Regression and compatibility

- Re-run the full v1.9.9 alarm/reminder/timer/stopwatch/device matrix in [`qa/QA-v1.9.9-manual.md`](QA-v1.9.9-manual.md), plus the 58-case base regression workbook.
- Recheck alarm create/edit visibility, dial touch boundary and scrolling, tone previews, notification actions, exact-alarm/full-screen access paths, reboot/time-change recovery, reminders, widgets/Widget Studio, Usage, and weather opt-in/offline behavior.
- Include min API 23, Android 12+ exact-alarm behavior, Android 14+ full-screen access, a current Android release, a major OEM skin and the user's target handset.
- Report actual passed/failed/flaky/blocked scenario counts and coverage. Do not invent a timing accuracy percentage or claim iOS/Google Clock parity.
