package app.space.notes.domain;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class NoteMergeTest {
    private Note n(String id,String body) {return new Note(id,"नोट",body,1,2,0,true,true,11);}
    @Test public void newIdsAppendWithoutRemovingCurrent() {
        NoteMerge.Plan p=NoteMerge.preview(Arrays.asList(n("one","old")),Arrays.asList(n("two","new")));
        assertEquals(2,p.notes.size());assertEquals(1,p.added);assertEquals("old",p.notes.get(0).body);
    }
    @Test public void exactDuplicateSkipped() {
        NoteMerge.Plan p=NoteMerge.preview(Arrays.asList(n("id","body")),Arrays.asList(n("id","body")));
        assertEquals(1,p.notes.size());assertEquals(1,p.unchanged);
    }
    @Test public void conflictsPreserveBothAndMetadata() {
        NoteMerge.Plan p=NoteMerge.preview(Arrays.asList(n("id","old")),Arrays.asList(n("id","new")));
        assertEquals(1,p.conflicts);assertEquals("old",p.notes.get(0).body);
        assertEquals("new",p.notes.get(1).body);assertNotEquals("id",p.notes.get(1).id);
        assertTrue(p.notes.get(1).archived);assertEquals(11,p.notes.get(1).color);
    }
    @Test public void repeatedConflictImportIsIdempotent() {
        Note incoming=n("id","new");NoteMerge.Plan first=NoteMerge.preview(Arrays.asList(n("id","old")),Arrays.asList(incoming));
        NoteMerge.Plan second=NoteMerge.preview(first.notes,Arrays.asList(incoming));
        assertEquals(2,second.notes.size());assertEquals(0,second.added);assertEquals(1,second.unchanged);
    }
    @Test public void emptyRestoreDoesNotDelete() {
        assertEquals(1,NoteMerge.preview(Arrays.asList(n("id","keep")),Collections.emptyList()).notes.size());
    }
    @Test(expected=IllegalArgumentException.class) public void duplicateIncomingRejected() {
        NoteMerge.preview(Collections.emptyList(),Arrays.asList(n("id","a"),n("id","b")));
    }
    @Test public void trashMetadataIsPreserved() {
        Note n=new Note("id","title","body",1,2,100,false,false,0);
        assertEquals(100,NoteMerge.preview(Collections.emptyList(),Arrays.asList(n)).notes.get(0).deletedAt);
    }
}
