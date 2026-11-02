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

## Final-byte process probe pin and stopped-state diagnostics

Code38 build41469717511 succeeded; raw artifact13155455677 is155142244bytes,
SHA256 `d0285d28318211854cb175bfb983f3f3c1f8180bc8d4106349ee2dd35c2ff3c7`,
source62c3c22b827c46dcc1a3c4af56c907f6f27015b2. Native qualification still pending.
The external probe is now pinned to those exact bytes and explicitly expects38.

Probe41470456918 reached the PDF viewer and configured page/query through actual
SAF, but timed out verifying stopped Activity state, BEFORE any accepted kill or
recovery. Its parser had required exactly one space in `* Hist #`; dumpsys can pad
that header. Accept whitespace padding but still require the same target Activity
and stopped signal; preserve filtered target-state observations if waiting fails.
This is harness hardening, not a claim that an app-state bug caused the timeout.
No test/PID/query/page/source-byte assertion is removed and no recovery success is
claimed until the new actual run is inspected.

## Exact-byte upgrade checks queued separately from native qualification

The update workflow now independently runs33→38 and35→38 on fresh, offline API34
emulators, using the same final raw38 artifact/hash/source above and the previously
qualified raw33/raw35 inputs. It verifies package/version/minSDK/no-INTERNET and
the common signing certificate before install-over. Neither path uninstalls or
clears data. Checks cover the existing two synthetic Note/Doc fixtures, not rich
libraries/audio/images/every migration or phone acceptance. libpulse and bounded
SDK-only preparation are explicit; migration assertions are never retried. Results
must be inspected before either path is called passed.
