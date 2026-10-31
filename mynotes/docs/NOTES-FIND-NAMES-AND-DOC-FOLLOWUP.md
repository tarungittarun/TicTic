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
|1: Find/conversion/names + Doc continuation|4|4/30|41185836938 / b2f9a866|240 JVM and257/257 API26 PASS; dark123/124 and landscape123/124 FAIL reminder state-description mismatch; all3 PDFs PASS|
|2: name presentation, adaptive Find and lifecycle/accessibility|4|8/30|41186948827 / f9ad2fad|build FAIL: missing fail() assertion import in new instrumentation wait helper; native skipped, all4 conservatively counted|
|3: instrumentation helper/import guard and no-op safety|4|12/30|pending launch|pending|

**18 new jobs unreserved.** Older unspent6 not automatically added to this cap.

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

## Checkpoint2
First evidence retained under `design-2026-10-29/native/41185836938/`; no missing profiles.
The full assertion remains: visible reminder text must equal its accessible state description.
Now both include the name and date. This is a real accessibility repair, not a waived failure.

Further review: Find now has a compact query/Close row, weighted results area, IME Search
keyboard-dismiss action, and detach-driven cancellation/dismissal. No fixed-height result list
that can consume a small viewport. Add actual Find-sheet fixture (bounded scene limit74).
Doc captions, including read-only captions, are searchable; reminder name/date is visible
and editable in Doc. Reminder notifications use the custom reminder name (private visibility
retained). Legacy PDF renders custom image/diagram captions; image kind/bytes remain unchanged.
A native journey names image/diagram/reminder, verifies durable data/bytes/time/shape and Undo.
The Doc reverse-conversion journey uses Document → Undo, the supported compact-viewport route.
240 JVM /258 full /125 focused expected; local575/Notes185 and Java syntax gates before push.

## Checkpoint3
Checkpoint2 compiled production code but failed instrumentation compilation: the new wait
helper called `fail()` without a static import. This was my test error, not waived. Replace
with explicit AssertionError and add an import contract over **all** MyNotes JVM/native
sources, including wildcard imports and masked strings/comments. Local syntax alone had
not detected the missing symbol; root's old import contract only covered root JVM modules.

All4 checkpoint2 slots stay conservatively reserved despite native being skipped. No attempt
to silently reuse them. This reservation is12/30,18 remain. No-op Save name now dismisses
without an Undo entry, timestamp/storage rewrite or unnecessary schema8 opt-in. Find's
compact query/Close pane has an accessibility title without a bulky visible title row.
Expected local576/Notes186;240 JVM,258 full,125 focused unchanged.
