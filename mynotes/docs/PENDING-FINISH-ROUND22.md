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
  invalid/extreme floats. The existing real-PDF two-controller reconstruction case
  additionally asserts current page, quarter-page offset and all five zoom floats,
  retained query, no OCR restart and cache cleanup.
- SDK-only retry (max3 attempts,180s each,5s between) addresses the evidenced
  emulator package ZIP bootstrap failure. No test/instrumentation/assertion is
  retried or suppressed; all profiles and exact once-only inventories stay. A
  successful emulator version command is required; raw bootstrap output retained.

No data codec, saved Note/Doc, original document, default speech/weights, network
permission, signature or package change. Increasing version37 preserves install-over
qualification. Final-byte upgrade smokes and physical acceptance remain required.
