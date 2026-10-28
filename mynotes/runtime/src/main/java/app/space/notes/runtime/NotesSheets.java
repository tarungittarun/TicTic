package app.space.notes.runtime;

import android.view.*;
import android.widget.*;
import android.graphics.Color;
import java.util.*;

/** Temporary, dismissible menus; never part of the document or a permanent stack of tool buttons. */
final class NotesSheets {
 private final NotesDesign design; private final FrameLayout shell;
 private final FrameLayout overlay;private final LinearLayout surface;private final TextView caption;
 private ScrollView scroll;private final LinearLayout parking;private final List<LinearLayout> panels=new ArrayList<>();
 private LinearLayout current;
 NotesSheets(NotesDesign design,FrameLayout shell){this.design=design;this.shell=shell;
  overlay=new FrameLayout(design.context);overlay.setBackgroundColor(0x66000000);overlay.setVisibility(View.GONE);
  View scrim=new View(design.context);scrim.setContentDescription("Dismiss tools");scrim.setOnClickListener(v->close());overlay.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
  surface=new LinearLayout(design.context);surface.setOrientation(LinearLayout.VERTICAL);surface.setClickable(true);surface.setPadding(design.dp(16),design.dp(4),design.dp(16),design.dp(16));android.graphics.drawable.GradientDrawable sheet=design.shape(design.background,0);float corner=design.dp(24);sheet.setCornerRadii(new float[]{corner,corner,corner,corner,0,0,0,0});surface.setBackground(sheet);surface.setElevation(design.dp(12));
  LinearLayout heading=new LinearLayout(design.context);heading.setGravity(Gravity.CENTER_VERTICAL);caption=design.heading("");caption.setMaxLines(2);caption.setEllipsize(android.text.TextUtils.TruncateAt.END);heading.addView(caption,new LinearLayout.LayoutParams(0,-2,1));heading.addView(design.icon("Close tools","close",this::close),new LinearLayout.LayoutParams(design.dp(48),design.dp(48)));surface.addView(heading);
  parking=new LinearLayout(design.context);parking.setVisibility(View.GONE);overlay.addView(parking);scroll=new ScrollView(design.context);scroll.setFillViewport(false);surface.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  overlay.addView(surface,new FrameLayout.LayoutParams(Math.min(design.dp(560),design.dp(design.context.getResources().getConfiguration().screenWidthDp)),design.dp(420),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL));shell.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
  shell.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->resize());
 }
 LinearLayout panel(String title,View launcher){LinearLayout panel=new LinearLayout(design.context);panel.setOrientation(LinearLayout.VERTICAL);panel.setVisibility(View.GONE);panel.setTag(launcher);panel.setContentDescription(title);parking.addView(panel,new LinearLayout.LayoutParams(-1,-2));panels.add(panel);return panel;}
 void show(LinearLayout panel,String title){
  if(!panels.contains(panel))return;
  // Only the active panel participates in scroll measurement. Recreate transient scroll state:
  // reused GONE siblings/focus and retained scroll offsets must never produce a blank sheet.
  for(LinearLayout p:panels){if(p.getParent()!=null)((ViewGroup)p.getParent()).removeView(p);p.setVisibility(View.GONE);if(p!=panel)parking.addView(p,new LinearLayout.LayoutParams(-1,-2));}
  surface.removeView(scroll);scroll=new ScrollView(design.context);scroll.setFillViewport(false);
  scroll.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);panel.setVisibility(View.VISIBLE);
  scroll.addView(panel,new ScrollView.LayoutParams(-1,-2));surface.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  current=panel;caption.setText(title);shell.getChildAt(0).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);if(android.os.Build.VERSION.SDK_INT>=28)surface.setAccessibilityPaneTitle(title);overlay.setVisibility(View.VISIBLE);resize();surface.requestLayout();
  scroll.post(()->{if(current==panel){scroll.scrollTo(0,0);Diagnostics.record("sheet.layout","rows="+panel.getChildCount()+" measured_height="+panel.getMeasuredHeight()+" viewport_height="+scroll.getHeight());}});
  surface.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
 }
 private void resize(){if(shell.getHeight()<=0)return;FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)surface.getLayoutParams();int height=Math.min(design.dp(560),Math.max(design.dp(112),(int)(shell.getHeight()*.72f)));if(lp.height!=height){lp.height=height;surface.setLayoutParams(lp);}}
 boolean close(){if(overlay.getVisibility()!=View.VISIBLE)return false;overlay.setVisibility(View.GONE);shell.getChildAt(0).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);current=null;return true;}
 void reset(){close();scroll.removeAllViews();parking.removeAllViews();panels.clear();current=null;}
 boolean open(){return overlay.getVisibility()==View.VISIBLE;}
}
