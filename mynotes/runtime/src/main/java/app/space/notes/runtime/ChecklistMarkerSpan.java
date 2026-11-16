package app.space.notes.runtime;

import android.graphics.Paint;
import android.text.style.ReplacementSpan;
import app.space.notes.domain.ChecklistAppearance;

/** Visual-only replacement for the plain-text [ ]/[x] prefix in board-card excerpts. */
public final class ChecklistMarkerSpan extends ReplacementSpan {
    private final ChecklistAppearance.Glyph glyph;
    private final int themeColor,color,outline,foreground;
    private final float density;
    private final boolean checked;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);

    public ChecklistMarkerSpan(ChecklistAppearance appearance,boolean checked,int color,int outline,int foreground,float density){
        if(appearance==null)throw new IllegalArgumentException("Missing checklist appearance");
        glyph=appearance.glyph;themeColor=appearance.themeColor;this.checked=checked;
        this.color=color;this.outline=outline;this.foreground=foreground;this.density=density;
    }

    public ChecklistAppearance.Glyph glyph(){return glyph;}
    public int themeColor(){return themeColor;}
    public int color(){return color;}
    public boolean checked(){return checked;}

    @Override public int getSize(Paint textPaint,CharSequence text,int start,int end,Paint.FontMetricsInt metrics){
        return Math.max(1,Math.round(Math.max(14*density,textPaint.getTextSize()*.92f)+3*density));
    }
    @Override public void draw(android.graphics.Canvas canvas,CharSequence text,int start,int end,float x,int top,int y,int bottom,Paint textPaint){
        float height=Math.max(1,bottom-top);float size=Math.min(height*.88f,Math.max(10*density,textPaint.getTextSize()*.82f));
        ChecklistMarkerRenderer.draw(canvas,x+getSize(textPaint,text,start,end,null)/2f,(top+bottom)/2f,size,glyph,color,outline,foreground,checked,255,paint);
    }
}
