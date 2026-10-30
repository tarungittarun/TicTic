package app.space.notes.runtime;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.Button;
/** Single tap, double tap, and hold-on-second-tap. Standard click/long-click remain accessible. */
public final class SearchGestures {
    private SearchGestures(){}
    public static void install(Button button,Runnable focus,Runnable keyboard,Runnable filters){
        Handler main=new Handler(Looper.getMainLooper());boolean[] second={false},held={false};
        Runnable hold=()->{if(second[0] && button.isAttachedToWindow()){held[0]=true;button.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);filters.run();}};
        GestureDetector detector=new GestureDetector(button.getContext(),new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onSingleTapConfirmed(MotionEvent e){if(button.isAttachedToWindow())button.performClick();return true;}
            @Override public boolean onDoubleTap(MotionEvent e){second[0]=true;held[0]=false;return true;}
            @Override public boolean onDoubleTapEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN)main.postDelayed(hold,ViewConfiguration.getLongPressTimeout());else if(e.getActionMasked()==MotionEvent.ACTION_UP){main.removeCallbacks(hold);if(!held[0])keyboard.run();second[0]=false;}else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){main.removeCallbacks(hold);second[0]=false;}return true;}
            @Override public void onLongPress(MotionEvent e){if(!second[0])button.performLongClick();}
        });
        button.setOnClickListener(v->focus.run());button.setOnLongClickListener(v->{filters.run();return true;});
        button.setTooltipText("Search · double-tap for keyboard · hold for Filters");
        button.setOnTouchListener((v,event)->{boolean result=detector.onTouchEvent(event);if(event.getActionMasked()==MotionEvent.ACTION_CANCEL){main.removeCallbacks(hold);second[0]=false;}return result;});
        button.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){main.removeCallbacks(hold);second[0]=false;}});
    }
}
