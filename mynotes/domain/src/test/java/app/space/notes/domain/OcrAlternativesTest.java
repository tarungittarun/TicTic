package app.space.notes.domain;
import org.junit.Test;
import static org.junit.Assert.*;
public class OcrAlternativesTest {
 @Test public void higherScoreCannotEraseEqualsOrArithmetic(){assertFalse(OcrAlternatives.prefer("TOTAL 25 + 5 = 30",80,"TOTAL 25 + 5 — 30",95));assertFalse(OcrAlternatives.prefer("25 + 5 = 30",60,"25 5 = 30",85));}
 @Test public void preserveSymbolMultiplicityAndCurrencies(){assertFalse(OcrAlternatives.prefer("a=b=c ₹25",50,"a=b c ₹25 extra detail",90));assertFalse(OcrAlternatives.prefer("₹25",50,"25 more text",90));assertTrue(OcrAlternatives.prefer("a=b=c ₹25",70,"a=b=c ₹25 more detail",70));}
 @Test public void additionalCoverageNeedsBoundedConfidenceAndNoSymbolLoss(){assertTrue(OcrAlternatives.prefer("Heading",80,"Heading and missing body text",78));assertFalse(OcrAlternatives.prefer("Heading",80,"Heading and invented low confidence text",60));assertFalse(OcrAlternatives.prefer("Heading",80,"",99));}
 @Test public void emptyBaselineCanUseAReviewedSparseCandidate(){assertTrue(OcrAlternatives.prefer("",0,"Recovered text",35));assertFalse(OcrAlternatives.prefer("",0,null,90));}
}
