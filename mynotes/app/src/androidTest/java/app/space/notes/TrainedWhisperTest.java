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
    @Test public void cancelledExperimentalRecognitionRetainsAudioWithoutInstallingModel()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File sample=File.createTempFile("trained-cancel-",".wav",context.getCacheDir());
        try{byte[] before={1,2,3,4};java.nio.file.Files.write(sample.toPath(),before);try{TrainedWhisper.transcribe(context,sample,new AtomicBoolean(true));fail("Cancellation must stop before model installation or WAV parsing");}catch(InterruptedException expected){assertArrayEquals(before,java.nio.file.Files.readAllBytes(sample.toPath()));}}finally{sample.delete();}
    }
}
