package app.space.notes;
import android.app.Instrumentation;
import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
/** Synthetic PNG behind a REAL local SAF grant; never bypass the app's file/cloud URI denial. */
final class LocalImageFixture implements AutoCloseable {
    final Uri uri;private final Context context;private final Uri tree,root;
    private LocalImageFixture(Context context,Uri tree,Uri root,Uri uri){this.context=context;this.tree=tree;this.root=root;this.uri=uri;}
    static LocalImageFixture create(Instrumentation instrumentation,android.graphics.Bitmap bitmap)throws Exception{
        Context context=instrumentation.getTargetContext();String folder="MyNotes-inline-test-"+UUID.randomUUID();
        try(android.os.ParcelFileDescriptor command=instrumentation.getUiAutomation().executeShellCommand("mkdir -p /sdcard/Documents/"+folder);InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(command)){while(in.read()!=-1){}}
        CountDownLatch received=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>(),error=new AtomicReference<>();
        BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent data){result.set(data.getStringExtra("uri"));error.set(data.getStringExtra("error"));received.countDown();}};IntentFilter filter=new IntentFilter(context.getPackageName()+".TEST_FOLDER_GRANTED");if(android.os.Build.VERSION.SDK_INT>=33)context.registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);else context.registerReceiver(receiver,filter);
        Uri tree;
        try{Uri initial=DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:Documents/"+folder);context.startActivity(new Intent().setClassName(context.getPackageName(),"app.space.notes.FolderGrantTestSupport").putExtra("initial",initial.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Set<String> activated=new HashSet<>();long end=android.os.SystemClock.uptimeMillis()+20000;while(received.getCount()>0 && android.os.SystemClock.uptimeMillis()<end){click(instrumentation.getUiAutomation().getRootInActiveWindow(),activated);received.await(150,TimeUnit.MILLISECONDS);}assertEquals("Real local image grant: "+error.get(),0,received.getCount());assertNotNull(error.get(),result.get());tree=Uri.parse(result.get());}finally{context.unregisterReceiver(receiver);}
        String id=DocumentsContract.getTreeDocumentId(tree);assertTrue("Never create/delete outside the new synthetic fixture",id.equals("primary:Documents/"+folder) || id.equals("home:"+folder));Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,id),image=DocumentsContract.createDocument(context.getContentResolver(),root,"image/png","photo.png");assertNotNull(image);try(OutputStream out=context.getContentResolver().openOutputStream(image,"wt")){assertNotNull(out);assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}instrumentation.waitForIdleSync();return new LocalImageFixture(context,tree,root,image);
    }
    private static boolean click(AccessibilityNodeInfo node,Set<String> activated){if(node==null)return false;String text=node.getText()==null?"":node.getText().toString(),upper=text.toUpperCase(Locale.ROOT);if(node.isEnabled() && (text.equalsIgnoreCase("Use this folder") || text.equalsIgnoreCase("Select") || text.equalsIgnoreCase("Allow") || upper.startsWith("ALLOW ACCESS TO"))){String phase=text.equalsIgnoreCase("Allow")?"confirm":"select";AccessibilityNodeInfo target=node;while(target!=null && !target.isClickable())target=target.getParent();if(target!=null && !activated.contains(phase) && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)){activated.add(phase);return true;}}for(int i=0;i<node.getChildCount();i++)if(click(node.getChild(i),activated))return true;return false;}
    public void close()throws Exception{try{assertTrue("Remove only the explicitly verified synthetic folder",DocumentsContract.deleteDocument(context.getContentResolver(),root));}finally{try{context.getContentResolver().releasePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(SecurityException alreadyRevoked){/* Deleting our fixture may revoke its own grant. */}}}
}
