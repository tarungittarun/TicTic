package app.space.notes.domain;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class RichLinksTest {
 @Test public void insertionPreservesSurroundingFormattingAndSource(){RichText source=new RichText("BeforeAfter",Arrays.asList(new Note.Mark("bold",0,11)));RichText next=RichLinks.insert(source,6,"Site","https://example.com");assertEquals("BeforeSiteAfter",next.text);assertEquals("BeforeAfter",source.text);assertTrue(next.marks.stream().anyMatch(mark->mark.type.equals("link") && mark.start==6 && mark.end==10));assertTrue(next.marks.stream().anyMatch(mark->mark.type.equals("bold") && mark.start==0 && mark.end==15));}
 @Test(expected=IllegalArgumentException.class) public void unsafeLinksNeverInsertText(){RichLinks.insert(new RichText("Keep",Collections.emptyList()),0,"Site","javascript:alert(1)");}
 @Test(expected=IllegalArgumentException.class) public void surrogateBoundariesRemainValidated(){RichLinks.insert(new RichText("😀",Collections.emptyList()),1,"Site","https://example.com");}
}
