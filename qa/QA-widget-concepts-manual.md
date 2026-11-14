# Widget concepts manual validation checklist

Use this checklist on a supported Android device or emulator with a launcher that supports app widgets. The cases below are **manual runtime coverage** and remain **NOT RUN**. Python source contracts, XML parsing, a Gradle build, and the partial API 34 journeys in `TESTING-widget-concepts.md` do not prove every launcher/device behavior described here.

## Install and catalog

- Build and install `:app:assembleLiteDebug` locally with Android Studio/SDK (the widget-concepts CI workflow compiles but does not upload or publish an APK). Confirm it installs as **MySpace** and upgrades the existing package without losing saved alarms or routines.
- Open the Widgets catalog. Confirm it lists **35 designs** (5 core, 15 curated styles, 15 purpose-built concepts) and that all **39 registered providers** remain unique and available.
- Place one legacy/core widget and at least one provider from each purpose-built family: alarm, world clock, calendar, routine, focus/timer, and stopwatch/laps. Confirm existing placed providers still render and open their expected screens.

## Concept behavior matrix — manual checks not run

| ID | Widget / scenario | Expected result | Status |
|---|---|---|---|
| WC01 | Wake Countdown: no alarms, then enabled one-time and repeating alarms; edit, disable, delete, date/time-zone change | Empty state is readable. Otherwise the soonest enabled occurrence and label are shown; supported RemoteViews countdown updates without app launches, and data changes refresh the widget. | NOT RUN |
| WC02 | Alarm Stack: zero to four enabled alarms with varied labels/repeat days, plus disabled alarms | Shows up to three soonest enabled occurrences in order; unused rows are blank rather than stale. | NOT RUN |
| WC03 | Wake Switch: enabled, disabled, and no-alarm states | One tap toggles the selected saved alarm and updates scheduling/widget state without opening a form; with no alarm, the safe Alarms route opens. | NOT RUN |
| WC04 | Quick Alarm: tap 10, 30, and 60 minute actions with permissions granted and denied | Uses the existing save/notification/exact-alarm/full-screen preflight; creates no duplicate after activity recreation; the scheduled time is never earlier than requested and saved alarm defaults are respected. | NOT RUN |
| WC05 | World Grid: 12/24-hour setting, timezone/date change, and DST transition | Home, London, New York, and Tokyo show the correct local times in their respective zones without stale duplicates. | NOT RUN |
| WC06 | Meeting Window: weekday/weekend plus representative London/New York local times | Both clocks and the 09:00–17:00 local work-hour state agree with each location's own date/time, including DST changes. | NOT RUN |
| WC07 | Week Strip: locales whose first day is Sunday vs Monday; month/year boundaries | Seven dates align with localized weekday labels; today is highlighted exactly once and boundaries are correct. | NOT RUN |
| WC08 | Month Grid: leap/non-leap February and four-, five-, and six-row months; resize below/above compact threshold | Full grid headers align to locale week start; all 42 dates are correct, today is accented once, and adjacent-month dates are muted. Compact dimensions switch to a seven-day current-week strip and expansion restores the full grid without clipping. | NOT RUN |
| WC09 | Year Pulse: leap day and final day of year | Day-of-year, remaining days, and progress are correct and refresh on date change. | NOT RUN |
| WC10 | Day Balance: ordinary day and spring/fall daylight-saving transitions | Elapsed/remaining values reflect the actual local-day length (including 23- and 25-hour days), and stay within valid bounds. | NOT RUN |
| WC11 | Routine Stack: none, disabled, and repeating weekly routines; create/edit/delete/start/end | Shows the next three enabled occurrences in order with correct names/times; saved-data and scheduled-occurrence changes refresh the widget. | NOT RUN |
| WC12 | Routine Countdown: none, next enabled routine, pause/disable, and scheduled start/end | Empty state and next occurrence are correct; supported live countdown and refresh behavior agree with schedule changes. | NOT RUN |
| WC13 | Focus Launch: 25/50/90 minute actions with timer idle, paused, and already running | The selected duration becomes the active timer, intentionally replaces any prior countdown, preserves notification/scheduler behavior, and refreshes Timer Live. | NOT RUN |
| WC14 | Timer Live: start, pause, resume, reset/change duration, and completion | Action text/state and progress match timer state; start/pause/resume/change/completion refresh correctly, and completion retains existing alert settings. | NOT RUN |
| WC15 | Lap Insights: no laps, several laps, and reset | Latest, fastest, and average splits match Stopwatch; reset clears the displayed statistics. | NOT RUN |

## Cross-cutting launcher and system checks — manual checks not run

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| WX01 | Place catalog designs, including legacy widgets already on a home screen | Correct provider/layout appears; existing provider IDs and placements remain valid. | NOT RUN |
| WX02 | Resize each new concept to launcher minimum, wide, and tall sizes | Text remains legible, content does not clip, and direct-action targets remain usable; record launcher/OEM minimum-size behavior. | NOT RUN |
| WX03 | Configure and reconfigure every new concept in Widget Studio | Preview matches the provider/data; Apply changes only that widget; Cancel discards edits; chosen label/surface/style persists. | NOT RUN |
| WX04 | Light/dark app surfaces, accent, wallpaper colors, and larger system font sizes | Primary text and action-chip contrast remains usable; calendar accents survive custom text styling. | NOT RUN |
| WX05 | TalkBack or equivalent accessibility navigation | Wake Switch, Quick Alarm, Focus Launch, and Timer Live announce clear action descriptions and activate once per gesture. | NOT RUN |
| WX06 | Force-stop/restart, reboot, app update, data edits, and stale queued alarms/timers | Saved data remains intact; widgets reconstruct current state and existing alarm/timer scheduler semantics remain unchanged. | NOT RUN |
| WX07 | Background action security | `WidgetActionReceiver` remains non-exported; explicit widget PendingIntents work from the launcher without exposing an exported action endpoint. | NOT RUN |

## System variations, limits, and evidence

- The app's current minimum supported API is 26. Run on supported API 26+ devices and representative launchers/OEMs; do not treat API 23 fallback behavior as supported-device coverage. Check countdown rendering, timezone/time/date changes, reboot, density, dark/light appearance, and launcher-specific minimum dimensions.
- CI currently exercises a subset on an API 34 emulator. It does not cover a physical launcher resize gesture, every style/provider, all permission combinations, or the full alarm/timer/routine scheduling matrix. Runtime outcomes and screenshot completeness are recorded separately in [`TESTING-widget-concepts.md`](../TESTING-widget-concepts.md).
- Inspect logcat for crashes, RemoteViews errors, rejected broadcasts, and exact-alarm/notification permission failures. Record device/API/launcher, reproduction steps, expected/actual results, and authentic screenshots. Mark a case passed only after it is run; do not infer a pass from source inspection or compilation.
