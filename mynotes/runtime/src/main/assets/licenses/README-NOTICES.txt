MyNotes third-party offline engines and models

Tesseract4Android 4.9.0, tag commit 15c534717b1cb58261b58d4e4c1200c7f81f668c:
https://github.com/adaptech-cz/Tesseract4Android/tree/4.9.0
Includes Tesseract 5.5.1 (Apache-2.0), Leptonica 1.85.0 (BSD-style),
libjpeg v9f (Independent JPEG Group license) and libpng 1.6.48 (libpng license).
Full bundled upstream notices are included in this directory.
Models: tessdata_fast 4.1.0, commit 65727574dfcd264acbb0c3e07860e4e9e9b22185,
Apache-2.0. Every eng/hin/kan model hash is pinned in ocr/models.json.
No Tesseract or tessdata source/model modifications are made by this app.

Vosk Android 0.3.75, https://github.com/alphacep/vosk-api, Apache-2.0.
Model: official vosk-model-small-en-us-0.15, Apache-2.0 per the official inventory:
https://github.com/alphacep/vosk-space/blob/f10f63bc6d751cf5c4198a7dc88515f40f2d7020/models.md
ZIP and every extracted member hash are pinned in voice/model.json.
Acquisition evidence: TicTic Actions run40578093243 (TLS download from official model host).
Model hashes pin repeatable bytes; acquisition is not independent authorship attestation.
Vosk's public example test.wav is Apache-2.0 and included only in androidTest assets,
not release assets; its SHA-256 is dcfea5712c43a43ba7ae8083afb39d36993e5a69c46e88b68aaa72b65cb615bb.

JNA 5.18.1, https://github.com/java-native-access/jna/tree/5.18.1,
dual Apache-2.0/LGPL-2.1-or-later; distributed here under the Apache-2.0 option.
Full Apache-2.0 text is included alongside the JNA upstream licensing notice.

Native AARs are unmodified version-pinned dependencies. CPU/model/device/language
qualification is separate from license notices. No cloud recognition API is called.
