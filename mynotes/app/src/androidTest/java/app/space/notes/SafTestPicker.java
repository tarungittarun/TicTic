package app.space.notes;

import android.app.Instrumentation;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;

/** Reaches only the native local-folder SAF picker; verifies the returned tree separately. */
final class SafTestPicker {
    private SafTestPicker() {}

    static final class Driver {
        private final Instrumentation instrumentation;
        private String lastAction="";
        private long lastAttemptAt;
        private int attempts;
        private boolean actionPending;
        private String lastOutcome="none";

        Driver(Instrumentation instrumentation){this.instrumentation=instrumentation;}

        boolean click(AccessibilityNodeInfo root){return clickNode(root);}

        private boolean clickNode(AccessibilityNodeInfo node){
            if(node==null)return false;
            CharSequence label=node.getText()!=null?node.getText():node.getContentDescription();
            String action=actionFor(label);
            if(action!=null && node.isEnabled() && node.isVisibleToUser()){
                AccessibilityNodeInfo target=node;
                while(target!=null && (!target.isClickable() || !target.isEnabled()))target=target.getParent();
                if(target!=null){
                    long now=SystemClock.uptimeMillis();
                    boolean retry=action.equals(lastAction);
                    if(!retry){lastAction=action;actionPending=false;}
                    // A successful click is a one-shot command. Keep observing this
                    // picker until its state changes; a second touch while the same
                    // button remains on screen can submit the SAF result twice and
                    // crash the API 29 DocumentsUI AsyncTask.
                    if(retry && actionPending){lastOutcome="waiting-for-ui-transition";return true;}
                    if(!retry || now-lastAttemptAt>=750){
                        attempts++;lastAction=action;lastAttemptAt=now;
                        if(!retry){
                            boolean accepted=target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            lastOutcome=accepted?"accessibility-click-accepted":"accessibility-click-rejected";
                            if(accepted){actionPending=true;return true;}
                        }
                        boolean touched=tap(target);actionPending=touched;
                        lastOutcome=(retry?"retry-with-touch":"touch-fallback")+" "+touched;
                        return touched;
                    }
                }
            }
            for(int i=0;i<node.getChildCount();i++)if(clickNode(node.getChild(i)))return true;
            return false;
        }

        private String actionFor(CharSequence value){
            if(value==null)return null;
            String text=value.toString().trim(),upper=text.toUpperCase(Locale.ROOT);
            if(upper.equals("USE THIS FOLDER") || upper.equals("SELECT"))return "select:"+upper;
            if(upper.equals("ALLOW") || upper.startsWith("ALLOW ACCESS TO"))return "confirm:"+upper;
            return null;
        }

        private boolean tap(AccessibilityNodeInfo target){
            Rect bounds=new Rect();target.getBoundsInScreen(bounds);
            if(bounds.isEmpty())return false;
            int x=bounds.centerX(),y=bounds.centerY();long downTime=SystemClock.uptimeMillis();
            MotionEvent down=MotionEvent.obtain(downTime,downTime,MotionEvent.ACTION_DOWN,x,y,0);
            MotionEvent up=MotionEvent.obtain(downTime,downTime+40,MotionEvent.ACTION_UP,x,y,0);
            down.setSource(InputDevice.SOURCE_TOUCHSCREEN);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            try{
                boolean first=instrumentation.getUiAutomation().injectInputEvent(down,true);
                SystemClock.sleep(40);
                boolean second=instrumentation.getUiAutomation().injectInputEvent(up,true);
                return first&&second;
            }finally{down.recycle();up.recycle();}
        }

        @Override public String toString(){return "attempts="+attempts+" lastAction="+lastAction+" lastOutcome="+lastOutcome;}
    }

    static String describe(AccessibilityNodeInfo node){
        StringBuilder out=new StringBuilder();appendDescription(node,out,new int[]{120});
        return out.length()==0?"null":out.toString();
    }

    private static void appendDescription(AccessibilityNodeInfo node,StringBuilder out,int[] remaining){
        if(node==null || remaining[0]--<=0)return;
        Rect bounds=new Rect();node.getBoundsInScreen(bounds);
        out.append('[').append(node.getClassName()).append(':').append(node.getText()).append(':').append(node.getContentDescription())
            .append(" enabled=").append(node.isEnabled()).append(" clickable=").append(node.isClickable())
            .append(" visible=").append(node.isVisibleToUser()).append(" bounds=").append(bounds).append(']');
        for(int i=0;i<node.getChildCount() && remaining[0]>0;i++)appendDescription(node.getChild(i),out,remaining);
    }
}
