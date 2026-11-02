package app.space.notes.runtime;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import app.space.notes.domain.ImageTextSelection;
/** Pixel-only viewport: bounded source bitmap, normalized boxes, no WebView or network. */
final class ImageTextSelectionView extends View {
 final ImageTextSelection selection;private final Bitmap image;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);private final RectF frame=new RectF();private RectF crop;private float zoom=1,panX,panY,downX,downY,startX,startY,lastX,lastY;private boolean gesturing,multi;private final ScaleGestureDetector detector;private Runnable changed=()->{};
 ImageTextSelectionView(Context context,Bitmap image,ImageTextSelection selection){super(context);this.image=image;this.selection=selection;setBackgroundColor(Color.rgb(24,27,31));setContentDescription("Image text selection. Tap words or drag an area. Pinch to zoom. Choose words opens an accessible text list.");setFocusable(true);setClickable(true);
  detector=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){layoutFrame();float x=(d.getFocusX()-frame.left)/frame.width(),y=(d.getFocusY()-frame.top)/frame.height();zoom=Math.max(1,Math.min(8,zoom*d.getScaleFactor()));layoutFrame();panX+=d.getFocusX()-(frame.left+x*frame.width());panY+=d.getFocusY()-(frame.top+y*frame.height());invalidate();return true;}});
 }
 void onChanged(Runnable changed){this.changed=changed;}
 void all(boolean value){selection.all(value);crop=value?new RectF(0,0,1,1):null;changed.run();invalidate();}
 void refresh(){changed.run();invalidate();}
 void resetZoom(){zoom=1;panX=panY=0;invalidate();}
 RectF crop(){return crop==null?null:new RectF(crop);}
 private float unit(float value){return Math.max(0,Math.min(1,value));}
 private void layoutFrame(){if(image.isRecycled())return;float fit=Math.min(getWidth()/(float)image.getWidth(),getHeight()/(float)image.getHeight()),w=image.getWidth()*fit*zoom,h=image.getHeight()*fit*zoom;panX=Math.max(-w/2,Math.min(w/2,panX));panY=Math.max(-h/2,Math.min(h/2,panY));frame.set((getWidth()-w)/2+panX,(getHeight()-h)/2+panY,(getWidth()+w)/2+panX,(getHeight()+h)/2+panY);}
 private RectF screen(float l,float t,float r,float b){return new RectF(frame.left+l*frame.width(),frame.top+t*frame.height(),frame.left+r*frame.width(),frame.top+b*frame.height());}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(image.isRecycled())return;layoutFrame();paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.FILL);canvas.drawRect(frame,paint);canvas.drawBitmap(image,null,frame,paint);for(int i=0;i<selection.words.size();i++){ImageTextSelection.Word w=selection.words.get(i);RectF box=screen(w.left,w.top,w.right,w.bottom);paint.setStyle(Paint.Style.FILL);paint.setColor(selection.selected(i)?0x7754BAFF:0x1954BAFF);canvas.drawRect(box,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(getResources().getDisplayMetrics().density);paint.setColor(0xCC168CE4);canvas.drawRect(box,paint);}if(crop!=null){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*getResources().getDisplayMetrics().density);paint.setColor(Color.YELLOW);canvas.drawRect(screen(crop.left,crop.top,crop.right,crop.bottom),paint);}paint.setStyle(Paint.Style.FILL);}
 @Override public boolean onTouchEvent(MotionEvent e){if(!isEnabled() || image.isRecycled())return false;detector.onTouchEvent(e);layoutFrame();if(frame.isEmpty())return false;int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){if(!frame.contains(e.getX(),e.getY()))return false;getParent().requestDisallowInterceptTouchEvent(true);gesturing=true;multi=false;downX=e.getX();downY=e.getY();startX=unit((downX-frame.left)/frame.width());startY=unit((downY-frame.top)/frame.height());lastX=downX;lastY=downY;return true;}if(!gesturing)return false;
  if(e.getPointerCount()>1){multi=true;float x=(e.getX(0)+e.getX(1))/2,y=(e.getY(0)+e.getY(1))/2;if(action==MotionEvent.ACTION_MOVE && !detector.isInProgress()){panX+=x-lastX;panY+=y-lastY;invalidate();}lastX=x;lastY=y;return true;}
  if(action==MotionEvent.ACTION_MOVE && !multi){float x=unit((e.getX()-frame.left)/frame.width()),y=unit((e.getY()-frame.top)/frame.height());if(Math.hypot(e.getX()-downX,e.getY()-downY)>8*getResources().getDisplayMetrics().density){crop=new RectF(Math.min(x,startX),Math.min(y,startY),Math.max(x,startX),Math.max(y,startY));selection.rectangle(crop.left,crop.top,crop.right,crop.bottom);changed.run();invalidate();}return true;}
  if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL){if(action==MotionEvent.ACTION_UP && !multi && Math.hypot(e.getX()-downX,e.getY()-downY)<=8*getResources().getDisplayMetrics().density){for(int i=0;i<selection.words.size();i++){ImageTextSelection.Word w=selection.words.get(i);if(startX>=w.left && startX<=w.right && startY>=w.top && startY<=w.bottom){selection.toggle(i);crop=new RectF(w.left,w.top,w.right,w.bottom);break;}}changed.run();invalidate();performClick();}gesturing=false;getParent().requestDisallowInterceptTouchEvent(false);return true;}return true;
 }
 @Override public boolean performClick(){super.performClick();return true;}
}
