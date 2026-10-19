# MySpace — architecture and delivery plan

> **MySpace · `app.space.one` · "One for many."**
> The TicTic clock becomes one app inside MySpace, keeping its own v1.11.2 lineage.

**Status:** approved direction; Phase 0 (CI) in progress. Open items are in §9.
**Scope:** grow the current single-app TicTic clock (v1.11.2, `app.tictic.clock`) into one install — **MySpace** — that hosts many apps.

**Owner decisions, 2026-10-18:**

1. Multi-module Gradle.
2. Rebrand to **MySpace**, `applicationId app.space.one`.
3. Adopt AndroidX; keep the toolchain flexible (see §5.1).
4. Navigation is **user-customisable**, not fixed.
5. **Keep every requested app — including email, music streaming and messaging — but the user controls which ones they have.** This is the defining product requirement and it changes the architecture; see §3.4.
6. Build and verify through GitHub Actions (no local Android build is possible — §1).

---

## 1. Ground truth (verified 2026-10-18)

| Aspect | Current state |
|---|---|
| Gradle | Gradle 7.6.4 wrapper, AGP 7.4.2, single `include ':app'` |
| Module | `:app`, namespace and `applicationId` = `app.tictic.clock`, `compileSdk 34`, `minSdk 23`, `targetSdk 34`, versionCode 23, versionName 1.11.2 |
| Code | 37 Java files, ~2,771 LOC, all in package `app.tictic.clock` |
| UI | Built **entirely in Java code**. `res/layout/` contains only `widget_*.xml`; there are no screen layouts |
| Navigation | `MainActivity` (501 dense lines): `String[] tabs = {"Clock","Reminders","Timer","Stopwatch","Routines","Usage"}`, one `render()` rebuilding the whole tree, plus a `settings`/`settingsGroup` mode |
| Data | one SharedPreferences file `tictic_local_v1`; collections stored as JSON strings via `DataStore.array()/putArray()` |
| Dependencies | **zero runtime dependencies**. `android.useAndroidX=false`. Vendored `junit-4.13.2.jar` + `hamcrest-core-1.3.jar` for tests only |
| Widgets | 24 registered `AppWidgetProvider` receivers, all inside `:app` |
| Permissions | RECEIVE_BOOT_COMPLETED, INTERNET, SCHEDULE_EXACT_ALARM, POST_NOTIFICATIONS, VIBRATE, WAKE_LOCK, FOREGROUND_SERVICE(+MEDIA_PLAYBACK), USE_FULL_SCREEN_INTENT |
| Tests | `qa/*.py` source-contract suite: **57 tests, passing locally**. 1 JVM unit test (`TimeMathTest`) |
| CI | **none** — no `.github/` directory on any branch |
| Git history | one commit, `26eca2f Added TicTic Project` |

### Two constraints that shape everything

**(a) No local Android build is possible in this workspace.** There is no JDK and no Android SDK, and `services.gradle.org` and `dl.google.com` are network-blocked (TLS refused), so the Gradle wrapper cannot even fetch its distribution. `bash gradlew assembleDebug` will not run here. **GitHub Actions is the only build/verify path** — which is why Phase 0 is CI, not features. `python3 -m unittest discover -s qa` *does* run locally and is the fast inner loop.

**(b) `gradlew` is committed without the executable bit** (`-rw-r--r--`). On a Linux CI runner `./gradlew` fails with permission denied. The workflow therefore calls `sh gradlew`, and the filemode should be fixed in the repo.

---

## 2. Feasibility of the requested apps

The 13 requested apps are **not** comparable in difficulty. They fall into three tiers, and the tier boundaries are set by Android platform policy, external-service requirements, and dependency weight — not by how much code we write.

### Tier A — offline, no external service, no heavy dependency. Build these.

