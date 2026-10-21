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

Decided 2026-10-19, closing the Phase 1 blockers:

7. **Widgets: accept one-time re-placement** across the rebrand. No compat providers (§4).
8. **Distribution: both Google Play and direct APK.** L3 on-demand modules are real, *and* the `full`/`lite` flavours must be first-class because a sideloaded APK cannot install dynamic features (§3.4).
9. **`minSdk` raised 23 → 26** (Android 8.0) to unlock CameraX/ML Kit and modern AndroidX (§5.1).
10. **Branching: work stays on `arena/3caca636-tictic`.** `origin/myspace` is *not* created by this session; the owner will create it if wanted (§9).

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
                            The ONLY module allowed to see :shell and every
                            :feature-*, because it assembles the registry.
:core-suite               → the contract between an app and the chrome:
                            SuiteApp, Screen, ScreenHost, SettingsGroup.
                            Interfaces and value types only.
:shell                    → implements that contract: SuiteRegistry, the three
                            navigation modes, and the user's ordering/pinning/
                            hiding (control layer L1). No app identity hardcoded.
:core-design              → palette, accents (the existing 16), glass presets,
                            AppGlassDrawable, GelMotion, AppSurfaceStyle,
                            TicIconView, AnalogClockView, Palette,
                            typography/corner/border/elevation/spacing scales,
                            press styles, Reduce-motion.
:core-data                → prefs access, theme/appearance, usage counters,
                            activity history, time formatting; later the Room
                            database, the JSON migration from tictic_local_v1,
                            and CSV export.
:core-domain              → pure JVM rules with no Android imports: TimeMath and
                            NavOrder. Unit-testable, which matters because there
                            is no emulator in this workspace.
:core-platform            → permissions flows, exact-alarm/notification/full-screen
                            preflight, boot/time-change rescheduling, power-saver,
                            foreground-service base.
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

Dependency rule: `:feature-* → :core-*`, `:shell → :core-*`, `:app → :shell + all :feature-*`. **Feature modules never depend on each other, and never on `:shell`.** Core modules may depend on other core modules (`:core-design → :core-data`, `:core-suite → :core-design + :core-domain`) but never upward on `:app`, `:shell` or `:feature-*`. Cross-app data flows through `:core-data` interfaces only — that is what keeps "add app N" cheap instead of a refactor each time.

**Why the contract is not in `:shell`.** This corrects the original graph, which had no `:core-suite`. Features must implement `SuiteApp`, so if that interface lived in `:shell` every feature would depend on the shell — coupling each app to the navigation *implementation*. Navigation is user-customisable across hub, rail and drawer, so the shell is exactly the part that will change. Splitting the contract out means the shell's rendering can be rewritten without one app module recompiling. Both sides implement/serve `:core-suite`; only `:app` sees both. Enforced by a contract test that fails if any module other than `:app` declares `project(':shell')`.

### 3.2 The app-registry contract (the piece that makes this scale)

Today's navigation is a hardcoded array inside a 501-line Activity. That is the single biggest blocker. Replace it with a declaration each module contributes. **As built in step 1g:**

```java
// :core-suite — the contract. Interfaces only, no implementation, no preferences.
public interface SuiteApp extends NavOrder.Entry {
    @Override String id();       // stable, comma-free, e.g. "clock", "calculator"
    @Override String label();    // "Calculator"
    @Override int defaultOrder();// position before the user rearranges
    String tagline();            // one line for the hub tile
    int iconGlyph();             // a TicIconView constant — drawn locally, no icon pack
    boolean supportsWidgets();   // surfaces a Widget Studio entry
    boolean acceptsRoute(String route);        // can this app open "timer"?
    List<SettingsGroup> settingsGroups();      // contributes Settings categories
    Screen createScreen(ScreenHost host, String route);
}
```

Three differences from the original sketch, each for a reason:

- **`int iconGlyph()`, not `Drawable icon()`.** `TicIconView` in `:core-design` already draws a consistent bold-line icon set locally, and the product promise is no third-party art. Suite apps add glyphs there rather than each shipping vector assets.
- **`acceptsRoute(String)` plus a `route` argument on `createScreen`.** The clock's existing widget intents already route to `alarms`, `reminders`, `timer`, `stopwatch`, `routines` and `usage`. Routes are strings rather than an enum so a new app defines its own destinations without editing the contract module.
- **`extends NavOrder.Entry`.** Lets `:shell` pass registered apps straight into the pure ordering rules in `:core-domain`, which are unit-testable. `@Override` on the three shared members makes the compiler catch signature drift instead of silently declaring unrelated methods.

