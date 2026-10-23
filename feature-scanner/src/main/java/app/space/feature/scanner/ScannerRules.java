package app.space.feature.scanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Page ordering and the scale-to-fit arithmetic for assembling a PDF.
 *
 * <p>Pure so it is tested. The fit calculation is where a document scanner quietly ruins
 * output: a page scaled by width alone overflows the sheet height and the framework clips
 * it, so the bottom of every scanned page is missing and nothing reports an error.
 */
public final class ScannerRules {

    private ScannerRules() {}

    /** PDF user space is 72 units per inch, so A4 is 595 x 842 and US Letter 612 x 792. */
    public static final float A4_WIDTH = 595f;
    public static final float A4_HEIGHT = 842f;
    public static final float LETTER_WIDTH = 612f;
    public static final float LETTER_HEIGHT = 792f;

    /** Margin in points. Zero would put text against the trim edge where printers clip. */
    public static final float MARGIN = 24f;

    /** One page: a picked image and where it sits in the document. */
    public static final class Page {
        public final String id;
        public final String sourceUri;
        public final int pixelWidth;
        public final int pixelHeight;
        public final int rotation;

        public Page(String id, String sourceUri, int pixelWidth, int pixelHeight, int rotation) {
            this.id = id == null ? "" : id;
            this.sourceUri = sourceUri == null ? "" : sourceUri;
            this.pixelWidth = Math.max(0, pixelWidth);
            this.pixelHeight = Math.max(0, pixelHeight);
            this.rotation = normaliseRotation(rotation);
        }

        /** Whether the page has usable dimensions. A zero-sized decode must not become a page. */
        public boolean isUsable() { return pixelWidth > 0 && pixelHeight > 0; }
    }

    /** Rotation is always one of 0, 90, 180, 270; anything else is folded into range. */
    public static int normaliseRotation(int degrees) {
        int out = degrees % 360;
        if (out < 0) out += 360;
        return out / 90 * 90;
    }

    /** Effective size after rotation, which is what the fit maths must use. */
    public static float[] rotatedSize(Page page) {
        boolean swap = page.rotation == 90 || page.rotation == 270;
        float width = swap ? page.pixelHeight : page.pixelWidth;
        float height = swap ? page.pixelWidth : page.pixelHeight;
        return new float[]{width, height};
    }

    /**
     * The scale that fits a page inside the printable area, and the offset that centres it.
     *
     * <p>Returns {@code {scale, left, top}}. Taking the MINIMUM of the width and height
     * ratios is the whole point: the maximum would fill one axis and overflow the other,
     * and the framework clips rather than reports, so the loss is invisible until the PDF
     * is printed.
     */
    public static float[] fitInside(Page page, float sheetWidth, float sheetHeight) {
        float[] size = rotatedSize(page);
        float availableWidth = Math.max(1f, sheetWidth - MARGIN * 2);
        float availableHeight = Math.max(1f, sheetHeight - MARGIN * 2);
        if (size[0] <= 0 || size[1] <= 0) return new float[]{1f, MARGIN, MARGIN};
        float byWidth = availableWidth / size[0];
        float byHeight = availableHeight / size[1];
        float scale = Math.min(byWidth, byHeight);
        float drawnWidth = size[0] * scale;
        float drawnHeight = size[1] * scale;
        float left = MARGIN + (availableWidth - drawnWidth) / 2f;
        float top = MARGIN + (availableHeight - drawnHeight) / 2f;
        return new float[]{scale, left, top};
    }

    public static float[] sheetSize(boolean a4) {
        return a4 ? new float[]{A4_WIDTH, A4_HEIGHT} : new float[]{LETTER_WIDTH, LETTER_HEIGHT};
    }

    /** A landscape page on a portrait sheet, which is the common scan of a wide receipt. */
    public static boolean isLandscape(Page page) {
        float[] size = rotatedSize(page);
        return size[0] > size[1];
    }

    /** Drops pages with no usable dimensions rather than emitting blank ones. */
    public static List<Page> usable(List<Page> pages) {
        List<Page> out = new ArrayList<>();
        if (pages != null) for (Page p : pages) if (p != null && p.isUsable()) out.add(p);
        return out;
    }

    /** Moves a page within the document. Clamps rather than throwing on an out-of-range step. */
    public static List<Page> moved(List<Page> pages, int from, int to) {
        List<Page> out = new ArrayList<>(pages == null ? Collections.<Page>emptyList() : pages);
        if (out.isEmpty()) return out;
        int source = Math.max(0, Math.min(from, out.size() - 1));
        int target = Math.max(0, Math.min(to, out.size() - 1));
        if (source == target) return out;
        Page page = out.remove(source);
        out.add(target, page);
        return out;
    }

    /** Rotates one page a quarter turn further. */
    public static Page rotated(Page page, int extraDegrees) {
        return new Page(page.id, page.sourceUri, page.pixelWidth, page.pixelHeight,
                page.rotation + extraDegrees);
    }

    /** Orders pages by their position in the document, ties on id for stability. */
    public static List<Page> ordered(List<Page> pages) {
        List<Page> out = new ArrayList<>(pages == null ? Collections.<Page>emptyList() : pages);
        Collections.sort(out, new Comparator<Page>() {
            @Override public int compare(Page a, Page b) { return a.id.compareTo(b.id); }
        });
        return out;
    }

