# MyNotes 0.28 device-quality changes — verification guide

Development candidate, not a public Release. Final run41359785324 passed9/9 jobs,282JVM
and1130Android test outcomes. See the qualification ledger for source/APK identity and the
full history; this is not a claim that it passed on your phone.

## First: preserve your existing notes

Install over the existing MyNotes app, using the same package and signing identity. **Do not
uninstall, clear app data, or delete more folders as a troubleshooting step.** Internal notes
remain the working copy; an optional selected App folder is an extra copy, not a replacement
for private storage. A pending/error folder checkpoint is not a verified recovery backup.

If normal startup cannot open the library, the recovery screen offers:
- **Export diagnostic report** without loading the note editor.
- **Enable issue logs and retry** (explicit opt-in; reports do not intentionally include note
  bodies or audio). Enable first, reproduce, then export, to capture a useful stack trace.
- **Use internal copy**, which verifies the existing private library before disconnecting the
  external folder. It will not bypass unfinished full-app recovery or erase folder files.

The supplied launcher screenshot contains a history warning, not the exception stack trace.
The exact crash cause is still unconfirmed. New regression coverage and defensive handling
are not proof that every device-specific crash has been eliminated.

## What to try

1. **Home:** Record voice is followed by Search. New Note/Doc is above the bottom navigation.
   Home's library-action Undo is contextual and labelled separately from editor Undo/Redo;
   it is not a universal restore button or a recovery-from-reinstall feature.
2. **Read image:** choose an image without a language prompt. OCR automatically uses the
   bundled English, Hindi and Kannada recognizers. Review the text before inserting it.
   It does not recognize every language or promise universal emoji/handwriting recognition.
3. **Record voice:** capture uses a speech-oriented microphone path with initialization
   fallback, supported noise suppression, and supported preferred microphone direction.
   Benefits depend on hardware/OS; emulator tests cannot prove clearer physical playback.
4. **Uncertain speech:** low/unknown native word-confidence results stay in Voice inbox for
   **Review transcript**, rather than being inserted automatically. Correct the words before
   insertion. Confidence is only a review heuristic, not a measured transcription accuracy;
   confidently wrong output is possible. Original WAV audio remains separate and exportable.
5. **Quick summary:** available from a note/Doc's actions. It selects up to three original
   sentences locally and can save them as a new note. It does not rewrite the source, upload
   it, run a generative language model, or guarantee that all important points were selected.
6. **Storage & recovery:** separates note organisation, backups, optional extra folder copies,
   older libraries/file opening and repair. Notes-only backups omit original voice recordings,
   profile/settings and edit history; full App-folder recovery has its own included categories.

## Ten common file types, plus native MyNotes JSON

This is a practical supported-format list, not a researched global popularity ranking.

| Type | In-app support and limits |
|---|---|
| TXT | View only, or an editable copy; Unicode/BOM-aware bounded text |
| PDF | Read-only native page preview, previous/next; no PDF editing or text search |
| DOCX | Structured editable subset, or text-only view; unsupported structures may be rejected |
| CSV | Plain text view/copy; no spreadsheet calculation |
| Markdown (.md/.markdown) | Plain source text view/copy, not rendered Markdown |
| HTML (.html/.htm) | Text extraction; no web browsing, scripts or remote image fetches |
| RTF | Bounded text extraction; a subset of controls/code pages, not full layout |
| ODT | Paragraph/heading text extraction; not full OpenDocument layout |
| XLSX | Cached text/value extraction with row numbers and sparse column gaps; no formula execution |
| PPTX | Numeric slide-file-order text extraction; no slide-layout/media fidelity |

Native MyNotes JSON uses its existing validated review/import path. Legacy DOC/XLS/PPT
can be handed to a separately installed reader only after confirmation; no installed reader
means no legacy-format preview. That reader has its own privacy policy.

Office parts currently appear in numeric internal file order, which may differ from the
visible workbook tab or reordered presentation slide order. Sheet names, formatting,
merged-cell geometry and calculated results not cached in the file are not reconstructed.

Sources are never edited in place. **Open editable copy** creates MyNotes content;
**View only** for text does not create a note. Bounds reject oversized or structurally unsafe
input instead of attempting unbounded extraction. Office macros, formulas, embedded objects
and external relationships are not executed. These are not full Word/Excel/PowerPoint editors.

## Speech research and training status

The speech weights have **not** changed: the bundled Vosk small English-US model remains.
No Indian-accent training, new weights, or measured Indian word-error-rate improvement is
claimed. The metadata audit completed successfully, but audio access remained gated:
IndicVoices declares CC-BY-4.0 with gated access; Svarah is gated and its audited metadata
has no explicit license value. Svarah is an evaluation benchmark, not training material.

Before any model adaptation: obtain authorized, accessible, correctly licensed training data
and trainable weights; separate speakers across train/development/test sets; reserve Svarah
for evaluation if access/use is permitted; compare English accents, spontaneous/slang speech,
noise, latency, peak memory and battery on real phones. Neither dataset metadata inspection
nor emulator synthesis counts as training or proves “PRO” speech quality.

## Physical acceptance still needed

- Upgrade without uninstalling; verify existing content and folders before troubleshooting.
- With a safe independent backup available, verify the previously unavailable-folder situation
  can reopen the internal copy; export an opt-in report if startup still fails.
- Confirm Home ordering, contextual Undo and editor Undo/Redo in portrait/landscape/large font.
- Try mixed-script photos, punctuation, symbols and emoji; compare with the original image.
- Record the same Indian-English phrases in quiet/noisy rooms and compare retained playback
  and reviewed transcript. Do not confuse understandable playback with recognition accuracy.
- Check representative real files of all ten types; source files must remain unchanged.

Automated synthetic/emulator checks supplement, rather than replace, this device acceptance.
