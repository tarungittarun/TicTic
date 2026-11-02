# Eleven-item physical feedback — first patch, candidate0.38/code41

Not yet qualified or delivered. Do not equate automated fixtures with phone acceptance.

## Attachment review (private raw export is NOT committed)

The supplied export contains1966 app events across versions0.28–0.32,646 coalesced
save events and114 completed save events. It includes Word preview/Open-file
failures and PDF renderer SecurityExceptions. It does not contain note bodies or
original input documents. These counts do not measure frame time or prove the
cause of lag. The screenshots directly demonstrate a blocking-looking Folders
loading screen and an inappropriate trash glyph on speech opt-in. Preserve those
reports even though newer automated runs passed.

## First patch and exact scope

4. Viewed Files entries now use the same clickable, described card language as
Settings; the Open file action has an explanatory card. History stays separate
from saved Notes/Docs and original files.
6/9/10. Private folder enumeration and private voice listing have separate reader
workers, no longer queued behind serial saves/SAF operations. Only LOCAL folder
names are read on the folder reader; connected-provider access stays serialized
with writes and augments already-usable private controls asynchronously. Connected
checks cannot repaint a later page (generation fences), nor falsely revert to a
pending message after finishing. No save ordering/content durability is weakened.
Voice listing initially constructs20 recording cards and retains Show more for all
remaining recordings; no recordings are deleted. Advanced boost/export/retry/note
actions start collapsed, while primary playback/progress remain available.
7. Speech opt-in uses a microphone, not a trash icon. Generic non-destructive
confirmations use a check; Delete/Remove/Clear retain destructive icons/warnings.
8. Checklist creation moves from Home's bottom row into Library. Existing creation
and editing semantics remain, and Trash never offers it.
11. Folders → Storage settings → Back returns to the actual Folders caller rather
than defaulting to Settings. This is one repaired route, not proof of every Back
path.

New native tests keep the serial writer deliberately blocked while opening private
Folders and Voice Inbox, check caller Back, and inspect the actual speech glyph
plus checklist location. Existing checklist fixtures navigate through Library;
existing playback-boost checks expand Recording options, preserving all original
content/transport assertions. All remain synthetic fixtures.

## Still open — must not call these done

1. Persistent long-press/drag Home ordering and “arranged as per user” Sort mode.
2. True on-page PDF glyph/handle selection (a separate text/OCR dialog is NOT it).
3. Original Word layout fidelity: logos/symbols/spacing/multi-section headers and
footers. Current preview is still approximate, not equivalent to Word.
4/6. Wider visual consistency and measured interaction/frame-time profiling.
5. CSV aliases were fixed in39; its exact-byte three-format process recovery passed
41480207122, including CSV query/row41. The new41 bytes still need their own checks.
9/10/11. Device-provider stalls, very large inboxes, complete Back-route coverage and
physical smoothness need further validation. Private-first UI does not promise a
hung external provider is cured.

Further standing pending work: image/OCR review process-death restoration, direct
glyph overlays, broad licensed Indian speech, universal emoji recognition and
physical startup/playback. No private export upload, no data clearing/uninstall,
no fabricated training, no public release or360-minute completion claim.
