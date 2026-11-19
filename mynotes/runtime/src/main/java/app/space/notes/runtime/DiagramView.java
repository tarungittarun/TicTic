package app.space.notes.runtime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.*;
import app.space.notes.domain.Diagram;
import app.space.notes.domain.DiagramResize;

/** Drag handles commit once on UP; CANCEL never persists a half-finished edit. */
public final class DiagramView extends View {
    public interface Listener {void selected(String id);void changed(Diagram diagram);default void longPressed(String id){}}
    public static final float MAX_ZOOM=5f;
    private Diagram diagram,original;private String selected;private final boolean dark;private final NotesDesign design;private final Listener listener;
    private final ScaleGestureDetector scaleDetector;
    private float downX,downY,zoom=1,panX,panY,lastNavX,lastNavY,lastNavSpan;
    private Diagram.Node moving;private boolean dragged,navigating,scaledByDetector;private int resizeCorner=-1;
    private final Paint handles=new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean longPressed;private final Runnable showContext=this::showShapeContext;
    private void showShapeContext(){if(moving!=null && !dragged && resizeCorner<0){longPressed=true;performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);listener.longPressed(moving.id);}}
    @Override protected void onDetachedFromWindow(){removeCallbacks(showContext);super.onDetachedFromWindow();}
    public DiagramView(Context context,Diagram diagram,boolean dark,Listener listener){this(context,diagram,new NotesDesign(context,dark),listener);}
    public DiagramView(Context context,Diagram diagram,NotesDesign design,Listener listener){
        super(context);this.diagram=diagram;this.design=design;this.dark=design.dark;this.listener=listener;setBackgroundColor(design.surface);setContentDescription("Flowchart canvas. Starts fitted to the page. Pinch to zoom up to five times; drag with two fingers to pan. Tap a shape to select, drag inside to move, or drag a selected corner to resize. Use Fit diagram to return to the whole page. Long press a shape or group for Duplicate and Delete.");setFocusable(true);
        scaleDetector=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScaleBegin(ScaleGestureDetector detector){return isEnabled() && getWidth()>0 && getHeight()>0;}
            @Override public boolean onScale(ScaleGestureDetector detector){scaledByDetector=true;zoomAt(detector.getFocusX(),detector.getFocusY(),detector.getScaleFactor());invalidate();androidx.core.view.ViewCompat.setStateDescription(DiagramView.this,Math.round(zoom*100)+" percent zoom");return true;}
        });
    }
    public void show(Diagram value,String selected){diagram=value;this.selected=selected;fitToScreen();invalidate();}
    public Diagram diagram(){return diagram;}
    @Override protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w),desired=Math.round(width*Diagram.HEIGHT/(float)Diagram.WIDTH),mode=MeasureSpec.getMode(h),height=mode==MeasureSpec.EXACTLY?MeasureSpec.getSize(h):mode==MeasureSpec.AT_MOST?Math.min(desired,MeasureSpec.getSize(h)):desired;setMeasuredDimension(width,height);}
    private float fitScale(){return Math.min(getWidth()/(float)Diagram.WIDTH,getHeight()/(float)Diagram.HEIGHT);}
    public float contentScale(){return fitScale()*zoom;}
    public float zoomScale(){return zoom;}
    public float contentOffsetX(){float scale=fitScale();return Math.max(0,(getWidth()-Diagram.WIDTH*scale)/2f)+panX;}
    public float contentOffsetY(){float scale=fitScale();return Math.max(0,(getHeight()-Diagram.HEIGHT*scale)/2f)+panY;}
    public float viewX(float diagramX){return contentOffsetX()+diagramX*contentScale();}
    public float viewY(float diagramY){return contentOffsetY()+diagramY*contentScale();}
    public void fitToScreen(){zoom=1;panX=panY=0;invalidate();androidx.core.view.ViewCompat.setStateDescription(this,"Fitted to the whole diagram");}
    public void fit(){fitToScreen();}
    /** Programmatic zoom controls use the centre; gestures continue to zoom around the fingers. */
    public void zoomBy(float factor){if(!Float.isFinite(factor) || factor<=0 || getWidth()<=0 || getHeight()<=0)return;zoomAt(getWidth()/2f,getHeight()/2f,factor);invalidate();androidx.core.view.ViewCompat.setStateDescription(this,Math.round(zoom*100)+" percent zoom");}
    private void clampPan(){
        if(getWidth()<=0 || getHeight()<=0)return;float base=fitScale();if(base<=0)return;float scale=base*zoom,baseX=Math.max(0,(getWidth()-Diagram.WIDTH*base)/2f),baseY=Math.max(0,(getHeight()-Diagram.HEIGHT*base)/2f);
        float minX=Math.min(0,getWidth()-Diagram.WIDTH*scale)-baseX,maxX=Math.max(0,getWidth()-Diagram.WIDTH*scale)-baseX;
        float minY=Math.min(0,getHeight()-Diagram.HEIGHT*scale)-baseY,maxY=Math.max(0,getHeight()-Diagram.HEIGHT*scale)-baseY;
        panX=Math.max(minX,Math.min(maxX,panX));panY=Math.max(minY,Math.min(maxY,panY));
    }
    private void zoomAt(float focusX,float focusY,float factor){
        if(getWidth()<=0 || getHeight()<=0)return;float base=fitScale();if(base<=0)return;float oldScale=base*zoom,pointX=(focusX-contentOffsetX())/oldScale,pointY=(focusY-contentOffsetY())/oldScale;
        float next=Math.max(1,Math.min(MAX_ZOOM,zoom*factor));if(next==zoom)return;zoom=next;float baseX=Math.max(0,(getWidth()-Diagram.WIDTH*base)/2f),baseY=Math.max(0,(getHeight()-Diagram.HEIGHT*base)/2f);
        panX=focusX-pointX*base*zoom-baseX;panY=focusY-pointY*base*zoom-baseY;clampPan();
    }
    private static float centroid(MotionEvent event,boolean x){float total=0;for(int i=0;i<event.getPointerCount();i++)total+=x?event.getX(i):event.getY(i);return total/Math.max(1,event.getPointerCount());}
    private static float pointerSpan(MotionEvent event){if(event.getPointerCount()<2)return 0;return (float)Math.hypot(event.getX(0)-event.getX(1),event.getY(0)-event.getY(1));}
    private void disallowParent(boolean value){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(value);}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);clampPan();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float scale=contentScale(),offsetX=contentOffsetX(),offsetY=contentOffsetY();if(scale<=0)return;
        c.save();c.translate(offsetX,offsetY);c.scale(scale,scale);DiagramRenderer.draw(c,diagram,dark,selected,design);c.restore();
        Diagram.Node node=diagram.find(selected);if(node!=null){handles.setColor(design.accent);float radius=design.dp(5);for(int corner=0;corner<4;corner++){float x=offsetX+(node.x+((corner==1 || corner==2)?node.width:0))*scale,y=offsetY+(node.y+(corner>=2?node.height:0))*scale;c.drawRoundRect(x-radius,y-radius,x+radius,y+radius,design.dp(2),design.dp(2),handles);}}
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private boolean navigate(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_MOVE){
            float x=centroid(event,true),y=centroid(event,false),span=pointerSpan(event);
            if(event.getPointerCount()>1){
                // A manual fallback keeps small pinches usable below Android's span slop.
                if(!scaledByDetector && lastNavSpan>0 && span>0){float factor=span/lastNavSpan;if(Float.isFinite(factor) && Math.abs(factor-1f)>.003f)zoomAt(lastNavX,lastNavY,factor);}
                panX+=x-lastNavX;panY+=y-lastNavY;clampPan();invalidate();lastNavSpan=span;
            }else{
                if(zoom>1){panX+=x-lastNavX;panY+=y-lastNavY;clampPan();invalidate();}
                lastNavSpan=0;
            }
            lastNavX=x;lastNavY=y;return true;
        }
        if(action==MotionEvent.ACTION_POINTER_UP){lastNavSpan=0;int remaining=event.getActionIndex()==0?1:0;if(remaining<event.getPointerCount()){lastNavX=event.getX(remaining);lastNavY=event.getY(remaining);}return true;}
        if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL){navigating=false;lastNavSpan=0;disallowParent(false);if(action==MotionEvent.ACTION_UP)performClick();return true;}
        return true;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!isEnabled() || getWidth()<=0 || getHeight()<=0)return false;
        scaledByDetector=false;scaleDetector.onTouchEvent(event);int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_POINTER_DOWN){
            removeCallbacks(showContext);longPressed=false;if(moving!=null && dragged && original!=null){diagram=original;invalidate();}
            moving=null;resizeCorner=-1;dragged=false;navigating=true;lastNavX=centroid(event,true);lastNavY=centroid(event,false);lastNavSpan=pointerSpan(event);disallowParent(true);return true;
        }
        if(navigating)return navigate(event);
        float scale=contentScale();if(scale<=0)return false;float x=(event.getX()-contentOffsetX())/scale,y=(event.getY()-contentOffsetY())/scale;
        switch(action){
            case MotionEvent.ACTION_DOWN:
                removeCallbacks(showContext);longPressed=false;original=diagram;moving=null;resizeCorner=-1;dragged=false;downX=x;downY=y;
                Diagram.Node selectedNode=diagram.find(selected);
                if(selectedNode!=null && !(x>selectedNode.x+selectedNode.width*.15f && x<selectedNode.x+selectedNode.width*.85f && y>selectedNode.y+selectedNode.height*.15f && y<selectedNode.y+selectedNode.height*.85f)){
                    float nearest=design.dp(24)/scale;for(int corner=0;corner<4;corner++){float hx=selectedNode.x+((corner==1 || corner==2)?selectedNode.width:0),hy=selectedNode.y+(corner>=2?selectedNode.height:0);float distance=(float)Math.hypot(x-hx,y-hy);if(distance<nearest){nearest=distance;resizeCorner=corner;}}
                    if(resizeCorner>=0){moving=selectedNode;disallowParent(true);return true;}
                }
                for(int i=diagram.nodes.size()-1;i>=0;i--){Diagram.Node n=diagram.nodes.get(i);if(x>=n.x && x<=n.x+n.width && y>=n.y && y<=n.y+n.height){moving=n;selected=n.id;listener.selected(n.id);postDelayed(showContext,ViewConfiguration.getLongPressTimeout());disallowParent(true);invalidate();return true;}}
                selected=null;listener.selected(null);invalidate();
                if(zoom>1){navigating=true;lastNavX=event.getX();lastNavY=event.getY();lastNavSpan=0;disallowParent(true);return true;}
                return true;
            case MotionEvent.ACTION_MOVE:
                if(moving==null)return navigating?navigate(event):true;
                if(longPressed)return true;
                if(Math.hypot(x-downX,y-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop()/scale)removeCallbacks(showContext);
                if(moving!=null && (dragged || Math.hypot(x-downX,y-downY)>8)){dragged=true;diagram=resizeCorner>=0?original.put(DiagramResize.corner(moving,resizeCorner,Math.round(x-downX),Math.round(y-downY))):original.move(moving.id,Math.round(x-downX),Math.round(y-downY));invalidate();}
                return true;
            case MotionEvent.ACTION_UP:
                removeCallbacks(showContext);disallowParent(false);if(dragged && !longPressed)listener.changed(diagram);else if(!longPressed)performClick();moving=null;original=null;return true;
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(showContext);if(original!=null)diagram=original;moving=null;original=null;disallowParent(false);invalidate();return true;
            default:return true;
        }
    }
}
