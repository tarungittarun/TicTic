# Changelog

## v1.11.2 — holistic responsive surfaces, five glass presets and selectable touch feedback

- Expanded the earlier alarm-form fix across related surfaces: compact Settings uses a bounded scroll area below a horizontally browsable category row; alarm create/edit and **Configure alarm defaults** use full-screen forms on compact or short-height screens with fixed heading/actions and clipped, scrollable content; wider, taller screens retain centered, bounded dialogs.
- Reworked Widget Studio responsively: compact devices use full-screen Studio while larger screens use a centered popup. The live RemoteViews preview/header and Cancel/Apply footer remain pinned around a bounded scrolling control form.
- Added five ready-to-use glass treatments—**Sunlit Bubble, Daylight Pearl, Golden Hour, Moonlit Opal and Aurora Frost**—with preset selection and editable opacity, sheen, depth and panel styling in app Design and Widget Studio. A live sample, stronger opacity floor, theme-aware highlights and solid critical actions keep glass behind content and prioritize text contrast.
- Added five selectable touch-feedback styles for buttons/cards: **Water ripple, Gel squeeze, Expressive spring, Glass wave and Magnetic lift**. Includes intensity, live preview, Reduce motion, and Android system-animation checks.
- Bumped metadata to `1.11.2` / versionCode `23`.
- Verification: clean `testDebugUnitTest assembleDebug` succeeded; JUnit `TimeMathTest` **7/7** and Python source-contract suite **57/57** passed; lint **0 errors / 242 warnings**. APK alignment and debug-signature verification passed. **Android runtime/device QA remains NOT RUN** (no device attached); see `TESTING-v1.11.2.md` and `qa/QA-v1.11.2-manual.md`. This is a debug sideload candidate, not a store-signed release.

## v1.11.1 — full-screen alarm editor and scroll containment

