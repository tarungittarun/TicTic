package app.space.notes.runtime;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;
/** Local paper with stroke undo/redo. Pointer cancellation never commits half a stroke. */
final class DrawingPad extends View {
 private static final class Stroke {final Path path;final int color;Stroke(Path path,int color){this.path=path;this.color=color;}}
 private final List<Stroke> strokes=new ArrayList<>(),redo=new ArrayList<>();private Path current;private Bitmap base;private int color=Color.BLACK,strokeColor;private boolean ready,saving;private long revision;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 DrawingPad(Context c){super(c);setContentDescription("Drawing canvas. Draw with a finger. Undo and redo controls are below.");setFocusable(true);setEnabled(false);paint.setStrokeWidth(4);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStyle(Paint.Style.STROKE);}
 void loaded(Bitmap bitmap){base=bitmap;ready=true;setEnabled(true);invalidate();}
 boolean ready(){return ready;}
 boolean saving(){return saving;}
 void saving(boolean value){saving=value;setEnabled(ready && !saving);}
 long revision(){return revision;}
 void color(int value){color=value;}
 void undo(){if(!saving && !strokes.isEmpty()){redo.add(strokes.remove(strokes.size()-1));revision++;invalidate();}}
 void redo(){if(!saving && !redo.isEmpty()){strokes.add(redo.remove(redo.size()-1));revision++;invalidate();}}
 private void drawPaper(Canvas c){c.drawColor(Color.WHITE);if(base!=null)c.drawBitmap(base,null,new RectF(0,0,1024,768),null);for(Stroke stroke:strokes){paint.setColor(stroke.color);c.drawPath(stroke.path,paint);}if(current!=null){paint.setColor(strokeColor);c.drawPath(current,paint);}}
 Bitmap snapshot(){Bitmap value=Bitmap.createBitmap(1024,768,Bitmap.Config.ARGB_8888);drawPaper(new Canvas(value));return value;}
 @Override protected void onMeasure(int width,int height){int w=MeasureSpec.getSize(width);setMeasuredDimension(w,Math.round(w*.75f));}
 @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/1024f,getHeight()/768f);drawPaper(c);c.restore();}
 @Override public boolean onTouchEvent(MotionEvent event){if(!ready || saving || getWidth()==0)return false;float x=Math.max(0,Math.min(1024,event.getX()*1024/getWidth())),y=Math.max(0,Math.min(768,event.getY()*768/getHeight()));switch(event.getActionMasked()){
  case MotionEvent.ACTION_DOWN:current=new Path();current.moveTo(x,y);current.lineTo(x+.1f,y+.1f);strokeColor=color;getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;
  case MotionEvent.ACTION_MOVE:if(current!=null)current.lineTo(x,y);invalidate();return true;
  case MotionEvent.ACTION_UP:if(current!=null){current.lineTo(x,y);strokes.add(new Stroke(current,strokeColor));current=null;redo.clear();revision++;}getParent().requestDisallowInterceptTouchEvent(false);performClick();invalidate();return true;
  case MotionEvent.ACTION_CANCEL:current=null;getParent().requestDisallowInterceptTouchEvent(false);invalidate();return true;
  default:return true;
 }}
 @Override public boolean performClick(){super.performClick();return true;}
 @Override protected void onDetachedFromWindow(){current=null;if(base!=null){base.recycle();base=null;}super.onDetachedFromWindow();}
}
