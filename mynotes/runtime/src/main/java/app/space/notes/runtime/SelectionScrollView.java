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
    private EditText rememberedSelectionField;private int rememberedSelectionStart=-1,rememberedSelectionEnd=-1;
    void clearManualRangeViewport(){manualRangeField=null;clearRememberedSelection();}
    private void clearRememberedSelection(){rememberedSelectionField=null;rememberedSelectionStart=rememberedSelectionEnd=-1;}
    void clearEditorSelection(EditText input){if(input==rememberedSelectionField)clearRememberedSelection();if(input==manualRangeField)manualRangeField=null;}
    /** A newly focused editor supersedes a range retained during another editor's focus handoff. */
    void onEditorFocus(EditText input){if(input!=rememberedSelectionField)clearRememberedSelection();if(input!=manualRangeField)manualRangeField=null;}
    /** Keep a native range through temporary ActionMode/window-focus handoffs; retire it after the editor owns focus and the user collapses it. */
    void observeEditorSelection(EditText input){if(input==null)return;if(input.hasSelection()){int start=input.getSelectionStart(),end=input.getSelectionEnd();rememberedSelectionField=input;rememberedSelectionStart=start;rememberedSelectionEnd=end;if(manualRangeField==input && (manualAnchor!=start || manualFocus!=end))manualRangeField=null;}else if(input.hasFocus() && input.hasWindowFocus())clearEditorSelection(input);}
    private boolean selectionRange(EditText input,int[] range){if(input==null)return false;if(input.hasSelection()){range[0]=input.getSelectionStart();range[1]=input.getSelectionEnd();if(range[0]!=range[1])return true;}if(manualRangeField==input && manualAnchor!=manualFocus){range[0]=manualAnchor;range[1]=manualFocus;return true;}if(rememberedSelectionField==input && rememberedSelectionStart>=0 && rememberedSelectionEnd>=0 && rememberedSelectionStart!=rememberedSelectionEnd && rememberedSelectionStart<=input.length() && rememberedSelectionEnd<=input.length()){range[0]=rememberedSelectionStart;range[1]=rememberedSelectionEnd;return true;}return false;}
    private EditText findSelectedEditor(View view){if(view instanceof EditText){EditText input=(EditText)view;int[] range=new int[2];if(input.isShown() && selectionRange(input,range))return input;}if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=group.getChildCount()-1;i>=0;i--){EditText found=findSelectedEditor(group.getChildAt(i));if(found!=null)return found;}}return null;}
    private EditText selectedEditor(View preferred){int[] range=new int[2];if(preferred instanceof EditText && selectionRange((EditText)preferred,range))return (EditText)preferred;if(manualRangeField!=null && manualRangeField.isAttachedToWindow() && selectionRange(manualRangeField,range))return manualRangeField;if(rememberedSelectionField!=null && rememberedSelectionField.isAttachedToWindow() && selectionRange(rememberedSelectionField,range))return rememberedSelectionField;return findSelectedEditor(this);}
    boolean manualRangeActive(View focus){EditText owner=selectedEditor(focus);int[] range=new int[2];return manualRangeField!=null && owner==manualRangeField && selectionRange(manualRangeField,range) && range[0]==manualAnchor && range[1]==manualFocus;}
    private void rememberManualRange(){EditText owner=selectedEditor(findFocus());int[] range=new int[2];if(owner!=null && selectionRange(owner,range)){manualRangeField=owner;manualAnchor=range[0];manualFocus=range[1];}}
    @Override public boolean requestChildRectangleOnScreen(View child,android.graphics.Rect rectangle,boolean immediate){if(manualRangeActive(findFocus()))return false;return super.requestChildRectangleOnScreen(child,rectangle,immediate);}
    @Override public void requestChildFocus(View child,View focused){int x=getScrollX(),y=getScrollY();boolean keep=manualRangeActive(focused);super.requestChildFocus(child,focused);if(keep && manualRangeActive(focused))scrollTo(x,y);}
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){int x=getScrollX(),y=getScrollY();boolean keep=manualRangeActive(findFocus());super.onLayout(changed,left,top,right,bottom);if(keep && manualRangeActive(findFocus()))scrollTo(x,y);}
    private MotionEvent down;private float lastY;private boolean scrolling;private VelocityTracker velocity;
    private boolean lastGestureDeferred;private String lastDeferralReason="idle";private int deferredMoveCount,deferredStartScroll,deferredMaxScroll,deferredLastDelta,deferredScrollBefore,deferredScrollAfter;
    String gestureDiagnostics(){View focused=findFocus();EditText owner=selectedEditor(focused);boolean active=manualRangeActive(focused);return "gestureDeferred="+lastGestureDeferred+" reason="+lastDeferralReason+" moves="+deferredMoveCount+" scroll="+deferredStartScroll+".."+deferredMaxScroll+" lastDelta="+deferredLastDelta+" step="+deferredScrollBefore+"→"+deferredScrollAfter+" focused="+(focused==null?"none":focused.getClass().getSimpleName())+" selectionOwner="+(owner==null?"none":owner.getTag())+" rememberedOwner="+(rememberedSelectionField==null?"none":rememberedSelectionField.getTag())+" remembered="+rememberedSelectionStart+".."+rememberedSelectionEnd+" manualOwner="+(manualRangeField==owner)+" manual="+manualAnchor+".."+manualFocus+" active="+active;}
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
        View focused=findFocus();EditText text=selectedEditor(focused);if(text==null){lastDeferralReason=focused instanceof EditText?"focused-editor-has-no-selection":"no-selected-editor";return false;}String owner=focused==text?"focused-editor":"selected-editor-fallback";
        int[] range=new int[2];if(!selectionRange(text,range)){lastDeferralReason=owner+"-no-selection";return false;}int a=range[0],b=range[1];
        if(nearEndpoint(text,a,event)||nearEndpoint(text,b,event)){lastDeferralReason=owner+"-near-selection-endpoint";return false;}
        int[] at=new int[2];text.getLocationOnScreen(at);float x=event.getRawX()-at[0],y=event.getRawY()-at[1];
        if(x>=0 && y>=0 && x<text.getWidth() && y<text.getHeight()){
            int offset=text.getOffsetForPosition(x,y);if(offset>=Math.min(a,b)&&offset<=Math.max(a,b)){lastDeferralReason=owner+"-touch-in-selected-range:"+offset;return false;}lastDeferralReason=owner+"-unselected-offset:"+offset;
        }else lastDeferralReason=owner+"-outside-writing-field";
        return true;
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        // Deferred range-preserving gestures bypass child/onTouch listeners. The
        // editor must still cancel pending automatic caret reveal before scrollBy.
        if((action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_MOVE) && userGesture!=null)userGesture.run();
        if(action==MotionEvent.ACTION_DOWN){release();lastGestureDeferred=defer(event);deferredMoveCount=0;deferredStartScroll=getScrollY();deferredMaxScroll=deferredStartScroll;deferredLastDelta=0;deferredScrollBefore=deferredStartScroll;deferredScrollAfter=deferredStartScroll;if(lastGestureDeferred){down=MotionEvent.obtain(event);lastY=event.getY();scrolling=false;velocity=VelocityTracker.obtain();velocity.addMovement(event);postDelayed(passHold,ViewConfiguration.getLongPressTimeout());return true;}}
        if(down==null)return super.dispatchTouchEvent(event);
        if(event.getPointerCount()>1){release();return super.dispatchTouchEvent(event);}
        velocity.addMovement(event);
        if(action==MotionEvent.ACTION_MOVE){
            deferredMoveCount++;if(!scrolling && Math.abs(event.getY()-down.getY())>ViewConfiguration.get(getContext()).getScaledTouchSlop()){scrolling=true;rememberManualRange();}
            if(scrolling)removeCallbacks(passHold);deferredLastDelta=scrolling?Math.round(lastY-event.getY()):0;deferredScrollBefore=getScrollY();if(scrolling)scrollBy(0,deferredLastDelta);deferredScrollAfter=getScrollY();deferredMaxScroll=Math.max(deferredMaxScroll,deferredScrollAfter);lastY=event.getY();return true;
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
    @Override protected void onDetachedFromWindow(){release();clearManualRangeViewport();super.onDetachedFromWindow();}
}
