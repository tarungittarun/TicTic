# TicTic redesign notes (v1.9.0)

## Intent

Keep TicTic's Clock screen focused on the current moment and make alarms/widgets feel like first-class clock features. Use a stronger circular timepiece identity, clean hierarchy and useful density, while keeping the app private, local and dependency-light.

## Home and navigation

- Replaced the earlier weak brand symbol with a higher-contrast circular clock face, clear hour/minute/second hands, dial marks and warm accent detail, used for the app identity and in-app surfaces.
- Render the live time during initial screen construction rather than waiting for a timer tick. The first body is now visible immediately instead of fading in from zero opacity; `MainActivity` updates the clock/greeting/date and avoids rebuilding the full view tree on ordinary resume.
- Home time defaults to a 12-hour readout with AM/PM. Settings also expose System and 24-hour formats, retaining device convention as an option.
- The top greeting is day-part aware and paired with a matching emoji. The hero card contains the clock alone plus the ordinal uppercase date beneath it—no “Local time”/“Your moment” label or duplicate date above.
- Alarms are shown immediately below the hero on Clock. The next content row is Quick Timer and Modes & Routines; World Clocks follows those shortcuts.
- The bottom destination rail uses bolder custom vector glyphs, horizontally scrolls, and offsets the selected destination toward center. Clock is centered on first opening and further destinations are reachable to its right.
- World Clocks follows the quick-action cards. Add-zone choices include EST/EDT (New York), IST (Kolkata), GMT (Greenwich), UTC and other popular regions; a custom IANA time-zone ID can be entered for less common zones. DST-aware regions use canonical zone IDs rather than a fixed abbreviation.
- Digital/analog choice, system/light/dark appearance and accent choices live in customization settings. Analog mode draws hour, minute and seconds hands.
- Distinct surface fills, outlines, restrained elevation and short entrance transitions make cards easier to parse without constant or distracting motion.

## Alarms and system behavior

The Clock screen and alarm-management destination share alarm editing. An alarm can use the device default/system tone, one of three bundled WAV tones (TicTic Dawn, Golden Hour, Pulse), or a tone selected from Android's ringtone picker. Existing repeat, label, vibration, snooze, gradual-volume and enable controls remain local.

Each alarm has a pre-alert enabled by default 10 minutes before its main occurrence. Users can disable it, select a 5/10/15/30/60-minute preset or enter a custom 1–180-minute lead, and choose a sound (same as alarm, device alarm tone, silent, one of the three bundled tones, or a tone from the system picker) and vibration. Reminder alerts use their own Android notification channel keyed by sound/vibration settings; Android and the user can further control that channel. If notification access is denied, the system blocks delivery. The selected reminder is refreshed when alarms are edited, disabled, removed, the device reboots or time-zone/time changes.

TicTic schedules main alarms and pre-alerts with Android `AlarmManager`; exact scheduling is used when the OS allows it and an inexact fallback is used otherwise. Exact-alarm and notification access are controlled by Android/user settings. A fallback alarm is not advertised as reliable for precision-critical timing.

## Widget system

The 24 registered provider components are presented as a curated 20-item gallery: five core clocks plus fifteen utility/signature entries. Components removed from the picker remain registered so existing home-screen placements continue to update. The `WidgetEngine` shares content rendering, style application, dimension-based visibility and click routing.

- Compact sizes prioritize a live clock/seconds; expanded widgets reveal available date, next alarm, progress, routine or world-time information.
- Glass is the transparent, wallpaper-oriented design: date above the seconds clock, next alarm beneath. It has per-instance text color, typeface/weight, size, panel/transparency and opacity controls.
- Other designs retain their themed cards until the user chooses another panel style. Studio preferences are keyed per app-widget instance.
- TextClock is used for continuously updating local time; World Pair uses a London-zone TextClock. Alarm/routine data refreshes when those records change, and providers update on boot/time changes/resize rather than forcing a high-frequency Java render loop.

See [`widget_specs.txt`](widget_specs.txt) for the full inventory and [`app/src/main/res/xml`](app/src/main/res/xml) for provider resize metadata.

## Lock screen: supported surface, not a promise of widgets

Ordinary third-party home-screen widgets do not have a single universal, cross-device lock-screen host on modern Android. TicTic therefore does not claim that its widget gallery will appear on every lock screen. It uses Android's next-alarm clock information when available, notification channels, and a show-when-locked/turn-screen-on alarm activity plus full-screen-intent path where the OS and user settings allow. Device makers, Android releases, notification privacy and full-screen-intent policy affect what appears. Validate it on target hardware.

## Research-informed decisions

Reference patterns reviewed: Google Clock's combined alarm, timer, stopwatch, world clock and widget offering; Alarmy's more explicit wake-up customization and routines; and Fossify Clock's offline/local-first clock stance. TicTic borrows the useful breadth and glanceable widget idea without copying intrusive wake-up missions. Instead of many copies of one widget, the gallery gives distinct styles or tasks (analog, world pair, focused timer, stopwatch, day-progress, nightstand, date-first and alarm-first) and changes density responsively.

