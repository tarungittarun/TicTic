# Public real screenshot OCR baseline

Original Google ML Kit sample screenshots, licensed Apache-2.0 by the source
repository; exact LICENSE and Git-object/SHA-256 identities are retained. These
are actual third-party Android UI screenshots, **not photographs of printed
scenes**, private user images, or newly generated synthetic screen mockups.
Google/ML Kit names and logos are source content, not an endorsement or branding
license. No source image or benchmark is packaged in the production application.

References were visually transcribed before native inference. Whitespace is
normalized; punctuation, casing and digits are not erased. Non-text navigation
and status icons and the graphical logo are not reference words. Original,
half-size and JPEG50 cases are separately labelled and hashed. These are
**development evaluation** examples, not untouched heldout or broad real-world
acceptance. Accuracy target: character error rate <= 0.15 per input, not a score
reported by the OCR engine. All misses and exceptions must be retained.

`PublicScreenshotBenchmark` passes only image pixels and automatic-language
selection to the unchanged production recognizer. The frozen reference is used
only after recognition. Each input's pixels and source bytes must survive.
Wall times cover emulator inference, including cold initialization on the first
call; they are not phone-speed claims. The separate opt-in workflow compiles test
assets only and never publishes a new production APK.
