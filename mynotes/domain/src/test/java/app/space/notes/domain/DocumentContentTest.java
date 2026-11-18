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
    @Test public void terminalInsertedContentCanKeepAnEditableWritingTail(){
        DocumentContent empty=new DocumentContent(Collections.emptyList(),true,true);
        assertSame(empty,empty.ensureWritableTail());
        DocumentContent checklist=empty.insert(0,DocumentContent.Block.checklist("list",Collections.emptyList(),false));
        DocumentContent ready=checklist.ensureWritableTail();
        assertEquals(2,ready.blocks.size());assertEquals(DocumentContent.Kind.CHECKLIST,ready.blocks.get(0).kind);
        assertNotNull(ready.blocks.get(1).text);assertEquals("",ready.blocks.get(1).text.text);assertTrue(ready.writingLines);assertTrue(ready.notesWorkspace);
        assertSame(ready,ready.ensureWritableTail());
        assertEquals("",ready.ensureWritableTail().blocks.get(1).text.text);
        DocumentContent placeholder=new DocumentContent(Collections.singletonList(text("blank","")),true,true);
        DocumentContent inserted=placeholder.replaceEmptyTextWith("blank",DocumentContent.Block.checklist("new-list","Checklist01",Collections.emptyList(),false)).ensureWritableTail();
        assertEquals(DocumentContent.Kind.CHECKLIST,inserted.blocks.get(0).kind);assertNotEquals("blank",inserted.blocks.get(0).id);assertNotNull(inserted.blocks.get(1).text);assertTrue(inserted.writingLines);assertTrue(inserted.notesWorkspace);
    }
    @Test public void removingInsertedChecklistJoinsWritingAndOffsetsRichMarks(){
        RichText before=new RichText("Before ",Arrays.asList(new Note.Mark("bold",0,6),new Note.Mark("link",0,6,"https://example.com/before")));
        RichText after=new RichText("after",Arrays.asList(new Note.Mark("italic",0,5),new Note.Mark("number",0,5,"2")));
        DocumentContent.Block left=DocumentContent.Block.text("left",before),list=DocumentContent.Block.checklist("list",Arrays.asList(new Note.Item("row","Remove me",false,0)),false),right=DocumentContent.Block.text("right",after);
        DocumentContent source=new DocumentContent(Arrays.asList(left,list,right),true,true);
        DocumentContent removed=source.remove("list");
        assertEquals(1,removed.blocks.size());assertEquals("left",removed.blocks.get(0).id);assertEquals("Before after",removed.blocks.get(0).text.text);
        assertEquals(4,removed.blocks.get(0).text.marks.size());
        assertTrue(removed.blocks.get(0).text.marks.stream().anyMatch(mark->mark.type.equals("bold") && mark.start==0 && mark.end==6));
        assertTrue(removed.blocks.get(0).text.marks.stream().anyMatch(mark->mark.type.equals("link") && mark.start==0 && mark.end==6 && mark.value.equals("https://example.com/before")));
        assertTrue(removed.blocks.get(0).text.marks.stream().anyMatch(mark->mark.type.equals("italic") && mark.start==7 && mark.end==12));
        assertTrue(removed.blocks.get(0).text.marks.stream().anyMatch(mark->mark.type.equals("number") && mark.start==7 && mark.end==12 && mark.value.equals("2")));
        assertEquals("Before ",source.block("left").text.text);assertEquals("after",source.block("right").text.text);assertTrue(removed.writingLines);assertTrue(removed.notesWorkspace);
        List<DocumentContent.Block> inserted=Arrays.asList(
                DocumentContent.Block.visual("photo","Image01",DocumentContent.Kind.IMAGE,image("photo")),
                DocumentContent.Block.visual("drawing","Drawing01",DocumentContent.Kind.DRAWING,image("Drawing")),
                DocumentContent.Block.diagram("diagram","Diagram01",Diagram.empty().put(new Diagram.Node("node","process","Flow",40,40,120,80,0))),
                DocumentContent.Block.table("table","Table01",TextTable.empty(1,1)));
        for(DocumentContent.Block middle:inserted){
            DocumentContent all=new DocumentContent(Arrays.asList(text("before","A"),middle,text("after","B")),true,true).remove(middle.id);
            assertEquals("Deleting "+middle.kind+" joins adjacent writing sections",1,all.blocks.size());assertEquals("AB",all.blocks.get(0).text.text);assertTrue(all.writingLines);assertTrue(all.notesWorkspace);
        }
    }
    @Test public void removingImageOrDrawingThroughLegacyImageProjectionJoinsWriting(){
        for(DocumentContent.Kind kind:Arrays.asList(DocumentContent.Kind.IMAGE,DocumentContent.Kind.DRAWING)){
            NoteImage attachment=image(kind==DocumentContent.Kind.DRAWING?"Drawing":"Photo");
            DocumentContent.Block visual=DocumentContent.Block.visual("visual","Visual01",kind,attachment);
            DocumentContent source=new DocumentContent(Arrays.asList(text("before","left"),visual,text("after","right")),true,true);
            DocumentContent removed=source.withImages(Collections.emptyList());
            assertEquals("Removing "+kind+" through Note.images rejoins writing",1,removed.blocks.size());
            assertEquals("leftright",removed.blocks.get(0).text.text);assertTrue(removed.writingLines);assertTrue(removed.notesWorkspace);
        }
    }
    @Test public void movingChecklistDoesNotJoinOrInvalidateItsAdjacentWritingTargets(){
        DocumentContent.Block left=text("left","left"),list=DocumentContent.Block.checklist("list",Collections.singletonList(new Note.Item("row","Move me",false,0)),false),right=text("right","right");
        DocumentContent source=new DocumentContent(Arrays.asList(left,list,right));
        DocumentContent detached=source.removeForMove("list");assertEquals(2,detached.blocks.size());assertEquals("left",detached.block("left").text.text);assertEquals("right",detached.block("right").text.text);
        DocumentContent moved=source.moveChecklistIntoText("list","right",5);assertEquals(3,moved.blocks.size());assertEquals("right",moved.block("right").text.text);assertEquals(DocumentContent.Kind.CHECKLIST,moved.blocks.get(2).kind);
    }
    @Test public void checklistRemovalAtEdgesKeepsTheNearestOrdinaryWritingArea(){
        DocumentContent.Block list=DocumentContent.Block.checklist("list",Collections.emptyList(),false),writing=text("writing","Keep typing");
        DocumentContent beginning=new DocumentContent(Arrays.asList(list,writing)).remove("list");assertEquals(1,beginning.blocks.size());assertEquals("writing",beginning.blocks.get(0).id);
        DocumentContent ending=new DocumentContent(Arrays.asList(writing,list)).remove("list");assertEquals(1,ending.blocks.size());assertEquals("writing",ending.blocks.get(0).id);assertSame(ending,ending.ensureWritableTail());
        DocumentContent only=new DocumentContent(Collections.singletonList(list)).remove("list");assertTrue(only.blocks.isEmpty());
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
    @Test public void checkedVisibilityPreferenceIsPerChecklistAndDoesNotChangeRows(){
        Note.Item done=new Note.Item("done","Milk",true,1,Arrays.asList(new Note.Mark("bold",0,4))),pending=new Note.Item("pending","Bread",false,0);
        DocumentContent.Block first=DocumentContent.Block.checklist("first","Shopping",Arrays.asList(done,pending),true),second=DocumentContent.Block.checklist("second","Packing",Collections.singletonList(new Note.Item("other","Passport",true,0)),false);
        DocumentContent source=new DocumentContent(Arrays.asList(first,second));DocumentContent expanded=source.replace("first",DocumentContent.Block.checklist("first",first.name,first.items,false));
        assertTrue(source.block("first").hideChecked);assertFalse(source.block("second").hideChecked);assertFalse(expanded.block("first").hideChecked);assertFalse(expanded.block("second").hideChecked);
        assertSame(done,expanded.block("first").items.get(0));assertSame(pending,expanded.block("first").items.get(1));assertTrue(expanded.block("first").items.get(0).checked);assertEquals(1,expanded.block("first").items.get(0).depth);assertEquals("bold",expanded.block("first").items.get(0).marks.get(0).type);
        DocumentContent collapsedAgain=expanded.replace("first",DocumentContent.Block.checklist("first",first.name,first.items,true));assertTrue(collapsedAgain.block("first").hideChecked);assertFalse(collapsedAgain.block("second").hideChecked);assertEquals(source.block("first").items,collapsedAgain.block("first").items);
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
    @Test public void checklistTextRoundTripRetainsMarkersDepthAndRowMarks(){
        Note.Item checked=new Note.Item("checked","Buy milk",true,1,Arrays.asList(new Note.Mark("bold",0,3)));
        Note.Item pending=new Note.Item("pending","Bread",false,0,Arrays.asList(new Note.Mark("italic",0,5)));
        DocumentContent source=new DocumentContent(Arrays.asList(DocumentContent.Block.checklist("list","Shopping",Arrays.asList(checked,pending),false)));
        DocumentContent converted=source.checklistToText("list");RichText plain=converted.block("list").text;
        assertEquals("Shopping\n  [x] Buy milk\n[ ] Bread",plain.text);
        assertTrue(plain.marks.stream().anyMatch(mark->mark.type.equals("bold") && plain.text.substring(mark.start,mark.end).equals("Buy")));
        assertTrue(plain.marks.stream().anyMatch(mark->mark.type.equals("italic") && plain.text.substring(mark.start,mark.end).equals("Bread")));
        int rowsStart=plain.text.indexOf('\n')+1;
        DocumentContent restored=converted.selectionToChecklist("list",rowsStart,plain.text.length());
        DocumentContent.Block list=null;for(DocumentContent.Block block:restored.blocks)if(block.kind==DocumentContent.Kind.CHECKLIST)list=block;
        assertNotNull(list);assertEquals(2,list.items.size());
        assertEquals("Buy milk",list.items.get(0).text);assertTrue(list.items.get(0).checked);assertEquals(1,list.items.get(0).depth);
        assertEquals("bold",list.items.get(0).marks.get(0).type);assertEquals(0,list.items.get(0).marks.get(0).start);assertEquals(3,list.items.get(0).marks.get(0).end);
        assertEquals("Bread",list.items.get(1).text);assertFalse(list.items.get(1).checked);assertEquals(0,list.items.get(1).depth);assertEquals("italic",list.items.get(1).marks.get(0).type);
    }
    @Test public void convertingSelectedRowsPreservesSurroundingTextAndOnlyParsesExplicitMarkers(){
        String sourceText="Prefix 😀\n  [x] checked\n  ordinary indentation\nSuffix";
        List<Note.Mark> marks=Arrays.asList(new Note.Mark("bold",0,sourceText.length()),new Note.Mark("italic",sourceText.indexOf("checked"),sourceText.indexOf("checked")+7));
        DocumentContent original=new DocumentContent(Arrays.asList(DocumentContent.Block.text("writing",new RichText(sourceText,marks))));
        int start=sourceText.indexOf("  [x]"),end=sourceText.indexOf("\nSuffix");DocumentContent converted=original.selectionToChecklist("writing",start,end);
        assertEquals("Prefix 😀\n",converted.blocks.get(0).text.text);assertEquals("\nSuffix",converted.blocks.get(2).text.text);
        DocumentContent.Block list=converted.blocks.get(1);assertTrue(list.items.get(0).checked);assertEquals("checked",list.items.get(0).text);assertEquals(1,list.items.get(0).depth);
        assertEquals("  ordinary indentation",list.items.get(1).text);assertFalse(list.items.get(1).checked);assertEquals(0,list.items.get(1).depth);
        assertTrue(list.items.get(0).marks.stream().anyMatch(mark->mark.type.equals("italic") && mark.start==0 && mark.end==7));
        assertEquals("Prefix 😀\n  [x] checked\n  ordinary indentation\nSuffix",original.blocks.get(0).text.text);
    }

    @Test public void checklistAppearanceIsPerListAndPlainTextIndependent(){
        Note.Item milk=new Note.Item("milk","Milk",true,1,Arrays.asList(new Note.Mark("bold",0,4))),bread=new Note.Item("bread","Bread",false,0);
        DocumentContent.Block shopping=DocumentContent.Block.checklist("shopping","Shopping",Arrays.asList(milk,bread),true);
        DocumentContent.Block packing=DocumentContent.Block.checklist("packing","Packing",Collections.singletonList(new Note.Item("passport","Passport",true,0)),false);
        DocumentContent original=new DocumentContent(Arrays.asList(shopping,packing));
        ChecklistAppearance starTeal=new ChecklistAppearance(ChecklistAppearance.Glyph.STAR,5);
        DocumentContent styled=original.replace("shopping",shopping.withChecklistAppearance(starTeal));
        assertEquals("Shopping\n  [x] Milk\n[ ] Bread\nPacking\n[x] Passport",styled.plainText());
        assertEquals(original.plainText(),styled.plainText());assertNotEquals(original.fingerprint(),styled.fingerprint());
        assertTrue(original.block("shopping").checklistAppearance.isDefault());assertEquals(starTeal,styled.block("shopping").checklistAppearance);
        assertTrue(styled.block("packing").checklistAppearance.isDefault());assertTrue(styled.block("shopping").hideChecked);
        DocumentContent.Block edited=styled.block("shopping").withChecklist(Arrays.asList(milk),false).renamed("Groceries");
        assertEquals(starTeal,edited.checklistAppearance);assertEquals("milk",edited.items.get(0).id);assertEquals("Groceries",edited.name);
        DocumentContent.Block copied=ContentCopies.copy(styled.block("shopping"));assertEquals(starTeal,copied.checklistAppearance);assertNotEquals("shopping",copied.id);assertNotEquals("milk",copied.items.get(0).id);
        assertEquals(12,ChecklistAppearance.THEME_COLOR_COUNT);assertEquals(11,ChecklistAppearance.NAMED_THEME_COLOR_COUNT);assertEquals(ChecklistAppearance.THEME_COLOR_COUNT,ChecklistAppearance.DEFAULT_THEME_COLOR);
        for(int color=1;color<=ChecklistAppearance.NAMED_THEME_COLOR_COUNT;color++)assertNotNull(new ChecklistAppearance(ChecklistAppearance.Glyph.DOT,color));
        assertThrows(IllegalArgumentException.class,()->new ChecklistAppearance(ChecklistAppearance.Glyph.DOT,0));assertThrows(IllegalArgumentException.class,()->new ChecklistAppearance(ChecklistAppearance.Glyph.DOT,ChecklistAppearance.THEME_COLOR_COUNT+1));
        assertThrows(IllegalArgumentException.class,()->DocumentContent.Block.checklist("bad",Collections.emptyList(),false).withChecklistAppearance(null));
    }
}