`:app` — not `:shell` — assembles the explicit list of apps present in the build and hands it to `SuiteRegistry`. No reflection or classpath scanning, so startup stays cheap, R8 keeps working, and the `full`/`lite` flavours differ by *which apps are added to the list* rather than by runtime checks. `SuiteRegistry` validates ids at construction (null, empty, comma, duplicate), because a bad id would corrupt the comma-joined nav preferences and surface much later as the wrong app opening.

Deep links resolve by asking the registry which app `acceptsRoute`, so the chrome never hardcodes an app's id or destination names — enforced by a contract test that fails if any app-id literal appears in `:shell`. Note this preserves *intent routing* only; placed widgets still need one-time re-placement because their provider component names change package (§4, owner decision 7).

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
| **L3 — Downloaded on demand** | Install an app only when they ask for it, and uninstall it again. Size and permissions follow. | **Play Feature Delivery** dynamic-feature modules, with `SplitInstallManager` in `:shell` and an "Apps" catalogue screen showing install/uninstall state | high: Play Console, on-demand delivery, split-APK testing | **Phase 5+, once 3–4 apps exist** |

**Distribution is both Google Play and direct APK (owner decision).** That makes L3 real *and* makes L2 mandatory rather than a fallback: a dynamic-feature split cannot be installed from a plain sideloaded APK, so the `full`/`lite` flavours must remain a supported, tested, first-class path — not a build curiosity. CI therefore builds a **matrix of flavours**, and the `lite` flavour is the one that ships as a direct APK. Practical consequences: a Play Console account (one-time fee) is needed before Phase 5; Play App Signing is required, so the existing debug-only artifact flow must gain a release signing path; and a privacy policy plus the Play data-safety form are prerequisites for the *first* networked module, not for the Play listing deadline generally.

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
| Widgets | **Decided: accept one-time re-placement.** 24 providers move cleanly to `:core-widgets`; no compat providers or aliases | Launcher-placed widgets bind to the provider's component name, so the package change orphans them. Accepted because there is no install base (v1.11.2 is an unreleased debug candidate) *and* the new `applicationId` already prevents in-place upgrade — so widgets would need re-placing regardless. **Must be stated in the release notes and in the export/import flow**, not discovered by users. |

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
- **`minSdk` is raised from 23 to 26** (Android 8.0) — owner decision. Rationale: CameraX and ML Kit document scanning (needed for the doc scanner) and much of modern AndroidX assume 24+; 26 additionally guarantees notification channels and adaptive icons. The cost is device reach on Android 6/7, which is acceptable because there is no install base yet.
  - **Known consequence, deliberately deferred:** 36 existing `SDK_INT` guards become dead code at minSdk 26 — 18× `>=26`, 13× `>=23`, 2× `>=24`, 1× `>=21`. These are *not* stripped as part of the bump, because they sit inside behaviour-sensitive paths (notification channel creation, volume-key dispatch, wallpaper colours at `>=27`, exact-alarm at `>=31`, `POST_NOTIFICATIONS` at `>=33`, full-screen intent at `>=34`). Guards at `>=27` and above remain live and must stay. Dead-guard removal is a separate, individually CI-verified cleanup after `:feature-clock` lands.
- **Language:** Java stays the default — all 2,771 existing lines are Java, and mixing in Kotlin prematurely doubles the review surface. New modules *may* use Kotlin where a library clearly favours it (Room/KSP, CameraX), decided per module rather than globally.
- **Dynamic features (L3, §3.4)** require a sufficiently modern AGP and Play Console setup — that is the hard floor under this flexibility, and it lands in Phase 5+.
- Every bump is validated by CI, since there is no local build. **Nothing is merged on "it compiles for me".**

---

## 5.9 Confirmed scope, measured position, and the obligations that come with it

**Owner decision, 2026-10-21: full 13-app scope, accepting the timeline.** All thirteen
requested apps stay in scope — Calculator, Notes/Tasks, Calendar, Expense tracker, Habit
tracker, Weather, Email, Music streamer, Files, Messages, Recorder, Doc scanner, and the
doc/spreadsheet writer — along with customisable navigation, the rebrand, and both
distribution channels. This reconfirms the earlier decision to keep every app rather than
defer or substitute any; it is not to be re-litigated.