| # | App | Notes | New permissions | Key dependencies |
|---|---|---|---|---|
| 1 | **Calculator** | Pure logic + UI. Reuses the existing dial/press-style design language. Fastest win; ideal first module to prove the shell. | none | none |
| 2 | **Notes / Tasks** | Local text + checklists. Wants real storage (see §6). | none | Room (or hand-rolled SQLite) |
| 3 | **Habit tracker** | Streaks, calendar heat-map. Mostly date arithmetic — the codebase already has `TimeMath` and `nextOccurrence()`. | none | Room |
| 4 | **Expense tracker** | Amounts, categories, monthly totals, CSV export. **Reuse the existing CSV export path from Usage** — it already writes via the document picker. | none | Room |
| 5 | **Weather** | Becomes a first-class app instead of a widget opt-in. **Open-Meteo is already integrated** (`WeatherJobService`, `WeatherRefresh`) and needs no API key; city is entered manually so no location permission. | none (INTERNET already declared) | none new |
| 6 | **Files** | Browse app-private + SAF-selected directories. A general file manager over shared storage is scoped badly by Android 11+; SAF-scoped browsing is honest and buildable. | READ_MEDIA_* on 33+, or SAF-only with **zero** permissions | DocumentFile (AndroidX) |
| 7 | **Recorder** | Voice notes via `MediaRecorder`. Small, self-contained, no backend. | RECORD_AUDIO (runtime) | none |
| 8 | **Music player (local)** | Plays audio already on the device + notification/lock-screen controls. The existing `AlarmSoundService`/`ReminderSoundService` foreground-service pattern transfers directly. | READ_MEDIA_AUDIO (33+) / READ_EXTERNAL_STORAGE | MediaSession (AndroidX) |
| 9 | **Calendar (local)** | Month/week views over the app's own events, reusing alarm/reminder/routine data. **Local-only calendar is Tier A.** Two-way sync is Tier B. | none for local-only | none |

### Tier B — buildable, but needs a real permission, a heavy dependency, or a big scope cut

