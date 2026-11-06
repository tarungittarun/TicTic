## MyNotes candidate71 — full automation qualification

Actual completed-run results; not a claim that every possible behavior or both apps have been fully accepted. No public APK release approval. Physical-device, novice-use and exhaustive paired Obsidian acceptance remain open.

Workflow outcome: failure. Exact tested source: 3c1478b8c8b09d077d97b5481603387cbc47b06a. Candidate: 0.71.0 / versionCode 71; stable app.space.notes identity and release signing are checked separately below.

[Completed qualification run and individual jobs](https://github.com/tarungittarun/TicTic/actions/runs/42277337133)

[Original run metadata and job outcomes](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/run.json)

[Independently reparsed native outcome summary](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/summary.json)

Fresh local checks: root Python QA 1095 passed; MyNotes Python QA 457 passed. These are separately discovered suites, not additional native/device tests. Calculator and its paused workflow were not changed or built.

The native selection was expanded from eight selected methods per profile to all 446 discovered methods. There are 37 Java test classes; that number is coincidental and does not establish acceptance of the 37 paired UX task families.

Native executions passed: 2671 / 2676 planned across six profiles. Every profile uses two disjoint method shards. Missing, duplicate, skipped, failed and unexpected outcomes are rejected. Native result counts are reparsed from original instrumentation, not inferred from process exit zero.

## Build, JVM, release identity and signed install-over

Fresh domain JVM: 366/366; exact result ok=True. Failures: []

[Exact JVM method receipt](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-build-evidence/evidence/full-jvm.json)

build: success

[Job steps and actual conclusion](https://github.com/tarungittarun/TicTic/actions/runs/42277337133/job/128096962104)

update (26, default): success

[Job steps and actual conclusion](https://github.com/tarungittarun/TicTic/actions/runs/42277337133/job/128100858946)

update (34, google_apis): success

[Job steps and actual conclusion](https://github.com/tarungittarun/TicTic/actions/runs/42277337133/job/128100859050)

Internal release APK: 155196632 bytes; SHA256 a7d6f685992c68fe4ffc7f3a5705d0514986c9d9fd82ca67c6f68d392ca41de3. Permissions: android.permission.RECORD_AUDIO, android.permission.POST_NOTIFICATIONS, android.permission.RECEIVE_BOOT_COMPLETED. No INTERNET permission is allowed. Explicit Android backup disabling, minimum API26, non-debuggable release, stable package/certificate and 156,213,948-byte size cap are enforced.

[Release byte identity and manifest audit](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-build-evidence/evidence/release-audit.json)

Internal release APK: 155196632 bytes; SHA256 a7d6f685992c68fe4ffc7f3a5705d0514986c9d9fd82ca67c6f68d392ca41de3. Permissions: android.permission.RECORD_AUDIO, android.permission.POST_NOTIFICATIONS, android.permission.RECEIVE_BOOT_COMPLETED. No INTERNET permission is allowed. Explicit Android backup disabling, minimum API26, non-debuggable release, stable package/certificate and 156,213,948-byte size cap are enforced.

[Release byte identity and manifest audit](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-build-evidence/quality/release-audit.json)

full71-release69-to71-api26-rich-public-update-evidence: ok=True; uninstall/clear=False. Public generated rich repository payloads only; not real microphone files, SAF, rich UI rendering or an exhaustive user library.

[Original signed install-over proof](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-release69-to71-api26-rich-public-update-evidence/release-rich-update-proof.json)

full71-release69-to71-api34-rich-public-update-evidence: ok=True; uninstall/clear=False. Public generated rich repository payloads only; not real microphone files, SAF, rich UI rendering or an exhaustive user library.

[Original signed install-over proof](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-release69-to71-api34-rich-public-update-evidence/release-rich-update-proof.json)

Install-over probes use a disposable public generated rich library, signed release69 followed by release71 with adb install -r. They check old-writer snapshots, private folders, installation identity and candidate write/readback. They do not prove physical SAF providers, real microphone files, every user-library history or UI rendering fidelity. Internal CI APK artifacts are not a public release.

## Completed corrections and isolated rechecks — original score unchanged

Completed recheck: 218 of 221 selected executions passed. API26 full shard passes; three unchanged navigation/large-text checks reproduce their failures. The original 2671/2676 full-run score is not overwritten.

| Recheck profile / scope | Passed / selected | Observed outcome |
|---|---|---|
| api26-portrait | 218/218 | Full owning shard 218/218; PDF observer correction verified; unchanged spreadsheet mismatch not reproduced. |
| api34-large | 0/2 | Both unchanged large-text layout checks fail again. |
| api36-portrait | 0/1 | Unchanged repeated workspace-creation method fails again in isolation. |

[Original rerun assertions: api26-portrait](https://github.com/tarungittarun/TicTic/blob/62aa26bfd5eb669b22e379e5acf2222acb8eb171/mynotes/docs/ux-improvement-ledger/implementations/42282354356/improvement71-api26-portrait/refinement-native.txt)

[Exact independently checked receipt](https://github.com/tarungittarun/TicTic/blob/62aa26bfd5eb669b22e379e5acf2222acb8eb171/mynotes/docs/ux-improvement-ledger/implementations/42282354356/improvement71-api26-portrait/refinement-native-verified.json)

[Original rerun assertions: api34-large](https://github.com/tarungittarun/TicTic/blob/62aa26bfd5eb669b22e379e5acf2222acb8eb171/mynotes/docs/ux-improvement-ledger/implementations/42282354356/improvement71-api34-large/refinement-native.txt)

[Exact independently checked receipt](https://github.com/tarungittarun/TicTic/blob/62aa26bfd5eb669b22e379e5acf2222acb8eb171/mynotes/docs/ux-improvement-ledger/implementations/42282354356/improvement71-api34-large/refinement-native-verified.json)

[Original rerun assertions: api36-portrait](https://github.com/tarungittarun/TicTic/blob/62aa26bfd5eb669b22e379e5acf2222acb8eb171/mynotes/docs/ux-improvement-ledger/implementations/42282354356/improvement71-api36-portrait/refinement-native.txt)

[Exact independently checked receipt](https://github.com/tarungittarun/TicTic/blob/62aa26bfd5eb669b22e379e5acf2222acb8eb171/mynotes/docs/ux-improvement-ledger/implementations/42282354356/improvement71-api36-portrait/refinement-native-verified.json)

Do not add these rerun counts to the original six-profile result as new unique coverage. No production implementation changed in this check phase. The PDF observer correction is instrumentation-only; it is not a new app UX fix. Three unresolved checks reproduce in isolation; the original spreadsheet discrepancy is not reproduced, not explained away or marked fixed.

42280357507: Observer-build JVM 366 passed, but the cross-build debug launch produced no native outcomes (0/218). This is blocked native setup, not 218 new app failures. Its failed receipts and original output are retained.

[Preserved attempt](https://github.com/tarungittarun/TicTic/actions/runs/42280357507)

42281432422: First isolated launch returned Activity Manager help and no native outcomes (0/3); no reproduction inferred. Original output retained.

[Preserved attempt](https://github.com/tarungittarun/TicTic/actions/runs/42281432422)

42282043388: Superseded recheck configuration used a shared artifact name with overwrite enabled. Do not use it as complete independent multi-profile retained evidence. The final 42282354356 run uses unique artifact names; original job history remains visible.

[Preserved attempt](https://github.com/tarungittarun/TicTic/actions/runs/42282043388)

Debug-pair provenance: original app SHA256 a451554c4b4bd1645fb4fc2f8da176c89822bfdb7a5476fccb13f4bb636ea228; used app SHA256 6c1a9de6bbdf7261b49bfa8712eecf2d7480a03126762f8efb857e75ce0ab08c. App/test certificate matches=True; all unsigned ZIP payload entries equal=False. Changed unsigned entries: ['lib/arm64-v8a/libnotes_whisper.so', 'lib/armeabi-v7a/libnotes_whisper.so', 'lib/x86_64/libnotes_whisper.so']. Different signatures are not byte-identical APKs. User release signing and release APKs were not changed.

[Actual debug payload and certificate proof](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/recheck-apk-identity.json)

These are source-equivalent rebuilds, not exact original-APK retests. The entry comparison finds three differing native Whisper shared libraries; it does not justify pooling cross-build speech speed/accuracy or declaring the binaries identical. No production source or model weights changed in this check phase.

## Five actual failed checks — triage, not waived

FQ-0001 | API26 light portrait, shard1 | After rotation, expected query Needle but observed VisibleNeedle. Original read-only/source-preservation journey failed its query equality check.

Classification: unresolved state/IME or fixture-interaction failure; exclusive app root cause not established. Status: not_reproduced_on_full_shard_rerun_original_failure_still_open.

Completed recheck: Corrected-launch run 42282354356: all218 owning-shard methods passed, including this unchanged spreadsheet assertion. No production app code was changed. This is non-reproduction, not proof of an app fix or an exclusive original cause.

Next discriminating check: Isolate prior Home search text/composition, fixture startup ownership and rotation sequencing on API26. Preserve the original VisibleNeedle observation until its cause is established.

[Original failed method: app.space.notes.RealFolderStorageTest#spreadsheetRotationKeepsViewerQueryAndReturnsToHomeWithoutImport](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api26-light-portrait-s1/refinement-native.txt)

FQ-0002 | API26 light portrait, shard1 | NoClassDefFoundError: android.view.WindowInsets$Type. The test observer called an API30-only platform class on API26 before completing the actual keyboard/search check.

Classification: confirmed incompatible test observer, not an app crash. Status: observer_correction_native_retest_passed_not_an_app_fix.

Completed recheck: The corrected API26 observer executed the actual composed-keyboard pixels, injected query/Enter, glyph highlighting, outside-tap deselection and original-byte assertions successfully in the 218/218 full-shard rerun. Primary-run NoClassDefFoundError remains preserved.

Next discriminating check: Keep API26 and API30+ observer branches in future whole-inventory qualification; do not count this harness correction as a production UX fix.

[Original failed method: app.space.notes.runtime.PdfViewerInteractionTest#keyboardSearchActionFindsAndHighlightsEmbeddedText](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api26-light-portrait-s1/refinement-native.txt)

FQ-0003 | API36 light portrait, shard1; app animation scale1 | A repeated actual Doc-to-Notes workspace creation journey could not find the named New document control after a swipe.

Classification: reproduced workspace creation/navigation failure; exclusive gesture, transition or fixture cause still not isolated. Status: reproduced_in_isolation_unresolved.

Completed recheck: Run42282354356, isolated unchanged method: 0/1; same Missing named control: New document failure with API36 app motion 1. Not merely explained away as a full-suite-only interaction.

Next discriminating check: Capture the exact failed cycle and actual active workspace/card bounds after each real swipe, then distinguish unacknowledged navigation from a prematurely queried control. Preserve all repeated creation/content/IME assertions and protect fast Back/save safety.

[Original failed method: app.space.notes.CoreJourneysTest#notesAndDocCreationRemainSeparateEvenAfterAnOldOptIn](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api36-light-portrait-s1/refinement-native.txt)

FQ-0004 | API34 dark portrait, font scale2.0, shard1 | The image preview occupied less than the existing required 35 percent of the dialog. A retained large-text screenshot from another selection journey also shows large fixed chrome, a relatively small image and an ellipsized Keep image action.

Classification: large-text layout regression; screenshot-reviewed, exact exclusive mechanism not isolated. Status: reproduced_in_isolated_large_text_probe_unfixed.

Completed recheck: Run42282354356: both unchanged large-text assertions failed again in a two-method probe at API34/dark/portrait/font 2.0/motion 0. Original numeric Settings result 168 total/112 visible and image-area 35-percent threshold were retained.

Next discriminating check: Implement a measured compact/scrollable secondary-control budget preserving system font scaling, both primary actions, all 48dp targets, the same selection/crop/bitmap and real geometry threshold. Require changed-build qualification before claiming fixed.

[Original failed method: app.space.notes.runtime.ImageSelectionTest#selectionViewportRetainsUsefulAreaInPortraitAndLandscape](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-large-dark-s1/refinement-native.txt)

FQ-0005 | API34 dark portrait, font scale2.0, shard1 | The final Settings touch row was only 112 pixels globally visible out of its 168-pixel row height. Settings remains last and Folders penultimate, but the initial large-text Library viewport clips the bottom of the final target.

Classification: large-text initial-menu layout regression; screenshot-reviewed, not proof Settings is impossible to reach by scrolling. Status: reproduced_in_isolated_large_text_probe_unfixed.

Completed recheck: Run42282354356: both unchanged large-text assertions failed again in a two-method probe at API34/dark/portrait/font 2.0/motion 0. Original numeric Settings result 168 total/112 visible and image-area 35-percent threshold were retained.

Next discriminating check: Reconcile actual scaled row/chrome measurement with the Library height cap without shrinking fonts/targets, changing Settings/Folders order, adding duplicate destinations or weakening the assertion.

[Original failed method: app.space.notes.CoreJourneysTest#wholeAppLibraryTraversalRecordsEachVisitedPage](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-large-dark-s1/refinement-native.txt)

## FQ-0004 — reviewed large-text capture

Actual retained screenshot from another journey on this same large-text profile; not a failure-instant capture or changed-build proof. Original pixels are unaltered. The image preview occupied less than the existing required 35 percent of the dialog. A retained large-text screenshot from another selection journey also shows large fixed chrome, a relatively small image and an ellipsized Keep image action.

[Open original screenshot](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-large-dark-s1/captures/image-text-selection.png)

## FQ-0005 — reviewed large-text capture

Actual retained screenshot from another journey on this same large-text profile; not a failure-instant capture or changed-build proof. Original pixels are unaltered. The final Settings touch row was only 112 pixels globally visible out of its 168-pixel row height. Settings remains last and Folders penultimate, but the initial large-text Library viewport clips the bottom of the final target.

[Open original screenshot](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-large-dark-s0/captures/e2e-settings-library.png)

## Every native profile and exact method results

| Profile shard | Passed / expected | Exact native result |
|---|---|---|
| api26-light-portrait-s0 | 228/228 | True |
| api26-light-portrait-s1 | 216/218 | False |
| api34-dark-landscape-s0 | 228/228 | True |
| api34-dark-landscape-s1 | 218/218 | True |
| api34-dark-portrait-s0 | 228/228 | True |
| api34-dark-portrait-s1 | 218/218 | True |
| api34-large-dark-s0 | 228/228 | True |
| api34-large-dark-s1 | 216/218 | False |
| api34-light-landscape-s0 | 228/228 | True |
| api34-light-landscape-s1 | 218/218 | True |
| api36-light-portrait-s0 | 228/228 | True |
| api36-light-portrait-s1 | 217/218 | False |

Profiles: API26 light portrait; API36 light portrait; API34 dark portrait; API34 dark portrait at font scale 2.0; API34 light landscape; API34 dark landscape. Nexus 5 synthetic x86_64 guests. API26 uses the default image; API34/36 use Google APIs images. These are not six physical phones or a universal device matrix.

Airplane mode, disabled Wi-Fi/data and absence of a default network are verified before installation. Microphone runtime permission is granted only on API36; other profiles exercise denied state. Fixtures are public/synthetic. API36 app animator scale is 1; other native profiles use deterministic scale 0. This does not measure comparative animation quality, jank, typing latency or human efficiency.

[full71-native-api26-light-portrait-s0 — original assertions](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api26-light-portrait-s0/refinement-native.txt)

[full71-native-api26-light-portrait-s1 — original assertions](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api26-light-portrait-s1/refinement-native.txt)

FAILED: app.space.notes.RealFolderStorageTest#spreadsheetRotationKeepsViewerQueryAndReturnsToHomeWithoutImport: status -2 org.junit.ComparisonFailure: expected:<[]Needle> but was:<[Visible]Needle>
	at org.junit.Assert.assertEquals(Assert.java:117)
	at org.junit.Assert.assertEquals(Assert.java:146)
	at app.space.notes.RealFolderStorageTest.lambda$spreadsheetRotationKeepsViewerQueryAndReturnsToHomeWithoutImport$54$app-space-notes-RealFolderStorageTest(RealFolderStorageTest.java:184)
	at app.space.notes.RealFolderStorageTest$$ExternalSyntheticLambda25.run(D8$$SyntheticClass:0)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:457)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at android.app.Instrumentation$SyncRunnable.run(Instrumentation.java:2092)
	at android.os.Handler.handleCallback(Handler.java:789)
	at android.os.Handler.dispatchMessage(Handler.java:98)
	at android.os.Looper.loop(Looper.java:164)
	at android.app.ActivityThread.main(ActivityThread.java:6541)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.Zygote$MethodAndArgsCaller.run(Zygote.java:240)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:767)


FAILED: app.space.notes.runtime.PdfViewerInteractionTest#keyboardSearchActionFindsAndHighlightsEmbeddedText: status -2 java.lang.NoClassDefFoundError: Failed resolution of: Landroid/view/WindowInsets$Type;
	at app.space.notes.runtime.PdfViewerInteractionTest.lambda$keyboardSearchActionFindsAndHighlightsEmbeddedText$28$app-space-notes-runtime-PdfViewerInteractionTest(PdfViewerInteractionTest.java:78)
	at app.space.notes.runtime.PdfViewerInteractionTest$$ExternalSyntheticLambda45.call(D8$$SyntheticClass:0)
	at app.space.notes.runtime.PdfViewerInteractionTest.lambda$await$2(PdfViewerInteractionTest.java:29)
	at app.space.notes.runtime.PdfViewerInteractionTest$$ExternalSyntheticLambda27.run(D8$$SyntheticClass:0)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:457)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at android.app.Instrumentation$SyncRunnable.run(Instrumentation.java:2092)
	at android.os.Handler.handleCallback(Handler.java:789)
	at android.os.Handler.dispatchMessage(Handler.java:98)
	at android.os.Looper.loop(Looper.java:164)
	at android.app.ActivityThread.main(ActivityThread.java:6541)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.Zygote$MethodAndArgsCaller.run(Zygote.java:240)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:767)
Caused by: java.lang.ClassNotFoundException: Didn't find class "android.view.WindowInsets$Type" on path: DexPathList[[zip file "/system/framework/android.test.runner.jar", zip file "/data/app/app.space.notes.debug.test-uROFeZx2JiSrbQ9mylOMZQ==/base.apk", zip file "/data/app/app.space.notes.debug-Sygniu_kSIGKHAZUzIgSpw==/base.apk"],nativeLibraryDirectories=[/data/app/app.space.notes.debug.test-uROFeZx2JiSrbQ9mylOMZQ==/lib/x86_64, /data/app/app.space.notes.debug-Sygniu_kSIGKHAZUzIgSpw==/lib/x86_64, /data/app/app.space.notes.debug.test-uROFeZx2JiSrbQ9mylOMZQ==/base.apk!/lib/x86_64, /data/app/app.space.notes.debug-Sygniu_kSIGKHAZUzIgSpw==/base.apk!/lib/x86_64, /system/lib64, /vendor/lib64]]
	at dalvik.system.BaseDexClassLoader.findClass(BaseDexClassLoader.java:93)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:379)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:312)
	... 14 more


Methods not passed (including recorded failures): app.space.notes.RealFolderStorageTest#spreadsheetRotationKeepsViewerQueryAndReturnsToHomeWithoutImport, app.space.notes.runtime.PdfViewerInteractionTest#keyboardSearchActionFindsAndHighlightsEmbeddedText

[full71-native-api34-dark-landscape-s0 — original assertions](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-dark-landscape-s0/refinement-native.txt)

[full71-native-api34-dark-landscape-s1 — original assertions](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-dark-landscape-s1/refinement-native.txt)

[full71-native-api34-dark-portrait-s0 — original assertions](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-dark-portrait-s0/refinement-native.txt)

[full71-native-api34-dark-portrait-s1 — original assertions](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-dark-portrait-s1/refinement-native.txt)

[full71-native-api34-large-dark-s0 — original assertions](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-large-dark-s0/refinement-native.txt)

[full71-native-api34-large-dark-s1 — original assertions](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-large-dark-s1/refinement-native.txt)

FAILED: app.space.notes.runtime.ImageSelectionTest#selectionViewportRetainsUsefulAreaInPortraitAndLandscape: status -2 java.lang.AssertionError: Image preview must not be a strip between fixed controls
	at org.junit.Assert.fail(Assert.java:89)
	at org.junit.Assert.assertTrue(Assert.java:42)
	at app.space.notes.runtime.ImageSelectionTest.lambda$selectionViewportRetainsUsefulAreaInPortraitAndLandscape$49$app-space-notes-runtime-ImageSelectionTest(ImageSelectionTest.java:65)
	at app.space.notes.runtime.ImageSelectionTest$$ExternalSyntheticLambda2.run(D8$$SyntheticClass:0)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:487)
	at java.util.concurrent.FutureTask.run(FutureTask.java:264)
	at android.app.Instrumentation$SyncRunnable.run(Instrumentation.java:2420)
	at android.os.Handler.handleCallback(Handler.java:958)
	at android.os.Handler.dispatchMessage(Handler.java:99)
	at android.os.Looper.loopOnce(Looper.java:205)
	at android.os.Looper.loop(Looper.java:294)
	at android.app.ActivityThread.main(ActivityThread.java:8177)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:552)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:971)


FAILED: app.space.notes.CoreJourneysTest#wholeAppLibraryTraversalRecordsEachVisitedPage: status -2 java.lang.AssertionError: The full final Settings touch row is visible expected:<168> but was:<112>
	at org.junit.Assert.fail(Assert.java:89)
	at org.junit.Assert.failNotEquals(Assert.java:835)
	at org.junit.Assert.assertEquals(Assert.java:647)
	at app.space.notes.CoreJourneysTest.lambda$wholeAppLibraryTraversalRecordsEachVisitedPage$13$app-space-notes-CoreJourneysTest(CoreJourneysTest.java:47)
	at app.space.notes.CoreJourneysTest$$ExternalSyntheticLambda518.run(D8$$SyntheticClass:0)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:487)
	at java.util.concurrent.FutureTask.run(FutureTask.java:264)
	at android.app.Instrumentation$SyncRunnable.run(Instrumentation.java:2420)
	at android.os.Handler.handleCallback(Handler.java:958)
	at android.os.Handler.dispatchMessage(Handler.java:99)
	at android.os.Looper.loopOnce(Looper.java:205)
	at android.os.Looper.loop(Looper.java:294)
	at android.app.ActivityThread.main(ActivityThread.java:8177)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:552)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:971)


Methods not passed (including recorded failures): app.space.notes.CoreJourneysTest#wholeAppLibraryTraversalRecordsEachVisitedPage, app.space.notes.runtime.ImageSelectionTest#selectionViewportRetainsUsefulAreaInPortraitAndLandscape

[full71-native-api34-light-landscape-s0 — original assertions](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-light-landscape-s0/refinement-native.txt)

[full71-native-api34-light-landscape-s1 — original assertions](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-light-landscape-s1/refinement-native.txt)

[full71-native-api36-light-portrait-s0 — original assertions](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api36-light-portrait-s0/refinement-native.txt)

[full71-native-api36-light-portrait-s1 — original assertions](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api36-light-portrait-s1/refinement-native.txt)

FAILED: app.space.notes.CoreJourneysTest#notesAndDocCreationRemainSeparateEvenAfterAnOldOptIn: status -2 java.lang.AssertionError: Missing named control: New document
	at org.junit.Assert.fail(Assert.java:89)
	at org.junit.Assert.assertTrue(Assert.java:42)
	at org.junit.Assert.assertNotNull(Assert.java:713)
	at app.space.notes.CoreJourneysTest.lambda$click$94$app-space-notes-CoreJourneysTest(CoreJourneysTest.java:193)
	at app.space.notes.CoreJourneysTest$$ExternalSyntheticLambda674.run(D8$$SyntheticClass:0)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:524)
	at java.util.concurrent.FutureTask.run(FutureTask.java:317)
	at android.app.Instrumentation$SyncRunnable.run(Instrumentation.java:2616)
	at android.os.Handler.handleCallback(Handler.java:995)
	at android.os.Handler.dispatchMessage(Handler.java:103)
	at android.os.Looper.loopOnce(Looper.java:248)
	at android.os.Looper.loop(Looper.java:338)
	at android.app.ActivityThread.main(ActivityThread.java:9067)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:593)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:932)


Methods not passed (including recorded failures): app.space.notes.CoreJourneysTest#notesAndDocCreationRemainSeparateEvenAfterAnOldOptIn

## Functional automation inventory, not UX ratings

| Internal test area | Unique methods | Passed executions / planned |
|---|---|---|
| Notes and checklists | 66 | 396/396 |
| Rich Doc and tables | 53 | 317/318 |
| Diagrams | 23 | 138/138 |
| Read image / OCR | 48 | 287/288 |
| Voice inbox and transcripts | 35 | 210/210 |
| Images and drawing | 19 | 114/114 |
| File viewers | 42 | 251/252 |
| Home, search and Library | 19 | 113/114 |
| Folders and storage | 46 | 275/276 |
| Backup, history and Trash | 67 | 402/402 |
| Reminders | 8 | 48/48 |
| Settings and diagnostics | 20 | 120/120 |

This categorisation groups existing test methods for navigation; it is not a new module-ordered run or a matched Obsidian experiment. The full original method inventories and outputs are retained per shard. File formats, rich text, persistence, folders, Trash, permissions, OCR, speech, playback state, exports, selection, diagrams and UI journeys are checked only to the extent of those named methods and fixtures.

Quantitative controlled OCR and speech-degradation reports are independently rescored in CI when their owning method is selected. Exported PDF link annotations and geometry are parsed independently using pypdf in shard0. The reports below are raw observations, not a general OCR/audio accuracy or speed ranking.

[Quantitative public-fixture report: full71-native-api26-light-portrait-s0/refinement-quality-speech-degradation-report.json](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api26-light-portrait-s0/refinement-quality-speech-degradation-report.json)

[Quantitative public-fixture report: full71-native-api26-light-portrait-s1/refinement-quality-ocr-common-text-report.json](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api26-light-portrait-s1/refinement-quality-ocr-common-text-report.json)

[Quantitative public-fixture report: full71-native-api34-dark-landscape-s0/refinement-quality-speech-degradation-report.json](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-dark-landscape-s0/refinement-quality-speech-degradation-report.json)

[Quantitative public-fixture report: full71-native-api34-dark-landscape-s1/refinement-quality-ocr-common-text-report.json](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-dark-landscape-s1/refinement-quality-ocr-common-text-report.json)

[Quantitative public-fixture report: full71-native-api34-dark-portrait-s0/refinement-quality-speech-degradation-report.json](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-dark-portrait-s0/refinement-quality-speech-degradation-report.json)

[Quantitative public-fixture report: full71-native-api34-dark-portrait-s1/refinement-quality-ocr-common-text-report.json](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-dark-portrait-s1/refinement-quality-ocr-common-text-report.json)

[Quantitative public-fixture report: full71-native-api34-large-dark-s0/refinement-quality-speech-degradation-report.json](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-large-dark-s0/refinement-quality-speech-degradation-report.json)

[Quantitative public-fixture report: full71-native-api34-large-dark-s1/refinement-quality-ocr-common-text-report.json](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-large-dark-s1/refinement-quality-ocr-common-text-report.json)

[Quantitative public-fixture report: full71-native-api34-light-landscape-s0/refinement-quality-speech-degradation-report.json](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-light-landscape-s0/refinement-quality-speech-degradation-report.json)

[Quantitative public-fixture report: full71-native-api34-light-landscape-s1/refinement-quality-ocr-common-text-report.json](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-light-landscape-s1/refinement-quality-ocr-common-text-report.json)

[Quantitative public-fixture report: full71-native-api36-light-portrait-s0/refinement-quality-speech-degradation-report.json](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api36-light-portrait-s0/refinement-quality-speech-degradation-report.json)

[Quantitative public-fixture report: full71-native-api36-light-portrait-s1/refinement-quality-ocr-common-text-report.json](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api36-light-portrait-s1/refinement-quality-ocr-common-text-report.json)

[Independent PDF verification: full71-native-api26-light-portrait-s0](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api26-light-portrait-s0/refinement-pdf-results.json)

[Independent PDF verification: full71-native-api34-dark-landscape-s0](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-dark-landscape-s0/refinement-pdf-results.json)

[Independent PDF verification: full71-native-api34-dark-portrait-s0](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-dark-portrait-s0/refinement-pdf-results.json)

[Independent PDF verification: full71-native-api34-large-dark-s0](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-large-dark-s0/refinement-pdf-results.json)

[Independent PDF verification: full71-native-api34-light-landscape-s0](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api34-light-landscape-s0/refinement-pdf-results.json)

[Independent PDF verification: full71-native-api36-light-portrait-s0](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/full71-native-api36-light-portrait-s0/refinement-pdf-results.json)

## What remains unaccepted — no misleading “everything passed”

1. All 37 paired task families still require wider matched installed-app observations against official Obsidian 1.15.0. Prior bounded comparison evidence remains valid only for its stated versions/tasks; this run is MyNotes-only. Extra MyNotes features are not comparative score advantages.

2. The two implemented improvements remain partially verified. Search-passage provenance/highlighting and compact Format regressions are included in the full inventory. This does not establish similar-note discovery at scale, all rich-content variants, TalkBack, novice discoverability or universal three-line keyboard-open writing context. Earlier landscape captures without the keyboard are not keyboard-open landscape acceptance.

3. Real-device touch/selection/reorder feel, large text, keyboard transitions, microphone/playback quality, physical provider persistence, accessibility and install-over on the user device remain separate acceptance tasks. No private content or recording was accessed.

4. Real-world multilingual/tiny/damaged/image-scene OCR, broad speech accents/noise and held-out ground-truth benchmarks beyond these native fixtures remain unaccepted on candidate71. No model training or cloud inference was performed. Passing bundled-fixture tests is not a general accuracy claim.

5. Human task time, efficiency, frame jank, battery, startup-to-ready, large-library responsiveness, all malformed-file/security inputs, every migration history and all OS/vendor combinations are not exhausted by this suite. The retained failures below, if any, are not waived by other passes.

This completes the recorded automated check scope only when the completed run says success; it cannot honestly close physical or exhaustive two-app UX acceptance. The existing execution ledger remains ongoing rather than marking untested tasks complete.

| Paired task family | Remaining acceptance task |
|---|---|
| PLAN-X01 / Find, organise, label and reorder | Find an archived body-only match while a folder filter is active; reorder a distant card, then return. |
| PLAN-X02 / Writing, reading and formatting | Format the middle of a paragraph, type new text, undo, dismiss the keyboard and reopen the note. |
| PLAN-X03 / Checklists, numbering and list editing | Insert an item in the middle, indent it, move it past the viewport, hide completed rows and undo. |
| PLAN-X04 / Tables and direct cell editing | Edit a long cell in a wide table; insert a column beside it, toggle header role, delete a row and undo. |
| PLAN-X05 / Images, attachments and inline media | Insert a photo, select two recognized words, keep the image, edit its crop, then remove only the intended attachment. |
| PLAN-X06 / Web hyperlinks and external opening | Add a long URL to selected words, correct a malformed address, reopen, edit and remove the link. |
| PLAN-X07 / Internal links, backlinks, graph and previews | Link A to a heading in B, rename B, return through backlinks and preview the target without losing A. |
| PLAN-X08 / Long-note orientation, tags, footnotes and word count | Locate a middle heading, find a repeated phrase, follow a footnote and return to the exact reading location. |
| PLAN-X09 / Properties and Bases | For users who need records, edit one typed field, filter a view, rename a file and reopen the view. |
| PLAN-X10 / Quick capture, daily notes, templates and unique notes | From a cold start, capture a short thought; then create a repeated meeting structure and return tomorrow. |
| PLAN-X11 / Freehand drawing and Canvas | Draw a diagram annotation, leave and reopen it; in Canvas connect two cards, pan/zoom and reopen the board. |
| PLAN-X12 / Diagrams, shapes, arrows and grouping | Add two shapes, connect them, group, resize, split the arrow, undo and reopen. |
| PLAN-X13 / PDF reading, selection, search and markup | Open a large scanned/mixed PDF, zoom a distant page, select offscreen text, find a hit with IME, mark a rotated page and export. |
| PLAN-X14 / Word files and original-layout expectations | Open a DOCX with headers, inline images, wide tables and hard page breaks; select text, zoom and create an editable copy. |
| PLAN-X15 / Large TXT and read-only text viewing | Open a large multilingual TXT, find a repeated phrase near the end, select across the viewport and return to the prior file list. |
| PLAN-X16 / Spreadsheet previews and cached values | Open quoted CSV and a multi-sheet XLSX with formulas, missing cached results and wide cells; locate a value and copy it. |
| PLAN-X17 / Markdown, HTML, RTF, ODT and slide imports | Open one realistic file of each advertised type, inspect text order and unsupported elements, then cancel or create a copy. |
| PLAN-X18 / OCR, image-region text and scanned-page recognition | Read tiny/oblique/multilingual public scenes, blank controls and selected image regions; compare exact expected text and corrected output. |
| PLAN-X19 / Voice recording, playback and transcription | Record varied Indian speakers, pause/resume, deny/regrant permission, play through a headset and correct names/numbers while listening. |
| PLAN-X20 / Reminders and notifications | Set, rename and reschedule a reminder, deny notifications, reboot and then tap the delivered notification. |
| PLAN-X21 / Home widget and Android entry points | Place/resize the widget, pin and archive notes, share text from another app and open a reminder deep link. |
| PLAN-X22 / Appearance, backgrounds, fonts and accessibility | Switch system theme, increase font scale, navigate with TalkBack and format selected text over a patterned background. |
| PLAN-X23 / First-use storage, private folders and optional App folder | Create notes offline, choose an empty optional folder, revoke access, save again and reconnect without clearing app data. |
| PLAN-X24 / Portable backup, import merge and full recovery | Export a near-limit rich library, exceed the limit safely, import identical and same-ID changed notes, then verify every supported content type. |
| PLAN-X25 / Trash, permanent deletion and recovery | Delete/restore a rich note, inspect what happens to its audio and exports, then purge only a disposable selected note. |
| PLAN-X26 / Archive, pinning and bookmarks | Pin a note, archive it, find it globally, restore it and compare a saved Obsidian bookmark task. |
| PLAN-X27 / Undo, redo, editing history and save feedback | Edit text/table/image, undo and redo, leave and reopen; repeat after controlled process restart on disposable data. |
| PLAN-X28 / Workspaces, tabs and mobile navigation | Switch between two notes, a PDF and Settings; reorder content near an edge, then return to the initial editing location. |
| PLAN-X29 / Settings, commands, quick actions and help discovery | Without a walkthrough, find move, undo, export, a setting and one uncommon editor action using touch alone. |
| PLAN-X30 / Export, sharing, duplication and note composition | Share a mixed note, export PDF and JSON, duplicate it, merge selected notes and undo the merge. |
| PLAN-X31 / Local summary and text assistance | Summarize varied notes with negation, dates and multilingual sentences; check the retained source and decide whether the result is useful. |
| PLAN-X32 / Privacy, permission boundaries and diagnostic logs | Deny microphone, use notes normally, enable logs for a dummy issue, filter/export/delete and inspect the resulting local file. |
| PLAN-X33 / Smoothness, speed, footprint and reliability | On matched hardware, cold start, create/type, search a large library, drag a distant card, open a large PDF and recover from a controlled failure. |
| PLAN-X34 / Core modules, community plugins and extensibility | Find a core module, understand its description, enable/disable it and identify changed controls; inspect restricted/plugin trust text without installing unknown code. |
| PLAN-X35 / Sync, Publish, account and service boundaries | For a future explicitly authorised service study, evaluate offline edits, conflicts, account loss, export and deliberate public visibility with disposable data. |
| PLAN-X36 / Slides, presentation and reading projections | Present a multi-slide disposable note, navigate, dismiss the keyboard, rotate and return to editing; compare PDF reading only as a separate output route. |
| PLAN-X37 / About, models, licences and honest help | Ask a novice to identify where notes are saved, how to protect audio and how to export useful diagnostics without sending private notes. |

## Evidence preservation and reproducibility

Original native logs, receipts, environment snapshots, public screenshots, quantitative reports, JVM XML and migration proofs are retained byte-for-byte where present. A missing receipt remains missing. The hash manifest records actual retained bytes. APKs/models/dependencies are excluded from Git. Capture dimensions vary by helper: some fixtures are resized to 540px before artifact creation; others use full-window PixelCopy. Retention does not reconstruct or alter pixels.

[Original-file SHA256 retention manifest](https://github.com/tarungittarun/TicTic/blob/7c371ccf5ea0f250e3a7e1c585c0015459216ea9/mynotes/docs/full-qualification/42277337133/retention.json)

[Exact qualification workflow source](https://github.com/tarungittarun/TicTic/blob/3c1478b8c8b09d077d97b5481603387cbc47b06a/.github/workflows/notes-full71.yml)

[Previous bounded improvement ledger — not superseded as paired evidence](https://github.com/tarungittarun/TicTic/blob/211fac005aa0f2fc8083aec64c879506efdbce10/mynotes/docs/ux-improvement-ledger/Improvement_Execution_Ledger.md)
