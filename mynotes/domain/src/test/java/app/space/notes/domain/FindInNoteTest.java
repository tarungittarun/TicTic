package app.space.notes.domain;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class FindInNoteTest {
 @Test public void literalCaseInsensitiveOffsetsStayInOriginalText(){List<FindInNote.Match> hits=FindInNote.search(Arrays.asList("Title","İ test TEST [x] 😀"),"test");assertEquals(2,hits.size());assertEquals(1,hits.get(0).field);assertEquals(2,hits.get(0).start);assertEquals(6,hits.get(0).end);assertEquals(1,FindInNote.search(Arrays.asList("a.*b"),".*").size());}
 @Test public void boundedAndEmpty(){assertTrue(FindInNote.search(Arrays.asList("abc"),"").isEmpty());assertEquals(101,FindInNote.search(Arrays.asList(String.join("",Collections.nCopies(200,"a"))),"a").size());assertTrue(FindInNote.search(Arrays.asList("abc"),"missing").isEmpty());}
 @Test public void neverReturnsHalfEmoji(){assertEquals(0,FindInNote.search(Arrays.asList("😀"),"\uDE00").size());assertEquals(1,FindInNote.search(Arrays.asList("😀"),"😀").size());}
 @Test public void convertWritesStatusMarkersWithoutConsumingLiteralRowPrefixes(){Note list=Note.create(1).list("List",Arrays.asList(new Note.Item("a","milk",true,0),new Note.Item("b","[ ] literal",false,1)),false,2);assertEquals("[x] milk\n  [ ] [ ] literal",list.convert(3).body);Note restored=list.convert(3).convert(4);assertTrue(restored.items.get(0).checked);assertEquals("[ ] literal",restored.items.get(1).text);assertEquals(1,restored.items.get(1).depth);assertTrue(list.plainText().contains("[x] milk"));assertTrue(list.checklist);}
}
