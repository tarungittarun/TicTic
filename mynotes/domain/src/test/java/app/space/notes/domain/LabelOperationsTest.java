package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LabelOperationsTest {
    private Note note(){return Note.create(1).properties(2,Arrays.asList("Work","Ideas"),2);}
    @Test public void globalRenameIncludesTrashAndArchive(){List<Note> result=LabelOperations.rename(Arrays.asList(note(),note().state(false,true,3,3)),"work","Projects",4);for(Note n:result){assertTrue(n.labels.contains("Projects"));assertEquals(2,n.color);}assertEquals(3,result.get(1).deletedAt);}
    @Test public void renameCombinesExistingCaseInsensitiveLabel(){Note n=note().properties(0,Arrays.asList("Work","workshop"),2);assertEquals(1,LabelOperations.rename(Arrays.asList(n),"Work","Workshop",3).get(0).labels.size());}
    @Test public void removePreservesDocumentContentAndOtherLabels(){Note original=note().edit("Title","Body",2);Note result=LabelOperations.rename(Arrays.asList(original),"work","",3).get(0);assertEquals(Arrays.asList("Ideas"),result.labels);assertEquals("Body",result.body);assertEquals(2,original.labels.size());}
    @Test public void additiveBulkLabelsPreserveEachNotesExistingTags(){assertEquals(Arrays.asList("Ideas","Personal","Work"),LabelOperations.add(Arrays.asList("Ideas","Work"),Arrays.asList("work","Personal")));}
    @Test(expected=IllegalArgumentException.class) public void invalidRenameIsAtomic(){LabelOperations.rename(Arrays.asList(note()),"Work","Bad, label",3);}
    @Test(expected=IllegalArgumentException.class) public void blankShareRefused(){new IncomingText("Title","   ");}
    @Test public void unicodeIncomingTextIsNotStripped(){IncomingText share=new IncomingText(null,"  ಕನ್ನಡ\nनोट  ");assertEquals("",share.title);assertEquals("  ಕನ್ನಡ\nनोट  ",share.body);}
    @Test(expected=IllegalArgumentException.class) public void oversizeIPCRefused(){char[] chars=new char[100001];Arrays.fill(chars,'a');new IncomingText("",new String(chars));}
}
