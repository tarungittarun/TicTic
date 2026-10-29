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
 private boolean ready,saving,navigating,grid;private int paperHeight=768;private boolean sized;private long revision;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private float zoom=1,panX,panY,focusX,focusY;private final ScaleGestureDetector scale;
 DrawingPad(Context c){super(c);setContentDescription("Drawing canvas. Draw with a finger. Undo and redo controls are below.");setFocusable(true);setEnabled(false);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
  scale=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){float next=Math.max(1,Math.min(4,zoom*d.getScaleFactor()));panX=d.getFocusX()-offsetX()-(d.getFocusX()-offsetX()-panX)*next/zoom;panY=d.getFocusY()-offsetY()-(d.getFocusY()-offsetY()-panY)*next/zoom;zoom=next;clampPan();invalidate();return true;}});
 }
 void loaded(Bitmap bitmap){base=bitmap;if(bitmap!=null)paperHeight=Math.max(paperHeight,Math.min(4096,Math.round(bitmap.getHeight()*1024f/bitmap.getWidth())));ready=true;setEnabled(true);invalidate();}
 boolean grid(){return grid;}void grid(boolean value){grid=value;invalidate();}
 private float scale(){return Math.min(getWidth()/1024f,getHeight()/(float)paperHeight);}
 private float offsetX(){return (getWidth()-1024*scale())/2;}private float offsetY(){return (getHeight()-paperHeight*scale())/2;}
 private float worldX(float x){return Math.max(0,Math.min(1024,(x-panX-offsetX())/(zoom*scale())));}
 private float worldY(float y){return Math.max(0,Math.min(paperHeight,(y-panY-offsetY())/(zoom*scale())));}
 boolean ready(){return ready;}boolean saving(){return saving;}
 void saving(boolean value){saving=value;setEnabled(ready && !saving);}
 long revision(){return revision;}
 void color(int value){color=value;if(mode==3)mode=0;}
 void tool(int value){if(value<0 || value>=TOOLS.length)throw new IllegalArgumentException("Unknown drawing tool");mode=value;}
 int tool(){return mode;}
 void width(float value){width=Math.max(1,Math.min(48,value));}
 float width(){return width;}
 void fit(){zoom=1;panX=panY=0;invalidate();}
 void clear(){if(!ready || saving)return;Path area=new Path();area.addRect(0,0,1024,paperHeight,Path.Direction.CW);strokes.add(new Stroke(area,Color.WHITE,1,true));redo.clear();revision++;invalidate();}
 void undo(){if(!saving && !strokes.isEmpty()){redo.add(strokes.remove(strokes.size()-1));revision++;invalidate();}}
 void redo(){if(!saving && !redo.isEmpty()){strokes.add(redo.remove(redo.size()-1));revision++;invalidate();}}
 private void stroke(Canvas c,Path path,int color,float width,boolean fill){paint.setStyle(fill?Paint.Style.FILL:Paint.Style.STROKE);paint.setColor(color);paint.setStrokeWidth(width);c.drawPath(path,paint);}
 private void drawPaper(Canvas c,boolean preview){c.drawColor(Color.WHITE);if(base!=null)c.drawBitmap(base,null,new RectF(0,0,1024,Math.min(paperHeight,base.getHeight()*1024f/base.getWidth())),null);for(Stroke s:strokes)stroke(c,s.path,s.color,s.width,s.fill);if(preview && current!=null)stroke(c,current,strokeColor,strokeWidth,false);}
 Bitmap snapshot(){Bitmap value=Bitmap.createBitmap(1024,paperHeight,Bitmap.Config.ARGB_8888);drawPaper(new Canvas(value),false);return value;}
 @Override protected void onMeasure(int width,int height){int w=MeasureSpec.getSize(width),h=MeasureSpec.getMode(height)==MeasureSpec.UNSPECIFIED?Math.round(w*.75f):MeasureSpec.getSize(height);setMeasuredDimension(w,h);if(w>0 && h>0){int target=Math.max(128,Math.min(4096,Math.round(h*1024f/w)));paperHeight=sized?Math.max(paperHeight,target):Math.max(target,base==null?0:Math.round(base.getHeight()*1024f/base.getWidth()));sized=true;}}
 private void clampPan(){float s=scale()*zoom;panX=Math.max(Math.min(0,getWidth()-offsetX()-1024*s),Math.min(0,panX));panY=Math.max(Math.min(0,getHeight()-offsetY()-paperHeight*s),Math.min(0,panY));}
 @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Color.WHITE);c.save();c.clipRect(0,0,getWidth(),getHeight());c.translate(offsetX()+panX,offsetY()+panY);c.scale(zoom*scale(),zoom*scale());c.clipRect(0,0,1024,paperHeight);drawPaper(c,true);if(grid){paint.setStyle(Paint.Style.STROKE);paint.setColor(0x22808080);paint.setStrokeWidth(1/(zoom*scale()));for(int x=0;x<=1024;x+=64)c.drawLine(x,0,x,paperHeight,paint);for(int y=0;y<=paperHeight;y+=64)c.drawLine(0,y,1024,y,paint);}if(offsetX()>1 || offsetY()>1){paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffd9ddd8);paint.setStrokeWidth(1/(zoom*scale()));c.drawRect(0,0,1024,paperHeight,paint);}c.restore();}
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(w>0 && h>0)clampPan();}
 private void point(float x,float y,boolean finish){
  if(strokeMode>=4){current.reset();if(strokeMode==4){current.moveTo(startX,startY);current.lineTo(x,y);}else{RectF box=new RectF(Math.min(startX,x),Math.min(startY,y),Math.max(startX,x),Math.max(startY,y));if(strokeMode==5)current.addRect(box,Path.Direction.CW);else current.addOval(box,Path.Direction.CW);}}
  else{current.quadTo(lastX,lastY,(lastX+x)/2,(lastY+y)/2);if(finish)current.lineTo(x,y);}lastX=x;lastY=y;
 }
 @Override public boolean onTouchEvent(MotionEvent e){
  if(!ready || saving || getWidth()==0 || getHeight()==0)return false;
  scale.onTouchEvent(e);int action=e.getActionMasked();
  if(e.getPointerCount()>1){current=null;float fx=(e.getX(0)+e.getX(1))/2,fy=(e.getY(0)+e.getY(1))/2;if(navigating && action==MotionEvent.ACTION_MOVE){panX+=fx-focusX;panY+=fy-focusY;clampPan();}focusX=fx;focusY=fy;navigating=true;getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;}
  if(navigating){if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL){navigating=false;getParent().requestDisallowInterceptTouchEvent(false);}return true;}
  float x=worldX(e.getX()),y=worldY(e.getY());
  switch(action){
   case MotionEvent.ACTION_DOWN:float rawX=(e.getX()-panX-offsetX())/(zoom*scale()),rawY=(e.getY()-panY-offsetY())/(zoom*scale());if(rawX<0 || rawX>1024 || rawY<0 || rawY>paperHeight){current=null;return true;}current=new Path();current.moveTo(x,y);current.lineTo(x+.1f,y+.1f);startX=lastX=x;startY=lastY=y;strokeMode=mode;strokeColor=mode==3?Color.WHITE:mode==2?(color&0x00ffffff)|0x55000000:mode==1?(color&0x00ffffff)|0xbb000000:color;strokeWidth=width*(mode==2?6:mode==3?4:mode==1?.65f:1);getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;
   case MotionEvent.ACTION_MOVE:if(current!=null){for(int i=0;i<e.getHistorySize();i++)point(worldX(e.getHistoricalX(i)),worldY(e.getHistoricalY(i)),false);point(x,y,false);}invalidate();return true;
   case MotionEvent.ACTION_UP:if(current!=null){point(x,y,true);strokes.add(new Stroke(current,strokeColor,strokeWidth,false));current=null;redo.clear();revision++;}getParent().requestDisallowInterceptTouchEvent(false);performClick();invalidate();return true;
   case MotionEvent.ACTION_CANCEL:current=null;getParent().requestDisallowInterceptTouchEvent(false);invalidate();return true;
   default:return true;
  }
 }
 @Override public boolean performClick(){super.performClick();return true;}
 @Override protected void onDetachedFromWindow(){current=null;if(base!=null){base.recycle();base=null;}super.onDetachedFromWindow();}
}
