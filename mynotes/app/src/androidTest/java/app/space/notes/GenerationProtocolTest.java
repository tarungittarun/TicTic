package app.space.notes;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.IOException;
import java.util.*;
import app.space.notes.domain.Note;
import app.space.notes.runtime.DocumentCodec;
import app.space.notes.runtime.GenerationProtocol;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Protocol tests run on Android JSON with a fault-injectable backend, not a SAF pass. */
@RunWith(AndroidJUnit4.class)
public class GenerationProtocolTest {
    private static final class Memory implements GenerationProtocol.Backend {
        final Map<String,byte[]> files=new HashMap<>();
        boolean partial,unavailable,failAfterClose;
        public Map<String,byte[]> readAll() throws Exception {
            if(unavailable)throw new IOException("revoked");return new HashMap<>(files);
        }
        public void create(String name,byte[] value) throws Exception {
            files.put(name,partial?Arrays.copyOf(value,value.length/2):value);
            if(partial || failAfterClose)throw new IOException("interrupted");
        }
    }
    private static final class Blobs implements GenerationProtocol.BlobBackend {
        final Map<String,byte[]> files=new HashMap<>(),media=new HashMap<>();boolean failBlob;
        public Map<String,byte[]> readAll(){return new HashMap<>(files);}public void create(String name,byte[] bytes){files.put(name,bytes);}public byte[] readBlob(String hash)throws Exception{if(!media.containsKey(hash))throw new IOException("missing media");return media.get(hash);}public void createBlob(String hash,byte[] bytes)throws Exception{if(failBlob)throw new IOException("media write failed");media.put(hash,bytes);}
    }
    private Note pictured(){app.space.notes.domain.NoteImage image=new app.space.notes.domain.NoteImage(UUID.randomUUID().toString(),"Image","image/png",1,1,"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aXioAAAAASUVORK5CYII=");return Note.create(1).images(Arrays.asList(image),2);}
    @Test public void autosavesShareMediaAndHydrateExactPortablePayload()throws Exception{Blobs backend=new Blobs();GenerationProtocol store=new GenerationProtocol(backend);Note n=pictured();GenerationProtocol.Snapshot first=store.commit(DocumentCodec.encode(Arrays.asList(n)),null);byte[] edited=DocumentCodec.encode(Arrays.asList(n.edit("Updated","Words",3)));store.commit(edited,first.head);assertEquals(1,backend.media.size());assertEquals(2,backend.files.size());for(byte[] record:backend.files.values()){org.json.JSONObject envelope=new org.json.JSONObject(new String(record,"UTF-8"));assertEquals(2,envelope.getInt("format"));assertTrue(envelope.getString("payload").contains("\"blob\""));assertTrue(!envelope.getString("payload").contains(n.images.get(0).data));}assertArrayEquals(edited,store.read().payload);}
    @Test public void corruptOrMissingSharedMediaNeverBecomesSilentImageLoss()throws Exception{Blobs backend=new Blobs();GenerationProtocol store=new GenerationProtocol(backend);store.commit(DocumentCodec.encode(Arrays.asList(pictured())),null);String hash=backend.media.keySet().iterator().next();backend.media.put(hash,new byte[]{1,2,3});try{store.read();fail("corrupt media accepted");}catch(IOException expected){}backend.media.clear();try{store.read();fail("missing media accepted");}catch(IOException expected){}}
    @Test public void failedMediaWriteDoesNotCommitAnUnresolvableGeneration()throws Exception{Blobs backend=new Blobs();GenerationProtocol store=new GenerationProtocol(backend);String head=store.commit(data("Original"),null).head;backend.failBlob=true;try{store.commit(DocumentCodec.encode(Arrays.asList(pictured())),head);fail("write should fail");}catch(IOException expected){}assertEquals(1,backend.files.size());assertEquals(head,store.read().head);}
    @Test public void sharedMediaCanExtendLegacyFormatOneHistory()throws Exception{Memory old=new Memory();String head=new GenerationProtocol(old).commit(data("Legacy"),null).head;Blobs backend=new Blobs();backend.files.putAll(old.files);GenerationProtocol store=new GenerationProtocol(backend);byte[] value=DocumentCodec.encode(Arrays.asList(pictured()));store.commit(value,head);assertArrayEquals(value,store.read().payload);assertEquals(2,backend.files.size());}
    private byte[] data(String title) throws Exception {
        return DocumentCodec.encode(Arrays.asList(new Note("stable","title: "+title,"ಕನ್ನಡ",1,2,0,false,false,0)));
    }
    @Test public void savesAndReopens() throws Exception {
        Memory backend=new Memory(); GenerationProtocol store=new GenerationProtocol(backend);
        byte[] value=data("first");GenerationProtocol.Snapshot saved=store.commit(value,null);
        assertEquals(saved.head,new GenerationProtocol(backend).read().head);
        assertArrayEquals(value,store.read().payload);
    }
    @Test public void interruptedWriteRetainsPrevious() throws Exception {
        Memory backend=new Memory();GenerationProtocol store=new GenerationProtocol(backend);
        String previous=store.commit(data("first"),null).head;backend.partial=true;
        try {store.commit(data("second"),previous);fail("must fail");}catch(IOException expected) { }
        assertEquals(previous,store.read().head);
    }
    @Test public void lostAcknowledgementDoesNotDuplicateCommit() throws Exception {
        Memory backend=new Memory();GenerationProtocol store=new GenerationProtocol(backend);
        String previous=store.commit(data("first"),null).head;backend.failAfterClose=true;
        byte[] value=data("second");
        try {store.commit(value,previous);fail("must fail");}catch(IOException expected) { }
        backend.failAfterClose=false;assertArrayEquals(value,store.commit(value,previous).payload);
        assertEquals(2,backend.files.size());
    }
    @Test public void staleWriterDoesNotOverwrite() throws Exception {
        Memory backend=new Memory();GenerationProtocol store=new GenerationProtocol(backend);
        String previous=store.commit(data("first"),null).head;
        store.commit(data("other writer"),previous);
        try {store.commit(data("my changes"),previous);fail("must conflict");}catch(IOException expected) { }
        assertEquals(2,backend.files.size());
    }
    @Test public void corruptOnlyLibraryIsNotEmpty() throws Exception {
        Memory backend=new Memory();backend.files.put("mynotes-gen-bad.json",new byte[]{'{'});
        try {new GenerationProtocol(backend).read();fail("must protect data");}catch(IOException expected) { }
    }
    @Test public void providerAccessFailureIsReported() throws Exception {
        Memory backend=new Memory();backend.unavailable=true;
        try {new GenerationProtocol(backend).read();fail("must report unavailable");}catch(IOException expected) { }
    }
    @Test public void previousPrivateFormatStillReads() throws Exception {
        byte[] original=("{\"version\":1,\"notes\":[{\"id\":\"legacy\",\"title\":\"old\",\"body\":\"kept\","
            +"\"createdAt\":1,\"updatedAt\":2,\"deletedAt\":0,\"pinned\":true,\"archived\":false,\"color\":0}]}")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("legacy",DocumentCodec.decode(original).get(0).id);
        assertTrue(DocumentCodec.decode(original).get(0).pinned);
    }
    @Test public void nonLocalProviderRejectedBeforeAccess() throws Exception {
        try {
            new app.space.notes.runtime.LocalTreeBackend(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getContentResolver(),
                android.net.Uri.parse("content://cloud.example/tree/library"));
            fail("cloud must be refused");
        }catch(IOException expected) { }
    }
}
