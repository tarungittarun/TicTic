package app.space.notes;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import app.space.notes.domain.*;
import app.space.notes.runtime.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PdfExportTest {
    private File export(Note note) throws Exception {File f=File.createTempFile("pdf-test-",".pdf",InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir());try(OutputStream out=new FileOutputStream(f)){PdfExporter.write(Arrays.asList(note),out);}return f;}
    private int pages(File file) throws Exception {try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer renderer=new PdfRenderer(fd)){return renderer.getPageCount();}}
    @Test public void emptyNoteProducesReadablePdf() throws Exception {File f=export(Note.create(1));try{assertEquals(1,pages(f));assertTrue(f.length()>100);}finally{f.delete();}}
    @Test public void longUnicodeTextPaginatesWithoutDroppingFinalPage() throws Exception {StringBuilder text=new StringBuilder();for(int i=0;i<300;i++)text.append(i+" ಕನ್ನಡ Hindi नोट\n");File f=export(Note.create(1).edit("Long",text.toString(),2));try{assertTrue(pages(f)>4);}finally{f.delete();}}
    @Test public void flowchartAddsVectorPageAndRendersVisibleContent() throws Exception {Diagram d=Diagram.empty().put(new Diagram.Node("a","decision","Yes?",100,100,240,160,6)).put(new Diagram.Node("b","terminator","End",100,500,240,100,2)).connect("a","b","Yes");File f=export(Note.create(1).diagram(d,2));try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer renderer=new PdfRenderer(fd)){assertEquals(2,renderer.getPageCount());try(PdfRenderer.Page page=renderer.openPage(1)){Bitmap bitmap=Bitmap.createBitmap(595,842,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);int colored=0;for(int y=80;y<600;y+=4)for(int x=50;x<500;x+=4){int color=bitmap.getPixel(x,y);if(Color.red(color)!=Color.blue(color))colored++;}assertTrue("Colored shapes must render on exported PDF page",colored>50);bitmap.recycle();}}finally{f.delete();}}
    @Test public void checklistExportsAndOriginalRemainsStructured() throws Exception {Note n=Note.create(1).convert(2);File f=export(n);try{assertEquals(1,pages(f));assertTrue(n.checklist);}finally{f.delete();}}
    @Test public void richSpansOffsetAfterTitleWithoutCrashing() throws Exception {File f=export(Note.create(1).rich("Title","Hello world",Arrays.asList(new Note.Mark("bold",0,5),new Note.Mark("h1",6,11)),2));try{assertEquals(1,pages(f));}finally{f.delete();}}
}
