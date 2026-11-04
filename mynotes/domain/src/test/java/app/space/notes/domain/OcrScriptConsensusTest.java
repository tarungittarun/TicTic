package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class OcrScriptConsensusTest {
 private static final String BASE="ಕನ್ಹಡ ಭಾಷೆ\nಭಾರತ ನಮಸ್ಕಾರ",OTHER="ಕನ್ನಡ ಭಾಷೆ\nಭಾರತ ನಮಸ್ಕಾರ";
 private List<ImageTextSelection.Word> words(String text){String[] tokens=text.split("\\s+");List<ImageTextSelection.Word> out=new ArrayList<>();for(int i=0;i<tokens.length;i++)out.add(new ImageTextSelection.Word(tokens[i],.1f+(i%2)*.3f,.1f+(i/2)*.3f,.3f+(i%2)*.3f,.2f+(i/2)*.3f,i/2));return out;}
 private boolean prefer(String ordinary,int score,String adaptive,int other){return OcrScriptConsensus.prefer(BASE,words(BASE),ordinary,score,words(ordinary),adaptive,other,words(adaptive));}
 @Test public void completeAlignedAgreementCanReplaceAnAmbiguousReading(){assertTrue(prefer(OTHER,70,OTHER,96));}
 @Test public void confidenceAloneCannotDropAWord(){assertFalse(prefer("ಭಾಷೆ ಭಾರತ ನಮಸ್ಕಾರ",96,"ಭಾಷೆ ಭಾರತ ನಮಸ್ಕಾರ",96));}
 @Test public void disagreeingCandidatesNeverWin(){assertFalse(prefer(BASE,96,OTHER,96));assertFalse(prefer(OTHER,96,BASE,96));}
 @Test public void unchangedReadingDoesNotTriggerReplacement(){assertFalse(prefer(BASE,96,BASE,96));}
 @Test public void lowOrInvalidConfidenceCannotWin(){for(int score:new int[]{-1,0,69,101})assertFalse(prefer(OTHER,score,OTHER,96));for(int score:new int[]{-1,0,89,101})assertFalse(prefer(OTHER,70,OTHER,score));}
 @Test public void mixedLatinHindiAndSingleLineTextAreExcluded(){for(String text:new String[]{"English text\nTOTAL 25", "ಕನ್ನಡ ಭಾಷೆ\nभारत नमस्कार",OTHER.replace('\n',' '),"ಕನ್ನಡ\nಭಾಷೆ"})assertFalse(OcrScriptConsensus.eligible(text,words(text)));}
 @Test public void staleAndShiftedBoxesCannotWin(){List<ImageTextSelection.Word> stale=words(BASE);assertFalse(OcrScriptConsensus.prefer(BASE,stale,OTHER,70,stale,OTHER,96,words(OTHER)));List<ImageTextSelection.Word> shifted=words(OTHER);shifted.set(0,new ImageTextSelection.Word("ಕನ್ನಡ",.7f,.7f,.9f,.9f,0));assertFalse(OcrScriptConsensus.prefer(BASE,words(BASE),OTHER,70,shifted,OTHER,96,words(OTHER)));}
 @Test public void malformedInputsAreRejected(){assertFalse(OcrScriptConsensus.eligible(null,null));List<ImageTextSelection.Word> bad=words(BASE);bad.set(0,null);assertFalse(OcrScriptConsensus.eligible(BASE,bad));}
}
