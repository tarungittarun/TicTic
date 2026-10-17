# Historical TicTic QA record — v1.9.4

> Current v1.11.0 results: [`TESTING-v1.11.0.md`](TESTING-v1.11.0.md). This file records the older v1.9.4 test state.

Date: 2026-10-10

## Verification performed

- Android build: `ANDROID_HOME=/tmp/android-sdk ANDROID_SDK_ROOT=/tmp/android-sdk bash gradlew assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL** on Android SDK 34 / Build-Tools 34.0.0. Gradle `testDebugUnitTest` remains **NO-SOURCE**; there are no Android/JUnit test sources.
- APK metadata: `aapt dump badging` confirmed `app.tictic.clock`, version 1.9.4 / versionCode 14, min API 23, target/compile API 34. `apksigner verify --verbose` passed v1/v2 signing; `zipalign -c -v 4` passed. The benign Gradle app-metadata JAR-entry warning appeared; signature verification succeeded.
- Python source-contract suite: `python3 -m unittest discover -s qa -v` — **16/16 PASS**. These are structural checks, not Android runtime tests.
- QA workbook: [`QA-Clock-Comparison-v1.9.4.xlsx`](QA-Clock-Comparison-v1.9.4.xlsx) contains 38 Clock/Alarm/Timer/Stopwatch scenarios plus a Google Clock comparison. Functional scenarios are intentionally **NOT RUN**.
- Lint: not run on v1.9.4. The preceding v1.9.3 `lintDebug` attempt timed out in `lintAnalyzeDebug` without producing a report; no lint pass is claimed.
- APK: `/home/user/artifacts/TicTic-v1.9.4-debug.apk`, 650,429 bytes; SHA-256 `d738bc55b2b8cf5c39c08544060ffea7b70ec938c5814434de70bf218702282c`.

## Runtime validation status — NOT DONE

`adb` is installed, but `adb devices -l` returned an empty list. No emulator or physical phone is attached. I did not tap through the app, wait for an alarm to fire, listen to audio, or verify lockscreen/notification behavior. Compilation and source checks do not prove behavior on the user's handset.

## v1.9.4 notification regression

- **ALM-025:** verify the main ringing notification has **Turn off** for both one-time and repeating alarms. Tap it once; verify ringing stops and only that alarm plus its reminder/snooze schedules are disabled/canceled.
- The early reminder also has **Turn off alarm**; ALM-021 tests that independent notification action.

## Earlier v1.9.3 regression checks

1. Disable versus queued delivery: a disabled/deleted alarm must not start ringing from a stale delivery; cleared snooze delivery is ignored.
2. Edit existing alarm: change label/time/repeat/sound/custom lead, save, reopen, and confirm the same ID contains the changes and the schedule reflects the new time.
3. Custom reminder entry: select Custom or reopen an existing custom value; Set/Edit minutes must be visible immediately, and the saved value must persist.

These and the new notification check are still runtime-unverified.

## Highest-risk device checks

1. Upgrade a v1.9.1 installation and verify legacy 10-minute / “same as alarm” settings migrate to a 30-minute TicTic ping and are rescheduled. Historical data cannot distinguish a custom 10-minute value from the former default, so stored 10-minute values also migrate; other stored leads remain.
2. Set an alarm at least 35 minutes ahead with notifications and exact-alarm access granted. Confirm the 30-minute event is one short “Reminder” ping—not the alarm service/ringtone or ringing activity—then verify the main alarm rings at its target time.
3. Use a custom 1-minute lead for a quicker delivery check; also test the past-lead warning.
4. Test notification permission and reminder-channel allowed/denied/muted paths.
5. On the ringing UI and its notification, single-tap Snooze, Stop, and Turn off. Repeat with one-time and repeating alarms, locked and unlocked.
6. Test volume-key modes Control volume/Snooze/Stop. Power remains Android/OEM-owned and is not intercepted.
7. Verify upcoming/snoozed rows, cancel snooze, one-time disable, repeat, reboot/time-change restoration, and package-update migration.
8. Compare Clock, Alarm, Timer and Stopwatch workflows against Google Clock on the same handset; record device, Android/OEM and Clock versions, tap count, timing and surfaces. The workbook's Google Clock notes are documentation-based, not a hands-on comparison.
