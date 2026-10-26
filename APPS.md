# MySpace — what is in the build

One install, fifteen apps. `applicationId app.space.one` · version **3.12.18** (versionCode 55) ·
`minSdk 26` · `targetSdk 34` · 22 Gradle modules.

Tagline: *One for many.*

Everything below is generated from the source, so it cannot drift from what is actually
registered. The suite is `MySpace`, grown out of the TicTic clock app; the clock is still here,
unchanged in behaviour, as app number 1.

## The fifteen apps

| # | App | Module | id | Rail order | Icon | What it is |
|---:|---|---|---|---:|---|---|
| 1 | **Clock** | `:feature-clock` | `clock` | 0 | `CLOCK` | Alarms, reminders, timer, stopwatch and routines. |
| 2 | **Calculator** | `:feature-calculator` | `calculator` | 20 | `CALCULATOR` | Arithmetic that never leaves the device. |
| 3 | **Notes** | `:feature-notes` | `notes` | 30 | `NOTES` | Notes and tasks, stored on this device. |
| 4 | **Habits** | `:feature-habits` | `habits` | 40 | `HABITS` | Daily habits, streaks and weekly rate. |
| 5 | **Expenses** | `:feature-expenses` | `expenses` | 50 | `EXPENSES` | Track spending by category, on device. |
| 6 | **Calendar** | `:feature-calendar` | `calendar` | 60 | `CALENDAR` | Events and dates, kept on this device. |
| 7 | **Tasks** | `:feature-tasks` | `tasks` | 70 | `TASKS` | Checklists with due dates and priority. |
| 8 | **Weather** | `:feature-weather` | `weather` | 80 | `WEATHER` | Forecasts from Open-Meteo for a city you type. |
| 9 | **Recorder** | `:feature-recorder` | `recorder` | 90 | `RECORDER` | Voice recordings, stored on this device. |
| 10 | **Files** | `:feature-files` | `files` | 100 | `FILES` | Browse the files this app owns. |
| 11 | **Documents** | `:feature-documents` | `documents` | 110 | `DOCUMENTS` | Write text documents and CSV spreadsheets. |
| 12 | **Messages** | `:feature-messages` | `messages` | 120 | `MESSAGES` | Text messages, sent and received on this phone. |
| 13 | **Music** | `:feature-music` | `music` | 130 | `MUSIC` | Play files you own and streams you add yourself. |
| 14 | **Scanner** | `:feature-scanner` | `scanner` | 140 | `SCANNER` | Turn picked images into a PDF document. |
| 15 | **Email** | `:feature-email` | `email` | 150 | `EMAIL` | Compose and archive mail; sending uses your own mail app. |

Registration is a single list: `MainActivity.suiteApps()`. The rail, the app picker and the
availability check all read it, so adding an app is one field plus one entry and nothing else in
the host. Each app has its own icon glyph — no two share one.

## What each app actually does

**Clock** — the original app: world clock, alarms, reminders, timer, stopwatch, routines, usage
and an activity log. Its seven destinations are navigation routes, so they can be reordered,
pinned or hidden individually from Settings ▸ Navigation, and widgets deep-link into them.

**Calculator** — recreated key for key after Google's calculator and then compared against it
feature by feature (3.12.0): the basic pad with Google's contextual percent (`200+10%` is 220),
live grey result under the display, double-width filled equals; a scientific page with sin, cos,
tan and their inverses, ln, log, sqrt, factorial, power, pi and e behind a DEG/RAD toggle;
history you open by swiping the display down, tap to reuse, long-press to copy and clear when
you want, persisted like Google's; hold-the-display to copy; a display that scrolls for long
numbers; key haptics. A recursive-descent parser underneath, so `2+3*4` is 14 and not 20, and
malformed input throws and displays `Error` rather than silently evaluating to zero. Keys refuse
sequences the grammar cannot hold, one tap at a time -- the rule that came from a real phone
typing `0/0------85522*3*-`.

**Notes** — titled notes with a case-insensitive search over title and body, newest first with
ties broken on id so the list cannot reshuffle under your finger.

**Habits** — daily tick-off with current streak, longest streak and a weekly rate. Streaks are
computed in epoch days, and a run ending *yesterday* still counts, so a habit does not reset at
midnight before you have had a chance to complete it.

**Expenses** — money is held in minor units as longs, never as doubles, so totals cannot drift
into `30.000000000000004`. Bad input is refused rather than stored as zero. Month totals,
category breakdown largest-first, and a half-open month filter.