| App | Why it is hard | Honest path |
|---|---|---|
| **Doc scanner** | Needs camera capture + perspective correction + OCR. The good OCR (ML Kit) is a large Google Play Services dependency; OpenCV is ~50 MB+. Also conflicts with the current "no third-party runtime libraries, all art local" promise. | CameraX + ML Kit Document Scanner, **Play-Services-dependent**, clearly labelled as an online/large-download feature. Or a "capture + auto-crop to PDF" version without OCR, which is far lighter. |
| **Calendar sync** | Reading/writing the device calendar needs READ/WRITE_CALENDAR; Google Calendar API needs OAuth + a Cloud project; CalDAV needs a real protocol client. | Ship local-only first (Tier A #9). Add `CalendarContract` read/write later as an opt-in. Full CalDAV/Google sync is a separate project. |
| **Doc writer** | A word processor means a rich-text engine: styled spans, pagination, undo stack, DOCX/ODF/PDF export. This is not a screen, it is a subsystem. | Scoped v1: plain-text/markdown notes with PDF and HTML export. DOCX interop is a later, separate effort. |
| **Spreadsheet writer** | A grid with formula parsing, cell dependencies, recalculation and XLSX export is a larger project than the entire current app (~2.8k LOC). | Scoped v1: CSV-backed table editor (add/edit/sort rows, per-column types, CSV export). A real formula engine is out of scope for this roadmap. |
| **Messages (SMS)** | Sending SMS needs SEND_SMS; being the default SMS app requires implementing all four of the platform's default-SMS roles (inbox, compose, quick-response, headless send). Google Play also restricts SMS/Call-Log permissions to apps whose **core** function is SMS — a multi-app suite will struggle to get approval, and it is a policy risk for the whole listing. | Recommend: **notification/reminder-style local messaging** or an in-app inbox, not carrier SMS. If real SMS is a hard requirement it should be its own standalone app, not part of the suite. |

### Tier C — kept on the roadmap, but they carry real external obligations

The owner's direction is to **keep all three** and give the user control over whether they are present. That is a sound product answer, and §3.4 explains how the architecture supports it. What it does *not* remove is the external work each one needs — these are the only three apps in the suite whose cost is dominated by things outside this repository.

| App | Obligation that must be budgeted | How it ships under user control |
|---|---|---|
| **Email** | A usable client needs IMAP + SMTP + MIME, and in 2026 the major providers require OAuth 2.0 backed by a **verified Google/Microsoft Cloud project**, a published privacy policy, and — for restricted Gmail scopes — a security assessment. Hand-rolled IMAP is months of protocol edge cases, so a vetted mail stack (Angus Mail or similar) is the realistic route. | Optional module, **off by default, not downloaded until enabled**. Declares no mail permission until the user turns it on. Sequenced last, after Phase 4. |
| **Music streamer** | There is no legal free catalogue. Commercial catalogues require partner agreements and their own SDKs; shipping a "streamer" without licensing is a takedown and Play-policy risk. The honest buildable form is **local playback + user-supplied internet-radio/stream URLs**, with a commercial catalogue added only if an agreement is ever secured. | Optional module, off by default. Network use is per-module opt-in, matching the existing per-widget weather pattern. |
| **Messages (SMS)** | Google Play restricts SMS/Call-Log permissions to apps whose **core** function is SMS. A suite whose core is "many apps" is unlikely to qualify, and a rejection here risks the **whole listing**, not just this module. Being a default SMS app also requires implementing all four platform roles (inbox, compose, quick-response, headless send). | Optional module, off by default, and — critically — **carrier SMS must be reachable without the suite holding the SMS permission by default**. If Play approval proves impossible, ship the in-app/local messaging form and keep carrier SMS as a separate standalone app. |

> **Consequence for the product promise.** `README.md` and `PRODUCT.md` currently state: *"no account, ads, remote analytics or third-party app/runtime libraries"*, local-first, offline. Choosing **AndroidX + multi-module** already ends the zero-dependency policy; **doc scanner** ends the no-heavy-SDK policy; **email and streaming** end local-first and no-account for those modules. These documents are the project's contract, so they are rewritten in Phase 1, not after. Suggested replacement policy: *local-first by default; every app is individually optional; every network-dependent app is opt-in, named, and listed in About; no ads, no remote analytics; accounts exist only inside the modules that need them.*

---

## 3. Target architecture

### 3.1 Module graph

```
:app                      → thin application module. Manifest merge target,
                            Application class, launcher Activity, app id
                            (app.space.one), icons. Holds NO feature code.
:shell                    → the suite chrome: app registry, hub/rail/drawer,
                            customisation (reorder/pin/hide), search, per-app
                            settings groups, deep-link routing.
:core-design              → palette, accents (the existing 16), glass presets,
                            AppGlassDrawable, GelMotion, AppSurfaceStyle,
                            typography/corner/border/elevation/spacing scales,
                            press styles, Reduce-motion. Extracted from today's
                            :app so every feature module inherits one look.
:core-data                → prefs access, Room database + DAOs, JSON migration
                            from tictic_local_v1, CSV export, backup/restore.
:core-domain              → TimeMath, scheduling helpers, occurrence rules —
                            pure JVM, no Android imports, unit-testable.
:core-platform            → permissions flows, exact-alarm/notification/full-screen
                            preflight, boot/time-change rescheduling, power-saver,
                            usage tracking, foreground-service base.
:core-widgets             → the widget renderer + 24 providers + Widget Studio,
                            so any feature app can expose widgets.
:feature-clock            → today's Clock/Alarms/Reminders/Timer/Stopwatch/
                            Routines/Usage, moved out of :app unchanged in behaviour.
:feature-calculator
:feature-notes            → notes + tasks
:feature-habits
:feature-expenses
:feature-weather          → promoted from widget opt-in to a full app
:feature-files
:feature-recorder
:feature-music            → local playback
:feature-calendar         → local calendar
```

Dependency rule: `:feature-* → :core-* → (nothing)`, `:shell → :core-*`, `:app → :shell + all :feature-*`. **Feature modules never depend on each other.** Cross-app data flows through `:core-data` interfaces only — that is what keeps "add app N" cheap instead of a refactor each time.

### 3.2 The app-registry contract (the piece that makes this scale)

Today's navigation is a hardcoded array inside a 501-line Activity. That is the single biggest blocker. Replace it with a declaration each module contributes:

```java
public interface SuiteApp {
    String id();                 // stable, e.g. "clock", "calculator"
    String label();              // "Calculator"
    String tagline();            // one line for the hub tile
    Drawable icon();             // vector, themed by :core-design
    int defaultOrder();          // initial position in the hub
    boolean supportsWidgets();   // surfaces a Widget Studio entry
    List<SettingsGroup> settingsGroups();
    Screen createScreen(ScreenHost host);   // the app's own nav + content
}
```

`:shell` discovers implementations (an explicit generated list — no reflection, keeping startup cheap and R8-friendly), renders the hub, and routes deep links like `tictic://app/calculator`. Existing widget intents that pass `tab=clock` keep working via a compatibility mapping, so **no placed widget breaks**.

### 3.3 Customisable navigation (per the owner's requirement)

Three interchangeable nav modes stored in prefs, user-selectable at runtime:

- **Hub grid** — an app launcher grid; scales to 13+ apps; recommended default once more than ~6 apps ship.
- **Slim rail** — today's horizontal rail, for the user's pinned apps only.
- **Drawer** — a side list of all apps.

Plus per-user **reorder, pin and hide**, and "open straight into last-used app" vs "always open the hub". Because `:shell` owns nav and `SuiteApp` owns content, adding an app requires no nav code change.

### 3.4 User control over which apps exist — the defining requirement

"Keep everything, but the user decides what to have" cannot be satisfied by a settings toggle alone. A hidden app still ships its code, its permissions and its attack surface. Control has to be real, and it has three layers, adopted in this order:

| Layer | What the user controls | Mechanism | Cost | When |
|---|---|---|---|---|
| **L1 — Visible** | Reorder, pin, hide any app. Hidden apps do not appear in the hub, rail, drawer, search or shortcuts. | `:shell` prefs over the `SuiteApp` registry | trivial | **Phase 1** |
| **L2 — Present in the binary** | Choose a build that does not contain the heavy or networked apps at all. | Gradle **product flavours**: `full` and `lite`. `lite` excludes scanner, email, streaming, messaging — and therefore never declares their permissions or SDKs. | moderate: per-module `missingDimensionStrategy`, flavour-aware CI matrix | **Phase 2** |
| **L3 — Downloaded on demand** | Install an app only when they ask for it, and uninstall it again. Size and permissions follow. | **Play Feature Delivery** dynamic-feature modules, with `SplitInstallManager` in `:shell` and an "Apps" catalogue screen showing install/uninstall state | high: Play Console, on-demand delivery, split-APK testing, and a sideload story (dynamic features do not install from a plain APK) | **Phase 5+, once 3–4 apps exist** |

Design consequences that must be honoured from Phase 1, because retrofitting them is expensive:

- **No app may assume another app is installed.** Every cross-app touchpoint goes through an optional capability lookup in `:core-data` (`registry.find("expenses")` returning empty is a normal, handled case).
- **Permissions are declared per module**, so a flavour or split that omits a module also omits its permissions. Nothing may be hoisted into `:app`.
- **Every module must survive being absent at runtime** — including its widgets, deep links, shortcuts and settings entries. `:shell` renders only what is actually installed.
- **Data outlives the module.** Uninstalling an app must not silently delete its data; offer keep-or-delete explicitly, and keep the export/import bundle (§4) as the safety net.
- **Network use stays per-module opt-in**, extending the pattern TicTic already uses for per-widget weather.

This is also what makes the Tier C apps safe to keep: email, streaming and messaging ship as L3 modules that a user who does not want them never downloads, so their permissions, dependencies and policy exposure stay out of the default product.

---

## 4. Rebrand and data continuity

| Item | Decision | Consequence |
|---|---|---|
| Product name | **MySpace** — "One for many" | `strings.xml` `app_name` is currently `TicTic`; the clock keeps "TicTic" as its in-suite app label |
| `applicationId` | **`app.space.one`** | A new id means existing `app.tictic.clock` installs **do not upgrade in place** — they become a separate app. Safe right now: v1.11.2 is a *debug candidate with device QA pending*, i.e. no installed base to lose. This is the cheapest moment to rebrand; later it will cost users their data. |
| Version | **MySpace starts at 2.0.0**; the clock feature keeps its v1.11.2 lineage in CHANGELOG | Avoids confusing "clock v1.11.2" with "suite v1.11.2" |
| Data migration | The clock's `tictic_local_v1` SharedPreferences must be carried over | Ship an **export/import**: old app writes a JSON bundle via the document picker; MySpace imports it. Also keeps a rollback path. This must be built *before* the rebrand ships, or existing alarms/routines/usage are stranded. |
| Widgets | 24 providers move to `:core-widgets`; provider class names change package | Launcher-placed widgets are bound to the provider's component name — **changing the package orphans every placed widget**. Mitigation: keep an `activity-alias`/compat provider at the old component name, or accept re-placement and document it. Decide in Phase 1; do not discover it at release. |

---

## 5. Persistence strategy

Today: one SharedPreferences file with JSON strings. That is fine for ~50 alarms; it is wrong for notes, expenses and habits (thousands of rows, queries, sorting, pagination).

- Move collection data to **Room** in `:core-data` (SQLite under the hood, AndroidX, no server).
- Keep **SharedPreferences only** for settings/preferences, where it is the right tool.
- Write a **one-time migration** that reads the JSON arrays out of `tictic_local_v1` and inserts them into Room, then flags itself done — following the existing `migrateReminderDefaults` / `migrateRingingControls` idiom already in `DataStore`.
- Preserve the existing CSV export shape so the Usage report keeps working.

### 5.1 Toolchain — deliberate flexibility

The owner asked for scope of flexibility rather than a fixed toolchain decision, so this is handled as a **reversible step taken at the right time**, not a commitment now:

- **Now (Phase 0/1):** stay on AGP 7.4.2 + Gradle 7.6.4, and get CI green on the *existing* code first. A known-good baseline is worth more than a modern build.
- **When the first Room module lands:** bump to **AGP 8.x + Gradle 8.x + JDK 17** in its own isolated PR, so a toolchain failure is never tangled with a feature failure. `compileSdk`/`targetSdk` 34 stay; `minSdk 23` is preserved.
- **Language:** Java stays the default — all 2,771 existing lines are Java, and mixing in Kotlin prematurely doubles the review surface. New modules *may* use Kotlin where a library clearly favours it (Room/KSP, CameraX), decided per module rather than globally.
- **Dynamic features (L3, §3.4)** require a sufficiently modern AGP and Play Console setup — that is the hard floor under this flexibility, and it lands in Phase 5+.
- Every bump is validated by CI, since there is no local build. **Nothing is merged on "it compiles for me".**

---

## 6. Phased roadmap

**Phase 0 — build signal (must be first; nothing is verifiable without it).**
Add `.github/workflows/android-ci.yml`: Python contract suite + `assembleDebug` + APK artifact. Fix the `gradlew` executable bit. Get one green run on the current code, *before* any refactor, so we have a known-good baseline to diff against. Tag that green commit.

**Phase 1 — structure (no new user features; the enabling refactor).**
Rebrand to **MySpace / `app.space.one`** (name, id, icons, `README.md` + `PRODUCT.md` policy rewrite) · create the module skeleton · extract `:core-design`, `:core-data`, `:core-domain`, `:core-platform`, `:core-widgets` · move the clock into `:feature-clock` with **behaviour unchanged** · build the `SuiteApp` registry + `:shell` with the three nav modes, reorder/pin/hide (**L1 control**) · export/import data bundle · widget-component compatibility decision. Exit criteria: CI green, all 57 contract tests green (paths updated), clock indistinguishable from v1.11.2 on device, one widget round-trip verified.

**Phase 2 — prove the shell with three easy apps, and make control real.** Calculator · Notes/Tasks · Habits. Each is a separate module and a separate PR. Add the **`full` / `lite` product flavours (L2 control)** and the flavour-aware CI matrix. Introduce Room here (with the AGP/JDK bump as its own isolated PR, §5.1). Exit criteria: adding the third app touches **zero** lines in `:shell`, and a `lite` build verifiably omits a module and its permissions.

**Phase 3 — data-heavy offline apps.** Expenses (reuse CSV export) · Weather (promote the existing Open-Meteo work to a full app) · Recorder (RECORD_AUDIO).

**Phase 4 — media and storage.** Local music player (MediaSession + foreground service) · Files (SAF-scoped).

**Phase 5 — the hard apps, scoped down, plus on-demand delivery.** Local Calendar (+ optional `CalendarContract` sync) · Doc scanner (capture + crop to PDF, OCR as an opt-in Play-Services extra) · Writer (markdown + PDF/HTML export) · Spreadsheet (CSV-backed table editor). Stand up **Play Feature Delivery (L3 control)** and the Apps catalogue screen with install/uninstall state, now that enough modules exist to make it worthwhile.

**Phase 6 — Tier C, each as an off-by-default on-demand module.** Messages (in-app/local form first; carrier SMS only if Play approval is realistic) · Music streaming (local playback + user-supplied stream URLs; a commercial catalogue only with an agreement) · Email (vetted mail stack + the OAuth/Cloud verification workstream). Each needs its own privacy and policy review before it ships, and each is sequenced so that a failure to clear policy does not block the rest of MySpace.

---

## 7. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| No local build; CI is the only feedback loop | Slow iteration, blind refactors | Keep the Python contract suite green locally; make CI fast and run it on every push; prefer many small PRs over one big-bang refactor |
| **CI logs cannot be downloaded from this workspace** — `results-receiver`, `pipelines` and `objects.githubusercontent.com` are all unreachable | A failing step is hard to diagnose; cycles get wasted guessing | Print diagnostics inside the workflow (see "Report toolchain"); read step *conclusions* via `gh api .../jobs`; keep each change small so failures stay attributable |
| Phase 1 refactor silently breaks the clock | Regressing a working v1.11.2 | Move code without rewriting it; land `:feature-clock` before any new app; device QA gate before release |
| Widget component names change package | **Every placed widget on users' home screens breaks** | Compat providers / aliases decided in Phase 1, verified on a real launcher |
| Rebrand strands existing data | Lost alarms/routines | Export/import bundle shipped *before* the id change |
| AndroidX + AGP upgrade cascade | Build breakage across 15+ modules | Do the AGP/JDK/AndroidX bump in Phase 1 while there are still few modules |
| Google Play policy on SMS/CALL_LOG | **Whole listing rejected**, not just the messaging module | Carrier SMS ships only inside an on-demand module that is off by default and never holds the permission unless enabled (§3.4 L3). If Play approval proves unreachable, fall back to the in-app/local messaging form and keep carrier SMS as a separate standalone app. |
| Email/streaming licensing & OAuth compliance | Months of unplanned work, possible policy breach | Kept on the roadmap but sequenced last (Phase 6), each as an off-by-default on-demand module with its own privacy and policy review, so a failure to clear policy does not block the rest of MySpace. Budget the Cloud-project/OAuth verification and any catalogue licensing as separate workstreams. |
| `MainActivity` is 501 dense lines with everything inline | Extraction is delicate, not mechanical | Extract screens one at a time behind the registry, CI after each |
| Dynamic-feature (L3) modules do not install from a plain APK | Sideloaded builds lose the on-demand control layer | Keep the `full`/`lite` flavours (L2) as a supported path, and decide the distribution target before Phase 5 (§9.4) |
| Repo had one commit and no CI history | No safe point to roll back to | **Resolved** — Phase 0 is green and the pre-refactor state is tagged |

---

## 8. Immediate next step

**Phase 0 is complete.** CI is green on the current, unmodified product code:

- Workflow: `.github/workflows/android-ci.yml` — run [39323536015](https://github.com/tarungittarun/TicTic/actions/runs/39323536015), all 20 steps `success` in ~1m10s.
- Python source contracts: **57 tests passing** (both locally and in CI).
- `assembleDebug`: **passing** on `ubuntu-24.04`, JDK 17 (temurin), Gradle 7.6.4, AGP 7.4.2.
- Artifact `MySpace-debug-apk`: **555,498 bytes**.
- `lint`: **passing**, so it can be made blocking rather than `continue-on-error` once Phase 1 starts moving files.

Two fixes were needed to get there, both worth remembering:

1. `gradlew` was committed as mode `100644`, which breaks `./gradlew` on Linux runners. Now `100755`.
2. `android-actions/setup-android@v3` failed on its first attempt and is unnecessary — GitHub's runners already ship `cmdline-tools`, `platform-tools`, `platforms;android-34` and `build-tools` 34.x. Replaced with a tolerant `sdkmanager` gap-filler, and the runner is **pinned to `ubuntu-24.04`** so SDK contents do not drift with the `ubuntu-latest` migration to Ubuntu 26 (begins 2026-10-19).

Known follow-ups, non-blocking: `actions/setup-java@v4` is deprecated (move to v5); several actions still target Node 20.

Also note: **CI logs cannot be downloaded from the review workspace** — `results-receiver`, `pipelines` and `objects.githubusercontent.com` are all unreachable, so `gh run view --log` fails. The workflow therefore has an explicit "Report toolchain" step, and diagnostics should be printed rather than relied on from logs. Step *conclusions* are readable via `gh api .../jobs`.

The pre-refactor state is tagged so there is always a rollback point. Next: Phase 1 (§6), starting with the module skeleton and the `SuiteApp` registry — but the widget-compatibility decision (§9.1) should be settled first, because it constrains the package layout.

## 9. Open items

Decided by the owner on 2026-10-18: product name **MySpace** · `applicationId` **`app.space.one`** · multi-module · AndroidX · customisable navigation · **all requested apps kept, with user control over which are present** (§3.4) · toolchain kept flexible and bumped only when a module needs it (§5.1).

Still open:

1. **Widgets across the rebrand** — invest in compat providers/aliases so home-screen widgets survive the package change, or accept re-placement and document it? Must be settled in Phase 1.
2. **Doc-scanner OCR** — accept a Play-Services/ML Kit dependency in an on-demand module, or ship crop-to-PDF only? Settled in Phase 5, but it affects the `lite` flavour definition.
3. **`myspace` branch** — it does not exist on `origin`; the only remote branch is `origin/main`, and the session branch `arena/3caca636-tictic` points at the same commit (`26eca2f`). This session is pinned to `arena/3caca636-tictic`, so confirm how `myspace` should be created and kept in sync.
4. **Distribution target** — Google Play (required for L3 on-demand delivery) and/or direct APK. L3 does not work from a plain sideloaded APK, so if sideloading matters the `full`/`lite` flavours (L2) must remain a supported path.
5. **Privacy policy and Play data-safety form** — mandatory before any networked module ships, and a prerequisite for the email OAuth work.
