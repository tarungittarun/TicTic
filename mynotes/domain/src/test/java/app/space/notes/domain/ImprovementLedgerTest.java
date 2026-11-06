package app.space.notes.domain;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class ImprovementLedgerTest {
 private Note note(String title,String body){return Note.create(1).edit(title,body,2);}
 @Test public void bodyOnlyResultHasAccurateRangeAndDoesNotMutate(){
  Note n=note("Meeting","Agenda cobalt42 decision");SearchPreview p=SearchPreview.forNote(n," COBALT42 ");
  assertEquals("Body",p.source);assertEquals("cobalt42",p.text.substring(p.start,p.end));assertEquals("Agenda cobalt42 decision",n.body);
 }
 @Test public void provenanceDoesNotInventBodyMatch(){
  SearchPreview title=SearchPreview.forNote(note("Needle","Other"),"needle");assertEquals("Title",title.source);
  Note label=note("Other","Different").properties(0,Arrays.asList("Needle"),3);assertEquals("Labels",SearchPreview.forNote(label,"needle").source);
  Note named=note("Other","Different").namedContent("diagram","Needle",3);assertEquals("Content names",SearchPreview.forNote(named,"needle").source);
  assertNull(SearchPreview.forNote(named,"absent"));assertNull(SearchPreview.forNote(named," "));
 }
 @Test public void unicodeExpansionMapsToOriginalGlyphNotFollowingCharacter(){
  SearchPreview x=SearchPreview.forNote(note("","İX 😀 marker"),"x");assertEquals("X",x.text.substring(x.start,x.end));
  SearchPreview dot=SearchPreview.forNote(note("","İX"),"\u0307");assertEquals("İ",dot.text.substring(dot.start,dot.end));
  SearchPreview emoji=SearchPreview.forNote(note("","😀 needle"),"NEEDLE");assertEquals("needle",emoji.text.substring(emoji.start,emoji.end));
 }
 @Test public void passageIsBoundedAndNotSplitInsideSurrogatePair(){
  StringBuilder b=new StringBuilder();for(int i=0;i<1000;i++)b.append("😀");b.append("needle");for(int i=0;i<1000;i++)b.append('x');
  SearchPreview p=SearchPreview.forNote(note("",b.toString()),"needle");assertTrue(p.text.length()<=164);assertEquals("needle",p.text.substring(p.start,p.end));assertTrue(p.text.startsWith("…"));
  assertFalse(Character.isLowSurrogate(p.text.charAt(1)));
 }
 @Test public void oversizedMatchUsesHonestBoundedFallback(){
  char[] chars=new char[200];Arrays.fill(chars,'x');String longQuery=new String(chars);SearchPreview p=SearchPreview.forNote(note("",longQuery),longQuery);assertEquals(-1,p.start);assertEquals(-1,p.end);assertTrue(p.text.length()<160);
 }
 @Test public void canonicalChecklistTableTextUsesExistingPlainTextSemantics(){
  Note legacy=note("","cobalt42");Note canonical=legacy.withContent(DocumentContent.fromLegacy(legacy),3);assertTrue(canonical.matches("cobalt42"));assertEquals("Body",SearchPreview.forNote(canonical,"cobalt42").source);
  Note list=note("","cobalt42").convert(3);assertEquals("Body",SearchPreview.forNote(list,"cobalt42").source);
 }
 @Test public void compactControlsLeaveThreeNormalLinesInSmallerBudget(){
  int panel=FormattingViewport.height(186,96,104,320);assertEquals(104,panel);assertTrue(186-panel>=26*3);
 }
 @Test public void formattingReservesBodyAndCapsGrowth(){
  assertEquals(150,FormattingViewport.height(250,100,144,320));assertEquals(320,FormattingViewport.height(900,100,144,320));
  assertEquals(144,FormattingViewport.height(200,100,144,320));assertEquals(60,FormattingViewport.height(60,100,144,320));assertEquals(0,FormattingViewport.height(0,100,144,320));
  assertThrows(IllegalArgumentException.class,()->FormattingViewport.height(-1,100,144,320));
 }
}
