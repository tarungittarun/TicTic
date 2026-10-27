package app.space.notes;
import android.content.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.channels.FileLock;
import app.space.notes.domain.*;
import app.space.notes.runtime.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class OfflineSpeechTest {
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private Context isolated(File root){return new ContextWrapper(context()){@Override public Context getApplicationContext(){return this;}@Override public File getFilesDir(){return root;}};}
    @Test public void realBundledModelRecognizesPublicSpeechFixtureOffline() throws Exception {File sample=File.createTempFile("speech-",".wav",context().getCacheDir());try{try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("voice/test.wav");OutputStream out=new FileOutputStream(sample)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}String text=OfflineSpeech.transcribe(context(),sample,new AtomicBoolean());assertTrue(text,text.contains("one"));assertTrue(text,text.contains("zero"));assertTrue(sample.isFile());}finally{sample.delete();}}
    @Test public void cancelledInferenceKeepsOriginalAudioBytes() throws Exception {File sample=File.createTempFile("speech-",".wav",context().getCacheDir());try(RandomAccessFile file=new RandomAccessFile(sample,"rw")){PcmWave.header(file,0);try{OfflineSpeech.transcribe(context(),sample,new AtomicBoolean(true));fail("cancelled");}catch(InterruptedException expected){assertEquals(44,sample.length());}}finally{sample.delete();}}
    @Test public void interruptedRecordingIsRecoveredBeforeRecognition() throws Exception {File root=new File(context().getCacheDir(),"voice-"+UUID.randomUUID());assertTrue(root.mkdir());try{VoiceFiles files=new VoiceFiles(isolated(root));String id=UUID.randomUUID().toString();try(RandomAccessFile out=new RandomAccessFile(files.pending(id),"rw")){PcmWave.header(out,0);out.write(new byte[3201]);}assertEquals(Arrays.asList(id),files.list());assertFalse(files.pending(id).exists());try(RandomAccessFile in=new RandomAccessFile(files.audio(id),"r")){assertEquals(3200,PcmWave.inspect(in).bytes);}}finally{remove(root);}}
    @Test public void activeRecordingIsNotRenamedByInboxScanner() throws Exception {File root=new File(context().getCacheDir(),"voice-"+UUID.randomUUID());assertTrue(root.mkdir());try{VoiceFiles files=new VoiceFiles(isolated(root));String id=UUID.randomUUID().toString();try(RandomAccessFile out=new RandomAccessFile(files.pending(id),"rw");FileLock lock=out.getChannel().lock()){PcmWave.header(out,0);out.write(new byte[3200]);assertTrue(files.list().isEmpty());assertTrue(files.pending(id).isFile());}}finally{remove(root);}}
    @Test public void failedTranscriptMetadataNeverDeletesRecording() throws Exception {File root=new File(context().getCacheDir(),"voice-"+UUID.randomUUID());assertTrue(root.mkdir());try{VoiceFiles files=new VoiceFiles(isolated(root));String id=UUID.randomUUID().toString();try(RandomAccessFile out=new RandomAccessFile(files.pending(id),"rw")){PcmWave.header(out,0);out.write(new byte[3200]);}files.complete(id);files.result(id,"","Failed; audio retained");assertEquals(3244,files.audio(id).length());assertTrue(files.result(id).getString("state").contains("Failed"));}finally{remove(root);}}
    @Test public void voiceSourceProvenanceRoundTripsAndDeduplicates() throws Exception {String id=UUID.randomUUID().toString();Note n=VoiceTranscripts.apply(VoiceTranscripts.placeholder(id,1),id,"recognized words",2);Note read=BackupCodec.decode(BackupCodec.encode(Arrays.asList(n))).get(0);assertEquals(n.voiceSources,read.voiceSources);assertTrue(read==VoiceTranscripts.apply(read,id,"recognized words",3));}
    private void remove(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)remove(child);file.delete();}
}
