package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class DocCompletionTest {
 @Test public void tableHeaderSurvivesCellAndColumnChangesWithoutInjectingFormatting(){TextTable table=TextTable.empty(2,2).withHeader(true).withCell(0,0,"Name");assertTrue(table.insertColumn(1).deleteColumn(1).insertRow(1).hasHeader);assertTrue(table.cells.get(0).get(0).marks.isEmpty());assertFalse(table.deleteRow(0).hasHeader);assertEquals("Name",table.withHeader(false).rows.get(0).get(0));}
 @Test(expected=IllegalArgumentException.class) public void insertionNeverSilentlyDemotesTheHeader(){TextTable.empty(2,2).withHeader(true).insertRow(0);}
 @Test public void visualMovementSplitsRichTextAndRetainsHeaderAndIdentity(){TextTable table=TextTable.empty(2,2).withHeader(true);DocumentContent.Block object=DocumentContent.Block.table("table","Table01",table);DocumentContent content=new DocumentContent(Arrays.asList(DocumentContent.Block.text("text",new RichText("BeforeAfter",Arrays.asList(new Note.Mark("bold",0,11)))),object),false);DocumentContent moved=content.moveIntoText("table","text",6);assertEquals(3,moved.blocks.size());assertEquals("Before",moved.blocks.get(0).text.text);assertSame(object,moved.blocks.get(1));assertTrue(moved.blocks.get(1).table.hasHeader);assertEquals("After",moved.blocks.get(2).text.text);assertEquals(5,moved.blocks.get(2).text.marks.get(0).end);assertEquals(2,content.blocks.size());}
 @Test public void headerRoleParticipatesInHistoryFingerprint(){Note original=Workspace.create(1,true);DocumentContent before=original.content.insert(1,DocumentContent.Block.table("t","Table01",TextTable.empty(2,2)));DocumentContent after=before.replace("t",DocumentContent.Block.table("t","Table01",before.block("t").table.withHeader(true)));assertNotEquals(before.fingerprint(),after.fingerprint());}
 @Test(expected=IllegalArgumentException.class) public void contentCannotBeMovedIntoItself(){DocumentContent text=Workspace.create(1,true).content;text.moveIntoText(text.blocks.get(0).id,text.blocks.get(0).id,0);}
}
