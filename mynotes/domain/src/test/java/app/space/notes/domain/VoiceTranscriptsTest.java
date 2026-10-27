package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class VoiceTranscriptsTest {
    private final String id="00000000-0000-0000-0000-000000000001";
    @Test public void recoveredTranscriptIsIdempotent(){Note n=VoiceTranscripts.apply(VoiceTranscripts.placeholder(id,1),id,"one zero one",2);assertTrue(n.body.contains("one zero one"));assertEquals(1,n.voiceSources.size());assertTrue(n==VoiceTranscripts.apply(n,id,"one zero one",3));}
    @Test public void userEditsBeforeCompletionArePreserved(){Note n=Note.create(1).edit("My title","Keep typed words",2);Note read=VoiceTranscripts.apply(n,id,"Recognized speech",3);assertEquals("My title",read.title);assertTrue(read.body.startsWith("Keep typed words"));assertTrue(read.body.contains("Recognized speech"));}
    @Test public void emptyRecognitionDoesNotReplaceAnything(){Note n=VoiceTranscripts.placeholder(id,1);assertTrue(n==VoiceTranscripts.apply(n,id," ",2));}
    @Test public void trashedNoteIsNeverResurrectedByRecognition(){Note n=VoiceTranscripts.placeholder(id,1).state(false,false,2,2);assertTrue(n==VoiceTranscripts.apply(n,id,"words",3));assertEquals(2,n.deletedAt);}
    @Test public void checklistRetainsRowsWhenTranscriptArrives(){Note n=Note.create(1).convert(2);Note read=VoiceTranscripts.apply(n,id,"words",3);assertTrue(read.checklist);assertEquals(n.items.get(0).id,read.items.get(0).id);assertTrue(read.items.size()>n.items.size());}
    @Test public void voiceSourceSurvivesMetadataCopyAndBackupConflictIdentity(){Note n=VoiceTranscripts.apply(Note.create(1),id,"words",2);assertEquals(n.voiceSources,n.properties(3,Arrays.asList("Work"),3).voiceSources);assertEquals(n.voiceSources,NoteOperations.duplicate(n,4).voiceSources);assertFalse(NoteMerge.same(n,n.rich("","changed",Collections.emptyList(),4)));}
    @Test public void stableNoteIdUsesRecordingIdentityNotCurrentTime(){assertEquals(VoiceTranscripts.noteId(id),VoiceTranscripts.placeholder(id,100).id);}
}
