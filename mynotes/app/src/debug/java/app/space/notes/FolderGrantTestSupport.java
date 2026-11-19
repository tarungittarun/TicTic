package app.space.notes;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
/** Debug APK only: obtains a real user-style SAF grant without mutating the Notes library settings. */
public final class FolderGrantTestSupport extends Activity {
 private static final String TAG="FolderGrantTest";
 private Intent completedGrant;
 @Override public void onCreate(Bundle state){super.onCreate(state);if(state==null){Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);picker.putExtra(DocumentsContract.EXTRA_INITIAL_URI,Uri.parse(getIntent().getStringExtra("initial")));android.util.Log.i(TAG,"launching native tree picker");startActivityForResult(picker,1);}}
 @Override public void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);android.util.Log.i(TAG,"picker result request="+request+" code="+result+" hasData="+(data!=null));Intent done=new Intent(getPackageName()+".TEST_FOLDER_GRANTED").setPackage(getPackageName());try{if(result!=RESULT_OK || data==null || data.getData()==null)throw new IllegalStateException("Folder selection cancelled");Uri uri=data.getData();String selected=DocumentsContract.getTreeDocumentId(uri),expected=getIntent().getStringExtra("expected"),alias=getIntent().getStringExtra("expectedAlias");if(expected!=null && !expected.equals(selected) && (alias==null || !alias.equals(selected)))throw new SecurityException("Selected folder does not match test fixture: "+selected);int required=Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION;if((data.getFlags()&required)!=required)throw new SecurityException("Local folder must grant both read and write access");getContentResolver().takePersistableUriPermission(uri,required);done.putExtra("uri",uri.toString());}catch(Exception failure){done.putExtra("error",failure.toString());}completedGrant=done;finish();}
 @Override protected void onStop(){super.onStop();Intent result=completedGrant;completedGrant=null;android.util.Log.i(TAG,"stopped with grant="+(result!=null));if(result!=null)new android.os.Handler(getMainLooper()).postDelayed(()->{getApplicationContext().sendBroadcast(result);android.util.Log.i(TAG,"grant result broadcast sent");},350);}
}
