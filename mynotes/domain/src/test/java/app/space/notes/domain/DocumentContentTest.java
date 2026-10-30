package app.space.notes.domain;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class DocumentContentTest {
    private NoteImage image(String name){return new NoteImage(UUID.randomUUID().toString(),name,"image/png",1,1,"iVBORw0KGgo=");}
    private DocumentContent.Block text(String id,String text){return DocumentContent.Block.text(id,new RichText(text,Collections.emptyList()));}
    @Test public void textCanSurroundVisualsAndMovesPreserveObjects(){
        DocumentContent.Block a=text("a","before"),b=DocumentContent.Block.visual("b","Image01",DocumentContent.Kind.IMAGE,image("legacy")),c=text("c","after");
        DocumentContent original=new DocumentContent(Arrays.asList(a,b,c));
        DocumentContent moved=original.move("b",3);assertSame(c,moved.blocks.get(1));assertSame(b,moved.blocks.get(2));
        assertSame(b,original.blocks.get(1));assertSame(original,original.move("b",2));
        assertSame(original,original.move("b",1));assertSame(a,moved.move("b",0).blocks.get(1));
        assertEquals("between",original.insert(2,text("middle","between")).blocks.get(2).text.text);
    }
    @Test public void visualRenameCannotChangeItsKindOrPixels(){
        NoteImage image=image("Drawing");DocumentContent.Block drawing=DocumentContent.Block.visual("ink","Drawing01",DocumentContent.Kind.DRAWING,image);
        DocumentContent.Block renamed=drawing.renamed("My sketch");
        assertEquals(DocumentContent.Kind.DRAWING,renamed.kind);assertEquals("ink",renamed.id);assertSame(image,renamed.image);
        assertEquals("Drawing",image.name);assertEquals("Drawing01",drawing.name);
        assertThrows(IllegalArgumentException.class,()->text("t","x").renamed("invalid"));
    }
    @Test public void deterministicLegacyProjectionPreservesRowsAndMediaWithoutMutatingSource(){
        NoteImage image=image("Drawing");Note source=new Note("note","title","text",1,2,0,true,false,2,false,false,Collections.emptyList(),Arrays.asList(new Note.Mark("bold",0,4)),Arrays.asList("label"),Diagram.empty(),Collections.emptyList(),0,"",3,Arrays.asList(image));
        DocumentContent first=DocumentContent.fromLegacy(source),second=DocumentContent.fromLegacy(source);
        assertEquals(2,first.blocks.size());assertEquals(first.blocks.get(0).id,second.blocks.get(0).id);
        assertEquals(first.blocks.get(1).id,second.blocks.get(1).id);assertSame(image,first.blocks.get(1).image);
        assertEquals(DocumentContent.Kind.DRAWING,first.blocks.get(1).kind);assertEquals("text",first.blocks.get(0).text.text);
        assertSame(source.marks.get(0),first.blocks.get(0).text.marks.get(0));assertEquals("text",source.body);assertTrue(source.pinned);assertEquals(3,source.background);
        Note.Item item=new Note.Item("row","checked",true,2);
        Note list=new Note("list","title","",1,2,0,false,false,0,true,true,Arrays.asList(item),Collections.emptyList(),Collections.emptyList());
        DocumentContent projection=DocumentContent.fromLegacy(list);assertSame(item,projection.blocks.get(0).items.get(0));assertTrue(projection.blocks.get(0).hideChecked);
        assertNotEquals(first.blocks.get(0).id,projection.blocks.get(0).id);
    }
    @Test public void defaultsDoNotRenameExistingVisualsAndUseSeparateKindCounters(){
        DocumentContent document=new DocumentContent(Arrays.asList(DocumentContent.Block.visual("a","Image01",DocumentContent.Kind.IMAGE,image("original"))));
        assertEquals("Image02",document.nextVisualName(DocumentContent.Kind.IMAGE));
        assertEquals("Drawing01",document.nextVisualName(DocumentContent.Kind.DRAWING));
        assertEquals("Diagram01",document.nextVisualName(DocumentContent.Kind.DIAGRAM));
        assertThrows(IllegalArgumentException.class,()->document.nextVisualName(DocumentContent.Kind.TEXT));
    }
    @Test public void invalidAndStaleOperationsCannotMutateOriginal(){
        DocumentContent.Block a=text("a","one");DocumentContent document=new DocumentContent(Arrays.asList(a));
        assertThrows(IllegalArgumentException.class,()->document.insert(0,a));
        assertThrows(IllegalArgumentException.class,()->document.insert(2,text("b","two")));
        assertThrows(IllegalArgumentException.class,()->document.move("missing",0));
        assertThrows(IllegalArgumentException.class,()->document.move("a",-1));
        assertThrows(IllegalArgumentException.class,()->document.replace("a",text("b","two")));
        assertSame(document,document.replace("a",a));assertSame(a,document.blocks.get(0));
        assertThrows(UnsupportedOperationException.class,()->document.blocks.clear());
    }
    @Test public void duplicateReferencesAndInvalidPayloadsAreRejected(){
        NoteImage image=image("original");
        assertThrows(IllegalArgumentException.class,()->new DocumentContent(Arrays.asList(DocumentContent.Block.visual("a","A",DocumentContent.Kind.IMAGE,image),DocumentContent.Block.visual("b","B",DocumentContent.Kind.IMAGE,image))));
        assertThrows(IllegalArgumentException.class,()->DocumentContent.Block.visual("x","X",DocumentContent.Kind.TEXT,image));
        assertThrows(IllegalArgumentException.class,()->DocumentContent.Block.text("x",null));
        assertThrows(IllegalArgumentException.class,()->DocumentContent.Block.table("x","X",null));
        assertThrows(IllegalArgumentException.class,()->DocumentContent.Block.diagram("x","X",null));
        assertThrows(IllegalArgumentException.class,()->new DocumentContent(Collections.nCopies(DocumentContent.MAX_BLOCKS+1,text("a","x"))));
    }
    @Test public void mixedTableAndChecklistSnapshotsAreImmutable(){
        TextTable table=TextTable.empty(2,2).withCell(0,0,"retained");
        DocumentContent.Block list=DocumentContent.Block.checklist("l",Arrays.asList(new Note.Item("r","row",true,1)),false);
        DocumentContent content=new DocumentContent(Arrays.asList(text("a","intro"),DocumentContent.Block.table("t","Table01",table),list,text("z","end")));
        assertSame(table,content.blocks.get(1).table);assertTrue(content.blocks.get(2).items.get(0).checked);
        assertThrows(UnsupportedOperationException.class,()->list.items.clear());
        assertEquals("intro",content.blocks.get(0).text.text);assertEquals("end",content.blocks.get(3).text.text);
    }

    @Test public void checklistNamesPreserveIdentityPayloadAndFingerprint(){
        Note.Item row=new Note.Item("row","Checked",true,2,Arrays.asList(new Note.Mark("bold",0,7)));
        DocumentContent.Block list=DocumentContent.Block.checklist("list",Arrays.asList(row),true).renamed("Shopping");
        assertEquals("Shopping",list.name);assertSame(row,list.items.get(0));assertTrue(list.hideChecked);
        DocumentContent document=new DocumentContent(Arrays.asList(list));assertTrue(document.plainText().contains("Shopping"));assertEquals("Checklist01",document.nextVisualName(DocumentContent.Kind.CHECKLIST));
        assertNotEquals(document.fingerprint(),document.replace("list",list.renamed("Work")).fingerprint());
        assertThrows(IllegalArgumentException.class,()->list.renamed(" "));assertThrows(IllegalArgumentException.class,()->list.renamed(String.join("",Collections.nCopies(201,"x"))));
    }
    @Test public void checklistMoveInsideRichTextPreservesSlicesAndSource(){
        DocumentContent.Block list=DocumentContent.Block.checklist("list","Plan",Arrays.asList(new Note.Item("row","Keep",true,1)),false);
        DocumentContent.Block writing=DocumentContent.Block.text("text",new RichText("One\nTwo\nThree",Arrays.asList(new Note.Mark("italic",0,13))));
        DocumentContent original=new DocumentContent(Arrays.asList(list,writing),true),moved=original.moveChecklistIntoText("list","text",4);
        assertEquals(3,moved.blocks.size());assertEquals("One\n",moved.blocks.get(0).text.text);assertSame(list,moved.blocks.get(1));assertEquals("Two\nThree",moved.blocks.get(2).text.text);assertEquals(4,moved.blocks.get(0).text.marks.get(0).end);assertEquals(9,moved.blocks.get(2).text.marks.get(0).end);assertTrue(moved.writingLines);assertSame(list,original.blocks.get(0));
    }
    @Test public void checklistMoveAtEdgesDoesNotCreateEmptyTextOrDuplicateRows(){
        DocumentContent.Block list=DocumentContent.Block.checklist("list","Todo",Arrays.asList(new Note.Item("row","Keep",false,0)),false);
        DocumentContent original=new DocumentContent(Arrays.asList(text("text","abc"),list));
        DocumentContent first=original.moveChecklistIntoText("list","text",0);assertEquals(2,first.blocks.size());assertSame(list,first.blocks.get(0));
        DocumentContent last=first.moveChecklistIntoText("list","text",3);assertEquals(2,last.blocks.size());assertSame(list,last.blocks.get(1));
    }
    @Test public void checklistMoveRejectsStaleOffsetsAndSplitSurrogatesAtomically(){
        DocumentContent.Block list=DocumentContent.Block.checklist("list",Collections.emptyList(),false);DocumentContent original=new DocumentContent(Arrays.asList(list,text("text","😀a")));
        assertThrows(IllegalArgumentException.class,()->original.moveChecklistIntoText("list","text",1));assertThrows(IllegalArgumentException.class,()->original.moveChecklistIntoText("list","text",4));assertThrows(IllegalArgumentException.class,()->original.moveChecklistIntoText("missing","text",0));assertThrows(IllegalArgumentException.class,()->original.moveChecklistIntoText("text","list",0));assertEquals(2,original.blocks.size());assertSame(list,original.blocks.get(0));
    }
}
