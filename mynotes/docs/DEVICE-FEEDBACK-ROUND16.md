# Physical viewer feedback: next-source work, not delivery approval

The user's 0.30/code33 feedback supersedes previous synthetic viewer usability
confidence. The private diagnostics remain outside Git and are not uploaded to CI.
They show a Word render failure at the oversized-line/image fit branch. This does
not establish a corrupt Word source. Earlier retained spreadsheet extraction errors
must not be mistaken for a new grid-parser failure in the current version.

## Prepared changes

- Universal search hint: **Search all content & settings** in both workspaces.
- Standalone Activity retains its live UI for orientation/screen geometry changes.
  Parsed workbooks, current query/sheet/scroll and PDF resources are not deliberately
  discarded on rotation. Font-scale and theme changes still use normal recreation.
  This is not process-death restoration. PDF page sizing responds to available width.
- Preview Back targets capture the actual opening context: Home, viewed history,
  an editor or Storage. PDF/Word/spreadsheets no longer unconditionally enter Storage.
- Oversized DOCX inline lines/images scale to the available empty paper body, preserving
  their source text and subsequent paragraphs instead of rejecting the preview.
  Approximate layout warnings remain. Legacy binary .doc still needs another reader.
- PDF pinch zoom (1–5x), panning while zoomed and double-tap reset. This magnifies the
  bounded raster; it is not arbitrary-resolution/vector zoom. Normal unzoomed dragging
  remains RecyclerView scrolling. Recycled pages reset zoom.
- Visible Select page text and Share original controls use the existing 48dp icon design.
  Long-pressing a page opens selectable embedded text; direct glyph selection/highlights
  over the raster are NOT implemented. Scanned pages still need OCR. Sharing grants
  read-only original access, not write access, and invokes the Android chooser.
  Spreadsheet sharing uses the same host operation.
- OCR selection opens immediately after recognition. Tap words/drag an area/pinch and
  choose selected text or an image crop. Selection alone does not save or alter notes;
  existing review/explicit-save fences and original pixels remain authoritative.

## Qualification

Added Android regressions for actual two-pointer page zoom/double-tap/reset,
oversized Word images followed by text, real SAF spreadsheet rotation with query
retention/original bytes/no import, caller-aware PDF Back, and selection-first review.
Current inventory:381 owned native methods and294 JVM cases. Added five host-side
safety contracts; these are source checks, not Android or physical acceptance.

Candidate version is0.31/code34 with unchanged package/signing, no Internet permission
and no data clearing. Actions-only compilation/native qualification is required before
handoff. No new physical speech/playback/emoji accuracy claim is made. The trained
speech engine, Vosk default and OCR weights are unchanged by this feedback round.

## Active qualification checkpoint

- Production/source candidate: `fec11f1985a190643d8e9af4e9f4d960696dcb8c`,
  Actions round17/run41432202683. Compilation,294 JVM cases, release/debug lint and
  stable-signature APK preparation passed. Eight native shards are still running.
- Round16/run41432085955 was superseded/cancelled before native qualification, to
  ensure the tall-image test includes non-final-line paragraph spacing. Not a failure
  result or additional successful native evidence.
- Raw0.31/code34 artifact13135456219 exists,155134196 bytes, SHA256
  `a3f8cb0168cab69aaa3e40ef32815fde76afde2c8d0672a39c930968b368ad3b`.
  Artifact existence is not delivery approval; final native checks are pending.
- Independent run41433217010 verifies the actual raw release33/34 APK bytes, manifests
  and common signing certificate, then attempts a real UI-created Note + Doc cold
  restart and install-over on a fresh API34 emulator. No debug APK, run-as file
  injection, uninstall or pm clear. It runs in parallel with native qualification;
  outcome is pending, not a passing migration claim. Its self-tests use mocked
  devices and must never be reported as emulator success.
- Latest local root gate:738 passing host tests. No new user APK handed off.

Release-smoke run41433217010 stopped before any emulator execution: SDK manager was
not on PATH (exit127). Switched to the same explicit command-line-tools SDK path
as the existing build setup. This failed setup is not install-over evidence. Added
bounded public-fixture UI failure annotations for the next smoke run; no phone data.

Run41433537525 **passed** the real release33→34 Note + Doc update and APK byte,
manifest/no-INTERNET/common-signer checks. Evidence artifact13134893449,908 bytes,
SHA2561044234242821614397ea3ae0a5e66935e5559cd888ed65064f8991f117b9d81.
This is actual emulator execution, separate from mocked harness self-tests.

Round17 native checks found the new pinch fixture used fixed raw pixel distances
below modern Android's minimum physical scaling span. API26 B185/185 passed;
completed landscape/font A shards passed, while landscape/font B and API36 A
reported only the two-pointer zoom assertion. Corrected fixture uses actual display
dimensions, stays inside view bounds and retains zoom/double-tap/reset assertions.
No production gesture threshold, assertion removal or fabricated passing outcome.
Next source must be requalified; round17 is not a green candidate handoff.