Measured at commit `0aed793`: 39 elapsed hours, 26 CI-verified slices, 29 CI runs (28
green), and 23% of the codebase extracted out of `:app` into eight modules. Against the
full scope the honest position is that **12 of the 13 apps do not exist**, and the one that
does is ~20% migrated — its domain rules moved to `:feature-clock`, but 0 of 6 screens
have, so it still renders from the legacy `MainActivity`. Phase 1 is roughly half complete.
Completing everything is on the order of **165–340 slices**; a shippable v1 (clock
migrated, navigation working, plus Calculator/Notes/Habits with `full`/`lite` flavours) is
**~70–110**.

**Three of these the owner must carry, and no amount of code resolves them:**

- **Email.** Gmail restricted scopes require OAuth verification against a Cloud project, a
  published privacy policy, and possibly a security assessment.
- **Music streamer.** Commercial catalogues require partner agreements or licensed SDKs.
  Only local playback and user-supplied stream URLs are buildable without them.
- **Carrier SMS.** Google Play restricts SMS and Call Log permissions to apps whose *core
  function* is SMS. MySpace would very likely be rejected; this needs a Play policy
  declaration, or the feature ships only through the direct-APK channel.

Also outstanding and owner-side: whether a funded Play Console account exists, the privacy
policy, and the Play data-safety form.

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

## 6.1 Phase 1 progress

