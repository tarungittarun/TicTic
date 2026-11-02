# Pending work: round20

Source work, not yet qualified or a new APK handoff. Prior0.31 remains the
qualified development download. Candidate0.32/code35 preserves signing/package.

## This increment
- Retained image selector responds to actual window bounds on rotation/resize;
  existing selection view, selected words, normalized crop and bitmap survive.
  Primary actions stay first in the sidebar, other controls scroll. No auto-save.
- Native regression rotates the same dialog both ways, checks identity/selection,
  crop and visible primary actions, restores the orientation request afterward.
- Spreadsheet capture fixture closes/detaches the host startup controller before
  installing its own root, and requires attachment/visibility before capture.
  Old round19 Home captures remain untouched and explicitly invalid grid evidence.
- Full9-job Actions qualification requested; outcomes must be checked, not assumed.

## Still pending
Scanned-PDF OCR needs explicit bounded per-page offline recognition, original-byte
preservation, cancellation and review-before-insertion tests. Direct PDF glyph
selection/vector redraw, exact Office fidelity and process-death restoration are
not implemented by this increment. Models/permissions are unchanged. Universal
emoji OCR and broad Indian-accent training require suitable licensed diverse data
and genuine held-out evaluation; no recognition or physical playback claim.

No private user file/recording uploaded; only synthetic native fixtures.
