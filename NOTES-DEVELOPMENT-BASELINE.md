# MyNotes — approved scope & development baseline

Date: 2026-10-27. Status: planning baseline; implementation and device qualification pending.

This document supersedes conflicting proposals in NOTES-PLAN.md and the visual overview. Read those for the original 100-feature inventory and diagram boards. No feature is considered delivered merely because it appears in a design or passes a source-contract check.

## 1. Owner decisions — binding

- Standalone MyNotes first. A single reusable Notes implementation must later integrate into MySpace; no parallel copy or standalone-only data model.
- Microphone permission is optional, requested only when recording. Denied, permanently denied, or revoked permission must not block any other feature. Playback and transcription of existing/imported audio do not need microphone permission.
- OCR and automatic voice transcription are required and **offline only**. No cloud fallback, remote model request or account. Ship the required baseline models locally; no first-use download dependency.
- Instant search runs off the UI thread and updates results in place. Do not silently replace it with submit-search.
- Trash retention: **7 days**, one policy constant. Delete moves to Trash with Undo; permanent deletion is a distinct destructive operation.
- Wall drag-reordering ships last, not omitted.
- All user data is local. Users can choose the directory where their library is saved.
- APK size is not a constraint. Memory, latency, battery, disk space and reliability still are.
- Include the diagram/flowchart scope on PDF pages 16–17. This extends, not replaces, the original 100 features.
- No calculator work, no tests.yml reruns, no unrequested shared-app rewrites.

## 2. Definition of the product

Deliver the full original inventory (NOTES-PLAN.md items 1–100), diagram suite, offline speech, folder-based storage, export/import and recovery. Account sign-in, Google backup/cloud collaboration and Wear OS remain outside the agreed scope. Local backup is NOT excluded by the Google-backup exclusion.

The visual overview remains the design reference: Home wall, drawer, text/list editors, capture, image/OCR/drawing, voice, labels/search, multi-select/merge, reminders, Trash, widgets and diagram canvas/library. Add a Storage & Offline Tools settings page showing chosen directory, write-access status, model availability, job progress, export/restore and local diagnostics. Permission prompts are contextual; never a startup wall.

A release may contain milestones, but unfinished scope remains explicitly open. No deletion of acceptance criteria to make a release green.

## 3. Architecture and integration contract

- `:notes`: thin standalone Android host (application ID app.space.notes). Own manifest, permission/folder result handling, lifecycle and entry points; no business rules in the Activity.
- Reusable Notes runtime/domain/repository/UI behind injected host capabilities. Do not replace the existing MySpace Notes registration or legacy store during standalone development. Introduce the new path additively, so changes do not accidentally alter the current MySpace screen.
- Notes-only runtime dependencies (OCR, ASR/JNI, storage and jobs) must not be pulled transitively into the current MySpace build before integration. If necessary use a Notes-owned runtime library consumed only by :notes until integration. Record that module-graph ADR before creating it; feature modules never import siblings or :app/:shell.
- Capability interfaces cover recording permission, directory picking, reminder permission/notification scheduling, sharing, widget refresh and theme. Domain code does not know application IDs or host Activities. Host contracts must be testable with both standalone and simulated suite adapters from day one.
- Separate Notes-owned versioned data schema, media namespace and background job identity. No changes to the shared Room schema or global preferences during standalone work. Existing NotesStore has a 500-item cap, delete-all/insert-all writes and shared Room/prefs coupling; it is not an adequate foundation for media/diagrams. Do not carry those limitations into the new repository.
- Host integration is not automatic data sharing: Android application sandboxes differ. Explicit export/import and a validated migration bring standalone libraries into MySpace without duplicates or deletion. Persisted folder grants must be acquired again by the receiving app.
- Preserve theme/locale/accessibility behaviour through adapters. Retain MySpace-compatible routes and stable note IDs. The future integration gate checks permissions, widgets, notifications, folder access, deep links, migration and every other app's regression suite.

## 4. Local storage design — folder is real storage, not just export

Use Android Storage Access Framework (ACTION_OPEN_DOCUMENT_TREE + persisted URI permission). Never treat a content URI as an ordinary filesystem path. No broad storage permission or MANAGE_EXTERNAL_STORAGE. Android restricts some roots and system folders; explain this in the picker help. Only support local storage providers for the offline guarantee; some document providers represent cloud storage and cannot promise offline access. Provider locality cannot be inferred reliably from a folder name: qualify supported providers and require a successful offline read/write test; do not label arbitrary providers offline-safe.

Default: app-private local library, usable immediately. Settings > Storage > Choose folder migrates the actual library to the selected supported local tree. Proposed format:

- library.json: library UUID, format version, committed generation and checksums.
- notes/<stable-id>/revision-<id>.json: title, structured text, list rows, diagram objects, labels, sort key, timestamps, reminders, archive/trash metadata and attachment references.
- media/<content-id>.<ext>: images, audio and drawing assets. No absolute paths inside note documents.
- commits/: generation manifests referencing complete immutable revisions.
- recovery/: previous committed manifests retained for recovery, with bounded retention.

