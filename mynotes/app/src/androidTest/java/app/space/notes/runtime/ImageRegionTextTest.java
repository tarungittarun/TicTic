package app.space.notes.runtime;

import android.graphics.Bitmap;
import android.graphics.RectF;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real worker/bitmap ownership and main-thread callback cancellation, not accuracy claims. */
@RunWith(AndroidJUnit4.class)
public class ImageRegionTextTest {
    @Test public void cancellationSuppressesLateResultAndEventuallyReleasesSource()throws Exception{
        Bitmap bitmap=Bitmap.createBitmap(400,300,Bitmap.Config.ARGB_8888);
        OfflineOcr.Scan scan=new OfflineOcr.Scan(new OfflineOcr.Result("","eng",0,400,300),bitmap);
        AtomicInteger delivered=new AtomicInteger();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            OfflineOcr.Cancellation task=ImageRegionText.read(InstrumentationRegistry.getInstrumentation().getTargetContext(),scan,new RectF(0,0,1,1),(text,error)->delivered.incrementAndGet());
            task.cancel();scan.close();
        });
        long deadline=android.os.SystemClock.elapsedRealtime()+10000;
        while(!bitmap.isRecycled() && android.os.SystemClock.elapsedRealtime()<deadline)Thread.sleep(20);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertTrue("Cancelled worker must release its retained source",bitmap.isRecycled());
        assertEquals("Cancelled area must never replace review text",0,delivered.get());
    }
    @Test public void asyncAreaReadUsesFreshSourceCropInsteadOfStaleFullImageText()throws Exception{
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();Bitmap bitmap=Bitmap.createBitmap(900,500,Bitmap.Config.ARGB_8888);android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);canvas.drawColor(android.graphics.Color.rgb(65,58,31));android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);paint.setColor(android.graphics.Color.WHITE);paint.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));paint.setTextSize(34);canvas.drawText("OUTSIDE HEADER",24,76,paint);canvas.drawText("OUTSIDE FOOTER",24,486,paint);paint.setColor(android.graphics.Color.rgb(44,48,55));canvas.drawRoundRect(175,115,725,410,22,22,paint);paint.setColor(android.graphics.Color.WHITE);paint.setTextSize(40);canvas.drawText("APP KEEPS STOPPING",210,205,paint);paint.setTextSize(30);paint.setTypeface(android.graphics.Typeface.DEFAULT);canvas.drawText("App info",330,275,paint);canvas.drawText("Close app",330,345,paint);
        OfflineOcr.Scan scan=new OfflineOcr.Scan(new OfflineOcr.Result("STALE FULL IMAGE TEXT", "eng", 90,900,500),bitmap);java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.atomic.AtomicReference<String> text=new java.util.concurrent.atomic.AtomicReference<>(""),failure=new java.util.concurrent.atomic.AtomicReference<>("");
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{ImageRegionText.read(context,scan,new RectF(.19f,.21f,.82f,.84f),(recognized,error)->{text.set(recognized);failure.set(error);done.countDown();});scan.close();});
        assertTrue("Fresh crop inference callback",done.await(60,java.util.concurrent.TimeUnit.SECONDS));InstrumentationRegistry.getInstrumentation().waitForIdleSync();assertEquals("",failure.get());String recognized=text.get().toUpperCase(java.util.Locale.ROOT);assertTrue(recognized,recognized.contains("APP"));assertTrue(recognized,recognized.contains("STOPPING"));assertFalse("Crop result must not reuse stale full-image text or outside words",recognized.contains("STALE") || recognized.contains("OUTSIDE"));assertTrue("The retained source is released after the async crop pass",bitmap.isRecycled());
    }
    @Test public void absentAreaDoesNotLeakSourceOrDeliverAfterCancellation(){
        Bitmap bitmap=Bitmap.createBitmap(10,10,Bitmap.Config.ARGB_8888);
        OfflineOcr.Scan scan=new OfflineOcr.Scan(new OfflineOcr.Result("","eng",0,10,10),bitmap);
        AtomicInteger delivered=new AtomicInteger();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            OfflineOcr.Cancellation task=ImageRegionText.read(InstrumentationRegistry.getInstrumentation().getTargetContext(),scan,null,(text,error)->delivered.incrementAndGet());
            task.cancel();assertFalse(bitmap.isRecycled());scan.close();assertTrue(bitmap.isRecycled());
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();assertEquals(0,delivered.get());
    }
}
