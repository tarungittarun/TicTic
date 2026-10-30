package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class DocumentHistoryTest {
    private Note note(){Note note=Note.create(1).edit("T","Body",2);return note.withContent(DocumentContent.fromLegacy(note),3);}
    @Test public void textAndStructuralEditsShareOneUndoRedoSequence(){Note a=note(),b=a.title("New",4),c=b.withContent(b.content.insert(1,DocumentContent.Block.table("t","Table01",TextTable.empty(1,1))),5);DocumentHistory h=new DocumentHistory();h.record(a,b,"title",0);h.record(b,c,null,1);assertSame(b,h.undo(c));assertSame(a,h.undo(b));assertSame(b,h.redo(a));assertSame(c,h.redo(b));}
    @Test public void typingCoalescesAndNewEditsInvalidateRedo(){Note a=note(),b=a.title("N",4),c=a.title("New",5);DocumentHistory h=new DocumentHistory();h.record(a,b,"title",1);h.record(b,c,"title",100);assertSame(a,h.undo(c));assertFalse(h.canUndo());assertTrue(h.canRedo());h.record(a,b,"title",1000);assertFalse(h.canRedo());}
    @Test public void keepsAtMostFiftySnapshotsAndCannotCrossDocuments(){Note a=note();DocumentHistory h=new DocumentHistory();for(int i=0;i<70;i++){Note b=a.title("Title"+i,10+i);h.record(a,b,null,i);a=b;}int steps=0;while(h.canUndo()){a=h.undo(a);steps++;}assertEquals(50,steps);final Note current=a;assertThrows(IllegalArgumentException.class,()->h.record(current,current.withId("other"),null,100));}
}
