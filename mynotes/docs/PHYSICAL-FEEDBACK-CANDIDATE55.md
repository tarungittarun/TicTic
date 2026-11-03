# Candidate55 — actual Notes IME handoff failure

2026-11-03. MyNotes0.52.0/code55. Not qualified yet.

Candidate53's API36 primary shard actually failed the existing repeated
Notes/Doc transition journey: the new Notes body was focused/served, but IME
visibility was false. This is distinct from the older Doc-new-field issue.
A single earlier observed visibility check could finish startup while a previous
Doc hide was still pending. A temporary unowned focus loss also ended the old
startup callback permanently.

Changes are scoped to creation of a new legacy Note. Before its first keyboard
request, observe two stable layout/height intervals. Finish only after the accepted
request is served and visible on two observations. Recover focus only when no
other view owns it, and cancel for detach/new body/closed controller or a bounded
20 attempts. Once Android accepts the request, observe only—never re-show against
a subsequent user/system hide. The existing Doc startup/insertion handling is
unchanged. Original four-cycle Notes/Doc IME/content assertions are unchanged.
Failure capture adds a bounded Notes startup-phase string; no new screenshot slot.

Folder-copy failure diagnostics are now captured on each actual provider attempt,
not on every fast private save during backoff. Private attachments are not uploaded.

Candidate54 run41580503782 remains independent; its successful build and native
outcomes must be recorded as they arrive, not retroactively attributed to55.
Inventory unchanged:310 JVM /414 Android methods /1,400 native profile outcomes.
Root805 passed before push. Actions-only Android builds, stable signer/package,
increasing version and install-over/no clear-data requirements remain.
