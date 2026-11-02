# Round21: remaining offline viewer work

Status: implementation candidate0.33/code36; NOT yet native-qualified or delivered.
User authorizes continued work without waiting for device feedback. No source/user
content/recording uploaded; private content remains on-device. Android builds
remain Actions-only. No changes to default Vosk, opt-in model weights, stored
content codecs or network permissions.

## Implemented; qualification required

- Explicit **Recognize page offline** in PDF Read options and text panel. Rasterize
  exactly the selected page on the existing renderer serial executor, max2400px
  edge/4MP, release native Page before real bundled eng+hin+kan OCR. Never create a
  Note/Doc, write the PDF or automatically index OCR. Select/review/copy text only.
- Cancel/Back/dismiss/close stop recognition; every bitmap is recycled on every
  path. Document close remains queued behind rendering/recognition, without UI
  thread waiting. Empty text cannot be copied as a successful result; engine score
  is not an accuracy percentage. JNI linkage errors get an understandable message.
- PDF/Word/CSV/XLSX views save compact local URI/type/back-origin and bounded view
  state in Android Bundle. Reconstruct/reopen read-only, restore page or worksheet/
  row/horizontal position and query. No source bytes/bitmaps in Bundle; revoked or
  unavailable provider access reports failure and leaves saved content intact.
  OCR jobs are deliberately not auto-restarted after reconstruction.
- File-open generation fences discard/close late results after navigation changes.
- Spreadsheet visual fixture uses successful **PixelCopy of its own Window**, not
  a screenshot of whatever other Activity is in the foreground. This is own-window
  synthetic rendering evidence, not physical/whole-device foreground acceptance.

## New native checks

Real scanned image-only PDF → actual auto-language Tesseract → selectable review;
blank page; cancellation/UI-thread rejection; renderer/resource cleanup after
queued cancel+close; bitmap budget/original bytes; reconstructed page/query; real
SAF CSV controller reconstruction/query/Back/no-import/original bytes. Every new
class is explicitly assigned to exhaustive CI and all4 focused profiles. Controller
reconstruction is not yet an actual OS process-kill or physical acceptance test.

## Not falsely marked finished

Direct inline PDF glyph-selection/drag handles, vector-quality zoom redraw and
universal Office layout fidelity are not delivered by these changes. Text remains
in a native selection panel. Unsaved image OCR review/process-kill recovery needs
separate bounded recovery design; rotation is already retained and qualified.
Universal emoji OCR and broad Indian speech need real licensed diverse data,
training/evaluation and measured unknown/error behavior, not asset-name coverage.
Physical microphone/playback/startup remains unverified without a phone. No new
training step or physical success is claimed. Source/licensing/geometry research
continues in `PENDING-OCR-RESEARCH-2026-11-02.md`.

## First qualification follow-up (not approval)

Run41456712021 compiled/linted and ran native profiles. Multiple executing profiles
reported the same new cancellation fixture failure: its assertion ran in the same
main-thread stack as AlertDialog.performClick, before Android's ButtonHandler
could deliver the actual Cancel listener. The fixture now drains the main message
queue before asserting cancellation, **before close()**; additionally verifies that
Cancel actually dismisses progress. Cleanup/no-review/source-file assertions stay.
No runtime workaround and no assertion/inventory deletion. Requalification remains
required. Android26 profiles that report no executed methods need raw diagnostic
inspection separately; do not confuse these with the executing fixture failure.

Public emoji training did actually execute separately: see the measured negative
result in `PENDING-OCR-RESEARCH-2026-11-02.md` and its exact retained report. It is
not approved or incorporated into the app; universal emoji recognition is pending.

### Android26 failure cause now evidenced

Bounded, sanitized source-job log excerpts were recovered by run41460830263 in
`design-2026-10-29/native/41456712021/bootstrap-diagnostics/`. Both jobs show
`Warning: An error occurred while preparing SDK package Android Emulator: Error
on ZipFile unknown archive.` followed by unavailable emulator TCP5554. No native
instrumentation transcript was produced. This is an evidenced SDK bootstrap
failure, not a measured app/test result. A complete fresh qualification run is
authorized with the callback-fenced fixture; no minimum API/profile is removed and
no missing case is accepted. Runtime source/code36 is unchanged by this retry.

First run executing profiles: API36 199/200 plus189/189; landscape137/137 plus
126/127; large-font137/137 plus126/127. The only reported executing-method failure
is the callback-timing fixture above. Both API26 shards passed0/200 and0/189, all
missing; the first run is therefore explicitly FAILED, not partial approval.