App-private SQLite/FTS is a rebuildable index/cache, not a hidden second authoritative library. Cache only necessary data; expose and document local caching. Models are app assets/private tooling, not user note data. Logs stay local and exclude note content/audio/transcript by default; user exports diagnostics explicitly.

SAF providers do not universally support atomic rename, fsync or transactional multi-file writes. Write immutable revisions and media first, verify checksums, then publish a new commit marker; retain the previous valid generation and recover by validated manifests after interruption. Design collision/conflict handling explicitly; no last-writer data loss if a user opens a shared folder in two hosts. Initial policy: single writer; detect foreign generations and pause writes until reconcile/reload. External modification is not silently overwritten.

Directory change: preflight available operations, snapshot current generation, copy, verify counts/checksums, commit destination, then switch pointer. Keep source until the user verifies the move; never delete it automatically. Cancel/failure leaves original authoritative. Insufficient space, unplugged SD card, revoked access or provider failure must never show a false Saved label. Preserve unsaved edits in a clearly disclosed private recovery journal, show Pending/Needs access, offer reconnect/change folder, and retry explicitly. Never silently switch the permanent destination.

Support versioned export/import bundles containing structured notes and media, with preview, validation, duplicate-ID policy and rollback. Migration tests must include existing seven-field legacy Notes records; existing MySpace data remains untouched until integration. Uninstall removes app-private data/grants; explain this and recommend a chosen local folder or backup. User-selected folders remain unencrypted unless a separately reviewed encryption feature is added; do not imply device sandbox protection applies there.

## 5. Offline ML research and qualification gates

### OCR

Candidate: bundled ML Kit Text Recognition artifacts, not Play Services downloadable models. Verify exact version, supported scripts, license, minSdk and transitive manifest before pinning. Latin and Devanagari are initial qualification targets; publish supported-script coverage rather than promise every language. Keep typed language-independent Unicode text intact even for scripts OCR cannot read. Run rotation, small/blurred text, crop and multi-image fixtures; retain original media and editable extracted text. Workers are cancellable and never run on the UI thread.

### Automatic speech transcription — required, not deferred

Candidate: whisper.cpp JNI, CPU baseline, bundled multilingual Whisper model. Upstream documents Android support, quantization and approximate memory usage: tiny 75 MiB disk/~273 MB memory; base 142 MiB/~388 MB; small 466 MiB/~852 MB (not measurements for our app). Reference: https://github.com/ggml-org/whisper.cpp (README and examples/whisper.android, reviewed 2026-10-27). MIT engine; verify exact model license/provenance and notices separately. Pin source revision, model hash and ABI build flags before adoption. No native libraries or model binaries committed during planning.

Do not rely on Android SpeechRecognizer or EXTRA_PREFER_OFFLINE: offline availability varies and a preference is not an offline guarantee. No INTERNET permission in the standalone merged manifest; test with network blocked from a clean install. Future suite host may have INTERNET for unrelated apps, so dependency review and an auditable local-engine-only path remain necessary.

Spike multilingual tiny/base quantized candidates before choosing the default. Test English, Hindi and Kannada samples, code switching, silence/noise, long recordings and interrupted jobs. Language coverage and accuracy must be measured; supported model languages are not an accuracy guarantee. Use a small bundled default with optional locally imported larger packs only if justified by benchmarks. Baseline transcription cannot depend on importing a pack.

Recording saves audio first; transcription follows automatically in a bounded background queue, with status, cancel, retry and editable result. Chunk/decode safely, retain audio on failure, checkpoint long jobs, cap concurrent ML work and respect low-memory/thermal signals. Older phones may take longer: show progress rather than promise real-time. Handle process death, ABI mismatch and model corruption explicitly. No speech or OCR is uploaded.

**Feasibility exit:** record real latency, peak RSS, quality, thermal/battery results and APK/model licensing for candidate engines on low/mid/high-tier devices. Failure means investigate/tune the implementation and report the blocker, not silently remove offline transcription. Broad all-phone perfection cannot be honestly guaranteed; publish the tested compatibility envelope and graceful failure behaviour.

## 6. Modern delivery workflow and traceability

Maintain a requirement ledger with stable IDs K001–K100 (original feature numbers), DG-* (diagram), ST-* (storage), ML-* (OCR/ASR), HX-* (host integration), and NF-* (quality). Before each implementation slice, add exact acceptance scenarios, test IDs, current state, dependencies and evidence links. States: Planned / In progress / Implemented-unverified / Verified / Blocked. Source grep is architectural evidence only, never proof a feature works.

Every slice: design/ADR -> data/API contract -> failing tests -> implementation -> review/diff -> automated tests -> device story -> APK + test guide + candid status. Store ADRs for storage authority/recovery, module graph, structured document format, ML engines, reminder policy and migration. Scope changes append an owner-visible change log; enhancements cannot weaken existing behaviours. Inventory every feature at release to catch orphaned requirements. Update the PDF and plan when an approved decision changes.

## 7. Milestones and exit criteria

