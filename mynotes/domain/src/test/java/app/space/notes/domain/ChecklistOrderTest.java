package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ChecklistOrderTest {
 private List<Note.Item> rows(){return Collections.unmodifiableList(Arrays.asList(new Note.Item("a","First",true,3),new Note.Item("b","Second",false,0),new Note.Item("c","Third",true,1)));}
 @Test public void movesToBothEndsPreservingEveryItemObject(){List<Note.Item> before=rows(),after=ChecklistOrder.move(before,"a",3);assertEquals("b",after.get(0).id);assertSame(before.get(0),after.get(2));assertEquals("a",before.get(0).id);assertEquals("a",ChecklistOrder.move(after,"a",0).get(0).id);}
 @Test public void edgesBesideSameItemAreNoOps(){List<Note.Item> rows=rows();assertSame(rows,ChecklistOrder.move(rows,"b",1));assertSame(rows,ChecklistOrder.move(rows,"b",2));}
 @Test public void invalidAndStaleMovesFailWithoutMutation(){List<Note.Item> rows=rows();assertThrows(IllegalArgumentException.class,()->ChecklistOrder.move(rows,"missing",0));assertThrows(IllegalArgumentException.class,()->ChecklistOrder.move(rows,"a",-1));assertThrows(IllegalArgumentException.class,()->ChecklistOrder.move(rows,"a",4));assertEquals("a",rows.get(0).id);}
 @Test public void duplicateIdsAreRefused(){List<Note.Item> rows=Arrays.asList(new Note.Item("a","One",false,0),new Note.Item("a","Two",false,1));assertThrows(IllegalArgumentException.class,()->ChecklistOrder.move(rows,"a",0));}
 @Test public void movePreservesCheckedDepthTextAndIdentity(){List<Note.Item> before=rows();List<Note.Item> after=ChecklistOrder.move(before,"c",1);assertSame(before.get(2),after.get(1));assertTrue(after.get(1).checked);assertEquals(1,after.get(1).depth);assertEquals("Third",after.get(1).text);assertThrows(UnsupportedOperationException.class,()->after.clear());}
}
