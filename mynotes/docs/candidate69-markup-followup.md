# Candidate69 markup follow-up — not qualified or delivered

Candidate68 remains the delivered, source-qualified APK. This continuation addresses two explicit coverage gaps from its receipt without changing models, Calculator, package or signing identity.

- Extend the existing native annotation case to export eight generated PDFs:0/90/180/270-degree rotation, each with and without a nonzero-origin crop box. Require actual PdfRenderer pixels at the visual normalized pen/highlighter coordinates, retained extracted source text, retained rotation and byte-identical original PDFs. These are rendered export checks, not physical drawing-gesture acceptance on those files.
- Make marked-copy writing honor a thread interrupt before opening a destination, between copy/readback chunks and before returning success. Preserve the interrupt flag and original/draft. A copy interrupted after writing starts may leave a partial destination; do not call it verified or delete arbitrary provider files.
- Extend the existing real-SAF native copy case: a pre-interrupted worker must exit with InterruptedIOException and leave the newly created destination empty, then an ordinary retry must verify successfully. This deterministic case does **not** prove power-loss or arbitrary mid-write recovery.

Version is increased to69/0.69.0 because production copy behavior changed. No69 APK delivery or qualification is implied. Source contracts and native declarations must be followed by actual Android execution; complete updated E2E and exact-APK install-over remain required before any next delivery.