- Reworked alarm create/edit into an **opaque, full-screen editor** on phones: a fixed heading and action footer frame a scrollable form, the page behind it no longer shows through, outside taps do not discard edits, and the keyboard can resize the form without covering the actions. On edits, **Save alarm** receives the clearest visual weight; Delete and Cancel remain available.
- Kept the task order focused: set time, then choose repeat days or a one-time date; expand **More settings** for the label, alarm behavior, sound, and pre-alarm reminder configuration. The full scroll area is clipped to its viewport so content cannot paint over pinned controls.
- Research basis: Google Clock’s documented alarm flow includes time, repeat, label and sound controls ([Google Clock Help](https://support.google.com/android/answer/2840926?hl=en)); Material 3 allows full-screen dialogs for multi-step compact-screen tasks and advises keeping titles/actions visible while content scrolls ([Dialogs guidelines](https://m3.material.io/components/dialogs/guidelines)). This is a TicTic-native implementation, not a copy of another app.
- Regression coverage now checks opaque full-screen editor setup and scroll clipping. Version metadata is `1.11.1` / versionCode `22`.
- Verification: `clean testDebugUnitTest assembleDebug lintDebug` succeeded; JUnit `TimeMathTest` 7/7 and Python source-contract tests 54/54 passed; lint reported 0 errors and 240 warnings. APK metadata, alignment and v1/v2 signatures verified. **Android runtime/device QA remains NOT RUN**, including keyboard/inset and actual create/edit interaction; this debug APK is for sideload testing, not a store release. See `TESTING-v1.11.1.md` and `ARTIFACTS-v1.11.1.md`.

## v1.11.0 — safer ringing controls, exportable Usage and unified liquid-glass styling

- Removed **Turn off alarm** from the active ringing screen and notification. Screenshot-driven follow-up reorganizes Settings → Alarms into a dedicated **While an alarm is ringing** card: choose **Snooze + Stop** or **Stop only** for the full-screen alarm and notification, then configure Volume Up and Volume Down independently. Explanations distinguish these visible buttons from key shortcuts and state when each applies. Stop always ends only this ring, leaving repeating schedules enabled; the separate early pre-alarm reminder may still offer Turn off alarm.
- Split Volume Up and Volume Down into independent **Snooze** / **Stop** settings. Existing shared actions migrate to both keys: legacy Control volume maps to Snooze; legacy Turn off alarm maps to Stop.
- Added an optional **Save usage report · CSV** action using Android’s document picker. The report includes the selected period’s local totals and retained activity rows; labels/details may be included. Nothing is exported or uploaded until the user chooses a destination.
- Replaced the same-looking Usage period buttons with a grouped Today / 7 days / 30 days selector and date caption. Summary and recent history now follow the selected window.
- After further visual feedback, softened the glass into a fresher pearlescent gel: diffused light blooms, a feathered reflection and a fine tinted edge replace the heavy, hard-edged look. Opacity still spans 15–100%, with live Matte-to-Glossy Finish and Depth tuning across shared surfaces and both themes; critical ringing actions retain an 82% opacity floor.
- Added direct-touch feedback: clickable app surfaces gently widen and compress like gel while pressed, then spring back. Main/settings, Widget Studio and catalog scrolling now includes a restrained scroll impulse and elastic pull at the top/bottom edges. Added an interactive HTML study, `Gel-Interaction-Preview.html`, to preview the visual direction and try the press/scroll feel; it is illustrative, not an Android runtime capture.
- Bumped version metadata to `1.11.0` / versionCode `21`.
- Verification after this follow-up: clean `testDebugUnitTest assembleDebug lintDebug` passed; 7/7 JUnit time-math tests and 53/53 Python source-contract checks passed; lint reported 0 errors and 240 warnings. APK metadata, zip alignment and v1/v2 debug signature checks passed (apksigner also prints its non-fatal warning about the Gradle `META-INF/com/android/build/gradle/app-metadata.properties` entry not being covered by the JAR signature). Runtime/device QA remains NOT RUN; see `TESTING-v1.11.0.md` and `ARTIFACTS-v1.11.0.md`.

## v1.10.0 — clearer key actions, stable settings, glass surfaces and optional names

- Replaced the four Volume Up/Down choices with just **Snooze** and **Dismiss**. Both hardware volume keys perform the selected action while the ringing screen is active; Snooze uses the alarm’s configured interval, while Dismiss stops the current ring and leaves repeating schedules enabled. Existing `Control volume` values migrate safely to Snooze; existing `Turn off alarm` values migrate to Dismiss so an old key preference cannot silently disable future alarms. Separate notification/on-screen Turn off controls are retained.
- Added timer settings for optional hundredths display and Start/Pause/Reset haptics; added stopwatch Start/Pause/Reset haptics alongside existing hundredths and lap-feedback choices. Timer completion vibration and alerts remain independently configurable.
- Changed Design preference rerenders to restore the Settings scroll position. Switching navigation groups still starts at the top of the selected group.
- Made Flat/Soft/Raised card elevation visually clearer using differentiated surface tones, stronger shadow levels and a live text explanation.
- Added a configurable app-wide glass-inspired material: opacity, Matte-to-Glossy finish and depth sliders with a live sample. It uses translucent gradients and highlights rather than promising live blur; light/dark themes remain available.
- Routine and reminder names are optional. Blank names save as the next available `Routine 01`, `Routine 02`… or `Reminder 01`, `Reminder 02`… value, avoiding sequence-name collisions.
- Bumped version metadata to `1.10.0` / versionCode `20`.
- Verification: clean `testDebugUnitTest assembleDebug lintDebug` build passed; 7/7 JUnit time-math tests and 48/48 Python source-contract checks passed; lint reported 0 errors and 241 warnings. APK metadata/alignment and v1/v2 debug signature checks passed. Runtime/device QA remains NOT RUN; do not label this debug APK release-ready until alarm-key migration/actions, settings, optional names, glass/elevation contrast and timer/stopwatch workflows are exercised on devices. See `TESTING-v1.10.0.md` and `ARTIFACTS-v1.10.0.md`.

## v1.9.9 — reliable timing, audible previews and clearer alarm controls

- Reworked the Volume Up/Down preference to a readable 2×2 choice layout: **Control volume**, **Snooze**, **Dismiss ring**, and **Turn off alarm**. The first three align with Google Clock’s documented choices; Turn off remains TicTic’s extra option. Both keys share the same selected action. The UI explains that Power-key remapping is reserved by Android and directs screen-off users to lock-screen notification actions.
- Added bounded five-second audition controls for the chosen alarm tone and reminder tone in their editors/default settings. A new preview stops when its dialog closes, the activity pauses, or the short preview timeout expires.
- Stopwatch now measures with `SystemClock.elapsedRealtime()` plus boot-aware fallback, displays hundredths with a 16 ms foreground refresh while running, and records pause/lap/reset from the same monotonic value. Existing v1.9.8 wall-clock stopwatch sessions migrate on read.
- Timer now uses a monotonic deadline for the running display, persists a wall deadline for Android scheduling, re-anchors after reboot/time changes and reschedules early RTC deliveries. Pause/resume/reset preserve the remaining duration. Android 12+ users are told before starting if exact-alarm access is unavailable.
- Clock-dial touch selection begins only within its visible circular face; outside touches are left to editor scrolling. Timer and other NumberPicker controls receive matching light/dark text, input, divider, accent and background styling.
- Added six real JUnit time-math tests and seven feedback-specific source regression checks; updated release metadata to versionName `1.9.9`, versionCode `19`.
- Verification: clean `testDebugUnitTest assembleDebug lintDebug` build succeeded; JUnit **6/6** and Python source-contract checks **43/43** passed. Lint reports **0 errors and 239 warnings**. APK metadata, zip alignment, and v1/v2 debug signatures were verified; checksum and size are in `ARTIFACTS-v1.9.9.md`.
- Reference: [Google Clock Help](https://support.google.com/clock/answer/2841106?hl=en) documents volume, snooze and dismiss as its volume-button choices. This is a documentation-level comparison, not hands-on parity evidence. Power-key interception and volume-key behavior while the app’s ringing surface is not active remain OS/OEM limitations.
- Runtime/device QA remains **NOT RUN**. An API 34 emulator attempt could not boot because host RAM was insufficient and hardware acceleration unavailable; no physical device was attached. The APK is a debug candidate, not release-ready. The public repository `https://github.com/tarungittarun/TicTic.git` remains empty; no commit or push was made because no authorized GitHub write mechanism is available.

## v1.9.8 — consistent popups, richer clock faces, history and controls

- Standardized app-owned dialogs with a top-right × close action, outside-tap dismissal, a rounded theme-aware surface, and consistent title treatment. The custom time dial and Widget Studio now also have a top-right close affordance; reminder/timer/routine/date dialogs use the shared treatment.
- Expanded Usage history to show the latest 20 by default with a **Show 80 more** action (up to 100 retained events).
- Added five digital clock styles and four analog dial styles to the home-clock settings.
- Added App Card border (none/soft/accent) and elevation (flat/soft/raised) options alongside existing corners and spacing.
- Both Volume Up and Volume Down now share a configurable alarm action: volume, snooze, stop, or turn off. Android reserves the Power key; the app explicitly does not intercept or promise Power-key actions.
- Updated Widget Studio with a top-right close control and outside dismissal; device-owned ringtone picker surfaces remain controlled by Android.
- Added structured implementation/decision/test notes for repository handoff. Repository URL has since been supplied, but the public GitHub repo is empty and no authorized push credentials are available in this session; no commits or pushes were made.
- Build/test status and device-QA limits are in `TESTING-v1.9.8.md`.

## v1.9.7 — consistent time editing, expanded reminder controls, master settings

- Alarm editor AM/PM choices are explicit, independently tappable controls; alarm, routine and reminder time selection use the same TicTic clock-dial language.
- Reminder Add/Edit “More settings” and Settings now expose multiple bundled/system/custom/silent sounds, vibration and configurable ring length (1–30 seconds). Reminder tones play through a bounded foreground media service and are stopped on reminder cancel/dismiss/snooze.
- Settings navigation rail is narrower and now includes master Routines, Timer and Stopwatch groups. Added routine start/duration/repeat/notification defaults; timer duration/alert/vibration preferences; stopwatch hundredths and lap haptics.
- Design settings now include 16 accent colors, app typography choices and card corner choices. Widget Studio spacing now changes both outer inset and vertical gaps in the rendered widget, and its pinned preview uses matching preview dimensions.
- All 24 registered widget providers advertise horizontal/vertical resize modes and Android 12+ 150×84 dp minimum-resize hints; the renderer scales typography and hides secondary content in compact sizes. Actual launcher/OEM resizing still varies.
- Source-contract checks and Android debug build pass. Device/emulator QA is not available in this workspace; see `TESTING-v1.9.7.md` for exact limits and outstanding manual checks.

## v1.9.6 — reminders, full-page settings and alarm editor visibility

- Put Reminders directly after Clock; removed the duplicate empty-state Add button so exactly one Add reminder action is shown in either list state. Reminder cards now open editing on tap, remove the separate Edit control, and use Dismiss. Dismissing one-time reminders completes them; dismissing a weekly reminder skips its next occurrence while preserving later repeats.
- Replaced the Settings popup with a full-page Settings view and left navigation for Design, Alarms, Reminders, Efficiency & Data, and About & Access. Reminder settings now expose default vibration and a configurable 5/10/15/30-minute snooze, with notification and exact-scheduling access controls.
- Fixed the invisible alarm create/edit dialog: the custom alarm editor shell was built but never attached to its Dialog window. It is now installed as the dialog content with a full-size root so the weighted form/scroll area is laid out.
- Reminder dismissal, create/edit/delete/show/snooze, reminder preferences, alarm defaults and default alarm tone changes are recorded in local Usage history where appropriate.
- Verification: Android SDK 34 `assembleDebug` passed and Python source-contract checks passed 25/25; Gradle unit tests are NO-SOURCE. Lint analysis was blocked by a Maven Central TLS dependency-resolution failure. No emulator/phone was attached; all 58 functional QA scenarios remain NOT RUN on device. See `TESTING-v1.9.6.md`.

## v1.9.5 — reminders, defaults and circular alarm-time dial

- Added a dedicated Reminders destination with local one-time and weekly schedules, notes, repeat-day selection, vibration choice, Done and 10-minute Snooze notification actions, and boot/time-change rescheduling. Android notification permission is requested contextually; exact-alarm access may still affect delivery precision.
- Added configurable new-alarm defaults in Settings → Alarms for snooze, vibration, gradual volume, alarm tone, pre-alarm enable/lead/tone/vibration. New alarms inherit them; More settings keeps per-alarm overrides.
- Rebuilt alarm creation around a draggable analogue time dial, clear time/AM-PM selection, working Repeat switch, weekdays/date selection, with remaining options collapsed behind **More settings**.
- Improved app and Widget Studio text/control contrast; Studio now reports an estimated text-to-surface contrast ratio and uses theme-aware control dialogs.
- Verification: Android SDK 34 debug compile/package completed; 20 Python source-contract checks pass. No emulator/handset is attached; device notifications, exact timing, touch feel and accessibility remain unverified. Lint not run.

## v1.9.4 — disable action on every alarm notification

- Added **Turn off** to the main ringing notification for both one-time and repeating alarms (not only to the pre-alarm reminder). It uses the existing disable path to stop ringing and cancel the alarm, reminder and snooze schedules.
- Added a regression case for the ringing notification action and rebuilt the debug APK. Device-level notification verification remains pending.

## v1.9.3 — alarm safety and editor corrections

- Added a direct **Turn off alarm** action to the pre-alarm reminder notification; it disables the associated alarm and cancels its future alarm, reminder and snooze schedules.
- Made alarm delivery re-check persisted state before ringing. A queued main-alarm broadcast for a now-disabled/deleted alarm is ignored; a stale snooze is ignored after its saved snooze deadline/state is cleared.
- Fixed editing an existing alarm: save now replaces the persisted alarm with the same stable ID instead of appending only on new alarms, then schedules the edited object. A missing/deleted ID is not silently resurrected.
- Made a visible Set/Edit custom minutes control available immediately when Custom is selected and when reopening an alarm that already uses a custom lead; the chosen value is saved with the alarm.
- Added four high-priority functional regression cases to the clock/alarm/timer/stopwatch matrix. SDK compilation and structural checks pass; Android runtime/device verification remains outstanding.

## v1.9.2 — distinct 30-minute pre-alarm ping and key-action handling

- Changed every new alarm’s pre-alert default to **30 minutes** and a bundled, one-shot TicTic notification ping. The early alert is labeled “Reminder”, does not start the alarm sound service, and is not the main alarm ringtone. Users can still choose a device notification tone, alarm tone, silence, or a custom tone.
- Migrated legacy 10-minute / “same as alarm” reminder settings to 30 minutes / TicTic ping and rescheduled them after package update. Because v1.9.1 did not record whether 10 minutes was chosen by the user or was the old default, any stored 10-minute value migrates to 30; other lead-time choices are retained.
- Routed active alarm volume-key handling through early key dispatch so Snooze/Stop modes consume the key rather than falling through to volume adjustment when the ringing Activity has focus; Control volume continues to use the alarm stream. Removed `FLAG_DISMISS_KEYGUARD` to avoid dismissing the keyguard as part of the alarm surface, while retaining show-when-locked/turn-screen-on. Added pressed-state feedback to the labeled controls.
- Added a risk-based clock/alarm/timer/stopwatch test specification and Google Clock feature comparison. SDK build/test tasks and static checks are run; no Android runtime is available in this environment, so device interactions remain unverified.

## v1.9.1 — alarm reliability and controls

- Moved contextual Alarms & reminders, notifications, and (Android 14+) full-screen access explanations ahead of saving; each is identified as optional with the consequence of declining. Save-with-delay remains available.
- Added a lead-time warning when a configured pre-alarm reminder would already be in the past. Alarm cards report when no ping can be scheduled this occurrence or when Android must schedule it inexactly. Reminder delivery history distinguishes notification access/channel suppression from a reminder that was posted.
- Rebuilt the ringing surface with high-contrast, explicit, large Snooze and Stop controls; the ringing notification now has direct Snooze and Stop actions and a Turn off action for repeating alarms.
- Added persistent snooze visibility and a cancel-snooze action to the alarm list; upcoming occurrences show their next date/time and remain editable/toggleable.
- Added a detailed Permissions & access section, per-user volume-key Control volume/Snooze/Stop behavior, and explicit Power-key limitations. Power is Android-owned and is not intercepted.
- QA: Android SDK 34 compile/package and the test task are recorded in `TESTING.md`; no emulator or phone is available here, so the physical ringing UI, notification actions, permission prompts, actual sound delivery and hardware keys still require on-device regression before calling these fixes runtime-verified.

## v1.9.0 — curated widgets, faithful Studio and opt-in weather

- Recurated the in-app picker to **20 short-named provider entries**: five core clocks plus fifteen selections from the already-registered providers. This picker curation does **not** fulfill the separate requirement for 15 genuinely new, distinct widget concepts; those remain outstanding. Existing provider components remain available so already placed widgets are not forcibly removed.
- Refactored Widget Studio to host the exact widget RemoteViews layout and shared styling renderer in its pinned preview. Content controls now appear only when relevant; panel color and opacity are grouped under a clear Panel Surface card.
- Added per-widget wallpaper-color matching using only Android `WallpaperColors` metadata. It is opt-in; no wallpaper image access, picker or permission prompt is used, and missing/unavailable metadata falls back to a sample palette.
- Added optional, per-widget weather for supported clock layouts. User-entered city, 30/60/120-minute request cadence, no location access. `JobScheduler` network constraints and Android's inexact timing can defer updates. No call is made while weather is disabled.
- Added INTERNET permission solely for this opt-in weather path. Core clock, alarms, timer, stopwatch, routines, usage and widget styling remain local/offline. Open-Meteo attribution and CC BY 4.0 are shown; the free endpoint is non-commercial only and commercial deployment requires a suitable paid plan.
- Reduced icon stroke thickness by approximately 40% (1.6→0.96; Usage 1.85→1.11).
- Build: debug APK compiled with Android SDK 34; device/emulator QA unavailable. `lintDebug` not run in this environment; see `ARTIFACTS.md` and `TESTING.md`.

## v1.8.0 — timer refinements, Usage and exciting widgets

- Reworked the Timer card to avoid fixed-height text clipping; the remaining time fits hours, minutes and seconds. Replaced the minutes-only editor with an updating Hours/Minutes/Seconds countdown picker and added a one-hour quick preset.
- Renamed the bottom destination from Activity/Log to **Usage** and replaced the list-style glyph with a matching custom chart icon; history remains available within Usage.
- Added four resize-aware widget styles under a new **Exciting widgets** catalog section: Prism 3D, Flip Deck, Smart Console and Halo Orbit. Their dates, alarms and text styling reuse per-widget Studio options; Smart Console has separate one-tap Timer and Alarm buttons.
- Inspired by the attached chunky digital desk-clock look, using original raised-edge, split-folio and luminous designs. No weather/temperature is fabricated; no network or third-party dependency was added. The debug APK remains under 700 KiB (674 KiB), up 96,136 bytes (16.2%) over v1.7.0 for the four widget designs and their provider metadata.

## v1.7.0 — wallpaper colors, popup Studio and smoother transitions

- Added named wallpaper-color swatches and the visual Hue/Saturation/Brightness picker; selected colors create a coordinated gradient in the Studio wallpaper preview and persist locally.
- Changed Widget Studio to a centered floating window covering about 90% of the display, with a dimmed backdrop, rounded corners and short fade/scale entrance/exit animations. The live scene stays pinned while controls scroll.
- Added lightweight fades for wallpaper changes and preview feedback. No runtime dependencies, image assets or network services were added for these changes.
- Kept the debug APK close to the previous release size; see `ARTIFACTS.md` for the measured result. Device-level popup/animation behavior remains awaiting handset QA.

## v1.6.0 — wallpaper-aware preview and simpler widget controls

- Widget Studio shows the device wallpaper only where public Android access permits; otherwise it keeps the sample wallpaper choices and does not request storage or broad file access.
- Replaced HEX entry with named color palettes, recent colors and a visual Hue/Saturation/Brightness picker.
- Renamed Studio controls with direct user-facing terms; changed spacing levels to Tight, Standard and Relaxed and synchronized all four preview/widget insets.
- Put widget content above the panel fill; synchronized surface, no-panel/outline, panel color and opacity behavior.
- Added a Matte-to-Glossy TicTic glass finish slider with a static highlight layer.
- Reduced unnecessary explanatory copy across app screens and moved compatibility/privacy notes into About.

## v1.5.0 — pinned Studio preview and numeric color editing

- Kept the live home-screen/wallpaper scene, wallpaper selector and preview tip pinned while the Widget Studio editing controls scroll independently.
- Made TicTic Frost Pane · shaded glass and Outline-only glass explicit surface choices.
- Added editable text/panel HEX values (`#RRGGBB` and `#AARRGGBB`) beside swatches, synchronized with live preview; numeric panel-opacity percentage stays visible as it changes.
- Updated QA steps for pinned-preview scrolling and direct numeric color entry.

## v1.4.0 — signature widgets, preview, efficiency controls and local history

- Removed battery snapshots and attribution language from the Activity UI; one-time migration deletes legacy battery snapshot preferences.
- Improved Activity/Log icon weight/contrast and added chronological history, including widget opens, added/updated styles and removals.
- Added dark/light Widget Studio theming, explicit dark/light spinner/dropdown rows, swatch outlines, and a live unsaved home-screen scene with swappable wallpaper samples.
- Added placed-widget reconfiguration metadata for supported launchers; Widget Studio restores that widget's saved settings. Launcher support and long-press menus vary.
- Added widget layout priorities, compact/balanced/roomy spacing, signature/soft/crisp/capsule/frosted/border-only panels, and expanded color palettes.
- Added app card corner/spacing settings and eight app accent colors.
- Added user-controlled switches for seconds, adaptive Battery Saver behavior, usage metrics and activity history. Under system Battery Saver, configurable optimizations slow redraw cadence, suppress second ticks and skip entrance animation. The same preference reduces second resolution in placed widgets.
- Deleted widget-instance preferences when widgets are removed. Kept resources and dependencies local; no analytics, battery API or network library added.
- Added product record and updated manual QA/release notes.

## v1.3.0 — refined surfaces, widget studio and usage summary

- Refined bottom navigation and feature glyphs; the TicTic header mark/wordmark returns to Clock.
- Added a Settings modal occupying approximately 85% of the screen and a modern custom alarm editor.
- Differentiated 20 widget designs with per-design layouts, fonts, spacing/backgrounds and defaults.
- Expanded per-instance Widget Studio controls for date/alarm/caption/context visibility, time-only alternatives, date/alarm detail formats, 8 typefaces, 12 text colors, 10 panels, size, alignment, transparency and opacity.
- Added local Today/7-day/30-day usage summary with foreground and Clock-screen time estimates, event/widget refresh counters and device-wide battery snapshots. The dashboard explicitly disclaimed battery attribution.

## v1.2.0 — time clarity and alarm reminders

- Added the refined clock-face brand, instant home time, AM/PM default, System and 24-hour options, day-part greeting, and ordinal uppercase date.
- Added canonical EST/EDT, IST, GMT and other world-clock zones, with custom IANA zone IDs.
- Added per-alarm pre-alerts with 5/10/15/30/60-minute presets or custom 1–180-minute lead, sound selection and vibration control.
- Preserved the earlier redesign: 20 resize-aware widgets, per-instance Glass controls, alarm-centric Clock home, analog/digital face, local bundled alarm tracks and lock-screen-aware alarm UI.
