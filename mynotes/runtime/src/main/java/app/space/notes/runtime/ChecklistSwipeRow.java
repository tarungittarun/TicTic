package app.space.notes.runtime;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.LinearLayout;
import androidx.core.view.ViewCompat;

/** Checked checklist rows can be dismissed horizontally without stealing vertical scroll or drag handles. */
final class ChecklistSwipeRow extends LinearLayout {
    interface DeleteAction { boolean remove(); }

    private boolean deletable;
    private final DeleteAction deleteAction;
    private int deleteAccessibilityAction;
    private final int touchSlop,longPressTimeout;
    private final float density;
    private final GradientDrawable deleteBackground;
    private final Paint deleteLabel=new Paint(Paint.ANTI_ALIAS_FLAG);
    private float downX,downY,swipeOffset;
    private boolean tracking,swiping,excludedStart;
    private ValueAnimator settleAnimator;

    ChecklistSwipeRow(Context context,boolean deletable,NotesDesign design,DeleteAction deleteAction){
        super(context);
        this.deletable=deletable;this.deleteAction=deleteAction;
        density=getResources().getDisplayMetrics().density;
        touchSlop=ViewConfiguration.get(context).getScaledTouchSlop();longPressTimeout=ViewConfiguration.getLongPressTimeout();
        setOrientation(HORIZONTAL);setGravity(android.view.Gravity.CENTER_VERTICAL);setBaselineAligned(false);setWillNotDraw(false);
        int red=design.dark?0xff8d3030:0xffa83b32;
        deleteBackground=design.shape(red,12);
        deleteLabel.setColor(android.graphics.Color.WHITE);deleteLabel.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));deleteLabel.setTextSize(dp(14));updateDeleteAccessibilityAction();
    }

    private int dp(int value){return Math.round(value*density);}
    void setSwipeEnabled(boolean enabled){if(deletable==enabled)return;deletable=enabled;tracking=false;swiping=false;excludedStart=false;if(!enabled){disallowAncestors(false);cancelSettle();}updateDeleteAccessibilityAction();}
    private void updateDeleteAccessibilityAction(){if(deleteAccessibilityAction!=0){ViewCompat.removeAccessibilityAction(this,deleteAccessibilityAction);deleteAccessibilityAction=0;}if(deletable && deleteAction!=null)deleteAccessibilityAction=ViewCompat.addAccessibilityAction(this,"Delete checked item",(view,args)->deleteAction.remove());}
    private float clamp(float value){return Math.max(-getWidth(),Math.min(getWidth(),value));}
    private float threshold(){return Math.max(dp(88),Math.min(dp(144),getWidth()*.28f));}

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(swipeOffset==0)return;
        float distance=Math.abs(swipeOffset);if(distance<dp(36))return;
        String label="Delete";float width=deleteLabel.measureText(label);
        float center=swipeOffset<0?getWidth()-distance/2f:distance/2f;
        android.graphics.Paint.FontMetrics metrics=deleteLabel.getFontMetrics();
        float baseline=(getHeight()-(metrics.ascent+metrics.descent))/2f;
        canvas.drawText(label,center-width/2f,baseline,deleteLabel);
    }

    @Override protected void dispatchDraw(Canvas canvas){
        int save=canvas.save();if(swipeOffset!=0)canvas.translate(swipeOffset,0);super.dispatchDraw(canvas);canvas.restoreToCount(save);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        if(!deletable)return super.onInterceptTouchEvent(event);
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                cancelSettle();downX=event.getX();downY=event.getY();tracking=true;swiping=false;excludedStart=touchStartedOnReorderHandle(downX,downY);return false;
            case MotionEvent.ACTION_POINTER_DOWN:
                tracking=false;return false;
            case MotionEvent.ACTION_MOVE:
                if(!tracking || excludedStart)return false;
                if(event.getEventTime()-event.getDownTime()>longPressTimeout){tracking=false;return false;}
                float dx=event.getX()-downX,dy=event.getY()-downY;
                if(Math.abs(dx)>dp(48) && Math.abs(dx)>Math.abs(dy)*1.5f){swiping=true;swipeOffset=clamp(dx);setBackground(deleteBackground);invalidate();disallowAncestors(true);return true;}
                if(Math.abs(dy)>touchSlop && Math.abs(dy)>Math.abs(dx)){tracking=false;}
                return false;
            case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:
                tracking=false;excludedStart=false;return false;
            default:return false;
        }
    }

    @Override public void requestDisallowInterceptTouchEvent(boolean disallowIntercept){
        // Editable text views request this for cursor/selection gestures. Keep observing the row
        // until movement is clearly vertical or horizontal; reorder handles remain excluded.
        if(deletable && tracking && !excludedStart && disallowIntercept)return;
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    private boolean touchStartedOnReorderHandle(float x,float y){
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);Object tag=child.getTag();
            if(tag instanceof String && ((String)tag).startsWith("row-handle:") && x>=child.getLeft() && x<child.getRight() && y>=child.getTop() && y<child.getBottom())return true;
        }
        return false;
    }

    private void disallowAncestors(boolean disallow){
        ViewParent parent=getParent();while(parent!=null){parent.requestDisallowInterceptTouchEvent(disallow);parent=parent.getParent();}
    }

    @Override public boolean onTouchEvent(MotionEvent event){
        if(!swiping)return super.onTouchEvent(event);
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_MOVE:
                swipeOffset=clamp(event.getX()-downX);invalidate();return true;
            case MotionEvent.ACTION_UP:
                float dx=event.getX()-downX,dy=event.getY()-downY;swipeOffset=clamp(dx);tracking=false;swiping=false;excludedStart=false;disallowAncestors(false);
                if(Math.abs(dx)>=threshold() && Math.abs(dx)>Math.abs(dy)*1.5f){swipeOffset=Math.copySign(Math.max(getWidth(),Math.abs(dx)),dx);invalidate();boolean removed=deleteAction!=null && deleteAction.remove();if(!removed)settleBack();}
                else settleBack();
                return true;
            case MotionEvent.ACTION_CANCEL:
                tracking=false;swiping=false;excludedStart=false;disallowAncestors(false);settleBack();return true;
            default:return true;
        }
    }

    private void settleBack(){
        cancelSettle();if(swipeOffset==0){setBackground(null);invalidate();return;}
        float from=swipeOffset;settleAnimator=ValueAnimator.ofFloat(from,0);settleAnimator.setDuration(150);settleAnimator.addUpdateListener(animation->{swipeOffset=(Float)animation.getAnimatedValue();invalidate();});
        settleAnimator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator animation){swipeOffset=0;setBackground(null);invalidate();settleAnimator=null;}});settleAnimator.start();
    }

    private void cancelSettle(){if(settleAnimator!=null){settleAnimator.cancel();settleAnimator=null;}if(swipeOffset!=0){swipeOffset=0;setBackground(null);invalidate();}}

    @Override protected void onDetachedFromWindow(){cancelSettle();tracking=false;swiping=false;super.onDetachedFromWindow();}
}
