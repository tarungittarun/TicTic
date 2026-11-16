package app.space.notes.runtime;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import app.space.notes.domain.ChecklistAppearance;

/** Accessible CheckBox button glyph whose visual state follows Android's checked drawable state. */
public final class ChecklistMarkerDrawable extends Drawable {
    private static final int[] CHECKED_STATE={android.R.attr.state_checked};
    private final ChecklistAppearance.Glyph glyph;
    private final int color,outline,foreground,intrinsicSize;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean checked;
    private int alpha=255;
    private ColorFilter colorFilter;

    public ChecklistMarkerDrawable(ChecklistAppearance.Glyph glyph,int color,int outline,int foreground,float density){
        if(glyph==null)throw new IllegalArgumentException("Missing checklist marker");
        this.glyph=glyph;this.color=color;this.outline=outline;this.foreground=foreground;
        intrinsicSize=Math.max(1,Math.round(24*density));paint.setColorFilter(colorFilter);
    }

    public ChecklistAppearance.Glyph glyph(){return glyph;}
    public int color(){return color;}
    public boolean checked(){return checked;}

    @Override public void draw(Canvas canvas){
        android.graphics.Rect bounds=getBounds();float size=Math.max(0,Math.min(bounds.width(),bounds.height())-getBounds().width()*.12f);
        ChecklistMarkerRenderer.draw(canvas,bounds.exactCenterX(),bounds.exactCenterY(),size,glyph,color,outline,foreground,checked,alpha,paint);
    }
    @Override protected boolean onStateChange(int[] state){
        boolean next=false;for(int value:state)if(value==CHECKED_STATE[0]){next=true;break;}
        if(next==checked)return false;checked=next;invalidateSelf();return true;
    }
    @Override public boolean isStateful(){return true;}
    @Override public int getIntrinsicWidth(){return intrinsicSize;}
    @Override public int getIntrinsicHeight(){return intrinsicSize;}
    @Override public void setAlpha(int value){alpha=Math.max(0,Math.min(255,value));invalidateSelf();}
    @Override public void setColorFilter(ColorFilter value){colorFilter=value;paint.setColorFilter(value);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
