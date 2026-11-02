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

Build/JVM294/lint passed for sourcefe6a0ded6b727f7b08c179187d671648e51db560,
native run41449624652 now executing8 shards (not yet a pass). Exact new APK
artifact13144272420,155134848bytes, SHA256
`2568af3d33e8856b288a2f76a9d36cde9aa116a25571eac272c0d5cdf0981813`.
Independent actual release34→35 update smoke requested; version pair is now an
explicit harness argument (legacy33→34 mock self-checks retained). No new APK
handed over until native and actual-byte smoke outcomes are checked.

Actual-byte release34→35 smoke41450689987 **SUCCESS**, source6260495788e38c2461e975dbfd24fe200002c70d,
job125294854374. Artifact13144098598 (908bytes), digest
`ed993bcb74f3f79b5980ee412afec7e6fa11f8059c06c505a817c25089bd5582`.
This validates exact byte identity/manifest/common signer and synthetic Note+Doc
install-over only. Native qualification and new captures remain pending.
