package app.space.notes;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import app.space.notes.domain.Note;
import app.space.notes.runtime.NotesRepository;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Android JSONObject + AtomicFile tests. Separate fixture path; never a user's library. */
@RunWith(AndroidJUnit4.class)
public class NotesPersistenceTest {
    private File fixture;
    private Context context;
    @Before public void setup() {
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
        fixture=new File(target.getCacheDir(),"notes-test-"+UUID.randomUUID());
        assertTrue(fixture.mkdirs());
        context=new ContextWrapper(target) {
            @Override public File getFilesDir() { return fixture; }
            @Override public SharedPreferences getSharedPreferences(String name,int mode) {
                return target.getSharedPreferences(fixture.getName()+"-"+name,mode);
            }
        };
    }
    @After public void cleanup() {
        InstrumentationRegistry.getInstrumentation().getTargetContext().deleteSharedPreferences(fixture.getName()+"-mynotes-storage-v1");
        remove(fixture);
    }
    private void remove(File file) {
        File[] children=file.listFiles();
        if(children!=null)for(File child:children)remove(child);
        file.delete();
    }
    @Test public void unicodeAndAllMetadataSurviveReopen() throws Exception {
        Note note=new Note("id","नोट","ಕನ್ನಡ",10,20,30,true,true,11);
        new NotesRepository(context).save(Arrays.asList(note));
        Note read=new NotesRepository(context).load().get(0);
        assertEquals(note.id,read.id); assertEquals(note.title,read.title);
        assertEquals(note.body,read.body); assertEquals(10,read.createdAt);
        assertEquals(20,read.updatedAt); assertEquals(30,read.deletedAt);
        assertTrue(read.pinned); assertTrue(read.archived); assertEquals(11,read.color);
    }
    @Test public void moreThanLegacyCapSurvives() throws Exception {
        List<Note> notes=new ArrayList<>();
        for(int i=0;i<650;i++)notes.add(Note.create(100));
        NotesRepository repository=new NotesRepository(context);
        repository.save(notes); assertEquals(650,repository.load().size());
    }
    @Test public void corruptLibraryFailsInsteadOfBecomingEmpty() throws Exception {
        NotesRepository repository=new NotesRepository(context);
        File file=new File(fixture,"notes-library/library-v1.json");
        try(FileOutputStream out=new FileOutputStream(file)) {
            out.write("{broken".getBytes(StandardCharsets.UTF_8));
        }
        try { repository.load(); fail("Corrupt data must be reported"); }
        catch(org.json.JSONException | java.io.IOException expected) { }
        assertEquals(7,file.length());
    }
    @Test public void emptyCommitIsPersisted() throws Exception {
        NotesRepository repository=new NotesRepository(context);
        repository.save(Arrays.asList(Note.create(10)));
        repository.save(new ArrayList<Note>());
        assertTrue(new NotesRepository(context).load().isEmpty());
    }
    @Test public void interruptedAtomicWriteRestoresPriorDocument() throws Exception {
        NotesRepository repository=new NotesRepository(context);
        repository.save(Arrays.asList(Note.create(10).edit("before","safe",20)));
        android.util.AtomicFile atomic=new android.util.AtomicFile(new File(fixture,"notes-library/library-v1.json"));
        FileOutputStream stream=atomic.startWrite();
        stream.write("incomplete".getBytes(StandardCharsets.UTF_8));
        atomic.failWrite(stream);
        assertEquals("before",new NotesRepository(context).load().get(0).title);
    }
    @Test public void restoreCheckpointsCurrentBeforeWritingMerged() throws Exception {
        NotesRepository repository=new NotesRepository(context);
        List<Note> before=Arrays.asList(Note.create(10).edit("before","safe",20));
        repository.save(before);
        List<Note> merged=new ArrayList<>(before);merged.add(Note.create(30).edit("restored","body",40));
        assertTrue(repository.applyRestore(before,merged));
        assertEquals(2,new NotesRepository(context).load().size());
        assertEquals("before",repository.previousSnapshot().get(0).title);
        assertEquals(1,repository.previousSnapshot().size());
    }
    @Test public void missingPreviousSnapshotDoesNotEmptyLibrary() throws Exception {
        NotesRepository repository=new NotesRepository(context);
        repository.save(Arrays.asList(Note.create(10)));
        try {repository.previousSnapshot();fail("missing snapshot must fail");}
        catch(java.io.FileNotFoundException expected) { }
        assertEquals(1,repository.load().size());
    }
}
