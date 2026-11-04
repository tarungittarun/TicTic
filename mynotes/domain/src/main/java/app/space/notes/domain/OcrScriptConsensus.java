package app.space.notes.domain;
import java.util.*;

/** Conservative same-script, same-word-geometry agreement; never a text oracle. */
public final class OcrScriptConsensus {
 private OcrScriptConsensus(){}
 private static String normalized(String s){return java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFC).trim().replaceAll("\\s+"," ");}
 public static boolean eligible(String text,List<ImageTextSelection.Word> words){
  if(text==null || text.length()>512 || text.trim().split("[\\r\\n]+").length<2 || words==null || words.size()<4 || words.size()>64 || OcrLineRecovery.nativeScript(text)!=Character.UnicodeScript.KANNADA)return false;
  Set<Integer> lines=new HashSet<>();for(ImageTextSelection.Word w:words){if(w==null)return false;lines.add(w.line);}
  return lines.size()>=2 && lines.size()<=8 && matches(text,words);
 }
 private static boolean matches(String text,List<ImageTextSelection.Word> words){
  try{ImageTextSelection selection=new ImageTextSelection(words);selection.all(true);return normalized(text).equals(normalized(selection.text()));}catch(IllegalArgumentException | NullPointerException invalid){return false;}
 }
 private static String nonLetters(String text){StringBuilder out=new StringBuilder();text.codePoints().forEach(cp->{int type=Character.getType(cp);if(!Character.isLetter(cp) && type!=Character.NON_SPACING_MARK && type!=Character.COMBINING_SPACING_MARK && type!=Character.ENCLOSING_MARK)out.appendCodePoint(cp);});return out.toString();}
 private static boolean aligned(List<ImageTextSelection.Word> a,List<ImageTextSelection.Word> b){
  if(a==null || b==null || a.size()!=b.size())return false;
  for(int i=0;i<a.size();i++){ImageTextSelection.Word x=a.get(i),y=b.get(i);if(x==null || y==null || !nonLetters(x.text).equals(nonLetters(y.text)))return false;
   float intersection=Math.max(0,Math.min(x.right,y.right)-Math.max(x.left,y.left))*Math.max(0,Math.min(x.bottom,y.bottom)-Math.max(x.top,y.top));
   float union=(x.right-x.left)*(x.bottom-x.top)+(y.right-y.left)*(y.bottom-y.top)-intersection;
   if(intersection<.65f*union)return false;
  }return true;
 }
 // Adaptive thresholding may pad a line vertically. Corroboration must cover
 // the tight ordinary word, stay bounded, and never swallow another word centre.
 private static boolean corroborates(List<ImageTextSelection.Word> tight,List<ImageTextSelection.Word> padded){
  if(tight.size()!=padded.size())return false;
  for(int i=0;i<tight.size();i++){ImageTextSelection.Word a=tight.get(i),b=padded.get(i);
   float area=(a.right-a.left)*(a.bottom-a.top),other=(b.right-b.left)*(b.bottom-b.top);
   float intersection=Math.max(0,Math.min(a.right,b.right)-Math.max(a.left,b.left))*Math.max(0,Math.min(a.bottom,b.bottom)-Math.max(a.top,b.top));
   if(intersection<.8f*Math.min(area,other) || Math.max(area,other)>3*Math.min(area,other))return false;
   for(int j=0;j<tight.size();j++)if(j!=i){ImageTextSelection.Word w=tight.get(j);float x=(w.left+w.right)/2,y=(w.top+w.bottom)/2;if(x>b.left && x<b.right && y>b.top && y<b.bottom)return false;}
  }return true;
 }
 public static boolean prefer(String base,List<ImageTextSelection.Word> original,String ordinary,int ordinaryConfidence,List<ImageTextSelection.Word> ordinaryWords,String adaptive,int adaptiveConfidence,List<ImageTextSelection.Word> adaptiveWords){
  if(!eligible(base,original) || !eligible(ordinary,ordinaryWords) || !eligible(adaptive,adaptiveWords) || ordinaryConfidence<70 || ordinaryConfidence>100 || adaptiveConfidence<90 || adaptiveConfidence>100)return false;
  return !normalized(base).equals(normalized(adaptive)) && normalized(ordinary).equals(normalized(adaptive)) && aligned(original,ordinaryWords) && corroborates(ordinaryWords,adaptiveWords);
 }
}
