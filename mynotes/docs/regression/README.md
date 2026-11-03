# Actual fix → test → find → fix → test

**Executed qualification:** v59 completed all419 native methods on all four API34 theme/orientation profiles (1,676PASS/0FAIL),331 JVM tests, all public corpus invocations including the explicitly corrected missing voice lane, and the frozen A/B set. The complete module-ordered PDF is being assembled from those actual outcomes. Recognition targets are not universally met; physical acceptance remains separate.

## Application changes, not report-only revisions

### v58: PDF options

- Replaced the indirectly activated list-label row with a labelled native CheckBox using the dialog theme and a minimum48dp target.
- Treat the checkbox value as a draft until **Choose PDF file**. Cancel/Back discard the draft rather than changing the Activity's export option.
- Kept the original failing native method and direct accessibility click. Strengthened it with checkable/clickable/initial/toggled state, Cancel preference preservation, and the original byte-for-byte document invariant.
- Original strict v57:1,672PASS/4FAIL. v58:1,676PASS/0FAIL (419 distinct methods ×4 API34 theme/orientation profiles).

### v59: OCR selection/coverage, unchanged model weights

- Stop a tiny confidence gain from erasing clear, pure-native-script text in favour of new Latin-looking fragments.
- Recover one spatially separate, same-script, similarly sized and aligned missing line from sparse recognition while preserving the original correct line and its word boxes. Confidence is conservatively lowered, not advertised as a correctness probability.
- Try one block-segmentation pass only for structurally suspicious numeric lines containing native letters adjacent to ASCII digits. Existing operator/currency multiplicity safeguards remain.
- Keep the bitmap and source document unchanged. Word-selection text must agree with the chosen/recovered output.
- No reference strings, sample IDs, input-font names or correctness labels enter production selection. No new weights, training, private inputs or cloud inference.

## A real failure found during the repair

The first real v59 JVM run was330PASS/1FAIL out of331. The new numeric guard fell through to the older length/near-confidence rule, admitting a lower-confidence rewrite. Fixed the production return logic and retained the exact failed test. Subsequent Actions build passed331/331 JVM tests, Android compilation and release/debug lint. The local machine has no javac; no local JVM or Android-build pass is fabricated.

## Independently verified OCR before/after

### Original development/reproduction corpus — one light-landscape profile

384 exact matching IDs, references and PNG hashes. Target CER≤0.20 flags:374/384 →378/384. Four cases improved; zero per-case CER regressions.

| Case | v58 CER | v59 CER | Observed result |
|---|---:|---:|---|
| ocr-0269 |0.4783|0|Both Kannada lines present; original correct lower line retained.|
| ocr-0275 |0.2174|0|Correct Kannada no longer replaced by Latin-looking fragments.|
| ocr-0294 |0.2778|0|The actual mixed-script numeric fixture reads exactly correctly.|
| ocr-0306 |0.2500|0.0833|Improved but **not correct**: operators/amount still wrong. A target flag is not semantic approval.|

Full before/after strings, all384 rows and remaining failures: [development comparison](59/original-corpus-light-landscape-comparison.json).

### Frozen extra validation, not development tuning

32 new own-authored references ×4 rendering conditions =128 synthetic inputs, repeated on4 API34 profiles.512 paired comparisons /1,024 inferences, not512 independent photographs. References and conditions were frozen before tuning/inference; outputs were not used to choose the rule.

- Baseline58 targets:496/512. Candidate59 targets:496/512.
- Improved pairs:0. Regressed pairs:0. Recognized strings are unchanged on this set; do not call that an accuracy gain.
- Mean CER both0.0279734682; micro CER both0.0259095921.
- Emulator median265.5ms versus341ms. Different CI hosts/load prevent attributing that difference to the patch or claiming phone latency.
- Debug APKs were re-signed together solely for cross-build instrumentation. Every non-signature ZIP entry was verified unchanged; the user release APK was never re-signed. Baseline original-byte SHA and complete payload proofs are retained.

[Every frozen reference/recognition/timing pair](59/heldout-reference-recognition.csv) · [JSON and payload/native proofs](59/heldout-comparison.json).

## Other completed checks and boundaries

- Exact stable-signed release57→59 install-over succeeded with two synthetic ASCII Note/Doc fixtures, unchanged first-install identity and no uninstall/clear-data: [actual proof](../design-2026-10-29/updates/41680664804/57-to59/release-update-smoke.json). This is not complete rich-library or phone migration acceptance.
- API36 dark landscape/font2 PDF action/state/Cancel check:4/4 methods passed using a refreshed live accessibility node. The earlier cached-state failure remains retained; the app click action and all state assertions remain. [Actual outcome/frame provenance](59/live-pdf-results.json).
- API26 light portrait/font2 PDF focus:4/4 methods passed in run41682997197. Earlier guests killed before native/environment evidence remain unknown, not failed app assertions or passes. The exact-APK recheck restored known SDK validation/root-only radio setup and returned to normal shell UID2000 before app testing. This is PDF-focus coverage, not419 methods on API26.
- Original missing voice lanes were emulator-setup failures with no execution artifacts. Corrections reuse the exact compiled APKs and unchanged data/assertions. Original failed workflows are not relabelled green.

## Still open

Residual OCR character/operator/amount errors, arbitrary fonts/handwriting/emoji and the private image are not fixed universally. Input-font variety does not establish recovered font/style fidelity. Default speech weights/output are unchanged; Indian-accent generalisation, real microphone/playback quality and long dictation remain unqualified. Physical top-edge drag, exact placement and actual private-library migration still require their own acceptance.

All work remains on the assigned Arena branch; Calculator and tests.yml are unchanged. Android builds/inference qualification use Actions, not local Android builds. Development APKs are not public releases.

## Genuine new development APK

[MyNotes v0.59.0/code59 — raw APK, not ZIP](https://github.com/tarungittarun/TicTic/actions/runs/41678235733/artifacts/13252716116)

155,165,372bytes; SHA256 `b94a1938f87e3fe20c5747522020811c0e4145b2e5e7758673c4d08cf57c1726`. Exact app source `54a98e50da3996f4600489a292b8d0139e9ef05c`; stable package/signing identity, min26/target34/no INTERNET permission. This is a genuinely changed app, not the old57 APK or a training claim. Install over the existing app; do not uninstall or clear data. Synthetic upgrade proof does not qualify every private rich library.
