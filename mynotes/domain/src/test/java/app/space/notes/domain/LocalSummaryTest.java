package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class LocalSummaryTest {
 @Test public void selectsOnlyVerbatimSentencesInSourceOrder(){String source="Meeting starts on Monday. Budget review needs the latest budget figures. Send budget figures before Monday. Do not invent a deadline.";List<String> result=LocalSummary.extract(source,3);assertTrue(result.size()<=3);int last=-1;for(String sentence:result){assertTrue(source.contains(sentence));int at=source.indexOf(sentence);assertTrue(at>last);last=at;}assertEquals(source,"Meeting starts on Monday. Budget review needs the latest budget figures. Send budget figures before Monday. Do not invent a deadline.");}
 @Test public void preservesUnicodeAndDoesNotInventContent(){String source="ಕನ್ನಡ 😀 + 25 = 30\nभारत नमस्ते\nKeep the original.";List<String> result=LocalSummary.extract(source,5);assertTrue(result.contains("ಕನ್ನಡ 😀 + 25 = 30"));assertTrue(result.contains("भारत नमस्ते"));assertTrue(LocalSummary.extract("",3).isEmpty());assertEquals(1,LocalSummary.extract("One line.\nOne line.",5).size());}
 @Test public void enforcesBounds(){for(int count:new int[]{0,6})try{LocalSummary.extract("Keep",count);fail();}catch(IllegalArgumentException expected){}try{LocalSummary.extract(String.join("",Collections.nCopies(100001,"a")),3);fail();}catch(IllegalArgumentException expected){}}
}