- Google Clock: https://play.google.com/store/apps/details?id=com.google.android.deskclock
- Alarmy: https://play.google.com/store/apps/details?id=droom.sleepIfYouCan&hl=en
- Fossify Clock: https://f-droid.org/en/packages/org.fossify.clock/
- Android `AlarmManager`: https://developer.android.com/reference/android/app/AlarmManager

## v1.3 release additions (historical): popup, Studio and summary

- Settings is presented as an approximately 85%-screen modal; alarm editing uses a TicTic-styled custom dialog. The header wordmark/logo returns to Clock.
- Navigation and feature icons use thinner custom glyphs while preserving scrollable destinations.
- Widget catalog cards have design-specific samples/typefaces/corners. Widget Studio exposes date/alarm/caption/context toggles, date and alarm detail formats (including time-only), eight typeface choices, twelve ink colors, ten panel colors, size/alignment/opacity/transparency and per-instance persistence. The widget renderer applies configured styles while retaining clock-first resize behavior.
- The Activity tab summarizes estimated foreground and Clock-screen durations, event totals, widget render counts and battery snapshots over today/7-day/30-day ranges. Counters remain local. Battery snapshots are device-wide only: Android does not expose per-app/per-widget/per-screen mAh drain to ordinary apps, so TicTic does not attribute battery use to itself.

## v1.4 additions: previews, reconfiguration and efficiency

- Removed battery sampling from the dashboard and purges the v1.3 snapshot preferences on upgrade; TicTic no longer shows a battery metric.
- Strengthened the Activity/Log glyph and added chronological history for widget placement/style changes, opens and removals. Usage metrics and event history have separate user-controlled switches; history can be cleared.
- Widget Studio adapts its theme, spinner labels/dropdowns and swatches to light or dark mode. The unsaved preview shows a sample home screen with changeable wallpaper colorways and the widget layered over it.
- Widget provider metadata advertises `reconfigurable`; where the launcher supports Android's action, long-press Configure reopens the same per-widget preferences. Android/OEM launchers decide which affordance to show.
- Added panel silhouette/treatment choices (signature, soft, crisp, capsule, translucent Frost Pane, edge-only), layout priority and compact/balanced/roomy inner spacing. The shape/color/opacity/edge layers are static RemoteViews assets; no continuous blur or high-rate rendering is used.
- Added eight app accents and larger widget ink/panel palettes, plus app card corners and spacing.
- Seconds and metrics are user-configurable. Adaptive Battery Saver mode pauses second precision, slows foreground clock/timer/stopwatch redraws and omits the Clock entrance animation; widgets switch to minute precision when the system saver is active.

## v1.5 Studio refinement

- The wallpaper/home-screen sample, preview caption and wallpaper choices remain visible in a fixed upper region while only the editing controls scroll.
- Text and panel colors can be typed as `#RRGGBB` or `#AARRGGBB`; the visible values track palette choices and live preview state. The panel opacity is labeled with a numerical percentage.
- Treatment labels clearly distinguish TicTic Frost Pane · shaded glass from Outline-only glass.

## v1.6 Studio and screen refinement

- Studio preview now hosts the same RemoteViews XML layout and shared styling renderer used on the home screen. Wallpaper color matching is per-widget opt-in and uses only Android WallpaperColors metadata; if unavailable it uses a sample palette. No image read, capture, picker or storage permission is used.
- Replaced HEX entry with named palette chips, recent colors and a visual Hue/Saturation/Brightness dialog.
- Renamed controls to direct labels (Content spacing, Background style, Panel color, Panel opacity); spacing changes all four insets in both preview and widget.
- Widget panel fill, color, opacity, glass highlight, outline and no-panel states are coordinated. The panel and glass highlight render behind the content.
- Added a Matte-to-Glossy slider for TicTic glass; app screens retain task labels and move longer compatibility/privacy notes to About.

## v1.7 popup Studio and wallpaper color

- Added a wallpaper palette and visual HSV color picker; the selected color renders as a gradient sample wallpaper and is stored locally.
- Widget Studio now opens as a centered, dimmed, rounded popup at about 90% of the display. Its preview remains fixed while the settings scroll. Short fade/scale window transitions, wallpaper fades and preview pulses provide feedback.
- No network service, third-party library or image asset was added for these changes; the v1.7 debug APK grew by only 4,142 bytes relative to v1.6. Runtime feel and actual sizing still need device QA.

## v1.8 timer, Usage and widget gallery

- The Timer readout now wraps to content rather than relying on a fixed-height text box; font size steps down for HH:MM:SS. The custom editor previews changes immediately and uses Hours, Minutes and Seconds pickers; quick presets include one hour.
- Replaced the Activity/Log tab label and list glyph with Usage and a locally drawn trend-chart icon; usage summaries and the chronological history remain together.
- Added four resizable designs in an **Exciting widgets** section: raised-edge cyan Prism 3D, split-folio Flip Deck, Smart Console with separate Timer and Alarm tap actions, and Halo Orbit with a live local-day progress bar. Existing date/alarm/style/size Studio controls apply to each instance.
- New designs use original lightweight drawable layers inspired by the attached physical digital clock's deep bezel/cyan display; they don't simulate weather data, use blur, or add remote services.

