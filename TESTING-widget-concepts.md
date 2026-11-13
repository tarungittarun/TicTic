# Widget concepts port validation record

**Scope:** fifteen purpose-built widget concepts plus responsive Month Grid, ported onto the MySpace-refactored `arena/3caca636-tictic` branch. This worktree started at `e882f01ccd048b38fc0006ceefb11c64d0ff1718`. These results belong to this target checkout only; earlier checks from `/home/user/TicTic` are not evidence for this port.

## Target-checkout source validation

- `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest qa.test_source_contracts -q` — **390 tests passed**. This is the existing TicTic/MySpace source-contract suite; it does not compile Android code.
- `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest qa.test_widget_concepts -q` — **11 tests passed**, covering the new catalog/provider mapping, task actions, layouts, compact/full Month Grid, timer refresh and calendar/countdown wiring.
- Python `xml.etree.ElementTree` parsed **128 app XML files with 0 parse errors**. The scan found **39 widget provider metadata files**, **41 widget layout files**, and **0 metadata-to-layout references missing**.
- A static Java `R.type.name` scan found **0 undefined candidates across 413 references**. This is a source-level scan, not AAPT/resource linking.
- Temporary `javalang` parsing (installed under `/tmp`, not added to the project) parsed **30 app Java files with 0 syntax errors**. This does not type-check Android APIs or compile Java against the SDK.
- A Python `HTMLParser` count verified the linked design board contains **35 widget cards**; the focused widget contract also checks the 35-design/39-provider counts.
- PyYAML parsing of the new workflow confirmed `workflow_dispatch` is its only trigger and it contains separate contract/build jobs; a source scan found no artifact-upload or Git-push step.
- `git diff --check` — passed on the current worktree.

A broader `python3 -m unittest discover -s qa -q` attempt ran **1,146 tests** but was not a clean repository-wide result. It mixes unrelated MyNotes qualification suites and reported **19 errors / 6 failures**, including missing optional PyYAML/Pillow, unavailable pinned MyNotes source-baseline files, and MyNotes retained-inventory mismatches. That aggregate attempt also caught two TicTic-specific contract assumptions; those were corrected and the focused TicTic suites above were re-run successfully. The broad aggregate was not re-run after those corrections, so the full mixed QA inventory remains unqualified. No MyNotes build, package, signing, install-over or Actions result is attributed to this TicTic work.

## Android build and runtime status

This workspace has no local Java/JDK, Gradle installation, Android SDK or `adb`; local Android compilation and emulator/device validation are therefore unavailable. A **manual-only**, non-publishing `Widget Concepts CI` workflow separates source contracts from JVM/Android compile checks. It builds `:app:assembleLiteDebug` only when dispatched and does not upload an APK, commit evidence, or publish anything. This file records local source checks; any remote Actions build/test outcome is reported separately and is not inferred from these checks.

No local APK, signature/install-over result, emulator, launcher resize/tap, permission, alarm/timer/routine scheduling or physical-device result is claimed. Source checks and Java syntax parsing do not prove Android API compatibility or RemoteViews behavior; a CI compile does not replace runtime QA. Use [`qa/QA-widget-concepts-manual.md`](qa/QA-widget-concepts-manual.md) for the separate runtime matrix.
