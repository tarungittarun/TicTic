package app.space.notes.domain;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class RichTextTest {
    @Test public void selectedRangeDoesNotConvertOrDiscardSurroundingText(){
        RichText text=new RichText("before\nselected\nafter",Arrays.asList(new Note.Mark("bold",0,21)));
        List<RichText> pieces=text.selection(7,15);
        assertEquals("before\n",pieces.get(0).text);assertEquals("selected",pieces.get(1).text);assertEquals("\nafter",pieces.get(2).text);
        assertEquals(text.text,pieces.get(0).text+pieces.get(1).text+pieces.get(2).text);
        assertEquals(0,pieces.get(1).marks.get(0).start);assertEquals(8,pieces.get(1).marks.get(0).end);
        assertEquals(21,text.marks.get(0).end);
    }
    @Test public void clipsAndRebasesOnlyIntersectingMarks(){
        RichText text=new RichText("abcdefgh",Arrays.asList(new Note.Mark("bold",0,4),new Note.Mark("italic",3,8),new Note.Mark("underline",0,1)));
        RichText cut=text.slice(2,6);assertEquals("cdef",cut.text);assertEquals(2,cut.marks.size());
        assertEquals(0,cut.marks.get(0).start);assertEquals(2,cut.marks.get(0).end);
        assertEquals(1,cut.marks.get(1).start);assertEquals(4,cut.marks.get(1).end);
    }
    @Test public void emptySelectionAndNewlinesArePreserved(){
        RichText text=new RichText("a\r\nb\n",Collections.emptyList());
        List<RichText> pieces=text.selection(3,3);assertEquals("",pieces.get(1).text);
        assertEquals(text.text,pieces.get(0).text+pieces.get(2).text);
        assertEquals("a\r\nb\n",text.slice(0,text.text.length()).text);
    }
    @Test public void refusesHalfSurrogatesAndInvalidRanges(){
        RichText text=new RichText("A\uD83D\uDE00B",Collections.emptyList());
        assertEquals("\uD83D\uDE00",text.slice(1,3).text);
        assertThrows(IllegalArgumentException.class,()->text.slice(1,2));
        assertThrows(IllegalArgumentException.class,()->text.slice(2,3));
        assertThrows(IllegalArgumentException.class,()->text.selection(-1,2));
        assertThrows(IllegalArgumentException.class,()->text.slice(3,1));
        assertThrows(IllegalArgumentException.class,()->text.slice(0,5));
        assertThrows(IllegalArgumentException.class,()->new RichText(text.text,Arrays.asList(new Note.Mark("bold",1,2))));
    }
    @Test public void snapshotsMarksAndRejectsOutOfBoundsWithoutTruncation(){
        List<Note.Mark> marks=new ArrayList<>();marks.add(new Note.Mark("bold",0,1));
        RichText text=new RichText("abc",marks);marks.clear();assertEquals(1,text.marks.size());
        assertThrows(UnsupportedOperationException.class,()->text.marks.clear());
        assertThrows(UnsupportedOperationException.class,()->text.selection(0,1).clear());
        assertThrows(IllegalArgumentException.class,()->new RichText("a",Arrays.asList(new Note.Mark("bold",0,2))));
        assertThrows(IllegalArgumentException.class,()->new RichText(null,Collections.emptyList()));
    }
}
