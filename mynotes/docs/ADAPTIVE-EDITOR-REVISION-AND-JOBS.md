# Adaptive editors, full-resolution previews — fresh40-job authorization

Request 2026-11-01; baseline931e681c (delivered0.23.0/code26).
New40 individual Actions-job cap, separate from the closed prior20/40 allocation.

## Acceptance
- Zoom image/drawing previews decode full stored resolution, not inline thumbnails. Diagrams redraw
  vectors at zoom scale. Never invent lost source pixels; existing imports were bounded/re-encoded.
- Doc Insert adopts the grouped, labelled Notes Insert surface; no duplicate Hyperlink bottom tool.
- Notes Format adopts Doc's format controls (font, size, clear, paragraph and numbering). Hyperlink
  lives in Insert in both editors and supports selected multi-line text with one destination URL.
- No visible Write text hints in Doc; empty writing lines remain editable by default.
- Notes overflow moves from the bottom dock to the top-right corner.
- Landscape editors move top/bottom controls into left/right vertical rails, keeping the document
  between them and frequent actions low. Preserve focus, selections, Undo and keyboard reachability.
- Inspect surface depth, separation, accessibility, portrait/landscape and large text across the app.
- Split exhaustive native inventories into more parallel jobs without omitting methods; count every
  individual job conservatively against40. Monitor through completed qualification, not pending.
- Calculator/tests.yml frozen; Android builds only in Actions; root QA gates every push.

## Attachments
User diagnostics(7) and six-page PDF inspected locally only; do not commit private attachments or text.
Diagnostics include history from older builds; inspect final environment before attributing events.
Full-resolution display cannot reconstruct pixels discarded by older imports.

## Jobs
| Reservation | Jobs | Allocated | Source/run | Result |
|---|---:|---:|---|---|

0 reserved/executed. Prior allowances are not added. No public release or billing approval inferred.

## Candidate 1 — reservation before launch
7/40 slots reserved (build + API26 a/b + API36 a/b + dark200% + landscape).
0 executed at reservation; 33 remain unallocated. No results claimed yet.
Full API inventories are method-sharded (151 + 140 = 291), disjoint and exhaustive,
including CoreJourneys on both halves. The 11 PDF tests stay together on shard a;
focused profiles retain all155 methods and PDF verification. JVM inventory260.
Local root631 tests PASS; changed/new Java parses; Calculator/tests.yml unchanged.
Candidate version0.24.0/code27, same package/signing policy. No public release authorization.

Implemented: shared FormatControls in both data-model-independent editors; grouped shared InsertPalette;
Notes top-right overflow (Pin/Archive retained inside it to avoid clipping the new corner action);
Insert-only multiline Hyperlink, Notes font/size/number controls and pending inline typing;
scrollable actual left/right controls in landscape with a side Format panel; raised/bordered tool surfaces;
full-stored-resolution zoom decode and uncached vector redraw; no Doc Write text hints.
Native authored checks cover shared Notes size/number persistence, multiline hyperlink/mixed styles in
both editors, actual rail geometry, and1600px stored preview decoding. Qualification still pending.
