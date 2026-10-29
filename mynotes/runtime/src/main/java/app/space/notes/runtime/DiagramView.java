package app.space.notes.runtime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import app.space.notes.domain.DiagramResize;
import android.view.*;
import app.space.notes.domain.Diagram;

/** Drag handles commit once on UP; CANCEL never persists a half-finished move. */
public final class DiagramView extends View {
    public interface Listener {void selected(String id);void changed(Diagram diagram);}
    private Diagram diagram,original;private String selected;private final boolean dark;private final NotesDesign design;private final Listener listener;
    private float downX,downY;private Diagram.Node moving;private boolean dragged;private int resizeCorner=-1;private final Paint handles=new Paint(Paint.ANTI_ALIAS_FLAG);
    public DiagramView(Context context,Diagram diagram,boolean dark,Listener listener){this(context,diagram,new NotesDesign(context,dark),listener);}
    public DiagramView(Context context,Diagram diagram,NotesDesign design,Listener listener){super(context);this.diagram=diagram;this.design=design;this.dark=design.dark;this.listener=listener;setBackgroundColor(design.surface);setContentDescription("Flowchart canvas. Tap a shape to select. Drag inside to move. Drag a selected shape corner to resize. Use Edit shape for labels and connectors. Group shapes is in Diagram options.");setFocusable(true);}
    public void show(Diagram value,String selected){diagram=value;this.selected=selected;invalidate();}
    public Diagram diagram(){return diagram;}
    @Override protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w);setMeasuredDimension(width,Math.round(width*Diagram.HEIGHT/(float)Diagram.WIDTH));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();float scale=getWidth()/(float)Diagram.WIDTH;c.scale(scale,scale);DiagramRenderer.draw(c,diagram,dark,selected,design);c.restore();Diagram.Node node=diagram.find(selected);if(node!=null){handles.setColor(design.accent);float radius=design.dp(5);for(int corner=0;corner<4;corner++){float x=(node.x+((corner==1 || corner==2)?node.width:0))*scale,y=(node.y+(corner>=2?node.height:0))*scale;c.drawRoundRect(x-radius,y-radius,x+radius,y+radius,design.dp(2),design.dp(2),handles);}}}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean onTouchEvent(android.view.MotionEvent event){
        if(!isEnabled())return false;float scale=getWidth()/(float)Diagram.WIDTH;if(scale<=0)return false;float x=event.getX()/scale,y=event.getY()/scale;
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                original=diagram;moving=null;resizeCorner=-1;dragged=false;downX=x;downY=y;Diagram.Node selectedNode=diagram.find(selected);if(selectedNode!=null && !(x>selectedNode.x+selectedNode.width*.15f && x<selectedNode.x+selectedNode.width*.85f && y>selectedNode.y+selectedNode.height*.15f && y<selectedNode.y+selectedNode.height*.85f)){float nearest=design.dp(24)/scale;for(int corner=0;corner<4;corner++){float hx=selectedNode.x+((corner==1 || corner==2)?selectedNode.width:0),hy=selectedNode.y+(corner>=2?selectedNode.height:0);float distance=(float)Math.hypot(x-hx,y-hy);if(distance<nearest){nearest=distance;resizeCorner=corner;}}if(resizeCorner>=0){moving=selectedNode;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return true;}}
                for(int i=diagram.nodes.size()-1;i>=0;i--){Diagram.Node n=diagram.nodes.get(i);if(x>=n.x && x<=n.x+n.width && y>=n.y && y<=n.y+n.height){moving=n;selected=n.id;listener.selected(n.id);getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;}}
                selected=null;listener.selected(null);invalidate();return true;
            case MotionEvent.ACTION_MOVE:
                if(moving!=null && (dragged || Math.hypot(x-downX,y-downY)>8)){dragged=true;diagram=resizeCorner>=0?original.put(DiagramResize.corner(moving,resizeCorner,Math.round(x-downX),Math.round(y-downY))):original.move(moving.id,Math.round(x-downX),Math.round(y-downY));invalidate();}return true;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);if(dragged)listener.changed(diagram);else performClick();moving=null;return true;
            case MotionEvent.ACTION_CANCEL:
                if(original!=null)diagram=original;moving=null;getParent().requestDisallowInterceptTouchEvent(false);invalidate();return true;
            default:return true;
        }
    }
}
