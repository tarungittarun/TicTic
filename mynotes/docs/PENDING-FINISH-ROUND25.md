# Round25 — current-target IME request acknowledgement; candidate0.37/code40

Not delivered or qualified. Last delivered remains0.32/code35.

39's actual-process PDF/DOCX/CSV and both33/35 upgrade checks passed, but its native
Android36-A New-note IME assertion failed (1317/1318 total). Preserve that failure;
never relabel it green or just retry the same assertion for a lucky pass.

Source audit found newNote's bounded startup loop set requestedForTarget=true
UNCONDITIONALLY after showSoftInput, even if Android rejected that request, and
could then accept still-visible old-Doc insets. Now retain the real boolean return
and require InputMethodManager.isActive(target) before visible IME can finish the
loop. Existing closed/current-body/attached/focus guards,10 attempts/100ms spacing
and native repeated-transition/content/IME assertions remain unchanged. No longer
wait or arbitrary delay was added. Failure diagnostics also expose the boolean
active-for-current-body state, never user text.

This is concrete source hardening, NOT proof that the failed run's exact root
cause is established or fixed. Fresh full native qualification is required. Source
host checks are not an IME emulator or physical acceptance substitute. Code40 must
also get its OWN exact-byte process and33/35 upgrade smokes, not reuse39's results.

Preserve CSV MIME aliases/local-only flags, PDF first-layout width guard, on-device
OCR review/copy/source preservation, stable signing/package and all user content.
Broader pending: image/OCR actual-process recovery, direct glyph overlays, exact
Office fidelity, universal emoji OCR, broad licensed Indian-speech validation,
physical playback/startup and device acceptance. No public release or360-minute
completion claim.
