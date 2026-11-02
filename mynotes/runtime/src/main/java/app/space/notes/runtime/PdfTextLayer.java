package app.space.notes.runtime;
import android.graphics.RectF;
import java.util.*;
/** Embedded glyph geometry in normalized, unrotated page coordinates, not OCR guesses. */
final class PdfTextLayer {
 static final class Glyph {final String text;final RectF box;Glyph(String text,float l,float t,float r,float b){this.text=text;box=new RectF(l,t,r,b);}}
 final List<Glyph> glyphs;
 PdfTextLayer(List<Glyph> glyphs){this.glyphs=Collections.unmodifiableList(new ArrayList<>(glyphs));}
 int nearest(float x,float y){int best=-1;float distance=Float.MAX_VALUE;for(int i=0;i<glyphs.size();i++){RectF b=glyphs.get(i).box;float dx=Math.max(b.left-x,Math.max(0,x-b.right)),dy=Math.max(b.top-y,Math.max(0,y-b.bottom)),d=dx*dx+dy*dy;if(d<distance){best=i;distance=d;}}return best;}
 String selected(int a,int b){StringBuilder out=new StringBuilder();Glyph previous=null;for(int i=Math.min(a,b);i<=Math.max(a,b) && i<glyphs.size();i++){if(i<0)continue;Glyph g=glyphs.get(i);if(previous!=null && out.length()>0 && !Character.isWhitespace(out.charAt(out.length()-1)) && !g.text.isEmpty() && !Character.isWhitespace(g.text.charAt(0))){float h=Math.max(previous.box.height(),g.box.height());if(Math.abs(g.box.bottom-previous.box.bottom)>h*.55f)out.append('\n');else if(g.box.left-previous.box.right>h*.2f)out.append(' ');}out.append(g.text);previous=g;}return out.toString();}
}
