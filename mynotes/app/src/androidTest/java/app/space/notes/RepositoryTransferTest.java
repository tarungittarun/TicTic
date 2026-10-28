package app.space.notes;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.IOException;
import java.util.*;
import app.space.notes.domain.Note;
import app.space.notes.domain.NoteMerge;
import app.space.notes.runtime.DocumentCodec;
import app.space.notes.runtime.GenerationProtocol;
import app.space.notes.runtime.NotesRepository;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/** Real AtomicFile + injected document backend. Does not claim SAF-device coverage. */
@RunWith(AndroidJUnit4.class)
public class RepositoryTransferTest {
    private File fixture;private Context context;
    private final Uri uri=Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ARestore");
    private static final class Memory implements GenerationProtocol.Backend {
        final Map<String,byte[]> files=new HashMap<>();boolean unavailable;
        public Map<String,byte[]> readAll() throws Exception {
            if(unavailable)throw new IOException("revoked");return new HashMap<>(files);
        }
        public void create(String name,byte[] bytes) {files.put(name,bytes);}
    }
    private Memory backend;
    private NotesRepository repo() {return new NotesRepository(context,u->new GenerationProtocol(backend));}
    private Note n(String id,String body) {return new Note(id,"title",body,1,2,0,false,false,0);}
    @Before public void setup() {
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
        fixture=new File(target.getCacheDir(),"transfer-"+UUID.randomUUID());assertTrue(fixture.mkdirs());backend=new Memory();
        context=new ContextWrapper(target) {
            @Override public File getFilesDir(){return fixture;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode) {
                return target.getSharedPreferences(fixture.getName()+"-"+name,mode);
            }
        };
    }
    private void remove(File file){File[] children=file.listFiles();if(children!=null)for(File f:children)remove(f);file.delete();}
    @After public void cleanup(){
        InstrumentationRegistry.getInstrumentation().getTargetContext().deleteSharedPreferences(fixture.getName()+"-mynotes-storage-v1");remove(fixture);
    }
    @Test public void openingLibraryPreservesBothAndPriorCheckpoint() throws Exception {
        NotesRepository repository=repo();List<Note> current=Arrays.asList(n("local","keep"));repository.save(current);
        new GenerationProtocol(backend).commit(DocumentCodec.encode(Arrays.asList(n("external","recover"))),null);
        GenerationProtocol.Snapshot preview=repository.inspectFolder(uri);
        List<Note> merged=NoteMerge.preview(current,DocumentCodec.decode(preview.payload)).notes;
        assertEquals(1,repository.load().size()); // preview has not changed current library
        repository.openExisting(uri,preview.head,current,merged);
        assertEquals(2,repo().load().size());assertEquals("local",repository.previousSnapshot().get(0).id);
        assertEquals(2,DocumentCodec.decode(new GenerationProtocol(backend).read().payload).size());
    }
    @Test public void stalePreviewDoesNotSwitchCurrentDestination() throws Exception {
        NotesRepository repository=repo();List<Note> current=Arrays.asList(n("local","keep"));repository.save(current);
        GenerationProtocol store=new GenerationProtocol(backend);
        String head=store.commit(DocumentCodec.encode(Arrays.asList(n("external","old"))),null).head;
        List<Note> merged=NoteMerge.preview(current,DocumentCodec.decode(repository.inspectFolder(uri).payload)).notes;
        store.commit(DocumentCodec.encode(Arrays.asList(n("external","new writer"))),head);
        try {repository.openExisting(uri,head,current,merged);fail("must refuse stale plan");}
        catch(IOException expected) { }
        assertEquals(1,repo().load().size());assertEquals("local",repo().load().get(0).id);
        assertEquals("new writer",DocumentCodec.decode(store.read().payload).get(0).body);
    }
    @Test public void revokedFolderSaveRecoversAcrossRepositoryRestartAndRetries() throws Exception {
        NotesRepository repository=repo();List<Note> current=Arrays.asList(n("local","before"));repository.save(current);repository.selectFolder(uri,current);
        backend.unavailable=true;
        try {repository.save(Arrays.asList(n("local","after")));fail("must show pending");}catch(IOException expected) { }
        NotesRepository reopened=repo();assertEquals("after",reopened.load().get(0).body);
        backend.unavailable=false;reopened.save(Arrays.asList(n("local","after")));
        assertEquals("after",DocumentCodec.decode(new GenerationProtocol(backend).read().payload).get(0).body);
    }
    @Test public void restoreIntoUnavailableFolderIsRetainedPrivatelyNotLost() throws Exception {
        NotesRepository repository=repo();List<Note> before=Arrays.asList(n("one","keep"));repository.save(before);repository.selectFolder(uri,before);
        List<Note> merged=NoteMerge.preview(before,Arrays.asList(n("two","restore"))).notes;
        backend.unavailable=true;assertFalse(repository.applyRestore(before,merged));
        assertEquals(2,repo().load().size());assertEquals(1,repository.previousSnapshot().size());
    }
    @Test public void compareAndApplyRestoreKeepsLatestWriterAndPreviousCheckpoint()throws Exception{
        NotesRepository repository=repo();List<Note> original=Arrays.asList(n("one","original")),base=Arrays.asList(n("one","preview base")),latest=Arrays.asList(n("one","newer writer"));repository.save(original);repository.applyRestore(original,base);repo().save(latest);
        org.junit.Assert.assertThrows(IOException.class,()->repository.applyRestoreIfCurrent(base,Arrays.asList(n("two","stale import"))));org.junit.Assert.assertArrayEquals(DocumentCodec.encode(latest),DocumentCodec.encode(repo().localSnapshot()));org.junit.Assert.assertArrayEquals(DocumentCodec.encode(original),DocumentCodec.encode(repository.previousSnapshot()));
    }
    @Test public void compareAndApplyRestoreCommitsOnlyMatchingLibraryAndStorage()throws Exception{
        NotesRepository repository=repo();List<Note> base=Arrays.asList(n("one","base")),merged=Arrays.asList(n("one","base"),n("two","import"));repository.save(base);assertTrue(repository.applyRestoreIfCurrent(base,merged));org.junit.Assert.assertArrayEquals(DocumentCodec.encode(base),DocumentCodec.encode(repository.previousSnapshot()));org.junit.Assert.assertArrayEquals(DocumentCodec.encode(merged),DocumentCodec.encode(repository.localSnapshot()));
        NotesRepository stale=repo();stale.load();repo().selectFolder(uri,merged);org.junit.Assert.assertThrows(IOException.class,()->stale.applyRestoreIfCurrent(merged,base));org.junit.Assert.assertArrayEquals(DocumentCodec.encode(merged),DocumentCodec.encode(repo().localSnapshot()));
    }

}
