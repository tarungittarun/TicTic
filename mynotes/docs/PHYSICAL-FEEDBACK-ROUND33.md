# Round33 — code48 / 0.45.0: remaining native blockers

## Evidence motivating the fixes

Code47 run41515265757: Android build passed. Native outcomes1217 passed,
4 failed and145 unexecuted due landscape-primary emulator boot timeout.
Three failures were `lateBackupCompletionNeverPullsUserBackIntoSettings`, at
Backups→Folders while the serial writer was blocked. The fourth was repeated
Notes/Doc creation, at document IME readiness on API36. No failures are waived.

## Runtime changes

* Header Back formerly used `icon`, whose `act` wrapper refuses every tap while
  `busy`. It therefore never reached the already-safe `back()` caller callback.
  Header Back now directly dispatches through `back()`, preserving its sheet,
  formatting, caller and busy checks. Mutating buttons retain `act`'s busy guard.
  Storage writes are neither cancelled nor reordered. Existing generation fences
  still prevent late completions from reopening the abandoned page.
* Document keyboard startup formerly issued a single unchecked show request.
  It now checks attachment, render generation, field membership, focus and window
  readiness; uses the API30+ inset request as well as InputMethodManager; and stops
  after acknowledged, served, visible observations or a bounded20 attempts.
  Render and detach remove the callback; an old queued callback is inert.
  Delayed cursor restoration is also fenced by render generation.
* The original queued-writer navigation test additionally verifies busy is active
  and navigation did not release the blocked writer. The repeated creation test
  retains all existing content, keyboard, opt-in and checklist assertions.
* A new attached native test exercises keyboard callback cancellation on detach,
  manually delivers the stale callback and verifies it cannot restart or change
  the document. Two root source-contract tests cover the guard/lifecycle boundaries.

## Qualification boundaries

Root tests and Actions results must be reported separately. Code48 Android build,
all eight native profiles, and code48 exact-byte install/process checks are pending
at authoring. Successful code44 smokes do not qualify48. Landscape-primary must
actually execute, not be counted as passing after its47 boot failure.
No new recommended APK, complete Word equivalence, universal emoji recognition,
physical acceptance or360–480minute completion claim is made here.

## Code49 follow-up, round34

48 large-font-primary144/145: the existing collapsed-caret List journey could
not hide the keyboard for its screenshot. Startup visibility polling was still
issuing show requests after Android had accepted the initial request, and could
therefore override the subsequent hide.49 keeps bounded readiness polling but
never reissues either show API after acknowledgement. It retains the original
native hide/settled-frame/screenshot assertions; no test retry or timeout increase.
Root source-contract coverage also enforces this no-reissue boundary.48 exact-byte
upgrade smokes passed; this does not certify49.49 native execution remains pending.

## Code50 follow-up, round35

48 final1370 outcomes:1365 passed,5 failed. Backups Back passed all four profiles.
Failures:3 List screenshot keyboard-hide assertions; API36 repeated document IME;
landscape actual Home drag.49 addresses keyboard-hide reissue. The Home diagnostic
showed an accepted release with2 moves and unchanged order: a stationary hover can
swap the same target back before StaggeredGrid finishes layout.50 latches that
specific target until the dragged center leaves its original rectangle. This does
not prohibit dragging back to the original slot or moving through other targets.

50 also allows bounded startup recovery when focus becomes unowned during resize,
but cancels if any other view owns focus or a content drag is active. It never
reissues an accepted show request. A failed document readiness assertion now records
only generic keyboard/focus/generation/attempt state plus a synthetic screenshot;
the same visibility/focus and settled-frame conditions remain mandatory. Further
API36 diagnosis may still be necessary;50 is not yet runtime-qualified.

## Code51 follow-up, round36

50 native:1369/1370 passed; the remaining API36 repeated Notes/Doc creation
assertion reported IME=false while field focus and window focus remained true;
startup exhausted its bounded attempts. Home real drag now passes all profiles,
including previously failing landscape.50 exact-byte upgrade33/35/40 and actual
PDF/DOCX/CSV process recovery also passed. No APK recommendation is made yet.

51 uses WindowInsetsController to hide on returning Home on API30+, matching the
show path, with legacy fallback. The document observes actual IME animation and
requires stable layout before its first show request; it still never reissues an
accepted request. Timeout diagnostics preserve the previous observation. Original
IME visibility/focus/settlement assertions and bounded20 attempts are unchanged.
51 Android/native and exact-byte qualification remain pending.

## Code52 follow-up, round37

Source audit before51 native conclusions: deferring every inserted-field keyboard
request would risk scheduling a first show after a user had already hidden it.
Only a newly created document crosses the Notes→Doc/Home keyboard handoff, so52
adds a one-time session-only handoff marker there. Ordinary inserted checklist/text
fields keep immediate acknowledged requests, matching49/50's passing hide behavior.
The marker is consumed, not stored in note content. All native assertions remain
unchanged;52 must independently qualify. No51 native failure is claimed here.
