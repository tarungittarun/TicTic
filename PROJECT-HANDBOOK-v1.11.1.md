# TicTic project handbook and engineering handoff

**Project:** TicTic, local-first Android clock/alarm app  
**Documented code version:** `1.11.1` (`versionCode 22`)  
**Disposition:** debug candidate builds successfully; handset/emulator QA remains pending. This handbook is a source-of-truth companion to `README.md`, `PRODUCT.md`, `CHANGELOG.md`, `TESTING-v1.11.1.md`, and `ARTIFACTS-v1.11.1.md`.

## 1. Project purpose and product principles

TicTic is an Android clock with alarms, reminders, timers, stopwatch, routines, and configurable home-screen clock widgets. The core app works offline and stores user state locally. Weather is an optional, per-widget network feature and is disabled until the user enables it for a supported widget and supplies a city.

Project-wide principles:

- Build useful clock/alarm workflows without an account, ads, remote analytics, or third-party app/runtime libraries.
- Ask for OS special access in context and explain what declining means. Android/OEM behavior must be represented as a platform constraint, not promised away.
- Record evidence honestly. Compilation and source tests do not prove timing, notification visibility, sound, touch feel, accessibility, or OEM behavior.
- Keep app-owned and OS-owned surfaces distinct. TicTic can theme its own dialogs; Android controls the system ringtone picker and permission/settings screens.
- Treat user data as private app-local data. Weather is the exception to offline core functionality and remains opt-in per widget.

## 2. Current scope and behavior

### Clock, alarm, and time selection

- Clock shows current local time, greeting/date, and user-selected digital or analog face. Time display supports system, 12-hour with AM/PM, and 24-hour formats.
- Alarm editor supports one-time/repeating schedules, explicit AM/PM, weekday/date selection, snooze, volume ramp, vibration, ringtone, and configurable pre-alarm reminder. New pre-alarm reminders use a distinct bundled short ping, default 30-minute lead; existing customization and migration rules are documented in `README.md` and `CHANGELOG.md`.
- The analog time dial only begins a time gesture when the initial touch is inside its circular face; touches outside the face should remain available for editor scrolling. This source behavior is not yet verified on device.
- Alarm changes are keyed by stable saved IDs and schedules are updated after save. Receivers re-check persisted enabled/state data to ignore stale queued alarm delivery.
- Upcoming/snoozed alarms remain visible and controllable. Main and early-alarm notifications have direct actions where the OS permits notifications.
- The active ringing screen and notification offer Snooze + Stop or Stop only, chosen in a dedicated Settings → Alarms “While an alarm is ringing” card. The card distinguishes visible actions from key shortcuts and states when each applies. Stop is always present and ends only this ring, preserving repeating schedules. The separate early pre-alarm reminder may still offer Turn off alarm. Volume Up and Volume Down have independent Snooze/Stop assignments. Existing shared actions migrate to both keys: Control volume becomes Snooze, legacy Turn off becomes Stop. TicTic cannot intercept/remap Android’s Power key; routing outside the active ringing UI varies by Android/OEM.

### Reminder, timer, stopwatch, routine

- Reminders support one-time and weekly schedules, sound/vibration/duration settings, snooze, dismissal, and local Usage events. Rows open the editor; the list has one Add action in both empty and populated states.
- Alarm and reminder sounds can be auditioned before saving; preview playback is bounded and is stopped on dialog/activity lifecycle events. Runtime audio-route testing is pending.
- Timer supports hours/minutes/seconds, a live custom-duration preview, optional hundredths, Start/Pause/Reset haptics, completion alerts and completion vibration. Running time is derived from monotonic elapsed time; wall-clock deadlines support OS scheduling and re-anchoring after reboot/time changes. Exact-alarm availability and notifications remain platform-controlled.
- Stopwatch uses monotonic elapsed time, optional hundredths display, pause/resume, laps, separately configurable button/lap haptics. A stored state needs device reboot/resume verification.
- Routines and Usage remain local. Usage/history collection is user-controlled and is not battery attribution. The Usage screen has a distinct segmented Today/7 days/30 days window; totals and recent history follow that selection. Users can save a CSV locally via the Android document picker; the report may include labels/details from retained history and is never shared automatically.

### Widgets and Widget Studio

- The picker retains 20 curated provider choices. There are 24 registered providers in source, supporting placed widgets and resize/reconfiguration metadata. User's separate requirement for 15 genuinely new, distinct widget concepts is still outstanding; it must not be presented as completed by the existing provider curation.
- Widget Studio uses the shared widget renderer/RemoteViews design, keeps its preview pinned while editing, and groups relevant controls. Text/surface contrast feedback is an estimate, not an accessibility certification.
- Per-widget wallpaper adaptation uses `WallpaperColors` metadata only, with a sample fallback. It does not read screenshots, request broad storage access, or open an image picker.
- Weather is off by default, supported only on selected widgets, uses a manually entered city, and has user-selected best-effort refresh cadence. No location permission is requested. Open-Meteo's free endpoint is non-commercial and attribution/licensing requirements are documented in `README.md` and `widget_specs.txt`.
- Android launchers/OEMs decide which resize/reconfigure hints they honor. Universal lock-screen widget support is not promised.