**Calendar** — a month grid and events on a chosen day. The grid is computed in a fixed UTC
calendar and is always 42 cells, so it does not shift a day between time zones or change height
between months. It deliberately does **not** read the device calendar provider: that would need
`READ_CALENDAR`, which the Settings page's "minimum access, and why" promise would then have to
justify.

**Tasks** — checklists with due dates and priority. Undated tasks sort *after* every dated task
rather than floating to the top, and a blank due date stays blank instead of defaulting to today
— defaulting would make every task overdue by tomorrow and train you to ignore the list.

**Weather** — Open-Meteo, no API key and **no location permission**: the city is typed, which is
what lets Settings keep promising no location is ever requested. Current conditions plus a
three-day outlook, °C/°F toggle. Fetches only when you press Update; a cached reading older than
twenty minutes is labelled stale rather than presented as current.

**Recorder** — voice notes, recorded to app-private storage, listed with duration and size, and
playable in-app. One hour cap. The recorder is released when the screen goes away, so it never
holds the microphone in the background.

**Files** — a browser for the files this app owns, with sizes, kinds, folder navigation and
breadcrumbs, a Notepad-grade text editor, sheet/image/PDF viewers, and whole-app export/import
as a zip (Files ▸ Backup). Since 3.11.0 it also reaches shared storage the honest way: the user
picks ONE folder through the Storage Access Framework (framework `DocumentsContract`,
persistable grant, no new dependency), Files browses it, opens files through the system, and
long-press copies them into app storage where the editor and viewers already live. It still
declares **no storage permission at all**. Every navigation step is guarded, so `..` and
absolute paths are refused and nothing can walk outside the root.

**Documents** — plain-text documents and CSV spreadsheets. CSV follows RFC 4180 quoting, so a
comma inside a cell does not shift every later column. Sheets report row and column counts and a
total over the first numeric column.

**Messages** *(full build only)* — conversations read from the system SMS provider and sent through the framework
`SmsManager`. Numbers are folded so `+91 98765 43210`, `098765 43210` and `9876543210` are one
conversation, while two genuinely different numbers stay separate. Alphanumeric sender headers
such as `VK-TICTIC` are kept distinct rather than all collapsing into one "Unknown sender"
thread. Long bodies are sent multipart so they are not silently truncated, and the segment count
shown before sending accounts for UCS-2, because any non-GSM character halves the segment size.

**Music** — your own audio files, picked through the system picker, and stream URLs you add
yourself. **No catalogue and no partner SDK**: that is the licensing constraint respected rather
than worked around. Only `http` and `https` may be streamed, because the field is user input and
a `file:` string would let it read arbitrary local paths. Repeat and seeded shuffle, so the queue
cannot move while you are looking at it.

**Scanner** — picked images assembled into one PDF, with per-page reorder, rotate and remove,
A4 or US Letter, and a size estimate before saving. Uses the framework `PdfDocument`, so it adds
**no dependencies**: CameraX plus ML Kit would add roughly 50 MB, which is what a `lite` flavour
cannot carry. Pages are fitted by the *minimum* of the width and height ratios — fitting by width
alone would overflow the sheet height, and the framework clips rather than reports, so the bottom
of every page would be missing with nothing to indicate why.

**Email** — compose, drafts, and a local archive of what has been sent, handing the message to
the mail app already on the device. **No credential is stored and no OAuth is needed**, which is
why this works without any account verification. Recipient parsing honours quoted display names,
so `"Doe, Jane <jane@example.com>"` is one recipient and not two invalid halves.

## What is deliberately NOT in the build

Each of these is an owner-side obligation or a platform constraint, not unfinished engineering.
Every one is stated **on the screen concerned** rather than hidden behind a UI that implies
otherwise.

| Gap | Why | What would unblock it |
|---|---|---|
| **Email — receiving** | Reading a mailbox needs IMAP, which needs either an app password held by this app or a verified OAuth sign-in | Gmail OAuth verification: a privacy policy, and possibly a security assessment |
| **Messages — Play distribution** | Play restricts SMS and Call Log permissions to apps whose *core function* is messaging | Filing Play's SMS and Call Log declaration form. The direct-APK build is unaffected and works now |
| **Music — commercial catalogue** | Streaming a catalogue needs partner agreements or a licensed SDK | A licensing deal. Local files and user-supplied URLs work now |
| **Scanner — in-app live preview** | AndroidX is now ON (3.11.0) and capture through the system camera works (3.10.0); what remains is CameraX preview inside the app, which is a visual change | The device loop: visual changes ship only where a regression can be seen. System-camera capture and image picking both work now and produce the same PDF |

## Permissions

