package app.space.notes.runtime;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;
/** Resolution-independent ink. Tool properties belong to each stroke, not the current brush.
 * Two fingers navigate; a cancelled gesture never becomes a stroke. Exports ignore the viewport. */
final class DrawingPad extends View {
 static final String[] TOOLS={"Pen","Pencil","Highlighter","Eraser","Line","Rectangle","Oval"};
 private static final class Stroke {
  final Path path;final int color;final float width;final boolean fill;
  Stroke(Path p,int c,float w,boolean f){path=p;color=c;width=w;fill=f;}
 }
 private final List<Stroke> strokes=new ArrayList<>(),redo=new ArrayList<>();
 private Path current;private Bitmap base;private int color=Color.BLACK,strokeColor,mode,strokeMode;private float width=4,strokeWidth,startX,startY,lastX,lastY;
 private boolean ready,saving,navigating;private long revision;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private float zoom=1,panX,panY,focusX,focusY;private final ScaleGestureDetector scale;
 DrawingPad(Context c){super(c);setContentDescription("Drawing canvas. Draw with a finger. Undo and redo controls are below.");setFocusable(true);setEnabled(false);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
  scale=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){float next=Math.max(1,Math.min(4,zoom*d.getScaleFactor()));panX=d.getFocusX()-(d.getFocusX()-panX)*next/zoom;panY=d.getFocusY()-(d.getFocusY()-panY)*next/zoom;zoom=next;clampPan();invalidate();return true;}});
 }
 void loaded(Bitmap bitmap){base=bitmap;ready=true;setEnabled(true);invalidate();}
 boolean ready(){return ready;}boolean saving(){return saving;}
 void saving(boolean value){saving=value;setEnabled(ready && !saving);}
 long revision(){return revision;}
 void color(int value){color=value;if(mode==3)mode=0;}
 void tool(int value){if(value<0 || value>=TOOLS.length)throw new IllegalArgumentException("Unknown drawing tool");mode=value;}
 int tool(){return mode;}
 void width(float value){width=Math.max(1,Math.min(48,value));}
 float width(){return width;}
 void fit(){zoom=1;panX=panY=0;invalidate();}
 void clear(){if(!ready || saving)return;Path area=new Path();area.addRect(0,0,1024,768,Path.Direction.CW);strokes.add(new Stroke(area,Color.WHITE,1,true));redo.clear();revision++;invalidate();}
 void undo(){if(!saving && !strokes.isEmpty()){redo.add(strokes.remove(strokes.size()-1));revision++;invalidate();}}
 void redo(){if(!saving && !redo.isEmpty()){strokes.add(redo.remove(redo.size()-1));revision++;invalidate();}}
 private void stroke(Canvas c,Path path,int color,float width,boolean fill){paint.setStyle(fill?Paint.Style.FILL:Paint.Style.STROKE);paint.setColor(color);paint.setStrokeWidth(width);c.drawPath(path,paint);}
 private void drawPaper(Canvas c,boolean preview){c.drawColor(Color.WHITE);if(base!=null)c.drawBitmap(base,null,new RectF(0,0,1024,768),null);for(Stroke s:strokes)stroke(c,s.path,s.color,s.width,s.fill);if(preview && current!=null)stroke(c,current,strokeColor,strokeWidth,false);}
 Bitmap snapshot(){Bitmap value=Bitmap.createBitmap(1024,768,Bitmap.Config.ARGB_8888);drawPaper(new Canvas(value),false);return value;}
 @Override protected void onMeasure(int width,int height){int w=MeasureSpec.getSize(width);if(MeasureSpec.getMode(height)!=MeasureSpec.UNSPECIFIED)w=Math.min(w,Math.max(0,Math.round(MeasureSpec.getSize(height)/.75f)));setMeasuredDimension(w,Math.round(w*.75f));}
 private void clampPan(){panX=Math.max(getWidth()*(1-zoom),Math.min(0,panX));panY=Math.max(getHeight()*(1-zoom),Math.min(0,panY));}
 @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.clipRect(0,0,getWidth(),getHeight());c.translate(panX,panY);c.scale(zoom*getWidth()/1024f,zoom*getHeight()/768f);drawPaper(c,true);c.restore();}
 private void point(float x,float y,boolean finish){
  if(strokeMode>=4){current.reset();if(strokeMode==4){current.moveTo(startX,startY);current.lineTo(x,y);}else{RectF box=new RectF(Math.min(startX,x),Math.min(startY,y),Math.max(startX,x),Math.max(startY,y));if(strokeMode==5)current.addRect(box,Path.Direction.CW);else current.addOval(box,Path.Direction.CW);}}
  else{current.quadTo(lastX,lastY,(lastX+x)/2,(lastY+y)/2);if(finish)current.lineTo(x,y);}lastX=x;lastY=y;
 }
 @Override public boolean onTouchEvent(MotionEvent e){
  if(!ready || saving || getWidth()==0 || getHeight()==0)return false;
  scale.onTouchEvent(e);int action=e.getActionMasked();
  if(e.getPointerCount()>1){current=null;float fx=(e.getX(0)+e.getX(1))/2,fy=(e.getY(0)+e.getY(1))/2;if(navigating && action==MotionEvent.ACTION_MOVE){panX+=fx-focusX;panY+=fy-focusY;clampPan();}focusX=fx;focusY=fy;navigating=true;getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;}
  if(navigating){if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL){navigating=false;getParent().requestDisallowInterceptTouchEvent(false);}return true;}
  float x=Math.max(0,Math.min(1024,(e.getX()-panX)/zoom*1024/getWidth())),y=Math.max(0,Math.min(768,(e.getY()-panY)/zoom*768/getHeight()));
  switch(action){
   case MotionEvent.ACTION_DOWN:current=new Path();current.moveTo(x,y);current.lineTo(x+.1f,y+.1f);startX=lastX=x;startY=lastY=y;strokeMode=mode;strokeColor=mode==3?Color.WHITE:mode==2?(color&0x00ffffff)|0x55000000:mode==1?(color&0x00ffffff)|0xbb000000:color;strokeWidth=width*(mode==2?6:mode==3?4:mode==1?.65f:1);getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;
   case MotionEvent.ACTION_MOVE:if(current!=null){for(int i=0;i<e.getHistorySize();i++)point(Math.max(0,Math.min(1024,(e.getHistoricalX(i)-panX)/zoom*1024/getWidth())),Math.max(0,Math.min(768,(e.getHistoricalY(i)-panY)/zoom*768/getHeight())),false);point(x,y,false);}invalidate();return true;
   case MotionEvent.ACTION_UP:if(current!=null){point(x,y,true);strokes.add(new Stroke(current,strokeColor,strokeWidth,false));current=null;redo.clear();revision++;}getParent().requestDisallowInterceptTouchEvent(false);performClick();invalidate();return true;
   case MotionEvent.ACTION_CANCEL:current=null;getParent().requestDisallowInterceptTouchEvent(false);invalidate();return true;
   default:return true;
  }
 }
 @Override public boolean performClick(){super.performClick();return true;}
 @Override protected void onDetachedFromWindow(){current=null;if(base!=null){base.recycle();base=null;}super.onDetachedFromWindow();}
}
