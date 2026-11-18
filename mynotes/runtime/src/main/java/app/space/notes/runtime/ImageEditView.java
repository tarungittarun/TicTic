package app.space.notes.runtime;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import app.space.notes.domain.ImageEditPlan;
import app.space.notes.domain.RichText;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Local preview owns one decoded bitmap. Crops and vector annotations never copy pixels. */
final class ImageEditView extends View {
 enum Tool { NONE, HIGHLIGHT, PEN, RECTANGLE, OVAL, ARROW, LINE, REDACT, TEXT }
 interface TextRequest { void place(float sourceX,float sourceY); }
 private static final int STATE_MAGIC=0x4d4e4931,STATE_VERSION=3;
 private static final int MAX_HISTORY=16,MAX_ANNOTATIONS=64,MAX_TOTAL_POINTS=768,MAX_STATE_INTS=5000;
 private static final int MAX_ANNOTATION_TEXT=256,MAX_TOTAL_TEXT=1024;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final Paint annotationPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final ArrayDeque<Snapshot> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
 private final ArrayList<Annotation> annotations=new ArrayList<>();
 private final ScaleGestureDetector zoomDetector;
 private Bitmap source;
 private ImageEditPlan plan;
 private Snapshot gesture,drawingBefore;
 private Annotation drawing;
 private Runnable changed=()->{};
 private TextRequest textRequest=(x,y)->{};
 private boolean cropMode,saving,navigating,multiFingerNavigation,scaledByDetector;
 private Tool drawingTool=Tool.NONE;
 private int drawingColor=Color.RED,drawingFont,handle=-1,pointer=-1,drawingPointer=-1,textPointer=-1,selectedText=-1,movingText=-1;
 private float drawingWidthDp=3f,lastTextX,lastTextY,scale=1,fitScale=1,ox,oy,zoom=1,panX,panY,lastNavX,lastNavY,lastNavSpan;
 private boolean textDidMove;private Snapshot movingTextBefore;

