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
