package app.space.notes.domain;

/** Bounded OCR raster enlargement; viewer zoom is deliberately independent. */
public final class OcrRasterScale {
    private OcrRasterScale() { }
    public static double factor(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid image dimensions");
        long pixels = (long) width * height;
        if (pixels > 4000000L) throw new IllegalArgumentException("OCR image exceeds pixel budget");
        // Tall screenshots need enlargement too. Limit to 2x, 4MP and 3200px/edge.
        double desired = Math.min(2.0, 1200.0 / Math.min(width, height));
        double budget = Math.min(Math.sqrt(4000000.0 / pixels), 3200.0 / Math.max(width, height));
        return Math.max(1.0, Math.min(desired, budget));
    }
}
