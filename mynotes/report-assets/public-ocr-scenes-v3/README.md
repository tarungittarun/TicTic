# Additional varied public OCR inputs — evaluation only

Two originals from JaidedAI/EasyOCR: `examples/english.png` and `examples/french.jpg`. Original blob and byte SHA-256, dimensions, licence and manually frozen visible references are in `manifest.json`; original repository licence retained. Photos/artwork/logos remain their owners. No endorsement. Not packaged in the production APK; no model training.

- English illustrated cyan poster, white/gold text, several font sizes and distracting illustrations. This historical health poster is a pixel fixture, **not current medical advice**.
- Oblique outdoor direction-sign photo: contrast reversal, perspective, small superscript, accents and icons. **French is not a supported OCR language**; evaluate as an explicit capability-limit challenge, not evidence of French support. Arrows/pedestrian icons are not reference text; invented letters from them remain errors.

References were transcribed before inference. Native outputs/latency/accuracy remain unmeasured until the test-only harness executes. Neither scene is an untouched heldout benchmark. This extends the previous UI screenshots/nature-photo negative controls; it does not replace their failures.
