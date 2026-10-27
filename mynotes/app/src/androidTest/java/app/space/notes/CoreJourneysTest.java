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
public class CoreJourneysTest {
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
    @Test public void richTextLabelsColourAndDuplicatePersist() throws Exception {
        start(Collections.emptyList());click("+ Text");
        main(()->{input("Title").setText("Rich journey");EditText body=input("Write your note…");body.setText("Hello world");body.setSelection(0,5);});
        click("Bold");click("Labels");main(()->input("Work, Personal, Ideas").setText("Work, Ideas"));click("Save labels");
        click("Colour");click("Blue");saved();
        Note n=new NotesRepository(context).load().get(0);assertEquals(6,n.color);assertEquals(1,n.marks.size());assertEquals("bold",n.marks.get(0).type);assertTrue(n.labels.contains("Work"));
        click("Copy");saved();List<Note> notes=new NotesRepository(context).load();assertEquals(2,notes.size());assertEquals(1,notes.get(1).marks.size());
    }
    @Test public void checklistTickIndentMoveHideConvertAndUndoPersist() throws Exception {
        start(Collections.emptyList());click("+ Checklist");main(()->itemInputs().get(0).setText("Milk"));click("+ Item");
        main(()->{List<EditText> inputs=itemInputs();inputs.get(1).setText("Coffee");inputs.get(1).requestFocus();firstTick(controller.view()).performClick();});
        click("Indent");click("↑");click("Hide checked");click("To text");
        main(()->assertTrue(input("Write your note…").getText().toString().contains("Coffee")));
        click("Undo");saved();Note n=new NotesRepository(context).load().get(0);
        assertTrue(n.checklist);assertTrue(n.hideChecked);assertEquals("Coffee",n.items.get(0).text);assertEquals(1,n.items.get(0).depth);assertTrue(n.items.get(1).checked);
    }
    @Test public void bulkPropertiesPinAndArchivePreserveBothNotes() throws Exception {
        start(Arrays.asList(note("One"),note("Two")));await(()->text("One")!=null && text("Two")!=null);
        selectCard("One",true);selectCard("Two",false);click("Colour");click("Teal");await(()->card(controller.view(),"One")!=null);
        click("Labels");main(()->input("Work, Personal, Ideas").setText("Shared"));click("Save labels");await(()->card(controller.view(),"One")!=null);
        click("Pin");await(()->text("★  One")!=null && text("★  Two")!=null);
        selectCard("★  One",true);selectCard("★  Two",false);click("Archive");saved();
        List<Note> result=new NotesRepository(context).load();assertEquals(2,result.size());for(Note n:result){assertTrue(n.pinned);assertTrue(n.archived);assertEquals(5,n.color);assertTrue(n.labels.contains("Shared"));}
    }
    @Test public void shareMergeAndUndoRestoreOriginals() throws Exception {
        start(Arrays.asList(note("One"),note("Two")));await(()->text("One")!=null && text("Two")!=null);
        selectCard("One",true);selectCard("Two",false);click("Share");assertTrue(shared.contains("One"));assertTrue(shared.contains("Two"));
        click("Merge");saved();List<Note> merged=new NotesRepository(context).load();assertEquals(3,merged.size());assertEquals(2,merged.stream().filter(n->n.deletedAt>0).count());
        click("Undo");saved();List<Note> restored=new NotesRepository(context).load();assertEquals(2,restored.size());for(Note n:restored)assertFalse(n.deletedAt>0);
    }
    private void remove(File f){File[] files=f.listFiles();if(files!=null)for(File child:files)remove(child);f.delete();}
    @After public void cleanup(){if(controller!=null)main(controller::close);if(activity!=null){for(String name:new String[]{"mynotes-storage-v1","mynotes-ui"})activity.deleteSharedPreferences(fixture.getName()+"-"+name);main(activity::finish);}if(fixture!=null)remove(fixture);}
}
