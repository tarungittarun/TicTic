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

## Implementation safeguards and current bounds
The first candidate `dc024ab1` failed compilation (lambda local-name collision), not native
qualification. The correction also fences repositories created before recovery using a
private storage epoch; the current controller reloads recovered notes/history before host
recreation can flush. Queued authored snapshots block empty-install recovery until saved.
Live atomic journals are read/repaired only by owning stores under their own locks; the
copier uses non-mutating file reads and verifies stable content before acknowledging.

Complete recovery means the dated **last complete checkpoint**, not every pending write.
If note-only generations are newer, rolled back, missing or damaged, that complete checkpoint
still recovers independently, into private-only storage. The source stays untouched. Preview
newer notes separately with Open library, then choose a new empty App folder for continued
copies. Note-only imports from a complete App folder do not switch destinations or replace
its auxiliary checkpoint state. A selected external folder can never silently roll back or
replace a verified nonempty private library.

Copy choices are frozen for each checkpoint. Choices changed during copying take effect in
the next coalesced pass; previous recovery copies remain. Markers, manifests, preferences,
voice metadata, PCM framing and registered destinations are validated. Recovery stages need
64 MiB free headroom beyond their declared file bytes and check space during streaming.
Current defensive bounds: 8 MiB checkpoint manifest, 10,000 registered state files per
checkpoint, 32 MiB individual JSON state files, 1 MiB transcript metadata, 20,000 entries in
one inspected SAF directory. Original WAVs stream, bounded by their RIFF format, not the old
512 MiB portable-audio-archive limit. Exceeding a bound keeps internal data and leaves a
visible pending checkpoint; it is not an unlimited-library claim. Retained historical
objects/checkpoints grow until the user moves to a new empty root; no silent garbage
collection/purge is provided. Notification grants/widgets/diagnostic consent are deliberately
not portable. Physical reinstall/removable-media/vendor-provider acceptance remains separate
from emulator fresh-context tests.
