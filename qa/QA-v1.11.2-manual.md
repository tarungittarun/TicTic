# TicTic v1.11.2 focused device QA

**Status: NOT RUN.** This is a test plan, not evidence of Android runtime behavior. `adb devices -l` found no attached device. Run on a physical device and a supported emulator where possible. For every case record model, Android/API, OEM, screen size/orientation, theme, glass preset, motion mode, steps, actual result and evidence.

## A. Settings → Alarms and alarm-default editor

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| UI01 | Open Settings → Alarms on a compact phone | Settings fills the page; horizontal settings-category row and vertically scrollable content remain distinct; no bottom navigation overlaps the content. | NOT RUN |
| UI02 | Scroll Settings → Alarms from top to bottom | All alarm settings and cards can be reached; content remains inside the scroll viewport with no painting over system bars or adjacent controls. | NOT RUN |
| UI03 | Open Configure alarm defaults on a compact phone | Editor is opaque and full-screen, with heading/close and Cancel/Save defaults pinned while the form body scrolls. | NOT RUN |
| UI04 | Scroll alarm defaults to final controls on a short screen | Last reminder controls are reachable; none overlap or hide behind the fixed action footer. | NOT RUN |
| UI05 | Focus any editable field / open Android keyboard in an alarm form | Window resizes as supported; fields can scroll above the keyboard and fixed actions remain usable. | NOT RUN |
| UI06 | Use Cancel, close, Save defaults; reopen | Cancel/close do not save; Save persists choices; settings scroll position and page behind the editor behave correctly. | NOT RUN |
| UI07 | Open default editor on a wide tablet/landscape window | Editor stays a centered, bounded dialog with contained scrolling and visible title/actions. | NOT RUN |
| UI08 | Change snooze, vibration, gentle volume, alarm sound, reminder lead/sound/vibration | Values persist and appear in the Settings summary; new alarms inherit defaults while per-alarm overrides remain independent. | NOT RUN |

## B. Alarm create/edit and dialogs

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| AL01 | Create and edit an alarm on a compact phone | Opaque full-screen form; heading and action footer remain visible; body alone scrolls. | NOT RUN |
| AL02 | Create and edit on a wide screen | Form is centered and bounded; it does not cover the full display unnecessarily or clip the footer. | NOT RUN |
| AL03 | Scroll through More settings; focus label with keyboard | Every control can be reached and the keyboard does not permanently hide actions. | NOT RUN |
| AL04 | Open nested date, time, tone, confirmation and custom-number dialogs | Dialog width/height is bounded; long message/view content scrolls within its viewport; actions remain visible. | NOT RUN |
| AL05 | Tap outside, use close, Cancel, Save, Delete | Draft/confirmation behavior is predictable; outside tap cannot discard main alarm edits; explicit actions work. | NOT RUN |
| AL06 | Test Light, Dark and Glass enabled | Alarm controls and text remain legible; backgrounds are opaque enough; glass never sits on top of critical text or controls. | NOT RUN |

## C. Widget Studio

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| WS01 | Open Studio for a new widget on a compact phone | Full-screen Studio; header, live final-layout preview and Cancel/Apply footer are pinned; configuration form scrolls independently. | NOT RUN |
| WS02 | Scroll from content settings through colors/weather to final settings | All controls can be reached without overlap, clipping or content drawing behind the footer. | NOT RUN |
| WS03 | Open Studio on a wide screen | Centered, bounded popup with visible margins, pinned preview/actions and bounded internal scroll. | NOT RUN |
| WS04 | Resize/rotate screen and open on short-height display | Preview remains bounded; scroll region shrinks and still scrolls to the end; Apply stays reachable. | NOT RUN |
| WS05 | Choose each of five glass presets; adjust finish, opacity and panel color | Preset preview reflects its hue; edits are marked custom, keep the chosen preset character, and update the widget scene. | NOT RUN |
| WS06 | Apply preset, save, close and reopen Studio | Selected preset and all custom values persist and render on the actual RemoteViews preview/widget. | NOT RUN |
| WS07 | Check text contrast warning at light/dark panel/text combinations | Low-contrast combinations are flagged; text remains readable against glass and selected palette colors. | NOT RUN |
| WS08 | Use keyboard for city/color entry and scroll | Keyboard does not cover input/actions; focused field remains reachable. | NOT RUN |
| WS09 | Cancel edits and compare placed widget; then Apply edits | Cancel discards unsaved changes; Apply saves to correct widget ID and refreshes the real widget. | NOT RUN |

## D. Glass presets, themes and legibility

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| GL01 | Enable Glass; preview Sunlit Bubble, Daylight Pearl, Golden Hour, Moonlit Opal and Aurora Frost | Five visually distinguishable but coherent light moods; names/notes and selected state are clear. | NOT RUN |
| GL02 | Tune surface opacity, sheen and depth; switch among presets | Sliders update the live preview; selecting a preset resets that preset’s starting values; later edits persist as custom. | NOT RUN |
| GL03 | Visit Clock, Reminders, Timer, Stopwatch, Routines, Usage and Settings | Shared glass remains behind content; text, controls and primary actions are not washed out or obscured. | NOT RUN |
| GL04 | Switch Light/Dark/System; restart app | Preference persists; theme contrast/highlight adapts and chosen preset/custom values remain applied. | NOT RUN |
| GL05 | Test translucent/high-sheen extremes, all accents and selected/unselected states | Body text, labels, fields, popup contents and critical alarm actions remain comfortably legible. | NOT RUN |
| GL06 | Ring alarm with Glass in light and dark appearances | Snooze/Stop labels and touch targets remain obvious; critical surface uses its opacity guardrail. | NOT RUN |
| GL07 | Verify copy and visual effect | UI does not claim live background blur or a captured wallpaper; effect reads as a layered tinted bubble/highlight. | NOT RUN |

## E. Touch feedback and reduced motion

| ID | Scenario | Expected result | Status |
|---|---|---|---|
| MO01 | Select Water ripple, Gel squeeze, Expressive spring, Glass wave and Magnetic lift | Each of the five options is selectable and is reflected in the preview label; preference persists after rerender/relaunch. | NOT RUN |
| MO02 | Press buttons/cards and controls on Clock, Settings and Studio | Feedback is distinct but restrained; release restores geometry and triggers only the intended action once. | NOT RUN |
| MO03 | Move finger out of a target, cancel a touch, tap rapidly | No accidental action; control does not remain pressed/scaled; scrolling still works. | NOT RUN |
| MO04 | Set Reduce motion and repeat press tests | Ripple remains but scale/lift/bounce are suppressed. | NOT RUN |
| MO05 | Disable Android system animations without app Reduce motion | App recognizes system setting and suppresses nonessential motion. | NOT RUN |
| MO06 | Use TalkBack and keyboard/accessibility navigation | Click actions remain operable and content descriptions/states are announced; touch listener does not block accessibility. | NOT RUN |

## F. Inherited regression

- Re-run alarm, reminder, ring notification/volume shortcut, Usage, timer, stopwatch, permissions, widget placement/reconfiguration, reboot/time-change and optional-weather cases in previous `qa/QA-v1.11.1-manual.md` plans.
- Include at least one target handset plus Android 12+ and 14+ behavior, and a major OEM skin; include compact phone and larger-screen/window-size configurations.
- Record app defects separately from launcher/OEM/system limitations. Do not infer runtime success from build, source-contract tests, lint, screenshots of a mock, or this plan.
- No cases above have been run in this workspace.
