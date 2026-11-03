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
  return score>confidence && candidate.length()>=baseline.length()*.75
      || score>=50 && score>=confidence-5 && candidate.length()>baseline.length()*1.15;
 }
 private static int count(String text,char symbol){int total=0;for(int i=0;i<text.length();i++)if(text.charAt(i)==symbol)total++;return total;}
}
