# TicTic v1.9.9 focused device QA

**Status: NOT RUN.** No emulator booted and no physical handset is attached. An Android 14/API 34 x86_64 emulator launch was attempted but exited before boot because the host lacked RAM (the emulator requested 2.5 GB; about 1 GB was available) and hardware acceleration was unavailable. No runtime scenario was exercised. This is a test plan, not evidence of device behavior. Mark each case PASS / FAIL / BLOCKED only after executing it; record device model, Android/API level, OEM skin, theme, notification/full-screen/exact-alarm permissions, steps, and evidence.

## A. Install, upgrade and basic regression

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| A01 | Clean install v1.9.9 | Launches; package/version shown correctly; no crash; defaults are sane. | NOT RUN |
| A02 | Upgrade v1.9.8 with alarms, reminders, running timer, and running stopwatch state | Existing records and settings remain; active timer/stopwatch resumes without a jump or reset; no duplicate delivery. | NOT RUN |
| A03 | Switch Clock → Reminders → Timer → Stopwatch → Routines → Usage → Settings and back | No blank page, stale content, input/color regressions, or crash; Clock/Reminders remain adjacent. | NOT RUN |
| A04 | Repeat A03 in system/light/dark themes and narrow/tall display | Labels and controls remain legible, sized, and scrollable. | NOT RUN |

## B. Alarm volume-key actions and system limits

The options are **Control volume**, **Snooze**, **Dismiss ring**, and **Turn off alarm**. Google Clock documents the first three; TicTic adds Turn off. Both Volume Up and Volume Down use the same setting. Power-key remapping is not a supported feature.

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| B01 | Inspect Settings → Alarms in light/dark mode and portrait/landscape widths | 2×2 choices are fully visible; selection is unmistakable; helper text explains both keys, active ringing surface, Power-key limit, and lock-screen notification. | NOT RUN |
| B02 | Trigger alarm while TicTic is foreground; choose Control volume | Either volume key adjusts the alarm stream without stopping/snoozing the alarm. | NOT RUN |
| B03 | Trigger alarm; choose Snooze; press each volume key separately | Each key snoozes once; sound/notification/ringing UI stop; correct snooze interval and next alarm state display. | NOT RUN |
| B04 | Trigger repeating alarm; choose Dismiss ring | Ring stops and notification clears; repeating alarm remains enabled for its next occurrence. | NOT RUN |
| B05 | Trigger repeating alarm; choose Turn off alarm | Ring stops, future schedule is disabled, next-alarm/widget state updates. | NOT RUN |
| B06 | Repeat B02–B05 with screen unlocked, app foreground/background, keyguard, screen off, notification permission on/off, full-screen access on/off | Full-screen alarm appears only where Android permits. Notification actions remain the fallback where available. Log OEM-specific volume-key routing; do not expect Power-key interception. | NOT RUN |
| B07 | Press Power while ringing, then wake screen | Android controls the Power key; TicTic must not claim it stopped/dismissed the alarm. Lock-screen notification actions are usable after waking where Android permits. | NOT RUN |
| B08 | Verify screen-off alarm after app swipe-away and after device reboot | Sound and notification behavior are correct subject to exact-alarm, full-screen, notification, battery and OEM policy; no duplicate rings. | NOT RUN |

## C. Tone audition and alarm/reminder workflow

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| C01 | Preview each bundled alarm tone and the device alarm tone in Settings and alarm editor | Correct sound plays; another preview replaces it; playback stops within the bounded preview window. | NOT RUN |
| C02 | Preview bundled ping, system notification, alarm-linked, custom and silent reminder options | Correct tone plays for audible choices; silent gives an explanatory message; custom URI errors are handled without crash. | NOT RUN |
| C03 | Close editor while preview plays; press Home/Back or launch Android picker during preview | Preview stops on dialog dismissal or Activity pause; no playback is left running. | NOT RUN |
| C04 | Save alarm/reminder after audition | Saved selected tone, duration, reminder lead, vibration and repeat behavior are unchanged by preview; no unintended alarm is scheduled. | NOT RUN |
| C05 | Create/edit/delete one-time and weekly alarms/reminders; test past/near lead times | Stable IDs, dates, weekdays, validation, scheduler updates and Usage history stay correct. | NOT RUN |

