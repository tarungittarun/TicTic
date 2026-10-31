package app.space.notes.domain;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class RichLinksTest {
 @Test public void replacementKeepsSurroundingTextAndStyles(){RichText source=new RichText("BeforeLinkAfter",Arrays.asList(new Note.Mark("bold",0,15)));RichText next=RichLinks.replace(source,6,10,"Site","https://example.com");assertEquals("BeforeSiteAfter",next.text);assertEquals("BeforeLinkAfter",source.text);assertEquals("bold=;",TypingStyle.capture(next,6,10).encode());assertTrue(next.marks.stream().anyMatch(mark->mark.type.equals("link") && mark.start==6 && mark.end==10));assertEquals("bold=;",TypingStyle.capture(next,0,6).encode());assertEquals("bold=;",TypingStyle.capture(next,10,15).encode());}
 @Test public void replacementIsValidatedBeforeAnyContentChange(){RichText source=new RichText("Keep",Collections.emptyList());assertThrows(IllegalArgumentException.class,()->RichLinks.replace(source,0,4,"Site","file:///private"));assertThrows(IllegalArgumentException.class,()->RichLinks.replace(source,0,4," ","https://example.com"));assertEquals("Keep",source.text);}
 @Test public void insertionPreservesSurroundingFormattingAndSource(){RichText source=new RichText("BeforeAfter",Arrays.asList(new Note.Mark("bold",0,11)));RichText next=RichLinks.insert(source,6,"Site","https://example.com");assertEquals("BeforeSiteAfter",next.text);assertEquals("BeforeAfter",source.text);assertTrue(next.marks.stream().anyMatch(mark->mark.type.equals("link") && mark.start==6 && mark.end==10));assertTrue(next.marks.stream().anyMatch(mark->mark.type.equals("bold") && mark.start==0 && mark.end==15));}
 @Test(expected=IllegalArgumentException.class) public void unsafeLinksNeverInsertText(){RichLinks.insert(new RichText("Keep",Collections.emptyList()),0,"Site","javascript:alert(1)");}
 @Test(expected=IllegalArgumentException.class) public void surrogateBoundariesRemainValidated(){RichLinks.insert(new RichText("😀",Collections.emptyList()),1,"Site","https://example.com");}
}
