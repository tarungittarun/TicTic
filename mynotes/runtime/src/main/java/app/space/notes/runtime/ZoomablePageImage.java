package app.space.notes.runtime;

import android.content.Context;
import android.graphics.Canvas;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/** Bounded visual zoom; keeps lazy page raster allocation independent of pinch scale. */
final class ZoomablePageImage extends ImageView {
    private float zoom=1,panX,panY,focusX,focusY;
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
            @Override public void onLongPress(MotionEvent event){if(!pinch.isInProgress())performLongClick();}
            @Override public boolean onDoubleTap(MotionEvent event){zoom=zoom>1?1:2;focusX=event.getX();focusY=event.getY();panX=panY=0;invalidate();return true;}
            @Override public boolean onScroll(MotionEvent first,MotionEvent current,float dx,float dy){if(zoom<=1 || pinch.isInProgress())return false;panX-=dx;panY-=dy;clamp();invalidate();return true;}
        });
    }
    private void clamp(){if(zoom<=1){panX=panY=0;return;}panX=Math.max(-(zoom-1)*(getWidth()-focusX),Math.min((zoom-1)*focusX,panX));panY=Math.max(-(zoom-1)*(getHeight()-focusY),Math.min((zoom-1)*focusY,panY));}
    void resetZoom(){zoom=1;panX=panY=0;invalidate();}
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
