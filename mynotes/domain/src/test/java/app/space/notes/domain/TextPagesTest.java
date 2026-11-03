package app.space.notes.domain;
import org.junit.Test;
import static org.junit.Assert.*;
public class TextPagesTest {
    @Test public void emptyTextStillHasOnePage(){TextPages pages=new TextPages("");assertEquals(1,pages.count());assertEquals("",pages.page(0));}
    @Test public void oversizedEditorTextIsLosslesslyPagedIncludingIndicAndEmoji(){StringBuilder text=new StringBuilder();for(int i=0;i<20000;i++)text.append("Line ").append(i).append(" ಕನ್ನಡ भारत 😀\n");TextPages pages=new TextPages(text.toString());assertTrue(pages.count()>1);StringBuilder restored=new StringBuilder();for(int i=0;i<pages.count();i++){String page=pages.page(i);assertTrue(page.length()<=TextPages.PAGE_CHARS);RichText.requireText(page);restored.append(page);}assertEquals(text.toString(),restored.toString());}
    @Test public void surrogateAtExactPageBoundaryStaysTogether(){StringBuilder text=new StringBuilder();for(int i=0;i<TextPages.PAGE_CHARS-1;i++)text.append('a');text.append("😀tail");TextPages pages=new TextPages(text.toString());assertEquals(TextPages.PAGE_CHARS-1,pages.page(0).length());assertEquals("😀tail",pages.page(1));}
    @Test(expected=IllegalArgumentException.class) public void malformedUnicodeIsNotDisplayedAsReplacementGlyphs(){new TextPages("bad\ud800");}
    @Test(expected=IllegalArgumentException.class) public void maximumIsEnforcedBeforeRendering(){new TextPages(new String(new char[4*1024*1024+1]));}
    @Test(expected=IndexOutOfBoundsException.class) public void invalidPageIsRefused(){new TextPages("keep").page(1);}
}