## D. Alarm clock-dial hit area and editor behavior

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| D01 | Tap/drag starting on dial face numbers and inner 24-hour ring | Time changes according to the selected hour/minute ring; hand/value update together. | NOT RUN |
| D02 | Start touch outside visible circular face, including nearby editor whitespace | Dial does not change time; vertical editor scrolling still works. | NOT RUN |
| D03 | Start inside face, drag beyond edge, cancel/interruption, then release | Gesture remains well behaved, parent scrolling resumes, no stuck selected field. | NOT RUN |
| D04 | Create/edit alarm in 12-hour and 24-hour modes; AM/PM and repeat/date variants | Correct time/date is saved and reopened; no editor invisibility or stale field. | NOT RUN |
| D05 | Repeat D01–D04 on small screen, large font/display scaling, and touch accessibility setting | Dial fits and touch targets remain usable. | NOT RUN |

## E. Stopwatch timing

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| E01 | Start stopwatch and observe at least 30 seconds | Time advances smoothly; hundredths visibly change at a readable cadence without one-second stalls or irregular wall-clock jumps. | NOT RUN |
| E02 | Record multiple laps | Cumulative lap and split values are ordered and stable to hundredths; newest lap display matches elapsed time. | NOT RUN |
| E03 | Pause, wait, resume | Paused value stays fixed; resume continues from that exact value. | NOT RUN |
| E04 | Navigate away, enter Settings, background/resume the app while running | Elapsed time remains continuous; returning to Stopwatch immediately refreshes instead of waiting a minute. | NOT RUN |
| E05 | Toggle hundredths, lap haptics; reset while running and paused | Display preference changes only presentation; lap data remains precise; reset clears timing/laps and allows clean restart. | NOT RUN |
| E06 | Restart device during an active stopwatch | Timebase recovery is plausible and stable; record the behavior and any wall-clock correction. | NOT RUN |

## F. Timer timing, notifications and recovery

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| F01 | Start 1-minute and short custom (1–10 second) timers | Display counts down in order without jumps, reaches zero once, and produces one completion notification. | NOT RUN |
| F02 | Set custom hours/minutes/seconds, including zero rejection and upper bound | Live preview follows each field; zero is rejected; valid value is stored. | NOT RUN |
| F03 | Pause, wait, resume; then reset | Remaining value freezes while paused, resumes accurately, reset restores selected full duration. | NOT RUN |
| F04 | Leave app, screen off, reopen app during countdown | Timer remains active, display matches elapsed time, no duplicate completion notification. | NOT RUN |
| F05 | Reboot during active timer | Boot receiver re-anchors remaining time and schedules exactly one completion. | NOT RUN |
| F06 | Move device wall clock forward/back while timer runs | TIME_SET handling re-anchors the same monotonic remainder and avoids premature/duplicate delivery. | NOT RUN |
| F07 | Android 12+ exact access allowed and denied | Prompt appears before start when denied; chosen path is honored; if denied, possible delay is disclosed and completion uses platform fallback. | NOT RUN |
| F08 | Toggle timer completion/vibration preferences; mute channel / deny notifications | Service honors app/channel settings and reports the actual notification limitations; no crash. | NOT RUN |

## G. Number input, fields and visual consistency

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| G01 | Open timer, custom reminder-lead, default reminder-lead and other NumberPickers in light mode | Selected value, wheel values, background, controls and input text have clear contrast; selection is easy to read. | NOT RUN |
| G02 | Repeat G01 in dark mode, with accent changes and each app typeface | No dark-on-dark/light-on-light text, clipped digits or low-contrast wheel values. | NOT RUN |
| G03 | Edit text fields, spinners, time dial, dates and buttons across alarm/reminder/routine dialogs | Shared typography, paddings, selection accents, rounded surfaces and title/close treatment feel consistent; OS-owned picker is documented as an exception. | NOT RUN |
| G04 | Test app-owned dialogs: X close, outside dismissal, Back, cancel and confirm | Dismissal does not accidentally save/delete; close actions behave consistently. | NOT RUN |

## H. Full regression and compatibility

- Re-run the 58-case base clock/alarm/timer/stopwatch regression workbook and all pre-existing v1.9.6–v1.9.8 manual cases.
- Verify reminder notification snooze/dismiss, notification channels, reboot and time-change recovery, Settings groups, app-card styles, Usage 20/100 behavior, all clock styles, routines, widgets/Studio, export and weather opt-in/offline behavior.
- Include API 23 minimum, API 31/32 exact-alarm policy, API 34 full-screen-intent policy, and a current Android release; include at least one major OEM skin and the user’s target device.
- Save app logs/screenshots or video only with consent; never include user data or credentials.
- Do not report a single “accuracy percentage” for this matrix. Report executed scenarios, pass/fail/flaky counts, device/API/OEM coverage and untested areas.