    /** A filename that cannot contain a character storage rejects. */
    public static String fileName(long createdAt, String label) {
        String when = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT)
                .format(new java.util.Date(createdAt));
        String safe = sanitise(label);
        return safe.isEmpty() ? "scan-" + when + ".pdf" : "scan-" + when + "-" + safe + ".pdf";
    }

    public static String sanitise(String label) {
        if (label == null) return "";
        String trimmed = label.trim().replaceAll("[^A-Za-z0-9_-]+", "-")
                .replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        return trimmed.length() > 40 ? trimmed.substring(0, 40) : trimmed;
    }

    /** Rough output size, for a warning before the user assembles a very large document. */
    public static long estimatedBytes(int pageCount, int averagePixelWidth, int averagePixelHeight) {
        if (pageCount <= 0) return 0;
        // JPEG-ish compression in the PDF container is roughly a fortieth of the raw pixels.
        long raw = (long) Math.max(1, averagePixelWidth) * Math.max(1, averagePixelHeight) * 3L;
        return pageCount * raw / 40L;
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.ROOT, "%.0f KB", bytes / 1024f);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024f * 1024f));
    }

    /** "1 page" rather than "1 pages" -- small, but it appears on every document. */
    public static String pageLabel(int count) {
        return count + (count == 1 ? " page" : " pages");
    }

    // ---- crop and enhance: the pure maths of straightening a photographed page ----

    public static final int MODE_ORIGINAL = 0;
    public static final int MODE_GRAY = 1;
    public static final int MODE_MAGIC = 2;

    public static float clamp01(float value) {
        return value < 0f ? 0f : (value > 1f ? 1f : value);
    }

    /**
     * Four points in any order, out as top-left, top-right, bottom-right, bottom-left.
     * The perspective map is only correct when source and destination corners correspond,
     * and "correspond" needs a canonical order: TL minimises x+y, BR maximises it,
     * TR maximises x-y, BL minimises it. True for any convex quad, which a dragged crop
     * frame always is until the user crosses the corners -- and crossing them just mirrors
     * the output rather than crashing anything.
     */
    public static float[] orderQuad(float[] pts) {
        if (pts == null || pts.length < 8) return new float[8];
        int tl = 0, tr = 0, br = 0, bl = 0;
        float minSum = Float.MAX_VALUE, maxSum = -Float.MAX_VALUE;
        float maxDiff = -Float.MAX_VALUE, minDiff = Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float x = pts[2 * i], y = pts[2 * i + 1];
            float sum = x + y, diff = x - y;
            if (sum < minSum) { minSum = sum; tl = i; }
            if (sum > maxSum) { maxSum = sum; br = i; }
            if (diff > maxDiff) { maxDiff = diff; tr = i; }
            if (diff < minDiff) { minDiff = diff; bl = i; }
        }
        return new float[]{
                pts[2 * tl], pts[2 * tl + 1],
                pts[2 * tr], pts[2 * tr + 1],
                pts[2 * br], pts[2 * br + 1],
                pts[2 * bl], pts[2 * bl + 1]};
    }

    /**
     * The output size of a corrected crop: the longer horizontal edge becomes the width, the
     * longer vertical edge the height, so no source pixel is squeezed. Ordered TL,TR,BR,BL.
     */
    public static int[] correctedSize(float[] orderedQuad) {
        if (orderedQuad == null || orderedQuad.length < 8) return new int[]{1, 1};
        double top = edge(orderedQuad, 0, 1);
        double right = edge(orderedQuad, 1, 2);
        double bottom = edge(orderedQuad, 2, 3);
        double left = edge(orderedQuad, 3, 0);
        int width = (int) Math.ceil(Math.max(top, bottom));
        int height = (int) Math.ceil(Math.max(left, right));
        return new int[]{Math.max(1, width), Math.max(1, height)};
    }

    private static double edge(float[] pts, int a, int b) {
        double dx = pts[2 * b] - pts[2 * a];
        double dy = pts[2 * b + 1] - pts[2 * a + 1];
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * The 4x5 colour matrix for an enhance mode. Gray is the standard luma projection;
     * "magic" is a contrast stretch -- documents photographed under room light come out
     * washed gray, and pulling the blacks down and the whites up is what makes text readable.
     */
    public static float[] correctionMatrix(int mode) {
        if (mode == MODE_GRAY) {
            return new float[]{
                    0.2126f, 0.7152f, 0.0722f, 0, 0,
                    0.2126f, 0.7152f, 0.0722f, 0, 0,
                    0.2126f, 0.7152f, 0.0722f, 0, 0,
                    0, 0, 0, 1, 0};
        }
        if (mode == MODE_MAGIC) {
            return new float[]{
                    1.4f, 0, 0, 0, -50f,
                    0, 1.4f, 0, 0, -50f,
                    0, 0, 1.4f, 0, -50f,
                    0, 0, 0, 1, 0};
        }
        return new float[]{
                1, 0, 0, 0, 0,
                0, 1, 0, 0, 0,
                0, 0, 1, 0, 0,
                0, 0, 0, 1, 0};
    }

    /** A page whose pixels were re-made: same identity and rotation, new source and size. */
    public static Page resourced(Page page, String newUri, int width, int height) {
        return new Page(page.id, newUri, width, height, page.rotation);
    }
}
