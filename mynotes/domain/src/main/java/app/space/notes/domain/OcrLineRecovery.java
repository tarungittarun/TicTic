package app.space.notes.domain;
import java.util.*;

/** Bounded same-column native-script line recovery, not a correctness/language oracle. */
public final class OcrLineRecovery {
 private OcrLineRecovery(){}
 public static final class Recovered {
  public final String text;public final int confidence;public final List<ImageTextSelection.Word> words;
  Recovered(String text,int confidence,List<ImageTextSelection.Word> words){this.text=text;this.confidence=confidence;this.words=Collections.unmodifiableList(words);}
 }
 static Character.UnicodeScript nativeScript(String text){
  Character.UnicodeScript script=null;int letters=0;
  for(int cp:text.codePoints().toArray())if(Character.isLetter(cp)){
   Character.UnicodeScript current=Character.UnicodeScript.of(cp);
   if(current!=Character.UnicodeScript.KANNADA && current!=Character.UnicodeScript.DEVANAGARI)return null;
   if(script!=null && current!=script)return null;script=current;letters++;
  }
  return letters>=3?script:null;
 }
 public static boolean eligible(String baseline,int confidence,String candidate,int score){
  return baseline!=null && candidate!=null && baseline.length()<=100000 && candidate.length()<=100000 && confidence>=80 && confidence<=100 && score>=70 && score<=100 && baseline.trim().split("[\\r\\n]+").length==1 && nativeScript(baseline)!=null;
 }
 private static final class Line {
  final List<ImageTextSelection.Word> words=new ArrayList<>();float l=1,t=1,r=0,b=0;
  void add(ImageTextSelection.Word w){words.add(w);l=Math.min(l,w.left);t=Math.min(t,w.top);r=Math.max(r,w.right);b=Math.max(b,w.bottom);}
  String text(){StringBuilder out=new StringBuilder();for(ImageTextSelection.Word w:words){if(out.length()>0)out.append(' ');out.append(w.text);}return out.toString();}
 }
 private static List<Line> lines(List<ImageTextSelection.Word> words){
  Map<Integer,Line> lines=new LinkedHashMap<>();for(ImageTextSelection.Word word:words)lines.computeIfAbsent(word.line,k->new Line()).add(word);return new ArrayList<>(lines.values());
 }
 private static float overlap(float a,float b,float c,float d){return Math.max(0,Math.min(b,d)-Math.max(a,c));}
 public static Recovered recover(String baseline,int confidence,List<ImageTextSelection.Word> original,String candidate,int score,List<ImageTextSelection.Word> alternatives){
  if(!eligible(baseline,confidence,candidate,score) || original==null || alternatives==null || original.isEmpty() || alternatives.isEmpty() || original.size()+alternatives.size()>4096)return null;
  List<Line> base=lines(original),other=lines(alternatives);if(base.size()!=1 || other.size()!=2)return null;
  Line kept=base.get(0),matched=null,extra=null;
  for(Line line:other){if(overlap(kept.t,kept.b,line.t,line.b)>=.5f*Math.min(kept.b-kept.t,line.b-line.t) && overlap(kept.l,kept.r,line.l,line.r)>0){if(matched!=null)return null;matched=line;}else{if(extra!=null)return null;extra=line;}}
  if(matched==null || extra==null || nativeScript(extra.text())!=nativeScript(baseline))return null;
  float height=kept.b-kept.t,extraHeight=extra.b-extra.t;
  if(overlap(kept.t,kept.b,extra.t,extra.b)>0 || overlap(kept.l,kept.r,extra.l,extra.r)<.5f*Math.min(kept.r-kept.l,extra.r-extra.l) || extraHeight<height*.5f || extraHeight>height*2f)return null;
  boolean above=extra.b<=kept.t;float gap=above?kept.t-extra.b:extra.t-kept.b;if(gap>6*Math.max(height,extraHeight))return null;
  String added=extra.text(),text=above?added+"\n"+baseline:baseline+(baseline.endsWith("\n")?"":"\n")+added;
  if(text.length()>100000)return null;
  List<ImageTextSelection.Word> merged=new ArrayList<>();Line[] order=above?new Line[]{extra,kept}:new Line[]{kept,extra};
  for(int i=0;i<order.length;i++)for(ImageTextSelection.Word w:order[i].words)merged.add(new ImageTextSelection.Word(w.text,w.left,w.top,w.right,w.bottom,i));
  return new Recovered(text,Math.min(confidence,score),merged);
 }
}
