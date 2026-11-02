package app.space.notes.runtime;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.*;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real scanned PDF pixels and native Tesseract; never private input or fabricated OCR. */
@RunWith(AndroidJUnit4.class)
public class PdfPageOcrTest {
    private final Instrumentation instrument=InstrumentationRegistry.getInstrumentation();
    private Context context(){return instrument.getTargetContext();}
    private File scanned(boolean blank)throws Exception{
        File file=File.createTempFile("public-scanned-page-",".pdf",context().getCacheDir());
        Bitmap pixels=Bitmap.createBitmap(1200,400,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(pixels);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(72);
        if(!blank){canvas.drawText("MEETING NOTES",70,140,paint);canvas.drawText("Monday at 10 AM",70,250,paint);}
        PdfDocument pdf=new PdfDocument();try{PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(600,200,1).create());page.getCanvas().drawBitmap(pixels,null,new Rect(0,0,600,200),null);pdf.finishPage(page);try(OutputStream out=new FileOutputStream(file)){pdf.writeTo(out);}}finally{pdf.close();pixels.recycle();}return file;
    }
    @Test public void scannedPdfRunsRealOcrAndNeverWritesOriginal()throws Exception{
        PDFBoxResourceLoader.init(context());File source=scanned(false);byte[] before=java.nio.file.Files.readAllBytes(source.toPath());File cache=File.createTempFile("ocr-private-copy-",".pdf",context().getCacheDir());java.nio.file.Files.copy(source.toPath(),cache.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);LocalPdfPreview.Document document=new LocalPdfPreview.Document(cache);document.claimText();
        try{assertTrue("Fixture must not contain an embedded text layer",document.text(0).trim().isEmpty());OfflineOcr.Result result=PdfPageOcr.recognize(context(),document,0,"auto",new OfflineOcr.Cancellation());assertTrue(result.text,result.text.toUpperCase(Locale.ROOT).contains("MEETING"));assertTrue(result.text,result.text.toUpperCase(Locale.ROOT).contains("MONDAY"));assertTrue(result.width<=2400 && result.height<=2400);assertTrue((long)result.width*result.height<=4000000);assertArrayEquals(before,java.nio.file.Files.readAllBytes(source.toPath()));assertTrue(document.text(0).trim().isEmpty());}finally{document.closeText();document.close();}assertFalse(cache.exists());assertArrayEquals(before,java.nio.file.Files.readAllBytes(source.toPath()));assertTrue(source.delete());
    }
    @Test public void blankScannedPageDoesNotInventText()throws Exception{
        File source=scanned(true);LocalPdfPreview.Document document=new LocalPdfPreview.Document(source);try{OfflineOcr.Result result=PdfPageOcr.recognize(context(),document,0,"auto",new OfflineOcr.Cancellation());assertTrue(result.text,result.text.trim().isEmpty());}finally{document.close();}assertFalse(source.exists());
    }
    @Test public void cancellationAndUiThreadGuardLeaveRendererUsable()throws Exception{
        File source=scanned(false);LocalPdfPreview.Document document=new LocalPdfPreview.Document(source);OfflineOcr.Cancellation cancelled=new OfflineOcr.Cancellation();cancelled.cancel();
        try{try{PdfPageOcr.recognize(context(),document,0,"auto",cancelled);fail("Cancelled work must not enter renderer/inference");}catch(InterruptedException expected){}
            instrument.runOnMainSync(()->{try{PdfPageOcr.recognize(context(),document,0,"auto",new OfflineOcr.Cancellation());fail("UI thread is not allowed");}catch(IllegalStateException expected){}catch(Exception wrong){throw new AssertionError(wrong);}});
            try(PdfRenderer.Page page=document.renderer.openPage(0)){assertEquals(600,page.getWidth());}
        }finally{document.close();}
    }
    private Object field(Object value,String name)throws Exception{Field field=value.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(value);}
    private void request(LocalPdfPreview preview){try{Method method=LocalPdfPreview.class.getDeclaredMethod("recognizePage",int.class);method.setAccessible(true);method.invoke(preview,0);}catch(Exception failure){throw new AssertionError(failure);}}
    private Activity host(){Activity activity=instrument.startActivitySync(new Intent(context(),app.space.notes.NotesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));instrument.runOnMainSync(()->{try{Field field=app.space.notes.NotesActivity.class.getDeclaredField("controller");field.setAccessible(true);NotesController startup=(NotesController)field.get(activity);if(startup!=null)startup.close();field.set(activity,null);}catch(Exception failure){throw new AssertionError(failure);}});return activity;}
    private void shutdown(LocalPdfPreview preview)throws Exception{ExecutorService renderer=(ExecutorService)field(preview,"worker"),text=(ExecutorService)field(preview,"textWorker");instrument.runOnMainSync(preview::close);assertTrue(renderer.awaitTermination(30,TimeUnit.SECONDS));assertTrue(text.awaitTermination(30,TimeUnit.SECONDS));instrument.waitForIdleSync();}
    @Test public void pdfViewportRoundTripsPageAndQueryWithoutBitmapsOrRestartingOcr()throws Exception{
        Activity activity=host();File original=File.createTempFile("public-reader-state-",".pdf",context().getCacheDir());PdfDocument generated=new PdfDocument();try{for(int i=0;i<3;i++){PdfDocument.Page page=generated.startPage(new PdfDocument.PageInfo.Builder(600,800,i+1).create());page.getCanvas().drawColor(Color.WHITE);generated.finishPage(page);}try(OutputStream out=new FileOutputStream(original)){generated.writeTo(out);}}finally{generated.close();}
        LocalPdfPreview[] preview={null};android.os.Bundle state=new android.os.Bundle();state.putInt("page",1);state.putFloat("pageTop",-.25f);state.putString("query","Needle");state.putFloatArray("zoom",new float[]{2,.4f,.6f,.2f,-.3f});
        try{for(int attempt=0;attempt<2;attempt++){File cache=File.createTempFile("state-reader-cache-",".pdf",context().getCacheDir());java.nio.file.Files.copy(original.toPath(),cache.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);LocalPdfPreview.Document document=new LocalPdfPreview.Document(cache);android.os.Bundle restore=new android.os.Bundle(state);instrument.runOnMainSync(()->{preview[0]=new LocalPdfPreview(activity,new NotesDesign(activity,false),document);activity.setContentView(preview[0]);preview[0].restoreViewport(restore);});instrument.waitForIdleSync();CountDownLatch laidOut=new CountDownLatch(1);instrument.runOnMainSync(()->preview[0].postOnAnimation(()->preview[0].postOnAnimation(laidOut::countDown)));assertTrue(laidOut.await(10,TimeUnit.SECONDS));instrument.waitForIdleSync();instrument.runOnMainSync(()->{android.os.Bundle saved=preview[0].savedViewport();assertEquals(1,saved.getInt("page"));assertEquals(-.25f,saved.getFloat("pageTop"),.005f);assertEquals("Needle",saved.getString("query"));assertArrayEquals(new float[]{2,.4f,.6f,.2f,-.3f},saved.getFloatArray("zoom"),.002f);state.putAll(saved);try{assertNull("Restoring a view must never restart private OCR automatically",field(preview[0],"ocrCancellation"));}catch(Exception failure){throw new AssertionError(failure);}});shutdown(preview[0]);preview[0]=null;assertFalse(cache.exists());}assertTrue(original.exists());
        }finally{if(preview[0]!=null)shutdown(preview[0]);original.delete();instrument.runOnMainSync(activity::finish);}
    }
    private TextView recognized(View root){if(root instanceof TextView && root.getContentDescription()!=null && "Recognized page text — review before copying".contentEquals(root.getContentDescription()))return (TextView)root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){TextView found=recognized(((ViewGroup)root).getChildAt(i));if(found!=null)return found;}return null;}
    @Test public void explicitPageActionOpensSelectableRealOcrReview()throws Exception{
        Activity activity=host();File source=scanned(false);byte[] before=java.nio.file.Files.readAllBytes(source.toPath());LocalPdfPreview.Document document=new LocalPdfPreview.Document(source);LocalPdfPreview[] preview={null};CountDownLatch reviewed=new CountDownLatch(1);java.util.concurrent.atomic.AtomicBoolean stopped=new java.util.concurrent.atomic.AtomicBoolean();
        try{instrument.runOnMainSync(()->{preview[0]=new LocalPdfPreview(activity,new NotesDesign(activity,false),document);activity.setContentView(preview[0]);request(preview[0]);activity.getWindow().getDecorView().post(new Runnable(){public void run(){if(stopped.get())return;try{if(field(preview[0],"pageTextDialog")!=null)reviewed.countDown();else activity.getWindow().getDecorView().postDelayed(this,25);}catch(Exception failure){throw new AssertionError(failure);}}});});
            assertTrue("Real page OCR review must complete",reviewed.await(60,TimeUnit.SECONDS));instrument.runOnMainSync(()->{try{AlertDialog review=(AlertDialog)field(preview[0],"pageTextDialog");assertTrue(review.isShowing());TextView text=recognized(review.getWindow().getDecorView());assertNotNull(text);assertTrue(text.isTextSelectable());assertTrue(text.getText().toString(),text.getText().toString().toUpperCase(Locale.ROOT).contains("MEETING"));assertTrue(review.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());}catch(Exception failure){throw new AssertionError(failure);}});assertArrayEquals(before,java.nio.file.Files.readAllBytes(source.toPath()));
        }finally{stopped.set(true);if(preview[0]!=null)shutdown(preview[0]);else document.close();instrument.runOnMainSync(activity::finish);}
    }
    @Test public void cancellingQueuedRecognitionAndClosingDoesNotLeakOrCreateReview()throws Exception{
        Activity activity=host();File source=scanned(false);LocalPdfPreview.Document document=new LocalPdfPreview.Document(source);LocalPdfPreview[] preview={null};CountDownLatch release=new CountDownLatch(1);
        try{instrument.runOnMainSync(()->{preview[0]=new LocalPdfPreview(activity,new NotesDesign(activity,false),document);activity.setContentView(preview[0]);});ExecutorService renderer=(ExecutorService)field(preview[0],"worker");renderer.execute(()->{try{release.await(20,TimeUnit.SECONDS);}catch(InterruptedException cancelled){Thread.currentThread().interrupt();}});
            instrument.runOnMainSync(()->{request(preview[0]);try{AlertDialog progress=(AlertDialog)field(preview[0],"pageOcrDialog");assertTrue("Recognition progress must be visible before cancelling",progress.isShowing());assertTrue("Cancel must be a real clickable action",progress.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());}catch(Exception failure){throw new AssertionError(failure);}});
            // AlertDialog posts its button listener and dismissal through ButtonHandler.
            // Do not assert inside performClick's stack or use close() to cancel for it.
            instrument.waitForIdleSync();
            instrument.runOnMainSync(()->{try{assertTrue("The actual Cancel callback must cancel the still-queued work before close",((OfflineOcr.Cancellation)field(preview[0],"ocrCancellation")).cancelled());assertFalse("Cancel must dismiss progress",((AlertDialog)field(preview[0],"pageOcrDialog")).isShowing());preview[0].close();}catch(Exception failure){throw new AssertionError(failure);}});
            release.countDown();shutdown(preview[0]);assertNull(field(preview[0],"pageTextDialog"));assertFalse(source.exists());
        }finally{release.countDown();if(preview[0]!=null)shutdown(preview[0]);else document.close();instrument.runOnMainSync(activity::finish);}
    }
}
