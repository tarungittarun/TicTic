package app.space.notes.runtime;

import android.content.Context;
import android.graphics.Canvas;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/** Bounded visual zoom; keeps lazy page raster allocation independent of pinch scale. */
class ZoomablePageImage extends ImageView {
    private float zoom=1,panX,panY,focusX,focusY;
    private float[] pendingZoom;
    private final ScaleGestureDetector pinch;
    private final GestureDetector gestures;
    ZoomablePageImage(Context context){
        super(context);setScaleType(ScaleType.FIT_CENTER);
        pinch=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector detector){
                // Preserve the content under the gesture's focal point as scale changes.
                float next=Math.max(1,Math.min(5,zoom*detector.getScaleFactor()));
                float x=detector.getFocusX(),y=detector.getFocusY();
                panX+=(zoom-1)*(x-focusX);panY+=(zoom-1)*(y-focusY);
                focusX=x;focusY=y;zoom=next;clamp();invalidate();return true;
            }
        });
        gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent event){return true;}
            @Override public boolean onSingleTapUp(MotionEvent event){performClick();return true;}
            @Override public void onLongPress(MotionEvent event){if(!pinch.isInProgress() && !onPageLongPress(event.getX(),event.getY()))performLongClick();}
            @Override public boolean onDoubleTap(MotionEvent event){zoom=zoom>1?1:2;focusX=event.getX();focusY=event.getY();panX=panY=0;invalidate();return true;}
            @Override public boolean onScroll(MotionEvent first,MotionEvent current,float dx,float dy){if(zoom<=1 || pinch.isInProgress())return false;panX-=dx;panY-=dy;clamp();invalidate();return true;}
        });
    }
    protected boolean onPageLongPress(float x,float y){return false;}
    protected android.graphics.Matrix imageToView(){android.graphics.Matrix matrix=new android.graphics.Matrix(getImageMatrix());matrix.postScale(zoom,zoom,focusX,focusY);matrix.postTranslate(panX,panY);return matrix;}
    private void clamp(){if(zoom<=1){panX=panY=0;return;}panX=Math.max(-(zoom-1)*(getWidth()-focusX),Math.min((zoom-1)*focusX,panX));panY=Math.max(-(zoom-1)*(getHeight()-focusY),Math.min((zoom-1)*focusY,panY));}
    void resetZoom(){pendingZoom=null;zoom=1;panX=panY=0;focusX=getWidth()/2f;focusY=getHeight()/2f;invalidate();}
    /** Five finite normalized numbers only: no raster, source bytes or document text. */
    float[] savedZoom(){
        if(pendingZoom!=null)return pendingZoom.clone();
        return new float[]{zoom,getWidth()>0?focusX/getWidth():.5f,getHeight()>0?focusY/getHeight():.5f,getWidth()>0?panX/getWidth():0,getHeight()>0?panY/getHeight():0};
    }
    void restoreZoom(float[] state){
        resetZoom();if(state==null || state.length!=5)return;
        for(float value:state)if(!Float.isFinite(value))return;
        pendingZoom=state.clone();applyPendingZoom();
    }
    private void applyPendingZoom(){
        if(pendingZoom==null || getWidth()<1 || getHeight()<1)return;
        float[] state=pendingZoom;pendingZoom=null;
        zoom=Math.max(1,Math.min(5,state[0]));focusX=Math.max(0,Math.min(1,state[1]))*getWidth();focusY=Math.max(0,Math.min(1,state[2]))*getHeight();
        panX=Math.max(-4,Math.min(4,state[3]))*getWidth();panY=Math.max(-4,Math.min(4,state[4]))*getHeight();clamp();invalidate();
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        if(pendingZoom!=null)applyPendingZoom();
        else if(oldw>0 && oldh>0){focusX*=w/(float)oldw;focusY*=h/(float)oldh;panX*=w/(float)oldw;panY*=h/(float)oldh;clamp();invalidate();}
    }
    float zoomFactor(){return zoom;}
    @Override public boolean onTouchEvent(MotionEvent event){
        if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(event.getPointerCount()>1 || zoom>1);
        pinch.onTouchEvent(event);gestures.onTouchEvent(event);
        if(event.getActionMasked()==MotionEvent.ACTION_UP || event.getActionMasked()==MotionEvent.ACTION_CANCEL){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override protected void onDraw(Canvas canvas){int state=canvas.save();canvas.translate(panX,panY);canvas.scale(zoom,zoom,focusX,focusY);super.onDraw(canvas);canvas.restoreToCount(state);}
}
