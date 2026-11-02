# Pending work: round20

## Final status

Development0.32/code35 qualified: native41449624652 SUCCESS9/9,1278/1278
outcomes/382 unique methods;294 JVM/lint; latest repository746 tests passed.
Both exact-byte signed0.31→0.32 and direct0.30→0.32 Note+Doc update smokes passed.
No public release/physical acceptance, exhaustive rich-library migration or emoji
recognition claim. Package/signing unchanged. Install over existing app; do not
uninstall or clear data.

APK: https://github.com/tarungittarun/TicTic/actions/runs/41449624652/artifacts/13144272420
`MyNotes-v0.32.0-35-development.apk`,155134848bytes, SHA256
`2568af3d33e8856b288a2f76a9d36cde9aa116a25571eac272c0d5cdf0981813`.
Expires2026-11-09T13:08:02Z. Sourcefe6a0ded6b727f7b08c179187d671648e51db560.

Chronological work notes follow; earlier pending statuses are superseded by this
final status. Remaining implementation/research limits remain pending.

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

Final native41449624652 **SUCCESS9/9 jobs**,1278/1278 outcomes,382 unique methods,
294 JVM/lint. Evidence botb05149e5. Rotation regression passed on all4 profiles.
Manual screenshots: dark/large-font and API36 portrait grids are now correct;
landscape grid capture still shows Home and is NOT visual approval. Selector
portrait/landscape captures show selected25 and reachable controls. See
`native/41449624652/MANUAL-REVIEW.md` for explicit scope and remaining capture race.
An additional actual code33→35 smoke is requested because0.30 is the latest
physically confirmed user version; code34→35 already passed. No source/APK change.

Direct0.30/code33→0.32/code35 smoke41452983989 **SUCCESS**, job125302491905,
sourceadc4292573bb6adfee5c3f47a38c0b0ab4001083. Evidence artifact13145951242
(909bytes), digest10d3f654f2571a59db6169881042c07912f802768ce22a1c7d983b9850920277.
The same final bytes/signature and real UI cold-restarted Note+Doc were checked,
with firstInstallTime retained, no uninstall/clear or debug-file injection. This
still is not physical or exhaustive rich-data acceptance.

Exactly11 justified CI jobs this increment:9-job native/build qualification and
two one-job actual-byte update smokes (from each recent development version).
No new speech/emoji training. Emoji filename candidate audit and speech corpus
provenance research are in `PENDING-OCR-RESEARCH-2026-11-02.md`; scanned-PDF OCR is
a plan, not shipped functionality. Landscape grid screenshot race still pending.
