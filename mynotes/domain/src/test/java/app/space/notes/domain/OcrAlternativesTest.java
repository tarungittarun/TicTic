package app.space.notes.domain;
import org.junit.Test;
import static org.junit.Assert.*;
public class OcrAlternativesTest {
 @Test public void higherScoreCannotEraseEqualsOrArithmetic(){assertFalse(OcrAlternatives.prefer("TOTAL 25 + 5 = 30",80,"TOTAL 25 + 5 — 30",95));assertFalse(OcrAlternatives.prefer("25 + 5 = 30",60,"25 5 = 30",85));}
 @Test public void preserveSymbolMultiplicityAndCurrencies(){assertFalse(OcrAlternatives.prefer("a=b=c ₹25",50,"a=b c ₹25 extra detail",90));assertFalse(OcrAlternatives.prefer("₹25",50,"25 more text",90));assertTrue(OcrAlternatives.prefer("a=b=c ₹25",70,"a=b=c ₹25 more detail",70));}
 @Test public void additionalCoverageNeedsBoundedConfidenceAndNoSymbolLoss(){assertTrue(OcrAlternatives.prefer("Heading",80,"Heading and missing body text",78));assertFalse(OcrAlternatives.prefer("Heading",80,"Heading and invented low confidence text",60));assertFalse(OcrAlternatives.prefer("Heading",80,"",99));}
 @Test public void emptyBaselineCanUseAReviewedSparseCandidate(){assertTrue(OcrAlternatives.prefer("",0,"Recovered text",35));assertFalse(OcrAlternatives.prefer("",0,null,90));}
 @Test public void tinyConfidenceGainCannotTurnClearKannadaIntoNewLatinFragments(){assertFalse(OcrAlternatives.prefer("ಕನ್ನಡ ಭಾಷೆ\nಭಾರತ ನಮಸ್ಕಾರ",87,"BYS ಭಾಷೆ\nಭಾರತ ನಮಸ್ಕಾರ",89));assertFalse(OcrAlternatives.prefer("भारत नमस्ते",88,"BCS नमस्ते",92));}
 @Test public void nativeGuardDoesNotBanExistingMixedScriptOrAdditionalLatinCoverage(){assertTrue(OcrAlternatives.prefer("Meeting ಭಾರತ ಕನ್ನಡ",80,"Meeting ಭಾರತ ಕನ್ನಡ and more",90));assertTrue(OcrAlternatives.prefer("ಭಾರತ ನಮಸ್ಕಾರ",80,"GST ಭಾರತ ನಮಸ್ಕಾರ",90));}
 @Test public void suspiciousNumericFragmentsRequestOneBlockCheckNotLanguageGuessing(){assertTrue(OcrAlternatives.needsBlockCheck("Meeting ಭಾರತ ಕನ್ನಡ\n1001254 5 ಎ30"));assertTrue(OcrAlternatives.needsBlockCheck("Meeting ಭಾರತ ಕನ್ನಡ\n0೩125 - 5 ಎ30"));assertFalse(OcrAlternatives.needsBlockCheck("Meeting ಭಾರತ ಕನ್ನಡ\nTotal 25 + 5 = 30"));assertFalse(OcrAlternatives.needsBlockCheck("Price INR 350\nಮೂಲ್ಯ ೩೫೦"));assertFalse(OcrAlternatives.needsBlockCheck("ಕನ್ನಡ ಭಾಷೆ\nಭಾರತ ನಮಸ್ಕಾರ"));}
 @Test public void equallyScoredBlockCanRemoveNumericScriptCorruptionWithoutLosingSymbols(){assertTrue(OcrAlternatives.prefer("Meeting ಭಾರತ ಕನ್ನಡ\n0೩125 - 5 ಎ30",88,"Meeting ಭಾರತ ಕನ್ನಡ\nTotal 25 - 5 530",88));assertFalse(OcrAlternatives.prefer("0೩125 - 5 ಎ30",88,"Total 25 + 5 530",88));assertFalse(OcrAlternatives.prefer("0೩125 - 5 ಎ30",88,"Total 25 - 5 530",87));}
}
