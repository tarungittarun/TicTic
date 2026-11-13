# Widget concepts manual validation checklist

Use this checklist on an Android 8.0+ device or emulator with a launcher that supports app widgets. These steps are runtime validation; passing Python source contracts, XML parsing or a Gradle compile does not prove launcher behavior.

## Install and catalog

- Build and install `:app:assembleLiteDebug` locally with Android Studio/SDK (the widget-concepts CI workflow compiles but does not upload or publish an APK). Confirm it installs as **MySpace** and upgrades the currently installed MySpace package without losing saved alarms or routines.
- Open the Widgets catalog. Confirm it lists **35 designs** (5 core, 15 curated styles and 15 purpose-built concepts); ensure provider identities remain unique across the 39 registered providers.
- Add at least one legacy/core widget and one provider from each purpose-built family: alarm, world-clock, calendar, routine, focus/timer and stopwatch/laps. Confirm existing placed providers still render and open the expected screen.

## Data-backed content and actions

- Create enabled, disabled, recurring and one-off alarms with different sounds/reminder defaults. Check Alarm Stack, Wake Countdown, Wake Switch and Quick Alarm. Toggle an alarm from Wake Switch; confirm the normal alarm scheduling and reminder rules are used.
- Try Quick Alarm at 10, 30 and 60 minutes, including denied notification/exact-alarm permissions. Confirm the established app preflight is shown and no duplicate alarm is created after activity recreation.
- Inspect World Grid's fixed Home/London/New York/Tokyo clocks and Meeting Window's London/New York work-hour status across time-zone and date transitions; verify Routine Stack/Countdown respond to create, edit, delete, pause and scheduled start/end.
- Start, pause, resume and finish timers; inspect Timer Live and Focus Launch. Start and reset the stopwatch with several laps; verify Lap Insights updates with latest, fastest and average splits.
- Exercise Month Grid in at least one broad/tall launcher size and one compact/small size. Resize both directions and verify the full six-week grid and compact seven-day structure switch without clipped labels; confirm today highlighting and tap-through navigation.

## System variations

- Repeat placement, resize, action and refresh checks on supported API 26+ devices. Check Chronometer rendering, reboot/time-zone/time/date changes, screen density, dark/light app appearance and launcher-specific minimum dimensions. API 23 is below the current minSdk; the pre-24 text fallback is a source branch only and is not a supported device test.
- Verify no stale values after app process death, adding/removing a widget, alarm changes, routine occurrences, timer changes, stopwatch laps, midnight and daylight-saving/time-zone changes.
- Inspect logcat for crashes, RemoteViews exceptions, rejected broadcasts and exact-alarm/notification permission failures. Record device/API/launcher, reproduction steps, and screenshots for failures.

**Status:** not performed by source tests or CI compilation. Report actual device runs separately; do not infer a pass from a successful build.
