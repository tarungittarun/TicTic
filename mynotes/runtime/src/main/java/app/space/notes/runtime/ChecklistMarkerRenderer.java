package app.space.notes.runtime;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import app.space.notes.domain.ChecklistAppearance;

/** Shared vector drawing for native checklist controls and compact card previews. */
final class ChecklistMarkerRenderer {
    private ChecklistMarkerRenderer(){}

    static int color(ChecklistAppearance appearance,NotesDesign design){
        return appearance.themeColor==ChecklistAppearance.DEFAULT_THEME_COLOR?design.accent:NotesBoard.colour(appearance.themeColor,false);
    }

    static void draw(Canvas canvas,float centerX,float centerY,float size,
                     ChecklistAppearance.Glyph glyph,int color,int outline,int foreground,
                     boolean checked,int alpha,Paint paint){
        if(size<=0)return;
        float stroke=Math.max(1.2f,size*.105f),inset=size*.13f;
        RectF bounds=new RectF(centerX-size/2+inset,centerY-size/2+inset,
                centerX+size/2-inset,centerY+size/2-inset);
        paint.setAntiAlias(true);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(withAlpha(color,alpha));
        if(glyph==ChecklistAppearance.Glyph.CHECK){
            float radius=size*.16f;
            if(checked){
                paint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(bounds,radius,radius,paint);
                paint.setColor(withAlpha(outline,alpha));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,size*.055f));canvas.drawRoundRect(bounds,radius,radius,paint);
                paint.setColor(withAlpha(foreground,alpha));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1.6f,size*.13f));
                Path tick=new Path();tick.moveTo(centerX-size*.25f,centerY);tick.lineTo(centerX-size*.06f,centerY+size*.19f);tick.lineTo(centerX+size*.28f,centerY-size*.2f);canvas.drawPath(tick,paint);
            }else{
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke*1.45f);paint.setColor(withAlpha(outline,alpha));canvas.drawRoundRect(bounds,radius,radius,paint);
                paint.setStrokeWidth(stroke*.72f);paint.setColor(withAlpha(color,alpha));canvas.drawRoundRect(bounds,radius,radius,paint);
            }
            paint.setStyle(Paint.Style.FILL);return;
        }
        Path shape=new Path();
        switch(glyph){
            case DOT:shape.addCircle(centerX,centerY,size*.31f,Path.Direction.CW);break;
            case SQUARE:shape.addRect(bounds,Path.Direction.CW);break;
            case DIAMOND:
                shape.moveTo(centerX,centerY-size*.38f);shape.lineTo(centerX+size*.38f,centerY);
                shape.lineTo(centerX,centerY+size*.38f);shape.lineTo(centerX-size*.38f,centerY);shape.close();break;
            case STAR:
                for(int point=0;point<10;point++){
                    double angle=-Math.PI/2+point*Math.PI/5;float radius=point%2==0?size*.39f:size*.18f;
                    float x=centerX+(float)Math.cos(angle)*radius,y=centerY+(float)Math.sin(angle)*radius;
                    if(point==0)shape.moveTo(x,y);else shape.lineTo(x,y);
                }shape.close();break;
            default:throw new IllegalArgumentException("Unsupported checklist marker");
        }
        if(checked){
            paint.setStyle(Paint.Style.FILL);paint.setColor(withAlpha(color,alpha));canvas.drawPath(shape,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,size*.055f));paint.setColor(withAlpha(outline,alpha));canvas.drawPath(shape,paint);
        }else{
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke*1.45f);paint.setColor(withAlpha(outline,alpha));canvas.drawPath(shape,paint);
            paint.setStrokeWidth(stroke*.72f);paint.setColor(withAlpha(color,alpha));canvas.drawPath(shape,paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private static int withAlpha(int color,int alpha){return (color&0x00ffffff)|(Math.max(0,Math.min(255,Color.alpha(color)*alpha/255))<<24);}
}
