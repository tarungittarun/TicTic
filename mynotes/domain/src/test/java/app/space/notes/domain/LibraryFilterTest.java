package app.space.notes.domain;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class LibraryFilterTest {
 private Note note(String... labels){return Note.create(1).properties(0,Arrays.asList(labels),2);}
 private LibraryFilter filter(boolean all,boolean empty,String... selected){return new LibraryFilter(all,empty,Arrays.asList(selected),"edited",true);}
 @Test public void allAndNoneAreUnambiguous(){assertTrue(filter(true,false).matches(note("Work")));assertTrue(filter(true,false).matches(note()));assertFalse(filter(false,false).matches(note("Work")));assertFalse(filter(false,false).matches(note()));}
 @Test public void arbitrarySubsetIsCaseInsensitiveAnyOf(){LibraryFilter f=filter(false,false,"WORK","Personal");assertTrue(f.matches(note("work")));assertTrue(f.matches(note("Other","PERSONAL")));assertFalse(f.matches(note("Other")));assertFalse(f.matches(note()));}
 @Test public void unlabelledCanBeCombined(){LibraryFilter f=filter(false,true,"Work");assertTrue(f.matches(note()));assertTrue(f.matches(note("Work")));assertFalse(f.matches(note("Other")));}
 @Test public void searchSnapshotCannotBeMutated(){List<String> names=new ArrayList<>(Arrays.asList("Work"));LibraryFilter f=new LibraryFilter(false,false,names,"title",false);names.clear();assertTrue(f.matches(note("work")));try{f.labels.clear();fail();}catch(UnsupportedOperationException expected){}}
 @Test public void pinsBeatChosenOrder(){Note a=Note.create(1).edit("Apple","",2),z=Note.create(1).edit("Zebra","",2).state(true,false,0,2);assertTrue(new LibraryFilter(true,true,Collections.emptyList(),"title",true).comparator().compare(z,a)<0);}
 @Test public void createdAndEditedSortDiffer(){Note a=Note.create(1).edit("a","",100),b=Note.create(2);assertTrue(filter(true,true).comparator().compare(a,b)<0);assertTrue(new LibraryFilter(true,true,Collections.emptyList(),"created",true).comparator().compare(b,a)<0);}
 @Test(expected=IllegalArgumentException.class) public void unknownSortRefused(){new LibraryFilter(true,true,Collections.emptyList(),"random",true);}
}
