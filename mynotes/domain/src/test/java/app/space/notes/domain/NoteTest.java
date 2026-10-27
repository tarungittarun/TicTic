package app.space.notes.domain;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NoteTest {
    @Test public void uniqueIds() { assertNotEquals(Note.create(1).id, Note.create(1).id); }
    @Test public void editPreservesIdAndCreation() {
        Note a=Note.create(10), b=a.edit("नोट", "ಕನ್ನಡ",20);
        assertEquals(a.id,b.id); assertEquals(10,b.createdAt); assertEquals(20,b.updatedAt);
        assertEquals("",a.title); assertEquals("नोट",b.title);
    }
    @Test public void backwardClockDoesNotRegressEditTime() {
        assertEquals(20,Note.create(20).edit("x","y",10).updatedAt);
    }
    @Test public void trashBoundary() {
        Note a=Note.create(10).state(false,false,100,100);
        assertFalse(a.expired(100+Note.TRASH_RETENTION_MS-1));
        assertTrue(a.expired(100+Note.TRASH_RETENTION_MS));
        assertFalse(a.expired(99));
    }
    @Test public void restoreDisarmsExpiry() {
        Note a=Note.create(10).state(true,true,100,100).state(true,true,0,200);
        assertFalse(a.expired(Long.MAX_VALUE)); assertTrue(a.archived); assertTrue(a.pinned);
    }
    @Test public void searchTrimsAndIgnoresCase() {
        Note a=Note.create(1).edit("Project","Meeting notes",2);
        assertTrue(a.matches(" MEETING ")); assertTrue(a.matches("project"));
        assertFalse(a.matches("missing")); assertTrue(a.matches(""));
    }
    @Test(expected=IllegalArgumentException.class) public void invalidColorRejected() {
        new Note("id","","",1,1,0,false,false,12);
    }
}
