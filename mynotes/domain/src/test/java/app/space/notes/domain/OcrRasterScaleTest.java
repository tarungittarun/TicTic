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
    @Test public void readableInkDoesNotNeedMoreInterpolationButTinyInkStillDoes() {
        boolean[] readable=new boolean[300];
        java.util.Arrays.fill(readable,20,45,true);java.util.Arrays.fill(readable,120,146,true);
        readable[60]=true;assertTrue(OcrRasterScale.readableInkRows(readable));
        boolean[] tiny=new boolean[300];
        java.util.Arrays.fill(tiny,20,32,true);java.util.Arrays.fill(tiny,120,132,true);
        assertFalse(OcrRasterScale.readableInkRows(tiny));
        assertFalse(OcrRasterScale.readableInkRows(new boolean[300]));
        java.util.Arrays.fill(tiny,true);assertFalse(OcrRasterScale.readableInkRows(tiny));
        boolean[] separatedAccents=new boolean[300];
        java.util.Arrays.fill(separatedAccents,20,29,true);java.util.Arrays.fill(separatedAccents,31,44,true);
        assertTrue(OcrRasterScale.readableInkRows(separatedAccents));
    }
    @Test(expected=IllegalArgumentException.class) public void invalidDimensionsRejected() {OcrRasterScale.factor(0,100);}
    @Test(expected=IllegalArgumentException.class) public void oversizedRejected() {OcrRasterScale.factor(Integer.MAX_VALUE,Integer.MAX_VALUE);}
}
