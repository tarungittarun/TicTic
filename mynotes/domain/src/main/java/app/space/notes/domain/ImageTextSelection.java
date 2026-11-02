package app.space.notes.domain;
import java.util.*;
/** Normalized, source-reading-order OCR words. Selection never invents or rewrites text. */
public final class ImageTextSelection {
 public static final class Word {
  public final String text;public final float left,top,right,bottom;public final int line;
  public Word(String text,float l,float t,float r,float b,int line){if(text==null)throw new IllegalArgumentException("Missing OCR word");RichText.requireText(text);if(text.trim().isEmpty() || text.length()>2000 || !Float.isFinite(l) || !Float.isFinite(t) || !Float.isFinite(r) || !Float.isFinite(b) || l<0 || t<0 || r>1 || b>1 || l>=r || t>=b || line<0)throw new IllegalArgumentException("Invalid OCR word region");this.text=text;left=l;top=t;right=r;bottom=b;this.line=line;}
 }
 public final List<Word> words;private final boolean[] selected;
 public ImageTextSelection(List<Word> words){if(words==null || words.size()>4096)throw new IllegalArgumentException("Too many OCR regions");this.words=Collections.unmodifiableList(new ArrayList<>(words));for(Word word:this.words)Objects.requireNonNull(word);selected=new boolean[words.size()];}
 public void all(boolean value){Arrays.fill(selected,value);}
 public void toggle(int index){selected[index]=!selected[index];}
 public boolean selected(int index){return selected[index];}
 public int count(){int n=0;for(boolean value:selected)if(value)n++;return n;}
 public void rectangle(float l,float t,float r,float b){if(!Float.isFinite(l+t+r+b))throw new IllegalArgumentException("Invalid selection");float a=Math.min(l,r),c=Math.max(l,r),d=Math.min(t,b),e=Math.max(t,b);for(int i=0;i<words.size();i++){Word w=words.get(i);selected[i]=w.left<c && w.right>a && w.top<e && w.bottom>d;}}
 public String text(){StringBuilder out=new StringBuilder();int line=-1;for(int i=0;i<words.size();i++)if(selected[i]){Word word=words.get(i);if(out.length()+word.text.length()+1>100000)throw new IllegalArgumentException("Selected text exceeds safe length");if(out.length()>0)out.append(line==word.line?' ':'\n');out.append(word.text);line=word.line;}return out.toString();}
}
