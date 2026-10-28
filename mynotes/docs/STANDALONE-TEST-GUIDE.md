# MyNotes — install and device acceptance guide


## 0.9 practical UX check — not replaced by automated totals

**Published: MyNotes 0.9.0 (10).** [Download APK](https://github.com/tarungittarun/TicTic/releases/download/mynotes-v0.9.0-10/MyNotes-v0.9.0-10.apk) · [checksum](../releases/MyNotes-v0.9.0-10.apk.sha256) · [receipt](../releases/MyNotes-v0.9.0-10.apk.release.json).

[Publication run40633291480](https://github.com/tarungittarun/TicTic/actions/runs/40633291480) passed all required gates and all 21 Android lanes. Each full review profile passed 118/118 methods; actual five-profile captures from equivalent source run40623972980 were inspected. This is not user acceptance or physical-device certification. SHA-256: `85bff67139e39c126c8b233551ec2a229a0311acc29a8c0a5392eb98efe9cf1d`.

Use the gated 0.9 APK only after publication; before that, the older download is not this redesign. Install **over** the existing app, not after uninstall/clear-data. Export important JSON and original voice WAVs separately.

1. Create a note without a title. Type `01` through `09`, each on a separate line. Select `01`, open **A / Formatting**, choose **H1**, then **H2**. Following numbers should move naturally, with no glyph collisions. Scroll to the last line if it is outside the viewport; clipping at the scroll edge is not overlapping paragraphs.
2. Put a heading between normal paragraphs. Select only part of that heading, including a reversed selection. Heading style should cover its whole paragraph, not absorb the preceding/following ones. Clear, Undo, return Home and reopen.
3. Start a heading before typing. Compose a word using your normal keyboard; press Enter and continue. The next paragraph should be ordinary text. This is especially important with the real IME/language you use.
4. Rotate to landscape with the keyboard open. You must see actual note text, not only toolbars or a fullscreen keyboard's separate editor. In cramped height, the top app toolbar can collapse; Android Back/gesture closes the keyboard and restores it. Open formatting; if needed the keyboard closes to leave useful space.
5. Set the largest system font and switch light/dark mode. Check Home cards, note body/formatting, checkboxes, colour names, labels, diagrams, Settings and storage/recovery. Long pages should scroll, not hide their only primary action.
6. Choose a colour while part of the body is selected; the selection should survive the return. Recreate/rotate the controller and confirm selection and the note survive. This is not a promise to remember every note's cursor across all cold starts.
7. Explore Plus / Capture, Organise and More before adding features: can a first-time user find scan, voice, labels, reminder, share and Trash? Native long-press tooltips name icon-only controls.
8. Diagnostics should start **off**. Only if reporting a problem: enable Settings → Collect diagnostic logs, reproduce, export deliberately, then stop/delete. Do not attach personal notes/audio to a bug report by accident.

Physical keyboard/IME behavior, TalkBack, OEM launchers, SD/provider failures and acoustic recognition quality remain device checks. Report the visible outcome and a reproducible sequence, not simply that an APK installed.


## Previous published release: 0.8.0 (9) — UI rejected

[**Download APK**](https://github.com/tarungittarun/TicTic/releases/download/mynotes-v0.8.0-9/MyNotes-v0.8.0-9.apk) · [checksum](../releases/MyNotes-v0.8.0-9.apk.sha256) · [full receipt](../releases/MyNotes-v0.8.0-9.apk.release.json)

Qualification [40602104296](https://github.com/tarungittarun/TicTic/actions/runs/40602104296):
**69 JVM tests, 104 unique Android tests / 851 executions, 21 emulator lanes / 30 expanded jobs, all required gates green**.
Each lane preserved a real seeded release note across install-over and matched the exact installed
APK hash. Local gates: **108 Notes checks / 498 root checks**. This is a delivered milestone,
not full-product or all-device certification. APK 66,510,484 bytes; SHA-256
`84c52ec8e287692d620a3265d4d790a39616433f1f52634b09812473b85bab16`.

New in 0.8: icon docks, grouped editor/diagram/voice tools and Settings → opt-in local diagnostics.
See [symbol meanings, privacy and logging steps](UI-AND-DIAGNOSTICS-v0.8.md).
Prior 0.7: local inexact reminders, contextual notification permission, resizable/re-placeable pinned-first
home widget, and stronger voice recovery/deletion guards. See [LOCAL-SURFACES-v0.7.md](LOCAL-SURFACES-v0.7.md).
The prior offline-engine checkpoint 0.6 remains available unchanged; its evidence is in [OFFLINE-ML-v0.6.md](OFFLINE-ML-v0.6.md).

## Install / data compatibility

Install over the existing MyNotes release. **Do not uninstall or clear app data.** Package
`app.space.notes` and certificate `08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`
remain fixed. Versions 0.7–0.8 write schema 5 and reads schemas 1–5 (0.6 wrote schema 4). Do not downgrade to an old reader. Existing folder libraries/old backups remain readable.
Export and verify a JSON backup before destructive experiments; **also export voice WAVs separately**.
Private recordings are not contained in JSON or selected-folder note generations and are lost by uninstall/clear-data.
The default private library is on-device. For local-only external storage/export, choose an **on-device or SD-card SAF provider**, not a cloud-backed folder. The app has no INTERNET permission, but a provider selected through Android's picker can have its own network/storage behavior.

Large APKs use versioned GitHub Release assets, not accumulating binary copies in Git. Their small
SHA/source/certificate/size receipts remain in `mynotes/releases/`. Old published versions are not replaced.

## Offline OCR and automatic voice transcription

1. Note → **Scan image** → choose English, Hindi or Kannada → choose a supported local image.
   Wait off-thread for OCR, review/edit the recognized text, then **Append to note** or **Save as new note**.
   Cancel/failure leaves note content and original image unchanged. The source image itself is not attached to the note.
2. Try a clean printed page, names, numbers, low contrast, rotated images and a genuinely blank page.
   Engine confidence is not accuracy. The release bundles all models and has no network permission;
   no first-use model download should be required. Cloud/file URIs and oversized/invalid inputs are refused explicitly.
   Current OCR accepts `com.android.externalstorage.documents`, `com.android.providers.media.documents` and `media` authorities only. Other providers, including the separate Downloads document provider and OEM/cloud authorities, are not generally supported. If rejected, copy the image into Pictures/Internal storage and select it through the supported local provider; this is a compatibility boundary, not OCR accuracy.
3. **Record voice** requests the optional microphone permission. First deny it and verify text/checklists,
   diagrams, storage and OCR still work. Then allow it from an explicit record action.
4. Speak English, tap **Stop and transcribe**, and open **Voice inbox**. WAV is saved before automatic
   bundled Vosk inference starts. Review the transcript against the retained recording. English model performance
   on Indian accents, noise and names is not established by the public numbers fixture.
5. Inbox: **Play**, **Stop playback**, **Export WAV**, **Retry transcript**, **Open note**.
   Verify an exported WAV in a separate player. Change activity/leave the app during recording: foreground-only capture stops.
   Exercise interrupted capture/reopen and check retained audio; do not expect background recording.
6. Edit a transcribed note, retry and restart: applied sources must not duplicate text or resurrect an automatically purged note.
   Explicit inbox Open note is a deliberate recovery action; automatic callbacks are not.

Native English OCR and speech inference, WAV recovery, denied/granted microphone, and controller recovery
have executable emulator checks. Hindi/Kannada controlled-system-font diagnostics additionally save source PNG,
recognized text, confidence and token matches; those controlled images are not a camera/handwriting language corpus.
See [engine provenance and precise limits](OFFLINE-ML-v0.6.md).

## New in 0.5

1. Open a note → **Diagram**. Add process, decision, start/end, input/output, ellipse
   and text-box shapes. Select a shape by tapping/dragging or the shape list. Try edit,
   color, directional movement, resize and delete/Undo. Connect to another shape and
   tap an arrow entry to label/remove it. Move endpoints and reopen to check retention.
2. **Export PDF** → local Internal storage or SD destination. Reopen the PDF in your
   reader; text/checklists paginate and a diagram occupies a vector page. Editable
   originals remain in Notes. PDF is not a round-trip backup; use JSON backup for that.
3. From another app, Share **plain text** to MyNotes. A new local note is created, no
   URL is fetched, and existing notes remain. Other incoming media is not implemented.
4. Home → **Filters** reveals view/sort/label controls; **Manage labels** supports global
   rename/combine/remove including Archive/Trash. In bulk label editing, check **Add to
   existing labels** to avoid replacing each selected note's individual tags.
5. Repeat selection and editing in landscape, night and 200% system font. Home controls
   collapse during selection to preserve the board viewport. Long tool rows scroll;
   editor content scrolls as a complete page so the writing area does not collapse.

Native PDF rendering/reopening, diagrams/codec/backup, and these resource variants were
exercised in emulators. Arrow text dialogs and every physical touch/picker path still
need review on your phone. Flowcharts are a fixed logical canvas, not auto-layout/freehand.
See [scope and acceptance cases](DIAGRAMS-AND-EXPORT-v0.5.md).

## Core regression journeys

1. Home: make text notes with different lengths. Confirm masonry auto-height, Pinned
   and Others grouping, grid/list toggle and persistent created/edited sorting.
2. Rich text: type a body, select a range using Android selection, tap Bold, Italic,
   Underline, Strike or Heading 1/2. Reopen to check styles. Clear removes formatting
   only from the selection. Controls scroll horizontally. Sticky typing styles,
   markdown triggers and a full document undo stack are not in this milestone.
3. Checklist: create rows, edit text, tick/untick, tap a row then move up/down, indent/
   outdent (0–3), hide/show completed rows, remove/add. Convert to text and Undo. Verify
   the saved item text, order, completion and indent after reopening.
4. Labels: edit a comma-separated set on a note; duplicate spelling/case normalizes.
   Home → Filters chips filter by label, and search sees labels/list text. New notes on a label
   page inherit it. Global rename/remove is now available; # autocomplete remains pending.
5. Colour: check all 12 swatches on cards in light/dark, plus title/body/chip contrast.
   Repeat on small screens and large font. Automated style/model tests are not a
   substitute for all palette/device accessibility checks.
6. Multi-select: long-press one card, tap others, then pin/archive/Trash, set labels/
   colour or share. Bulk labels REPLACE by default; the additive checkbox preserves prior tags. Cancel leaves
   notes unchanged. Done or Back exits selection. Restore trashed notes before editing.
7. Merge two notes: new plain-text note opens; source notes go to Trash. Undo restores
   source states and removes an untouched merged result. If you edit the result first,
   Undo may keep that edited result rather than discard typed content. Source rich
   formatting is flattened in the merged text; original documents remain in Trash.
8. Copy: creates a new ID/time, preserving content, list structure, formatting and tags.
   Share uses the explicit text chooser (100k-character message guard; backup for larger).
9. Rotation/background: test mid-word, styled selection and checklist rows. Bundle draft
   is bounded to 32 KiB; larger drafts depend on lifecycle flush. Forced-kill inside the
   autosave debounce is not a passed crash-proof guarantee.
10. Export/import a mix of old text, rich text and checklists. Check schema-3 round-trip,
    metadata, unchanged duplicate skipping and deterministic conflict copies, including
    re-import of old 0.3.0 backups. Portable backup envelope remains bounded to 8 MiB.
11. Regression: folder choose/open/move/retry, backup preview/cancel/checkpoint, pins,
    archive, seven-day Trash, search focus and native selection. Actual SAF/SD faults
    remain for device qualification; injected protocol tests do not certify providers.

## Automated scope and how to judge it

The accepted checkpoint above is distinct from newer candidate runs. Normal qualification includes
source/protocol self-tests, pure JVM rules, lint, release/debug/instrumentation builds, immutable APK
identity/privacy audit and independent Android aspects. Release-byte update/hash/launch evidence is
separate from debug-host instrumentation. Controller journeys use real views with isolated repositories;
OS host callbacks and selected-folder protocols may be injected, not every physical provider/share app.

Accepted qualification uses **30 expanded jobs / 21 Android lanes**, including API26/29/34/35/36,
night, landscape, 200% fonts, tablet, notification-granted and microphone-granted variants. Every normal
lane seeds a real note in the highest accepted prior APK and requires that title after install-over.
Static ELF inventory identifies packaged ABI architectures and alignment, not physical ARM/16-KiB behavior.

A separate nonpublishing native/durability soak uses the debug host, not the exact signed release binary.
Only a completed timed report establishes its duration/iteration/PSS results. Its component source is
recorded independently; it does not exercise controller UI, background AudioRecord or physical battery/thermal conditions.

## Phone checklist for reminders/widget

- Reminder: choose a future date/time, deny/allow notifications contextually, clear, Undo, edit, Trash,
  restore and restart. Confirm data stays intact even when notifications are off.
- Let a reminder actually fire with the app closed; open its notification and verify it opens the original note.
  Edit another note after delivery: this must not dismiss the delivered reminder. Clear/Trash/time changes should invalidate stale alerts.
- Restart the phone with a future reminder. Check actual delivery after reboot and under Doze/OEM battery settings.
  These are **inexact** alarms; exact timing and missed-power-off catch-up are not promised.
- Home → **Home widget**, or long-press launcher → Widgets → MyNotes library. Resize, remove and re-add it.
  Notes must remain in the app. Pinned notes sort first; Archive/Trash are excluded. A widget row opens, rather than copies, a note.
- Widget titles are visible on your home screen. It is a four-title projection, not a scrollable/interactive checklist.

## Still open

Full photo/file attachment workflows, wall/list drag (last), markdown/list customization, hashtag
autocomplete, final snackbars/full undo-redo, recurring/location reminders and richer widget configuration,
startup corruption recovery UI, folder compaction/index scaling, actual physical SAF/SD/photo-picker/EXIF
and noisy/accented speech corpus, TalkBack/IME/OEM/16-KiB-device acceptance, Play publication and eventual
MySpace integration. No full-feature or universal-correctness claim is made.
