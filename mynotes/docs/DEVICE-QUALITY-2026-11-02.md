# Device-quality follow-up — active work ledger

Started 2026-11-01 21:49 UTC (2026-11-02 local). Requested window: 360 minutes;
this ledger records actual work/results, never an invented elapsed-time/training claim.
Previous App-folder qualification (30 jobs) and speech/OCR research (40 jobs) are closed,
separate scopes. Current follow-up: public corpus audit run41354950288 SUCCESS (1 job). Qualification
round1 run41354950129 superseded while its build was running: an over-broad RTF-fixture
edit accidentally removed pre-existing methods, caught by inventory comparison before
native qualification. All methods restored and a pinned baseline-preservation gate added.
Manual cancellation returned insufficient integration permission; workflow concurrency
will supersede the old run. Count its final actual job outcomes, not planned slots.
Round2 expects 282 JVM and 1116 native outcomes; final results pending.

Priorities: startup/Undo failure and independent diagnostics, home layout, simpler storage,
automatic OCR, common-file previews, speech quality and useful local intelligence.

The supplied launcher screenshot shows a truncated history-mismatch notice, **not a crash
stack trace**. It does not establish the exception or prove lost current notes. Reproduce
Undo with a removed App folder and add diagnostic access independent of library loading.
Never clear user notes or silently reinitialize an unavailable selected folder.

## Research already inspected in this follow-up
- AI4Bharat/Svarah GitHub README: Indian-accented English benchmark, not a training split;
  audio distribution links to Hugging Face (not an allowed sandbox download host).
- AI4Bharat/NPTEL2020-Indian-English-Speech-Dataset: educational-domain data with transcript
  quality caveats, large external downloads, not a substitute for spontaneous phone speech.
- AI4Bharat/IndicVoices: spontaneous Indian-language corpus; data/model downloads are on
  external hosts. Do not report training until licensed data and trainable weights are
  actually available, split without speaker leakage, trained and independently evaluated.

No new Indian-accent training or model adoption has occurred. User authorization permits
training, but does not make unavailable data/models accessible or establish their licensing.
