package app.space.notes.runtime;
import android.content.*;
import android.graphics.*;
import android.view.*;
import java.util.function.BiConsumer;
/** Selection stays on the actual PDF raster; floating native Copy and draggable endpoints. */
final class PdfSelectionImage extends ZoomablePageImage {
 private PdfTextLayer layer;private int anchor=-1,focus=-1,handle=-1;private boolean loading;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private ActionMode mode;
 private final BiConsumer<PdfSelectionImage,float[]> request;
 PdfSelectionImage(android.content.Context context,BiConsumer<PdfSelectionImage,float[]> request){super(context);this.request=request;}
 void clearSelection(){if(mode!=null){ActionMode old=mode;mode=null;old.finish();}anchor=focus=handle=-1;invalidate();}
 void clearLayer(){clearSelection();layer=null;loading=false;}
 private float[] normalized(float x,float y){if(getDrawable()==null)return null;Matrix inverse=new Matrix();if(!imageToView().invert(inverse))return null;float[] point={x,y};inverse.mapPoints(point);point[0]/=Math.max(1,getDrawable().getIntrinsicWidth());point[1]/=Math.max(1,getDrawable().getIntrinsicHeight());return point;}
 @Override protected boolean onPageLongPress(float x,float y){float[] point=normalized(x,y);if(point==null)return false;if(layer==null){if(!loading){loading=true;request.accept(this,point);}return true;}select(point);return true;}
 void selectionReady(PdfTextLayer value,float[] point){loading=false;layer=value;select(point);}
 void selectionFailed(){loading=false;}
 private void select(float[] point){if(layer==null || layer.glyphs.isEmpty())return;int nearest=layer.nearest(point[0],point[1]);if(nearest<0)return;RectF box=layer.glyphs.get(nearest).box;float dx=Math.max(box.left-point[0],Math.max(0,point[0]-box.right)),dy=Math.max(box.top-point[1],Math.max(0,point[1]-box.bottom));if(Math.hypot(dx*getWidth(),dy*getHeight())>32*getResources().getDisplayMetrics().density)return;anchor=focus=nearest;getParent().requestDisallowInterceptTouchEvent(true);showActions();invalidate();}
 private RectF rectangle(int i){RectF rect=new RectF(layer.glyphs.get(i).box);rect.left*=getDrawable().getIntrinsicWidth();rect.right*=getDrawable().getIntrinsicWidth();rect.top*=getDrawable().getIntrinsicHeight();rect.bottom*=getDrawable().getIntrinsicHeight();imageToView().mapRect(rect);return rect;}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(layer==null || anchor<0 || getDrawable()==null)return;paint.setColor(0x665DA9FF);for(int i=Math.min(anchor,focus);i<=Math.max(anchor,focus);i++)canvas.drawRect(rectangle(i),paint);paint.setColor(0xff1265ab);RectF a=rectangle(anchor),b=rectangle(focus);float radius=7*getResources().getDisplayMetrics().density;canvas.drawCircle(a.left,a.bottom,radius,paint);canvas.drawCircle(b.right,b.bottom,radius,paint);}
 @Override public boolean onTouchEvent(MotionEvent event){
  if(layer!=null && anchor>=0 && getDrawable()!=null && event.getPointerCount()==1){int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN){RectF a=rectangle(anchor),b=rectangle(focus);double da=Math.hypot(event.getX()-a.left,event.getY()-a.bottom),db=Math.hypot(event.getX()-b.right,event.getY()-b.bottom);if(Math.min(da,db)<32*getResources().getDisplayMetrics().density)handle=da<db?0:1;else{clearSelection();return super.onTouchEvent(event);}}if(action==MotionEvent.ACTION_MOVE){float[] p=normalized(event.getX(),event.getY());if(p!=null){int i=layer.nearest(p[0],p[1]);if(i>=0){if(handle==0)anchor=i;else focus=i;invalidate();}}}if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL){handle=-1;if(action==MotionEvent.ACTION_UP)showActions();if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}else if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return true;}
  return super.onTouchEvent(event);
 }
 private void showActions(){if(mode!=null || anchor<0)return;mode=startActionMode(new ActionMode.Callback(){public boolean onCreateActionMode(ActionMode m,Menu menu){menu.add(0,1,0,"Copy");menu.add(0,2,1,"Select all");menu.add(0,3,2,"Clear selection");return true;}public boolean onPrepareActionMode(ActionMode m,Menu menu){return false;}public boolean onActionItemClicked(ActionMode m,MenuItem item){if(item.getItemId()==1){try{((ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("PDF selection",layer.selected(anchor,focus)));}catch(RuntimeException unavailable){android.widget.Toast.makeText(getContext(),"Clipboard unavailable; original unchanged",0).show();}m.finish();}else if(item.getItemId()==2){anchor=0;focus=layer.glyphs.size()-1;invalidate();}else m.finish();return true;}public void onDestroyActionMode(ActionMode m){mode=null;anchor=focus=handle=-1;invalidate();}},ActionMode.TYPE_FLOATING);}
 @Override protected void onDetachedFromWindow(){clearSelection();super.onDetachedFromWindow();}
}
