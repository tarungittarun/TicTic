package app.space.notes;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.*;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import app.space.notes.domain.Note;
import app.space.notes.runtime.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/** Real editor/controller journeys, isolated files/prefs. dispatch-first, no shell gestures. */
@RunWith(AndroidJUnit4.class)
public class VoiceRecoveryJourneysTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private Activity activity;private Context context;private File fixture;private NotesController controller;
    private String shared;
    private final NotesHost host=new NotesHost(){
        public void showMessage(String value) { }
        public void chooseLocalFolder() { }
        public void openExistingFolder() { }
        public void exportBackup() { }
        public void importBackup() { }
        public String versionLabel(){return "Core journey";}
        public void shareText(String title,String text){shared=text;}
    };
    @Before public void setup() {
        Intent intent=new Intent(instrumentation.getTargetContext(),NotesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity=instrumentation.startActivitySync(intent);
        fixture=new File(activity.getCacheDir(),"core-"+UUID.randomUUID());assertTrue(fixture.mkdirs());
        context=new ContextWrapper(activity){
            @Override public Context getApplicationContext(){return this;}
            @Override public File getFilesDir(){return fixture;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){return activity.getSharedPreferences(fixture.getName()+"-"+name,mode);}
        };
    }
    private void start(List<Note> seed) throws Exception {
        new NotesRepository(context).save(seed);
        main(()->{controller=new NotesController(context,host,null);activity.setContentView(controller.view());});
        await(()->text("+ Text")!=null);instrumentation.waitForIdleSync();
    }
    private void main(Runnable action){instrumentation.runOnMainSync(action);}
    private void await(BooleanSupplier condition) throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicBoolean ended=new AtomicBoolean();
        activity.runOnUiThread(new Runnable(){public void run(){if(ended.get())return;if(condition.getAsBoolean())done.countDown();else activity.getWindow().getDecorView().postDelayed(this,25);}});
        try{assertTrue("UI condition timed out",done.await(12,TimeUnit.SECONDS));}finally{ended.set(true);}
    }
    private TextView find(View view,String target,boolean hint){
        if(view instanceof TextView){TextView v=(TextView)view;CharSequence value=hint?v.getHint():v.getText();if(value!=null && value.toString().equals(target))return v;}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=g.getChildCount()-1;i>=0;i--){TextView found=find(g.getChildAt(i),target,hint);if(found!=null)return found;}}
        return null;
    }
    private TextView text(String value){return find(controller.view(),value,false);}
    private EditText input(String hint){return (EditText)find(controller.view(),hint,true);}
    private void click(String label){main(()->{TextView v=text(label);assertNotNull(v);v.performClick();});instrumentation.waitForIdleSync();}
    private void saved() throws Exception {await(()->savedIn(controller.view()));}
    private boolean savedIn(View v){if(v instanceof TextView && ((TextView)v).getText().toString().startsWith("Saved ·"))return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(savedIn(g.getChildAt(i)))return true;}return false;}
    private List<EditText> itemInputs(){List<EditText> result=new ArrayList<>();collectItems(controller.view(),result);return result;}
    private void collectItems(View v,List<EditText> result){if(v instanceof EditText && "List item".contentEquals(((EditText)v).getHint()))result.add((EditText)v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collectItems(g.getChildAt(i),result);}}
    private CheckBox firstTick(View v){if(v instanceof CheckBox)return (CheckBox)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){CheckBox c=firstTick(g.getChildAt(i));if(c!=null)return c;}}return null;}
    private TextView card(View view,String title){
        if(view instanceof TextView){String value=((TextView)view).getText().toString().replaceFirst("^[★✓]\\s+","");if(value.equals(title.replaceFirst("^[★✓]\\s+","")))return (TextView)view;}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){TextView found=card(g.getChildAt(i),title);if(found!=null)return found;}}return null;
    }
    private void selectCard(String title,boolean first){main(()->{TextView v=card(controller.view(),title);assertNotNull(v);View card=(View)v.getParent();if(first)card.performLongClick();else card.performClick();});instrumentation.waitForIdleSync();}
    private Note note(String title){return Note.create(1).edit(title,"Body "+title,2);}
    private String voice(String value,boolean corrupt) throws Exception {String id=UUID.randomUUID().toString();VoiceFiles files=new VoiceFiles(context);try(java.io.RandomAccessFile out=new java.io.RandomAccessFile(files.pending(id),"rw")){app.space.notes.domain.PcmWave.header(out,0);out.write(new byte[3200]);}files.complete(id);if(corrupt){try(java.io.FileOutputStream out=new java.io.FileOutputStream(files.transcript(id))){out.write("broken metadata".getBytes(java.nio.charset.StandardCharsets.UTF_8));}}else files.result(id,value,"DONE");return id;}
    private void reopen() throws Exception {main(()->{controller.close();controller=new NotesController(context,host,null);activity.setContentView(controller.view());});await(()->text("+ Text")!=null);}
    @Test public void durableTranscriptRecoveredOnceAcrossControllerRecreation() throws Exception {String id=voice("one zero one",false);start(Collections.emptyList());await(()->newTextVisible("Voice note"));List<Note> first=new NotesRepository(context).load();assertEquals(1,first.size());assertTrue(first.get(0).voiceSources.contains(id));reopen();assertEquals(1,new NotesRepository(context).load().size());}
    @Test public void purgedVoiceNoteIsNotRecreatedFromRetainedAudio() throws Exception {String id=voice("words",false);Note applied=app.space.notes.domain.VoiceTranscripts.apply(app.space.notes.domain.VoiceTranscripts.placeholder(id,1),id,"words",2).state(false,false,3,3);start(Arrays.asList(applied));assertEquals(0,new NotesRepository(context).load().size());reopen();assertEquals(0,new NotesRepository(context).load().size());assertTrue(new VoiceFiles(context).audio(id).isFile());}
    @Test public void corruptTranscriptDoesNotBlockOtherRecoveredNotes() throws Exception {String valid=voice("good transcript",false);String damaged=voice("",true);new VoiceFiles(context).audio(damaged).setLastModified(System.currentTimeMillis()+1000);start(Collections.emptyList());await(()->newTextVisible("Voice note"));List<Note> result=new NotesRepository(context).load();assertEquals(1,result.size());assertTrue(result.get(0).voiceSources.contains(valid));assertTrue(new VoiceFiles(context).audio(damaged).isFile());}
    private boolean newTextVisible(String title){return card(controller.view(),title)!=null;}
    private void remove(File f){File[] files=f.listFiles();if(files!=null)for(File child:files)remove(child);f.delete();}
    @After public void cleanup(){if(controller!=null)main(controller::close);if(activity!=null){for(String name:new String[]{"mynotes-storage-v1","mynotes-ui"})activity.deleteSharedPreferences(fixture.getName()+"-"+name);main(activity::finish);}if(fixture!=null)remove(fixture);}
}
