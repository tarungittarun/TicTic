package app.space.notes;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.text.Layout;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import app.space.notes.domain.Note;
import app.space.notes.runtime.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
/** Attached-window reflow/raster exercise during the native soak; commands, not touch/navigation coverage. */
final class NativeEditorExercise implements AutoCloseable {
 private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
 private Activity activity;private NotesController controller;
 private final NotesHost host=new NotesHost(){public void showMessage(String s){}public void shareText(String a,String b){}public void chooseLocalFolder(){}public void openExistingFolder(){}public void exportBackup(){}public void importBackup(){}public String versionLabel(){return "Synthetic editor exercise";}};
 NativeEditorExercise(File root) throws Exception{
  activity=instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),NotesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));File files=new File(root,"editor");assertTrue(files.mkdir());
  Context context=new ContextWrapper(activity){@Override public Context getApplicationContext(){return this;}@Override public File getFilesDir(){return files;}@Override public SharedPreferences getSharedPreferences(String n,int m){return activity.getSharedPreferences(root.getName()+"-editor-"+n,m);}};
  Note seed=Note.create(1).edit("Paragraph stability","01\n02\n03\n04\n05\n06\n07\n08\n09",2);new NotesRepository(context).save(Collections.singletonList(seed));instrumentation.runOnMainSync(()->{controller=new NotesController(context,host,null);activity.setContentView(controller.view());controller.openNote(seed.id);});await(()->body()!=null);
 }
 private TextView find(View view,String name){if(view instanceof TextView){TextView text=(TextView)view;if(name.equals(text.getText().toString()) || name.contentEquals(text.getContentDescription()==null?"":text.getContentDescription()) || name.contentEquals(text.getHint()==null?"":text.getHint()))return text;}if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){TextView found=find(group.getChildAt(i),name);if(found!=null)return found;}}return null;}
 private EditText body(){return (EditText)find(controller.view(),"Write your note…");}
 private void await(java.util.function.BooleanSupplier ready) throws Exception{CountDownLatch done=new CountDownLatch(1);AtomicBoolean ended=new AtomicBoolean();activity.runOnUiThread(new Runnable(){public void run(){if(ended.get())return;if(ready.getAsBoolean())done.countDown();else activity.getWindow().getDecorView().postDelayed(this,25);}});try{assertTrue("Attached editor condition timed out",done.await(12,TimeUnit.SECONDS));}finally{ended.set(true);}}
 void cycle(int iteration) throws Exception{
  instrumentation.runOnMainSync(()->{EditText field=body();field.setTextSize(iteration%2==0?18:36);field.setText("01\n02\n03\n04\n05\n06\n07\n08\n09");field.setSelection(0,2);TextView heading=find(controller.view(),"Heading 1");if(!heading.isShown())find(controller.view(),"Text formatting").performClick();find(controller.view(),"Heading 2").performClick();find(controller.view(),"Text").performClick();find(controller.view(),"Heading 1").performClick();});
  await(()->body().getLayout()!=null && !body().isLayoutRequested());instrumentation.waitForIdleSync();instrumentation.runOnMainSync(()->{EditText field=body();Layout layout=field.getLayout();assertEquals(9,layout.getLineCount());assertEquals(1,TextStyles.read(field.getText()).size());assertEquals(2,TextStyles.read(field.getText()).get(0).end);assertTrue("First-line heading must reserve real space",layout.getLineBottom(0)-layout.getLineTop(0)>layout.getLineBottom(1)-layout.getLineTop(1));for(int line=1;line<layout.getLineCount();line++)assertTrue(layout.getLineBaseline(line)>layout.getLineBaseline(line-1));assertTrue(field.getHeight()>=layout.getHeight()+field.getCompoundPaddingTop()+field.getCompoundPaddingBottom());Bitmap raster=Bitmap.createBitmap(layout.getWidth(),layout.getHeight(),Bitmap.Config.ARGB_8888);layout.draw(new Canvas(raster));raster.recycle();});
 }
 @Override public void close(){if(controller!=null)instrumentation.runOnMainSync(controller::close);if(activity!=null)instrumentation.runOnMainSync(activity::finish);}
}
