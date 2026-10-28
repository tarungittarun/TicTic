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
 private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();private final Context context=instrumentation.getTargetContext();private Uri tree;private String folder,rootId;
 private boolean click(AccessibilityNodeInfo node,String... names){if(node==null)return false;CharSequence text=node.getText();if(node.isEnabled() && text!=null)for(String name:names)if(name.equalsIgnoreCase(text.toString())){AccessibilityNodeInfo target=node;while(target!=null && !target.isClickable())target=target.getParent();if(target!=null)return target.performAction(AccessibilityNodeInfo.ACTION_CLICK);}for(int i=0;i<node.getChildCount();i++)if(click(node.getChild(i),names))return true;return false;}
 @Before public void grant()throws Exception{
  folder="MyNotes-folder-test-"+UUID.randomUUID();try(android.os.ParcelFileDescriptor command=instrumentation.getUiAutomation().executeShellCommand("mkdir -p /sdcard/Documents/"+folder);java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(command)){while(in.read()!=-1){}}
  CountDownLatch received=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>(),error=new AtomicReference<>();BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent data){result.set(data.getStringExtra("uri"));error.set(data.getStringExtra("error"));received.countDown();}};
  IntentFilter filter=new IntentFilter(context.getPackageName()+".TEST_FOLDER_GRANTED");if(android.os.Build.VERSION.SDK_INT>=33)context.registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);else context.registerReceiver(receiver,filter);
  try{Uri initial=DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:Documents/"+folder);Intent intent=new Intent().setClassName(context.getPackageName(),"app.space.notes.FolderGrantTestSupport").putExtra("initial",initial.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(intent);long end=android.os.SystemClock.uptimeMillis()+20000;while(received.getCount()>0 && android.os.SystemClock.uptimeMillis()<end){click(instrumentation.getUiAutomation().getRootInActiveWindow(),"Use this folder","Select","Allow");received.await(150,TimeUnit.MILLISECONDS);}assertEquals("Real SAF grant did not complete: "+error.get(),0,received.getCount());assertNotNull(error.get(),result.get());tree=Uri.parse(result.get());rootId=DocumentsContract.getTreeDocumentId(tree);assertTrue("Do not test against any other directory: "+rootId,rootId.equals("primary:Documents/"+folder) || rootId.equals("home:"+folder));}finally{context.unregisterReceiver(receiver);}
 }
 private List<String> children(Uri directory)throws Exception{List<String> names=new ArrayList<>();Uri listing=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(directory));try(Cursor cursor=context.getContentResolver().query(listing,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){assertNotNull(cursor);while(cursor.moveToNext())names.add(cursor.getString(0));}return names;}
 @Test public void realDirectoriesAndVerifiedMovesAreVisibleToDocumentsProvider()throws Exception{
  LocalNotebookFolders files=new LocalNotebookFolders(context.getContentResolver(),tree);files.create("Projects ಕನ್ನಡ");files.create("Archive");assertTrue(files.list().contains("Projects ಕನ್ನಡ"));Note note=Note.create(1).edit("Real folder note","Portable body",2).folder("Projects ಕನ್ನಡ",3);files.reconcile(Arrays.asList(note));Uri source=DocumentsContract.buildDocumentUriUsingTree(tree,rootId+"/Projects ಕನ್ನಡ"),destination=DocumentsContract.buildDocumentUriUsingTree(tree,rootId+"/Archive");assertEquals(1,children(source).size());assertTrue(children(source).get(0).endsWith(".mynote.json"));files.reconcile(Arrays.asList(note.folder("Archive",4)));assertTrue(children(source).isEmpty());assertEquals(1,children(destination).size());files.reconcile(Arrays.asList(note.folder("Archive",4)));assertEquals("Retry must not duplicate the file",1,children(destination).size());Uri stored=DocumentsContract.buildDocumentUriUsingTree(tree,rootId+"/Archive/"+children(destination).get(0));try(java.io.InputStream in=context.getContentResolver().openInputStream(stored)){assertNotNull(in);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1)bytes.write(buffer,0,count);Note decoded=DocumentCodec.decode(bytes.toByteArray()).get(0);assertEquals(note.id,decoded.id);assertEquals("Portable body",decoded.body);assertEquals("Archive",decoded.folder);}
 }
 @Test public void imageAutosavesUseOneVerifiedRealMediaFile()throws Exception{
  android.graphics.Bitmap pixels=android.graphics.Bitmap.createBitmap(20,20,android.graphics.Bitmap.Config.ARGB_8888);pixels.eraseColor(android.graphics.Color.RED);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();assertTrue(pixels.compress(android.graphics.Bitmap.CompressFormat.PNG,100,bytes));pixels.recycle();app.space.notes.domain.NoteImage image=new app.space.notes.domain.NoteImage(UUID.randomUUID().toString(),"Image","image/png",20,20,Base64.getEncoder().encodeToString(bytes.toByteArray()));Note note=Note.create(1).images(Arrays.asList(image),2);GenerationProtocol store=new GenerationProtocol(new LocalTreeBackend(context.getContentResolver(),tree));GenerationProtocol.Snapshot first=store.commit(DocumentCodec.encode(Arrays.asList(note)),null);GenerationProtocol.Snapshot second=store.commit(DocumentCodec.encode(Arrays.asList(note.edit("Edited","Body",3))),first.head);Note read=DocumentCodec.decode(store.read().payload).get(0);assertEquals(image.data,read.images.get(0).data);Uri media=DocumentsContract.buildDocumentUriUsingTree(tree,rootId+"/.mynotes-media-v1");assertEquals(1,children(media).size());assertTrue(children(media).get(0).endsWith(".bin"));assertTrue(!new LocalNotebookFolders(context.getContentResolver(),tree).list().contains(".mynotes-media-v1"));assertEquals(second.head,store.read().head);
 }
 @After public void cleanup()throws Exception{if(tree!=null && (DocumentsContract.getTreeDocumentId(tree).equals("primary:Documents/"+folder) || DocumentsContract.getTreeDocumentId(tree).equals("home:"+folder))){DocumentsContract.deleteDocument(context.getContentResolver(),DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree)));context.getContentResolver().releasePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}}
}