### Settings and visual system

- Settings is a full page with grouped left navigation for Design, Alarms, Reminders, Routines, Timer, Stopwatch, Efficiency & Data, and About & Permissions. Option rerenders preserve the current Settings scroll offset; changing groups opens at that group’s top.
- Flat/Soft/Raised card elevations have distinct fill/shadow/edge treatments and explanatory labels. Design includes coordinated Frosted/Luminous/Sculpted app glass with an obvious 15–100% opacity span, Matte-to-Glossy finish, Subtle-to-Floating depth and a live preview that redraws from slider values. `AppGlassDrawable` draws theme-aware pearlescent blooms, a feathered reflection, fine tinted rim and depth (no live blur); shared surfaces extend through main pages, Settings, Widget Studio/catalog chrome and the ringing screen. `GelMotion` adds a tactile press-squish/spring to clickable app surfaces, and `AliveScrollView` adds restrained scroll impulses and elastic top/bottom pulls. Critical ringing controls retain an 82% opacity floor. The interactive HTML study is illustrative; runtime/device appearance remains unverified.
- Reminder/routine names are optional; blank values save using the next free numbered label. Theme-aware controls, app typography/accent/card styles and dialog close/outside-dismiss behavior are app-owned. The actual widget preview stays faithful to widget settings; OS-owned pickers remain exceptions.
- NumberPicker selectors use theme-aware contexts rather than reflection into hidden `NumberPicker` members; light/dark contrast still requires visual QA on a real Android runtime.

## 3. Architecture map

All primary Java sources are under `app/src/main/java/app/tictic/clock/`; Android resources live under `app/src/main/res/`.

| Area | Key classes | Responsibility |
|---|---|---|
| Main application UI | `MainActivity`, `DataStore`, `UsageTracker`, `AppSurfaceStyle`, `AppGlassDrawable` | Clock and feature navigation, settings, local preferences/data, usage counters/history, editors and shared app UI/material renderer. |
| Alarm scheduling/delivery | `AlarmScheduler`, `AlarmReceiver`, `AlarmActionReceiver`, `PreAlarmReceiver`, `BootReceiver` | Persisted alarm schedules, stale-delivery checks, early reminder and notification actions, restoration after system events. |
| Alarm sound and ringing UI | `AlarmSoundService`, `AlarmActivity`, `TonePreview`, `AlarmTimeDialView` | Playback/ringing surface, key actions, bounded sound audition and analog time dial. |
| Reminders | `ReminderScheduler`, `ReminderReceiver`, `ReminderActionReceiver`, `ReminderSoundService` | Local reminder schedule, notification actions, bounded playback and recovery. |
| Timer and time math | `TimerReceiver`, `TimeMath` | Monotonic remaining-time math, OS timer delivery and recovery. `TimeMathTest` covers pure calculations only. |
| Widgets | `WidgetEngine`, `WidgetVariants`, `ClockWidgetProvider`, `DateWidgetProvider`, `NextAlarmWidgetProvider`, `WidgetConfigActivity`, `WidgetCatalogActivity` | Widget registration/rendering, Studio, resize and reconfiguration. |
| Weather | `WeatherJobService`, `WeatherRefresh` | Optional widget-only weather fetch/cache/update behavior; disabled when feature is off. |
| Efficiency | `PowerSaverReceiver` | Responds to Android Battery Saver state for configured UI/widget update efficiency. |

The manifest is `app/src/main/AndroidManifest.xml`. Main resource families are layouts, drawables, values/themes, widget provider metadata, animation and bundled short tone files. JUnit/Hamcrest are vendored under `app/libs/` for local test-only use and are not packaged in the app APK; license files are alongside the JARs.

## 4. Build and reproduce checks

Requirements: JDK 11 for this Gradle 7.6.4/Android Gradle Plugin setup; Android SDK Platform 34 and Build Tools 34.0.0. Do not assume this project wrapper works with a newer JDK merely because the SDK manager needs one.

```sh
bash gradlew --no-daemon --max-workers=2 \
  -Dorg.gradle.jvmargs='-Xmx768m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8' \
  clean testDebugUnitTest assembleDebug lintDebug
python3 -m unittest discover -s qa -v
```

- Gradle wrapper version: 7.6.4.
- SDK configuration is machine-specific. `local.properties` is ignored by Git and must not be published; set `sdk.dir` for the local machine.
- SDK Platform 34/Build Tools 34.0.0 were provisioned locally for the verified build; the machine-specific SDK path is in ignored `local.properties` and SDK binaries are not part of the project archive. No emulator image was provisioned for this build; an earlier API 34 attempt did not boot under the workspace host’s memory limits.
- JUnit: 7 pure `TimeMath` unit tests. Python suite: 54 source-contract checks. Both are useful regression guards but do not simulate Android activities/services/notifications or physical device timing.
- Latest clean follow-up lint result: 0 errors, 240 warnings. Do not treat warnings as resolved; inspect the HTML/text reports when making changes.
- APK metadata/signature/alignment and checksum are recorded in `ARTIFACTS-v1.11.1.md`.

