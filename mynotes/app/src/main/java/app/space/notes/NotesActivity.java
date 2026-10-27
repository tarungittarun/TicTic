package app.space.notes;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import app.space.notes.runtime.NotesController;
import app.space.notes.runtime.NotesHost;
/** Thin standalone host. Business/UI code is reusable by a future MySpace adapter. */
public final class NotesActivity extends Activity implements NotesHost {
    private NotesController controller;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        controller=new NotesController(this,this,state);
        setContentView(controller.view());
    }
    @Override protected void onPause() { controller.flush(); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        controller.saveState(state); super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() { controller.close(); super.onDestroy(); }
    @Override public void onBackPressed() { if(!controller.back()) super.onBackPressed(); }
    @Override public void chooseLocalFolder() { chooseTree(94); }
    @Override public void openExistingFolder() { chooseTree(95); }
    @Override public void exportBackup() {
        Intent picker=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json");
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.putExtra(Intent.EXTRA_TITLE,"MyNotes-backup-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",java.util.Locale.ROOT).format(new java.util.Date())+".json");
        openPicker(picker,96);
    }
    @Override public void importBackup() {
        Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json");
        picker.addCategory(Intent.CATEGORY_OPENABLE);openPicker(picker,97);
    }
    private void openPicker(Intent picker,int request) {
        try {startActivityForResult(picker,request);}
        catch(android.content.ActivityNotFoundException e) {showMessage("No document picker is installed.");}
    }
    private void chooseTree(int request) {
        Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        try { startActivityForResult(picker,request); }
        catch(android.content.ActivityNotFoundException e) { showMessage("No folder picker is installed."); }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request<94 || request>97)return;
        if(result!=RESULT_OK || data==null || data.getData()==null) {
            android.util.Log.i("MyNotes","picker outcome=cancelled request="+request);return;
        }
        Uri uri=data.getData();
        if(request==96) {controller.exportBackup(uri);return;}
        if(request==97) {controller.importBackup(uri);return;}
        try {
            int flags=data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if(flags!=(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)) {
                showMessage("Folder needs read and write access. Storage has not changed.");return;
            }
            getContentResolver().takePersistableUriPermission(uri,flags);
            if(request==94)controller.selectFolder(uri);else controller.openExistingFolder(uri);
        } catch(SecurityException e) { showMessage("Folder permission could not be retained. Storage has not changed."); }
    }
    @Override public void shareText(String title,String text) {
        Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT,title).putExtra(Intent.EXTRA_TEXT,text);
        try {startActivity(Intent.createChooser(share,"Share notes"));}
        catch(android.content.ActivityNotFoundException e) {showMessage("No sharing app is installed.");}
    }
    @Override public String versionLabel() { return "MyNotes " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"; }
    @Override public void showMessage(String message) {
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }
}
