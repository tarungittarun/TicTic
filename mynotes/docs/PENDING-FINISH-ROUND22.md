# Round22 candidate: normalized PDF/Word-page viewport retention

Candidate0.34/code37, NOT native-qualified or delivered. Last delivered remains
0.32/code35. Round21 retry41461174962 is separately source-pinned to4a9594a5;
its source/build36 is not retroactively changed by this candidate. Keep that run
and its peers/evidence; Round22 uses a separate concurrency group, not cancellation.

## Source changes; actual new Android qualification required

- Current PDF/prepared-Word page stores zoom1–5, normalized focal point and pan
  (five finite floats), plus normalized page-top offset, page number and query.
  Width changes capture before child layout/rebinding, restore at the new geometry.
- Fresh view reconstruction supports applying zoom before first layout. Snapshot
  arrays are copied, malformed/nonfinite state resets safely, finite extremes are
  clamped. Recycled noncurrent pages still reset to fit; this is current-viewport
  retention, not a1000-page zoom-history cache. No bitmap/source bytes in Bundle.
- Same bounded lazy raster remains: **not vector-quality redraw while zoomed**.
  No direct glyph handles, Office fidelity or actual process-kill claim is added.
- Two actual native View-state cases cover reconstruction/resize/copy isolation and
  invalid/extreme floats. The existing real-PDF two-view reconstruction case
  additionally asserts current page, quarter-page offset and all five zoom floats,
  retained query, no OCR restart and cache cleanup.
- SDK-only retry (max3 attempts,180s each,5s between) addresses the evidenced
  emulator package ZIP bootstrap failure. No test/instrumentation/assertion is
  retried or suppressed; all profiles and exact once-only inventories stay. A
  successful emulator version command is required; raw bootstrap output retained.

No data codec, saved Note/Doc, original document, default speech/weights, network
permission, signature or package change. Increasing version37 preserves install-over
qualification. Final-byte upgrade smokes and physical acceptance remain required.

## First candidate37 build and bootstrap result (not native approval)

Run41463352662 **build passed**, including294 executed JVM tests/lint and APKs.
All eight native shards reported0 executed methods. Preserved
`refinement-native-sdk-bootstrap.txt` establishes the new preflight fault: emulator
package installation could succeed, but `emulator -version` failed loading
`libpulse.so.0` before the later emulator-runner setup. This is a CI dependency
ordering mistake, not measured app behavior. Install Ubuntu `libpulse0` explicitly
before the preflight; retain the version check, retries and all exact inventories.
Runtime/app/tests/version37 are unchanged by this retry. No native approval or
upgrade smoke success is claimed from this failed run.

A new external ADB public-fixture harness is prepared for actual stopped-process
kill/task relaunch, distinct from fresh-controller tests. Its host parser/fixture
checks pass; it has NOT yet run on Android and is not recovery acceptance. It does
not force-stop/uninstall/clear data and checks new PID, PDF/Word page+query and CSV
query+anchor row plus unchanged source bytes. Image OCR recovery remains separate.

## Actual-process probe dispatched independently of approval

The exact built release37 APK from41465255273 is pinned as artifact13151287079,
155142120bytes, SHA256
`6fe241380a01601348e3935dc3a1cfca56b9ab865304ccf6b9cbe3244d78af6c`.
Its build succeeded; the native matrix remains pending, so it is NOT delivered.
A separate nonpublishing process probe verifies those bytes, common signer,
package/version/min26/noINTERNET, then runs the public3-format ADB harness on a
fresh API34 emulator with no clearing/uninstall/force-stop. Its source guard
requires GitHub Actions plus emulator hardware identity, exact installedcode37 and
a new collision-free fixture directory. Host gates are not Android success; the
actual run/outcome still needs inspection. Do not treat a successful build or this
queued probe as release approval or image-OCR recovery.
