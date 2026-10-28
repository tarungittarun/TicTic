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
        TextView candidate=null;
        if(view instanceof TextView){TextView v=(TextView)view;CharSequence value=hint?v.getHint():v.getText();if((value!=null && value.toString().equals(target)) || (!hint && UiActions.matches(v,target))){if(v.isShown())return v;candidate=v;}}
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=g.getChildCount()-1;i>=0;i--){TextView found=find(g.getChildAt(i),target,hint);if(found!=null){if(found.isShown())return found;if(candidate==null)candidate=found;}}}return candidate;
    }
    private TextView text(String value){return find(controller.view(),value,false);}
    private EditText input(String hint){return (EditText)find(controller.view(),hint,true);}
    private void click(String label){main(()->{TextView v=text(label);assertNotNull(v);UiActions.reveal(v);v.performClick();});instrumentation.waitForIdleSync();}
    private void addLabel(String name){main(()->input("Create label").setText(name));click("Create label");}
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
    @Test public void iconDockAndEditorDisclosuresHaveAccessibleNames() throws Exception {
        start(Collections.emptyList());main(()->{for(String name:new String[]{"New text note","+ Checklist","Filters","Library"}){TextView v=text(name);assertNotNull(v);assertTrue(v.isShown());assertEquals(name,v.getContentDescription().toString());assertEquals(name,v.getTooltipText().toString());assertTrue(v.getMinimumHeight()>=48*context.getResources().getDisplayMetrics().density);}});
        click("+ Text");main(()->{assertFalse(text("Scan image").isShown());assertFalse(text("Labels").isShown());assertFalse(text("Bold").isShown());});
        click("Capture tools");main(()->assertTrue(text("Scan image").isShown()));click("Text formatting");main(()->assertTrue(text("Bold").isShown()));click("Back");click("Settings");main(()->assertNotNull(text("Collect diagnostic logs")));
    }
    @Test public void richTextLabelsColourAndDuplicatePersist() throws Exception {
        start(Collections.emptyList());click("+ Text");
        main(()->{input("Title").setText("Rich journey");EditText body=input("Write your note…");body.setText("Hello world");body.setSelection(0,5);});
        click("Bold");click("Labels");addLabel("Work");addLabel("Ideas");click("Save labels");
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
        click("Labels");addLabel("Shared");click("Save labels");await(()->card(controller.view(),"One")!=null);
        click("Pin");await(()->card(controller.view(),"One")!=null && ((View)card(controller.view(),"One").getParent()).getContentDescription().toString().startsWith("Pinned."));
        selectCard("One",true);selectCard("Two",false);click("Archive");saved();
        List<Note> result=new NotesRepository(context).load();assertEquals(2,result.size());for(Note n:result){assertTrue(n.pinned);assertTrue(n.archived);assertEquals(5,n.color);assertTrue(n.labels.contains("Shared"));}
    }
    @Test public void shareMergeAndUndoRestoreOriginals() throws Exception {
        start(Arrays.asList(note("One"),note("Two")));await(()->text("One")!=null && text("Two")!=null);
        selectCard("One",true);selectCard("Two",false);click("Share");assertTrue(shared.contains("One"));assertTrue(shared.contains("Two"));
        click("Merge");saved();List<Note> merged=new NotesRepository(context).load();assertEquals(3,merged.size());assertEquals(2,merged.stream().filter(n->n.deletedAt>0).count());
        click("Undo");saved();List<Note> restored=new NotesRepository(context).load();assertEquals(2,restored.size());for(Note n:restored)assertFalse(n.deletedAt>0);
    }
    /** Unlike the dispatch-first legacy journeys, these taps travel through Android's input dispatcher. */
    private void tap(String name){
        final android.graphics.Rect bounds=new android.graphics.Rect();final int[] location=new int[2];final TextView[] target=new TextView[1];
        main(()->{target[0]=text(name);assertNotNull(name,target[0]);assertTrue(name+" must be discoverable before tapping",target[0].isShown());target[0].requestRectangleOnScreen(new android.graphics.Rect(0,0,target[0].getWidth(),target[0].getHeight()),true);});
                try{instrumentation.getUiAutomation().waitForIdle(200,5000);}catch(java.util.concurrent.TimeoutException e){throw new AssertionError("Native UI did not settle before tap "+name,e);}
        instrumentation.waitForIdleSync();main(()->{assertTrue(name,target[0].getGlobalVisibleRect(bounds));assertEquals(name+" not vertically clipped",target[0].getHeight(),bounds.height());assertEquals(name+" not horizontally clipped",target[0].getWidth(),bounds.width());assertTrue(name+" has a touch target",bounds.height()>=24 && bounds.width()>=24);target[0].getLocationOnScreen(location);});
        long now=android.os.SystemClock.uptimeMillis();android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_DOWN,location[0]+target[0].getWidth()/2f,location[1]+target[0].getHeight()/2f,0),up=android.view.MotionEvent.obtain(now,now+60,android.view.MotionEvent.ACTION_UP,location[0]+target[0].getWidth()/2f,location[1]+target[0].getHeight()/2f,0);instrumentation.sendPointerSync(down);instrumentation.sendPointerSync(up);down.recycle();up.recycle();instrumentation.waitForIdleSync();
    }
    private void closeKeyboard(){
        final boolean[] shown={false};main(()->{View decor=activity.getWindow().getDecorView();if(android.os.Build.VERSION.SDK_INT>=30 && decor.getRootWindowInsets()!=null)shown[0]=decor.getRootWindowInsets().isVisible(android.view.WindowInsets.Type.ime());else{android.graphics.Rect frame=new android.graphics.Rect();decor.getWindowVisibleDisplayFrame(frame);shown[0]=decor.getHeight()-frame.bottom>100*context.getResources().getDisplayMetrics().density;}});
        if(shown[0]){long now=android.os.SystemClock.uptimeMillis();assertTrue(instrumentation.getUiAutomation().injectInputEvent(new android.view.KeyEvent(now,now,android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_BACK,0),true));assertTrue(instrumentation.getUiAutomation().injectInputEvent(new android.view.KeyEvent(now,now+50,android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_BACK,0),true));}instrumentation.waitForIdleSync();
    }
    private String debugMarks(EditText b){StringBuilder out=new StringBuilder();for(Note.Mark m:TextStyles.read(b.getText()))out.append(m.type).append(":").append(m.start).append("-").append(m.end).append("; ");return out.toString();}
    private void laidOut() throws Exception{await(()->input("Write your note…")!=null && input("Write your note…").getLayout()!=null && !input("Write your note…").isLayoutRequested());instrumentation.waitForIdleSync();}
    /** Give every rendered visual line a unique colour, then find the actual solid glyph pixels. */
    private void assertGlyphsStayInsideTheirNativeLineBoxes() throws Exception{
        laidOut();main(()->{EditText editor=input("Write your note…");android.text.Layout layout=editor.getLayout();int count=layout.getLineCount();int[] colours=new int[count];for(int line=0;line<count;line++){colours[line]=android.graphics.Color.rgb(20+(line*23)%180,35+(line*31)%180,45+(line*37)%180);int from=layout.getLineStart(line),to=layout.getLineEnd(line);if(to>from)editor.getText().setSpan(new android.text.style.ForegroundColorSpan(colours[line]),from,to,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}android.graphics.Bitmap pixels=android.graphics.Bitmap.createBitmap(layout.getWidth(),layout.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Canvas canvas=new android.graphics.Canvas(pixels);canvas.drawColor(android.graphics.Color.WHITE);layout.draw(canvas);int previousBottom=-1;for(int line=0;line<count;line++){if(editor.getText().subSequence(layout.getLineStart(line),layout.getLineEnd(line)).toString().trim().isEmpty())continue;int top=Integer.MAX_VALUE,bottom=-1;for(int y=0;y<pixels.getHeight();y++)for(int x=0;x<pixels.getWidth();x++)if(pixels.getPixel(x,y)==colours[line]){top=Math.min(top,y);bottom=Math.max(bottom,y);}assertTrue("Actual glyph pixels exist for line "+line,bottom>=0);assertTrue("Glyph ascent escapes line "+line,top>=layout.getLineTop(line));assertTrue("Glyph descent escapes line "+line,bottom<layout.getLineBottom(line));assertTrue("Adjacent glyphs need breathing room at "+line,top>previousBottom+1);previousBottom=bottom;}assertTrue("Editor grows to fit real document height",editor.getHeight()>=layout.getHeight()+editor.getCompoundPaddingTop()+editor.getCompoundPaddingBottom());pixels.recycle();});
    }
    @Test public void nativeHeadingReflowsBeforeAndAfterParagraphsWithoutGlyphOverlap() throws Exception{
        start(Collections.emptyList());tap("New text note");main(()->{input("Write your note…").setText("Before Hgjpq\nHeading Hgjpq\nAfter Hgjpq");input("Write your note…").setSelection(15,20);});tap("Text formatting");tap("Heading 1");laidOut();main(()->{List<Note.Mark> marks=TextStyles.read(input("Write your note…").getText());assertEquals(1,marks.size());assertEquals(13,marks.get(0).start);assertEquals(26,marks.get(0).end);assertTrue(input("Write your note…").getLayout().getLineBottom(1)-input("Write your note…").getLayout().getLineTop(1)>input("Write your note…").getLayout().getLineBottom(0)-input("Write your note…").getLayout().getLineTop(0));});assertGlyphsStayInsideTheirNativeLineBoxes();
    }
    @Test public void nativeWrappedHeadingAndMixedScriptsKeepSeparateLineBoxes() throws Exception{
        start(Collections.emptyList());tap("New text note");main(()->{EditText body=input("Write your note…");body.setText("Before Hgjpq\nA long heading Hgjpq with enough words to wrap naturally across several visual lines\nAfter Hgjpq ಕನ್ನಡ नमस्ते");body.setSelection(20,30);});tap("Text formatting");tap("Heading 2");assertGlyphsStayInsideTheirNativeLineBoxes();
    }
    @Test public void nativeCaretAndReverseSelectionStyleWholeParagraphOnly() throws Exception{
        start(Collections.emptyList());tap("New text note");main(()->{EditText b=input("Write your note…");b.setText("first\nsecond\nthird");b.setSelection(8);});tap("Text formatting");tap("Heading 1");main(()->{List<Note.Mark> marks=TextStyles.read(input("Write your note…").getText());assertEquals(6,marks.get(0).start);assertEquals(12,marks.get(0).end);EditText b=input("Write your note…");b.setSelection(16,14);});tap("Heading 2");main(()->{EditText b=input("Write your note…");assertEquals(16,b.getSelectionStart());assertEquals(14,b.getSelectionEnd());assertEquals(debugMarks(b),2,TextStyles.read(b.getText()).size());});assertGlyphsStayInsideTheirNativeLineBoxes();
    }
    @Test public void nativeHeadingTypingExtendsAndEnterReturnsToNormalText() throws Exception{
        start(Collections.emptyList());tap("New text note");tap("Text formatting");tap("Heading 1");main(()->{EditText b=input("Write your note…");b.getText().append("Heading");b.setSelection(b.length());b.getText().append(" extended");assertEquals(b.length(),TextStyles.read(b.getText()).get(0).end);b.setSelection(b.length());b.getText().append("\n");b.setSelection(b.length());b.getText().append("Normal paragraph");b.setSelection(b.length());List<Note.Mark> marks=TextStyles.read(b.getText());assertEquals(1,marks.size());assertEquals("Heading extended".length(),marks.get(0).end);});assertGlyphsStayInsideTheirNativeLineBoxes();
    }
    @Test public void nativeHeadingClearUndoAndReopenRetainParagraphMetrics() throws Exception{
        start(Collections.emptyList());tap("New text note");main(()->{EditText b=input("Write your note…");b.setText("Heading Hgjpq\nNormal Hgjpq");b.setSelection(0,13);});tap("Text formatting");tap("Heading 1");main(()->input("Write your note…").setSelection(0,input("Write your note…").length()));tap("Clear");main(()->assertTrue(TextStyles.read(input("Write your note…").getText()).isEmpty()));tap("Undo");saved();assertGlyphsStayInsideTheirNativeLineBoxes();android.os.Bundle state=new android.os.Bundle();main(()->{controller.saveState(state);controller.close();controller=new NotesController(context,host,state);activity.setContentView(controller.view());});await(()->input("Write your note…")!=null);assertGlyphsStayInsideTheirNativeLineBoxes();main(()->assertEquals("h1",TextStyles.read(input("Write your note…").getText()).get(0).type));
    }
    @Test public void nativeSheetDismissalDoesNotLeakTouchesIntoTheDocument() throws Exception{
        start(Collections.emptyList());tap("New text note");main(()->input("Write your note…").setText("Unchanged writing"));tap("Capture tools");assertTrue(text("Scan image").isShown());tap("Close tools");main(()->{assertFalse(text("Scan image").isShown());assertEquals("Unchanged writing",input("Write your note…").getText().toString());assertNotNull(input("Write your note…"));});tap("Back");tap("Library");tap("Settings");main(()->assertNotNull(text("Collect diagnostic logs")));
    }
    @Test public void nativeInputConnectionComposingWordAndEnterKeepParagraphIntent() throws Exception{
        start(Collections.emptyList());tap("New text note");tap("Text formatting");tap("Heading 1");main(()->{EditText field=input("Write your note…");android.view.inputmethod.EditorInfo info=new android.view.inputmethod.EditorInfo();android.view.inputmethod.InputConnection connection=field.onCreateInputConnection(info);assertNotNull(connection);assertTrue((info.imeOptions&android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN)!=0);assertTrue((info.imeOptions&android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI)!=0);connection.beginBatchEdit();connection.setComposingText("H",1);connection.setComposingText("He",1);connection.setComposingText("Heading",1);connection.finishComposingText();connection.endBatchEdit();assertEquals("Heading",field.getText().toString());assertEquals(debugMarks(field),1,TextStyles.read(field.getText()).size());assertEquals(7,TextStyles.read(field.getText()).get(0).end);connection.commitText("\n",1);connection.setComposingText("Normal",1);connection.finishComposingText();assertEquals("Heading\nNormal",field.getText().toString());assertEquals(7,TextStyles.read(field.getText()).get(0).end);assertEquals(field.length(),field.getSelectionEnd());});assertGlyphsStayInsideTheirNativeLineBoxes();
    }
    @Test public void nativeNumericFirstLineMatchesReportedOverlapCase() throws Exception{
        start(Collections.emptyList());tap("New text note");main(()->{EditText field=input("Write your note…");field.setText("01\n02\n03\n04\n05\n06\n07\n08\n09");field.setSelection(0,2);});tap("Text formatting");tap("Heading 1");laidOut();screenshot("numeric-h1");assertGlyphsStayInsideTheirNativeLineBoxes();tap("Heading 2");laidOut();screenshot("numeric-h2");assertGlyphsStayInsideTheirNativeLineBoxes();
    }
    @Test public void nativeBoardAdaptsColumnsToLargeFontsAndTabletWidth() throws Exception{
        start(Arrays.asList(note("Readable card")));main(()->{View board=findBoard(controller.view());assertNotNull(board);int width=context.getResources().getConfiguration().screenWidthDp;float scale=context.getResources().getConfiguration().fontScale;int expected=scale>1.3f?Math.max(1,Math.min(3,width/320)):Math.max(2,Math.min(4,width/240));try{Object manager=board.getClass().getMethod("getLayoutManager").invoke(board);assertEquals(expected,((Integer)manager.getClass().getMethod("getSpanCount").invoke(manager)).intValue());}catch(ReflectiveOperationException e){throw new AssertionError("Read actual RecyclerView span geometry",e);}});
    }
    @Test public void nativeSearchRetainsReadableResultsAboveTheKeyboard() throws Exception{
        start(Arrays.asList(note("Readable result")));await(()->card(controller.view(),"Readable result")!=null);main(()->{EditText field=input("Search notes and labels");field.requestFocus();((android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(field,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});screenshot("search-keyboard",false);main(()->{TextView result=card(controller.view(),"Readable result");android.graphics.Rect visible=new android.graphics.Rect();assertNotNull(result);assertTrue("A result must remain visible while searching",result.getGlobalVisibleRect(visible));assertTrue("The result title must fit above the keyboard",visible.height()>=result.getLineHeight());});
    }
    private View findBoard(View view){if(view.getClass().getName().equals("app.space.notes.runtime.NotesBoard"))return view;if(view instanceof ViewGroup){ViewGroup parent=(ViewGroup)view;for(int i=0;i<parent.getChildCount();i++){View result=findBoard(parent.getChildAt(i));if(result!=null)return result;}}return null;}
    @Test public void nativeCaretSurvivesColourPageAndControllerRecreation() throws Exception{
        start(Collections.emptyList());tap("New text note");main(()->{EditText field=input("Write your note…");field.setText("first\nsecond\nthird");field.setSelection(11,7);});tap("Organise tools");tap("Colour");tap("Blue");main(()->{assertEquals(11,input("Write your note…").getSelectionStart());assertEquals(7,input("Write your note…").getSelectionEnd());});android.os.Bundle state=new android.os.Bundle();main(()->{controller.saveState(state);controller.close();controller=new NotesController(context,host,state);activity.setContentView(controller.view());});await(()->input("Write your note…")!=null);main(()->{assertEquals(11,input("Write your note…").getSelectionStart());assertEquals(7,input("Write your note…").getSelectionEnd());});
    }
    private void screenshot(String name) throws Exception{screenshot(name,true);}
    private void screenshot(String name,boolean hideKeyboard) throws Exception{
        if(hideKeyboard)closeKeyboard();try{instrumentation.getUiAutomation().waitForIdle(300,5000);}catch(java.util.concurrent.TimeoutException e){throw new AssertionError("UI did not settle for screenshot "+name,e);}instrumentation.waitForIdleSync();CountDownLatch frames=new CountDownLatch(1);main(()->controller.view().postOnAnimation(()->controller.view().postOnAnimation(frames::countDown)));assertTrue(frames.await(5,TimeUnit.SECONDS));android.graphics.Bitmap full=instrumentation.getUiAutomation().takeScreenshot();assertNotNull("A real rendered screenshot is required",full);int width=Math.min(context.getResources().getConfiguration().smallestScreenWidthDp>=600?1080:540,full.getWidth());android.graphics.Bitmap image=android.graphics.Bitmap.createScaledBitmap(full,width,Math.max(1,full.getHeight()*width/full.getWidth()),true);File dir=new File(activity.getApplicationContext().getFilesDir(),"ux-review");assertTrue(dir.isDirectory() || dir.mkdirs());try(java.io.FileOutputStream out=new java.io.FileOutputStream(new File(dir,name+".png"))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}if(image!=full)image.recycle();full.recycle();
    }
    @Test public void renderedSyntheticReviewCapturesMainSurfacesInTheRealDeviceTheme() throws Exception{
        Note a=Note.create(1).edit("Small ideas, kept close","A quiet place for thoughts.\nRead a little. Make something.\nLeave room for tomorrow.",2).properties(2,Arrays.asList("Personal"),3);
        Note pinned=Note.create(4).edit("Weekend list","Fresh coffee\nA walk without a phone\nCall home",5).state(true,false,0,6);Note c=Note.create(7).edit("Paper & Ink","Clear words. Warm details.\nOne calm writing surface.",8);start(Arrays.asList(a,pinned,c));await(()->card(controller.view(),"Weekend list")!=null);screenshot("home");
        main(()->controller.openNote(c.id));laidOut();screenshot("editor");main(()->{EditText b=input("Write your note…");b.requestFocus();((android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(b,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});screenshot("editor-keyboard",false);main(()->{EditText field=input("Write your note…");android.graphics.Rect visible=new android.graphics.Rect();assertTrue("A real body viewport must remain above the keyboard",field.getGlobalVisibleRect(visible));assertTrue("At least a native line must be visible, not only editor toolbars: visible="+visible+" line_px="+field.getLineHeight()+" focused="+field.hasFocus()+" root_px="+controller.view().getHeight()+" density="+context.getResources().getDisplayMetrics().density,visible.height()>=field.getLineHeight());});main(()->input("Write your note…").setSelection(0,11));tap("Text formatting");tap("Heading 1");screenshot("format-keyboard",false);screenshot("format");tap("Close formatting");tap("Capture tools");screenshot("capture");tap("Close tools");tap("Organise tools");tap("Colour");screenshot("palette");tap("Cancel");tap("Organise tools");tap("Labels");screenshot("labels");tap("Cancel");tap("Capture tools");tap("Diagram");tap("Add shape");tap("Process");screenshot("diagram");tap("Back to note");tap("Back");tap("+ Checklist");main(()->{input("Title").setText("A little everyday");itemInputs().get(0).setText("Read a chapter");});tap("Add checklist item");main(()->itemInputs().get(1).setText("Make time outside"));screenshot("checklist");tap("Back");tap("Library");tap("Settings");screenshot("settings");tap("Storage & recovery");screenshot("storage");
    }
    private void remove(File f){File[] files=f.listFiles();if(files!=null)for(File child:files)remove(child);f.delete();}
    @After public void cleanup(){if(controller!=null)main(controller::close);if(activity!=null){for(String name:new String[]{"mynotes-storage-v1","mynotes-ui"})activity.deleteSharedPreferences(fixture.getName()+"-"+name);main(activity::finish);}if(fixture!=null)remove(fixture);}
}
