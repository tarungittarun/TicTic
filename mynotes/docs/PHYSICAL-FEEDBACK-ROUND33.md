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
