package app.space.notes.domain;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChecklistEditingTest {
    @Test public void enterSplitsAtCaretAndPreservesRowDataAndOrder(){
        Note.Item source=new Note.Item("source","BeforeAfter",true,2,Arrays.asList(new Note.Mark("bold",0,11),new Note.Mark("italic",6,11)));
        Note.Item sibling=new Note.Item("sibling","Keep",false,0);
        ChecklistEditing.Split split=ChecklistEditing.afterEnter(Arrays.asList(source,sibling),"source",6,6);
        assertEquals(3,split.rows.size());Note.Item prefix=split.rows.get(0),suffix=split.rows.get(1);
        assertEquals("source",prefix.id);assertEquals("Before",prefix.text);assertTrue(prefix.checked);assertEquals(2,prefix.depth);
        assertEquals(1,prefix.marks.size());assertEquals("bold",prefix.marks.get(0).type);assertEquals(0,prefix.marks.get(0).start);assertEquals(6,prefix.marks.get(0).end);
        assertEquals(split.insertedId,suffix.id);assertNotEquals("source",suffix.id);assertEquals("After",suffix.text);assertFalse(suffix.checked);assertEquals(2,suffix.depth);
        assertEquals(2,suffix.marks.size());assertEquals("bold",suffix.marks.get(0).type);assertEquals(0,suffix.marks.get(0).start);assertEquals(5,suffix.marks.get(0).end);assertEquals("italic",suffix.marks.get(1).type);assertEquals(0,suffix.marks.get(1).start);assertEquals(5,suffix.marks.get(1).end);
        assertSame(sibling,split.rows.get(2));assertEquals("BeforeAfter",source.text);assertThrows(UnsupportedOperationException.class,()->split.rows.clear());
    }

    @Test public void enterReplacesSelectionAndClipsMarksAtBothEdges(){
        Note.Item source=new Note.Item("source","abcdef",false,1,Collections.singletonList(new Note.Mark("underline",0,6)));
        ChecklistEditing.Split split=ChecklistEditing.afterEnter(Collections.singletonList(source),"source",2,4);
        assertEquals("ab",split.rows.get(0).text);assertEquals("ef",split.rows.get(1).text);
        assertEquals(2,split.rows.get(0).marks.get(0).end);assertEquals(0,split.rows.get(1).marks.get(0).start);assertEquals(2,split.rows.get(1).marks.get(0).end);
    }

    @Test public void removeRowDoesNotMutateSiblingsAndUncheckAllPreservesRowData(){
        Note.Item done=new Note.Item("done","Milk",true,2,Collections.singletonList(new Note.Mark("bold",0,4)));
        Note.Item pending=new Note.Item("pending","Bread",false,1);
        List<Note.Item> original=Arrays.asList(done,pending);
        List<Note.Item> removed=ChecklistEditing.removeRow(original,"done");
        assertEquals(1,removed.size());assertSame(pending,removed.get(0));assertSame(done,original.get(0));
        List<Note.Item> unchecked=ChecklistEditing.uncheckAll(original);
        assertNotSame(original,unchecked);assertFalse(unchecked.get(0).checked);assertFalse(unchecked.get(1).checked);
        assertEquals("done",unchecked.get(0).id);assertEquals("Milk",unchecked.get(0).text);assertEquals(2,unchecked.get(0).depth);assertEquals("bold",unchecked.get(0).marks.get(0).type);
        assertSame(unchecked,ChecklistEditing.uncheckAll(unchecked));
        assertThrows(IllegalArgumentException.class,()->ChecklistEditing.removeRow(original,"missing"));
    }

    @Test public void trailingBlankRowsAreRemovedButInteriorBlankRowsRemain(){
        Note.Item interior=new Note.Item("interior","",false,0),text=new Note.Item("text","Keep",false,0),blank=new Note.Item("blank","  \t",false,0);
        List<Note.Item> result=ChecklistEditing.withoutTrailingBlankRows(Arrays.asList(interior,text,blank,new Note.Item("last","",false,0)));
        assertEquals(2,result.size());assertSame(interior,result.get(0));assertSame(text,result.get(1));assertSame(result,ChecklistEditing.withoutTrailingBlankRows(result));
    }

    @Test public void canonicalCleanupChangesOnlyChecklistRowsAndPreservesAppearanceAndText(){
        Note.Item interior=new Note.Item("interior","",false,0),keep=new Note.Item("keep","Buy",true,1,Collections.singletonList(new Note.Mark("bold",0,3))),blank=new Note.Item("blank"," \t",false,2);
        ChecklistAppearance appearance=ChecklistAppearance.of("square",3);
        DocumentContent.Block list=DocumentContent.Block.checklist("list","Tasks",Arrays.asList(interior,keep,blank),true,appearance);
        DocumentContent.Block prose=DocumentContent.Block.text("prose",new RichText("Untouched",Collections.emptyList()));
        DocumentContent before=new DocumentContent(Arrays.asList(list,prose),true,true),after=ChecklistEditing.withoutTrailingBlankRows(before);
        assertNotSame(before,after);assertEquals(2,after.blocks.size());assertEquals("prose",after.blocks.get(1).id);assertEquals("Untouched",after.blocks.get(1).text.text);assertTrue(after.writingLines);assertTrue(after.notesWorkspace);
        DocumentContent.Block trimmed=after.block("list");assertEquals("Tasks",trimmed.name);assertTrue(trimmed.hideChecked);assertEquals(appearance,trimmed.checklistAppearance);assertEquals(2,trimmed.items.size());assertSame(interior,trimmed.items.get(0));assertSame(keep,trimmed.items.get(1));
        assertSame(after,ChecklistEditing.withoutTrailingBlankRows(after));DocumentContent alreadyClean=before.replace("list",DocumentContent.Block.checklist("list","Tasks",Arrays.asList(interior,keep),true,appearance));assertSame(alreadyClean,ChecklistEditing.withoutTrailingBlankRows(alreadyClean));
    }

    @Test(expected=IllegalArgumentException.class) public void enterRejectsStaleRow(){ChecklistEditing.afterEnter(Collections.emptyList(),"missing",0,0);}
    @Test(expected=IllegalArgumentException.class) public void enterRejectsInvalidSelection(){ChecklistEditing.afterEnter(Collections.singletonList(new Note.Item("source","abc",false,0)),"source",3,2);}
}
