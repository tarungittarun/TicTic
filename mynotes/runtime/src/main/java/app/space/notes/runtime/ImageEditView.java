package app.space.notes.runtime;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import app.space.notes.domain.ImageEditPlan;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Local preview owns one decoded bitmap. Crops and vector annotations never copy pixels. */
final class ImageEditView extends View {
 enum Tool { NONE, HIGHLIGHT, PEN, RECTANGLE, OVAL, ARROW }
 private static final int STATE_MAGIC=0x4d4e4931,STATE_VERSION=1;
 private static final int MAX_HISTORY=16,MAX_ANNOTATIONS=64,MAX_TOTAL_POINTS=768,MAX_STATE_INTS=5000;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final Paint annotationPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final ArrayDeque<Snapshot> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
 private final ArrayList<Annotation> annotations=new ArrayList<>();
 private Bitmap source;
 private ImageEditPlan plan;
 private Snapshot gesture,drawingBefore;
 private Annotation drawing;
 private Runnable changed=()->{};
 private boolean cropMode,saving;
 private Tool drawingTool=Tool.NONE;
 private int drawingColor=Color.RED,handle=-1,pointer=-1,drawingPointer=-1;
 private float scale=1,ox,oy;

 ImageEditView(Context c){super(c);setFocusable(true);setContentDescription("Image preview. Use Crop presets, Highlight, Pen, or Shapes.");}
 void loaded(Bitmap bitmap,ImageEditPlan restored){source=bitmap;plan=restored!=null && restored.sourceWidth==bitmap.getWidth() && restored.sourceHeight==bitmap.getHeight()?restored:new ImageEditPlan(bitmap.getWidth(),bitmap.getHeight());notifyChanged();}
 boolean ready(){return source!=null && !source.isRecycled();}
 boolean saving(){return saving;}
 void saving(boolean value){saving=value;notifyChanged();}
 boolean dirty(){return plan!=null && (plan.changed() || !annotations.isEmpty());}
 ImageEditPlan plan(){return plan;}
 boolean cropMode(){return cropMode;}
 Tool drawingTool(){return drawingTool;}
 int drawingColor(){return drawingColor;}
 int annotationCount(){return annotations.size();}
 void settleGesture(){finishGesture(true);finishDrawing(true);}
 void cropMode(boolean value){if(saving)return;finishGesture(false);finishDrawing(false);cropMode=value;if(value)drawingTool=Tool.NONE;notifyChanged();}
 void drawingTool(Tool tool,int color){if(saving)return;finishGesture(false);finishDrawing(false);cropMode=false;drawingTool=tool==null?Tool.NONE:tool;drawingColor=color;notifyChanged();}
 boolean canUndo(){return !saving && !undo.isEmpty();}
 boolean canRedo(){return !saving && !redo.isEmpty();}
 void onStateChanged(Runnable listener){changed=listener;notifyChanged();}
 void edit(ImageEditPlan next){if(!ready() || saving || next==null)return;finishGesture(false);finishDrawing(false);if(plan.equals(next))return;remember(snapshot());plan=next;cropMode=false;drawingTool=Tool.NONE;notifyChanged();}
 void reset(){if(!ready() || saving)return;finishGesture(false);finishDrawing(false);ImageEditPlan original=new ImageEditPlan(source.getWidth(),source.getHeight());if(plan.equals(original) && annotations.isEmpty())return;remember(snapshot());plan=original;annotations.clear();cropMode=false;drawingTool=Tool.NONE;notifyChanged();}
 void undo(){finishGesture(false);finishDrawing(false);if(canUndo()){redo.addLast(snapshot());restore(undo.removeLast());notifyChanged();}}
 void redo(){finishGesture(false);finishDrawing(false);if(canRedo()){undo.addLast(snapshot());restore(redo.removeLast());notifyChanged();}}
 int[] historyState(boolean forward){List<Snapshot> values=new ArrayList<>(forward?redo:undo);while(values.size()>MAX_HISTORY)values.remove(0);while(values.size()>1 && encodedSize(values)>MAX_STATE_INTS)values.remove(0);return encodeSnapshots(values);}
 int[] annotationState(){return encodeSnapshots(java.util.Collections.singletonList(snapshot()));}
 void restoreAnnotations(int[] state){if(state==null || state.length==0)return;try{List<Snapshot> decoded=decodeSnapshots(state);if(decoded.size()!=1 || decoded.get(0).plan.sourceWidth!=plan.sourceWidth || decoded.get(0).plan.sourceHeight!=plan.sourceHeight || !decoded.get(0).plan.equals(plan))throw new IllegalArgumentException("Image preview state changed");annotations.clear();annotations.addAll(decoded.get(0).annotations);}catch(IllegalArgumentException invalid){annotations.clear();}notifyChanged();}
 void restoreHistory(int[] previous,int[] next){try{readHistory(undo,previous);readHistory(redo,next);}catch(IllegalArgumentException invalid){undo.clear();redo.clear();}notifyChanged();}
 private void readHistory(ArrayDeque<Snapshot> history,int[] values){
  history.clear();if(values==null || values.length==0)return;
  if(values[0]!=STATE_MAGIC){if(values.length>400 || values.length%8!=0)throw new IllegalArgumentException("Invalid preview history");for(int at=0;at<values.length;at+=8){int[] state=new int[8];System.arraycopy(values,at,state,0,8);ImageEditPlan item=ImageEditPlan.restore(state);if(item.sourceWidth!=plan.sourceWidth || item.sourceHeight!=plan.sourceHeight)throw new IllegalArgumentException("History source changed");history.addLast(new Snapshot(item,java.util.Collections.emptyList()));}return;}
  for(Snapshot value:decodeSnapshots(values)){if(value.plan.sourceWidth!=plan.sourceWidth || value.plan.sourceHeight!=plan.sourceHeight)throw new IllegalArgumentException("History source changed");history.addLast(value);while(history.size()>MAX_HISTORY)history.removeFirst();}
 }
 private Snapshot snapshot(){return new Snapshot(plan,new ArrayList<>(annotations));}
 private void restore(Snapshot value){plan=value.plan;annotations.clear();annotations.addAll(value.annotations);}
 private void remember(Snapshot before){undo.addLast(before);while(undo.size()>MAX_HISTORY)undo.removeFirst();redo.clear();}
 private void notifyChanged(){
  invalidate();if(plan!=null){String mode=cropMode?"Crop: drag a corner.":drawingTool!=Tool.NONE?"Draw with one finger: "+toolName(drawingTool)+".":"Choose Crop, Highlight, Pen, or Shapes.";setContentDescription("Image preview, "+plan.cropWidth()+" by "+plan.cropHeight()+" pixels. "+annotations.size()+" annotations. "+mode);}changed.run();
 }
 private static String toolName(Tool tool){switch(tool){case HIGHLIGHT:return "highlight";case PEN:return "pen";case RECTANGLE:return "rectangle";case OVAL:return "oval";case ARROW:return "arrow";default:return "browse";}}
 static Matrix matrix(ImageEditPlan p){float[] m=p.matrix();Matrix value=new Matrix();value.setValues(new float[]{m[0],m[1],m[2],m[3],m[4],m[5],0,0,1});return value;}
 /** Called only on the disk worker, with its own bitmap, never this view's bitmap. */
 static Bitmap render(Bitmap bitmap,ImageEditPlan plan){return render(bitmap,plan,null);}
 static Bitmap render(Bitmap bitmap,ImageEditPlan plan,int[] annotationState){
  List<Annotation> marks=annotationState==null?java.util.Collections.emptyList():decodeAnnotations(annotationState,plan);
  Bitmap out=Bitmap.createBitmap(plan.cropWidth(),plan.cropHeight(),Bitmap.Config.ARGB_8888);
  try{
   out.setHasAlpha(bitmap.hasAlpha());Canvas canvas=new Canvas(out);canvas.translate(-plan.left,-plan.top);canvas.drawBitmap(bitmap,matrix(plan),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));canvas.concat(matrix(plan));Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);for(Annotation mark:marks)drawAnnotation(canvas,ink,mark);return out;
  }catch(RuntimeException | OutOfMemoryError failure){out.recycle();throw failure;}
 }
 private static List<Annotation> decodeAnnotations(int[] state,ImageEditPlan plan){
  List<Snapshot> decoded=decodeSnapshots(state);if(decoded.size()!=1 || !decoded.get(0).plan.equals(plan))throw new IllegalArgumentException("Image annotation preview changed");return decoded.get(0).annotations;
 }
 private static int encodedSize(List<Snapshot> snapshots){int size=3;for(Snapshot snapshot:snapshots){size+=9;for(Annotation annotation:snapshot.annotations)size+=4+annotation.points.length;}return size;}
 private static int[] encodeSnapshots(List<Snapshot> snapshots){
  if(snapshots.size()>MAX_HISTORY)throw new IllegalArgumentException("Image preview history limit exceeded");int size=encodedSize(snapshots);
  for(Snapshot snapshot:snapshots){int points=0;for(Annotation annotation:snapshot.annotations)points+=annotation.pointCount();if(snapshot.annotations.size()>MAX_ANNOTATIONS || points>MAX_TOTAL_POINTS)throw new IllegalArgumentException("Image annotation limit exceeded");}if(size>MAX_STATE_INTS)throw new IllegalArgumentException("Image preview history limit exceeded");
  int[] result=new int[size];int at=0;result[at++]=STATE_MAGIC;result[at++]=STATE_VERSION;result[at++]=snapshots.size();
  for(Snapshot snapshot:snapshots){for(int value:snapshot.plan.state())result[at++]=value;result[at++]=snapshot.annotations.size();for(Annotation annotation:snapshot.annotations){result[at++]=annotation.kind.ordinal();result[at++]=annotation.color;result[at++]=Float.floatToIntBits(annotation.width);result[at++]=annotation.pointCount();for(float value:annotation.points)result[at++]=Float.floatToIntBits(value);}}
  return result;
 }
 private static List<Snapshot> decodeSnapshots(int[] values){
  if(values==null || values.length<3 || values.length>MAX_STATE_INTS || values[0]!=STATE_MAGIC || values[1]!=STATE_VERSION)throw new IllegalArgumentException("Invalid image preview state");int count=values[2];if(count<0 || count>MAX_HISTORY)throw new IllegalArgumentException("Invalid image preview history size");int at=3;List<Snapshot> decoded=new ArrayList<>();
  for(int n=0;n<count;n++){
   if(at+9>values.length)throw new IllegalArgumentException("Truncated image preview state");int[] geometry=new int[8];System.arraycopy(values,at,geometry,0,8);at+=8;ImageEditPlan geometryPlan=ImageEditPlan.restore(geometry);int markCount=values[at++];if(markCount<0 || markCount>MAX_ANNOTATIONS)throw new IllegalArgumentException("Invalid image annotation count");int totalPoints=0;List<Annotation> marks=new ArrayList<>();
   for(int i=0;i<markCount;i++){
    if(at+4>values.length)throw new IllegalArgumentException("Truncated image annotation");int kind=values[at++],color=values[at++];float width=Float.intBitsToFloat(values[at++]);int pointCount=values[at++];if(kind<0 || kind>=Tool.values().length-1 || pointCount<1 || pointCount>MAX_TOTAL_POINTS || at+pointCount*2>values.length)throw new IllegalArgumentException("Invalid image annotation");float[] points=new float[pointCount*2];for(int p=0;p<points.length;p++)points[p]=Float.intBitsToFloat(values[at++]);totalPoints+=pointCount;if(totalPoints>MAX_TOTAL_POINTS)throw new IllegalArgumentException("Image annotation point limit exceeded");Annotation mark=new Annotation(Tool.values()[kind+1],color,width,points);validateAnnotation(mark,geometryPlan);marks.add(mark);
   }
   decoded.add(new Snapshot(geometryPlan,marks));
  }
  if(at!=values.length)throw new IllegalArgumentException("Trailing image preview state");return decoded;
 }
 private static void validateAnnotation(Annotation mark,ImageEditPlan plan){
  if(mark.points.length<2 || mark.points.length%2!=0 || !finite(mark.width) || mark.width<=0 || mark.width>Math.max(plan.sourceWidth,plan.sourceHeight))throw new IllegalArgumentException("Invalid annotation geometry");
  if((mark.tool==Tool.RECTANGLE || mark.tool==Tool.OVAL || mark.tool==Tool.ARROW) && mark.pointCount()!=2)throw new IllegalArgumentException("Invalid shape geometry");
  for(int i=0;i<mark.points.length;i+=2)if(!finite(mark.points[i]) || !finite(mark.points[i+1]) || mark.points[i]<0 || mark.points[i]>plan.sourceWidth || mark.points[i+1]<0 || mark.points[i+1]>plan.sourceHeight)throw new IllegalArgumentException("Annotation outside image");
 }
 private static boolean finite(float value){return !Float.isNaN(value) && !Float.isInfinite(value);}
 private static void drawAnnotation(Canvas canvas,Paint ink,Annotation mark){
  ink.setColor(mark.color);ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(mark.width);ink.setStrokeCap(Paint.Cap.ROUND);ink.setStrokeJoin(Paint.Join.ROUND);float[] points=mark.points;int count=mark.pointCount();
  if(mark.tool==Tool.HIGHLIGHT || mark.tool==Tool.PEN){if(count==1){canvas.drawCircle(points[0],points[1],Math.max(.5f,mark.width/2),ink);}else{Path path=new Path();path.moveTo(points[0],points[1]);for(int i=1;i<count;i++)path.lineTo(points[i*2],points[i*2+1]);canvas.drawPath(path,ink);}return;}
  float x1=points[0],y1=points[1],x2=points[2],y2=points[3];if(mark.tool==Tool.RECTANGLE){canvas.drawRect(Math.min(x1,x2),Math.min(y1,y2),Math.max(x1,x2),Math.max(y1,y2),ink);return;}if(mark.tool==Tool.OVAL){canvas.drawOval(Math.min(x1,x2),Math.min(y1,y2),Math.max(x1,x2),Math.max(y1,y2),ink);return;}
  canvas.drawLine(x1,y1,x2,y2,ink);double angle=Math.atan2(y2-y1,x2-x1);float head=Math.max(mark.width*4,10);float left=(float)(angle+Math.PI*.82),right=(float)(angle-Math.PI*.82);Path arrow=new Path();arrow.moveTo(x2,y2);arrow.lineTo(x2+(float)Math.cos(left)*head,y2+(float)Math.sin(left)*head);arrow.moveTo(x2,y2);arrow.lineTo(x2+(float)Math.cos(right)*head,y2+(float)Math.sin(right)*head);canvas.drawPath(arrow,ink);
 }
 private float dp(float n){return n*getResources().getDisplayMetrics().density;}
 private float[] screenToSource(float x,float y){
  float oxp=(x-ox)/scale,oyp=(y-oy)/scale;if(oxp<plan.left || oxp>plan.right || oyp<plan.top || oyp>plan.bottom)return null;float[] m=plan.matrix();float dx=oxp-m[2],dy=oyp-m[5],det=m[0]*m[4]-m[1]*m[3];if(det==0)return null;float sx=(m[4]*dx-m[1]*dy)/det,sy=(-m[3]*dx+m[0]*dy)/det;return new float[]{Math.max(0,Math.min(plan.sourceWidth,sx)),Math.max(0,Math.min(plan.sourceHeight,sy))};
 }
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);canvas.drawColor(0xff242424);if(!ready() || plan==null)return;
  float margin=dp(28);scale=Math.max(.001f,Math.min(Math.max(1,getWidth()-margin*2)/plan.width(),Math.max(1,getHeight()-margin*2)/plan.height()));ox=(getWidth()-plan.width()*scale)/2;oy=(getHeight()-plan.height()*scale)/2;
  canvas.save();canvas.translate(ox,oy);canvas.scale(scale,scale);paint.setColor(Color.WHITE);canvas.drawRect(0,0,plan.width(),plan.height(),paint);canvas.drawBitmap(source,matrix(plan),paint);canvas.save();canvas.concat(matrix(plan));for(Annotation mark:annotations)drawAnnotation(canvas,annotationPaint,mark);if(drawing!=null)drawAnnotation(canvas,annotationPaint,drawing);canvas.restore();canvas.restore();
  float l=ox+plan.left*scale,t=oy+plan.top*scale,r=ox+plan.right*scale,b=oy+plan.bottom*scale;
  paint.setColor(0xa0000000);canvas.drawRect(ox,oy,ox+plan.width()*scale,t,paint);canvas.drawRect(ox,b,ox+plan.width()*scale,oy+plan.height()*scale,paint);canvas.drawRect(ox,t,l,b,paint);canvas.drawRect(r,t,ox+plan.width()*scale,b,paint);
  if(cropMode){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setColor(0xffffd477);canvas.drawRect(l,t,r,b,paint);paint.setStyle(Paint.Style.FILL);for(int i=0;i<4;i++){float x=i%2==0?l:r,y=i<2?t:b;paint.setColor(Color.BLACK);canvas.drawCircle(x,y,dp(9),paint);paint.setColor(0xffffd477);canvas.drawCircle(x,y,dp(7),paint);}}
 }
 @Override public boolean onTouchEvent(MotionEvent event){
  if(!ready() || saving || plan==null)return false;if(cropMode)return touchCrop(event);if(drawingTool!=Tool.NONE)return touchDrawing(event);return false;
 }
 private boolean touchCrop(MotionEvent event){
  int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN){float best=dp(28);handle=-1;for(int i=0;i<4;i++){float x=ox+(i%2==0?plan.left:plan.right)*scale,y=oy+(i<2?plan.top:plan.bottom)*scale;float distance=(float)Math.hypot(event.getX()-x,event.getY()-y);if(distance<=best){best=distance;handle=i;}}if(handle<0)return false;gesture=snapshot();pointer=event.getPointerId(0);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return true;}
  if(gesture==null)return false;if(action==MotionEvent.ACTION_CANCEL || event.getPointerCount()!=1){finishGesture(false);return true;}
  if(action==MotionEvent.ACTION_MOVE){int index=event.findPointerIndex(pointer);if(index<0){finishGesture(false);return true;}int x=Math.round((event.getX(index)-ox)/scale),y=Math.round((event.getY(index)-oy)/scale);int l=gesture.plan.left,t=gesture.plan.top,r=gesture.plan.right,b=gesture.plan.bottom;int minW=Math.min(16,gesture.plan.cropWidth()),minH=Math.min(16,gesture.plan.cropHeight());if(handle%2==0)l=Math.max(0,Math.min(r-minW,x));else r=Math.min(plan.width(),Math.max(l+minW,x));if(handle<2)t=Math.max(0,Math.min(b-minH,y));else b=Math.min(plan.height(),Math.max(t+minH,y));plan=gesture.plan.crop(l,t,r,b);invalidate();return true;}
  if(action==MotionEvent.ACTION_UP){finishGesture(true);performClick();return true;}return true;
 }
 private boolean touchDrawing(MotionEvent event){
  int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN){float[] point=screenToSource(event.getX(),event.getY());if(point==null)return false;drawingBefore=snapshot();drawingPointer=event.getPointerId(0);float width=(drawingTool==Tool.HIGHLIGHT?dp(22):dp(3))/scale;drawing=new Annotation(drawingTool,drawingColor,Math.max(.5f,width),drawingTool==Tool.RECTANGLE || drawingTool==Tool.OVAL || drawingTool==Tool.ARROW?new float[]{point[0],point[1],point[0],point[1]}:new float[]{point[0],point[1]});if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;}
  if(drawing==null)return false;if(action==MotionEvent.ACTION_CANCEL || event.getPointerCount()!=1){finishDrawing(false);return true;}int index=event.findPointerIndex(drawingPointer);if(index<0){finishDrawing(false);return true;}
  if(action==MotionEvent.ACTION_MOVE || action==MotionEvent.ACTION_UP){float[] point=screenToSource(event.getX(index),event.getY(index));if(point!=null)updateDrawing(point);if(action==MotionEvent.ACTION_UP){finishDrawing(true);performClick();}return true;}return true;
 }
 private void updateDrawing(float[] point){
  if(drawing.tool==Tool.RECTANGLE || drawing.tool==Tool.OVAL || drawing.tool==Tool.ARROW){drawing.points[2]=point[0];drawing.points[3]=point[1];}
  else{int count=drawing.pointCount();float x=drawing.points[(count-1)*2],y=drawing.points[(count-1)*2+1];if(Math.hypot(point[0]-x,point[1]-y)>=Math.max(.5f,drawing.width*.25f) && count<MAX_TOTAL_POINTS && totalPoints(annotations)+count<MAX_TOTAL_POINTS){float[] next=Arrays.copyOf(drawing.points,drawing.points.length+2);next[next.length-2]=point[0];next[next.length-1]=point[1];drawing.points=next;}}
  invalidate();
 }
 private void finishDrawing(boolean accept){
  if(drawing==null)return;if(accept){boolean shape=drawing.tool==Tool.RECTANGLE || drawing.tool==Tool.OVAL || drawing.tool==Tool.ARROW;boolean visible=!shape || Math.abs(drawing.points[2]-drawing.points[0])>=1 || Math.abs(drawing.points[3]-drawing.points[1])>=1;if(visible && annotations.size()<MAX_ANNOTATIONS && totalPoints(annotations)+drawing.pointCount()<=MAX_TOTAL_POINTS){validateAnnotation(drawing,plan);annotations.add(drawing);remember(drawingBefore);}else if(visible)NotesNotice.show(getContext(),"Annotation limit reached; save this image before adding more marks.");}else if(drawingBefore!=null)restore(drawingBefore);
  drawing=null;drawingBefore=null;drawingPointer=-1;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);notifyChanged();
 }
 private void finishGesture(boolean accept){if(gesture==null)return;if(accept && !gesture.plan.equals(plan))remember(gesture);else if(!accept)restore(gesture);gesture=null;handle=-1;pointer=-1;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);notifyChanged();}
 @Override public boolean performClick(){super.performClick();return true;}
 @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();if(source!=null){source.recycle();source=null;}}
 private static int totalPoints(List<Annotation> marks){int count=0;for(Annotation mark:marks)count+=mark.pointCount();return count;}
 private static final class Snapshot {final ImageEditPlan plan;final List<Annotation> annotations;Snapshot(ImageEditPlan plan,List<Annotation> annotations){this.plan=plan;this.annotations=java.util.Collections.unmodifiableList(new ArrayList<>(annotations));}}
 private static final class Annotation {
  final Tool tool;final int kind,color;final float width;float[] points;
  Annotation(Tool tool,int color,float width,float[] points){this.tool=tool;this.kind=tool.ordinal()-1;this.color=color;this.width=width;this.points=points.clone();}
  int pointCount(){return points.length/2;}
 }
}