## Verification and known limits

The v1.9.4 build and artifact hashes are recorded in [`ARTIFACTS.md`](ARTIFACTS.md). Android compile/package passed; Python source-contract checks passed 16/16; Gradle Android tests are NO-SOURCE. Lint was not run on v1.9.4 (the preceding v1.9.3 lint analysis timed out without a report). No emulator or handset is attached. Alarm ping sound/timing, one-tap interactions, notification actions, permission screens, volume keys, plus Clock/Timer/Stopwatch behavior remain unverified at runtime. Use [`TESTING.md`](TESTING.md) and the 38-case `QA-Clock-Comparison-v1.9.4.xlsx` before marking any P0 interaction PASS. Battery drain is not measured. The project does not promise a universal lock-screen widget, cloud synchronization, Wear OS companion, sleep-tracking service or anti-dismissal wake-up missions.


## v1.9.0 follow-up

- Curated the widget gallery to 20 short-named provider entries (five core plus fifteen selections from existing providers) while leaving provider identities installed for compatibility. This picker change does not fulfill the separate request for 15 genuinely new, distinct widget concepts; that work remains outstanding.
- Studio content controls are capability-specific. Date/alarm/extra rows and their format choices appear only where meaningful; panel color and opacity share a separate labeled surface card. The preview is a live `AppWidgetHostView` updated with `WidgetEngine.previewViews`, which reuses the actual provider layout XML and widget style renderer.
- Optional weather may be enabled separately per supported widget. City is manual; no location permission is used. A JobScheduler network-constrained periodic job stores current conditions locally and may be deferred by Android. Requests use Open-Meteo's free non-commercial API; attribution/CC BY 4.0 are disclosed and commercial use requires a paid plan.
- Wallpaper color adaptation uses WallpaperColors metadata only, selected per widget; metadata failure/platform unavailability falls back to a sample palette.
- The registered providers remain at 24 because removed designs must not strand existing placed widgets; the user-facing curated picker presents 20.

## v1.9.1 alarm follow-up

- Moved exact-alarm, notification and Android 14 full-screen explanations into the pre-save flow. Saving with delayed scheduling remains an explicit choice; denying notifications is explained as losing the pre-ping/notification actions, and denying full-screen access as losing the large locked-screen presentation.
- A reminder lead time already in the past now triggers a pre-save warning, and the alarm list indicates when the current occurrence cannot get a ping or when exact access is missing. Pre-alarm history distinguishes notifications blocked by the app/channel from a notification posted to Android.
- Replaced the ringing control layout with large, explicit, high-contrast Snooze and Stop actions. The foreground notification carries actionable Snooze and Stop, plus Turn off for repeating alarms. The alarm list includes upcoming date/time and snooze cancellation.
- Added a detailed Permissions & access inventory, volume-key modes (volume/Snooze/Stop), and an explicit Power-key limitation. Apps cannot reliably capture Android's Power key, so TicTic does not map it to snooze.
- SDK 34 compile/package and APK checks pass; no emulator/phone QA was possible. The prior device screenshot showed the exact class of issue that build success cannot catch, so this release remains runtime-unverified pending the checklist in `TESTING.md`.

## v1.9.2 reminder and key-path correction

- New and migrated default pre-alerts are a single short bundled notification ping 30 minutes before the scheduled alarm; the pre-alert receiver posts only a notification and does not start the alarm sound service. Custom lead times and alert tones remain available.
- Upgrade migration moves stored legacy 10-minute / “same as alarm” values to 30-minute / TicTic ping and reschedules them on package replacement. Since older data did not distinguish a user-selected 10 from the old default, stored 10-minute values also migrate; other values persist.
- Early volume-key dispatch now consumes a single down event for Snooze/Stop when the ringing Activity has focus; Control volume uses the alarm stream. The Power key remains Android/OEM-controlled. Keyguard dismissal flags were removed; the activity keeps show-when-locked/turn-screen-on behavior.
- Added a 33-case risk-based test matrix, Google Clock feature comparison and 11 static source-contract checks. Build and static checks pass; none substitutes for handset testing.


## v1.9.3 alarm safety and editor fixes

- Pre-alarm notifications include a direct Turn off alarm action for that alarm.
- Alarm receiver rechecks persisted enabled/snooze state to ignore deleted/disabled or stale queued deliveries.
- Editing replaces the stored JSON object by alarm ID and reschedules the saved values.
- Custom reminder minute input is immediately accessible through a visible Set/Edit control.
- Added four regression scenarios; source-contract checks remain static, and device runtime acceptance still requires the target Android handset.

## v1.9.4 ringing-notification control

- Added a Turn off action to the main ringing notification for both one-time and repeating alarms; it stops ringing and disables/cancels that alarm through the existing action receiver.
- Added ALM-025 and one structural contract test. Build passes; emulator/handset notification interaction remains unverified.
