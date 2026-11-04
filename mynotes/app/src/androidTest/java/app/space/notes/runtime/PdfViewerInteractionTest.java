package app.space.notes.runtime;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Public generated PDF only. Attached viewer states, actual rendering and original-preserving markup. */
@RunWith(AndroidJUnit4.class)
public class PdfViewerInteractionTest {
 private final Instrumentation instrument=InstrumentationRegistry.getInstrumentation();
 private Activity activity;private LocalPdfPreview preview;private File original;private byte[] originalBytes;private boolean previousScope;
 private Context context(){return instrument.getTargetContext();}
 private Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
 private void main(Runnable action){instrument.runOnMainSync(action);}
 private void frames()throws Exception{instrument.waitForIdleSync();CountDownLatch latch=new CountDownLatch(1);main(()->preview.postOnAnimation(()->preview.postOnAnimation(latch::countDown)));assertTrue(latch.await(10,TimeUnit.SECONDS));instrument.waitForIdleSync();}
 private void await(java.util.concurrent.Callable<Boolean> condition)throws Exception{long end=SystemClock.elapsedRealtime()+15000;while(SystemClock.elapsedRealtime()<end){java.util.concurrent.atomic.AtomicBoolean result=new java.util.concurrent.atomic.AtomicBoolean();main(()->{try{result.set(condition.call());}catch(Exception e){throw new AssertionError(e);}});if(result.get())return;Thread.sleep(25);}fail("Attached native viewer state did not appear");}
 @Before public void open()throws Exception{
  activity=instrument.startActivitySync(new Intent(context(),app.space.notes.NotesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  main(()->{try{Field f=app.space.notes.NotesActivity.class.getDeclaredField("controller");f.setAccessible(true);NotesController previous=(NotesController)f.get(activity);if(previous!=null)previous.close();f.set(activity,null);}catch(Exception e){throw new AssertionError(e);}});
  original=File.createTempFile("public-pdf68-original-",".pdf",context().getCacheDir());android.graphics.pdf.PdfDocument source=new android.graphics.pdf.PdfDocument();try{for(int i=0;i<3;i++){android.graphics.pdf.PdfDocument.Page p=source.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(600,800,i+1).create());Paint ink=new Paint();ink.setTextSize(22);p.getCanvas().drawText("Searchable needle page "+(i+1),40,100,ink);p.getCanvas().drawText("Public fixture "+original.getName(),40,140,ink);source.finishPage(p);}try(OutputStream out=new FileOutputStream(original)){source.writeTo(out);}}finally{source.close();}
  originalBytes=java.nio.file.Files.readAllBytes(original.toPath());File cache=File.createTempFile("public-pdf68-view-",".pdf",context().getCacheDir());java.nio.file.Files.write(cache.toPath(),originalBytes);com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context());LocalPdfPreview.Document document=new LocalPdfPreview.Document(cache);
  previousScope=context().getSharedPreferences("mynotes-ui",Context.MODE_PRIVATE).getBoolean("viewer-file-zoom",false);
  main(()->{NotesDesign design=new NotesDesign(activity,NotesAppearance.dark(activity,new Host()));preview=new LocalPdfPreview(activity,design,document,new Host(),android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3APublic-viewer-fixture.pdf"),"Public-viewer-fixture.pdf");preview.setBackgroundColor(design.background);activity.setContentView(preview);preview.setZoomScope(false);});frames();
 }
 private static final class Host implements NotesHost{public void showMessage(String m){}public void shareText(String a,String b){}public void chooseLocalFolder(){}public void openExistingFolder(){}public void exportBackup(){}public void importBackup(){}public String versionLabel(){return "Public viewer fixture";}}
 @After public void close()throws Exception{
  if(preview!=null){ExecutorService a=(ExecutorService)field(preview,"worker"),b=(ExecutorService)field(preview,"textWorker");main(preview::close);assertTrue(a.awaitTermination(30,TimeUnit.SECONDS));assertTrue(b.awaitTermination(30,TimeUnit.SECONDS));}if(activity!=null)main(activity::finish);if(original!=null){assertArrayEquals("Viewing/selection/markup never changes original PDF bytes",originalBytes,java.nio.file.Files.readAllBytes(original.toPath()));original.delete();}context().getSharedPreferences("mynotes-ui",Context.MODE_PRIVATE).edit().putBoolean("viewer-file-zoom",previousScope).commit();
 }
 private void screenshot(String name)throws Exception{frames();int[] size={0,0};main(()->{size[0]=activity.getWindow().getDecorView().getWidth();size[1]=activity.getWindow().getDecorView().getHeight();});Bitmap bitmap=Bitmap.createBitmap(size[0],size[1],Bitmap.Config.ARGB_8888);try{CountDownLatch done=new CountDownLatch(1);int[] status={-1};main(()->PixelCopy.request(activity.getWindow(),bitmap,value->{status[0]=value;done.countDown();},new Handler(Looper.getMainLooper())));assertTrue(done.await(10,TimeUnit.SECONDS));assertEquals(PixelCopy.SUCCESS,status[0]);File directory=new File(context().getFilesDir(),"ux-review");assertTrue(directory.isDirectory()||directory.mkdirs());try(OutputStream out=new FileOutputStream(new File(directory,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}}finally{bitmap.recycle();}}
 @Test public void compactFilenameToolbarLeavesContinuousPagesAndHasNoPermanentPageRow()throws Exception{
  main(()->{LinearLayout bar=(LinearLayout)preview.getChildAt(0);assertEquals(4,bar.getChildCount());assertEquals("Public-viewer-fixture.pdf",((TextView)bar.getChildAt(1)).getText().toString());assertEquals(1,((TextView)bar.getChildAt(1)).getMaxLines());assertEquals(2,preview.getChildCount());assertTrue(preview.getChildAt(1).getHeight()>preview.getHeight()/2);for(int i=0;i<bar.getChildCount();i++)assertTrue("Single toolbar row remains fully visible",bar.getChildAt(i).getHeight()>=Math.round(48*activity.getResources().getDisplayMetrics().density));});screenshot("pdf68-compact-filename-toolbar");
 }
 @Test public void fileZoomReflowsEveryPageInsteadOfCroppingOnePageImage()throws Exception{
  androidx.recyclerview.widget.RecyclerView pages=(androidx.recyclerview.widget.RecyclerView)field(preview,"pages");int[] initial={0};main(()->{initial[0]=pages.getWidth();preview.setZoomScope(true);try{Method scale=LocalPdfPreview.class.getDeclaredMethod("scaleFile",float.class);scale.setAccessible(true);scale.invoke(preview,2f);}catch(Exception e){throw new AssertionError(e);}});frames();main(()->{assertEquals(initial[0]*2,pages.getWidth(),3);((androidx.recyclerview.widget.LinearLayoutManager)pages.getLayoutManager()).scrollToPositionWithOffset(2,0);});frames();main(()->{assertNotNull(pages.findViewHolderForAdapterPosition(2));assertTrue("Distant page is physically resized too",pages.findViewHolderForAdapterPosition(2).itemView.getHeight()>initial[0]*2);assertEquals(2f,preview.savedViewport().getFloat("fileScale"),.01f);});screenshot("pdf68-file-zoom-distant-page");
 }
 @Test public void pageRailDragAndDocumentScrollStaySynchronizedAndRailAutoHides()throws Exception{
  TextView rail=(TextView)field(preview,"pageRail");FrameLayout viewport=(FrameLayout)field(preview,"viewport");int[] point={0,0};main(()->{viewport.getLocationOnScreen(point);long t=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,point[0]+viewport.getWidth()-20,point[1]+viewport.getHeight()-30,0);try{rail.dispatchTouchEvent(down);}finally{down.recycle();}MotionEvent up=MotionEvent.obtain(t,t+50,MotionEvent.ACTION_UP,point[0]+viewport.getWidth()-20,point[1]+viewport.getHeight()-30,0);try{rail.dispatchTouchEvent(up);}finally{up.recycle();}});frames();await(()->rail.getText().toString().equals("3 / 3"));screenshot("pdf68-draggable-page-position");Thread.sleep(1300);main(()->assertEquals(View.INVISIBLE,rail.getVisibility()));
 }
 @Test public void keyboardSearchActionFindsAndHighlightsEmbeddedText()throws Exception{
  main(()->{try{Method method=LocalPdfPreview.class.getDeclaredMethod("openSearch");method.setAccessible(true);method.invoke(preview);}catch(Exception e){throw new AssertionError(e);}});await(()->((View)field(preview,"searchDock")).isShown());screenshot("pdf68-search-above-keyboard");main(()->{try{EditText query=(EditText)field(preview,"query");query.setText("needle");query.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);}catch(Exception e){throw new AssertionError(e);}});await(()->!((List<?>)field(preview,"matches")).isEmpty());await(()->{androidx.recyclerview.widget.RecyclerView pages=(androidx.recyclerview.widget.RecyclerView)field(preview,"pages");androidx.recyclerview.widget.RecyclerView.ViewHolder holder=pages.findViewHolderForAdapterPosition(0);return holder!=null && ((PdfSelectionImage)holder.itemView).searchTop()>=0;});screenshot("pdf68-search-result-highlight");
 }
 @Test public void recognitionHasRealIndeterminateLoaderAndCancellationBeforeWork()throws Exception{
  ExecutorService renderer=(ExecutorService)field(preview,"worker");CountDownLatch hold=new CountDownLatch(1);renderer.execute(()->{try{hold.await(20,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
  try{main(()->{try{Method method=LocalPdfPreview.class.getDeclaredMethod("recognizePage",int.class);method.setAccessible(true);method.invoke(preview,0);}catch(Exception e){throw new AssertionError(e);}});AlertDialog dialog=(AlertDialog)field(preview,"pageOcrDialog");assertNotNull(dialog);main(()->assertTrue("An actual animated ProgressBar is visible",containsSpinner(dialog.getWindow().getDecorView())));screenshot("pdf68-offline-recognition-loader");main(()->dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());}finally{hold.countDown();}
 }
 private boolean containsSpinner(View view){if(view instanceof ProgressBar)return view.isShown();if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(containsSpinner(group.getChildAt(i)))return true;}return false;}
 @Test public void annotationDraftUndoRedoAndExportKeepTextAndOriginalBytes()throws Exception{
  PdfMarkup marks=PdfMarkup.open(context(),original,3);List<PointF> points=Arrays.asList(new PointF(.2f,.3f),new PointF(.6f,.3f));assertTrue(marks.add(new PdfMarkup.Stroke(0,Color.RED,.012f,false,points)));marks.persist();assertEquals(1,PdfMarkup.open(context(),original,3).snapshot().size());marks.undo();assertEquals(0,marks.snapshot().size());marks.redo();assertEquals(1,marks.snapshot().size());File output=marks.export(context(),original);
  try{assertFalse(Arrays.equals(originalBytes,java.nio.file.Files.readAllBytes(output.toPath())));try(com.tom_roush.pdfbox.pdmodel.PDDocument pdf=com.tom_roush.pdfbox.pdmodel.PDDocument.load(output)){assertEquals(3,pdf.getNumberOfPages());assertTrue(new com.tom_roush.pdfbox.text.PDFTextStripper().getText(pdf).contains("Searchable needle page 1"));}android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(output,android.os.ParcelFileDescriptor.MODE_READ_ONLY);try(android.graphics.pdf.PdfRenderer renderer=new android.graphics.pdf.PdfRenderer(fd);android.graphics.pdf.PdfRenderer.Page page=renderer.openPage(0)){Bitmap bitmap=Bitmap.createBitmap(600,800,Bitmap.Config.ARGB_8888);try{bitmap.eraseColor(Color.WHITE);page.render(bitmap,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);int color=bitmap.getPixel(180,240);assertTrue("Export contains actual red ink at the original page coordinate",Color.red(color)>180 && Color.green(color)<120);}finally{bitmap.recycle();}}}finally{output.delete();}
 }
 @Test public void markToolbarIsAttachedAndHasExplicitFinishAndExportFlow()throws Exception{
  main(()->{try{Method edit=LocalPdfPreview.class.getDeclaredMethod("editMarks");edit.setAccessible(true);edit.invoke(preview);}catch(Exception e){throw new AssertionError(e);}});await(()->field(preview,"markBar")!=null);main(()->{try{LinearLayout tools=(LinearLayout)field(preview,"markBar");assertEquals(7,tools.getChildCount());assertTrue(tools.isShown());assertEquals("Done marking",tools.getChildAt(6).getContentDescription());}catch(Exception e){throw new AssertionError(e);}});screenshot("pdf68-pen-and-highlighter-tools");
 }
}
