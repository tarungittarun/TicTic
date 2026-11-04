# Pending-work implementation cycle — 4 November 2026

This is an implementation/evidence log, not a completion or release declaration.
The previous qualified composite remains build63/report41789450818; build64's
signed APK is unchanged. New code must be qualified separately.

## Actual changes

- Standard Vosk: inspect a validated WAV before model provisioning. **Only exact
  digital zero** bypasses model verification/loading/decoding. There is no
  amplitude threshold, so a single quiet nonzero PCM byte keeps normal inference.
  Cancellation, truncated/malformed/empty input and source preservation are tested.
  Six new JVM tests plus a native isolated-storage check cover this boundary.
  This does not improve noisy/Indian-English acoustic accuracy and is not a
  measured general phone-speed gain. Models/hashes remain unchanged.
- Image reading: compact 48dp named/tooltip secondary tools, primary area reading,
  explicit copy without saving/dismissing, expandable selected-text preview.
  The main review no longer presents an engine score as apparent accuracy; its
  information dialog explicitly says the score is not an accuracy percentage.
- Native image tools and copy/source invariants were added to the exact ordered
  inventory: 429 native methods. No failing assertion was removed or relaxed.

## Counterexample and repair

Original pilot41805417580/source0c7b3f9f: both landscape lanes passed52/52;
portrait lanes passed51/52, failing the original retained-dialog rotation test.
“Keep selected image” had156px height but only149px visible after reflow.
Exact-zero silence/source preservation passed in all four original lanes.

Repaira1857eec moves primary image actions into a fixed sidebar area outside the
secondary ScrollView. The image bitmap, selection/crop and callbacks are retained.
The same original full-visible-height assertion stays unchanged. Pilot41806836439
qualifies the repaired source and explicit-copy test. A pending run is not a pass.

Build65/full41805972777 was already started before this UI counterexample; keep its
outcomes separate. Build66/full41807474878 includes the repair and now gates the
full native/corpus lanes on all four current-source recognition-focus lanes first.
No old-source screenshot or inference is labelled as a new-source measurement.

## Signed install-over

Run41805650798, API34 job126491872721, passed actual signed63→64 UI creation,
cold restart, install-over, Note/Doc readback and unchanged installation identity.
This checks two public ASCII fixtures, not an entire rich library or a phone.

Its API26 job126491872011 failed at `svc wifi disable` with137, before installing
either APK. The narrow AOSP follow-up41806376244/job126494306064 failed at the same
pre-install command. Cause log runs41806094477 and41806995249 retain both attempts.
Do not relabel them app crashes, OOM proof, migration failure or absent artifacts.

The next API26 provisioning uses an emulator-only, no-Notes-installed guard,
temporary root for the protected airplane-mode broadcast, three consecutive
actual disabled-Wi-Fi/no-default-network observations, then verified shellUID2000.
No failed radio command is ignored, no data is cleared, and no app dialog is
suppressed. Unit tests reject physical/populated devices, failed broadcasts,
remaining active default networks and unstable state. A guard correction changes
`pm path` (exit1 when absent) to successful-empty `pm list packages`. Cancellation
of obsolete queued attempt41807216965 was denied403; it is **not** described as
cancelled. Current follow-up41807291666 must still provide its actual result.

## Still open

- The24 OCR/685 speech target misses recorded in build63 are not declared fixed.
  The six distinct erroneous OCR inputs still fail ordinary PSM3/6/11 diagnostics;
  choosing a higher engine score is not a correctness fix. Larger Kannada weights
  previously failed an actual native regression; they are not blindly bundled.
- No new model, training or experimental attention adoption. No broad real-scene,
  unseen-speaker, physical microphone/playback or phone-speed qualification.
- No physical device is attached to this workspace. Physical acceptance cannot be
  manufactured from emulator results. Private screenshot pixels from the earlier
  session are not present in this restored workspace and are not uploaded to CI.

## Subsequent measured outcomes (4 November)

- Signed63→64 follow-up41807291666 **passed both API26 and API34**;
  original reports are in `updates/41807291666/`. The earlier failures remain
  pre-install provisioning failures, not migration failures.
