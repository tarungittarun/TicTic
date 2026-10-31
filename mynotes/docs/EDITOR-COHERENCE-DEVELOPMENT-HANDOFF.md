# MyNotes 0.21.0 (24) — integrated Notes and Doc editing

**Qualified development build, not novice, physical-device or public-release approval.**

[Download raw signed APK — MyNotes-v0.21.0-24-development.apk](https://github.com/tarungittarun/TicTic/actions/runs/41204791613/artifacts/13044376601)

GitHub sign-in may be required. This artifact is the APK itself, not a ZIP.
Expires **2026-11-07 13:17:50 UTC**. Size66,707,516 bytes.
GitHub artifact metadata SHA-256 (not independently downloaded/hashed in this sandbox):
`c386b4c3a658ae8374f4e073894c1c8fc69c49cba624303e3db84cea310d7a18`.

Package `app.space.notes`; min26/target34/compile35. Retained signer checked by CI:
`08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`.

## What changed
- Notes stays writing-first; Doc remains a separate rich workspace within the same APK.
- Find stays on the writing page, with query, previous/next, count/preview and Close. Live writing
  updates matches without moving the typing caret. Closing releases temporary observers; no saved
  search marks are added.
- Clearly named Insert/Format tools and drawn icons replace ambiguous main-toolbar glyphs.
- Doc's formatting ribbon stays beside the writing: combine B/I/U/strike without reopening menus.
  Selection versus new-typing scope is explicit. Advanced controls remain available by scrolling.
- Whole-note/document properties use branded temporary trays; selected-content options name their
  target. Cancel keeps the writer, cursor and saved data intact. Creating a folder does not itself
  move the note; explicitly choose a destination. Applied changes remain undoable.
- Large-text and landscape layouts reflow rather than reducing the chosen text size.
- Existing rich content, named content, clean checklist conversion and Copy to Doc are retained.
  No data-schema migration or intentional flattening. No new network service. Calculator unchanged.

## Evidence
[Final Actions run41204791613](https://github.com/tarungittarun/TicTic/actions/runs/41204791613)
against source `9e0e470bf45a91509d03a65d7d59ae73fc3579ee`; evidence bot `11a03654`.

| Gate | Result |
|---|---:|
| Local root source contracts |596 passed|
| Local Notes source contracts |206 passed|
| Actions JVM |241 passed|
| API26 full native |268/268|
| Android16/API36 full native |268/268|
| API34 dark,200% text,portrait |134/134|
| API34 landscape,100% text |134/134|
| Independent exported-PDF checks |4/4 profiles passed|

No omitted native cases or hidden failures. Reports, actual environment snapshots, exported-PDF
probes and synthetic screenshots are under `design-2026-10-29/native/41204791613/`.
Tests cover live Find edits/caret/format preservation, same-editor Cancel, labels Cancel, folder
Create/Move/Undo, actual provider file contents, style selection/new typing and prior rich features.
Automation and rendered screenshots are not first-time-user acceptance.

## Install safely
Export a backup first; separately retain important original recordings. Install over the current
app: **do not uninstall or clear data**. Backups omit original audio, empty private folders, edit
history and saved styles. Schema8 content names remain supported; do not downgrade to an APK
without schema8 support. Keep the backup until the phone walkthrough is complete.

## Still needs real-device acceptance
Motorola edge50neo/Android16 keyboard and selection; novice comprehension; TalkBack and combined
large-text/landscape; permissions, recording, actual file-provider failures, power loss and reminder
delivery under OEM battery policies. These are unverified, not represented as passed.
Billing was not checked; this is not Play/public-release approval.

## Effort and job accounting
Fresh35-job allowance only:35 conservatively reserved,31 actually executed,4 skipped slots from
run3 held unused. No further qualification run planned. The requested180-minute streak was not met;
no uninterrupted duration or idle work is claimed.

[Usage guide](EDITOR-COHERENCE-USER-GUIDE.md) ·
[Design, failures and job ledger](EDITOR-COHERENCE-REDESIGN-AND-JOBS.md)
