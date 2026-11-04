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
