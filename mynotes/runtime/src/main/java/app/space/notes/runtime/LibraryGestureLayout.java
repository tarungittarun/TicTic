package app.space.notes.runtime;
import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import java.util.function.BooleanSupplier;
/** Home-only leading-edge swipe, never the native text editor's gestures. */
final class LibraryGestureLayout extends FrameLayout {
 private final BooleanSupplier enabled;private final Runnable open;private final float density;private float x,y;private boolean candidate,claimed;private String lastDecision="no down event";
 private android.view.View.OnDragListener documentDrop;
 @Override public void setOnDragListener(android.view.View.OnDragListener listener){documentDrop=listener;super.setOnDragListener(listener);}
 /** Route document drops before editable children can coerce a URI into note text. */
 @Override public boolean dispatchDragEvent(android.view.DragEvent event){android.content.ClipDescription description=event.getClipDescription();if(event.getLocalState()==null && description!=null && (description.hasMimeType("application/json") || description.hasMimeType("application/octet-stream") || description.hasMimeType("application/vnd.mynotes.document")))return documentDrop!=null && documentDrop.onDrag(this,event);return super.dispatchDragEvent(event);}
 LibraryGestureLayout(Context c,BooleanSupplier enabled,Runnable open){super(c);this.enabled=enabled;this.open=open;density=getResources().getDisplayMetrics().density;}
 @Override protected void onLayout(boolean changed,int l,int t,int r,int b){super.onLayout(changed,l,t,r,b);if(android.os.Build.VERSION.SDK_INT>=29){java.util.List<android.graphics.Rect> exclusion=new java.util.ArrayList<>();if(enabled.getAsBoolean()){int top=getHeight()/2;exclusion.add(new android.graphics.Rect(0,top,Math.round(24*density),Math.min(getHeight(),top+Math.round(200*density))));}setSystemGestureExclusionRects(exclusion);}}
 @Override public boolean onInterceptTouchEvent(MotionEvent e){if(e.getActionMasked()==0){x=e.getX();y=e.getY();claimed=false;float edge=Math.max(24*density,(float)Math.ceil(getWidth()*.20));boolean allowed=enabled.getAsBoolean();candidate=x>=0 && x<=edge && allowed;lastDecision="x="+x+" edge="+edge+" allowed="+allowed+" width="+getWidth();}if(e.getActionMasked()==2 && candidate){float dx=e.getX()-x,dy=Math.abs(e.getY()-y);if(dy>24*density && dy>Math.abs(dx))candidate=false;else if(dx>48*density && dx>dy*1.5f)claimed=true;}return claimed;}
 @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==0 && candidate)return true;if(e.getActionMasked()==2 && candidate){float dx=e.getX()-x,dy=Math.abs(e.getY()-y);if(dx>48*density && dx>dy*1.5f)claimed=true;}if(!claimed){if(e.getActionMasked()==1 || e.getActionMasked()==3)candidate=false;return candidate || super.onTouchEvent(e);}if(e.getActionMasked()==1){boolean invoke=e.getX()-x>48*density && enabled.getAsBoolean();candidate=claimed=false;performClick();if(invoke)open.run();}else if(e.getActionMasked()==3)candidate=claimed=false;return true;}
 @Override public boolean performClick(){super.performClick();return true;}
}
