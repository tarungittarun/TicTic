package app.space.notes;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
/** Debug APK only: obtains a real user-style SAF grant without mutating the Notes library settings. */
public final class FolderGrantTestSupport extends Activity {
 @Override public void onCreate(Bundle state){super.onCreate(state);if(state==null){Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);picker.putExtra("android.provider.extra.INITIAL_URI",Uri.parse(getIntent().getStringExtra("initial")));startActivityForResult(picker,1);}}
 @Override public void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);Intent done=new Intent(getPackageName()+".TEST_FOLDER_GRANTED").setPackage(getPackageName());try{if(result!=RESULT_OK || data==null || data.getData()==null)throw new IllegalStateException("Folder selection cancelled");Uri uri=data.getData();getContentResolver().takePersistableUriPermission(uri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));done.putExtra("uri",uri.toString());}catch(Exception failure){done.putExtra("error",failure.toString());}sendBroadcast(done);finish();}
}
