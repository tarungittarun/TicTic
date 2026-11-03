# Candidate54 — responding to candidate53 native findings

2026-11-03. MyNotes0.51.0/code54. Not qualified yet.

Candidate53 source ebf4a1d2, run41578780795: Android build passed, but native
shards found real filter clipping in landscape. Its half-height viewport was
smaller than a native touch row. API26 and font2 also exposed the old assumption
that the first three Sort rows must fit simultaneously. Actual user requirement
now explicitly caps the sheet at half the screen and requires inner scrolling.

Candidate54 reclaims decorative sheet/title vertical padding **only for filters**.
Native touch targets, the existing Close/Apply hierarchy, stable tallest-tab height
and half-screen cap remain. The Sort test now reveals each option through native
inner scrolling, then requires its entire row to be visible (also in landscape).
The far-edge tap test likewise scrolls the row into view first; full-width and
real injected-touch/selection assertions remain.

The older TXT journey expected View only/Open editable copy modal buttons, which
were intentionally replaced by default read-only preview and explicit Create
editable copy. It now follows those controls. Original no-import, Find, edited
copy persistence, original source byte-equality and Back assertions are retained.
The new large/small real-SAF TXT journeys remain unchanged.

A verified pending-folder flag is restored from private metadata and set only
after atomic write/readback. Home, legacy Notes and canonical Notes/Docs show it
after navigation, not only on the save callback. No external head fence is reset.

No candidate53/54 APK should be represented as qualified before the complete
Android results. Source root gate and Actions identity are recorded with the push.
No private attachments are included. Candidate53 jobs are allowed to finish and
preserve their own evidence; candidate54 has a separate concurrency group.
