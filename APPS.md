# MySpace — what is in the build

One install, fifteen apps. `applicationId app.space.one` · version **3.1.0** (versionCode 25) ·
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

**Calculator** — a recursive-descent parser, so `2+3*4` is 14 and not 20. Malformed input throws
and displays `Error` rather than silently evaluating to zero.

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
breadcrumbs. Uses app-private storage rather than the Storage Access Framework, because
`useAndroidX=false` means `DocumentFile` is unavailable — so it needs **no permission at all**.
Every navigation step is guarded, so `..` and absolute paths are refused and nothing can walk
outside the root.

**Documents** — plain-text documents and CSV spreadsheets. CSV follows RFC 4180 quoting, so a
comma inside a cell does not shift every later column. Sheets report row and column counts and a
total over the first numeric column.

**Messages** — conversations read from the system SMS provider and sent through the framework
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
| **Scanner — live camera capture** | Full-resolution capture needs a `FileProvider` to hand the camera app a writable URI, and `FileProvider` ships with AndroidX, which this project has disabled | Enabling AndroidX. Picking images works now and produces the same PDF |

## Permissions

| Permission | Which app | When it is requested |
|---|---|---|
| `INTERNET` | Weather | Already declared for the clock's weather widget. Nothing polls in the background |
| `RECORD_AUDIO` | Recorder | Only when you first press Record, never at launch |
| `READ_SMS`, `SEND_SMS` | Messages | Only when you open the message list or press send |

**Not requested by anything:** location, contacts, storage, camera, calendar. Files, Scanner and
Documents all work inside app-private storage, which needs no permission at all.

The clock's existing permissions are unchanged: exact alarms, notifications, boot-completed,
wake lock, vibrate, full-screen intent, foreground service and `BIND_JOB_SERVICE`.

## Where your data lives

Every app stores its own data under a stable key in the shared `tictic_local_v1` preferences
file, or as files under app-private storage. Data outlives its module: the formats are plain JSON
and plain files with no module-specific encoding, so they survive a module being excluded from a
build and are readable by the export/import bundle that is still to come.

| Module | Pref key(s) or files | Cap |
|---|---|---:|
| `:feature-calendar` | `calendar_items` | 1000 |
| `:feature-expenses` | `expenses_items` | 2000 |
| `:feature-habits` | `habits_items` | 100 |
| `:feature-notes` | `notes_items` | 500 |
| `:feature-tasks` | `tasks_items` | 1000 |
| `:feature-weather` | `weather_city`, `weather_payload`, `weather_fetched_at`, `weather_celsius` | — |
| `:feature-music` | `music_streams`, `music_picked`, `music_repeat`, `music_shuffle` | — |
| `:feature-recorder` | `recorder_index`, plus audio files under `files/recordings/` | — |
| `:feature-documents` | `documents_items` | — |
| `:feature-email` | `email_items` | — |
| `:feature-scanner` | PDFs under `files/scans/` | — |
| `:feature-files` | nothing of its own — it browses `filesDir` | — |

## Navigating it

The rail at the bottom shows your pinned destinations. It has an **Apps** slot which opens a
picker listing every installed app. Tapping the MySpace wordmark in the header returns to the
clock — that is the only way back, and while another app is on screen the header names that app
and hides the clock's settings gear, because clock settings do not describe another app's screen.

Settings ▸ Navigation lets you reorder, pin and hide destinations; the choices are stored as
routes rather than positions, so reordering cannot silently retarget a widget deep link. Hiding
the last visible destination is refused, because an empty rail is a state you could not escape
without clearing app data. Hidden destinations keep their data and still open from widgets.

Two navigation modes — hub and drawer — are declared but not yet rendered; the rail is the only
visual mode, and the Apps picker is the interim app switcher.