| Milestone | Scope | Exit evidence |
|---|---|---|
| M0: feasibility & contracts | Module ADR, schema, SAF durability prototype, ASR/OCR qualification, host adapter contract, requirement ledger | Tested crash/reopen storage path, measured local ML on representative devices, dependency/license report; unresolved risks visible |
| M1: standalone vertical slice | MyNotes host; Home/text editor; autosave; chosen folder; recovery; schema/export/import foundation; logging | Signed installable, create/edit/reopen and folder-switch tests, permission denial tests, data-loss fault injection |
| M2: core Keep loop | Pins, colour, archive, 7-day Trash/Undo, drawer, multi-select, search foundation | Each state transition tested; restored notes/media intact; lifecycle restart proof |
| M3: text & lists | Formatting, triggers, nested/custom lists, reorder list rows, merge, copy/share, labels | Round-trip structured text/list fixtures; accessible editing; merge provenance |
| M4: media & diagrams | Images/OCR, drawings, record/play/automatic offline ASR; diagram starter then complete library | Airplane-mode clean-install journeys; audio preserved on failed ASR; diagram round-trip/connector/PNG tests |
| M5: full standalone product | Reminders/search/filter/index depth, widgets/share-in/quick capture, templates/layers, all remaining inventory; wall drag LAST | All inventory mapped to passing outcomes; reboot/timezone/permission/widget stories; scale/performance qualification |
| M6: integration readiness & suite integration | Freeze standalone contract; validated import; suite adapter; permission/scheduler/widget routes and full regression | Same Notes engine used by both hosts; receiving app re-grants folder; no duplicate/lost notes; all-app regression and owner acceptance |

Widgets, share targets and reminders must work in standalone first, not wait for MySpace integration. Host adapter contract tests start early. No promise of a byte-identical rebuilt APK (toolchain metadata may differ): demonstrate unchanged unrelated source/resources/dependencies and behavioural regression instead.

Each milestone delivers APK + public link + TEST-GUIDE + open-issues status. GitHub Actions remains the Android build path. Use a dedicated Notes workflow only after its design is reviewed; do not edit/rerun frozen tests.yml. All normal planning pushes use [skip ci]; later build triggers must be deliberate and reported, not accidental rerun loops.

## 8. Quality and release gates

- Unit/domain tests: rich-text/list serialization, merge, reminder arithmetic, 7-day boundary, IDs, diagram geometry, connectors, undo/redo, migration.
- Repository/fault tests: killed write/copy, disk full, revoked/unplugged tree, corrupted manifest/media, external generations, import conflicts, old-schema upgrade, index rebuild.
- UI outcomes: gestures, caret/selection, no keyboard/focus churn during search, multi-select, all dialogs in app signature, both themes, all colours, large font, TalkBack, rotation and background/restart.
- Permission matrix: microphone allow/deny/permanent deny/revoke during recording; notification deny; exact-alarm unavailable; folder cancel/revoke. Existing audio/transcript and non-audio features remain usable.
- Offline/privacy: fresh install in airplane mode, packaged model verification, no runtime dependency download, no content in diagnostics, export/share only on explicit action.
- Provisional performance budgets (measure, then document any justified adjustment): no disk/ML on main thread; search latest query wins with cancellation and <=200 ms p95 after 150 ms debounce on declared reference device with 10,000 mixed notes; visible list uses virtualization; media decoded at display size. Storage save acknowledgement follows durable commit, not keystroke. Track save latency rather than fake instant success.
- Device matrix: minSdk 26 and current supported Android releases; arm64 physical low-memory/mid/high-tier devices; x86_64 emulator; 32-bit ABI inclusion decided by native compatibility spike, not silently excluded. Long recordings, large images, thousands of notes and large diagrams get bounded-memory tests. Do not assert these tests passed before they run.
- CI evidence distinguishes source contracts, JVM tests, emulator tests and physical-device tests. Stop after two environment-shaped failures and instrument rather than spend hours retrying. Freeze calculator workflows and code.

## 9. Initial risk register and present outcome

| Risk | Mitigation / evidence required | Current state |
|---|---|---|
| ASR quality/speed on older phones | Multilingual small-model spike; measured CPU/RAM/thermal data; progress/retry | Not yet qualified |
| SAF durability/provider differences | Generation manifests, fault injection, supported-provider matrix | Design only |
| Folder grant transfer between APKs | Explicit migration + receiving-host picker | Design only |
| Rich content schema drift | Versioned documents, golden round-trips, migration tests | Design only |
| New Notes code changes current MySpace transitively | Additive runtime, graph diff and old-path contracts | Implementation pending |
| Design breadth hides missed functionality | Full feature ledger + acceptance evidence before release | Original inventory retained; expanded ledger due M0 |

Outcome of this planning round: approvals recorded; old transcription deferral rejected; folder storage and offline qualification added as first-class gates; standalone-first milestones replace integration-only widgets assumption. No app implementation, APK build, ML benchmark or device pass is claimed here. Next execution slice is M0 contracts and feasibility, followed by M1's usable standalone vertical slice.
