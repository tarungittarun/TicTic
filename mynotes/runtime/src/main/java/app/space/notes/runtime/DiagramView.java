package app.space.notes.runtime;

import android.content.Context;
import android.graphics.Canvas;
import android.view.*;
import app.space.notes.domain.Diagram;

/** Drag handles commit once on UP; CANCEL never persists a half-finished move. */
public final class DiagramView extends View {
    public interface Listener {void selected(String id);void changed(Diagram diagram);}
    private Diagram diagram,original;private String selected;private final boolean dark;private final Listener listener;
    private float downX,downY;private Diagram.Node moving;private boolean dragged;
    public DiagramView(Context context,Diagram diagram,boolean dark,Listener listener){super(context);this.diagram=diagram;this.dark=dark;this.listener=listener;setBackgroundColor(android.graphics.Color.parseColor(dark?"#202124":"#FFFFFF"));setContentDescription("Flowchart canvas. Tap a shape to select. Drag a shape or group to move it. Edit shape and Group provide labelled tools.");setFocusable(true);}
    public void show(Diagram value,String selected){diagram=value;this.selected=selected;invalidate();}
    public Diagram diagram(){return diagram;}
    @Override protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w);setMeasuredDimension(width,Math.round(width*Diagram.HEIGHT/(float)Diagram.WIDTH));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();float scale=getWidth()/(float)Diagram.WIDTH;c.scale(scale,scale);DiagramRenderer.draw(c,diagram,dark,selected);c.restore();}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean onTouchEvent(android.view.MotionEvent event){
        float scale=getWidth()/(float)Diagram.WIDTH;if(scale<=0)return false;float x=event.getX()/scale,y=event.getY()/scale;
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                original=diagram;moving=null;dragged=false;downX=x;downY=y;
                for(int i=diagram.nodes.size()-1;i>=0;i--){Diagram.Node n=diagram.nodes.get(i);if(x>=n.x && x<=n.x+n.width && y>=n.y && y<=n.y+n.height){moving=n;selected=n.id;listener.selected(n.id);getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;}}
                selected=null;listener.selected(null);invalidate();return true;
            case MotionEvent.ACTION_MOVE:
                if(moving!=null && (dragged || Math.hypot(x-downX,y-downY)>8)){dragged=true;diagram=original.move(moving.id,Math.round(x-downX),Math.round(y-downY));invalidate();}return true;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);if(dragged)listener.changed(diagram);else performClick();moving=null;return true;
            case MotionEvent.ACTION_CANCEL:
                if(original!=null)diagram=original;moving=null;getParent().requestDisallowInterceptTouchEvent(false);invalidate();return true;
            default:return true;
        }
    }
}