 ImageEditView(Context c){
  super(c);setFocusable(true);setContentDescription("Image preview. Pinch to zoom; drag with two fingers to pan. Use Text, Cover, Highlight, Pen, Shapes, or Crop.");
  zoomDetector=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
   @Override public boolean onScaleBegin(ScaleGestureDetector detector){return ready() && !saving;}
   @Override public boolean onScale(ScaleGestureDetector detector){scaledByDetector=true;zoomAt(detector.getFocusX(),detector.getFocusY(),detector.getScaleFactor());invalidate();return true;}
  });
 }
 void loaded(Bitmap bitmap,ImageEditPlan restored){source=bitmap;plan=restored!=null && restored.sourceWidth==bitmap.getWidth() && restored.sourceHeight==bitmap.getHeight()?restored:new ImageEditPlan(bitmap.getWidth(),bitmap.getHeight());updateViewport();notifyChanged();}
 boolean ready(){return source!=null && !source.isRecycled();}
 boolean saving(){return saving;}
 void saving(boolean value){saving=value;notifyChanged();}
 boolean dirty(){return plan!=null && (plan.changed() || !annotations.isEmpty());}
 ImageEditPlan plan(){return plan;}
 boolean cropMode(){return cropMode;}
 Tool drawingTool(){return drawingTool;}
 int drawingColor(){return drawingColor;}
 float drawingWidthDp(){return drawingWidthDp;}
 int drawingFont(){return drawingFont;}
 boolean textSelected(){return selectedText>=0 && selectedText<annotations.size() && annotations.get(selectedText).tool==Tool.TEXT;}
 int annotationCount(){return annotations.size();}
 float zoomScale(){return zoom;}
 float panOffsetX(){return panX;}
 float panOffsetY(){return panY;}
 float previewScale(){updateViewport();return scale;}
 float previewOriginX(){updateViewport();return ox;}
 float previewOriginY(){updateViewport();return oy;}
 void onTextRequested(TextRequest request){textRequest=request==null?(x,y)->{}:request;}
 void settleGesture(){finishGesture(true);finishDrawing(true);finishTextMove(true);}
 void cropMode(boolean value){if(saving)return;finishGesture(false);finishDrawing(false);finishTextMove(true);cropMode=value;if(value){drawingTool=Tool.NONE;selectedText=-1;}notifyChanged();}
 void drawingTool(Tool tool,int color){
  if(saving)return;finishGesture(false);finishDrawing(false);finishTextMove(true);cropMode=false;Tool next=tool==null?Tool.NONE:tool;
  if(next!=drawingTool)drawingWidthDp=next==Tool.HIGHLIGHT?22f:next==Tool.TEXT?26f:3f;
  drawingTool=next;drawingColor=color;if(next!=Tool.TEXT)selectedText=-1;notifyChanged();
 }
 void drawingColor(int color){if(saving)return;if(textSelected()){Annotation mark=annotations.get(selectedText);if(!updateSelectedTextStyleWidth(color,mark.width,mark.font))return;}drawingColor=color;notifyChanged();}
 void drawingWidthDp(float value){if(saving)return;if(!finite(value) || value<1 || value>64)throw new IllegalArgumentException("Image stroke size must be 1–64 dp");if(textSelected() && !updateSelectedTextStyle(drawingColor,value,drawingFont))return;drawingWidthDp=value;notifyChanged();}
 void drawingFont(int value){if(saving)return;if(value<0 || value>3)throw new IllegalArgumentException("Choose an available text font");if(textSelected()){Annotation mark=annotations.get(selectedText);if(!updateSelectedTextStyleWidth(mark.color,mark.width,value))return;}drawingFont=value;notifyChanged();}
 private boolean updateSelectedTextStyle(int color,float sizeDp,int font){return updateSelectedTextStyleWidth(color,Math.max(.5f,dp(sizeDp)/Math.max(.001f,scale)),font);}
 private boolean updateSelectedTextStyleWidth(int color,float width,int font){
  if(!textSelected())return true;Annotation old=annotations.get(selectedText);
  if(old.color==color && Math.abs(old.width-width)<.001f && old.font==font)return true;
  Annotation updated=new Annotation(Tool.TEXT,color,width,old.points,old.text,font);RectF bounds=textBounds(updated);
  if(bounds.width()>source.getWidth() || bounds.height()>source.getHeight()){NotesNotice.show(getContext(),"Text does not fit this image at that size. Choose a smaller size or font.");return false;}
  float x=updated.points[0],y=updated.points[1],minX=x-bounds.left,maxX=source.getWidth()-(bounds.right-x),minY=y-bounds.top,maxY=source.getHeight()-(bounds.bottom-y);
  updated.points[0]=Math.max(minX,Math.min(maxX,x));updated.points[1]=Math.max(minY,Math.min(maxY,y));validateAnnotation(updated,plan);
  Snapshot before=snapshot();annotations.set(selectedText,updated);remember(before);return true;
 }
 private void syncTextToolStyle(){
  if(!textSelected())return;Annotation mark=annotations.get(selectedText);float density=Math.max(.001f,dpDensity());drawingColor=mark.color;drawingWidthDp=Math.max(1,Math.min(64,mark.width*scale/density));drawingFont=mark.font;
 }
 boolean canUndo(){return !saving && !undo.isEmpty();}
 boolean canRedo(){return !saving && !redo.isEmpty();}
 void onStateChanged(Runnable listener){changed=listener;notifyChanged();}
 void edit(ImageEditPlan next){if(!ready() || saving || next==null)return;finishGesture(false);finishDrawing(false);finishTextMove(true);if(plan.equals(next))return;remember(snapshot());plan=next;cropMode=false;drawingTool=Tool.NONE;updateViewport();notifyChanged();}
 void reset(){if(!ready() || saving)return;finishGesture(false);finishDrawing(false);finishTextMove(false);selectedText=-1;ImageEditPlan original=new ImageEditPlan(source.getWidth(),source.getHeight());if(plan.equals(original) && annotations.isEmpty())return;remember(snapshot());plan=original;annotations.clear();cropMode=false;drawingTool=Tool.NONE;updateViewport();notifyChanged();}
 void fitToScreen(){zoom=1;panX=panY=0;updateViewport();invalidate();}
 void undo(){finishGesture(false);finishDrawing(false);finishTextMove(false);if(canUndo()){redo.addLast(snapshot());restore(undo.removeLast());updateViewport();notifyChanged();}}
 void redo(){finishGesture(false);finishDrawing(false);finishTextMove(false);if(canRedo()){undo.addLast(snapshot());restore(redo.removeLast());updateViewport();notifyChanged();}}
 int[] historyState(boolean forward){List<Snapshot> values=new ArrayList<>(forward?redo:undo);while(values.size()>MAX_HISTORY)values.remove(0);while(values.size()>1 && encodedSize(values)>MAX_STATE_INTS)values.remove(0);return encodeSnapshots(values);}
 int[] annotationState(){return encodeSnapshots(java.util.Collections.singletonList(snapshot()));}
 void restoreAnnotations(int[] state){if(state==null || state.length==0)return;try{List<Snapshot> decoded=decodeSnapshots(state);if(decoded.size()!=1 || decoded.get(0).plan.sourceWidth!=plan.sourceWidth || decoded.get(0).plan.sourceHeight!=plan.sourceHeight || !decoded.get(0).plan.equals(plan))throw new IllegalArgumentException("Image preview state changed");annotations.clear();annotations.addAll(decoded.get(0).annotations);selectedText=-1;}catch(IllegalArgumentException invalid){annotations.clear();selectedText=-1;}notifyChanged();}
 void restoreHistory(int[] previous,int[] next){try{readHistory(undo,previous);readHistory(redo,next);}catch(IllegalArgumentException invalid){undo.clear();redo.clear();}notifyChanged();}
 private void readHistory(ArrayDeque<Snapshot> history,int[] values){
  history.clear();if(values==null || values.length==0)return;
  if(values[0]!=STATE_MAGIC){if(values.length>400 || values.length%8!=0)throw new IllegalArgumentException("Invalid preview history");for(int at=0;at<values.length;at+=8){int[] state=new int[8];System.arraycopy(values,at,state,0,8);ImageEditPlan item=ImageEditPlan.restore(state);if(item.sourceWidth!=plan.sourceWidth || item.sourceHeight!=plan.sourceHeight)throw new IllegalArgumentException("History source changed");history.addLast(new Snapshot(item,java.util.Collections.emptyList()));}return;}
  for(Snapshot value:decodeSnapshots(values)){if(value.plan.sourceWidth!=plan.sourceWidth || value.plan.sourceHeight!=plan.sourceHeight)throw new IllegalArgumentException("History source changed");history.addLast(value);while(history.size()>MAX_HISTORY)history.removeFirst();}
 }
 private Snapshot snapshot(){List<Annotation> copy=new ArrayList<>();for(Annotation annotation:annotations)copy.add(copyAnnotation(annotation));return new Snapshot(plan,copy);}
 private static Annotation copyAnnotation(Annotation annotation){return new Annotation(annotation.tool,annotation.color,annotation.width,annotation.points,annotation.text,annotation.font);}
 private void restore(Snapshot value){plan=value.plan;updateViewport();annotations.clear();for(Annotation annotation:value.annotations)annotations.add(copyAnnotation(annotation));if(selectedText>=annotations.size() || selectedText>=0 && annotations.get(selectedText).tool!=Tool.TEXT)selectedText=-1;syncTextToolStyle();}
 private void remember(Snapshot before){undo.addLast(before);while(undo.size()>MAX_HISTORY)undo.removeFirst();redo.clear();}
 private void notifyChanged(){
  invalidate();if(plan!=null){String mode=cropMode?"Crop: drag a highlighted corner.":drawingTool==Tool.TEXT?(textSelected()?"Text box selected. Drag it to move; tap an empty image area to add text.":"Tap the image to place text."):drawingTool==Tool.REDACT?"Opaque black cover: drag across details. This is not secure data sanitization.":drawingTool!=Tool.NONE?"Draw with one finger: "+toolName(drawingTool)+".":"Pinch to zoom; use two fingers to pan. Choose a tool to edit.";setContentDescription("Image preview, "+plan.cropWidth()+" by "+plan.cropHeight()+" pixels. "+annotations.size()+" annotations. "+mode);}changed.run();
 }
 private static String toolName(Tool tool){switch(tool){case HIGHLIGHT:return "highlight";case PEN:return "pen";case RECTANGLE:return "rectangle";case OVAL:return "oval";case ARROW:return "arrow";case LINE:return "line";case REDACT:return "black cover";case TEXT:return "text";default:return "browse";}}
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
 void addText(String value,float sourceX,float sourceY){addText(value,sourceX,sourceY,drawingColor,drawingWidthDp,drawingFont);}
 void addText(String value,float sourceX,float sourceY,int color,float sizeDp,int font){
  if(!ready() || saving || plan==null)throw new IllegalStateException("Image preview is not ready");
  RichText.requireText(value);if(value.trim().isEmpty())throw new IllegalArgumentException("Enter text to add");if(value.length()>MAX_ANNOTATION_TEXT)throw new IllegalArgumentException("Image text is limited to 256 characters");
  int totalText=0;for(Annotation mark:annotations)totalText+=mark.text.length();if(annotations.size()>=MAX_ANNOTATIONS || totalText+value.length()>MAX_TOTAL_TEXT)throw new IllegalArgumentException("Image annotation limit reached; save or reset edits first");
  if(totalPoints(annotations)>=MAX_TOTAL_POINTS)throw new IllegalArgumentException("Image annotation limit reached; save or reset edits first");
  if(font<0 || font>3 || !finite(sizeDp) || sizeDp<1 || sizeDp>64)throw new IllegalArgumentException("Choose a valid text style");float size=Math.max(.5f,dp(sizeDp)/Math.max(.001f,scale));float x=Math.max(0,Math.min(source.getWidth(),sourceX)),y=Math.max(0,Math.min(source.getHeight()-Math.min(size,source.getHeight()),sourceY));
  Annotation mark=new Annotation(Tool.TEXT,color,size,new float[]{x,y+Math.min(size,source.getHeight())},value,font);RectF bounds=textBounds(mark);if(bounds.width()>source.getWidth() || bounds.height()>source.getHeight())throw new IllegalArgumentException("Text does not fit this image at the selected size");float minX=mark.points[0]-bounds.left,maxX=source.getWidth()-(bounds.right-mark.points[0]),minY=mark.points[1]-bounds.top,maxY=source.getHeight()-(bounds.bottom-mark.points[1]);mark.points[0]=Math.max(minX,Math.min(maxX,mark.points[0]));mark.points[1]=Math.max(minY,Math.min(maxY,mark.points[1]));validateAnnotation(mark,plan);remember(snapshot());annotations.add(mark);selectedText=annotations.size()-1;syncTextToolStyle();notifyChanged();
 }
 private static List<Annotation> decodeAnnotations(int[] state,ImageEditPlan plan){
  List<Snapshot> decoded=decodeSnapshots(state);if(decoded.size()!=1 || !decoded.get(0).plan.equals(plan))throw new IllegalArgumentException("Image annotation preview changed");return decoded.get(0).annotations;
 }
 private static int encodedSize(List<Snapshot> snapshots){int size=3;for(Snapshot snapshot:snapshots){size+=9;for(Annotation annotation:snapshot.annotations)size+=6+annotation.points.length+annotation.text.length();}return size;}
 private static int[] encodeSnapshots(List<Snapshot> snapshots){
  if(snapshots.size()>MAX_HISTORY)throw new IllegalArgumentException("Image preview history limit exceeded");int size=encodedSize(snapshots);
  for(Snapshot snapshot:snapshots){int points=0,text=0;for(Annotation annotation:snapshot.annotations){points+=annotation.pointCount();text+=annotation.text.length();validateAnnotation(annotation,snapshot.plan);}if(snapshot.annotations.size()>MAX_ANNOTATIONS || points>MAX_TOTAL_POINTS || text>MAX_TOTAL_TEXT)throw new IllegalArgumentException("Image annotation limit exceeded");}if(size>MAX_STATE_INTS)throw new IllegalArgumentException("Image preview history limit exceeded");
  int[] result=new int[size];int at=0;result[at++]=STATE_MAGIC;result[at++]=STATE_VERSION;result[at++]=snapshots.size();
  for(Snapshot snapshot:snapshots){for(int value:snapshot.plan.state())result[at++]=value;result[at++]=snapshot.annotations.size();for(Annotation annotation:snapshot.annotations){result[at++]=annotation.kind;result[at++]=annotation.color;result[at++]=Float.floatToIntBits(annotation.width);result[at++]=annotation.pointCount();for(float value:annotation.points)result[at++]=Float.floatToIntBits(value);result[at++]=annotation.text.length();for(int i=0;i<annotation.text.length();i++)result[at++]=annotation.text.charAt(i);result[at++]=annotation.font;}}
  return result;
 }
 private static List<Snapshot> decodeSnapshots(int[] values){
  if(values==null || values.length<3 || values.length>MAX_STATE_INTS || values[0]!=STATE_MAGIC || (values[1]<1 || values[1]>STATE_VERSION))throw new IllegalArgumentException("Invalid image preview state");int version=values[1],count=values[2];if(count<0 || count>MAX_HISTORY)throw new IllegalArgumentException("Invalid image preview history size");int at=3;List<Snapshot> decoded=new ArrayList<>();
  for(int n=0;n<count;n++){
   if(at+9>values.length)throw new IllegalArgumentException("Truncated image preview state");int[] geometry=new int[8];System.arraycopy(values,at,geometry,0,8);at+=8;ImageEditPlan geometryPlan=ImageEditPlan.restore(geometry);int markCount=values[at++];if(markCount<0 || markCount>MAX_ANNOTATIONS)throw new IllegalArgumentException("Invalid image annotation count");int totalPoints=0,totalText=0;List<Annotation> marks=new ArrayList<>();
   for(int i=0;i<markCount;i++){
    if(at+4>values.length)throw new IllegalArgumentException("Truncated image annotation");int kind=values[at++],color=values[at++];float width=Float.intBitsToFloat(values[at++]);int pointCount=values[at++];int maxKind=version==1?Tool.ARROW.ordinal()-1:Tool.values().length-2;if(kind<0 || kind>maxKind || pointCount<1 || pointCount>MAX_TOTAL_POINTS || at+pointCount*2>values.length)throw new IllegalArgumentException("Invalid image annotation");float[] points=new float[pointCount*2];for(int p=0;p<points.length;p++)points[p]=Float.intBitsToFloat(values[at++]);String text="";
    if(version>=2){if(at>=values.length)throw new IllegalArgumentException("Truncated image annotation text");int length=values[at++];if(length<0 || length>MAX_ANNOTATION_TEXT || at+length>values.length)throw new IllegalArgumentException("Invalid image annotation text");StringBuilder value=new StringBuilder(length);for(int c=0;c<length;c++)value.append((char)values[at++]);text=value.toString();totalText+=length;if(totalText>MAX_TOTAL_TEXT)throw new IllegalArgumentException("Image annotation text limit exceeded");}
    int font=0;if(version>=3){if(at>=values.length)throw new IllegalArgumentException("Truncated image text font");font=values[at++];}
    totalPoints+=pointCount;if(totalPoints>MAX_TOTAL_POINTS)throw new IllegalArgumentException("Image annotation point limit exceeded");Annotation mark=new Annotation(Tool.values()[kind+1],color,width,points,text,font);validateAnnotation(mark,geometryPlan);marks.add(mark);
   }
   decoded.add(new Snapshot(geometryPlan,marks));
  }
  if(at!=values.length)throw new IllegalArgumentException("Trailing image preview state");return decoded;
 }
 private static void validateAnnotation(Annotation mark,ImageEditPlan plan){
  if(mark.points.length<2 || mark.points.length%2!=0 || !finite(mark.width) || mark.width<=0 || mark.width>Math.max(plan.sourceWidth,plan.sourceHeight))throw new IllegalArgumentException("Invalid annotation geometry");
  boolean shape=mark.tool==Tool.RECTANGLE || mark.tool==Tool.OVAL || mark.tool==Tool.ARROW || mark.tool==Tool.LINE || mark.tool==Tool.REDACT;
  if(shape && mark.pointCount()!=2)throw new IllegalArgumentException("Invalid shape geometry");if(mark.tool==Tool.TEXT && mark.pointCount()!=1)throw new IllegalArgumentException("Invalid text geometry");
  if(mark.text.length()>MAX_ANNOTATION_TEXT)throw new IllegalArgumentException("Image annotation text limit exceeded");RichText.requireText(mark.text);if(mark.tool==Tool.TEXT?mark.text.trim().isEmpty():!mark.text.isEmpty())throw new IllegalArgumentException("Invalid image annotation text");if(mark.font<0 || mark.font>3 || mark.tool!=Tool.TEXT && mark.font!=0)throw new IllegalArgumentException("Invalid image text font");
  for(int i=0;i<mark.points.length;i+=2)if(!finite(mark.points[i]) || !finite(mark.points[i+1]) || mark.points[i]<0 || mark.points[i]>plan.sourceWidth || mark.points[i+1]<0 || mark.points[i+1]>plan.sourceHeight)throw new IllegalArgumentException("Annotation outside image");
 }
 private static boolean finite(float value){return !Float.isNaN(value) && !Float.isInfinite(value);}
 private static void drawAnnotation(Canvas canvas,Paint ink,Annotation mark){
  float[] points=mark.points;int count=mark.pointCount();
  if(mark.tool==Tool.TEXT){drawTextAnnotation(canvas,ink,mark);return;}
  ink.setColor(mark.color);ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(mark.width);ink.setStrokeCap(Paint.Cap.ROUND);ink.setStrokeJoin(Paint.Join.ROUND);
  if(mark.tool==Tool.HIGHLIGHT || mark.tool==Tool.PEN){if(count==1){canvas.drawCircle(points[0],points[1],Math.max(.5f,mark.width/2),ink);}else{Path path=new Path();path.moveTo(points[0],points[1]);for(int i=1;i<count;i++)path.lineTo(points[i*2],points[i*2+1]);canvas.drawPath(path,ink);}return;}
  float x1=points[0],y1=points[1],x2=points[2],y2=points[3];
  if(mark.tool==Tool.REDACT){boolean antiAlias=ink.isAntiAlias();ink.setAntiAlias(false);ink.setStyle(Paint.Style.FILL);ink.setColor(Color.BLACK);canvas.drawRect((float)Math.floor(Math.min(x1,x2)),(float)Math.floor(Math.min(y1,y2)),(float)Math.ceil(Math.max(x1,x2)),(float)Math.ceil(Math.max(y1,y2)),ink);ink.setAntiAlias(antiAlias);return;}
  if(mark.tool==Tool.RECTANGLE){canvas.drawRect(Math.min(x1,x2),Math.min(y1,y2),Math.max(x1,x2),Math.max(y1,y2),ink);return;}
  if(mark.tool==Tool.OVAL){canvas.drawOval(Math.min(x1,x2),Math.min(y1,y2),Math.max(x1,x2),Math.max(y1,y2),ink);return;}
  canvas.drawLine(x1,y1,x2,y2,ink);if(mark.tool==Tool.LINE)return;
  double angle=Math.atan2(y2-y1,x2-x1);float head=Math.max(mark.width*4,10);float left=(float)(angle+Math.PI*.82),right=(float)(angle-Math.PI*.82);Path arrow=new Path();arrow.moveTo(x2,y2);arrow.lineTo(x2+(float)Math.cos(left)*head,y2+(float)Math.sin(left)*head);arrow.moveTo(x2,y2);arrow.lineTo(x2+(float)Math.cos(right)*head,y2+(float)Math.sin(right)*head);canvas.drawPath(arrow,ink);
 }
 private static void drawTextAnnotation(Canvas canvas,Paint ink,Annotation mark){
  ink.setTypeface(textTypeface(mark.font));ink.setTextSize(mark.width);ink.setTextAlign(Paint.Align.LEFT);ink.setStrokeJoin(Paint.Join.ROUND);ink.setStrokeWidth(Math.max(1,mark.width*.10f));String[] lines=mark.text.split("\\n",-1);float baseline=mark.points[1];
  ink.setStyle(Paint.Style.STROKE);ink.setColor(Color.BLACK);for(String line:lines){canvas.drawText(line,mark.points[0],baseline,ink);baseline+=mark.width*1.2f;}
  baseline=mark.points[1];ink.setStyle(Paint.Style.FILL);ink.setColor(mark.color);for(String line:lines){canvas.drawText(line,mark.points[0],baseline,ink);baseline+=mark.width*1.2f;}ink.setTypeface(Typeface.DEFAULT);ink.setStyle(Paint.Style.STROKE);
 }
 private static Typeface textTypeface(int font){String[] families={"sans-serif-medium","serif","monospace","cursive"};return Typeface.create(families[Math.max(0,Math.min(3,font))],Typeface.BOLD);}
 private RectF textBounds(Annotation mark){Paint measure=new Paint(Paint.ANTI_ALIAS_FLAG);measure.setTypeface(textTypeface(mark.font));measure.setTextSize(mark.width);float width=0;String[] lines=mark.text.split("\\n",-1);for(String line:lines)width=Math.max(width,measure.measureText(line));Paint.FontMetrics metrics=measure.getFontMetrics();float pad=Math.max(2,mark.width*.18f),lineHeight=mark.width*1.2f;return new RectF(mark.points[0]-pad,mark.points[1]+metrics.ascent-pad,mark.points[0]+width+pad,mark.points[1]+Math.max(metrics.descent,metrics.ascent+lines.length*lineHeight)+pad);}
 private int hitText(float x,float y){for(int i=annotations.size()-1;i>=0;i--){Annotation mark=annotations.get(i);if(mark.tool!=Tool.TEXT)continue;RectF bounds=textBounds(mark);float padding=dp(12)/Math.max(.001f,scale);bounds.inset(-padding,-padding);if(bounds.contains(x,y))return i;}return -1;}
 private void drawTextSelection(Canvas canvas,Annotation mark){RectF bounds=textBounds(mark);annotationPaint.setStyle(Paint.Style.STROKE);annotationPaint.setStrokeWidth(Math.max(.5f,dp(1.5f)/Math.max(.001f,scale)));annotationPaint.setColor(0xff5577dd);canvas.drawRoundRect(bounds,dp(3)/Math.max(.001f,scale),dp(3)/Math.max(.001f,scale),annotationPaint);annotationPaint.setStyle(Paint.Style.FILL);float radius=dp(4)/Math.max(.001f,scale);for(int i=0;i<4;i++){float x=i%2==0?bounds.left:bounds.right,y=i<2?bounds.top:bounds.bottom;canvas.drawCircle(x,y,radius,annotationPaint);}}
 private float dp(float n){return n*getResources().getDisplayMetrics().density;}
 private float[] screenToSource(float x,float y){
  if(plan==null)return null;float oxp=(x-ox)/scale,oyp=(y-oy)/scale;if(oxp<plan.left || oxp>plan.right || oyp<plan.top || oyp>plan.bottom)return null;float[] m=plan.matrix();float dx=oxp-m[2],dy=oyp-m[5],det=m[0]*m[4]-m[1]*m[3];if(det==0)return null;float sx=(m[4]*dx-m[1]*dy)/det,sy=(-m[3]*dx+m[0]*dy)/det;return new float[]{Math.max(0,Math.min(plan.sourceWidth,sx)),Math.max(0,Math.min(plan.sourceHeight,sy))};
 }
 private boolean insideImage(float x,float y){float ix=(x-ox)/scale,iy=(y-oy)/scale;return ix>=0 && iy>=0 && ix<=plan.width() && iy<=plan.height();}
 private float dpDensity(){return getResources().getDisplayMetrics().density;}
 private void updateViewport(){
  if(plan==null || getWidth()<=0 || getHeight()<=0)return;float margin=dp(28);fitScale=Math.max(.001f,Math.min(Math.max(1,getWidth()-margin*2)/plan.width(),Math.max(1,getHeight()-margin*2)/plan.height()));scale=fitScale*zoom;
  float imageWidth=plan.width()*scale,imageHeight=plan.height()*scale,centerX=(getWidth()-imageWidth)/2,centerY=(getHeight()-imageHeight)/2;
  float minX=Math.min(0,getWidth()-imageWidth)-centerX,maxX=Math.max(0,getWidth()-imageWidth)-centerX,minY=Math.min(0,getHeight()-imageHeight)-centerY,maxY=Math.max(0,getHeight()-imageHeight)-centerY;
  panX=Math.max(minX,Math.min(maxX,panX));panY=Math.max(minY,Math.min(maxY,panY));ox=centerX+panX;oy=centerY+panY;
 }
 private void zoomAt(float focusX,float focusY,float factor){
  if(plan==null || getWidth()<=0 || getHeight()<=0)return;updateViewport();float imageX=(focusX-ox)/scale,imageY=(focusY-oy)/scale;float next=Math.max(1,Math.min(8,zoom*factor));if(next==zoom)return;zoom=next;
  float margin=dp(28);fitScale=Math.max(.001f,Math.min(Math.max(1,getWidth()-margin*2)/plan.width(),Math.max(1,getHeight()-margin*2)/plan.height()));scale=fitScale*zoom;float centerX=(getWidth()-plan.width()*scale)/2,centerY=(getHeight()-plan.height()*scale)/2;panX=focusX-imageX*scale-centerX;panY=focusY-imageY*scale-centerY;updateViewport();
 }
 private static float centroid(MotionEvent event,boolean x){float total=0;for(int i=0;i<event.getPointerCount();i++)total+=x?event.getX(i):event.getY(i);return total/Math.max(1,event.getPointerCount());}
 private static float pointerSpan(MotionEvent event){if(event.getPointerCount()<2)return 0;float x=centroid(event,true),y=centroid(event,false);double squared=0;for(int i=0;i<event.getPointerCount();i++){float dx=event.getX(i)-x,dy=event.getY(i)-y;squared+=(double)dx*dx+(double)dy*dy;}return (float)Math.sqrt(squared/event.getPointerCount());}
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);updateViewport();}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);canvas.drawColor(0xff242424);if(!ready() || plan==null)return;updateViewport();
  canvas.save();canvas.translate(ox,oy);canvas.scale(scale,scale);paint.setColor(Color.WHITE);canvas.drawRect(0,0,plan.width(),plan.height(),paint);canvas.drawBitmap(source,matrix(plan),paint);canvas.save();canvas.concat(matrix(plan));for(Annotation mark:annotations)drawAnnotation(canvas,annotationPaint,mark);if(drawing!=null)drawAnnotation(canvas,annotationPaint,drawing);if(textSelected())drawTextSelection(canvas,annotations.get(selectedText));canvas.restore();canvas.restore();
  float l=ox+plan.left*scale,t=oy+plan.top*scale,r=ox+plan.right*scale,b=oy+plan.bottom*scale;
  paint.setColor(0xa0000000);canvas.drawRect(ox,oy,ox+plan.width()*scale,t,paint);canvas.drawRect(ox,b,ox+plan.width()*scale,oy+plan.height()*scale,paint);canvas.drawRect(ox,t,l,b,paint);canvas.drawRect(r,t,ox+plan.width()*scale,b,paint);
  if(cropMode){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setColor(0xffffd477);canvas.drawRect(l,t,r,b,paint);paint.setStyle(Paint.Style.FILL);for(int i=0;i<4;i++){float x=i%2==0?l:r,y=i<2?t:b;paint.setColor(Color.BLACK);canvas.drawCircle(x,y,dp(9),paint);paint.setColor(0xffffd477);canvas.drawCircle(x,y,dp(7),paint);}}
 }
 @Override public boolean onTouchEvent(MotionEvent event){
  if(!ready() || saving || plan==null)return false;updateViewport();int action=event.getActionMasked();scaledByDetector=false;zoomDetector.onTouchEvent(event);
  if(action==MotionEvent.ACTION_POINTER_DOWN){if(gesture!=null)finishGesture(false);if(drawing!=null)finishDrawing(false);if(movingText>=0)finishTextMove(false);textPointer=-1;navigating=true;multiFingerNavigation=true;lastNavX=centroid(event,true);lastNavY=centroid(event,false);lastNavSpan=pointerSpan(event);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return true;}
  if(navigating)return touchNavigation(event);
  if(action==MotionEvent.ACTION_DOWN){
   if(cropMode && touchCrop(event))return true;
   if(drawingTool!=Tool.NONE && touchDrawing(event))return true;
   if(!insideImage(event.getX(),event.getY()))return false;navigating=true;multiFingerNavigation=false;lastNavX=event.getX();lastNavY=event.getY();if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return true;
  }
  if(cropMode)return touchCrop(event);if(drawingTool!=Tool.NONE)return touchDrawing(event);return false;
 }
 private boolean touchNavigation(MotionEvent event){
  int action=event.getActionMasked();if(action==MotionEvent.ACTION_MOVE){
   if(event.getPointerCount()>1){
    float x=centroid(event,true),y=centroid(event,false),span=pointerSpan(event);
    // Android's detector has a minimum span; keep short two-finger pinches usable too.
    if(!scaledByDetector && lastNavSpan>0 && span>0){float factor=span/lastNavSpan;if(Float.isFinite(factor) && Math.abs(factor-1f)>.001f)zoomAt(lastNavX,lastNavY,factor);}
    boolean stationaryPinch=zoomDetector.isInProgress() && Math.abs(zoomDetector.getScaleFactor()-1f)<.001f;
    if(!zoomDetector.isInProgress() || stationaryPinch){panX+=x-lastNavX;panY+=y-lastNavY;updateViewport();invalidate();}
    lastNavX=x;lastNavY=y;lastNavSpan=span;
   }
   else{float x=event.getX(),y=event.getY();boolean mayPan=zoom>1 && (multiFingerNavigation || drawingTool==Tool.NONE && !cropMode);if(mayPan){panX+=x-lastNavX;panY+=y-lastNavY;updateViewport();invalidate();}lastNavX=x;lastNavY=y;lastNavSpan=0;}
   return true;
  }
  if(action==MotionEvent.ACTION_POINTER_UP){lastNavSpan=0;int remaining=event.getActionIndex()==0?1:0;if(remaining<event.getPointerCount()){lastNavX=event.getX(remaining);lastNavY=event.getY(remaining);}return true;}
  if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL){navigating=false;multiFingerNavigation=false;lastNavSpan=0;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);if(action==MotionEvent.ACTION_UP)performClick();return true;}return true;
 }
 private boolean touchCrop(MotionEvent event){
  int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN){float best=dp(28);handle=-1;for(int i=0;i<4;i++){float x=ox+(i%2==0?plan.left:plan.right)*scale,y=oy+(i<2?plan.top:plan.bottom)*scale;float distance=(float)Math.hypot(event.getX()-x,event.getY()-y);if(distance<=best){best=distance;handle=i;}}if(handle<0)return false;gesture=snapshot();pointer=event.getPointerId(0);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return true;}
  if(gesture==null)return false;if(action==MotionEvent.ACTION_CANCEL || event.getPointerCount()!=1){finishGesture(false);return true;}
  if(action==MotionEvent.ACTION_MOVE){int index=event.findPointerIndex(pointer);if(index<0){finishGesture(false);return true;}int x=Math.round((event.getX(index)-ox)/scale),y=Math.round((event.getY(index)-oy)/scale);int l=gesture.plan.left,t=gesture.plan.top,r=gesture.plan.right,b=gesture.plan.bottom;int minW=Math.min(16,gesture.plan.cropWidth()),minH=Math.min(16,gesture.plan.cropHeight());if(handle%2==0)l=Math.max(0,Math.min(r-minW,x));else r=Math.min(plan.width(),Math.max(l+minW,x));if(handle<2)t=Math.max(0,Math.min(b-minH,y));else b=Math.min(plan.height(),Math.max(t+minH,y));plan=gesture.plan.crop(l,t,r,b);invalidate();return true;}
  if(action==MotionEvent.ACTION_UP){finishGesture(true);performClick();return true;}return true;
 }
 private boolean touchDrawing(MotionEvent event){
  int action=event.getActionMasked();if(movingText>=0)return touchTextMove(event);if(textPointer>=0){if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL)textPointer=-1;return true;}
  if(action==MotionEvent.ACTION_DOWN){float[] point=screenToSource(event.getX(),event.getY());if(point==null)return false;if(drawingTool==Tool.TEXT){int hit=hitText(point[0],point[1]);if(hit>=0){selectedText=hit;syncTextToolStyle();movingText=hit;movingTextBefore=snapshot();textDidMove=false;lastTextX=point[0];lastTextY=point[1];textPointer=event.getPointerId(0);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);notifyChanged();return true;}selectedText=-1;textPointer=event.getPointerId(0);textRequest.place(point[0],point[1]);performClick();notifyChanged();return true;}selectedText=-1;drawingBefore=snapshot();drawingPointer=event.getPointerId(0);float width=dp(drawingWidthDp)/scale;boolean shape=isShape(drawingTool);drawing=new Annotation(drawingTool,drawingColor,Math.max(.5f,width),shape?new float[]{point[0],point[1],point[0],point[1]}:new float[]{point[0],point[1]});if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;}
  if(drawing==null)return false;if(action==MotionEvent.ACTION_CANCEL || event.getPointerCount()!=1){finishDrawing(false);return true;}int index=event.findPointerIndex(drawingPointer);if(index<0){finishDrawing(false);return true;}
  if(action==MotionEvent.ACTION_MOVE || action==MotionEvent.ACTION_UP){float[] point=screenToSource(event.getX(index),event.getY(index));if(point!=null)updateDrawing(point);if(action==MotionEvent.ACTION_UP){finishDrawing(true);performClick();}return true;}return true;
 }
 private boolean touchTextMove(MotionEvent event){
  int action=event.getActionMasked();if(action==MotionEvent.ACTION_CANCEL || event.getPointerCount()!=1){finishTextMove(false);return true;}
  int index=event.findPointerIndex(textPointer);if(index<0){finishTextMove(false);return true;}
  if(action==MotionEvent.ACTION_MOVE || action==MotionEvent.ACTION_UP){float[] point=screenToSource(event.getX(index),event.getY(index));if(point!=null && movingText<annotations.size()){Annotation mark=annotations.get(movingText);float dx=point[0]-lastTextX,dy=point[1]-lastTextY;if(Math.abs(dx)>.001f || Math.abs(dy)>.001f){RectF bounds=textBounds(mark);dx=Math.max(-bounds.left,Math.min(source.getWidth()-bounds.right,dx));dy=Math.max(-bounds.top,Math.min(source.getHeight()-bounds.bottom,dy));mark.points[0]+=dx;mark.points[1]+=dy;lastTextX=point[0];lastTextY=point[1];textDidMove|=Math.abs(dx)>.001f || Math.abs(dy)>.001f;invalidate();}}if(action==MotionEvent.ACTION_UP){finishTextMove(true);performClick();}return true;}return true;
 }
 private void finishTextMove(boolean accept){if(movingText<0)return;if(accept && textDidMove && movingTextBefore!=null)remember(movingTextBefore);else if(!accept && movingTextBefore!=null)restore(movingTextBefore);movingText=-1;textPointer=-1;textDidMove=false;movingTextBefore=null;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);notifyChanged();}
 private static boolean isShape(Tool tool){return tool==Tool.RECTANGLE || tool==Tool.OVAL || tool==Tool.ARROW || tool==Tool.LINE || tool==Tool.REDACT;}
 void addRedaction(float x1,float y1,float x2,float y2){
  if(!ready() || saving || plan==null)throw new IllegalStateException("Image preview is not ready");if(annotations.size()>=MAX_ANNOTATIONS || totalPoints(annotations)+2>MAX_TOTAL_POINTS)throw new IllegalArgumentException("Image annotation limit reached; save or reset edits first");
  Annotation mask=new Annotation(Tool.REDACT,Color.BLACK,Math.max(.5f,dp(3)/Math.max(.001f,scale)),new float[]{x1,y1,x2,y2});validateAnnotation(mask,plan);remember(snapshot());annotations.add(mask);notifyChanged();
 }
 private void updateDrawing(float[] point){
  if(isShape(drawing.tool)){drawing.points[2]=point[0];drawing.points[3]=point[1];}
  else{int count=drawing.pointCount();float x=drawing.points[(count-1)*2],y=drawing.points[(count-1)*2+1];if(Math.hypot(point[0]-x,point[1]-y)>=Math.max(.5f,drawing.width*.25f) && count<MAX_TOTAL_POINTS && totalPoints(annotations)+count<MAX_TOTAL_POINTS){float[] next=Arrays.copyOf(drawing.points,drawing.points.length+2);next[next.length-2]=point[0];next[next.length-1]=point[1];drawing.points=next;}}
  invalidate();
 }
 private void finishDrawing(boolean accept){
  if(drawing==null)return;if(accept){boolean shape=isShape(drawing.tool);boolean visible=!shape || Math.abs(drawing.points[2]-drawing.points[0])>=1 || Math.abs(drawing.points[3]-drawing.points[1])>=1;if(visible && annotations.size()<MAX_ANNOTATIONS && totalPoints(annotations)+drawing.pointCount()<=MAX_TOTAL_POINTS){validateAnnotation(drawing,plan);annotations.add(drawing);remember(drawingBefore);}else if(visible)NotesNotice.show(getContext(),"Annotation limit reached; save this image before adding more marks.");}else if(drawingBefore!=null)restore(drawingBefore);
  drawing=null;drawingBefore=null;drawingPointer=-1;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);notifyChanged();
 }
 private void finishGesture(boolean accept){if(gesture==null)return;if(accept && !gesture.plan.equals(plan))remember(gesture);else if(!accept)restore(gesture);gesture=null;handle=-1;pointer=-1;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);notifyChanged();}
 @Override public boolean performClick(){super.performClick();return true;}
 @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();navigating=false;if(source!=null){source.recycle();source=null;}}
 private static int totalPoints(List<Annotation> marks){int count=0;for(Annotation mark:marks)count+=mark.pointCount();return count;}
 private static final class Snapshot {final ImageEditPlan plan;final List<Annotation> annotations;Snapshot(ImageEditPlan plan,List<Annotation> annotations){this.plan=plan;this.annotations=java.util.Collections.unmodifiableList(new ArrayList<>(annotations));}}
 private static final class Annotation {
  final Tool tool;final int kind,color,font;final float width;final String text;float[] points;
  Annotation(Tool tool,int color,float width,float[] points){this(tool,color,width,points,"",0);}
  Annotation(Tool tool,int color,float width,float[] points,String text){this(tool,color,width,points,text,0);}
  Annotation(Tool tool,int color,float width,float[] points,String text,int font){this.tool=tool;this.kind=tool.ordinal()-1;this.color=color;this.width=width;this.points=points.clone();this.text=text==null?"":text;this.font=font;}
  int pointCount(){return points.length/2;}
 }
}
