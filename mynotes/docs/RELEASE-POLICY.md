# MyNotes update identity and version policy

- Release application ID stays `app.space.notes`. Debug uses a separate `.debug` ID;
  debug is not an update channel for the release app.
- Preserve the signing identity at mynotes/signing/notes.p12. Do not generate a new
  signing key during CI, copy calculator code or change the release ID to solve a build.
- Every published APK has a strictly higher versionCode and an explicit versionName.
  0.1.0 (1) -> 0.2.0 (2) -> 0.2.1 (3) -> 0.3.0 (4) -> 0.4.0 (5). Never overwrite a published version's APK.
- CI uses Android apksigner to verify APK signatures and compares certificate SHA-256,
  application ID and numeric versionCode with every published release. A mismatch or
  same/lower versionCode blocks publishing.
- Android emulator smoke installs 0.1.0 release and updates it with `adb install -r`
  without uninstall. This checks install compatibility, not every user's data migration.
  Separate format tests exercise the original v1 text document reader.
- Version 0.4.0 writes schema 2 and reads old schema 1. Migration and structured backup
  round-trip tests pass; unknown future formats fail closed. No deletion/reinstall is
  part of updating. Do not downgrade to a schema-1-only reader.
- Size/device/permission/model qualification remains separate from signing compatibility.
  Third-party/unofficial APKs with another certificate cannot be updated by this key.
- Normal docs changes use [skip ci]. Intentional versioned builds use only Notes workflow;
  old workflows remain paused on this branch.

## 0.5.0 (6) checkpoint

Version chain now includes 0.5.0 (6), schema 3 writes / 1,2,3 reads. CI40578488274
verified update identity against all retained releases and installed-byte hashes on all
13 lanes before publication. `894fd9b` stores the unchanged tested candidate, checksum,
source and audit receipt. Do not overwrite or downgrade. New pipeline failures remain
non-publishing; same-version CI-only revalidation tests the immutable official APK.
