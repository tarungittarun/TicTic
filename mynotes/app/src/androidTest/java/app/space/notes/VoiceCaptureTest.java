package app.space.notes;
import android.content.*;
import android.content.pm.PackageManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import app.space.notes.domain.PcmWave;
import app.space.notes.runtime.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class VoiceCaptureTest {
    @Test public void contextualMicrophonePolicyAndDurableCaptureMatchLane() throws Exception {
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();boolean granted=InstrumentationRegistry.getArguments().getString("expectedMic","denied").equals("granted");
        assertEquals(granted?PackageManager.PERMISSION_GRANTED:PackageManager.PERMISSION_DENIED,target.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO));
        File root=new File(target.getCacheDir(),"capture-"+UUID.randomUUID());assertTrue(root.mkdir());Context context=new ContextWrapper(target){@Override public Context getApplicationContext(){return this;}@Override public File getFilesDir(){return root;}};
        CountDownLatch started=new CountDownLatch(1),finished=new CountDownLatch(1);AtomicReference<String> saved=new AtomicReference<>(),error=new AtomicReference<>();
        VoiceCapture capture=new VoiceCapture(context,new VoiceCapture.Listener(){public void started(String id){started.countDown();}public void saved(String id){saved.set(id);finished.countDown();}public void failed(String message){error.set(message);finished.countDown();}});
        ExecutorService worker=Executors.newSingleThreadExecutor();try{worker.execute(capture);
            if(granted){assertTrue("Capture did not start: "+error.get(),started.await(8,TimeUnit.SECONDS));File pending=new VoiceFiles(context).pending(capture.id);long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(pending.length()<6444 && System.nanoTime()<until)Thread.sleep(20);capture.stop();assertTrue(finished.await(10,TimeUnit.SECONDS));assertEquals(null,error.get());assertNotNull(saved.get());try(RandomAccessFile wave=new RandomAccessFile(new VoiceFiles(context).audio(saved.get()),"r")){assertTrue(PcmWave.inspect(wave).bytes>0);}}
            else{assertTrue(finished.await(5,TimeUnit.SECONDS));assertNotNull(error.get());assertTrue(error.get().contains("permission"));assertTrue(new VoiceFiles(context).list().isEmpty());}
        }finally{capture.stop();worker.shutdown();worker.awaitTermination(12,TimeUnit.SECONDS);remove(root);}
    }
    @Test public void stoppedBeforeWorkerStartsStillCompletesControllerCallback() throws Exception {
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();File root=new File(target.getCacheDir(),"cancel-capture-"+UUID.randomUUID());assertTrue(root.mkdir());Context context=new ContextWrapper(target){@Override public Context getApplicationContext(){return this;}@Override public File getFilesDir(){return root;}};java.util.concurrent.atomic.AtomicInteger completed=new java.util.concurrent.atomic.AtomicInteger();
        try{VoiceCapture capture=new VoiceCapture(context,new VoiceCapture.Listener(){public void started(String id){throw new AssertionError("Cancelled recording must not start microphone");}public void saved(String id){throw new AssertionError("No recording exists");}public void failed(String message){assertTrue(message,message.contains("cancelled"));completed.incrementAndGet();}});capture.stop();capture.run();assertEquals(1,completed.get());assertTrue(new VoiceFiles(context).list().isEmpty());}finally{remove(root);}
    }
    private void remove(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)remove(child);file.delete();}
}
