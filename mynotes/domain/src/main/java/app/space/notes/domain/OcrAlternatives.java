package app.space.notes.domain;

/** Conservative segmentation choice, not a language detector or calibrated correctness score. */
public final class OcrAlternatives {
 private OcrAlternatives(){}
 private static final String SYMBOLS="=+−-*/×÷%₹$€£";
 public static boolean prefer(String baseline,int confidence,String candidate,int score){
  if(candidate==null || candidate.trim().isEmpty())return false;
  if(baseline==null || baseline.trim().isEmpty())return true;
  // Sparse text can rank higher while treating an equals sign as a graphical rule.
  // Never discard already recognized mathematical/currency symbols for that score.
  for(int i=0;i<SYMBOLS.length();i++){char symbol=SYMBOLS.charAt(i);if(count(candidate,symbol)<count(baseline,symbol))return false;}
  // A tiny confidence gain must not replace clear native-script words with new
  // Latin-looking fragments. Existing genuinely mixed-script baselines stay eligible.
  if(confidence>=80 && OcrLineRecovery.nativeScript(baseline)!=null && hasLatin(candidate) && nativeLetters(candidate)<nativeLetters(baseline))return false;
  if(needsBlockCheck(baseline) && !needsBlockCheck(candidate))return score>=confidence && score>=50;
  return score>confidence && candidate.length()>=baseline.length()*.75
      || score>=50 && score>=confidence-5 && candidate.length()>baseline.length()*1.15;
 }
 /** Native letters wedged into an otherwise numeric run warrant one block pass. */
 public static boolean needsBlockCheck(String text){
  if(text==null || text.length()>100000)return false;
  for(String line:text.split("[\\r\\n]+")){
   int digits=0,nativeLetters=0;boolean adjacent=false;int[] points=line.codePoints().toArray();
   for(int i=0;i<points.length;i++){int cp=points[i];if(cp>='0' && cp<='9')digits++;
    if(Character.isLetter(cp) && (Character.UnicodeScript.of(cp)==Character.UnicodeScript.KANNADA || Character.UnicodeScript.of(cp)==Character.UnicodeScript.DEVANAGARI)){
     nativeLetters++;if(i>0 && points[i-1]>='0' && points[i-1]<='9' || i+1<points.length && points[i+1]>='0' && points[i+1]<='9')adjacent=true;
    }
   }
   if(digits>=4 && nativeLetters>0 && nativeLetters<=2 && adjacent)return true;
  }
  return false;
 }
 private static boolean hasLatin(String text){return text.codePoints().anyMatch(cp->Character.isLetter(cp) && Character.UnicodeScript.of(cp)==Character.UnicodeScript.LATIN);}
 private static long nativeLetters(String text){return text.codePoints().filter(cp->Character.isLetter(cp) && (Character.UnicodeScript.of(cp)==Character.UnicodeScript.KANNADA || Character.UnicodeScript.of(cp)==Character.UnicodeScript.DEVANAGARI)).count();}
 private static int count(String text,char symbol){int total=0;for(int i=0;i<text.length();i++)if(text.charAt(i)==symbol)total++;return total;}
}
