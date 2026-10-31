package app.space.notes.runtime;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.view.*;
import android.widget.*;
import app.space.notes.domain.*;

/** View-only magnification: never alters pixels, drawing coordinates or document history. */
final class VisualZoomDialog {
 private VisualZoomDialog(){}
 static void image(Context context,NotesDesign design,NoteImage image,boolean drawing){show(context,design,new NoteImageView(context,image),drawing?5:3,drawing?"Drawing":"Image");}
 static void diagram(Context context,NotesDesign design,Diagram diagram){show(context,design,new DiagramPreview(context,diagram,design),5,"Diagram");}
 private static void show(Context context,NotesDesign design,View content,int maximum,String name){
  android.app.Dialog dialog=new android.app.Dialog(context);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
  LinearLayout layout=new LinearLayout(context);layout.setOrientation(LinearLayout.VERTICAL);layout.setBackgroundColor(design.background);layout.setPadding(design.dp(12),design.dp(8),design.dp(12),design.dp(8));
  LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);header.addView(design.label(name+" · Zoom",18,design.ink),new LinearLayout.LayoutParams(0,-2,1));header.addView(design.icon("Close zoom","close",dialog::dismiss),new LinearLayout.LayoutParams(design.dp(48),design.dp(48)));layout.addView(header);
  ZoomPane pane=new ZoomPane(context,maximum);pane.addView(content,new android.widget.FrameLayout.LayoutParams(-1,-1));layout.addView(pane,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout tools=new LinearLayout(context);TextView level=design.label("100%",15,design.ink);level.setGravity(Gravity.CENTER);pane.changed=()->{level.setText(Math.round(pane.zoom*100)+"%");androidx.core.view.ViewCompat.setStateDescription(pane,level.getText());};
  tools.addView(design.icon("Zoom out","minus",()->pane.scale(pane.zoom-.5f)),new LinearLayout.LayoutParams(design.dp(48),design.dp(48)));tools.addView(level,new LinearLayout.LayoutParams(0,design.dp(48),1));tools.addView(design.icon("Zoom in","add",()->pane.scale(pane.zoom+.5f)),new LinearLayout.LayoutParams(design.dp(48),design.dp(48)));tools.addView(design.textAction("Reset",()->{pane.x=0;pane.y=0;pane.scale(1);}),new LinearLayout.LayoutParams(-2,-2));layout.addView(tools);
  TextView help=design.label("Pinch to zoom · drag to pan · up to "+maximum+"×",13,design.muted);help.setMaxLines(2);layout.addView(help);layout.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->help.setVisibility(b-t<design.dp(280)?View.GONE:View.VISIBLE));
  dialog.setContentView(layout,new ViewGroup.LayoutParams(-1,-1));dialog.show();if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout(-1,-1);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);}
 }

 private static final class ZoomPane extends FrameLayout {
  final float maximum;float zoom=1,x,y,lastX,lastY;Runnable changed;final ScaleGestureDetector detector;
  ZoomPane(Context context,int max){super(context);maximum=max;setClipChildren(true);setContentDescription("Visual zoom preview");setMinimumHeight(0);detector=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector event){float next=Math.max(1,Math.min(maximum,zoom*event.getScaleFactor()));x=event.getFocusX()-getWidth()/2f-(event.getFocusX()-getWidth()/2f-x)*next/zoom;y=event.getFocusY()-getHeight()/2f-(event.getFocusY()-getHeight()/2f-y)*next/zoom;scale(next);return true;}});}
  void scale(float value){zoom=Math.max(1,Math.min(maximum,value));clamp();invalidate();if(changed!=null)changed.run();}
  private void clamp(){float dx=getWidth()*(zoom-1)/2,dy=getHeight()*(zoom-1)/2;x=Math.max(-dx,Math.min(dx,x));y=Math.max(-dy,Math.min(dy,y));}
  @Override protected void dispatchDraw(Canvas canvas){int saved=canvas.save();canvas.clipRect(0,0,getWidth(),getHeight());canvas.translate(x,y);canvas.scale(zoom,zoom,getWidth()/2f,getHeight()/2f);super.dispatchDraw(canvas);canvas.restoreToCount(saved);}
  @Override public boolean onInterceptTouchEvent(android.view.MotionEvent event){return true;}
  @Override public boolean onTouchEvent(android.view.MotionEvent event){detector.onTouchEvent(event);if(event.getActionMasked()==MotionEvent.ACTION_DOWN){lastX=event.getX();lastY=event.getY();return true;}if(event.getActionMasked()==MotionEvent.ACTION_MOVE && !detector.isInProgress() && event.getPointerCount()==1){x+=event.getX()-lastX;y+=event.getY()-lastY;clamp();invalidate();}int pointer=event.getActionMasked()==MotionEvent.ACTION_POINTER_UP && event.getActionIndex()==0 && event.getPointerCount()>1?1:0;lastX=event.getX(pointer);lastY=event.getY(pointer);if(event.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;}
  @Override public boolean performClick(){super.performClick();return true;}
 }
}
