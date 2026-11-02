# Shipped OCR emoji repertoire: measured limitation, not recognition accuracy

Inspected the **actual SHA-verified eng/hin/kan traineddata shipped by the app**,
not merely a model README. The pinned LSTM unicharsets have112/123/140 entries
including three reserved entries. Against the pinned Unicode17.0 list of3944 fully
qualified emoji sequences, **zero** have all required output code points available,
even in the union of these three models. Smiling faces, emoji hearts, family
sequences, the India flag and fully qualified keycaps are concrete missing examples.

This is an output-representation audit, **not an OCR accuracy benchmark**. Characters
being available would not demonstrate recognition. Some ordinary text symbols and
digits are available, but that does not make fully qualified emoji available. No
private image was read, no inference/training was performed, and no weights changed.

Consequences:
- Do not claim universal emoji OCR or invent guessed emoji from ambiguous pixels.
- Lens-like **selected visual area** extraction keeps original pixels, including an
  unrecognized emoji; selectable OCR text is a different result.
- Typed/imported Unicode emoji, clipboard preservation and literal viewed-content
  emoji search are separate capabilities; absence from OCR repertoire does not mean
  the app must delete emoji already present in text.
- Genuine emoji OCR needs a separately evaluated expanded output/detector model,
  licensed varied visual training data, real-font/vendor tests and uncertainty review.
  A Noto-only template demonstration would not prove Samsung or universal coverage.

## Reproduce locally (no Android build, no user data)

Run `python -m mynotes.tools.ci.emoji_charset_audit` with the app OCR manifest,
a directory containing those exact three model files, and the pinned emoji-test
blob. CLI help lists arguments. Output is retained in
`EMOJI-OUTPUT-REPERTOIRE-2026-11-02.json`; tests cover component extraction,
reserved markers, corrupt input/pin rejection and identity/report consistency.

Primary sources:
- [Shipped tessdata_fast commit](https://github.com/tesseract-ocr/tessdata_fast/tree/65727574dfcd264acbb0c3e07860e4e9e9b22185)
- [Tesseract component21 definition](https://github.com/tesseract-ocr/tesseract/blob/5.5.1/src/ccutil/tessdatamanager.h#L58-L82)
- [Unicode17.0 emoji test list](https://github.com/unicode-org/unicodetools/blob/main/unicodetools/data/emoji/17.0/emoji-test.txt),
  immutable Git blob `e42fdef494399d2a05b9b75930a3231fe81b2436`, SHA256
  `1d8a944f88d7952f7ef7c5167fef3c67995bcae24543949710231b03a201acda`.
