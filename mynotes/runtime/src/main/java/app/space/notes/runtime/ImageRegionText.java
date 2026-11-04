package app.space.notes.runtime;

import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.*;

/** Bounded background crop inference, source ownership and cancelled-result suppression. */
final class ImageRegionText {
    interface Listener { void complete(String text,String failure); }
    private static final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1));
    private static final Handler main=new Handler(Looper.getMainLooper());
    private ImageRegionText(){}
    static OfflineOcr.Cancellation read(Context context,OfflineOcr.Scan scan,RectF box,Listener listener){
        OfflineOcr.Cancellation cancel=new OfflineOcr.Cancellation();
        if(!scan.retain()){main.post(()->listener.complete("","Image is no longer available."));return cancel;}
        Context app=context.getApplicationContext();RectF area=new RectF(box);
        try{worker.execute(()->{
            String text="",failure="";
            try{text=OfflineOcr.recognizeRegion(app,scan.image,area,scan.result.language,cancel).text;}
            catch(InterruptedException stopped){Thread.currentThread().interrupt();}
            catch(Exception | LinkageError | OutOfMemoryError error){failure="Could not read this area. Try a smaller selection. Your image is unchanged.";}
            finally{scan.release();}
            String result=text,error=failure;
            main.post(()->{if(!cancel.cancelled())listener.complete(result,error);});
        });}catch(RejectedExecutionException busy){scan.release();main.post(()->{if(!cancel.cancelled())listener.complete("","Another area is being read. Try again shortly.");});}
        return cancel;
    }
}
