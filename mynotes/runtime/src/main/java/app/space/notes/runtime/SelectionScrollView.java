package app.space.notes.runtime;

import android.content.Context;
import android.text.Layout;
import android.view.*;
import android.widget.*;

/** Defer blank/unselected-area taps so a scroll never collapses a native range.
 * Real taps retain normal caret behavior; endpoint gestures stay in Android's editor.
 */
final class SelectionScrollView extends ScrollView {
    private Runnable userGesture;
    void onUserGesture(Runnable listener){userGesture=listener;}
    private EditText manualRangeField;private int manualAnchor,manualFocus;
    void clearManualRangeViewport(){manualRangeField=null;}
    boolean manualRangeActive(View focus){return focus!=null && focus==manualRangeField && manualRangeField.hasSelection() && manualRangeField.getSelectionStart()==manualAnchor && manualRangeField.getSelectionEnd()==manualFocus;}
    private void rememberManualRange(){View focus=findFocus();if(focus instanceof EditText && ((EditText)focus).hasSelection()){manualRangeField=(EditText)focus;manualAnchor=manualRangeField.getSelectionStart();manualFocus=manualRangeField.getSelectionEnd();}}
    @Override public boolean requestChildRectangleOnScreen(View child,android.graphics.Rect rectangle,boolean immediate){if(manualRangeActive(findFocus()))return false;return super.requestChildRectangleOnScreen(child,rectangle,immediate);}
    @Override public void requestChildFocus(View child,View focused){int x=getScrollX(),y=getScrollY();boolean keep=manualRangeActive(focused);super.requestChildFocus(child,focused);if(keep && manualRangeActive(focused))scrollTo(x,y);}
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){int x=getScrollX(),y=getScrollY();boolean keep=manualRangeActive(findFocus());super.onLayout(changed,left,top,right,bottom);if(keep && manualRangeActive(findFocus()))scrollTo(x,y);}
    private MotionEvent down;private float lastY;private boolean scrolling;private VelocityTracker velocity;
    private final Runnable passHold=()->{if(down!=null && !scrolling){MotionEvent held=MotionEvent.obtain(down);release();super.dispatchTouchEvent(held);held.recycle();}};
    SelectionScrollView(Context context){super(context);}
    private boolean nearEndpoint(EditText text,int offset,MotionEvent event){
        Layout layout=text.getLayout();if(layout==null)return false;int[] origin=new int[2];text.getLocationOnScreen(origin);
        int line=layout.getLineForOffset(Math.max(0,Math.min(text.length(),offset)));
        float x=origin[0]+text.getTotalPaddingLeft()+layout.getPrimaryHorizontal(offset)-text.getScrollX();
        float y=origin[1]+text.getTotalPaddingTop()+layout.getLineBottom(line)-text.getScrollY();
        return Math.hypot(event.getRawX()-x,event.getRawY()-y)<32*getResources().getDisplayMetrics().density;
    }
    private boolean defer(MotionEvent event){
        View focus=findFocus();if(!(focus instanceof EditText))return false;EditText text=(EditText)focus;
        int a=text.getSelectionStart(),b=text.getSelectionEnd();if(a<0 || b<0 || a==b)return false;
        if(nearEndpoint(text,a,event)||nearEndpoint(text,b,event))return false;
        int[] at=new int[2];text.getLocationOnScreen(at);float x=event.getRawX()-at[0],y=event.getRawY()-at[1];
        if(x>=0 && y>=0 && x<text.getWidth() && y<text.getHeight()){
            int offset=text.getOffsetForPosition(x,y);if(offset>=Math.min(a,b)&&offset<=Math.max(a,b))return false;
        }
        return true;
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        // Deferred range-preserving gestures bypass child/onTouch listeners. The
        // editor must still cancel pending automatic caret reveal before scrollBy.
        if((action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_MOVE) && userGesture!=null)userGesture.run();
        if(action==MotionEvent.ACTION_DOWN){release();if(defer(event)){down=MotionEvent.obtain(event);lastY=event.getY();scrolling=false;velocity=VelocityTracker.obtain();velocity.addMovement(event);postDelayed(passHold,ViewConfiguration.getLongPressTimeout());return true;}}
        if(down==null)return super.dispatchTouchEvent(event);
        if(event.getPointerCount()>1){release();return super.dispatchTouchEvent(event);}
        velocity.addMovement(event);
        if(action==MotionEvent.ACTION_MOVE){
            if(!scrolling && Math.abs(event.getY()-down.getY())>ViewConfiguration.get(getContext()).getScaledTouchSlop()){scrolling=true;rememberManualRange();}
            if(scrolling)removeCallbacks(passHold);if(scrolling)scrollBy(0,Math.round(lastY-event.getY()));lastY=event.getY();return true;
        }
        if(action==MotionEvent.ACTION_UP){
            if(scrolling){velocity.computeCurrentVelocity(1000,ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity());fling(Math.round(-velocity.getYVelocity()));}
            else{super.dispatchTouchEvent(down);super.dispatchTouchEvent(event);}
            release();return true;
        }
        if(action==MotionEvent.ACTION_CANCEL){release();return true;}
        return true;
    }
    private void release(){removeCallbacks(passHold);if(down!=null){down.recycle();down=null;}if(velocity!=null){velocity.recycle();velocity=null;}scrolling=false;}
    @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
}