| Permission | Which app | When it is requested |
|---|---|---|
| `INTERNET` | Weather | Already declared for the clock's weather widget. Nothing polls in the background |
| `RECORD_AUDIO` | Recorder | Only when you first press Record, never at launch |
| `READ_SMS`, `SEND_SMS` | Messages, **full build only** | Only when you open the message list or press send. The lite build does not declare them at all — see below |

**Not requested by anything:** location, contacts, storage, camera, calendar. Files, Scanner and
Documents all work inside app-private storage, which needs no permission at all.

The clock's existing permissions are unchanged: exact alarms, notifications, boot-completed,
wake lock, vibrate, full-screen intent, foreground service and `BIND_JOB_SERVICE`.

## Where your data lives

Every app stores its own data under a stable key in the shared `tictic_local_v1` preferences
file, or as files under app-private storage. Since 3.11.0 notes and calendar events also live in
a Room database (`myspace.db`) — the app reads Room, and the prefs JSON stays as the mirror,
which is what the export bundle carries and what repopulates the database after a restore. Data
outlives its module: the formats are plain JSON and plain files with no module-specific
encoding, so they survive a module being excluded from a build, and the export/import bundle
(Files ▸ Backup) moves all of it between installs.

| Module | Pref key(s) or files | Cap |
|---|---|---:|
| `:feature-calendar` | Room `events` table, mirrored in `calendar_items` | 1000 |
| `:feature-expenses` | `expenses_items` | 2000 |
| `:feature-habits` | `habits_items` | 100 |
| `:feature-notes` | Room `notes` table, mirrored in `notes_items` | 500 |
| `:feature-tasks` | `tasks_items` | 1000 |
| `:feature-weather` | `weather_city`, `weather_payload`, `weather_fetched_at`, `weather_celsius` | — |
| `:feature-music` | `music_streams`, `music_picked`, `music_repeat`, `music_shuffle` | — |
| `:feature-recorder` | `recorder_index`, plus audio files under `files/recordings/` | — |
| `:feature-documents` | `documents_items` | — |
| `:feature-email` | `email_items` | — |
| `:feature-scanner` | PDFs under `files/scans/` | — |
| `:feature-files` | `files_saf_tree` (the shared-folder grant); otherwise it browses `filesDir` | — |

## Navigating it

The rail at the bottom shows your pinned destinations. It has an **Apps** slot which opens a
picker listing every installed app. Tapping the MySpace wordmark in the header returns to the
clock — that is the only way back, and while another app is on screen the header names that app
and hides the clock's settings gear, because clock settings do not describe another app's screen.

Settings ▸ Navigation lets you choose the **navigation style** and reorder, pin and hide
destinations. Choices are stored as routes rather than positions, so reordering cannot silently
retarget a widget deep link. Hiding the last visible destination is refused, because an empty
rail is a state you could not escape without clearing app data. Hidden destinations keep their
data and still open from widgets — and they stay hidden in all three styles, since all three
read the same visible-destination list.

| Style | What you get | What it costs |
|---|---|---|
| **Rail** *(default)* | One scrolling row at the bottom. Every destination is one tap away. | Two taps to reach an app that is not pinned |
| **Hub** | Every destination **and** every installed app as a tile, in two rows. Nothing sits behind a picker. | The most vertical space of the three |
| **Drawer** | One bar naming where you are; a handle summons the full list. | Destinations are no longer one tap away |

Rail is the default, so nothing changes for an existing install until you choose otherwise.

**Inside another app** the bottom bar changes: it shows a Clock button to go home, the name of
the app you are in, and an Apps button to switch directly. The clock's own destinations are not
offered there, because they route into the clock — offering them over another app would move you
there without saying so. Before this, opening any app other than the clock rendered no navigation
at all, so switching apps meant going home first.

## Two builds: lite and full

Google Play Protect **refuses rather than warns** about any APK installed outside the Play Store
that declares SMS access. While the manifest carried `READ_SMS` and `SEND_SMS`, every sideloaded
build of this suite was blocked at install — all fifteen apps, taken down by the one that uses
them. There are therefore two flavours:

| Build | SMS permissions | Messages app | Installs sideloaded |
|---|---|---|---|
| **lite** | not declared | opens, explains it is not in this build | yes, as the `dist` build type |
| **lite-dist** *(published here)* | not declared, not debuggable | same | yes, and Play Protect's harmful-app trigger is gone |
| **full** | declared | complete, once you grant access | no — Play Protect blocks it until the Play declaration form is filed |

The flavour manifest strips the permissions with `tools:node="remove"`, and the Messages screen
detects that its permission is undeclared — `checkSelfPermission` cannot tell "not granted" from
"not grantable" — so it states the situation instead of offering a button that could never work.
