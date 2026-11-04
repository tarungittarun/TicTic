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
import static org.junit.Assert.assertArrayEquals;
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
    @Test public void permanentNoteCreationMarkerSurvivesTranscriptRetries() throws Exception {File root=new File(context().getCacheDir(),"voice-"+UUID.randomUUID());assertTrue(root.mkdir());try{VoiceFiles files=new VoiceFiles(isolated(root));String id=UUID.randomUUID().toString();try(RandomAccessFile out=new RandomAccessFile(files.pending(id),"rw")){PcmWave.header(out,0);out.write(new byte[100]);}files.complete(id);assertFalse(files.noteWasCreated(id));files.noteCreated(id);files.result(id,"words","DONE");files.result(id,"new words","DONE");assertTrue(files.noteWasCreated(id));assertEquals(144,files.audio(id).length());}finally{remove(root);}}
    @Test public void attenuatedAndSeededNoiseSpeechHaveBoundedMeasuredRegressionAndPreserveSource()throws Exception{
        File original=File.createTempFile("speech-corpus-",".wav",context().getCacheDir());try{
            try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("voice/test.wav");OutputStream out=new FileOutputStream(original)){byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);}
            byte[] source=java.nio.file.Files.readAllBytes(original.toPath());String digest=OfflineOcr.digest(new ByteArrayInputStream(source));String clean=OfflineSpeech.transcribe(context(),original,new AtomicBoolean());assertFalse(clean.trim().isEmpty());org.json.JSONArray rows=new org.json.JSONArray();
            for(int variant=0;variant<2;variant++){File altered=File.createTempFile("speech-degraded-",".wav",context().getCacheDir());try{byte[] bytes=source.clone();Random random=new Random(20261101);try(RandomAccessFile input=new RandomAccessFile(original,"r")){PcmWave.Data data=PcmWave.inspect(input);for(long offset=data.offset;offset+1<data.offset+data.bytes;offset+=2){int i=(int)offset;int value=(short)((bytes[i]&255)|(bytes[i+1]<<8));int adjusted=variant==0?value/3:value+(random.nextInt(401)-200);adjusted=Math.max(-32768,Math.min(32767,adjusted));bytes[i]=(byte)adjusted;bytes[i+1]=(byte)(adjusted>>8);}}java.nio.file.Files.write(altered.toPath(),bytes);long began=android.os.SystemClock.elapsedRealtime();String recognized=OfflineSpeech.transcribe(context(),altered,new AtomicBoolean());RecognitionMetrics.Score score=RecognitionMetrics.words(clean,recognized);assertFalse(recognized,recognized.trim().isEmpty());assertTrue("relative WER="+score.rate()+" text="+recognized,score.rate()<=.6);try(InputStream unchanged=new FileInputStream(altered)){assertEquals(OfflineOcr.digest(new ByteArrayInputStream(bytes)),OfflineOcr.digest(unchanged));}rows.put(new org.json.JSONObject().put("variant",variant==0?"one-third-amplitude":"seeded-plus-minus-200-pcm16").put("baseline",clean).put("recognized",recognized).put("relative_wer",score.rate()).put("elapsed_ms",android.os.SystemClock.elapsedRealtime()-began));}finally{altered.delete();}}
            assertEquals(digest,OfflineOcr.digest(new ByteArrayInputStream(java.nio.file.Files.readAllBytes(original.toPath()))));try(OutputStream out=new FileOutputStream(new File(context().getFilesDir(),"speech-degradation-report.json"))){out.write(new org.json.JSONObject().put("source_sha256",digest).put("recognizer","Pinned bundled Vosk, unchanged model").put("scope","Synthetic perturbations of one public fixture. Relative WER against model clean output, NOT ground-truth WER, accents or physical microphone acceptance.").put("cases",rows).toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        }finally{original.delete();}
    }
    @Test public void exactDigitalSilenceSkipsModelProvisioningAndPreservesAudio()throws Exception{
        File root=new File(context().getCacheDir(),"silent-model-"+UUID.randomUUID());assertTrue(root.mkdir());File audio=new File(root,"original.wav");
        try{try(RandomAccessFile out=new RandomAccessFile(audio,"rw")){out.setLength(PcmWave.HEADER+32000);PcmWave.header(out,32000);}
            byte[] before=java.nio.file.Files.readAllBytes(audio.toPath());long began=android.os.SystemClock.elapsedRealtime();
            OfflineSpeech.Result result=OfflineSpeech.transcribeResult(isolated(root),audio,new AtomicBoolean());
            assertEquals("",result.raw);assertEquals("",result.text);assertFalse(result.reviewRequired);
            assertFalse("Silent audio must not install/load the speech model",new File(root,"offline-speech-v1").exists());
            assertArrayEquals(before,java.nio.file.Files.readAllBytes(audio.toPath()));
            android.util.Log.i("NotesSpeechTiming","silent_native_fixture_ms="+(android.os.SystemClock.elapsedRealtime()-began));
        }finally{remove(root);}
    }
    private void remove(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)remove(child);file.delete();}
}