- Repaired pilot41806836439 passed53/53 in light portrait and both landscape
  profiles. Dark portrait executed0/53: retained current-window evidence showed
  an external Pixel Launcher ANR over a resumed Notes activity, not a Notes ANR.
  Helper e4b15cee now checks only current displays, requires all focused windows
  to be Notes, and may recover only that exact external Launcher once after
  retaining evidence. Notes/unknown errors remain blockers. Root tests pass;
  the helper change still requires native exercise.
- Full66/run41807474878 attempt1 built the exact signed APK and passed all53
  focus tests in three profiles. Dark portrait **timed out booting the emulator**
  and executed0/53; native/benchmark gates correctly skipped. Check-run
  126502279030 retains the boot-timeout annotation. This is distinct from the
  earlier pilot's Launcher ANR and does not qualify the missing profile.
- Signed64→66/run41809268340 passed API26/job126504096066 and
  API34/job126504096277. Exact APK hashes, stable signer, offline state,
  Note/Doc persistence, old-release cold restart and unchanged first-install
  identity were required. The read-only evidence-retention workflow downloads
  these original artifacts; it does not repeat passing device tests. Rich-data
  migration and physical acceptance remain separate, unverified work.

### Exact-binary continuation, not a new candidate

Direct failed-job rerun was denied by GitHub with403 `Resource not accessible by
integration`; no rerun was started. The continuation workflow instead verifies
original foundation13311605121/sourcea267b059/digest1543b3f0…, the three successful
focus job identities, and the exact original boot-timeout annotation. It reuses
those binaries, runs **only dark portrait focus**, then unlocks the previously
skipped four full-native and twelve corpus cohorts. Production/instrumentation
source directories must have no diff from the APK source. Runner/helper source
is separately recorded in `continuation-provenance.json`; original app source is
not relabelled. Fresh guests must prove offline before either APK installation.

## Real public screenshot baseline (separate from exact66 continuation)

Added two actual third-party Google sample Android UI screenshots, not newly
painted fixtures, with their original Apache-2.0 LICENSE, verified Git blob IDs
and SHA-256s. References were visually transcribed before native inference.
Six explicitly labelled inputs cover original, half-size and JPEG50 forms;
this is two real screenshots, **not six independent scenes**. These examples
are development evaluation, not untouched heldout or camera-photo acceptance.

An opt-in test-only harness passes pixels to the unchanged automatic recognizer;
references are used only afterward. It records references/hypotheses, hashes,
source-pixel preservation and separately scoped inference times. An independent
Python edit-distance scorer rejects missing/duplicate inputs, rewritten targets,
wrong identities/dimensions, source mutation and native-score disagreement.
Every profile must execute all six cases and its three environment assertions;
any operational failure or CER above0.15 fails the separate research workflow.
No images, new model or benchmark class are included in the production APK.
Native results are pending; this is not yet an accuracy-gain claim.

## Completed older-source benchmark evidence, not overall candidate approval

Full65/run41805972777 finished. Its corpus is complete4752/4752; its native
qualification is incomplete (1714 recorded outcomes for429×4 expected) and its
portrait clipping failures predate the repaired66 UI. Do not offer it as a newly
qualified complete app. Measured65 targets still miss24 OCR observations
(20 Kannada/4 mixed) and691 speech observations (117 clean/114 clipped/123 quiet/
337 noisy), versus685 speech misses in the older63 report. There is no claimed
causal acoustic gain or new model; full66 must retain its own measurements.

There **is** a narrow measured exact-zero speed result: sixteen generated
silence controls in four profiles kept empty output. Median emulator times for
1/5/15/30-second zero WAVs changed from1021.5/942/1341.5/1717.5ms in the63 report
to1/1/3/2.5ms in65. The benchmark generator/timer file has the same Git blob in
both sources. Old silence rows did not record byte hashes, so this is a matched
generated-control comparison, not a falsely hash-bound audio comparison.
A recorded0ms means below the1ms timer resolution, never infinite speedup.
`exact-zero-emulator-speed-evidence.json` preserves all sixteen paired rows and
capture identities. This is not a quiet-speech optimization or phone-speed claim.

Continuation41810449937 passed the missing dark-portrait focus53/53 using the
original66 test APKs; all four exact-binary focus profiles are now covered across
the original run and continuation. The previously skipped full-native/corpus
cohorts are running. Current-display focus helper code was exercised by the
successful continuation; its conditional external-ANR recovery branch is not
claimed exercised without retained evidence that it actually occurred.