| Step | State | Evidence |
|---|---|---|
| **0 — CI build signal** | ✅ done | [run 39323536015](https://github.com/tarungittarun/TicTic/actions/runs/39323536015); tagged `baseline-pre-myspace-refactor` |
| **1a — rebrand + `minSdk 26`** | ✅ done | [run 39367315293](https://github.com/tarungittarun/TicTic/actions/runs/39367315293), APK 544,051 B. `applicationId app.space.one`, `versionName 2.0.0`, label MySpace. `namespace` deliberately unchanged (§9.5) |
| **1b — `:core-domain` extraction** | ✅ done | [run 39369552158](https://github.com/tarungittarun/TicTic/actions/runs/39369552158), all 23 steps green. `rootProject.name = 'MySpace'`, `include ':core-domain'`, `TimeMath` → `app.space.core.domain` and `public`, two consumers re-imported, 64 contract tests |
| **1c — `:core-design` (part 1)** | ✅ done | [run 39375464088](https://github.com/tarungittarun/TicTic/actions/runs/39375464088), all steps green, APK 546,586 B. First **Android library** module: `com.android.library`, namespace-only (no manifest), builds fine with `useAndroidX=false`. Holds `TicIconView` only. 68 contract tests |
| **1d-1 — `:core-data` + theme primitives** | ✅ done | [run 39376190362](https://github.com/tarungittarun/TicTic/actions/runs/39376190362), all steps green, APK 547,655 B. `AppTheme` holds `themeMode`/`dark`/`ACCENT_COLORS`/`accent`; `DataStore` delegates. **73 contract tests** |
| **1d-2 — `:core-data` generic storage** | ✅ done | [run 39387424120](https://github.com/tarungittarun/TicTic/actions/runs/39387424120), all steps green, APK 549,426 B. `AppPrefs` (`prefs`/`array`/`putArray`/`find`/`PREF`), `UsageTracker` moved whole, `AppActivityLog` (`log`). **76 contract tests** |
| **1c-2a — `GelMotion` + `AnalogClockView`** | ✅ done | [run 39389088856](https://github.com/tarungittarun/TicTic/actions/runs/39389088856), all steps green, APK 551,203 B. First **library-to-library** dependency (`:core-design` → `:core-data`). **77 contract tests** |
| **1c-2b — glass trio** | ✅ done | [run 39557877405](https://github.com/tarungittarun/TicTic/actions/runs/39557877405), all steps green, APK 551,668 B. `GlassPreset` + `AppSurfaceStyle` + `AppGlassDrawable`. **Design system extraction complete.** **79 contract tests** |
| **1c/1d — `:core-design` + `:core-data`** | ✅ **complete** | `:core-design` holds all 6 design classes (icons, analog face, motion, presets, surfaces, renderer); `:core-data` holds 4 (theme, prefs, usage, activity log). `:app` is down from 37 Java files to **28** |
| **1d-3 — `:core-data` formatting** | ✅ done | [run 39559422583](https://github.com/tarungittarun/TicTic/actions/runs/39559422583), all steps green. `AppFormat` holds `formatTime`/`formatDate`; 12 call sites untouched via the facade. **80 contract tests** |
| **1g — `:core-suite` contract + `:shell` chrome** | ✅ done | [run 39561312731](https://github.com/tarungittarun/TicTic/actions/runs/39561312731), all steps green. 6 modules. Interfaces only, no implementations, no existing product code touched. `NavOrder` + 12 JVM unit tests in `:core-domain` (report artifact 6,159 → 7,520 B confirms they executed). **89 contract tests** |
| **1h-i — `UiKit` drawing primitives** | ✅ done | [run 39563925916](https://github.com/tarungittarun/TicTic/actions/runs/39563925916), all steps green, APK 560,207 B. `dp`/`vbox`/`hbox`/`lp`/`label`/`shape`/typeface moved to `:core-design`; `MainActivity` keeps one-line delegates (net 18 lines changed); `applyPalette` now resolves a `Palette`, so the ten theme literals are single-sourced. **91 contract tests** |
| **1h-ii — finish the design-system extraction** | ✅ done | [run 39574265239](https://github.com/tarungittarun/TicTic/actions/runs/39574265239), all steps green, APK 560,951 B. `card`/`pillButton`/`sectionTitle`/`gap`/glass/elevation/`pageBackground` moved to `UiKit`; `sectionTitle`/`gap` now take a parent. Unified a **four-way** colour-blend duplication into `Palette.blend`. **92 contract tests** |
| **1d-4 — clock domain into `:feature-clock`** | ✅ done | [run 39588177143](https://github.com/tarungittarun/TicTic/actions/runs/39588177143), all steps green, APK 563,429 B. First app module. `AlarmRules` + `RingingControls`; `DataStore` now pure delegation. **13 JVM unit tests** on the recurrence rule (report artifact 7,520 → 20,524 B confirms they ran). **95 contract tests** |
| **1h-iii — `ClockApp` declared** | ✅ done | [run 39589555708](https://github.com/tarungittarun/TicTic/actions/runs/39589555708), all steps green, APK 564,165 B. The contract's first implementor. `ROUTES` parity with `applyRoute` asserted in both directions and verified non-vacuous. Nothing wired into `MainActivity`, so behaviour cannot change. **98 contract tests** |
| **1h-iv·1 — `StopwatchScreen`, the first real `Screen`** | ✅ done | [run 39601731931](https://github.com/tarungittarun/TicTic/actions/runs/39601731931), all steps green, APK 570,618 B. Implements `Screen`, owns its own ticker at the four original rates, reaches the host only through `ScreenHost`. Returned by `ClockApp.createScreen` for the `stopwatch` route. Not wired into `MainActivity`, so behaviour cannot have changed. **102 contract tests** |
| **1h-iv·2 — stopwatch cutover** | ✅ done | [run 39602977308](https://github.com/tarungittarun/TicTic/actions/runs/39602977308), all steps green, APK 570,232 B. **`MainActivity implements ScreenHost` and tab 3 renders through `StopwatchScreen` — the first screen driven by the contract.** Deleted the 2,267-char legacy duplicate and the `nextTickDelay` tab-3 branch. **102 contract tests** |
| **1h-iv·3 — move `TicAlertBuilder` to `:core-design`** | ⬜ **next; blocks four of the five remaining screens** | A 1,740-char **private inner class of `MainActivity`**, and every editor dialog in the clock goes through `alertBuilder()` — **22 call sites**: `showReminderEditor` 4, `showAlarmEditor` 3, `showRoutineEditor` 2, `buildActivity` / `showAlarmDefaultsEditor` / `chooseTimerLength` 1 each. It is design-system code (builds its title row from `hbox`/`label`/`dp` and the `text`/`muted` palette colours), so `:core-design` is its home. Moving it unblocks Timer, Routines, Reminders and Usage **at once** — doing it per screen would mean hitting the same wall four times |
| **1h-iv·4 — remaining five screens** | ⬜ | Then Clock, Timer, Routines, Reminders, Usage — one CI-verified screen at a time. Routines and Reminders also need the `ScreenHost` refresh channel for the schedulers and `WidgetEngine.refreshAll`, and `ScreenHost` still has no `toast()` |
| **1h-v — settings groups** | ⬜ | ~12 groups move to `SettingsGroup`, so a suite Settings page renders the clock's categories |
| 1d-5 — Room + JSON migration + CSV export | ⬜ | the actual storage upgrade (§5), needs the AGP/JDK bump (§5.1) |
| **1e·1 — `:core-platform` created** | ✅ done | [run 39595590343](https://github.com/tarungittarun/TicTic/actions/runs/39595590343), all steps green, APK 565,112 B. `SystemBoot.count` moved out of `TimerReceiver`, which was the last `:app`-only dependency blocking the stopwatch screen. Receiver keeps a delegate, so its three call sites and `MainActivity`'s two are untouched. **99 contract tests** |
| **1e·2 — power-saver rule unified** | ✅ done | [run 39598731947](https://github.com/tarungittarun/TicTic/actions/runs/39598731947), all steps green, APK 565,348 B. `PowerState.isOptimizing` replaces three identical copies in `MainActivity`, `WidgetEngine` and `AnalogClockView`; all three delegate, eleven call sites untouched. **The last prerequisite before a `Screen` can be written.** **100 contract tests** |
| 1e·3 — rest of `:core-platform` | ⬜ | permissions preflight, boot/time rescheduling, usage |
| 1f — `:core-widgets` | ⬜ | widget renderer, 24 providers, Widget Studio |
| 1g-2 — nav rendering in `:shell` | ⬜ | the hub/rail/drawer views themselves, plus the reorder/pin/hide UI. Follows 1h so there is a real app to render |
| 1i — export/import bundle | ⬜ | must ship before the id change reaches users |
| 1j — dead `SDK_INT` guard cleanup | ⬜ | 36 guards, deliberately deferred (§5.1) |

**What blocked a screen from moving, and what still does (revised after 1d-4).** 1h-i found four helpers entangled with the Activity; 1h-ii moved all of them, so the design system stopped being the blocker. Measuring again pointed at data access instead: `MainActivity` makes **140 `DataStore.` calls** and `DataStore` lives in `:app`, which a `:feature-clock` module may never depend on — the §3.1 rule, enforced by test. Moving screens first would have meant either inverting that rule or rewriting 140 call sites blind, with no local compiler to catch a mistake.

**1d-4 resolved this.** `:feature-clock` now exists and holds the clock's own rules, so a screen can reach them; and everything else a screen needs — preferences, collections, activity log, formatting, theme — was already in `:core-data` and `:core-design`, which a feature may depend on. `DataStore` is pure delegation and holds no logic, so those 140 call sites still compile unchanged.

That reversal is worth keeping visible: this document previously claimed **1d-4 cannot precede 1h**, reasoning that the clock domain logic belongs in `:feature-clock` and that module did not exist. The premise was right and the conclusion was backwards — creating the module *first* is what made the screens movable at all. The superseded wording is quoted here rather than quietly deleted so the change of mind is auditable.

**One coupling that is still architectural (measured in 1h-iv).** A feature module may reach `:core-*` but never `:app`, and three of the classes a screen needs when it *persists* data live in `:app`: `WidgetEngine`, and the `AlarmScheduler` / `ReminderScheduler` / `RoutineReceiver` schedulers. Direct coupling by tab:

| tab | `:app`-only classes touched directly |
|---|---|
| `buildClock` | `DataStore` only |
| `buildTimer` | `DataStore` only |
| `buildStopwatch` | `DataStore` only |
| `buildActivity` | `DataStore` only |
| `buildReminders` | `DataStore`, `ReminderScheduler` |
| `buildRoutines` | `DataStore`, `RoutineReceiver` — and `saveRoutines()` calls `WidgetEngine.refreshAll` |

`DataStore` is not a blocker: it is pure delegation into `:core-data` and `:feature-clock`, both reachable. The schedulers and `WidgetEngine` are, so either **1e (`:core-platform`) and 1f (`:core-widgets`) must precede those two screens**, or `ScreenHost` grows one generic post-persistence refresh channel that the host implements by rescheduling and refreshing widgets. The second is the better fit for the contract — `ScreenHost` is already defined as the only channel from app to chrome, and "my data changed, update anything bound to it" is a need every widget-backed app will have, not a clock special case. It should be added when the first such screen actually needs it, not before.

This is direct coupling only; the transitive closure per screen still needs checking, since `buildTimer` reaches `requestTimerStart`/`cancelTimer` which likely touch `TimerReceiver`. **Stopwatch is the strongest candidate for the first move**: pure in-memory state plus haptics, no scheduler, no widget refresh, no editor dialog. Two facts measured since fix its prerequisites exactly:

- **The tick cadence is 16 ms.** `nextTickDelay()` returns 16 ms for the stopwatch while running — roughly 60 fps, which the hundredths display needs — 100 ms under battery saver, and 1 s when paused. A `Screen` owns its own ticker through `onResume`/`onPause`, so it must reproduce all three rates or the display either stutters or drains the battery.
- **Battery-saver state is still an `:app` method.** `batterySaverOptimization()` gates that cadence, and power-saver state is already assigned to `:core-platform` in 1e. So that read must move first — one more small slice, and the last one before a screen can be written.

What remains besides that is per-screen rather than architectural:

- **Screen state and lifecycle.** Each tab keeps its own state in `MainActivity` fields (`routines`, `swElapsed`, `timerRunning`) and shares one `ticker`/`handler`. A `Screen` must own its state and drive it from `onResume`/`onPause`, which the contract already provides.
- **Editor dialogs travel with their screen.** `showRoutineEditor` alone is ~5 KB and is called from the Routines tab; it moves with it rather than staying behind.
- **Two small contract gaps.** `appSwitch()` is still an Activity-local control builder, and `ScreenHost` offers `invalidate()` for re-render but no `toast()` for transient messages. Both are cheap to add when the first screen needs them, and it is better to grow the contract from a real consumer's requirement than to guess at it now.

Also noted in 1h-ii and deliberately left alone: `AlarmActivity.ringingBackground()` rebuilds the same gradient as `AppSurfaceStyle.pageBackground()` but with `.20f` fixed where app pages use `dark?.21f:.12f`, so in light mode the ringing screen carries a visibly stronger accent tint. That may well be intentional — an alarm screen arguably should be more vivid — so it needs a visual decision on a device, not a silent unification inside a refactor whose contract is that behaviour does not change. The same applies to eight hardcoded dark-mode field fills in `MainActivity`.

**Why 1c was narrowed, and why 1d became the critical path.** The plan originally listed `:core-design` before `:core-data`. Reading the sources showed that is backwards: every design class except `TicIconView` reads theme/accent/preference state through `DataStore`, which lived in `:app`. Extracting them first would have made `:core-design` depend on `:app` and inverted the §3.1 rule. So 1c shipped the one genuinely decoupled class and proved the Android-library module type, and the glass/motion system waited for `:core-data`.

The enforcing test had to change once `:core-data` existed, and that is worth recording: it originally forbade *any* reference to the legacy store in `:core-design`, which was correct while `:core-data` was absent and wrong afterwards, since depending on it is then the whole point. It now asserts the real rule — `:core-design` must use `:core-data` and declare that dependency, and must never reach past it into `:app`.

**`DataStore` is the hard knot in 1d.** Measured: **452 references across 25 files**, 144 of them in `MainActivity` alone. By method — `prefs` 182, `dark` 81, `log` 46, `array` 33, `accent` 24, `putArray` 18, `formatTime` 14, `nextOccurrence` 11, `find` 9, then the clock-specific tail. About fifteen contract tests assert on `DataStore.java` content *and* on `DataStore.xxx(...)` call-site strings in other files.

It also mixes two concerns that belong in different modules: theme/settings/storage access (`prefs`, `dark`, `themeMode`, `accent`, `array`, `putArray`, `find`, `log`, `formatTime`, `formatDate`) belongs in `:core-data`, while the clock domain logic (`nextOccurrence`, `daysLabel`, `nextAlarmText`, `alarmVolumeAction`, `showRingingSnoozeAction`, and both migrations) belongs in `:feature-clock`.

**Strategy: facade, not rewrite.** Each group's implementation moves to `:core-data` while `DataStore` keeps a thin delegating method, so **no call site changes** and the existing contract assertions stay valid. Call sites are rewritten only after every group has moved. This keeps each slice small enough to be attributable in a single CI run — which matters because CI is the only compiler available and each iteration costs ~2 minutes.

**The most dangerous constant in this work** is the preference file name `tictic_local_v1`. `AppTheme` reads `SharedPreferences` directly and so carries its own copy. If the two literals ever diverge, appearance settings get read from a different file than `DataStore` writes — silently stranding every user's settings on upgrade, with no crash to reveal it. A contract test now compares both literals and pins the value.

**1b is the load-bearing proof.** `TimeMath` was chosen as the wedge because it is pure JVM and has only two consumers — so it exercises the entire chain (new module → `settings.gradle` → cross-module dependency → public API across a package boundary → moved test → repointed contracts) at the smallest possible blast radius. If the module graph were misconfigured, this is where it would show, cheaply.

**A real gap was found and fixed while doing it:** `assembleDebug` does *not* run Gradle `test` tasks, so the JVM regression suite had **never executed in CI** — `TimeMathTest` was passing locally and being silently skipped on every push. CI now runs `testDebugUnitTest test` as its own step and uploads the reports. Given there is no local Android build, that suite is the only place those assertions meet real compiled classes.

**Ordering note.** 1c–1f extract shared code that `:feature-clock` still depends on, so the clock keeps compiling throughout; 1g introduces the registry the shell needs; 1h moves the clock last, once everything it uses is already in a `:core-*` module. Each step is one CI-verified commit — never a big-bang rename.

**Where this leaves the sequence.** 1c and 1d-1/1d-2 are done, so `:core-domain`, `:core-design` and `:core-data` all exist and the dependency rule holds in both directions — enforced by tests, not convention. Two things are now genuinely blocked and worth stating rather than discovering later:

- **1d-4 must precede the screen moves** *(this bullet previously said the opposite — corrected after 1h-ii measured 140 `DataStore.` calls in `MainActivity`)*. The clock domain logic in `DataStore` (`nextOccurrence`, `daysLabel`, `nextAlarmText`, the ringing-control accessors, both migrations) belongs in `:feature-clock`. Parking it in `:core-data` would put clock recurrence rules in the shared data layer and make every future app depend on them. So: create `:feature-clock`, move the domain logic in, then move screens. See the revised blocker note above.
- **1g and 1h are mutually entangled.** The shell needs screens to host; today one 501-line `MainActivity` renders all six tabs and the whole settings surface inline. Introducing `SuiteApp` first means the clock has to implement it from inside `:app`; extracting the clock first means moving screens with no registry to move them into. The resolution is to define `SuiteApp` and `ScreenHost` in `:shell` **as an interface with no implementations**, then extract the clock into `:feature-clock` as the first and only implementor — so the interface is designed against a real consumer rather than speculated, and the clock is never without a home.

That makes **1g-then-1h, interleaved**, the next real work, with 1d-3 (`formatTime`/`formatDate`) as a small unblocking slice. 1e (`:core-platform`) should follow 1h, because the permission-preflight and boot-rescheduling code is currently tangled with clock scheduling and separating them is cleaner once the clock has its own module.

## 7. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| No local build; CI is the only feedback loop | Slow iteration, blind refactors | Keep the Python contract suite green locally; make CI fast and run it on every push; prefer many small PRs over one big-bang refactor |
| **CI logs cannot be downloaded from this workspace** — `results-receiver`, `pipelines` and `objects.githubusercontent.com` are all unreachable | A failing step is hard to diagnose; cycles get wasted guessing | Print diagnostics inside the workflow (see "Report toolchain"); read step *conclusions* via `gh api .../jobs`; keep each change small so failures stay attributable |
| Phase 1 refactor silently breaks the clock | Regressing a working v1.11.2 | Move code without rewriting it; land `:feature-clock` before any new app; device QA gate before release |
| Widget component names change package | Placed widgets are orphaned by the rebrand | **Accepted and decided**: no install base exists, and the new `applicationId` already blocks in-place upgrade, so widgets need re-placing regardless. Must be stated in the release notes and inside the export/import flow. |
| Rebrand strands existing data | Lost alarms/routines | Export/import bundle shipped *before* the id change |
| AndroidX + AGP upgrade cascade | Build breakage across 15+ modules | Bump only when a module needs it (§5.1), in its own isolated CI-verified PR |
| Supporting **both** Play and direct-APK distribution | Two delivery paths to build, sign and test; L3 cannot be relied on alone | CI builds a flavour matrix; `lite` is the sideloadable path and is first-class, not a fallback. Play App Signing and a release signing path are added before Phase 5. |
| Google Play policy on SMS/CALL_LOG | **Whole listing rejected**, not just the messaging module | Carrier SMS ships only inside an on-demand module that is off by default and never holds the permission unless enabled (§3.4 L3). If Play approval proves unreachable, fall back to the in-app/local messaging form and keep carrier SMS as a separate standalone app. |
| Email/streaming licensing & OAuth compliance | Months of unplanned work, possible policy breach | Kept on the roadmap but sequenced last (Phase 6), each as an off-by-default on-demand module with its own privacy and policy review, so a failure to clear policy does not block the rest of MySpace. Budget the Cloud-project/OAuth verification and any catalogue licensing as separate workstreams. |
| `MainActivity` is 501 dense lines with everything inline | Extraction is delicate, not mechanical | Extract screens one at a time behind the registry, CI after each |
| Dynamic-feature (L3) modules do not install from a plain APK | Sideloaded builds lose the on-demand control layer | Keep the `full`/`lite` flavours (L2) as a supported path, and decide the distribution target before Phase 5 (§9.4) |
| Repo had one commit and no CI history | No safe point to roll back to | **Resolved** — Phase 0 is green and the pre-refactor state is tagged |
| **The review workspace periodically loses local git state** — observed three turns running: HEAD reverts to `26eca2f` while the working tree keeps the edits, and remote-tracking refs disappear | A naive commit squashes already-pushed work and diverges from the remote; a force-push would then destroy it | Treat `origin` as authoritative. At the start of each session run `git fetch` and compare `HEAD` against `origin/arena/3caca636-tictic`; if they differ, `git reset --hard origin/<branch>` after confirming the working tree adds nothing new. **Never force-push.** Commit and push every slice immediately so the remote is never behind |
| **`git checkout --` on a `git mv`'d file whose edits are unstaged** silently restores the *pre-move* content | Hit during 1c-2b: it reverted `AppGlassDrawable` to `package app.tictic.clock` with its legacy store refs, undoing the move | Never `git checkout --` a file whose changes exist only in the working tree; restore from an explicit backup copy instead. The contract tests caught this immediately — which is the argument for asserting the module boundary in code rather than trusting a clean run |
| Visibility edits applied by script can miss overloads | A `replace(..., 1)` left two of three `AppSurfaceStyle.glass()` overloads package-private; `MainActivity` calls the 8-arg one, so this was a compile error caught only by inspection | Assert the *count* of public overloads in a contract test, and inspect scripted edits rather than trusting them. Both new tests were verified non-vacuous by deliberately breaking the code and confirming they fail |

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

**All Phase 1 blockers are now closed.** Decided by the owner across 2026-10-18 and 2026-10-19: product name **MySpace** (`app.space.one`, "One for many") · multi-module Gradle · AndroidX · customisable navigation · all requested apps kept with three layers of user control (§3.4) · toolchain kept flexible (§5.1) · **widgets: accept one-time re-placement** · **distribution: both Google Play and direct APK** · **`minSdk` 23 → 26** · **branching: work stays on `arena/3caca636-tictic`**; `origin/myspace` is not created by this session.

Still open, but none of these block Phase 1:

1. **Doc-scanner OCR** — accept a Play-Services/ML Kit dependency in an on-demand module, or ship crop-to-PDF only? Needed before Phase 5; it also affects how the `lite` flavour is defined. `minSdk 26` removes the main technical obstacle.
2. **Per-app product specs do not exist.** Nothing in the repository specifies the calculator, notes, habits, expenses, recorder, scanner, writer or spreadsheet behaviour. These are written **just-in-time**, one short product note per app immediately before its phase — speculating on a spreadsheet editor before the shell exists would produce documents that are wrong. Only the **platform contract** (§3) needed specifying up front, and it is written.
3. **Branding assets do not exist.** The icon is a single vector `ic_app.xml` (TicTic), with no `mipmap-*` directories and **no adaptive icon**. MySpace needs a launcher icon (with adaptive-icon support, now guaranteed available at `minSdk 26`) plus up to 13 in-suite app icons.
4. **Play Console prerequisites** — developer account, Play App Signing, privacy policy and the data-safety form. Required before the first networked module ships, not before Phase 1.
5. **Interim inconsistency, by design.** Until `:feature-clock` is extracted, the About screen still reads "TicTic · version …" and the 24 widget providers still live in `app.tictic.clock`. Only the launcher label and `applicationId` change now. This is intentional: keeping the Java `namespace` at `app.tictic.clock` means R references and the manifest's relative component names stay valid, so the rebrand lands as a small CI-verifiable change instead of a big-bang package rename.
