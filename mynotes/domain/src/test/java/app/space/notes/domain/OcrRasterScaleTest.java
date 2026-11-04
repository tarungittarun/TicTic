package app.space.notes.domain;
import org.junit.Test;
import static org.junit.Assert.*;

public class OcrRasterScaleTest {
    @Test public void actualTallScreenshotRetainsOriginalPixelsBeforeRecognition() {
        assertEquals(1, OcrRasterScale.decodeSample(1200,2670));
        assertEquals(1, OcrRasterScale.decodeSample(2670,1200));
        assertEquals(2, OcrRasterScale.decodeSample(2400,5340));
        assertEquals(2, OcrRasterScale.decodeSample(2001,2001));
    }
    @Test public void tallScreenshotGetsMoreRecognitionPixelsNotJustViewerZoom() {
        double scale = OcrRasterScale.factor(705, 1568);
        assertTrue(scale > 1.5);
        assertTrue(705L * 1568 * scale * scale <= 4000000.01);
    }
    @Test public void boundedForPortraitLandscapeAndTinyInputs() {
        for (int[] size : new int[][]{{705,1568},{1568,705},{200,100},{1080,2400},{2000,2000},{1,4000000}}) {
            double scale = OcrRasterScale.factor(size[0],size[1]);
            assertTrue(scale >= 1 && scale <= 2);
            assertTrue((long)size[0]*size[1]*scale*scale <= 4000000.01);
        }
    }
    @Test public void largeDocumentIsNotEnlarged() {assertEquals(1, OcrRasterScale.factor(2000,2000),0);assertEquals("Preserve qualified native-script document raster",1,OcrRasterScale.factor(2400,1000),0);}
    @Test(expected=IllegalArgumentException.class) public void invalidDimensionsRejected() {OcrRasterScale.factor(0,100);}
    @Test(expected=IllegalArgumentException.class) public void oversizedRejected() {OcrRasterScale.factor(Integer.MAX_VALUE,Integer.MAX_VALUE);}
}
