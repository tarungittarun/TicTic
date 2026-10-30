package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class CanonicalNoteTest {
    private Note sample(){Note old=Note.create(1).rich("title","before selected after",Arrays.asList(new Note.Mark("bold",0,21)),2);return old.withContent(DocumentContent.fromLegacy(old),3);}
    @Test public void metadataMutationsNeverFlattenCanonicalContent(){
        Note note=sample();DocumentContent content=note.content;
        assertSame(content,note.properties(2,Arrays.asList("work"),4).content);
        assertSame(content,note.state(true,true,5,5).content);
        assertSame(content,note.folder("Work",5).content);
        assertSame(content,note.background(2,5).content);
        assertSame(content,note.reminder(100,5).content);
        assertSame(content,note.withId("copy").content);
        assertSame(content,note.voiceSource(UUID.randomUUID().toString(),5).content);
        assertSame(content,note.title("renamed",5).content);
        assertSame(content,NoteOperations.duplicate(note,5).content);
    }
    @Test public void legacyEditableProjectionCannotOverwriteCanonicalDocument(){
        Note note=sample();
        assertThrows(IllegalStateException.class,()->note.rich("title","lost",Collections.emptyList(),4));
        assertThrows(IllegalStateException.class,()->note.edit("title","lost",4));
        assertThrows(IllegalStateException.class,()->note.list("title",Collections.emptyList(),false,4));
        assertThrows(IllegalStateException.class,()->note.convert(4));assertEquals("before selected after",note.content.blocks.get(0).text.text);
    }
    @Test public void selectedConversionRetainsOutsideFormattingAndRichRows(){
        Note note=sample();String id=note.content.blocks.get(0).id;
        DocumentContent converted=note.content.selectionToChecklist(id,7,15);
        assertEquals("before ",converted.blocks.get(0).text.text);assertEquals("selected",converted.blocks.get(1).items.get(0).text);assertEquals(" after",converted.blocks.get(2).text.text);
        assertEquals("bold",converted.blocks.get(1).items.get(0).marks.get(0).type);assertEquals(8,converted.blocks.get(1).items.get(0).marks.get(0).end);
        assertEquals(7,converted.blocks.get(0).text.marks.get(0).end);assertEquals(6,converted.blocks.get(2).text.marks.get(0).end);
        assertEquals(1,note.content.blocks.size());
    }
    @Test public void mergeAndSearchIncludeTableAndBlockOrder(){
        Note note=sample();DocumentContent content=note.content.insert(1,DocumentContent.Block.table("table","Accounts",TextTable.empty(1,1).withCell(0,0,"unique cell")));
        Note table=note.withContent(content,3);assertTrue(table.matches("unique cell"));assertFalse(NoteMerge.same(note,table));
        Note reordered=table.withContent(content.move("table",0),3);assertFalse(NoteMerge.same(table,reordered));
        NoteMerge.Plan merge=NoteMerge.preview(Arrays.asList(table),Arrays.asList(reordered));assertEquals(1,merge.conflicts);assertNotNull(merge.notes.get(1).content);
    }
    @Test public void linkAndFontRangesAreValidatedAndSurviveSlicing(){
        RichText rich=new RichText("click here",Arrays.asList(new Note.Mark("link",0,10,"https://example.com/path"),new Note.Mark("font",0,10,"serif"),new Note.Mark("size",0,10,"24")));
        assertEquals("https://example.com/path",rich.slice(2,8).marks.get(0).value);
        assertThrows(IllegalArgumentException.class,()->new Note.Mark("size",0,1,"500"));
        assertThrows(IllegalArgumentException.class,()->new Note.Mark("font",0,1,"/sdcard/font.ttf"));
        for(String link:Arrays.asList("javascript:alert(1)","file:///data/a","content://private/a","intent://a","https://user:pass@example.com","https://example.com\n","https://example.com/path\u202eevil"))assertThrows(IllegalArgumentException.class,()->SafeLinks.requireWeb(link));
    }
    @Test public void insertionAtCaretPreservesBothTextHalvesAndTheirFormatting(){
        Note note=sample();String id=note.content.blocks.get(0).id;
        DocumentContent next=note.content.insertAtText(id,7,DocumentContent.Block.table("t","Table01",TextTable.empty(1,1)));
        assertEquals("before ",next.blocks.get(0).text.text);assertEquals("selected after",next.blocks.get(2).text.text);
        assertEquals(7,next.blocks.get(0).text.marks.get(0).end);assertEquals(14,next.blocks.get(2).text.marks.get(0).end);
        assertEquals("before selected after",note.content.blocks.get(0).text.text);
    }
    @Test public void mergeKeepsCanonicalTablesAndFormattingInsteadOfFlattening(){
        Note first=sample();DocumentContent content=first.content.insert(1,DocumentContent.Block.table("table","Table01",TextTable.empty(1,1).withRichCell(0,0,new RichText("cell",Arrays.asList(new Note.Mark("bold",0,4))))));first=first.withContent(content,4);
        Note merged=NoteOperations.merge(Arrays.asList(first,first.withId("duplicate")),10);assertNotNull(merged.content);assertEquals(2,merged.content.blocks.stream().filter(block->block.table!=null).count());for(DocumentContent.Block block:merged.content.blocks)if(block.table!=null)assertEquals("bold",block.table.cells.get(0).get(0).marks.get(0).type);
        assertEquals(2,first.content.blocks.size());assertEquals("before selected after",first.content.blocks.get(0).text.text);
    }
    @Test public void sharingAndSearchRetainNumberingAndHyperlinkDestinations(){RichText text=new RichText("Read",Arrays.asList(new Note.Mark("number",0,4,"3"),new Note.Mark("link",0,4,"https://example.com/manual")));Note note=sample().withContent(new DocumentContent(Arrays.asList(DocumentContent.Block.text("linked",text))),5);assertEquals("3. Read (https://example.com/manual)",note.plainText());assertTrue(note.matches("example.com/manual"));}
    @Test public void excessiveFormattingAndTextAreRejectedWithoutChangingTheSource(){Note source=sample();List<Note.Mark> marks=new ArrayList<>();for(int i=0;i<=RichText.MAX_MARKS;i++)marks.add(new Note.Mark("bold",0,1));assertThrows(IllegalArgumentException.class,()->new RichText("x",marks));String huge=String.join("",Collections.nCopies(DocumentContent.MAX_TEXT_WEIGHT+1,"x"));assertThrows(IllegalArgumentException.class,()->new DocumentContent(Arrays.asList(DocumentContent.Block.text("huge",new RichText(huge,Collections.emptyList())))));assertEquals("before selected after",source.content.blocks.get(0).text.text);}

    @Test public void repeatedUndoRedoDoesNotRetainDeadCursorSnapshots()throws Exception{Note initial=sample(),current=initial.title("Edited",4);DocumentHistory history=new DocumentHistory();history.rememberCursor(initial,new DocumentHistory.Cursor("title",0,0));history.record(initial,current,null,1);history.rememberCursor(current,new DocumentHistory.Cursor("title",6,6));for(int i=0;i<200;i++){Note target=i%2==0?history.undo(current):history.redo(current);DocumentHistory.Cursor cursor=history.cursor(target);assertNotNull(cursor);current=target.state(target.pinned,target.archived,target.deletedAt,10+i);history.rememberCursor(current,cursor);}java.lang.reflect.Field field=DocumentHistory.class.getDeclaredField("cursors");field.setAccessible(true);assertTrue(((Map<?,?>)field.get(history)).size()<=3);}

    @Test public void malformedUnicodeCannotBecomeQuestionMarksDuringCanonicalSave(){for(String text:Arrays.asList("\ud800","\udc00","a\ud800b"))assertThrows(IllegalArgumentException.class,()->new RichText(text,Collections.emptyList()));String valid="😀 ಕನ್ನಡ é 👩‍💻";assertEquals(valid,new RichText(valid,Collections.emptyList()).text);}

    @Test public void transcriptArrivalCleansOnlyAnUntouchedCanonicalPendingMarker(){String id=UUID.randomUUID().toString();Note old=VoiceTranscripts.placeholder(id,1),canonical=old.withContent(DocumentContent.fromLegacy(old),2),applied=VoiceTranscripts.apply(canonical,id,"Offline text",3);assertFalse(applied.plainText().contains("transcription pending"));assertTrue(applied.plainText().contains("Offline text"));assertTrue(applied.voiceSources.contains(id));assertSame(applied,VoiceTranscripts.apply(applied,id,"Repeated",4));Note edited=canonical.withContent(canonical.content.replace(canonical.content.blocks.get(0).id,DocumentContent.Block.text(canonical.content.blocks.get(0).id,new RichText("My edits: "+VoiceTranscripts.pending(id),Collections.emptyList()))),4);assertTrue(VoiceTranscripts.apply(edited,id,"Text",5).plainText().contains("My edits: "+VoiceTranscripts.pending(id)));}

}
