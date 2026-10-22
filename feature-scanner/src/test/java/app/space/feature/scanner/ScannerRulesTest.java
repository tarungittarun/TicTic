package app.space.feature.scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ScannerRulesTest {

    private static ScannerRules.Page page(String id, int w, int h, int rotation) {
        return new ScannerRules.Page(id, "content://img/" + id, w, h, rotation);
    }

    @Test public void sheetSizesAreTheStandardPdfUserSpaceDimensions() {
        // PDF user space is 72 units per inch, so A4 is 595 x 842 and Letter 612 x 792.
        float[] a4 = ScannerRules.sheetSize(true);
        assertEquals(595f, a4[0], 0.001f);
        assertEquals(842f, a4[1], 0.001f);
        float[] letter = ScannerRules.sheetSize(false);
        assertEquals(612f, letter[0], 0.001f);
        assertEquals(792f, letter[1], 0.001f);
    }

    @Test public void aPageIsScaledToFitBothAxesNotJustTheWidth() {
        // This is the whole point of the minimum. Scaling by width alone would overflow the
        // sheet height, and the framework clips rather than reports, so the bottom of every
        // scanned page would be missing with nothing to indicate why.
        ScannerRules.Page tall = page("a", 1000, 4000, 0);
        float[] fit = ScannerRules.fitInside(tall, ScannerRules.A4_WIDTH, ScannerRules.A4_HEIGHT);
        float drawnHeight = 4000 * fit[0];
        float drawnWidth = 1000 * fit[0];
        assertTrue("height must fit: " + drawnHeight,
                drawnHeight <= ScannerRules.A4_HEIGHT - ScannerRules.MARGIN * 2 + 0.01f);
        assertTrue("width must fit: " + drawnWidth,
                drawnWidth <= ScannerRules.A4_WIDTH - ScannerRules.MARGIN * 2 + 0.01f);
        // The height is the binding constraint for a tall page, so the width is smaller
        // than the available width and the page is centred horizontally.
        assertTrue(drawnWidth < ScannerRules.A4_WIDTH - ScannerRules.MARGIN * 2);
    }

    @Test public void aWidePageIsConstrainedByWidthInstead() {
        ScannerRules.Page wide = page("b", 4000, 1000, 0);
        float[] fit = ScannerRules.fitInside(wide, ScannerRules.A4_WIDTH, ScannerRules.A4_HEIGHT);
        assertTrue(4000 * fit[0] <= ScannerRules.A4_WIDTH - ScannerRules.MARGIN * 2 + 0.01f);
        assertTrue(1000 * fit[0] < ScannerRules.A4_HEIGHT - ScannerRules.MARGIN * 2);
    }

    @Test public void theDrawingIsCentredWithinTheMargins() {
        ScannerRules.Page square = page("c", 1000, 1000, 0);
        float[] fit = ScannerRules.fitInside(square, ScannerRules.A4_WIDTH, ScannerRules.A4_HEIGHT);
        float drawn = 1000 * fit[0];
        float expectedLeft = ScannerRules.MARGIN
                + (ScannerRules.A4_WIDTH - ScannerRules.MARGIN * 2 - drawn) / 2f;
        assertEquals(expectedLeft, fit[1], 0.01f);
        // Left is never less than the margin, so nothing is drawn into the trim area.
        assertTrue(fit[1] >= ScannerRules.MARGIN);
        assertTrue(fit[2] >= ScannerRules.MARGIN);
    }

    @Test public void rotationIsFoldedIntoQuarterTurnsAndSwapsTheEffectiveSize() {
        assertEquals(0, ScannerRules.normaliseRotation(360));
        assertEquals(90, ScannerRules.normaliseRotation(450));
        assertEquals(270, ScannerRules.normaliseRotation(-90));
        assertEquals(180, ScannerRules.normaliseRotation(181));
        float[] upright = ScannerRules.rotatedSize(page("a", 1000, 2000, 0));
        assertEquals(1000f, upright[0], 0.001f);
        float[] sideways = ScannerRules.rotatedSize(page("a", 1000, 2000, 90));
        assertEquals(2000f, sideways[0], 0.001f);
        assertEquals(1000f, sideways[1], 0.001f);
        // A rotated page must be fitted using its rotated size, or the fit maths is wrong
        // by the aspect ratio.
        assertFalse(ScannerRules.isLandscape(page("a", 1000, 2000, 0)));
        assertTrue(ScannerRules.isLandscape(page("a", 1000, 2000, 90)));
    }

    @Test public void pagesWithNoUsableDimensionsAreDroppedRatherThanEmittedBlank() {
        List<ScannerRules.Page> out = ScannerRules.usable(Arrays.asList(
                page("a", 100, 200, 0), page("b", 0, 200, 0), page("c", 100, 0, 0), null));
        assertEquals(1, out.size());
        assertEquals("a", out.get(0).id);
        assertEquals(0, ScannerRules.usable(null).size());
    }

    @Test public void reorderingMovesAPageAndClampsRatherThanThrowing() {
        List<ScannerRules.Page> pages = Arrays.asList(page("a", 1, 1, 0), page("b", 1, 1, 0),
                page("c", 1, 1, 0));
        assertEquals(Arrays.asList("b", "a", "c"), ids(ScannerRules.moved(pages, 0, 1)));
        assertEquals(Arrays.asList("a", "c", "b"), ids(ScannerRules.moved(pages, 1, 2)));
        assertEquals(Arrays.asList("c", "a", "b"), ids(ScannerRules.moved(pages, 2, 0)));
        // Out-of-range steps clamp to the nearest valid position and still perform the move,
        // rather than throwing or doing nothing. That matters because the move buttons sit at
        // the ends of a list whose length changes as pages are removed, so a click can arrive
        // with an index that was valid a moment ago. Asserting identity here was wrong: a
        // clamped move to the end is a move to the end, not a no-op.
        assertEquals(Arrays.asList("b", "c", "a"), ids(ScannerRules.moved(pages, 0, 99)));
        assertEquals(Arrays.asList("c", "a", "b"), ids(ScannerRules.moved(pages, 5, 0)));
        // A move onto itself is the one genuine no-op.
        assertEquals(ids(pages), ids(ScannerRules.moved(pages, 1, 1)));
        assertTrue(ScannerRules.moved(null, 0, 1).isEmpty());
    }

    @Test public void rotatingAPageKeepsItsIdentityAndSource() {
        ScannerRules.Page original = page("a", 1000, 2000, 0);
        ScannerRules.Page turned = ScannerRules.rotated(original, 90);
        assertEquals("a", turned.id);
        assertEquals(original.sourceUri, turned.sourceUri);
        assertEquals(90, turned.rotation);
        assertEquals(0, original.rotation);
        assertEquals(180, ScannerRules.rotated(turned, 90).rotation);
        assertEquals(0, ScannerRules.rotated(ScannerRules.rotated(turned, 90), 180).rotation);
    }

    @Test public void filenamesContainNoCharacterStorageRejects() {
        String name = ScannerRules.fileName(1_700_000_000_000L, "Rent: Oct/Nov");
        assertTrue(name, name.endsWith(".pdf"));
        assertFalse(name, name.contains(":"));
        assertFalse(name, name.contains("/"));
        assertTrue(name, name.contains("Rent-Oct-Nov"));
        assertTrue(ScannerRules.fileName(1_700_000_000_000L, "").startsWith("scan-"));
        assertFalse(ScannerRules.fileName(1_700_000_000_000L, "!!!").endsWith("-.pdf"));
    }

    @Test public void theSizeEstimateScalesWithPageCount() {
        long one = ScannerRules.estimatedBytes(1, 1000, 1000);
        long three = ScannerRules.estimatedBytes(3, 1000, 1000);
        assertEquals(one * 3, three);
        assertEquals(0, ScannerRules.estimatedBytes(0, 1000, 1000));
        assertTrue(one > 0);
    }

    @Test public void thePageLabelIsSingularForOnePage() {
        assertEquals("1 page", ScannerRules.pageLabel(1));
        assertEquals("0 pages", ScannerRules.pageLabel(0));
        assertEquals("3 pages", ScannerRules.pageLabel(3));
    }

    @Test public void sizeFormattingCrossesEachBoundary() {
        assertEquals("512 B", ScannerRules.formatBytes(512));
        assertEquals("1 KB", ScannerRules.formatBytes(1024));
        assertEquals("1.0 MB", ScannerRules.formatBytes(1024 * 1024));
    }

    private static List<String> ids(List<ScannerRules.Page> pages) {
        List<String> out = new java.util.ArrayList<>();
        for (ScannerRules.Page p : pages) out.add(p.id);
        return out;
    }
}
