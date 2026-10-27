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
