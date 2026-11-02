package app.space.notes.runtime;

import android.content.*;
import android.os.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class WhisperConnectionJourneysTest {
    @Test public void cancellationBeforeLateConnectionNeverStartsNativeWorkAndUnbindsOnce()throws Exception{
        Context real=InstrumentationRegistry.getInstrumentation().getTargetContext();AtomicReference<ServiceConnection> captured=new AtomicReference<>();CountDownLatch bound=new CountDownLatch(1);AtomicInteger unbinds=new AtomicInteger(),runs=new AtomicInteger();AtomicBoolean cancelled=new AtomicBoolean();
        Context fake=new ContextWrapper(real){@Override public Context getApplicationContext(){return this;}@Override public boolean bindService(Intent intent,ServiceConnection connection,int flags){captured.set(connection);bound.countDown();return true;}@Override public void unbindService(ServiceConnection connection){assertSame(captured.get(),connection);unbinds.incrementAndGet();}};
        File audio=File.createTempFile("late-connection-audio-",".wav",real.getCacheDir());byte[] before={1,2,3};java.nio.file.Files.write(audio.toPath(),before);ExecutorService worker=Executors.newSingleThreadExecutor();
        try{
            Future<OfflineSpeech.Result> future=worker.submit(()->WhisperProcessClient.transcribe(fake,audio,cancelled));assertTrue("Fake service connection reached",bound.await(15,TimeUnit.SECONDS));cancelled.set(true);
            try{future.get(15,TimeUnit.SECONDS);fail("Cancellation must not return a transcript");}catch(ExecutionException expected){assertTrue(expected.getCause().toString(),expected.getCause() instanceof InterruptedException);}
            assertEquals(1,unbinds.get());Messenger endpoint=new Messenger(new Handler(Looper.getMainLooper()){@Override public void handleMessage(Message message){if(message.what==WhisperWorkerService.RUN)runs.incrementAndGet();}});
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->captured.get().onServiceConnected(new ComponentName(real,WhisperWorkerService.class),endpoint.getBinder()));InstrumentationRegistry.getInstrumentation().waitForIdleSync();assertEquals("A connection arriving after cancellation must not send RUN",0,runs.get());assertArrayEquals(before,java.nio.file.Files.readAllBytes(audio.toPath()));
        }finally{cancelled.set(true);worker.shutdownNow();audio.delete();}
    }
}
