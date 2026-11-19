package app.space.notes;
import android.app.Instrumentation;
import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
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
        Uri tree;SafTestPicker.Driver picker=new SafTestPicker.Driver(instrumentation);
        try{Uri initial=DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:Documents/"+folder);context.startActivity(new Intent().setClassName(context.getPackageName(),"app.space.notes.FolderGrantTestSupport").putExtra("initial",initial.toString()).putExtra("expected","primary:Documents/"+folder).putExtra("expectedAlias","home:"+folder).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));long end=android.os.SystemClock.uptimeMillis()+20000;while(received.getCount()>0 && android.os.SystemClock.uptimeMillis()<end){picker.click(instrumentation.getUiAutomation().getRootInActiveWindow());received.await(150,TimeUnit.MILLISECONDS);}assertEquals("Real local image grant: "+error.get()+" picker="+picker+" UI="+SafTestPicker.describe(instrumentation.getUiAutomation().getRootInActiveWindow()),0,received.getCount());assertNotNull(error.get(),result.get());tree=Uri.parse(result.get());}finally{context.unregisterReceiver(receiver);}
        String id=DocumentsContract.getTreeDocumentId(tree);assertTrue("Never create/delete outside the new synthetic fixture",id.equals("primary:Documents/"+folder) || id.equals("home:"+folder));Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,id),image=DocumentsContract.createDocument(context.getContentResolver(),root,"image/png","photo.png");assertNotNull(image);try(OutputStream out=context.getContentResolver().openOutputStream(image,"wt")){assertNotNull(out);assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}instrumentation.waitForIdleSync();return new LocalImageFixture(context,tree,root,image);
    }
    public void close()throws Exception{try{assertTrue("Remove only the explicitly verified synthetic folder",DocumentsContract.deleteDocument(context.getContentResolver(),root));}finally{try{context.getContentResolver().releasePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(SecurityException alreadyRevoked){/* Deleting our fixture may revoke its own grant. */}}}
}
