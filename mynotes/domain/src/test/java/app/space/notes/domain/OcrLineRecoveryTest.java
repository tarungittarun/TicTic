package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class OcrLineRecoveryTest {
 private static final String BASE="ಭಾರತ ನಮಸ್ಕಾರ",CANDIDATE="ಕನ್ನಡ ಭಾಷೆ\nಭಾರತ ನಸಮನಾರ";
 private ImageTextSelection.Word w(String text,float x,float y,float right,float bottom,int line){return new ImageTextSelection.Word(text,x,y,right,bottom,line);}
 private List<ImageTextSelection.Word> base(){return Arrays.asList(w("ಭಾರತ",.05f,.28f,.16f,.33f,1),w("ನಮಸ್ಕಾರ",.18f,.28f,.35f,.33f,1));}
 private List<ImageTextSelection.Word> extra(){return Arrays.asList(w("ಕನ್ನಡ",.05f,.14f,.15f,.19f,1),w("ಭಾಷೆ",.17f,.14f,.28f,.19f,1),w("ಭಾರತ",.05f,.28f,.16f,.33f,2),w("ನಸಮನಾರ",.18f,.28f,.35f,.33f,2));}
 @Test public void recoverMissingUpperLineWithoutOverwritingCorrectBaseline(){OcrLineRecovery.Recovered r=OcrLineRecovery.recover(BASE,96,base(),CANDIDATE,76,extra());assertNotNull(r);assertEquals("ಕನ್ನಡ ಭಾಷೆ\n"+BASE,r.text);assertEquals(76,r.confidence);assertEquals(4,r.words.size());assertEquals("ನಮಸ್ಕಾರ",r.words.get(3).text);assertEquals(base().get(1).left,r.words.get(3).left,0);ImageTextSelection selected=new ImageTextSelection(r.words);selected.all(true);assertEquals(r.text,selected.text());}
 @Test public void rejectLowConfidenceNewLine(){assertNull(OcrLineRecovery.recover(BASE,96,base(),CANDIDATE,69,extra()));assertNull(OcrLineRecovery.recover(BASE,79,base(),CANDIDATE,76,extra()));}
 @Test public void rejectNewLanguageAndLatinNoise(){List<ImageTextSelection.Word> words=extra();words.set(0,w("BYS",.05f,.14f,.15f,.19f,1));assertNull(OcrLineRecovery.recover(BASE,96,base(),CANDIDATE,76,words));words.set(0,w("भारत",.05f,.14f,.15f,.19f,1));assertNull(OcrLineRecovery.recover(BASE,96,base(),CANDIDATE,76,words));}
 @Test public void rejectDifferentColumn(){List<ImageTextSelection.Word> words=extra();words.set(0,w("ಕನ್ನಡ",.6f,.14f,.7f,.19f,1));words.set(1,w("ಭಾಷೆ",.72f,.14f,.9f,.19f,1));assertNull(OcrLineRecovery.recover(BASE,96,base(),CANDIDATE,76,words));}
 @Test public void rejectMissingMatchingExistingLine(){List<ImageTextSelection.Word> words=extra();words.set(2,w("ಭಾರತ",.5f,.6f,.6f,.65f,2));words.set(3,w("ನಸಮನಾರ",.7f,.6f,.9f,.65f,2));assertNull(OcrLineRecovery.recover(BASE,96,base(),CANDIDATE,76,words));}
 @Test public void rejectEmptyBoxesAndMultipleBaselineLines(){assertNull(OcrLineRecovery.recover(BASE,96,Collections.emptyList(),CANDIDATE,76,extra()));assertFalse(OcrLineRecovery.eligible(BASE+"\n"+BASE,96,CANDIDATE,76));}
 @Test public void rejectFarAwayLabel(){List<ImageTextSelection.Word> words=extra();words.set(0,w("ಕನ್ನಡ",.05f,.84f,.15f,.89f,1));words.set(1,w("ಭಾಷೆ",.17f,.84f,.28f,.89f,1));assertNull(OcrLineRecovery.recover(BASE,96,base(),CANDIDATE,76,words));}
 @Test public void existingBaselineWhitespaceAndWordCoordinatesRemainIntact(){String original="  "+BASE+"  \n";OcrLineRecovery.Recovered r=OcrLineRecovery.recover(original,96,base(),CANDIDATE,76,extra());assertNotNull(r);assertTrue(r.text.endsWith(original));for(int i=0;i<base().size();i++){assertEquals(base().get(i).top,r.words.get(i+2).top,0);assertEquals(base().get(i).right,r.words.get(i+2).right,0);}}
 @Test public void recoverLowerLineInReadingOrder(){List<ImageTextSelection.Word> words=extra();words.set(0,w("ಕನ್ನಡ",.05f,.42f,.15f,.47f,1));words.set(1,w("ಭಾಷೆ",.17f,.42f,.28f,.47f,1));OcrLineRecovery.Recovered r=OcrLineRecovery.recover(BASE,96,base(),CANDIDATE,76,words);assertNotNull(r);assertEquals(BASE+"\nಕನ್ನಡ ಭಾಷೆ",r.text);assertEquals("ಭಾರತ",r.words.get(0).text);assertEquals(1,r.words.get(2).line);}
 @Test public void englishBaselineDoesNotActivateNativeRecovery(){assertFalse(OcrLineRecovery.eligible("Correct existing text",96,CANDIDATE,76));}
 @Test public void rejectStaleBaselineWordText(){List<ImageTextSelection.Word> boxes=base();boxes.set(1,w("ಭಾಷೆ",.18f,.28f,.35f,.33f,1));assertNull("Do not expose a recovered display whose selected baseline words differ",OcrLineRecovery.recover(BASE,96,boxes,CANDIDATE,76,extra()));}
 @Test public void rejectStaleCandidateWordText(){assertNull("Sparse UTF8 text and sparse word boxes must describe the same candidate",OcrLineRecovery.recover(BASE,96,base(),"ಬೇರೆ ಭಾಷೆ\nಭಾರತ ನಸಮನಾರ",76,extra()));}
}
