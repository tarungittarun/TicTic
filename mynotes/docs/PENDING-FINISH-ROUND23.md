# Round23 candidate0.35/code38: drawn-host reader reconstruction

NOT yet qualified or delivered. Last delivered remains0.32/code35. The candidate37
baseline built fromb3553c99 actually passed run41465255273:294 JVM/lint and all
1314 native outcomes/391 unique methods across API26/36, landscape and200%font.
That evidence is preserved, not retroactively attached to newer source.

## Additional source change requiring fresh qualification

A new viewer inserted into an already attached Activity can execute View.post
before first measure. Normalized page offset must wait for actual Recycler width,
not use1px as a fake basis. A removable layout listener now does that, gated by
closed state; no OCR/job/file write is introduced. The existing real-PDF round-trip
case now first waits for a drawn host Window and asserts restoration before the
new viewer's initial measure. Keep page/offset/query/five-float zoom/cache cleanup/
no-OCR-restart assertions. Two standalone native zoom cases and all389 baseline
methods remain;391 unique cases,1314 planned outcomes, no skipped/duplicate allowed.

Version increases to38; package/signature/min26/noINTERNET/default Vosk/trained speech
opt-in/OCR weights/storage codecs and original bytes stay unchanged. Final-byte
33→38 and35→38 upgrade smokes and actual-process recovery of the final APK are
still needed. A process probe of the earlier candidate37 remains independent and
cannot certify these new bytes. No uninstall/clear-data instructions.

Still pending: image OCR selection/review actual-process-death recovery, inline PDF
glyph/handle selection, vector-quality zoom redraw, exact Office layout fidelity,
universal emoji detection/recognition/unknown calibration, broad Indian-speaker
validation, and physical playback/startup/rotation acceptance. Do not mark these
complete from source, asset coverage, same-style fitting or emulator success.

## Probe harness navigation follow-up, not app recovery approval

Probe41469160310 failed in its initial opening phase: the Library popup was still
showing its scrollable first rows, before Open file/SAF selection. No process-kill
acceptance can be inferred. The harness now scrolls the actual Library menu to
reach Open file, keeps the real SAF path and Word choice, and includes missing
picker-target diagnostics. PID/state/source-byte assertions are unchanged. It can
also explicitly pin a future installed version; its currently pinned APK37 is
still not evidence for finalcode38. Host mocks verify scrolling/version guards,
not Android success. No production navigation behavior is changed by this fix.
