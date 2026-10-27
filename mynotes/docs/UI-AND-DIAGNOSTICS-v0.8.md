# MyNotes 0.8 — UI clarity and local diagnostics

Status: **candidate 0.8.0(9), Android qualification pending**. Do not confuse this with accepted 0.7.0(8). This slice prioritizes usability, not new note features.

## Navigation and hierarchy

- **Home:** notes retain their virtualized grid/list and off-thread search. Library navigation is disclosed under **Library** rather than four permanently competing destination buttons. The bottom dock has five stable icon targets: new text, new checklist, filters, undo, settings. A short hint explains creation and long-press help.
- **Editor:** fixed Back / note type / Pin / Undo toolbar, independent scrollable content and a fixed tool dock. Secondary tools live in **Writing**, **Capture**, **Organise** and **More** disclosure groups. Back/Pin/Undo are not repeated in the normal More group.
- **Writing:** B, I, U, strike, H1/H2 and clear formatting. Their full accessibility names and tooltips remain Bold, Italic, Underline, Strike, Heading 1, Heading 2 and Clear.
- **Capture:** diagram, PDF export, image OCR, voice recording and voice inbox. **Organise:** reminders, labels and colour. **More:** sharing, copying, archive/Trash and conversion. Checklist-specific controls have their own disclosure group.
- **Diagrams:** Add shape, selected-shape editing/connection/resize, position/undo, shape list and arrow list are grouped rather than exposing every command at once. The existing canvas and note data are unchanged.
- **Voice inbox:** each recording has a compact five-icon row rather than a horizontally overflowing row of five long labels. Recording dates, state and transcript previews remain visible.
- **Settings:** storage/recovery, widget placement, model/license notices, diagnostics and an icon guide. New chrome uses a cool light palette, dark-theme equivalents, rounded controls and touch ripples.

Only the main flows above were redesigned; this is not a claim that every legacy secondary dialog, widget or physical launcher is now a finished design system. UI/UX review remains the priority before further product features.

## Deliberate symbol meanings

Icons are drawn as consistent outline paths, not emoji or device-font substitutes. Icon-only controls have at least 48dp minimum touch targets, accessible names and Android long-press tooltips. Labels remain where there is room and ambiguity would otherwise be high.

| Outline symbol | Meaning / location |
|---|---|
| Plus | New text on Home; capture/add tools in the editor |
| Three checked lines | New checklist |
| Descending-width horizontal lines | Filters, not navigation |
| Bent left arrow | Undo |
| Cog | Settings |
| Left arrow | Back |
| Pushpin | Pin/unpin; accessible name reflects the action |
| Underlined A | Text formatting group |
| Tag | Organise group / Labels |
| Vertical three dots | More actions |
| Three connected circles | Share (paired with its label in secondary tools) |
| Bell | Reminder (paired with its label) |
| Palette with colour wells | Colour (paired with its label) |
| Triangle | Play recording |
| Square | Stop playback |
| Up arrow over a tray | Export WAV |
| Circular arrow | Retry transcription |
| Lined document | Open the recording's note |

The two plus uses are contextual; screen position, tooltip and accessible name distinguish them. No icon is intended to replace user guidance entirely. On-device discoverability, contrast, TalkBack and motor-accessibility feedback are still important.

## Diagnostics: what the switch means

**Off by default. Settings → Collect diagnostic logs** persists across app restarts. Initialization also runs for cold application starts. Turn it on before reproducing an issue; it cannot reconstruct events from before enablement.

Collected, best-effort:

- Timestamped, elapsed-time and thread-tagged app breadcrumbs; reviewed semantic UI action names, disclosure changes, selection/formatting outcomes, saves, storage transfers, import/export results and controller recovery events.
- Picker cancellation/acceptance and contextual permission outcomes, without destination URIs or shared content.
- App/package version, Android version/security patch, manufacturer/model/ABIs, locale/timezone, screen dimensions/density/orientation/font scale/night mode and power-saving state.
- Available/free memory, PSS/native allocation samples, free private storage, microphone/notification availability, low-memory/trim callbacks.
- Caught failure class/stack frames and best-effort uncaught Java crash stack frames, **without exception messages**. The previous crash handler is chained, not suppressed.

Not collected: note titles/bodies, custom label/control captions, search text, transcripts, audio, images, clipboard, URI/file paths, account identifiers or other apps' logs. A reviewed static control-name allowlist prevents dynamic user captions entering UI breadcrumbs. Device information/timing can still identify a phone; review exports before sharing.

This is **not “all possible information” or a guarantee to explain every failure**: native crashes, OS process kills, events while disabled, uninstrumented paths, rotated-out records, queue saturation and failed storage can leave gaps. No screenshots, system-wide logcat or silent upload are performed.

## Storage/export controls

- App-private rotating JSON-lines files: approximately **1 MiB maximum** across current/previous files.
- A bounded 256-entry background queue; saturated queues drop breadcrumbs rather than block the UI. Diagnostics must not break note editing.
- Disable stops collection and invalidates queued records; existing files remain available for export until deleted.
- Delete invalidates queued writes and removes app-local log files. It does not remove previously exported copies.
- Export takes an ordered private snapshot, then writes on a separately bounded worker. Slow/failed document-provider I/O does not hold the logging lock. Export errors retain private logs; a partial destination may need removing.
- Export is user initiated via Android's document picker. There is no new network/storage/log-reading permission. For local-only export choose on-device/SD storage rather than a cloud-backed provider.

### Reporting a problem

1. Settings → enable collection. Optionally delete old diagnostic logs first.
2. Reproduce the problem once; note what you expected and what happened.
3. Settings → Export diagnostics; inspect the text file and share it deliberately alongside your description. Add a manually reviewed screenshot if the problem is visual.
4. Disable collection when finished; delete local logs if desired.

## Qualification policy

Real Android tests cover icon names/tooltips/touch minima, hidden-versus-open editor tools, default-off/persistence/disable, queue invalidation/deletion, bounded rotation, JSON export and exception-message/custom-caption exclusion. Existing journeys now open the actual disclosure control and match accessible icon names; they do not force hidden views visible. The formatting-layout check explicitly opens Writing before requiring positive tool dimensions, matching the redesigned interaction.

The usual source/JVM/lint/build/audit/update/emulator gates remain mandatory before publication. Physical-phone beauty/discoverability and TalkBack outcomes are not implied by dispatch-based tests. Previous native component soak results are historical, separately scoped evidence—not a long UI/diagnostics stress run of this candidate.
