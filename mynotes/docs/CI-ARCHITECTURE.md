# MyNotes parallel acceptance pipeline

Adapted from Checktrain02 run [40567027720](https://github.com/tarungittarun/Checktrain02/actions/runs/40567027720), not copied wholesale. Train/network/live-data checks do not belong in this offline notes app. Public APK publication is **after** all required gates, never before the emulator checks.

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
