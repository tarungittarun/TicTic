package app.space.notes.domain;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class FolderPresentationTest {
 @Test public void unicodeNamesAndTrimming(){assertEquals("Projects ಕನ್ನಡ",FolderNames.require(" Projects ಕನ್ನಡ "));}
 @Test public void traversalAndSeparatorsAreRefused(){for(String value:new String[]{""," ",".","..","a/b","a\\b","a\nb"}){try{FolderNames.require(value);fail(value);}catch(IllegalArgumentException expected){}}}
 @Test public void oldConstructorsDefaultToPlainRoot(){Note n=Note.create(1);assertEquals("",n.folder);assertEquals(0,n.background);}
 @Test public void fifteenPatternsAccepted(){for(int i=1;i<=15;i++)assertEquals(i,Note.create(1).background(i,2).background);}
 @Test(expected=IllegalArgumentException.class) public void unknownPatternRefused(){Note.create(1).background(16,2);}
 @Test public void allMutationPathsRetainPresentation(){Note n=Note.create(1).folder("Work",2).background(12,3);List<Note> changed=Arrays.asList(n.edit("a","b",4),n.properties(2,Arrays.asList("Home"),4),n.state(true,true,0,4),n.rich("a","b",Collections.emptyList(),4),n.convert(4),n.withId("other"),n.diagram(Diagram.empty(),4),n.reminder(5000,4),n.voiceSource(UUID.randomUUID().toString(),4),NoteOperations.duplicate(n,5));for(Note value:changed){assertEquals("Work",value.folder);assertEquals(12,value.background);}}
 @Test public void folderAndBackgroundDifferencesCannotBeLostOnRestore(){Note n=Note.create(1);assertFalse(NoteMerge.same(n,n.folder("Work",1)));assertFalse(NoteMerge.same(n,n.background(2,1)));assertEquals(1,NoteMerge.preview(Arrays.asList(n),Arrays.asList(n.folder("Work",1))).conflicts);}
}
