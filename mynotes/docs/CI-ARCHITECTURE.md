# MyNotes parallel acceptance pipeline

Adapted from Checktrain02 run [40567027720](https://github.com/tarungittarun/Checktrain02/actions/runs/40567027720), not copied wholesale. Train/network/live-data checks do not belong in this offline notes app. Public APK publication is **after** all required gates, never before the emulator checks.

## Current pipeline (accepted 0.7)

The main workflow instantiates **30 jobs**: 21 Android matrix lanes plus nine nonmatrix jobs.
`workflow_guard.py` checks the expanded total, not just the ten YAML job definitions. Independent
source/CI tests, dependency/release inventory, JVM, lint and both builds start without waiting for
each other. Audit waits for the signed candidate; Android waits for source discovery, audit and the
shared test APKs. All required gates must pass before publication. Always-run acceptance fails
closed for missing/duplicate/skipped/unexpected methods or mismatched installed APK hashes.

Current planned inventory is discovered from Java `@Test` methods, not manually maintained totals.
Run `python3 -m mynotes.tools.ci.test_plan` for exact current counts. The API/aspect matrix targets
26/29/34/35/36, real landscape/night/200% font resources, tablet, contextual mic denied/granted and
notification-granted policy. Matrix fail-fast remains false: a failed lane does not hide the rest.
No measured speedup over the smaller serial workload is claimed.

Each lane downloads compiled APKs (no emulator-side rebuild), verifies the latest accepted prior
APK identity, seeds a unique note through real release UI, updates without uninstalling and requires
that title after update. Before/after XML and baseline identity are retained. The installed accepted
release is pulled, its hash/size compared, then the redundant full per-lane APK copy is removed;
the shared accepted artifact retains the bytes. Source APK proof is separate from debug-host journeys.

The APK audit resolves legacy in-Git APKs and pinned GitHub Release receipts, verifies download size,
SHA, signed package/certificate/version and monotonic upgrade policy, and enforces the reviewed
permission allowlist (optional microphone/notifications plus normal boot reception; no Internet).
Static native ELF inventory verifies packaged ARMv7/ARM64/x86_64 architectures and reports 16-KiB
segment alignment/stack flags; that is not a physical 16-KiB or ARM execution qualification.

Large accepted APKs are uploaded as draft GitHub Release assets then exposed together; no clobber or
replacement is allowed. Small checksum/source/audit/size/signature receipts are committed. Release and
test compile artifacts/baselines have short retention; evidence/accepted bytes are retained longer.
Same-version test/CI/docs work audits and reuses the existing official APK only if protected app/build
inputs match its source. Runtime/resource/build/signing changes need a higher public versionCode.

API26 radio setup temporarily roots only the fixture, then requires shell UID2000 before installing
or testing. A closed transport during adbd restart is not treated as proof of state: a bounded UID
verification must establish the requested identity. There are no app-test or whole-lane retries.
A bounded Node bootstrap observer captures the emulator action itself, so pre-script SDK/AVD/boot
failures produce useful annotations rather than just “instrumentation missing”. Compiler and app
failures also emit bounded contextual annotations; source/protocol self-tests exercise these tools.

The separate **three-expanded-job, read-only native soak** compiles once, then drives API26 and API35
for the requested timed interval. It cannot publish. It records real native English OCR/ASR, interrupted
PCM repair, atomic note/backup cycles, source-WAV integrity and post-warmup PSS/native allocations.
Normal CI runs a short version; only the dedicated completed reports establish a long duration.
Triggers match exercised components, so unrelated UI/controller-only fixes do not discard a long
engine/disk measurement. Recorded source identities keep that scope explicit.

### Accepted and diagnostic checkpoints

- Dedicated soak [40586522279](https://github.com/tarungittarun/TicTic/actions/runs/40586522279) completed:
  API26 **70m 0.978s /3,205 cycles**, API35 **70m 0.387s /2,095 cycles**. Max sampled post-cycle
  PSS 193.38/113.65 MiB; maximum above warmup 8.06/6.13 MiB. This is not true inference-peak memory
  or zero-leak proof. Debug component source `899e5a0` has unchanged exercised inputs at accepted8;
  later capture/controller fixes are qualified by the normal gate, not by this soak. See
  [durable metrics](evidence/2026-10-28-qualification.json) and [scope/report card](REPORT-CARD.md).


- Current 0.7 acceptance [40590113347](https://github.com/tarungittarun/TicTic/actions/runs/40590113347):
  **69 JVM, 98 unique Android cases, 803/803 executions /21 lanes**, all 30 expanded jobs green.
  Local gates 107 Notes /497 root. APIs26/29/34/35/36; notification and mic positive/negative policy;
  real seeded prior-release update and exact installed APK identity in every lane. Static inventory:
  **18 libraries /3 ABIs**, no 64-bit load segment below 16 KiB alignment. Not physical ARM/16 KiB certification.
- The final landscape fix uses fixed-to-user-rotation policy and reapplies locked rotation after
  release UIAutomator seeding, immediately before instrumentation. Real resumed Activity resources
  **and physical display aspect** must agree; portrait actual1080×1920 was correctly rejected earlier.
  All 98 final landscape cases passed; expected values/coverage were not reduced.

- Native 0.6 acceptance [40582488414](https://github.com/tarungittarun/TicTic/actions/runs/40582488414):
  **64 JVM, 80 unique Android tests, 662 executions / 20 lanes**, all required gates green.
- The initial native dependency build exposed an old D8/JNA failure; the isolated Notes toolchain moved
  to AGP8.7.3 / Gradle8.9 / compile35. Calculator/MySpace builds were not changed or rerun.
- 0.7 qualification caught a stale schema-version test after the intentional schema 5 migration;
  the assertion now verifies schema 5 plus default reminder zero for schema 1 data. Orientation is
  checked against a real resumed Notes Activity and physical display aspect, not an inactive
  application context. The expected landscape resource value was not relaxed.
- Independent failures exposed adbd root/unroot transport closure and pre-script emulator bootstrap
  failures. Those caused additional state/bootstrap instrumentation, not blind full-suite retries.
- Controlled-font OCR diagnostics observed exact Hindi `भारत नमस्ते` (confidence 90) and Kannada
  `ಕನ್ನಡ ಭಾಷೆ` (confidence 96) on API26 in run 40586522348. Confidence is not accuracy; the source PNGs,
  recognized text and token match flags remain artifacts, not a broad language qualification.

---

## Historical foundation (0.3; retained evidence, not the current matrix)

## Dependency graph

```text
source + CI self-tests ─────────────────────┐
JVM domain tests ──────────────────────────┼──────────────────┐
signed release build → APK audit ───────────┤                  │
debug + instrumentation build ──────────────┤                  │
                                           ↓                  │
                         Android matrix (independent lanes)    │
                                           └──────────────────┤
                                                              ↓
                                            immutable publish (new code only)
                                                              ↓
                                            always-run acceptance summary
```

The first four jobs run independently. Emulator jobs download compiled artifacts; they do **not** rebuild. Java/SDK setup and Gradle caching are shared through `.github/actions/setup-mynotes`. Fail-fast is disabled within the matrix so one failure does not hide evidence from other APIs/aspects. Newer branch runs cancel obsolete runs. Jobs have bounded timeouts and artifacts have 7–14-day retention. No unrelated app/workflow is invoked.

## Source-enforced test plan

`mynotes/tools/ci/plan.json` owns partitions and emulator APIs. At introduction:

| Lane | Purpose | Expected test executions |
|---|---|---:|
| storage-api26 | Minimum supported SDK; persistence, structured codec, backup and transfer/generation protocols | 35 |
| ui-api29 | Core controller/view journeys and board layout | 5 |
| full-api34 | Every Android test again on modern Android | 40 |
| JVM (independent) | Domain, schema, merge and model rules | 29 |

There are **40 unique Android cases / 80 Android executions**, not 80 distinct tests. Counts are discovered, not hardcoded into acceptance commands. New methods enter their class's lanes automatically. New classes must be assigned once, with no overlaps. New modules require explicit CI wiring. Removing the minimum or full-modern lane fails the plan guard.

Instrumentation uses raw AndroidJUnitRunner per-case status records. A green exit code or an `OK` string alone is insufficient. Every expected case must pass exactly once, with no extra cases, skips, assumptions, duplicates, crash or missing completion. JVM XML receives the same exhaustive executed-case check. Gate self-tests simulate these failure modes; workflow topology validation rejects accidental serialization, missing publication dependencies, broad write permissions and dependency cycles.

## Release vs test-host evidence

Every lane installs the oldest retained release, updates **without uninstalling**, launches the accepted official release, captures its window/screenshot/package state, pulls the installed base APK and verifies its SHA-256 against the downloaded artifact and audit metadata. Airplane mode is enabled and Wi-Fi/data disabled. The audited release currently has **no permissions**, including no INTERNET permission.

The API 26 AOSP image denies shell Wi-Fi toggling (`CHANGE_WIFI_STATE`). Only radio provisioning uses emulator root on that lane; the harness unroots and verifies shell UID 2000 before any app install/test. No permission or privilege is added to MyNotes.

Then the lane installs the separately compiled debug/test APKs and runs its instrumentation cases. These run the current app/runtime source under the debug host, **not inside the signed release**. Release upgrade/hash/launch smoke and debug-host journeys are deliberately reported separately. This is not a claim that all 40 tests directly drive the exact signed bytes, nor that airplane mode simulates all real-world failures.

For a new app version, the accepted APK is the candidate. The APK audit verifies stable package/signing identity, strictly increasing versionCode, minSdk, non-debuggable status, backup privacy and reviewed permissions before any emulator runs. Publication requires source checks, JVM, both builds, audit and every Android lane. It publishes only the audited/tested bytes plus checksum, source and audit metadata; existing files must not be overwritten.

For **CI/test/docs-only work at the current version**, there is no forced version bump: the candidate is compiled/audited, but the existing immutable published APK is the accepted release and is not republished. This is allowed only when package/certificate/name match and all app/build inputs match its recorded source commit. Unknown new build inputs are protected by default. Runtime/resource/build/signing changes require a higher versionCode; the original strict `verify_update.py` policy remains unchanged. Metadata distinguishes the run source, official APK source, candidate hash and accepted hash.

## Failure evidence and maintenance

The final acceptance job runs even after failed/skipped dependencies and fails closed for missing evidence, incomplete method coverage, mismatched installed APKs or improper publication. Its Actions summary reports each gate, lane counts, release SHA and source. Artifact bundles retain runner/JVM output, XML reports, bounded logcat, package/activity state and screenshots. Build failures emit bounded compiler context as check annotations so useful diagnosis does not rely on downloadable logs alone. A setup/boot failure can have no instrumentation output: it is a failure, never a zero-test pass.

When adding features:
1. Add real model/device tests, then assign any new class in `plan.json`; adjust APIs/aspects when justified.
2. Add an explicit APK permission review/test when optional microphone, reminders or media arrives. Never silently relax the current permission allowlist.
3. Update same-version source-equivalence exclusions only for demonstrably non-app inputs.
4. Keep this workflow, its topology guard and adversarial parser tests aligned. Run `python3 -m unittest discover -s qa` before pushing, and deliberately run Notes CI for build/runtime changes.
5. Inspect actual Actions critical-path/job timings before claiming improvement or adding more shards. Extra lanes improve API coverage but consume more runner-minutes; cache hits and queue/boot overhead matter.

This matrix does not certify all phones, physical SAF/SD providers, accessibility, IMEs, scale/durability failure combinations, OCR or speech models. Those need their own qualified fixtures and lanes when implemented. The previous successful serial run is [40569713346](https://github.com/tarungittarun/TicTic/actions/runs/40569713346); new timing/qualification evidence will be recorded after the parallel run, not assumed.

## Bring-up evidence

- Initial parallel run [40572285326](https://github.com/tarungittarun/TicTic/actions/runs/40572285326): source, JVM, both builds, audit and API 29/34 passed; API 26 failed before instrumentation; final acceptance correctly failed and publication was skipped.
- Instrumented run [40573209545](https://github.com/tarungittarun/TicTic/actions/runs/40573209545): command/stack annotations identified API 26 `svc wifi disable` shell permission denial, not an app crash. The harness now provisions that fixture with temporary emulator root and explicitly unroots before app work. This is an explained correction, not an unexplained emulator retry.
- These expanded runs do not establish a speedup over the old single-API serial run. The matrix adds qualification and runner usage; wall-clock timing still needs a green comparable run.

### Qualified parallel run

[40574272823](https://github.com/tarungittarun/TicTic/actions/runs/40574272823), source `64ef4269aa224d5a85b7473c005da92c056c1f0d`: **all required jobs and final acceptance passed**. Publication was intentionally skipped for same-version revalidation. Local pre-push root gate: **464 Python tests passed** (these are structural/tooling tests, not additional Android journeys).

| Job | Measured duration |
|---|---:|
| Source/protocol/CI self-tests | 20s |
| JVM domain tests | 53s |
| Signed release compilation | 61s |
| Debug + instrumentation compilation | 66s |
| APK audit | 41s |
| Android storage/API 26 | 102s |
| Android UI/API 29 | 119s |
| Android full/API 34 | 122s |
| Final acceptance | 17s |

The first four jobs started together; all three Android lanes started together. Source run creation to completion: **4m51s**, including **42s** before the first jobs started; first job start to completion: **4m09s**. Old serial run creation to completion was **3m53s**, with only one API/40 Android executions and without the new final summary. These are different workloads and cache/queue states: **no measured end-to-end speedup is claimed**. The expanded pipeline doubles Android executions, qualifies three APIs, and gives independent failure isolation. Audit/setup overhead and emulator provisioning remain candidates for future measured optimization, not reasons to remove acceptance gates.

The acceptance summary verified:
- API 26: **35/35**, API 29: **5/5**, API 34: **40/40**; JVM: **29/29**.
- Installed/downloaded/audited release SHA-256 matched on all three APIs: `bc7464078be8f267fbcb64838f6622d82a6001c97356f89e67ffa6d9802231bc`.
- Official APK remained `MyNotes-v0.4.0-5.apk`, originally built from `cb26ef8a2b24ae38077628b404a1059631309cbc`; no overwrite or unnecessary app version increment.
- API 26 temporary radio-provisioning root was removed before app installation/test, and ordinary shell UID was verified.

Some maintained upstream actions still declare Node 20; GitHub reports that it forces them onto Node 24. This run passed under that runtime. Track upstream action updates and requalify those changes rather than hiding runtime warnings or upgrading actions without a validation run.
