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
