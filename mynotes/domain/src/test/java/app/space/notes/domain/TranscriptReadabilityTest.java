package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class TranscriptReadabilityTest {
 private TranscriptReadability.Word w(String s,double start,double end){return new TranscriptReadability.Word(s,start,end);}
 @Test public void pausesProduceCommaAndNewSentence(){assertEquals("Hello there, friend. Welcome.",TranscriptReadability.format(Arrays.asList(w("hello",0,.2),w("there",.25,.5),w("friend",1,1.2),w("welcome",2.3,2.6))));}
 @Test public void neverInventsQuestionsOrExclamations(){assertEquals("Is this ready.",TranscriptReadability.format(Arrays.asList(w("is",0,.1),w("this",.2,.3),w("ready",.4,.5))));}
 @Test public void existingEndPunctuationIsPreserved(){assertEquals("Ready!",TranscriptReadability.sentence("ready!"));assertEquals("What?",TranscriptReadability.sentence("what?"));}
 @Test public void emptyStaysEmpty(){assertEquals("",TranscriptReadability.format(Collections.emptyList()));assertEquals("",TranscriptReadability.sentence("  "));}
 @Test public void overlappingTimesDoNotInventBreaks(){assertEquals("One two.",TranscriptReadability.format(Arrays.asList(w("one",0,1),w("two",.5,1.5))));}
 @Test(expected=IllegalArgumentException.class) public void badTimingRefused(){w("bad",Double.NaN,1);}
}
