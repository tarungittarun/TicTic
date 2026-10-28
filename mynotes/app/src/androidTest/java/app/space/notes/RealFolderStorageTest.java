package app.space.notes;
import android.app.Instrumentation;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import app.space.notes.domain.Note;
import app.space.notes.runtime.*;
import static org.junit.Assert.*;
/** Exercises Android's actual local DocumentsProvider/grant, not an in-memory folder substitute. */
@RunWith(AndroidJUnit4.class)
public class RealFolderStorageTest {
 private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();private final Context context=instrumentation.getTargetContext();private Uri tree;private String folder;
 private boolean click(AccessibilityNodeInfo node,String... names){if(node==null)return false;CharSequence text=node.getText();if(node.isEnabled() && text!=null)for(String name:names)if(name.equalsIgnoreCase(text.toString())){AccessibilityNodeInfo target=node;while(target!=null && !target.isClickable())target=target.getParent();if(target!=null)return target.performAction(AccessibilityNodeInfo.ACTION_CLICK);}for(int i=0;i<node.getChildCount();i++)if(click(node.getChild(i),names))return true;return false;}
 @Before public void grant()throws Exception{
  folder="MyNotes-folder-test-"+UUID.randomUUID();try(android.os.ParcelFileDescriptor command=instrumentation.getUiAutomation().executeShellCommand("mkdir -p /sdcard/Documents/"+folder);java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(command)){while(in.read()!=-1){}}
  CountDownLatch received=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>(),error=new AtomicReference<>();BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent data){result.set(data.getStringExtra("uri"));error.set(data.getStringExtra("error"));received.countDown();}};
  IntentFilter filter=new IntentFilter(context.getPackageName()+".TEST_FOLDER_GRANTED");if(android.os.Build.VERSION.SDK_INT>=33)context.registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);else context.registerReceiver(receiver,filter);
  try{Uri initial=DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:Documents/"+folder);Intent intent=new Intent().setClassName(context.getPackageName(),"app.space.notes.FolderGrantTestSupport").putExtra("initial",initial.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(intent);long end=android.os.SystemClock.uptimeMillis()+20000;while(received.getCount()>0 && android.os.SystemClock.uptimeMillis()<end){click(instrumentation.getUiAutomation().getRootInActiveWindow(),"Use this folder","Select","Allow");received.await(150,TimeUnit.MILLISECONDS);}assertEquals("Real SAF grant did not complete: "+error.get(),0,received.getCount());assertNotNull(error.get(),result.get());tree=Uri.parse(result.get());assertEquals("Do not test against any other directory", "primary:Documents/"+folder,DocumentsContract.getTreeDocumentId(tree));}finally{context.unregisterReceiver(receiver);}
 }
 private List<String> children(Uri directory)throws Exception{List<String> names=new ArrayList<>();Uri listing=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(directory));try(Cursor cursor=context.getContentResolver().query(listing,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){assertNotNull(cursor);while(cursor.moveToNext())names.add(cursor.getString(0));}return names;}
 @Test public void realDirectoriesAndVerifiedMovesAreVisibleToDocumentsProvider()throws Exception{
  LocalNotebookFolders files=new LocalNotebookFolders(context.getContentResolver(),tree);files.create("Projects ಕನ್ನಡ");files.create("Archive");assertTrue(files.list().contains("Projects ಕನ್ನಡ"));Note note=Note.create(1).edit("Real folder note","Portable body",2).folder("Projects ಕನ್ನಡ",3);files.reconcile(Arrays.asList(note));Uri source=DocumentsContract.buildDocumentUriUsingTree(tree,"primary:Documents/"+folder+"/Projects ಕನ್ನಡ"),destination=DocumentsContract.buildDocumentUriUsingTree(tree,"primary:Documents/"+folder+"/Archive");assertEquals(1,children(source).size());assertTrue(children(source).get(0).endsWith(".mynote.json"));files.reconcile(Arrays.asList(note.folder("Archive",4)));assertTrue(children(source).isEmpty());assertEquals(1,children(destination).size());files.reconcile(Arrays.asList(note.folder("Archive",4)));assertEquals("Retry must not duplicate the file",1,children(destination).size());
 }
 @After public void cleanup()throws Exception{if(tree!=null && DocumentsContract.getTreeDocumentId(tree).equals("primary:Documents/"+folder)){DocumentsContract.deleteDocument(context.getContentResolver(),DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree)));context.getContentResolver().releasePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}}
}
