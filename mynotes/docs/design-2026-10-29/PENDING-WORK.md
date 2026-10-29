# Pending-work continuation — 29 October 2026

## Current delivery boundary

The supplied development APK remains **0.13.1/code15**, source `19db0fd`. It is not a published Release. Last Android results for that source: API26 **184/184**, landscape **60/61**. The remaining Pin journey is not waived. Tablet **61/61** belongs to the preceding code14 source. Public Release remains 0.12/code13.

The new working candidate is **0.13.2/code16**, **source only**. No Android build, native execution, new APK or new runtime capture was produced in this continuation. No additional GitHub Actions jobs were launched.

## Implemented in source this continuation

### Grouping prerequisites
- With zero or one ungrouped shape, Group shapes displays a non-empty signature sheet explaining the prerequisite.
- Its primary Add shape action opens the existing illustrated picker. Cancel leaves the diagram unchanged.
- A legacy single remaining grouped shape can still reach Ungroup; the prerequisite guard does not block that recovery.
- The existing two-choice validation and disabled unavailable Ungroup remain in place.

### OCR review
- Empty/whitespace-only review text has persistent inline guidance, rather than relying on a disappearing toast.
- Save/Append actions are disabled and dimmed until there is text; availability updates as the user edits.
- Language metadata reads English, Hindi / Devanagari or Kannada, rather than the engine code.
- Existing save-time blank-text and unavailable-target guards remain. Merely reviewing or cancelling does not intentionally append anything.
- Large-font/landscape/IME rendering of this review still requires Android capture and inspection; the source changes do not establish that acceptance.

### Pin failure diagnostics
- The previous duplicate programmatic IME dismissal remains removed; that correction is still awaiting Android execution.
- An unclipped-control timeout now records attachment, Activity finishing/destroyed state and decor focus.
- The failure captures a real system screenshot directly, without waiting for animation frames from a potentially detached fixture view.
- The failed assertion still fails. There is no tap retry, performClick fallback or automatic waiver.

## Regression preparation — authored, not executed

Three new Android cases cover:
1. Zero/one-shape grouping prerequisites and unchanged diagram data.
2. Empty OCR review, whitespace validation, editing, cancellation without note mutation, and explicit reviewed-text append.
3. Previous-snapshot restore preview, Cancel and header Back, checking that both the current library and previous checkpoint remain unchanged.

The OCR UI fixture uses an explicitly synthetic recognizer result; it is not a recognition-accuracy test. Restore/group fixtures use isolated synthetic note storage, not user files.

Seven new capture names are registered end-to-end: `group-prerequisites`, `shape-picker`, `read-image-language`, `ocr-review-empty`, `ocr-review`, `backups`, `restore-preview`. They have **not yet been rendered or reviewed**. The previously examined main inventory is still 40 scene types. The capture registry now has 52 normal/fault names, with a bounded 56-image maximum.

Future discovered inventories: **187 full / 64 focused** Android cases. These are expected counts, not passes. The earlier 184/61 results must not be relabelled with the new totals.

## Local verification

- Root structural suite: **534 passed**.
- Notes structural suite: **144 passed**.
- **105 Java files** parsed without syntax errors using tree-sitter; the per-file hashes are in `pending-java-syntax.json`.
- Notes XML parsed; Git whitespace check passed.

Syntax/structural checks are not javac, Android resource linking, lint, APK generation or device execution. Newly added guards pin grouping/OCR availability, failure diagnostics, capture bounds and explicit-only expensive workflow triggers to prevent another incidental source/version-triggered farm.

## Still pending

- Execute the corrected Pin journey and new rare-screen cases on Android; inspect their captures, especially OCR with large text and the keyboard.
- Requalify the latest source across the required release matrix before any public replacement.
- Motorola selection/rotation, image paste into another app, actual notification delivery and physical folder operations.
- Review provider/permission interactions and first-use restore/checkpoint error states. A previous snapshot is only available after a checkpoint-producing operation; the current Backup UI still needs broader first-use walkthroughs.
- Archive enhancement remains paused. Wall drag ordering stays last. Calculator code is unchanged.

The approved CI allocation is exhausted. Any further build/test job requires renewed approval; publication gates have not been relaxed.


## New deep-refinement request (30 October IST)

Source-only follow-up: imported-image crop/rotate/mirror/history editor and per-row checklist actions. See `../REFINEMENT-360-2026-10-30.md`, `../FEATURE-EDIT-AUDIT-2026-10-30.md` and `../LOCAL-AI-PRIVACY-ASSESSMENT.md`. Local source checks now 542 root / 152 Notes; 108 Java syntax parses. Nine JVM and three native cases authored, unexecuted. This does not replace the code15 evidence, produce a code16 APK, complete native acceptance, authorize CI or fulfill the six-hour elapsed-work request.
