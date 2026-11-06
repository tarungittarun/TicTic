# OurApp_vs_Obsidian — phase 1

[Open OurApp_vs_Obsidian.pdf](OurApp_vs_Obsidian.pdf) — **39 pages**, two modules, 20 scoped findings with actual screenshot evidence, impact, suggested direction and manual acceptance checks.

1. Finding and organising notes.
2. Editing and reading note content.

This installment is a **manual expert UX inspection of retained real frames plus source review**, not a new live device session or completed participant usability study. It does not rank smoothness or claim timing results. New hands-on tasks and frame/performance measurements are explicitly marked not run. Native/E2E counts are not used as UX scores. Features unique to either app do not earn feature-count credit.

See the PDF for methodology, your five concerns, evidence limits, comparative trade-offs, priorities, an eight-task manual protocol, safety gates and remaining-module scope. No runtime code was changed or Android build requested.

- `OurApp_vs_Obsidian.md`: text edition of the findings.
- `review-data.json`: structured findings and source frame references.
- `evidence-manifest.json`: SHA-256 and dimensions of the 33 inspected retained source frames; no duplicate screenshots are stored in this folder.
- Generator: `mynotes/tools/comparisons/ourapp_ux_report.py` (Pillow + ReportLab).

MyNotes baseline: delivered 0.69.0/code69, source/capture provenance in run42021523354. Obsidian: official1.15.0/code396, capture source42179340885, retained report42182162974. Baseline source review at `bf39d35dc625a86a84f44d03a178da4569a2b594`. Earlier capture methods, content and OS environments were not matched; the PDF states these limits rather than inventing a live A/B comparison.