## 5. Compatibility, permissions, and privacy

- `minSdk 23`, `targetSdk 34`, `compileSdk 34`.
- Manifest permissions include boot reception, internet (for optional weather), exact scheduling, notifications, vibration, wake lock, foreground media playback and full-screen intent. Permission declarations do not guarantee settings access or delivery.
- Android 12+ exact alarm access is user-controlled; if absent, schedules may use an inexact fallback and be late. Android 13+ notification permission/channel state can suppress notifications. Android 14+ full-screen alarm intent access is user-controlled and restricted by Android policy.
- Do Not Disturb, alarm/notification channel volume, battery rules, lock-screen privacy, OEM policy, launcher behavior, and current foreground/background state can affect delivery and presentation. Test granted and denied paths.
- No runtime location permission. Manual city text is used only if a supported widget's weather is explicitly enabled. Weather is a network feature; all other core clock functions remain offline.
- Do not add broad storage/screenshot capture or imply wallpaper capture. Only `WallpaperColors` metadata is allowed by current product requirements.
- Do not put credentials, machine-specific SDK paths, local user data, keystores or signing secrets in the repository. `local.properties` is ignored. Use a protected release keystore outside source control.

## 6. Verification status and open work

### Verified for v1.11.1 source/build

- Clean Gradle unit-test/debug-assemble/lint invocation completed successfully.
- JUnit `TimeMathTest` 7/7 passed; Python source-contract checks 54/54 passed.
- Lint 0 errors / 240 warnings (warnings remain untriaged).
- Screenshot-driven alarm-controls separation, gel renderer, press feedback and elastic scroll compile and pass static source-contract checks; live/device appearance and gesture feel remain unverified.
- APK package/version/SDK metadata, zip alignment, v1/v2 debug signature and SHA-256 verified.

### Still unverified

- No Android app screen was exercised in a booted emulator or on a phone. An API 34 emulator attempt exited before boot because it requested 2.5 GB RAM while about 1 GB was available; hardware acceleration was unavailable. All rows in `qa/QA-v1.11.1-manual.md` are NOT RUN.
- Alarm/reminder timing and audio, screen-off/keyguard/full-screen paths, volume-key routing, timer/stopwatch lifecycle and reboot/time-change recovery, dial scrolling boundary, visible contrast and launcher/OEM widget behavior.
- No same-device Google Clock comparison. Documentation alignment is not parity proof.
- No production-signed APK, store publication, or GitHub push.

### Product backlog carried forward

- Complete 15 new, genuinely distinct widget concepts while retaining concise names and 20 picker choices.
- Run the focused v1.11.1 matrix plus prior regression cases on representative Android/API/OEM combinations, including the user's target device. Report actual passed/failed/flaky/blocked cases, not an invented accuracy rate.
- Re-check alarm create/edit, audio, timers and stopwatch after device QA. Only call the release final after evidence is recorded and remaining warnings/issues are triaged.

## 7. Repository contribution/publishing checklist

1. Clone the intended remote branch and inspect existing files/commits before importing. The current remote `main` already contains one user-created file named `1`; preserve it unless the owner intentionally removes it.
2. Copy project source/docs/artifacts without `local.properties`, `.gradle/`, generated `build/` outputs, machine caches or private credentials. Review `git status` before staging.
3. Run the build and tests on the target environment; update `CHANGELOG.md`, versioned `TESTING-vX.md`, `ARTIFACTS-vX.md`, checksum and README status.
4. Check `git diff --check`, inspect the diff, confirm binary artifacts are intentional, and ensure no credentials/user data were staged.
5. Commit with a meaningful message, push only after the owner has authenticated in their own Git environment, and verify the commit appears on the expected branch.
6. Keep runtime QA and release signing separate from source-build success. A debug APK is for testing, not public store release.

`PUBLISH-TO-GITHUB.md` provides the concrete import steps for the current public repository. This session cannot authenticate or push; the repository owner must run the authenticated push locally.

## 8. Reusable practice for future projects

Use the accompanying project engineering log template as a starting point. For each future repository, record: user requirements and explicit non-goals; architecture and data flow; permission/privacy matrix; environment/version pins; exact reproducible commands; automated test scope and limits; device/browser/runtime matrix; measured artifact size/hash/signing; known limitations; unresolved requirements; repository/authorship status; and the exact next action. Separate **implemented**, **build-verified**, **runtime-verified**, and **released** so documentation never overstates the evidence.

## v1.11.1 alarm editor follow-up

The alarm create/edit surface is now an opaque full-screen editor on compact screens with a pinned heading and action footer around a clipped scroll form. Save is emphasized during edits. Source-contract tests cover the full-screen/opaque window and scroll clipping; keyboard/inset behavior and all UI workflows still require manual Android QA.
