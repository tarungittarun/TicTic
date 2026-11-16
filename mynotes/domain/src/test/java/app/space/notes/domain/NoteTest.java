package app.space.notes.domain;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NoteTest {
    @Test public void uniqueIds() { assertNotEquals(Note.create(1).id, Note.create(1).id); }
    @Test public void editPreservesIdAndCreation() {
        Note a=Note.create(10), b=a.edit("नोट", "ಕನ್ನಡ",20);
        assertEquals(a.id,b.id); assertEquals(10,b.createdAt); assertEquals(20,b.updatedAt);
        assertEquals("",a.title); assertEquals("नोट",b.title);
    }
    @Test public void backwardClockDoesNotRegressEditTime() {
        assertEquals(20,Note.create(20).edit("x","y",10).updatedAt);
    }
    @Test public void trashBoundary() {
        Note a=Note.create(10).state(false,false,100,100);
        assertFalse(a.expired(100+Note.TRASH_RETENTION_MS-1));
        assertTrue(a.expired(100+Note.TRASH_RETENTION_MS));
        assertFalse(a.expired(99));
    }
    @Test public void restoreDisarmsExpiry() {
        Note a=Note.create(10).state(true,true,100,100).state(true,true,0,200);
        assertFalse(a.expired(Long.MAX_VALUE)); assertTrue(a.archived); assertTrue(a.pinned);
    }
    @Test public void searchTrimsAndIgnoresCase() {
        Note a=Note.create(1).edit("Project","Meeting notes",2);
        assertTrue(a.matches(" MEETING ")); assertTrue(a.matches("project"));
        assertFalse(a.matches("missing")); assertTrue(a.matches(""));
    }
    @Test(expected=IllegalArgumentException.class) public void invalidColorRejected() {
        new Note("id","","",1,1,0,false,false,12);
    }
    @Test public void checklistTextConversionRoundTripsStateAndKeepsOrdinaryIndentation(){
        Note source=Note.create(1).list("List",java.util.Arrays.asList(new Note.Item("done","Checked",true,2),new Note.Item("todo","Pending",false,0)),false,2);
        Note text=source.convert(3);
        assertEquals("    [x] Checked\n[ ] Pending",text.body);
        assertTrue(text.plainText().contains("[x] Checked"));assertTrue(text.matches("checked"));
        Note restored=text.convert(4);assertTrue(restored.checklist);assertEquals(2,restored.items.size());
        assertTrue(restored.items.get(0).checked);assertEquals(2,restored.items.get(0).depth);assertEquals("Checked",restored.items.get(0).text);
        assertFalse(restored.items.get(1).checked);assertEquals(0,restored.items.get(1).depth);assertEquals("Pending",restored.items.get(1).text);
        Note plain=Note.create(5).edit("","  keep this indentation",6).convert(7);
        assertEquals("  keep this indentation",plain.items.get(0).text);assertEquals(0,plain.items.get(0).depth);assertFalse(plain.items.get(0).checked);
    }
    @Test public void checklistPlainTextPrefixesDriveSearchIndependentOfVisibilityAndAppearance(){
        Note legacy=Note.create(10).list("Legacy list",java.util.Arrays.asList(
            new Note.Item("legacy-done","LegacyDoneToken",true,1),
            new Note.Item("legacy-open","LegacyOpenToken",false,0)),true,11);
        assertEquals("  [x] LegacyDoneToken\n[ ] LegacyOpenToken\n",legacy.plainText());
        assertEquals(legacy.plainText(),legacy.list(legacy.title,legacy.items,false,12).plainText());
        assertTrue(legacy.matches("LegacyDoneToken"));assertTrue(legacy.matches("[x]"));
        assertTrue(app.space.notes.domain.SearchPreview.forNote(legacy,"LegacyDoneToken").text.contains("[x] LegacyDoneToken"));

        DocumentContent.Block list=DocumentContent.Block.checklist("canonical-list","Canonical tasks",java.util.Arrays.asList(
            new Note.Item("canonical-done","CanonicalDoneToken",true,1),
            new Note.Item("canonical-open","CanonicalOpenToken",false,2)),true,
            new ChecklistAppearance(ChecklistAppearance.Glyph.STAR,5));
        Note canonical=Note.create(12).withContent(new DocumentContent(java.util.Collections.singletonList(list)),13);
        assertEquals("Canonical tasks\n  [x] CanonicalDoneToken\n    [ ] CanonicalOpenToken",canonical.plainText());
        DocumentContent.Block shown=DocumentContent.Block.checklist(list.id,list.name,list.items,false,list.checklistAppearance);
        Note expanded=canonical.withContent(canonical.content.replace(list.id,shown),14);
        assertEquals(canonical.plainText(),expanded.plainText());assertFalse(canonical.plainText().contains("star"));
        assertTrue(canonical.matches("CanonicalDoneToken"));assertTrue(canonical.matches("[x]"));
        assertTrue(app.space.notes.domain.SearchPreview.forNote(canonical,"CanonicalDoneToken").text.contains("[x] CanonicalDoneToken"));
        assertFalse(canonical.matches("star"));
    }
    @Test public void changingChecklistDepthPreservesRowIdentityStatusAndRichMarks(){
        Note.Mark bold=new Note.Mark("bold",0,4);Note.Item row=new Note.Item("stable","Keep",true,0,java.util.Collections.singletonList(bold));
        Note.Item nested=row.change(row.text,row.checked,3);assertEquals("stable",nested.id);assertEquals("Keep",nested.text);assertTrue(nested.checked);assertEquals(3,nested.depth);assertEquals(1,nested.marks.size());assertEquals("bold",nested.marks.get(0).type);assertEquals(0,nested.marks.get(0).start);assertEquals(4,nested.marks.get(0).end);
        Note.Item root=nested.change(nested.text,nested.checked,0);assertEquals(0,root.depth);assertEquals("stable",root.id);assertTrue(root.checked);assertEquals("Keep",root.text);assertEquals("bold",root.marks.get(0).type);
    }
    @Test(expected=IllegalArgumentException.class) public void checklistDepthCannotBeNegative(){new Note.Item("row","Invalid",false,-1);}
    @Test(expected=IllegalArgumentException.class) public void checklistDepthCannotExceedThree(){new Note.Item("row","Invalid",false,4);}

}
