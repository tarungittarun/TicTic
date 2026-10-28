package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class StructuredNoteTest {
    @Test public void checklistConversionPreservesLines(){
        Note n=Note.create(1).edit("List","Milk\nCoffee",2).convert(3);
        assertTrue(n.checklist);assertEquals(2,n.items.size());assertEquals("Coffee",n.items.get(1).text);
    }
    @Test public void checkedIndentedListSearchesAndConverts(){
        Note n=Note.create(1).list("Todo",Arrays.asList(new Note.Item("a","Buy milk",true,2)),true,2);
        assertTrue(n.matches("MILK"));assertTrue(n.plainText().contains("    [x] Buy milk"));
        assertFalse(n.convert(3).checklist);assertTrue(n.convert(3).body.contains("Buy milk"));
    }
    @Test public void labelsNormalizeAndSearch(){
        Note n=Note.create(1).properties(3,Arrays.asList(" Work ","work","Ideas"),2);
        assertEquals(2,n.labels.size());assertTrue(n.matches("ideas"));assertEquals(3,n.color);
    }
    @Test public void formattingPreservedOnMetadataChange(){
        Note n=Note.create(1).rich("t","hello",Arrays.asList(new Note.Mark("bold",0,5)),2);
        assertEquals(1,n.state(true,true,10,10).properties(4,Arrays.asList("tag"),10).marks.size());
    }
    @Test public void partialClearPreservesOutsideRanges(){
        List<Note.Mark> result=TextFormatting.apply(Arrays.asList(new Note.Mark("bold",0,10)),"clear",3,7);
        assertEquals(2,result.size());assertEquals(3,result.get(0).end);assertEquals(7,result.get(1).start);
    }
    @Test public void toggleCoveredRangeOff(){
        assertTrue(TextFormatting.apply(Arrays.asList(new Note.Mark("bold",0,5)),"bold",0,5).isEmpty());
    }
    @Test public void headingReplacesOtherHeading(){
        List<Note.Mark> marks=TextFormatting.apply(Arrays.asList(new Note.Mark("h1",0,5)),"h2",0,5);
        assertEquals(1,marks.size());assertEquals("h2",marks.get(0).type);
    }
    @Test public void restoreConflictIncludesStructuredContent(){
        Note original=Note.create(1).edit("t","same",2);
        Note changed=original.rich("t","same",Arrays.asList(new Note.Mark("bold",0,4)),2);
        NoteMerge.Plan plan=NoteMerge.preview(Arrays.asList(original),Arrays.asList(changed));
        assertEquals(1,plan.conflicts);assertEquals(1,plan.notes.get(1).marks.size());
        assertEquals(0,NoteMerge.preview(plan.notes,Arrays.asList(changed)).added);
    }
    @Test public void duplicateHasNewCreationTimeAndKeepsRows(){
        Note original=Note.create(1).convert(2);Note copy=NoteOperations.duplicate(original,100);
        assertNotEquals(original.id,copy.id);assertEquals(100,copy.createdAt);assertTrue(copy.checklist);
        assertEquals(original.items.get(0).id,copy.items.get(0).id);
    }
    @Test public void mergeKeepsSourceTextAndLabels(){
        Note a=Note.create(1).edit("A","first",2).properties(2,Arrays.asList("Work"),2);
        Note b=Note.create(1).edit("B","second",2).convert(3);
        Note merged=NoteOperations.merge(Arrays.asList(a,b),100);
        assertTrue(merged.body.contains("A\nfirst"));assertTrue(merged.body.contains("[ ] second"));assertTrue(merged.labels.contains("Work"));
        assertEquals(0,a.deletedAt);assertEquals(0,b.deletedAt);
    }
    @Test(expected=IllegalArgumentException.class) public void outOfBoundsFormattingRejected(){Note.create(1).rich("","a",Arrays.asList(new Note.Mark("bold",0,2)),2);}
    @Test(expected=IllegalArgumentException.class) public void invalidIndentRejected(){new Note.Item("id","text",false,4);}
    @Test(expected=IllegalArgumentException.class) public void duplicateRowIdRejected(){Note.create(1).list("",Arrays.asList(new Note.Item("a","one",false,0),new Note.Item("a","two",false,0)),false,2);}
    @Test public void immutableCollectionsDefendAgainstCallerMutation(){
        List<String> labels=new ArrayList<>(Arrays.asList("safe"));Note n=Note.create(1).properties(0,labels,2);labels.clear();assertEquals(1,n.labels.size());
    }
    @Test public void legacyConflictIdentityRemainsStable(){
        Note old=new Note("id","title","old",1,2,0,false,false,0);
        Note incoming=new Note("id","title","incoming",1,2,0,false,false,0);
        assertEquals("b1b8eb11-9e32-3b22-8053-959de414bead",NoteMerge.preview(Arrays.asList(old),Arrays.asList(incoming)).notes.get(1).id);
    }
    @Test public void headingSelectionStylesWholeParagraphNotNeighbours(){
        String text="before\nselected line\nafter";List<Note.Mark> marks=TextFormatting.paragraph(Collections.emptyList(),text,"h1",9,13);
        assertEquals(1,marks.size());assertEquals(7,marks.get(0).start);assertEquals(20,marks.get(0).end);
    }
    @Test public void headingAtCaretAndNormalTextKeepInlineFormatting(){
        String text="first\nsecond";List<Note.Mark> marks=TextFormatting.paragraph(Arrays.asList(new Note.Mark("bold",6,12)),text,"h2",8,8);
        assertEquals(2,marks.size());marks=TextFormatting.paragraph(marks,text,"text",7,7);assertEquals(1,marks.size());assertEquals("bold",marks.get(0).type);
    }
    @Test public void paragraphSelectionExcludesNextLineAtBoundary(){
        List<Note.Mark> marks=TextFormatting.paragraph(Collections.emptyList(),"one\ntwo\nthree","h1",0,4);assertEquals(1,marks.size());assertEquals(3,marks.get(0).end);
    }
    @Test public void oldPartialHeadingsNormalizeToParagraphsAndDoNotStack(){
        List<Note.Mark> marks=TextFormatting.normalizeParagraphs(Arrays.asList(new Note.Mark("h1",1,2),new Note.Mark("h2",2,3)),"hello\nworld");assertEquals(1,marks.size());assertEquals("h2",marks.get(0).type);assertEquals(0,marks.get(0).start);assertEquals(5,marks.get(0).end);
    }
    @Test public void paragraphHeadingTogglesAndSupportsUnicode(){
        String text="ಕನ್ನಡ 😀\nನೋಟು";List<Note.Mark> m=TextFormatting.paragraph(Collections.emptyList(),text,"h1",1,1);assertEquals(text.indexOf('\n'),m.get(0).end);assertTrue(TextFormatting.paragraph(m,text,"h1",1,1).isEmpty());
    }
    @Test public void blankParagraphDoesNotCreateInvalidMark(){assertTrue(TextFormatting.paragraph(Collections.emptyList(),"a\n\nb","h1",2,2).isEmpty());}
}
