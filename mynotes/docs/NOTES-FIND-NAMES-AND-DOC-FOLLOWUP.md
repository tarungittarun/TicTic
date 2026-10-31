# Notes corrections and Doc continuation — new30-job allowance

User requests Find within created Notes; clean checklist-to-text without generated brackets;
editable names for inserted visual/other content including diagram, reminder and image;
continue remaining Doc work. User also requests a189-minute streak. No uninterrupted
runtime, unattended work, measured189 minutes or billing claim is made.

## Job authorization
New **30 individual Actions jobs** explicitly approved. Separate from the earlier24 exhausted
and later10 (4 used,6 unspent). Reserve before launch, not a target. No tests.yml changes.

| Reservation | Jobs | New total | Run/source | Outcome |
|---|---:|---:|---|---|
|1: Find/conversion/names + Doc continuation|4|4/30|pending launch|pending|

**26 new jobs unreserved.** Older unspent6 not automatically added to this cap.

## Implemented for qualification
- Notes → More actions → Find in note. Literal case-insensitive matching with original
  UTF-16 offsets; off-thread/cancellable matching,256-character query and100-result cap.
  Title, writing, checklist items and displayed captions are searchable. Tap a result to
  select/jump to writing; captions scroll/announce. Query/highlights are never saved as marks.
- Checklist-to-text strips only generated checkbox prefixes, not user-authored bracket text;
  retains line boundaries/indentation; old checklist state remains undoable. Text export/share
  still uses checkbox markers, distinct from explicit conversion.
- Images/drawings: per-item actions → Rename; diagram → Rename diagram;
  reminder → Rename reminder. Blank resets the default. Names are bounded, undoable and
  durable in private storage, folders, portable backups, copies and restart-safe history.
  Display names do NOT change image identity/bytes or the legacy Drawing kind discriminator.
- Names require schema8 only when present; unnamed content retains existing schema6/7 output.
  Reads1–8; older APKs reject8 rather than silently stripping names. **Do not downgrade.**
- Doc continuation: Find in document over writing/title/list/table cells; explicit whole-checklist
  Convert to text preserving caption, indentation and rich row marks; contextual reminder rename.
  Existing visual Rename remains intact.

## Qualification and remaining Doc acceptance
Local source gates and Android build/native/PDF qualification required before APK handoff.
Java syntax parsing is not compilation. New tests cover clean conversion, search offsets/caps,
naming mutation/copy/kind preservation, codec/backup round trips/rejection and native Find/Undo.
The screenshot allowlist grows by one synthetic scene; bounded renderer limit73 (was72).
No fixtures or assertions are removed.

Review the approved Doc acceptance against final screenshots/journeys, not just feature presence.
Physical novice/Motorola Android16, reminder delivery, native selection/reflow, OEM lifecycle,
provider behavior and low-storage/power-loss remain real acceptance work, not emulator claims.
Prior Doc handoff is historical for this source. No public Release is authorized by this run.
