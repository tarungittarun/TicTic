# MyNotes 0.5 — diagrams, PDF, incoming shares and broader qualification

## Acceptance intent (before Actions qualification)

- **DIA-01** Given a text or checklist note, add process, decision, start/end, input/output, ellipse and text-box shapes; save/reopen preserves geometry, IDs, color and text.
- **DIA-02** Given two shapes, connect them with a directed arrow, edit its label, move/resize either shape; edges retain endpoints. Removing a shape removes only its attached arrows. Undo restores the prior diagram state.
- **DIA-03** Given a drag, cancellation does not commit partial geometry. Keyboard/button-accessible shape list, directional move/resize controls and text dialogs supplement touch dragging.
- **DIA-04** Given old schema 1/2 documents or a schema-3 backup, import preserves old content and every diagram field; malformed/dangling geometry refuses the document instead of silently dropping visuals.
- **PDF-01** Given rich text, checklists and a flowchart, export paginated vector PDF locally, reopen/read back bytes and verify the digest. Source editable notes are unchanged. PDFRenderer can reopen and render the generated pages in emulator tests.
- **SHR-01** Given an Android plain-text share, create a new local note while preserving previous documents. Reject blank or oversized IPC input. Other media remains unsupported.
- **LAB-01** Add labels in bulk without overwriting each note's existing tags; globally rename/combine/remove labels across Notes, Archive and Trash with Home Undo.
- **UI-01** At normal/200% font, day/night, portrait/landscape and tablet resource variants, editing retains a nonzero writing surface and scrollable tools. This does not certify every physical touch path or accessibility service.

## Implementation and boundaries

Schema **3 writes / 1, 2 and 3 reads**. Existing plain-note conflict-copy IDs remain stable; diagrams participate in equality/conflict identities. Copy preserves editable diagrams. Merge deliberately refuses diagram sources rather than flattening/losing their visuals. Backups retain them within the existing 8 MiB backup envelope.

The flowchart is an **800 × 1000 logical canvas** scaled to screen width. Six common shapes, directed labeled edges, drag, button movement, resize, color and text edits are implemented. It is a first diagram editor, not arbitrary freehand ink, multi-page canvas, graph auto-layout, alignment snapping, routing around obstacles, image import or collaborative editing. Nodes can overlap; long visible shape/edge labels are clipped while full text is retained/searchable. Diagram Undo is the existing one-operation undo, not a full undo/redo stack. Diagrams can coexist with a note's text/checklist.

PDF uses Android's native vector PdfDocument and the same shape renderer; no network/model dependency. Export is a document, **not a round-trip backup**. Full notes/labels and flowchart page are exported; no attachments or model output exist yet. Text is paginated; diagrams occupy one fitted page. PDF export has a 200-page working-memory safety ceiling without restricting note storage. It renders to private temporary storage before copying to the local Android documents provider and verifies readback. Provider copy failure can leave a partial destination; the source remains unchanged. PDF text/font appearance still needs physical readers/language qualification.

Incoming sharing supports **ACTION_SEND / text/plain** only. The incoming shared text is saved locally as a new note; it does not import network content or dereference URLs. 100,000-character limit is an IPC safeguard, not an editor/library size limit. Sharing while loading is queued through the existing controller readiness path; adversarial process death before that queue commits remains a qualification gap.

Global labels are derived from documents. Empty/unused global labels are not retained. Bulk replacement remains the default for backwards compatibility; the explicit additive checkbox retains individual labels. Renaming to an existing case-insensitive label combines them. Removal removes only the tag, never notes. No hashtag autocomplete yet.

## Pipeline expansion

The workflow now has six independent early jobs (source, inventory, lint, JVM, release build, instrumentation build), APK audit, **13 emulator lanes**, publication and final acceptance: **22 expanded jobs total**, below the 30-job budget. Matrix cases include disjoint storage/UI/diagram/share-label aspects, minimum and middle APIs, full modern API, real 200% resource font scale, night resources, landscape and Pixel C tablet profile. The runner verifies actual Android font/night/orientation configuration, not just requested metadata.

All lanes reuse the two compiled test APKs and the one audited release. Installed release hashes, install-over, offline configuration and launch smoke remain separate from debug-host instrumentation. Independent lint and immutable release/dependency-pin reports provide different findings without blocking one another's startup. No transitive CVE audit or all-phone certification is claimed. Job growth is purposeful, not a claim that more jobs automatically reduce wall time.

## Still open

Required offline OCR and automatic offline speech transcription are **not implemented** by this release. Neither are media recording, reminders/widgets, full rich typing/markdown, a full undo stack, physical provider/SD failure qualification or wall dragging (reserved for last). No microphone/network permissions are introduced. These must not be confused with delivered diagrams/PDF/share support.

## Qualified outcome

**Delivered** by [40578488274](https://github.com/tarungittarun/TicTic/actions/runs/40578488274).
All 13 lanes passed: **441 executions / 63 unique Android cases**, **51 JVM**;
468 local checks. All installed release hashes matched. Lint and inventory passed.
Publication `894fd9b`, source `957299626c3207ef559260058222c601dc3b7591`,
APK SHA `e61839edd88d7941c71e89ddb26cccdc9b1ef293cf079ead572f98f59d744451`.
The expanded matrix found actual font/landscape selection viewport failures; they were
fixed before publication, not hidden. Runtime still needs the physical review above.
