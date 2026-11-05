# Candidate69 markup follow-up — not qualified or delivered

Candidate68 remains the delivered, source-qualified APK. This continuation addresses two explicit coverage gaps from its receipt without changing models, Calculator, package or signing identity.

- Extend the existing native annotation case to export eight generated PDFs:0/90/180/270-degree rotation, each with and without a nonzero-origin crop box. Require actual PdfRenderer pixels at the visual normalized pen/highlighter coordinates, retained extracted source text, retained rotation and byte-identical original PDFs. These are rendered export checks, not physical drawing-gesture acceptance on those files.
- Make marked-copy writing honor a thread interrupt before opening a destination, between copy/readback chunks and before returning success. Preserve the interrupt flag and original/draft. A copy interrupted after writing starts may leave a partial destination; do not call it verified or delete arbitrary provider files.
- Extend the existing real-SAF native copy case: a pre-interrupted worker must exit with InterruptedIOException and leave the newly created destination empty, then an ordinary retry must verify successfully. This deterministic case does **not** prove power-loss or arbitrary mid-write recovery.

Version is increased to69/0.69.0 because production copy behavior changed. No69 APK delivery or qualification is implied. Source contracts and native declarations must be followed by actual Android execution; complete updated E2E and exact-APK install-over remain required before any next delivery.

## First execution and correction

Focused42019299312 compiled/linted successfully but did not qualify. Its new rotation case stopped at the90-degree text assertion before reaching that variant's pixel checks. The assertion incorrectly demanded unrotated phrase layout. The replacement first verifies original letters and then compares exact before/after PDFTextStripper output; red/yellow pixel-position assertions are unchanged.

The continuation also fixes an existing unreadable-draft hazard found in review: AtomicFile.openRead can throw FileNotFoundException for a file that exists but is not readable. That is not a fresh draft. Refuse loading instead of silently constructing an empty draft that a later save could replace. A native app-UID permission test restores permissions in finally and verifies the retained bytes. No qualification is claimed until the replacement native run passes.
