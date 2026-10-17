# TicTic engineering log — v1.9.8

**Date:** 2026-10-15  
**Status:** Source changes implemented and debug APK built. Repository URL has been supplied; the repo is public but empty. The GitHub connector is unsupported in this session and no authorized write credential is available, so no commit or push was made.

## 1. Purpose / user requests addressed

- Put a top-right close (×) on app-owned dialogs and dismiss them on outside taps.
- Keep popup corners and surfaces consistent across alarm, time, date, reminder, routine, timer, color-picker and Widget Studio windows.
- Show 20 recent Usage-history records by default and allow revealing up to 80 more, capped at 100.
- Give one shared Volume Up/Down alarm action selector, with more available actions; document Android's Power-key restriction.
- Add home Clock digital and analog styles.
- Add card-border and card-elevation customization alongside existing card corners/spacing.
- Keep the release, implementation, test outcomes, failures, constraints and later repository handoff structured.

## 2. Stack / build context

- Native Android app in Java; Java source/target compatibility 8.
- Android Gradle Plugin 7.4.2; Gradle wrapper 7.6.4.
- Compile/target SDK 34; min SDK 23; application ID `app.tictic.clock`.
- Android framework UI (`Activity`, `Dialog`, `AlertDialog`, `RemoteViews`), local `SharedPreferences`/JSON, `AlarmManager`, notifications and services. No external app runtime libraries are declared in `app/build.gradle`.
- Core clock/alarm/timer/routine functions remain local/offline. Optional weather is a separately user-enabled widget feature.

## 3. Implementation map and decisions

### Common popups — `app/src/main/java/app/tictic/clock/MainActivity.java`

- Added `TicAlertBuilder`, used by `alertBuilder()`. Its custom title bar includes a right-side × close button; created dialogs use the current app card shape/surface and enable outside-touch dismissal.
- Existing alarm editor already had a close ×; added an accessibility description and enabled outside dismissal.
- Shared clock-dial popup gained its own top-right × and outside dismissal.
- Replaced native `DatePickerDialog` uses in alarm and reminder editing with themed `DatePicker` controls inside the common dialog, so those flows can use TicTic title, close, corner and dismiss behavior.
- Ringtone selection invokes Android's ringtone picker. It is another app's OS/OEM-owned surface; TicTic cannot inject its own title bar or cross into it.

### Widget Studio — `WidgetConfigActivity.java` and `app/src/main/res/values/styles.xml`

- Added a top-right × to the popup header; outside taps finish/cancel the configuration activity.
- Changed both light and dark popup themes' `windowCloseOnTouchOutside` to `true`.
- Added a Widget Studio alert-builder counterpart with the same close title, rounded panel and outside dismissal for its color dialog.

### Recent history — `MainActivity.java`, `DataStore.java`

- Added a session display limit of 20 and a “Show 80 more” control that reveals up to 100 events. The screen resets to 20 on a fresh Usage-tab visit and after clearing.
- Existing `DataStore.log()` already retains at most 100 local events, so no storage migration/schema change was needed.

### Clock face options — `MainActivity.java`, `AnalogClockView.java`

- Preserved existing digital/analog mode preference (`clock_face`) for compatibility.
- Added independent style preferences: `clock_digital_style` (Soft, Serif, Mono, Rounded, Condensed) and `clock_analog_style` (Classic markers, Minimal, Numerals, Accent ring).
- Analog style stays in the native custom Canvas view and continues to honor the existing seconds/Battery Saver setting.

### Alarm hardware controls — `MainActivity.java`, `AlarmActivity.java`

- The Settings choice now applies one action to both Volume Up and Volume Down while the alarm activity is foreground: Volume, Snooze, Stop, or Turn off.
- Added the Turn off option, which invokes the existing alarm-disable path and clears associated schedule/snooze state.
- Deliberately did not intercept Power. Android reserves that key and third-party interception is not dependable or promised. The app explains this limitation in Settings.

### Appearance — `MainActivity.java`

- Added App Card border choices (None, Soft, Accent) and elevation choices (Flat, Soft, Raised); existing corner and density choices remain.
- Card builder reads the new local preference keys, applying them consistently to app card surfaces.

