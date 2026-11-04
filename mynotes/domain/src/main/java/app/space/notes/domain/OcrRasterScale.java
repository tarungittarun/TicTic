package app.space.notes.domain;

/** Bounded OCR raster enlargement; viewer zoom is deliberately independent. */
public final class OcrRasterScale {
    private OcrRasterScale() { }
    public static int decodeSample(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid image dimensions");
        int sample = 1;
        while ((long)(width / sample) * (height / sample) > 4000000L
                || Math.max(width, height) / sample > 3200) sample *= 2;
        return sample;
    }
    /** Keep already readable ink at native resolution; interpolation can erase native-script details. */
    public static boolean readableInkRows(boolean[] ink) {
        if (ink == null || ink.length > 3200) throw new IllegalArgumentException("Invalid bounded row projection");
        int[] heights = new int[ink.length]; int bands = 0, start = -1, last = -1;
        for (int y = 0; y <= ink.length + 2; y++) {
            if (y < ink.length && ink[y]) {if (start < 0) start = y; last = y;}
            else if (start >= 0 && y - last > 2) {
                int height = last - start + 1;
                if (height >= 8 && height <= 128 && height * 4 <= ink.length * 3) heights[bands++] = height;
                start = -1;
            }
        }
        if (bands == 0) return false;
        java.util.Arrays.sort(heights, 0, bands);
        return heights[(bands - 1) / 2] >= 20;
    }
    public static double factor(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid image dimensions");
        long pixels = (long) width * height;
        if (pixels > 4000000L) throw new IllegalArgumentException("OCR image exceeds pixel budget");
        // Tall screenshots need enlargement too. Limit to 2x, 4MP and 3200px/edge.
        double desired = Math.min(width,height) >= 1000 ? 1.0 : Math.min(2.0, 1200.0 / Math.min(width, height));
        double budget = Math.min(Math.sqrt(4000000.0 / pixels), 3200.0 / Math.max(width, height));
        return Math.max(1.0, Math.min(desired, budget));
    }
}
