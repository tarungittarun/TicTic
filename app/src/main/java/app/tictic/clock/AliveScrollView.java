package app.tictic.clock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ScrollView;

/** Adds a restrained elastic pull at the edges and a short stretch impulse during scrolling. */
final class AliveScrollView extends ScrollView {
    private float density,lastFingerY,edgePull;
    private boolean dragging,edgeDragging;
    private final Runnable settleScroll=()->settle(false);

    AliveScrollView(Context context){super(context);init(context);}
    AliveScrollView(Context context,AttributeSet attrs){super(context,attrs);init(context);}
    AliveScrollView(Context context,AttributeSet attrs,int style){super(context,attrs,style);init(context);}

    private void init(Context context){
        density=context.getResources().getDisplayMetrics().density;
        setOverScrollMode(View.OVER_SCROLL_NEVER);
        setFillViewport(true);
        setClipChildren(true);
        setClipToPadding(false);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){lastFingerY=event.getY();dragging=false;edgePull=0f;}
        boolean intercept=super.onInterceptTouchEvent(event);
        if(intercept)dragging=true;
        return intercept;
    }

    @Override public boolean performClick(){return super.performClick();}

    // Drag gestures are owned by this scroll container; item taps stay with its child views.
    @SuppressLint("ClickableViewAccessibility")
    @Override public boolean onTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){lastFingerY=event.getY();dragging=true;edgePull=0f;}
        else if(action==MotionEvent.ACTION_MOVE&&dragging){
            float fingerDelta=event.getY()-lastFingerY;
            lastFingerY=event.getY();
            View content=getChildAt(0);
            if(content!=null){
                boolean atTop=getScrollY()<=0;
                boolean atBottom=getScrollY()+getHeight()>=content.getHeight()-1;
                if((atTop&&fingerDelta>0f)||(atBottom&&fingerDelta<0f)){
                    edgeDragging=true;
                    edgePull=Math.max(-dp(22),Math.min(dp(22),edgePull+fingerDelta*.22f));
                    float pivot=edgePull>=0f?0f:content.getHeight();
                    content.setPivotY(pivot);
                    content.setTranslationY(edgePull);
                    content.setScaleY(1f+Math.min(.032f,Math.abs(edgePull)/Math.max(1f,content.getHeight())*.82f));
                }else if(edgeDragging){
                    edgeDragging=false;
                    edgePull=0f;
                    settle(true);
                }
            }
        }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
            dragging=false;
            edgeDragging=false;
            edgePull=0f;
            settle(true);
        }
        return super.onTouchEvent(event);
    }

    @Override protected void onScrollChanged(int x,int y,int oldX,int oldY){
        super.onScrollChanged(x,y,oldX,oldY);
        View content=getChildAt(0);
        if(content==null||edgeDragging||y==oldY)return;
        content.removeCallbacks(settleScroll);
        content.setPivotY(y>oldY?content.getHeight():0f);
        float impulse=Math.min(.014f,Math.abs(y-oldY)*.00032f);
        content.setScaleY(1f+impulse);
        content.postDelayed(settleScroll,88);
    }

    private int dp(float value){return (int)(value*density+.5f);}
    private void settle(boolean spring){
        View content=getChildAt(0);
        if(content==null)return;
        content.removeCallbacks(settleScroll);
        content.animate().cancel();
        content.animate().translationY(0f).scaleY(1f)
                .setDuration(spring?360:180)
                .setInterpolator(spring?new OvershootInterpolator(.72f):new DecelerateInterpolator(1.3f))
                .withEndAction(()->content.setLayerType(View.LAYER_TYPE_NONE,null)).start();
    }
}
