package app.space.notes.runtime;
import android.graphics.*;
import android.view.*;
import android.widget.FrameLayout;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;
/** Pixel assertions exercise real Android paths, brush histories and export, not source strings. */
public class DrawingToolsTest {
 private void onMain(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
 private DrawingPad paper(){android.content.Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();DrawingPad pad=new DrawingPad(c);new FrameLayout(c).addView(pad);pad.measure(View.MeasureSpec.makeMeasureSpec(1024,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(768,View.MeasureSpec.EXACTLY));pad.layout(0,0,1024,768);pad.loaded(null);return pad;}
 private void draw(DrawingPad pad,float x,float y,float endX,float endY){long now=android.os.SystemClock.uptimeMillis();for(int i=0;i<3;i++){MotionEvent e=MotionEvent.obtain(now,now+i*20,new int[]{0,2,1}[i],i==0?x:endX,i==0?y:endY,0);assertTrue(pad.onTouchEvent(e));e.recycle();}}
 @Test public void rectangleAndOvalAreOutlinesNotFilledBlocks(){onMain(()->{DrawingPad p=paper();p.tool(5);p.width(8);draw(p,100,100,600,500);Bitmap b=p.snapshot();assertEquals(Color.BLACK,b.getPixel(300,100));assertEquals(Color.WHITE,b.getPixel(300,300));b.recycle();p.undo();p.tool(6);draw(p,100,100,600,500);b=p.snapshot();assertEquals(Color.BLACK,b.getPixel(350,100));assertEquals(Color.WHITE,b.getPixel(350,300));b.recycle();});}
 @Test public void brushWidthAndColourAreCapturedPerStroke(){onMain(()->{DrawingPad p=paper();p.width(16);p.color(Color.RED);draw(p,100,100,700,100);p.color(Color.BLUE);p.width(2);draw(p,100,200,700,200);Bitmap b=p.snapshot();assertEquals(Color.RED,b.getPixel(400,105));assertEquals(Color.BLUE,b.getPixel(400,200));assertEquals(Color.WHITE,b.getPixel(400,205));b.recycle();});}
 @Test public void highlighterAndEraserHaveDifferentPixelSemantics(){onMain(()->{DrawingPad p=paper();p.tool(2);p.color(Color.RED);draw(p,100,100,700,100);Bitmap b=p.snapshot();int ink=b.getPixel(400,100);assertEquals(255,Color.red(ink));assertTrue(Color.green(ink)>0 && Color.green(ink)<255);b.recycle();p.tool(3);draw(p,100,100,700,100);b=p.snapshot();assertEquals(Color.WHITE,b.getPixel(400,100));b.recycle();p.undo();b=p.snapshot();assertEquals(ink,b.getPixel(400,100));b.recycle();});}
 @Test public void clearCanBeUndoneAndRedoDoesNotChangeOtherStrokes(){onMain(()->{DrawingPad p=paper();draw(p,100,100,700,100);p.clear();Bitmap b=p.snapshot();assertEquals(Color.WHITE,b.getPixel(400,100));b.recycle();p.undo();b=p.snapshot();assertEquals(Color.BLACK,b.getPixel(400,100));b.recycle();p.redo();b=p.snapshot();assertEquals(Color.WHITE,b.getPixel(400,100));assertEquals(1024,b.getWidth());assertEquals(768,b.getHeight());b.recycle();});}
 @Test public void mediaStoreImageImportsOwnPixelsAndSurvivesSourceDeletion()throws Exception{
  android.content.Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();OfflineOcr.requireLocal(android.net.Uri.parse("content://com.android.providers.media.documents/document/image%3A1"));
  if(android.os.Build.VERSION.SDK_INT<29)return; // API26 real stream coverage is the SAF fixture; scoped own-media insertion starts at 29.
  android.content.ContentValues values=new android.content.ContentValues();values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"MyNotes-image-test-"+java.util.UUID.randomUUID()+".png");values.put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png");values.put("is_pending",1);android.net.Uri uri=c.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);assertNotNull(uri);
  try{Bitmap pixels=Bitmap.createBitmap(32,24,Bitmap.Config.ARGB_8888);pixels.eraseColor(Color.GREEN);try(java.io.OutputStream out=c.getContentResolver().openOutputStream(uri)){assertTrue(pixels.compress(Bitmap.CompressFormat.PNG,100,out));}pixels.recycle();values.clear();values.put("is_pending",0);c.getContentResolver().update(uri,values,null,null);app.space.notes.domain.NoteImage image=NoteImageFiles.importImage(c,uri);assertEquals(32,image.width);assertEquals(24,image.height);assertEquals(1,c.getContentResolver().delete(uri,null,null));uri=null;Bitmap saved=NoteImageFiles.decode(image);assertNotNull(saved);assertEquals(Color.GREEN,saved.getPixel(10,10));saved.recycle();}finally{if(uri!=null)c.getContentResolver().delete(uri,null,null);}
 }
}
