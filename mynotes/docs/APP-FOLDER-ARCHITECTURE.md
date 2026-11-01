# Complete on-device App folder — design and acceptance

Baseline 0.26/code29, closure50f27a1c. This revision is not qualified yet.
User priority: no loss of authored content; internal storage always remains active.
No folder selected means app-private storage only (uninstall removes that private copy).
New root must be empty; recognized existing roots enter explicit recovery, never initialization.
Unrelated nonempty roots are refused. Existing legacy note-library roots remain readable.

## Layout (versioned marker, not a directory name heuristic)
- `MyNotes-App.json`: identity and supported layout version, no phone-specific grant URI.
- `Content/Notes/`: complete editable `.mynote.json` Notes/Docs; app folder controls create
  user subfolders here. Rich content, images and drawings remain in the native format.
- `Content/Voice/`: readable original completed WAV copies. Interrupted recording bytes
  remain private pending recovery; never advertise an active recording as mirrored.
- `Settings/`, `Profile/`, `History/`, `Diagnostics/`: understandable reserved namespaces.
  Profiles have no fictional account/login fields; currently user preferences are portable.
- `Recovery/Library/`: verified immutable note generations and media.
- `Recovery/Objects/`: content-addressed, readback-verified auxiliary data objects.
- `Recovery/Checkpoints/`: immutable manifests linking complete internal file/preference
  snapshots to checksummed objects, with a single parent/head chain. Last verified
  checkpoint remains recoverable when a write fails. No automatic object garbage collection.
- `README.txt`: what users can organize, recovery steps, plaintext/privacy and pending-save limits.

## Copy choices
Notes/Docs and their attachments mandatory while a root is connected. Default-on voice,
history/recovery and settings/profile; diagnostics default-off and never enables collection.
Turning off a category stops future mirroring; existing recovery objects are not silently
removed. UI discloses that excluded data cannot be relied on after uninstall. App-private
copies continue in every mode. No network or account required.

## Correctness/performance requirements
Private atomic save first. Provider I/O and hashing run on bounded, coalesced workers, not
on UI thread. Stream large recordings; reuse verified immutable objects; report pending
rather than claiming both copies saved after a provider failure. Never treat flush/close
as proof of power-loss durability. In-flight recordings and partial atomic files are not
promoted to a complete external checkpoint. Reinstallation requires a fresh Android tree
grant; old grant URIs are not restored from preferences.

Recovery validates marker, supported version, complete manifest chain, every object length
and digest, native library decoding and safe owned paths BEFORE applying. A complete-app
restore must not overwrite a nonempty installation; use a new private installation or
explicit note-only merge with disclosure. Preserve private rollback/staging and recover an
interrupted restore before normal controller loading. No restoring executable/model/cache
files or arbitrary filesystem paths. OS notification/widget grants require fresh consent.

## File opening
Allow known on-device Files/MediaStore providers consistently; still reject cloud/web/file
URIs. Opening a native note should reopen the matching editable note, or review an import
with conflicts preserved. Private generation files are recovery records, not standalone
portable notes; identify them and resolve through their granted library or explain how to
open the App folder. TXT should offer an editable imported note, not a dead-end preview.
DOCX needs bounded structured import (supported styles/tables), with an explicit warning
for unsupported layout/media; retain originals, never imply Word layout fidelity.

## Required qualification (not yet evidence)
Native real-provider initialize/nonempty refusal; revoke/write/readback failures; choices;
fresh-context/reinstall-style full recovery including WAV/settings/history/empty folders;
corruption/future version/path traversal; interrupted restore/rollback; concurrent change;
no source deletion; native note re-open + TXT editing/find + structured DOCX. Run existing
migration/backup/history/rich-document suites. Android builds Actions-only; monitor to final
results. Old passing0.26 outcomes do not qualify this new architecture.
