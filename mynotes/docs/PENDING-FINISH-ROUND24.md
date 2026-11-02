# Round24 — CSV local picker MIME compatibility; candidate0.36/code39

Not delivered or qualified yet. Last delivered stays0.32/code35.

## Concrete gap found by actual-process probe

Final38 run41476377220 passed its PDF and DOCX kill/relaunch assertions before
failing to select CSV in real SAF. Overall three-format acceptance is STILL false.
The earlier grid picker diagnostics omitted disabled nodes; visible DOCX/PDF do
not prove why CSV is absent. Nevertheless the production whitelist demonstrably
omits Android's standard `text/comma-separated-values` mapping for `.csv`.

Pinned AOSP Android14 sources:
- https://github.com/aosp-mirror/platform_frameworks_base/blob/android-14.0.0_r1/mime/java-res/android.mime.types
  line109: `?text/comma-separated-values csv`; its documented semantics override
  the extension-to-MIME mapping even though the MIME itself uses putIfAbsent.
- https://github.com/aosp-mirror/platform_frameworks_base/blob/android-14.0.0_r1/mime/java/android/content/type/DefaultMimeMapFactory.java
  loads Debian, Android, then vendor mappings in that order.

## Change and required qualification

Add text/comma-separated-values and application/csv alongside text/csv; retain all
previous supported MIME entries, CATEGORY_OPENABLE and EXTRA_LOCAL_ONLY. Reader
format routing still uses the supported original file name; no parsing, content
storage or original source mutation changes. New native LocalFilePickerTest
intercepts the ACTUAL picker Intent (returns Cancel; never selects a real file),
checks the running platform's CSV MIME and every previous format/local-only flag.

New version39/0.36 requires fresh Actions build/native qualification and exact-byte
33→39/35→39 upgrades plus three-format actual-process probe. Native SDK-only retry
now explicitly installs/validates Platform26 and GoogleAPIs x86_64 image, addressing
the proven pre-test ZIP failure; no app assertion retry/skip/reduction.

The probe now chooses SAF list presentation for three small fixtures and includes
disabled labeled nodes in future failures. This helper change does not trigger a
new APK38 probe; next run must deliberately pin final39 bytes once built.

## Preserved evidence and broader pending scope

38 source62c3c22b has1314/1314 planned native outcomes/391 distinct methods across
FAILED base41469717511 (1113 outcomes) plus SUCCESS original-byte201-method
supplement41474686572. Both exact33→38 and35→38 synthetic Note/Doc upgrades passed
41471791076. These results are not retroactive qualification for39.

Still pending: image-selector/OCR-review actual process-death restoration, direct
PDF glyph overlays, exact Office rendering, universal emoji OCR (separate-style
pilot only3/64), broad licensed Indian-speech validation, physical startup/playback
and user-device acceptance. No physical/public-release/360-minute claim. No
uninstall/clear-data; original files and internal user content remain prioritized.

## Exact39 bytes available; qualification still running

Build41478009279 succeeded, source986ebe50b2b0fb0da41fc5bd253d8d731527bd1c.
Raw39 artifact13160085024,155142488bytes, SHA256
`cab8b452dad05953b8265eee7b90fee8ec8f002ea69b31287bf4ed9970de0557`.
Host777PASS8.227s; native1318 planned outcomes/392 distinct methods, NOT yet passed.

The actual-process and dual33/35 install-over workflows are now pinned to these
exact39 bytes/source/version. They verify bytes/package/minSDK/no-INTERNET/common
signer before exercising the app. Successful small synthetic reports will be
validated and committed automatically to separate run-scoped evidence directories;
failed or partial probes cannot enter that success-only evidence step. Each bot
commit is host-gated and stays on the session branch. No raw APK/user data is
committed. This removes reliance on a later sandbox blob download/relay.

## Inspected final outcomes — do not deliver39

Actual-process41480207122 SUCCESS: report committed under process-recovery/
41480207122. PDF3825→5088 and DOCX5088→5850 retained query/page2; CSV5850→6392
retained query/row41; all three were STOPPED before kill and all original hashes
unchanged. No force-stop/uninstall/clear-data. This is synthetic API34 evidence,
not image/OCR review/zoom/horizontal/physical acceptance.

Upgrade41480206676 SUCCESS on both33→39 and35→39, committed under updates/
41480206676. Each checks the same two synthetic Note/Doc fixtures, retained install
identity, exact final bytes and common signer. Not exhaustive rich-data migration.

Native41478009279 FAILED:1317/1318 outcomes, seven passing profiles, Android36-A
200/201. The existing notesAndDocCreationRemainSeparateEvenAfterAnOldOptIn case
failed New-note IME readiness at CoreJourneysTest:564, with focused/attached body
and window focus but ime=false. All other planned outcomes passed; this is a real
assertion failure, NOT a pre-test SDK failure. No assertion retry/reduction allowed.
Native reports/screenshots are committed in its run directory. Candidate39 is
withheld from delivery despite the successful other checks.
