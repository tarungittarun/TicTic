package app.space.notes;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.content.Context;
import app.space.notes.runtime.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TrainedWhisperTest {
    @Test public void actualTrainedCpuModelRunsOfflineAndPreservesPublicAudio()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File sample=File.createTempFile("trained-public-",".wav",context.getCacheDir());
        try{
            try(InputStream input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("voice/test.wav");OutputStream output=new FileOutputStream(sample)){byte[] b=new byte[8192];for(int n;(n=input.read(b))!=-1;)output.write(b,0,n);}
            byte[] before=java.nio.file.Files.readAllBytes(sample.toPath());long start=android.os.SystemClock.elapsedRealtime();
            OfflineSpeech.Result result=TrainedWhisper.transcribe(context,sample,new AtomicBoolean());
            assertFalse("Public fixture must produce reviewable words, not merely load the JNI library",result.raw.trim().isEmpty());assertTrue(result.reviewRequired);assertTrue(Double.isNaN(result.wordConfidence));
            assertArrayEquals(before,java.nio.file.Files.readAllBytes(sample.toPath()));
            try(InputStream input=context.getAssets().open("voice/trained-whisper.json")){String manifest=new String(LocalOpenFile.bounded(input,65536),java.nio.charset.StandardCharsets.UTF_8);assertTrue(manifest.contains("41393453222"));assertTrue(manifest.contains("84dcc55f0c1bcdc53e62d90a50c4dbe284669222f13ecbcbf458d62af42bc338"));}
            // Timing is observational, never a physical-device speed or accuracy claim.
            android.util.Log.i("NotesPublicBenchmark","trained_cpu_elapsed_ms="+(android.os.SystemClock.elapsedRealtime()-start));
        }finally{sample.delete();}
    }
    @Test public void privateWorkerDeathReturnsErrorWithoutKillingNotesOrLosingAudio()throws Exception{
        Context original=InstrumentationRegistry.getInstrumentation().getTargetContext();
        java.util.concurrent.CountDownLatch connected=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<android.content.ServiceConnection> proxy=new java.util.concurrent.atomic.AtomicReference<>();
        Context context=new android.content.ContextWrapper(original){
            @Override public Context getApplicationContext(){return this;}
            @Override public boolean bindService(android.content.Intent intent,android.content.ServiceConnection connection,int flags){
                android.content.ServiceConnection wrapped=new android.content.ServiceConnection(){
                    @Override public void onServiceConnected(android.content.ComponentName name,android.os.IBinder binder){connection.onServiceConnected(name,binder);connected.countDown();}
                    @Override public void onServiceDisconnected(android.content.ComponentName name){connection.onServiceDisconnected(name);}
                    @Override public void onBindingDied(android.content.ComponentName name){connection.onBindingDied(name);}
                    @Override public void onNullBinding(android.content.ComponentName name){connection.onNullBinding(name);}
                };proxy.set(wrapped);return super.bindService(intent,wrapped,flags);
            }
            @Override public void unbindService(android.content.ServiceConnection ignored){super.unbindService(proxy.get());}
        };
        File sample=File.createTempFile("trained-worker-death-",".wav",context.getCacheDir());java.util.concurrent.ExecutorService caller=java.util.concurrent.Executors.newSingleThreadExecutor();
        try{
            try(InputStream input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("voice/test.wav");OutputStream output=new FileOutputStream(sample)){byte[] b=new byte[8192];for(int n;(n=input.read(b))!=-1;)output.write(b,0,n);}
            byte[] before=java.nio.file.Files.readAllBytes(sample.toPath());int parent=android.os.Process.myPid();
            java.util.concurrent.Future<OfflineSpeech.Result> result=caller.submit(()->TrainedWhisper.transcribe(context,sample,new AtomicBoolean()));
            assertTrue("Private service must actually connect",connected.await(30,java.util.concurrent.TimeUnit.SECONDS));
            android.app.ActivityManager manager=(android.app.ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);int child=0;
            for(android.app.ActivityManager.RunningAppProcessInfo process:manager.getRunningAppProcesses())if(process.processName.equals(context.getPackageName()+":offline_whisper") && process.uid==android.os.Process.myUid())child=process.pid;
            assertTrue("Kill only our private worker, never the Notes process",child>0 && child!=parent);android.os.Process.killProcess(child);
            try{result.get(15,java.util.concurrent.TimeUnit.SECONDS);fail("Worker death must return an explicit error");}catch(java.util.concurrent.ExecutionException expected){assertTrue(expected.getCause().toString(),expected.getCause() instanceof IOException);assertTrue(expected.getCause().getMessage().contains("retained"));}
            assertEquals(parent,android.os.Process.myPid());assertArrayEquals(before,java.nio.file.Files.readAllBytes(sample.toPath()));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->assertEquals(android.os.Looper.getMainLooper(),android.os.Looper.myLooper()));
        }finally{caller.shutdownNow();sample.delete();}
    }
    @Test public void cancelledExperimentalRecognitionRetainsAudioWithoutInstallingModel()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File sample=File.createTempFile("trained-cancel-",".wav",context.getCacheDir());
        try{byte[] before={1,2,3,4};java.nio.file.Files.write(sample.toPath(),before);try{TrainedWhisper.transcribe(context,sample,new AtomicBoolean(true));fail("Cancellation must stop before model installation or WAV parsing");}catch(InterruptedException expected){assertArrayEquals(before,java.nio.file.Files.readAllBytes(sample.toPath()));}}finally{sample.delete();}
    }
}
