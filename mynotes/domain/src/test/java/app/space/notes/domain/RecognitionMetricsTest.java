package app.space.notes.domain;
import org.junit.Test;
import static org.junit.Assert.*;
public class RecognitionMetricsTest {
 @Test public void unicodeEmojiIsOneCodepointNotTwoSurrogates(){RecognitionMetrics.Score s=RecognitionMetrics.characters("A😀+","A🙂+");assertEquals(1,s.edits);assertEquals(3,s.referenceUnits);assertEquals(1/3d,s.rate(),.000001);}
 @Test public void symbolsAndPunctuationAreNeverErased(){assertEquals(2,RecognitionMetrics.characters("25 + 5 = 30","25 - 5 : 30").edits);assertEquals(1,RecognitionMetrics.words("ready?","ready").edits);}
 @Test public void canonicalUnicodeCombiningFormsMatch(){assertEquals(0,RecognitionMetrics.characters("é","e\u0301").edits);}
 @Test public void wordCaseAndWhitespaceNormalizationIsExplicit(){assertEquals(0,RecognitionMetrics.words("ONE\n two","one   TWO").edits);}
 @Test public void insertionsCanExceedOneHundredPercent(){assertEquals(3,RecognitionMetrics.words("one","two three four").edits);assertEquals(3d,RecognitionMetrics.words("one","two three four").rate(),0);}
 @Test public void deletionInsertionAndEmptyConventions(){assertEquals(2,RecognitionMetrics.characters("ab","").edits);assertEquals(2,RecognitionMetrics.characters("","ab").edits);assertEquals(0d,RecognitionMetrics.characters("","").rate(),0);}
 @Test(expected=IllegalArgumentException.class) public void quadraticWorkIsBounded(){RecognitionMetrics.characters(new String(new char[3000]),new String(new char[3000]));}
 @Test(expected=IllegalArgumentException.class) public void oversizedInputRejectedBeforeNormalization(){RecognitionMetrics.words(new String(new char[16001]),"");}
 @Test public void strictCaseSensitiveCharacterMetric(){assertEquals(1,RecognitionMetrics.characters("Hello","hello").edits);}
}
