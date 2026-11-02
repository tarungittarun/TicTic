package app.space.notes.domain;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ImageTextSelectionTest {
 private ImageTextSelection words(){return new ImageTextSelection(Arrays.asList(new ImageTextSelection.Word("नमस्ते",.1f,.1f,.3f,.3f,1),new ImageTextSelection.Word("25",.4f,.1f,.5f,.3f,1),new ImageTextSelection.Word("+",.1f,.5f,.2f,.7f,2),new ImageTextSelection.Word("😀",.3f,.5f,.4f,.7f,2)));}
 @Test public void selectingAllPreservesUnicodeSymbolsAndEngineLineOrder(){ImageTextSelection s=words();s.all(true);assertEquals("नमस्ते 25\n+ 😀",s.text());assertEquals(4,s.count());}
 @Test public void clearingSelectsNothingAndNeverInventsText(){ImageTextSelection s=words();s.all(true);s.all(false);assertEquals("",s.text());assertEquals(0,s.count());}
 @Test public void reversedDragIntersectsOnlyChosenRegions(){ImageTextSelection s=words();s.rectangle(.6f,.4f,0,0);assertEquals("नमस्ते 25",s.text());}
 @Test public void touchingAnEdgeDoesNotSelectUnrelatedWords(){ImageTextSelection s=words();s.rectangle(.5f,.1f,.9f,.3f);assertEquals("",s.text());}
 @Test public void tappingTogglesWithoutChangingTheRecognizedWord(){ImageTextSelection s=words();s.toggle(3);assertEquals("😀",s.text());s.toggle(3);assertEquals("",s.text());}
 @Test(expected=IllegalArgumentException.class) public void nonFiniteBoxesAreRejected(){new ImageTextSelection.Word("bad",Float.NaN,0,1,1,0);}
 @Test(expected=IllegalArgumentException.class) public void outOfImageBoxesAreRejected(){new ImageTextSelection.Word("bad",0,0,1.1f,1,0);}
 @Test(expected=IllegalArgumentException.class) public void brokenSurrogatesAreNotSilentlySaved(){new ImageTextSelection.Word("\ud83d",0,0,1,1,0);}
 @Test(expected=IllegalArgumentException.class) public void regionCountIsBounded(){new ImageTextSelection(Collections.nCopies(4097,new ImageTextSelection.Word("a",0,0,1,1,0)));}
 @Test(expected=IllegalArgumentException.class) public void outputLengthIsBounded(){ImageTextSelection s=new ImageTextSelection(Collections.nCopies(100,new ImageTextSelection.Word(String.join("",Collections.nCopies(1500,"x")),0,0,1,1,0)));s.all(true);s.text();}
 @Test public void callerCannotReorderRegionsAfterConstruction(){List<ImageTextSelection.Word> source=new ArrayList<>(words().words);ImageTextSelection s=new ImageTextSelection(source);source.clear();s.all(true);assertEquals(4,s.count());}
 @Test(expected=UnsupportedOperationException.class) public void exposedRegionListIsImmutable(){words().words.clear();}
}
