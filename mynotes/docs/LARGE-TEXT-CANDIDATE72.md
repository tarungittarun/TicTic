# MyNotes 0.72.0 / code 72 — large-text UI follow-up

This is a source candidate, **not yet a qualified APK or release**. The runtime version is increased because app behavior changed.

## User-visible changes

- **Library sheet:** at portrait font scale 1.5× or above, the sheet can use the available screen height rather than the previous 560dp cap. This is intended to make Settings discoverable without shrinking text or touch targets. Any remaining overflow stays scrollable.
- **Image text selection:** portrait count, selected-text preview, and secondary tools now scroll in a bounded area. “Use selected text”, “Keep selected image”, and “Read this area” remain outside that scroller. The layout budgets space for the image preview.

## GitHub build and focused Android qualification

`.github/workflows/notes-improvement70.yml` is now a narrowly source-triggered Candidate72 workflow. Its shared setup action provisions Temurin Java 17, the pinned Android SDK/build tools, and required native build tools/model inputs. It builds release/debug/instrumentation APKs, runs JVM tests and lint, then executes the focused native journey on four emulator profiles, including portrait font scale 2.0. The workflow does not publish or commit evidence.

The focused native cases include the existing Library traversal and image-selection viewport checks. The image check retains the 35% preview assertion and also checks the bounded secondary scroller and un-clipped primary actions. No native test methods were added or removed, so the existing inventory remains unchanged.

## Acceptance status

The source edits and workflow wiring are present locally on the active branch. They have **not yet been pushed to GitHub**, so no Candidate72 Actions build or emulator result exists yet. Do not call the prior candidate71 findings fixed, and do not distribute an APK until the candidate72 build and relevant Android checks pass.

Historical candidate71 results remain unchanged: the full run had failures and the isolated large-text issues were unresolved. Physical-device acceptance remains separate and has not been performed here.