### Version / tests / docs

- `app/build.gradle`: bumped to versionName 1.9.8, versionCode 18.
- `qa/test_source_contracts.py`: grew to 36 source-contract checks, including popup, history, clock style, card and hardware-action wiring.
- `CHANGELOG.md`, `README.md`, [`TESTING-v1.9.8.md`](TESTING-v1.9.8.md), [`qa/QA-v1.9.8-manual.md`](qa/QA-v1.9.8-manual.md), [`ARTIFACTS-v1.9.8.md`](ARTIFACTS-v1.9.8.md), and this log record scope and limitations. A reusable multi-project template is available in [`PROJECT-ENGINEERING-LOG-TEMPLATE.md`](PROJECT-ENGINEERING-LOG-TEMPLATE.md).

## 4. Verification performed

- `python3 -m unittest discover -s qa -v`: **36/36 PASS**. These are static source-contract checks, not view interaction tests.
- Clean Android command (see `TESTING-v1.9.8.md`): `testDebugUnitTest` and `assembleDebug` completed successfully. `testDebugUnitTest` is **NO-SOURCE**.
- APK package metadata, zip alignment and v1/v2 signing checks are recorded in `ARTIFACTS-v1.9.8.md`.
- Lint was not run. No Android emulator or device is connected; all touch/layout/audio/hardware/launcher manual QA remains NOT RUN.

## 5. Mistakes / environment recovery

- The first v1.9.8 build attempt failed because the workspace's temporary SDK path (`/tmp/android-sdk`) had been cleared. Android command-line tools, SDK Platform 34 and Build Tools 34.0.0 were restored; the clean build then passed.
- In the preceding release workspace, Gradle's default 2 GB daemon disappeared during a clean build. This constrained environment succeeded with a 768 MB heap and two workers. The exact successful command is in the test report.
- No app-code rollback was necessary for either environment failure. SDK setup is environment-only and is not included in the source archive.

## 6. Repository inspection / push status

- Supplied remote: `https://github.com/tarungittarun/TicTic.git`.
- Anonymous GitHub API inspection reports `private=false`, `default_branch=main`, `size=0`; branch listing is empty and the commits endpoint reports the empty-repository conflict. `git ls-remote` returned no refs. This is an empty repository, not an existing TicTic source tree to merge into.
- GitHub connector lookup returned **unsupported** in this Arena session. Anonymous read access works, but anonymous GitHub pushes are not authorized. No credential/helper is present in the workspace.
- No initial commit was created; no branch was created; no push was attempted. Do not place a personal access token or password in chat. A write-authorized checkout/credential mechanism is needed before any push. Because the remote is empty, the next repository operation must be an explicitly approved initial seed to `main` or to a new working branch, not an incremental update.

## 7. Risks / constraints not to overstate

- A source check or successful build does not prove that dialog corners, outside touches or touch targets render as intended across API/OEM themes. Run the manual checklist.
- Android's ringtone picker is system-owned; it retains its own controls/style.
- The Power key cannot be reliably remapped by this app. Only shared Volume Up/Down action behavior is implemented.
- History pagination only displays up to the 100 records the app already retains; it is not an unlimited archive/export.
- The previous request for 15 genuinely new distinct widget concepts remains outstanding; the existing curated set is retained.
- All v1.9.8 focused manual cases and prior functional workbook scenarios remain NOT RUN. The app is debug-signed, not store-signed.

## 8. Future repository commit ledger format

When a repository is supplied, use one focused commit per coherent change. Do not fabricate commit hashes or claim a push before it occurs. Record each actual commit here using:

```text
### Commit: <actual hash> — <subject>
- Date / branch:
- User-facing goal:
- Files changed:
- Approach and key decisions:
- Regression risks / compatibility:
- Tests run and exact outcomes:
- Failures encountered and resolution:
- Follow-up / known gaps:
- Push / PR URL (or “not pushed”):
```

Suggested logical commit boundaries for this release (proposed only; **not actual commits**): shared popup chrome; Usage history pagination; home clock styles; shared volume-key options; card surface settings; QA/release documentation.
