package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ParagraphNumberingTest {
    @Test public void newlineSplitsAnInheritedNumberIntoConsecutiveParagraphs(){List<Note.Mark> marks=ParagraphNumbering.normalize(Arrays.asList(new Note.Mark("number",0,7,"1")),"One\nTwo");assertEquals(2,marks.size());assertEquals("1",marks.get(0).value);assertEquals("2",marks.get(1).value);assertEquals(4,marks.get(1).start);}
    @Test public void deletedNewlineCannotLeaveTwoPrefixesOnOneParagraph(){List<Note.Mark> marks=ParagraphNumbering.normalize(Arrays.asList(new Note.Mark("number",0,3,"1"),new Note.Mark("number",3,6,"2")),"OneTwo");assertEquals(1,marks.size());assertEquals("1",marks.get(0).value);assertEquals(6,marks.get(0).end);}
    @Test public void separateListsKeepTheirStartingNumberAndInlineStyles(){List<Note.Mark> marks=ParagraphNumbering.normalize(Arrays.asList(new Note.Mark("bold",0,3),new Note.Mark("number",0,3,"4"),new Note.Mark("number",6,9,"8")),"One\nx\nTwo");assertEquals(3,marks.size());assertEquals("bold",marks.get(0).type);assertEquals("4",marks.get(1).value);assertEquals("8",marks.get(2).value);}
}
