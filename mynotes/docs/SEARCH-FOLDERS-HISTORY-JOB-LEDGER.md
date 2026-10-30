# Search, simple local folders, and restart-safe edit history

New user request continues this session. No new 35-job allowance is claimed: prior code19 consumed9/35; this work uses the remaining26 as a ceiling, not a target. Data/security first; Calculator unchanged, Archive paused, Wall deferred.

## Reservation1 — two jobs, cumulative11/35;24 unreserved

Reserve before launch: shared compilation/JVM/lint/signing build and full API26 native inventory. New app0.17.0/code20 retains package/signer and note schemas. Final qualification still needs dark2× and landscapeAPI34; no qualified APK handoff until gates complete.

Implementation: Search icon immediately after Record voice; single tap focuses, double tap shows IME, holding second tap opens Filters. Normal accessibility click/long-click and existing Filters button remain. Folders organise local notes without requiring storage migration; private folders and connected real SAF folders remain distinct. Local backups continue rejecting cloud providers and verify full readback. Advanced storage location controls are separated from ordinary folders/backups.

History: bounded private AtomicFile sidecar, checksum envelope and independently hashed deduplicated snapshots, exact current-note anchor matching, undo AND redo and canonical cursor state, up to50 steps/direction per note/64 recent notes, global12Mi character retention and16Mi encoded-file ceiling (additional conservative unique-payload budget). Purged/missing notes never restored. Old/mismatched history is discarded, never substituted for current notes. Corrupt/unknown history is retained and persistence blocked until explicit Reset edit history; current notes continue saving. A crash between note and sidecar writes invalidates mismatching history safely. History is device-private, excluded from exported backups; not an unbounded or unlimited history promise. UI batch actions retain existing session semantics; persistent edit history is per-note, not app navigation or recovery of permanently purged notes.

Diagnostics attachment was reviewed locally, not committed:7 folders-open failures and3 rejected import previews on older code16. Redacted diagnostics do not reveal the rejected file contents; no claim to have repaired unknown input files.

Authored new checks:4 domain history tests,6 native history/recovery/private-folder tests,3 controller/gesture journeys. They are not counted as runtime PASS until actually executed. Local Python/source checks are separate from Android compilation.

## Reservation1 outcome — sourceab1d4fb/run41054984154

Build124060131566 PASS: compilation, exact executed JVM216, lint and stable signing. Full API26/job124061151845 **237/238**: new search gestures, both canonical/legacy cold-controller Undo/Redo, all6 recovery/private-folder tests passed. One existing regression failed: Redo remained enabled immediately after typing a replacement following Undo, until the debounced note-model save. Original assertion is retained. Independent PDF verification PASS; evidence preserved in965dbb5.

## Reservation2 — four jobs; cumulative15/35 reserved,20 unreserved

Reserve BEFORE launch: one build, full API26, API34 dark2×, API34 landscape. Fix immediate redo invalidation before autosave (new domain regression); align note-action Undo with persisted pointers rather than recording Undo as a new edit. Clear session history on explicit history reset. Cancel moved/aborted held-search gestures. Avoid full-note fingerprint generation in the per-keystroke history hook; encoding remains off-main. Retouch canonical registry references after bounded history eviction. Validate folder UTF-16; simplify folder front page to organisation only, with local Backups/Storage settings separate; keep storage switching in its proper settings page. Add local-only Android picker flags in addition to the existing enforced local export boundary. Add native private create/move/reopen journey and64-history-cap test. Source contracts explicitly check these safety properties. The former private-folders UI assertion is updated to the redesigned controls and waits for its real asynchronous load; its unchanged-note assertion remains.

Expected authored inventories now JVM217, full native240 and focused111, subject to execution. Local gates558 root/168 MyNotes PASS (overlapping discovery, not726 distinct tests). No physical-device or public-release approval claimed.
