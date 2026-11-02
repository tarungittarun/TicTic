package app.space.notes.domain;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ManualOrderTest {
 @Test public void filteredSlotsPreserveAllHiddenItems(){assertEquals(Arrays.asList("c","hidden","a","other","b"),ManualOrder.replaceVisible(Arrays.asList("a","hidden","b","other","c"),Arrays.asList("c","a","b")));}
 @Test public void emptyDragDoesNotChangeOrder(){assertEquals(Arrays.asList("a","b"),ManualOrder.replaceVisible(Arrays.asList("a","b"),Collections.emptyList()));}
 @Test(expected=IllegalArgumentException.class) public void duplicatesRejected(){ManualOrder.replaceVisible(Arrays.asList("a","b"),Arrays.asList("a","a"));}
 @Test(expected=IllegalArgumentException.class) public void unknownRejected(){ManualOrder.replaceVisible(Arrays.asList("a","b"),Arrays.asList("c"));}
 @Test public void pinsStayFirstAndUnknownsStayReachable(){Note a=Note.create(1),b=Note.create(2),p=Note.create(3).state(true,false,0,3),unknown=Note.create(4);List<Note> notes=new ArrayList<>(Arrays.asList(a,b,p,unknown));notes.sort(new LibraryFilter(true,true,Collections.emptyList(),"manual",true).comparator(Arrays.asList(b.id,a.id,p.id)));assertEquals(Arrays.asList(p,b,a,unknown),notes);}
}
