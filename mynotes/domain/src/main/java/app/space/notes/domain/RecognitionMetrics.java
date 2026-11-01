package app.space.notes.domain;
import java.text.Normalizer;
import java.util.*;
/** Bounded evaluation only, not a recognition model. CER keeps case, spaces and symbols.
 * WER normalizes NFC, case and whitespace only; it never removes punctuation/errors.
 * Empty reference has denominator one, explicitly a count convention, not corpus accuracy.
 */
public final class RecognitionMetrics {
 private RecognitionMetrics(){}
 public static final class Score {
  public final int edits,referenceUnits,hypothesisUnits;
  Score(int edits,int reference,int hypothesis){this.edits=edits;referenceUnits=reference;hypothesisUnits=hypothesis;}
  public double rate(){return edits/(double)Math.max(1,referenceUnits);}
 }
 private static String normalized(String text){if(text==null || text.length()>16000)throw new IllegalArgumentException("Bounded evaluation text required");return Normalizer.normalize(text,Normalizer.Form.NFC);}
 public static Score characters(String reference,String hypothesis){return score(normalized(reference).codePoints().boxed().toArray(),normalized(hypothesis).codePoints().boxed().toArray());}
 public static Score words(String reference,String hypothesis){return score(words(reference),words(hypothesis));}
 private static String[] words(String text){String value=normalized(text).toLowerCase(Locale.ROOT).trim();return value.isEmpty()?new String[0]:value.split("\\s+");}
 private static Score score(Object[] a,Object[] b){if(a.length>4096 || b.length>4096 || (long)a.length*b.length>4000000)throw new IllegalArgumentException("Evaluation pair exceeds work budget");int[] previous=new int[b.length+1],next=new int[b.length+1];for(int j=0;j<=b.length;j++)previous[j]=j;for(int i=1;i<=a.length;i++){next[0]=i;for(int j=1;j<=b.length;j++)next[j]=Math.min(Math.min(previous[j]+1,next[j-1]+1),previous[j-1]+(a[i-1].equals(b[j-1])?0:1));int[] swap=previous;previous=next;next=swap;}return new Score(previous[b.length],a.length,b.length);}
}
