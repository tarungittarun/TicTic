# Search, simple local folders, and restart-safe edit history

New user request continues this session. No new 35-job allowance is claimed: prior code19 consumed9/35; this work uses the remaining26 as a ceiling, not a target. Data/security first; Calculator unchanged, Archive paused, Wall deferred.

## Reservation1 — two jobs, cumulative11/35;24 unreserved

Reserve before launch: shared compilation/JVM/lint/signing build and full API26 native inventory. New app0.17.0/code20 retains package/signer and note schemas. Final qualification still needs dark2× and landscapeAPI34; no qualified APK handoff until gates complete.

Implementation: Search icon immediately after Record voice; single tap focuses, double tap shows IME, holding second tap opens Filters. Normal accessibility click/long-click and existing Filters button remain. Folders organise local notes without requiring storage migration; private folders and connected real SAF folders remain distinct. Local backups continue rejecting cloud providers and verify full readback. Advanced storage location controls are separated from ordinary folders/backups.

History: bounded private AtomicFile sidecar, checksum envelope and independently hashed deduplicated snapshots, exact current-note anchor matching, undo AND redo and canonical cursor state, up to50 steps/direction per note/64 recent notes, global12Mi character retention and16Mi encoded-file ceiling (additional conservative unique-payload budget). Purged/missing notes never restored. Old/mismatched history is discarded, never substituted for current notes. Corrupt/unknown history is retained and persistence blocked until explicit Reset edit history; current notes continue saving. A crash between note and sidecar writes invalidates mismatching history safely. History is device-private, excluded from exported backups; not an unbounded or unlimited history promise. UI batch actions retain existing session semantics; persistent edit history is per-note, not app navigation or recovery of permanently purged notes.

Diagnostics attachment was reviewed locally, not committed:7 folders-open failures and3 rejected import previews on older code16. Redacted diagnostics do not reveal the rejected file contents; no claim to have repaired unknown input files.

Authored new checks:4 domain history tests,6 native history/recovery/private-folder tests,3 controller/gesture journeys. They are not counted as runtime PASS until actually executed. Local Python/source checks are separate from Android compilation.
