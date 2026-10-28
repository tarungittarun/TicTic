package app.space.notes.runtime;
import android.content.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import app.space.notes.domain.*;
import static org.junit.Assert.*;

/** Native JSON/AtomicFile semantics, independent of a model or microphone permission. */
@RunWith(AndroidJUnit4.class)
public class ArchiveSemanticTest {
 private static final String ID="00000000-0000-0000-0000-000000000001";
 private byte[] utf(String text){return text.getBytes(StandardCharsets.UTF_8);}
 private byte[] metadata()throws Exception{return utf(new JSONObject().put("version",1).put("id",ID).put("text","ಕನ್ನಡ Hindi नोट").put("state","Ready").put("rawText","Original words").put("formatting","Conservative punctuation").toString());}
 @Test public void nativeLibraryAndTranscriptFieldsRoundTripWithoutContentChanges()throws Exception{Note note=Note.create(1).edit("नोट","ಕನ್ನಡ body { [ \\\" quoted",2);assertArrayEquals(DocumentCodec.encode(Arrays.asList(note)),DocumentCodec.encode(ArchiveJson.library(BackupCodec.encode(Arrays.asList(note)))));JSONObject value=ArchiveJson.metadata(metadata(),ID);assertEquals("ಕನ್ನಡ Hindi नोट",value.getString("text"));assertEquals("Original words",value.getString("rawText"));}
 @Test public void invalidUtf8AndDeepMetadataFailBeforeRecursiveJsonParsing()throws Exception{assertThrows(IOException.class,()->ArchiveJson.metadata(new byte[]{'{',(byte)0xc3,0x28,'}'},ID));StringBuilder deep=new StringBuilder();for(int i=0;i<1000;i++)deep.append('[');for(int i=0;i<1000;i++)deep.append(']');assertThrows(IOException.class,()->ArchiveJson.metadata(utf(deep.toString()),ID));}
 @Test public void nestedEncodedLibraryPayloadIsAlsoDepthBounded()throws Exception{StringBuilder deep=new StringBuilder();for(int i=0;i<1000;i++)deep.append('[');for(int i=0;i<1000;i++)deep.append(']');byte[] bytes=utf(new JSONObject().put("version",1).put("payload",deep.toString()).toString());assertThrows(IOException.class,()->ArchiveJson.library(bytes));}
 @Test public void metadataIdentityTypesAndFractionalVersionsAreRejected()throws Exception{JSONObject value=new JSONObject(new String(metadata(),StandardCharsets.UTF_8));value.put("id",UUID.randomUUID().toString());assertThrows(IOException.class,()->ArchiveJson.metadata(utf(value.toString()),ID));value.put("id",ID).put("version",1.5);assertThrows(IOException.class,()->ArchiveJson.metadata(utf(value.toString()),ID));value.put("version",1).put("text",new JSONObject());assertThrows(IOException.class,()->ArchiveJson.metadata(utf(value.toString()),ID));}
 @Test public void malformedJsonErrorsDoNotEchoTranscriptContent()throws Exception{String secret="PRIVATE_TRANSCRIPT_NEVER_IN_DIAGNOSTICS";IOException error=assertThrows(IOException.class,()->ArchiveJson.metadata(utf("{\"text\":\""+secret),ID));assertFalse(error.toString().contains(secret));assertNull(error.getCause());}
 @Test public void atomicMetadataSnapshotsAreBoundedAndLeaveOriginalRecordingUntouched()throws Exception{
  Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();File root=new File(target.getCacheDir(),"archive-semantic-"+UUID.randomUUID());assertTrue(root.mkdir());Context context=new ContextWrapper(target){@Override public File getFilesDir(){return root;}@Override public Context getApplicationContext(){return this;}};
  try{VoiceFiles voices=new VoiceFiles(context);assertNull(voices.archiveMetadata(ID));try(RandomAccessFile wave=new RandomAccessFile(voices.audio(ID),"rw")){wave.setLength(48);PcmWave.header(wave,4);}voices.result(ID,"Original transcript","Ready");assertEquals("Original transcript",ArchiveJson.metadata(voices.archiveMetadata(ID),ID).getString("text"));try(FileOutputStream out=new FileOutputStream(voices.transcript(ID))){byte[] chunk=new byte[8192];for(int i=0;i<=ArchiveManifest.MAX_METADATA_BYTES/chunk.length;i++)out.write(chunk);}assertThrows(IOException.class,()->voices.archiveMetadata(ID));assertEquals(48,voices.audio(ID).length());assertTrue(voices.transcript(ID).length()>ArchiveManifest.MAX_METADATA_BYTES);assertFalse(voices.noteWasCreated(ID));}
  finally{File[] children=new File(root,"voice-inbox-v1").listFiles();if(children!=null)for(File file:children)assertTrue(file.delete());assertTrue(new File(root,"voice-inbox-v1").delete());assertTrue(root.delete());}
 }
 @Test public void transcriptContentIdentityIgnoresOperationalStateUuidAndKeyOrderButPreservesWords()throws Exception{
  String original=ArchiveJson.metadataContentHash(metadata(),ID),other=UUID.randomUUID().toString();JSONObject changed=new JSONObject(new String(metadata(),StandardCharsets.UTF_8));changed.put("id",other).put("state","RUNNING");assertEquals(original,ArchiveJson.metadataContentHash(utf(changed.toString()),other));
  JSONObject reordered=new JSONObject().put("formatting",changed.getString("formatting")).put("rawText",changed.getString("rawText")).put("state","DONE").put("text",changed.getString("text")).put("id",other).put("version",1);assertEquals(original,ArchiveJson.metadataContentHash(utf(reordered.toString()),other));reordered.put("rawText","Different original words");assertNotEquals(original,ArchiveJson.metadataContentHash(utf(reordered.toString()),other));
 }

}
