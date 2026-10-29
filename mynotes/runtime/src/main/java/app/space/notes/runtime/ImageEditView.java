package app.space.notes.runtime;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import app.space.notes.domain.ImageEditPlan;
import java.util.ArrayDeque;

/** Local preview owns one decoded bitmap. Geometry history never duplicates image pixels. */
final class ImageEditView extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final ArrayDeque<ImageEditPlan> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
 private Bitmap source;
 private ImageEditPlan plan,gesture;
 private Runnable changed=()->{};
 private boolean cropMode,saving;
 private int handle=-1,pointer=-1;
 private float scale=1,ox,oy;
 ImageEditView(Context c){super(c);setFocusable(true);setContentDescription("Image preview. Use Crop or Crop presets in Image options to change the crop.");}
 void loaded(Bitmap bitmap,ImageEditPlan restored){source=bitmap;plan=restored!=null && restored.sourceWidth==bitmap.getWidth() && restored.sourceHeight==bitmap.getHeight()?restored:new ImageEditPlan(bitmap.getWidth(),bitmap.getHeight());notifyChanged();}
 int[] historyState(boolean forward){ArrayDeque<ImageEditPlan> history=forward?redo:undo;int[] values=new int[history.size()*8];int at=0;for(ImageEditPlan item:history)for(int value:item.state())values[at++]=value;return values;}
 void restoreHistory(int[] previous,int[] next){try{readHistory(undo,previous);readHistory(redo,next);}catch(IllegalArgumentException invalid){undo.clear();redo.clear();}notifyChanged();}
 private void readHistory(ArrayDeque<ImageEditPlan> history,int[] values){history.clear();if(values==null)return;if(values.length>400 || values.length%8!=0)throw new IllegalArgumentException("Invalid preview history");for(int at=0;at<values.length;at+=8){int[] state=new int[8];System.arraycopy(values,at,state,0,8);ImageEditPlan item=ImageEditPlan.restore(state);if(item.sourceWidth!=plan.sourceWidth || item.sourceHeight!=plan.sourceHeight)throw new IllegalArgumentException("History source changed");history.addLast(item);}}
 boolean ready(){return source!=null && !source.isRecycled();}
 boolean saving(){return saving;}
 void saving(boolean value){saving=value;notifyChanged();}
 boolean dirty(){return plan!=null && plan.changed();}
 ImageEditPlan plan(){return plan;}
 void settleGesture(){finishGesture(true);}
 boolean cropMode(){return cropMode;}
 void cropMode(boolean value){if(saving)return;finishGesture(false);cropMode=value;notifyChanged();}
 boolean canUndo(){return !saving && !undo.isEmpty();}
 boolean canRedo(){return !saving && !redo.isEmpty();}
 void onStateChanged(Runnable listener){changed=listener;notifyChanged();}
 void edit(ImageEditPlan next){if(!ready() || saving)return;finishGesture(false);if(plan.equals(next))return;remember(plan);plan=next;notifyChanged();}
 private void remember(ImageEditPlan before){undo.addLast(before);while(undo.size()>50)undo.removeFirst();redo.clear();}
 void undo(){finishGesture(false);if(canUndo()){redo.addLast(plan);plan=undo.removeLast();notifyChanged();}}
 void redo(){finishGesture(false);if(canRedo()){undo.addLast(plan);plan=redo.removeLast();notifyChanged();}}
 private void notifyChanged(){invalidate();if(plan!=null)setContentDescription("Image preview, "+plan.cropWidth()+" by "+plan.cropHeight()+" pixels. "+(cropMode?"Drag a crop corner, or use Crop presets in Image options.":"Crop is off."));changed.run();}
 static Matrix matrix(ImageEditPlan p){float[] m=p.matrix();Matrix value=new Matrix();value.setValues(new float[]{m[0],m[1],m[2],m[3],m[4],m[5],0,0,1});return value;}
 /** Called only on the disk worker, with its own bitmap, never this view's bitmap. */
 static Bitmap render(Bitmap bitmap,ImageEditPlan plan){
  Bitmap out=Bitmap.createBitmap(plan.cropWidth(),plan.cropHeight(),Bitmap.Config.ARGB_8888);
  try{out.setHasAlpha(bitmap.hasAlpha());Canvas canvas=new Canvas(out);canvas.translate(-plan.left,-plan.top);canvas.drawBitmap(bitmap,matrix(plan),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));return out;}catch(RuntimeException | OutOfMemoryError failure){out.recycle();throw failure;}
 }
 private float dp(float n){return n*getResources().getDisplayMetrics().density;}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);canvas.drawColor(0xff242424);if(!ready())return;
  float margin=dp(28);scale=Math.max(.001f,Math.min(Math.max(1,getWidth()-margin*2)/plan.width(),Math.max(1,getHeight()-margin*2)/plan.height()));ox=(getWidth()-plan.width()*scale)/2;oy=(getHeight()-plan.height()*scale)/2;
  canvas.save();canvas.translate(ox,oy);canvas.scale(scale,scale);paint.setColor(Color.WHITE);canvas.drawRect(0,0,plan.width(),plan.height(),paint);canvas.drawBitmap(source,matrix(plan),paint);canvas.restore();
  float l=ox+plan.left*scale,t=oy+plan.top*scale,r=ox+plan.right*scale,b=oy+plan.bottom*scale;
  paint.setColor(0xa0000000);canvas.drawRect(ox,oy,ox+plan.width()*scale,t,paint);canvas.drawRect(ox,b,ox+plan.width()*scale,oy+plan.height()*scale,paint);canvas.drawRect(ox,t,l,b,paint);canvas.drawRect(r,t,ox+plan.width()*scale,b,paint);
  if(cropMode){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setColor(0xffffd477);canvas.drawRect(l,t,r,b,paint);paint.setStyle(Paint.Style.FILL);for(int i=0;i<4;i++){float x=i%2==0?l:r,y=i<2?t:b;paint.setColor(Color.BLACK);canvas.drawCircle(x,y,dp(9),paint);paint.setColor(0xffffd477);canvas.drawCircle(x,y,dp(7),paint);}}
 }
 @Override public boolean onTouchEvent(MotionEvent event){
  if(!ready() || saving || !cropMode)return false;
  int action=event.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){float best=dp(28);handle=-1;for(int i=0;i<4;i++){float x=ox+(i%2==0?plan.left:plan.right)*scale,y=oy+(i<2?plan.top:plan.bottom)*scale;float distance=(float)Math.hypot(event.getX()-x,event.getY()-y);if(distance<=best){best=distance;handle=i;}}if(handle<0)return false;gesture=plan;pointer=event.getPointerId(0);getParent().requestDisallowInterceptTouchEvent(true);return true;}
  if(gesture==null)return false;
  if(action==MotionEvent.ACTION_CANCEL || event.getPointerCount()!=1){finishGesture(false);return true;}
  if(action==MotionEvent.ACTION_MOVE){int index=event.findPointerIndex(pointer);if(index<0){finishGesture(false);return true;}int x=Math.round((event.getX(index)-ox)/scale),y=Math.round((event.getY(index)-oy)/scale);int l=gesture.left,t=gesture.top,r=gesture.right,b=gesture.bottom;int minW=Math.min(16,gesture.cropWidth()),minH=Math.min(16,gesture.cropHeight());if(handle%2==0)l=Math.max(0,Math.min(r-minW,x));else r=Math.min(plan.width(),Math.max(l+minW,x));if(handle<2)t=Math.max(0,Math.min(b-minH,y));else b=Math.min(plan.height(),Math.max(t+minH,y));plan=gesture.crop(l,t,r,b);invalidate();return true;}
  if(action==MotionEvent.ACTION_UP){finishGesture(true);performClick();return true;}return true;
 }
 private void finishGesture(boolean accept){if(gesture==null)return;if(accept && !gesture.equals(plan))remember(gesture);else if(!accept)plan=gesture;gesture=null;handle=-1;pointer=-1;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);notifyChanged();}
 @Override public boolean performClick(){super.performClick();return true;}
 @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();if(source!=null){source.recycle();source=null;}}
}
