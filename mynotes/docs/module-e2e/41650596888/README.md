# Executed module-ordered E2E and recognition report

**Completed evidence, not an all-pass or physical-phone approval.**

Main deliverable: **MyNotes-Module-E2E-Report.pdf**, bookmarked in the same12-module order as the actual strict native execution. All four API34 profiles are retained: light/dark × portrait/landscape. Detailed source and recheck identities are inside `provenance.json`.

## Actual outcomes

- Strict ordered execution41658476468: **1,672 PASS /4 FAIL /0 unexecuted**,419 methods ×4 profiles. The same PDF export-options checkbox activation assertion fails in all four profiles. No assertion was waived or failure converted to success. The original mixed-shard baseline separately passed1,676/1,676; that does not erase the ordered-run failure.
- Isolated original-APK probe41663113958 reproduces that assertion failure. Retained frames show the checkbox **is visible**, so this is an unqualified activation/accessibility interaction, not proof that the option is absent. The File viewers chapter includes a diagnostic screenshot and the precise remaining qualification work.
- Recognition: **4,752 accepted, independently re-scored observations**, with **0 operational errors**. OCR1,536; speech3,216. Original invalid-orientation observations remain rejected and visible beside accepted rechecks. Total model observations including rejected runs:5,940.
- OCR: **1,496/1,536** meet CER≤0.20. Missing Kannada text and mixed-script substitutions remain. High confidence did not prevent a missing line.
- Voice: **2,519/3,216** meet the declared normalized-WER/silence target. Indian-English KSP is substantially worse than US-English SLT in these two speakers; this is not a population/accent generalization.
- Original source-build JVM: **317/317 passed**. Production app/runtime/domain main source trees match the v57 baseline. **0 training steps; no new application APK or runtime/model changes.**

## Amount/type/time and sample limits

- OCR:384 rendering conditions per profile, **324 distinct image hashes**,8 templates (7 kind labels). Four requested font families,28/48/72px, light/dark inputs,1.12M/2.4M pixels and0°/2° geometry settings. They are not384 independent photographed documents. PNG compressed bytes and exact hashes remain per row. Accepted OCR median351ms; observed range196–3,944ms across CI hosts.
- Speech: **200 distinct source recordings,100 distinct prompts,2 speakers**, four amplitude/noise/gain conditions and four silence controls per profile. PCM16 mono16kHz. Across accepted profiles,9,697.916 seconds of audio processed in5,049,705ms summed measurement time: aggregate RTF0.5207. These are repeated conditions, not3,216 independent speakers or long spontaneous recordings.
- Each chapter records actual module method count and elapsed time. Recognition summaries pair timing with actual bytes, pixels, audio duration and condition. Vosk creates/closes its native model per transcription; materialized files may be warm. Fixture synthesis is excluded. Emulator results are **not physical phone latency** or causal theme-speed comparisons.

## Files and truth boundaries

`measurements.csv` / `measurements.json`: every accepted reference, hypothesis, flag and timing. `*-rejected-original-observations.json`: rejected original orientation cohort, never silently overwritten. `performance-summary.json` and `voice-speaker-summary.json`: measured aggregates. `strict-native-module-executions.json`: exact module order, times and outcomes. `pdf-option-diagnostic/`: independent failed-probe frames and hashes. `pdf-layout-audit.json`: PDF bounds/bookmarks/image integrity only—not WCAG certification or model accuracy approval.

Original run41650596888 failed three dark-landscape resource assertions (portrait guest); exact-original-APK rechecks41652746046 and41654076991 passed unchanged assertions and input-hash checks. Invalid ordered-driver attempt41654548239 has0 validated outcomes because raw statuses were mishandled; no app result is inferred from it. These failed attempts remain disclosed.

Physical microphone/loudness, supplied private-image accuracy, handwriting, universal emoji/scripts, recovered font/style fidelity, long spontaneous dictation and real-device drag precision remain unqualified. No private attachment, note or recording was uploaded.
