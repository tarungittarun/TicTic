package app.space.notes;

import android.app.Instrumentation;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;

/** Reaches only the native local-folder SAF picker; verifies the returned tree separately. */
final class SafTestPicker {
    private static final long ACTION_TRANSITION_SETTLE_MS=1500;
    private SafTestPicker() {}

    static final class Driver {
        private final Instrumentation instrumentation;
        private String lastAction="";
        private long lastAttemptAt;
        private int attempts;
        private boolean actionPending,fallbackTouchUsed,accessibilityPending;
        private String lastOutcome="none";

        Driver(Instrumentation instrumentation){this.instrumentation=instrumentation;}

        boolean click(AccessibilityNodeInfo root){return clickNode(root);}

        private boolean clickNode(AccessibilityNodeInfo node){
            if(node==null)return false;
            CharSequence label=node.getText()!=null?node.getText():node.getContentDescription();
            String action=actionFor(label);
            if(action!=null && node.isEnabled() && node.isVisibleToUser()){
                AccessibilityNodeInfo target=actionTarget(node);
                if(target!=null && target.isEnabled() && target.isVisibleToUser()){
                    long now=SystemClock.uptimeMillis();
                    boolean retry=action.equals(lastAction);
                    // A new label can appear before the previous click's picker
                    // transition has finished. Do not let SELECT -> confirmation
                    // dispatch overlapping DocumentsUI tasks.
                    if(actionPending){
                        if(retry){
                            // An accepted accessibility click is normally a one-shot command.
                            // On API 26 the picker can report it accepted without dispatching
                            // SELECT; after a bounded wait allow one native tap fallback.
                            if(Build.VERSION.SDK_INT==26 && accessibilityPending && !fallbackTouchUsed && now-lastAttemptAt>=ACTION_TRANSITION_SETTLE_MS){
                                fallbackTouchUsed=true;lastAttemptAt=now;attempts++;boolean touched=tap(target);actionPending=touched;lastOutcome="api26-touch-fallback "+touched;return touched;
                            }
                            // Do not repeat on newer DocumentsUI builds: resubmitting the
                            // same grant can crash the picker.
                            lastOutcome="waiting-for-ui-transition";return true;
                        }
                        if(now-lastAttemptAt<ACTION_TRANSITION_SETTLE_MS){lastOutcome="waiting-for-ui-transition";return true;}
                    }
                    if(!retry){lastAction=action;actionPending=false;fallbackTouchUsed=false;accessibilityPending=false;}
                    if(!retry || now-lastAttemptAt>=750){
                        attempts++;lastAction=action;lastAttemptAt=now;
                        if(!retry){
                            boolean accepted=target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            lastOutcome=accepted?"accessibility-click-accepted":"accessibility-click-rejected";
                            if(accepted){accessibilityPending=true;actionPending=true;return true;}
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

        // SAF confirmation dialogs expose a non-clickable title such as "Allow access to..."
        // above the real ALLOW button. Never activate an arbitrary clickable dialog container.
        private AccessibilityNodeInfo actionTarget(AccessibilityNodeInfo node){
            for(AccessibilityNodeInfo target=node;target!=null;target=target.getParent()){
                if(target.isClickable() && isButton(target))return target;
            }
            return null;
        }

        private boolean isButton(AccessibilityNodeInfo node){
            CharSequence name=node.getClassName();return name!=null && name.toString().endsWith("Button");
        }

        private String actionFor(CharSequence value){
            if(value==null)return null;
            String text=value.toString().trim(),upper=text.toUpperCase(Locale.ROOT);
            if(upper.equals("USE THIS FOLDER"))return "use-this-folder";
            if(upper.equals("SELECT"))return "select";
            if(upper.equals("ALLOW"))return "confirm-access";
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
