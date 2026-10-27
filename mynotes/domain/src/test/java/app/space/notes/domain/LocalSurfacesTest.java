package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class LocalSurfacesTest {
 @Test public void reminderSurvivesEditsConversionAndSourceApplication(){Note n=Note.create(1).reminder(1000,2).edit("title","body",3).convert(4).properties(2,Arrays.asList("work"),5).voiceSource(UUID.randomUUID().toString(),6);assertEquals(1000,n.reminderAt);assertEquals(1000,n.withId("other").diagram(Diagram.empty(),7).reminderAt);}
 @Test public void trashCancelsSchedulingAndRestoreRetainsTime(){Note n=Note.create(1).reminder(500,2);assertTrue(LocalSurfaces.scheduled(n,100));assertFalse(LocalSurfaces.scheduled(n.state(false,false,101,101),102));assertTrue(LocalSurfaces.scheduled(n.state(false,true,0,103),104));assertFalse(LocalSurfaces.scheduled(n,500));}
 @Test public void widgetIsBoundedPinnedFirstAndExcludesPrivateBins(){List<Note> notes=new ArrayList<>();for(int i=0;i<10;i++)notes.add(Note.create(i+1));Note pinned=Note.create(1).state(true,false,0,2);notes.add(pinned);notes.add(Note.create(1).state(true,true,0,3));notes.add(Note.create(1).state(true,false,4,4));List<Note> view=LocalSurfaces.widget(notes);assertEquals(4,view.size());assertEquals(pinned.id,view.get(0).id);assertTrue(view.get(1).updatedAt>view.get(2).updatedAt);}
 @Test public void lateVoiceRetryCannotRecreatePurgedNote(){assertFalse(VoiceTranscripts.mayApplyToLibrary(false,true));assertTrue(VoiceTranscripts.mayApplyToLibrary(true,true));assertTrue(VoiceTranscripts.mayApplyToLibrary(false,false));}
 @Test public void invalidReminderRejectedAndDuplicateDoesNotCopyAlert(){Note n=Note.create(1).reminder(1000,2);assertEquals(0,NoteOperations.duplicate(n,3).reminderAt);try{n.reminder(-1,3);fail();}catch(IllegalArgumentException expected){}}
}
