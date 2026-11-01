# MyNotes 0.24.0 / code27 — adaptive-editor development handoff

**Qualified development build; not a public Release or physical-device acceptance.**

## Installable APK

[Download MyNotes-v0.24.0-27-development.apk](https://github.com/tarungittarun/TicTic/actions/runs/41291286599/artifacts/13077372878)

- Raw APK upload (`archive: false`), not a ZIP; GitHub access may be required.
- Size: **66,728,576 bytes**. Artifact expires **2026-11-08 09:03:47 UTC**.
- GitHub artifact metadata SHA-256: `7ec741b412b6ffd066926cde42bd0bf27359295a6b047aeff580366917b6a89a`.
  Not downloaded/independently hashed in this sandbox; signed-download redirects failed
  previously. Do not describe the metadata digest as a local verification.
- Source: `0fbf43f6fe41671e987024c01abfc1b6d5232347`.
- [Qualification run 41291286599](https://github.com/tarungittarun/TicTic/actions/runs/41291286599), all6 jobs SUCCESS.
- Package `app.space.notes`; version0.24.0/code27; minimum API26; schema9 unchanged.
- CI verified the release APK signer against the existing update identity:
  `08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca`.

**Install over the existing app. Do not uninstall or clear storage.** Export a backup
first; retain original voice recordings separately because note backups omit original
audio. Unsupported downgrades remain rejected. No new data migration was introduced.

## Delivered changes

- Clicked images/drawings decode the full bounded stored raster instead of the768px
  inline thumbnail; diagrams redraw their vector content at zoom scale. Existing
  stored limits remain1600px/900KiB: previously discarded pixels cannot be recovered.
- Doc uses the grouped Notes-style Insert surface, retaining document-specific tools.
- Notes uses the shared Doc-style Format controls, including font, size/steppers,
  inline Clear, paragraphs and numbering; pending typing and existing spans retained.
- Hyperlink is Insert-only, consistently named, with selected multiple lines able to
  share one destination. No bottom-row Hyperlink command.
- Doc is directly editable text on each new line, without Write text prompts or an
  Insert Text command. Notes overflow is top-right in portrait.
- Notes/Doc writing editors use real left/right landscape rails and side Format;
  constrained keyboard/large-text views scroll instead of shrinking touch targets.
  Drawing/diagram canvases retain their specialized toolbars, not new dual rails.
- Raised/bordered panels improve separation between writing, tool sections and dialogs.
  Calculator and tests.yml unchanged; no public-release/billing action taken.

## Verification

| Gate / profile | Final result |
|---|---:|
| Local root Python QA | 635 passed |
| Local Notes Python QA | 245 passed |
| JVM domain suite / debug+release build and lint | 260 passed / success |
| API26 light portrait | 292/292 |
| API36 light portrait, app motion enabled | 292/292 |
| API34 dark portrait, font200% | 156/156 |
| API34 light landscape | 156/156 |
| Native total | **896/896**, exact inventory |
| Independent PDF verification | four fixtures on each of four profiles |

Native instrumentation targets the debug-host variant. The signed release APK was
built/linted from the same source; it was not physically installed in this sandbox.
No skipped/duplicate/unexpected native outcomes. Evidence commit `13560101` preserves
336 synthetic JPEGs, original PNG hashes, per-profile environment/PDF/native records and
source-pinned provenance under `design-2026-10-29/native/41291286599/`.

The fresh40-job allowance is **closed40/40 used** (7+9+9+9+6). Earlier failed candidates
are not substituted for the final green result; see ADAPTIVE-EDITOR-REVISION-AND-JOBS.md.

Visual review:144 broader captures on identical production UI code plus48 final-source
control captures; see ADAPTIVE-EDITOR-VISUAL-REVIEW.md. Private uploads stayed local.

## Still requires real-device acceptance

Physical/OEM keyboard and novice acceptance, combined landscape+200% font and other
untested combinations remain unclaimed. On the phone, check install-over/reopen without
loss, attachment zoom, multiline Hyperlink, Notes Format/numbering, and rotation with
keyboard/selection/Undo. Short rails and large-text sheets require scrolling; not every
action fits simultaneously. Automation is not a zero-bug guarantee.
