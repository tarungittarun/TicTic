# MyNotes report card — 2026-10-28 (IST)

## Current accepted UI/diagnostics milestone: 0.8.0(9)

[**Download latest MyNotes 0.8.0(9)**](https://github.com/tarungittarun/TicTic/releases/download/mynotes-v0.8.0-9/MyNotes-v0.8.0-9.apk)

Main Home/editor toolbars now use accessible outline icons, with Writing/Capture/Organise/More
categories instead of permanently exposing all commands. Diagram tools are grouped and voice actions
use compact icon rows. Settings includes default-off local diagnostic collection, export/delete and
an explicit privacy boundary. [Full UX/symbol/diagnostics guide](UI-AND-DIAGNOSTICS-v0.8.md).

[Accepted run40602104296](https://github.com/tarungittarun/TicTic/actions/runs/40602104296):
**69 JVM,104 unique Android tests,851/851 executions across21 lanes; all30 jobs green**.
All21 real prior-release note seeds survived install-over; all21 installed hashes matched.
Local gates:108 Notes /498 root. APK66,510,484 bytes; SHA-256
`84c52ec8e287692d620a3265d4d790a39616433f1f52634b09812473b85bab16`.
No permissions added; package/certificate unchanged. Install over the old app; do not uninstall/clear data.
Physical-phone discoverability/TalkBack and complete aesthetic acceptance remain user/device checks,
not inferred from case counts. This milestone does not complete every secondary screen/dialog.
The earlier70-minute native component soak below is not a long UI/diagnostics stress test of0.8.

## Historical accepted 0.7 build and qualification (retained unchanged)

[**MyNotes 0.7.0(8) — APK download**](https://github.com/tarungittarun/TicTic/releases/download/mynotes-v0.7.0-8/MyNotes-v0.7.0-8.apk)

- Size 66,498,132 bytes; SHA-256 `59229cbd1578d4107c91172a5ae178b8b2d3fe062757d1f04923aaf3c69aceaf`.
- Stable package `app.space.notes`; certificate `08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`.
- Source `280bf84efb870361abd2f04be88ba81a78d5cae1`; publication commit `9693d89`.
- Install over the prior release. **Do not uninstall/clear data. Export JSON plus important voice WAVs separately.**
- Native releases live in versioned GitHub Release assets; pinned small receipts stay in Git. Releases1–8 were not overwritten.

## What was actually delivered

| Slice | Outcome | Boundary |
|---|---|---|
| Offline OCR | Bundled English/Hindi/Kannada Tesseract models; local-image selection, editable review, append/new note | Printed/control images; three supported local-provider authorities, not general Downloads/OEM/cloud support; no camera/handwriting/all-language accuracy claim; original image not attached |
| Automatic offline ASR | Bundled English Vosk model, foreground optional microphone, durable PCM WAV before inference, playback/export/retry | English only; physical/noisy/Indian-accent corpus not qualified; WAVs are outside JSON backups |
| Voice data safety | Interrupted WAV recovery, idempotent source application, corrupt-entry isolation, cross-worker atomic transcript lock, durable deletion guards including failed/pending placeholders | Readonly guard delays purge rather than resurrecting notes; explicit inbox recovery remains intentional |
| Reminders | Portable schema 5 epoch, date/time chooser, Clear/Undo, optional notifications, stale/Trash validation, reboot/update rescheduling, open existing note | Inexact; no recurrence/location/snooze/exact-alarm exemption or missed-power-off guarantee |
| Home widget | Resizable/re-placeable four-title view, pinned/recent ordering, excludes Archive/Trash, opens original note, refreshes from private atomic snapshot | Not a scrollable interactive checklist; physical launcher pin/resize/OEM behavior still needs device checks |
| Prior core | Rich notes/checklists/labels, search, local generations/backup, editable diagrams/flowcharts and vector PDF retained | Full markdown/full undo/drag/product baseline not complete |

## Measured qualification

[Accepted normal run 40590113347](https://github.com/tarungittarun/TicTic/actions/runs/40590113347)

| Gate/evidence | Result |
|---|---|
| Local Python gates | **107 Notes checks; 497 root checks passed** |
| JVM domain rules | **69/69** |
| Android unique methods | **98**, explicitly discovered/partitioned |
| Android executions | **803/803 across 21 lanes** |
| Main workflow expanded jobs | **30**, budget enforced; matrix fail-fast disabled |
| Final normal run elapsed | **7m 43s** from run creation to completion; not a matched-workload speedup benchmark |
| Emulator versions | API26/29/34/35/36, with aspect-specific coverage |
| Full resource variants | API34, API36, night, 200% font, real landscape and tablet: **98/98 each** |
| Permissions | Granted microphone API26/35; denied in other lanes; granted notification local-surfaces lane passed |
| Update safety | **21/21 real release-note seeds survived install-over** from highest accepted prior APK; no uninstall |
| Binary identity | **21/21 installed accepted APK hashes matched audit/download** |
| Native static inventory | **18 libraries: 6 per ARMv7/ARM64/x86_64**; correct ELF machines/classes; no 64-bit load alignment below 16 KiB |
| Artifact/publication gate | Only audited, emulator-verified bytes published; original package/certificate retained |

Static ELF evidence is not ARM or16 KiB-device execution. Debug-host instrumentation is separate
from official-release install/hash/launch/update evidence. Neither is universal phone certification.

## Extended native/durability soak

[Run 40586522279](https://github.com/tarungittarun/TicTic/actions/runs/40586522279) is separate and nonpublishing.
**Completed successfully on API26 and API35** (all three workflow jobs green):

| API | Measured elapsed | Complete OCR/ASR/disk cycles | Warm post-cycle PSS | Maximum post-cycle PSS | Max minus warm |
|---|---:|---:|---:|---:|---:|
| 26 | 70m 0.978s | **3,205** | 185.32 MiB | 193.38 MiB | 8.06 MiB |
| 35 | 70m 0.387s | **2,095** | 107.51 MiB | 113.65 MiB | 6.13 MiB |

**5,300 complete cycles** total. Each cycle required real native English OCR and ASR, interrupted
PCM recovery, repository equality and portable-backup readback. Both final source WAV hashes
remained `dcfea5712c43a43ba7ae8083afb39d36993e5a69c46e88b68aaa72b65cb615bb`.
Different API/host throughput is not a controlled performance comparison.

The run cycles real English OCR/ASR, interrupted PCM repair, atomic repository/portable-backup
readback and source-WAV integrity with a bounded32-note working set. It checks gross retained
post-cycle PSS growth against a 128 MiB post-warmup allowance, not zero leaks or true inference-peak
memory. It uses a debug host and records its own source `899e5a0`; exercised native/domain/repository/
projection inputs are unchanged in the accepted app. Later UI/capture/deletion-controller fixes
are covered by the complete normal gates, not claimed as exercised by this component soak.

## Findings that changed the implementation/pipeline

- Old D8 could not dex JNA5.18.1: isolated Notes moved to AGP8.7.3/Gradle8.9/compile35; no calculator/suite rebuild.
- Landscape qualification initially measured an insufficient context/policy. Real Activity plus physical aspect exposed portrait1080×1920; fixed display policy/late rotation restored genuine landscape. Strict assertions stayed intact.
- Root/unroot could close ADB before its response. Fixture transitions now require the actual requested UID, never assume root status or run tests privileged.
- Pre-script emulator failures had no usable app log. A bounded bootstrap observer now captures SDK/AVD/boot output too, without changing action exit status or retrying tests.
- Atomic journal readers and OS receiver projections could race saves. Shared snapshot/last-state locks and a parallel-reader test prevent stale display/delivery and read rollback.
- Ordinary edits could dismiss an already delivered reminder. Notifications now carry the expected epoch and only stale/cleared/trashed/changed reminders are invalidated.
- Late successful or failed voice results could recreate purged placeholders. Durable creation markers, busy-result deferral, pending-placeholder coverage and readonly-guard journeys address this.
- Quick recording cancellation before worker start could leave capture stuck; its completion callback now always clears controller state.
- Final run archived **16 controlled-font OCR diagnostic records across eight lanes**: Hindi `भारत नमस्ते` (confidence 90 on API26, 91 elsewhere) and Kannada `ಕನ್ನಡ ಭಾಷೆ` (96); all recorded token matches true. Confidence/token recovery is not a broad accuracy score.

## Deliberately still open

Photo/file attachments, recurring/location reminders, richer widget configuration, full markdown/
undo-redo/hashtag workflows, startup corruption-recovery UX and large-index/compaction work.
**Wall dragging stays last.** Physical SAF/SD/photo/EXIF, real launcher placement/reboot/Doze/OEM,
TalkBack/IME, noisy/accented speech and physical ARM/16 KiB-page trials and very large simultaneous reminder catalogues (including OS per-app alarm quotas) remain unqualified. Play
publication and MySpace integration are not done. Calculator/MySpace application code and their
build/test workflows were not rebuilt or rerun for this work.

[Machine-readable qualification evidence](evidence/2026-10-28-qualification.json) · [Next acceptance slices](NEXT-ACCEPTANCE.md) · [Device acceptance guide](STANDALONE-TEST-GUIDE.md) · [OS surfaces scope](LOCAL-SURFACES-v0.7.md) · [CI architecture](CI-ARCHITECTURE.md) · [offline engine/model provenance](OFFLINE-ML-v0.6.md)
