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
    private String ocrLanguage="eng",voiceExportId;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(state!=null){ocrLanguage=state.getString("ocrLanguage","eng");voiceExportId=state.getString("voiceExportId");}
        controller=new NotesController(this,this,state);
        setContentView(controller.view());
        if(state==null)acceptIncoming(getIntent());
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);acceptIncoming(intent);}
    private void acceptIncoming(Intent intent){if(Intent.ACTION_VIEW.equals(intent.getAction())){String id=intent.getStringExtra("noteId");if(id!=null && !id.isEmpty())controller.openNote(id);}else acceptShare(intent);}
    @Override public void requestHomeWidget(){android.appwidget.AppWidgetManager manager=android.appwidget.AppWidgetManager.getInstance(this);if(manager.isRequestPinAppWidgetSupported())manager.requestPinAppWidget(new android.content.ComponentName(this,app.space.notes.runtime.NotesWidget.class),null,null);else NotesHost.super.requestHomeWidget();}
    @Override public void requestReminderPermission(){if(android.os.Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},89);else controller.chooseReminder();}
    private void acceptShare(Intent intent){
        if(!Intent.ACTION_SEND.equals(intent.getAction()))return;
        if(!"text/plain".equals(intent.getType())){showMessage("Only plain-text shares are supported in this release.");return;}
        try{
            CharSequence body=intent.getCharSequenceExtra(Intent.EXTRA_TEXT);String subject=intent.getStringExtra(Intent.EXTRA_SUBJECT);
            controller.receiveText(new app.space.notes.domain.IncomingText(subject,body==null?null:body.toString()));
        }catch(RuntimeException invalid){showMessage("Nothing imported: "+invalid.getMessage());}
    }
    @Override protected void onPause() { controller.pause(); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("ocrLanguage",ocrLanguage);state.putString("voiceExportId",voiceExportId);
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
    @Override public void chooseOcrImage(String language){ocrLanguage=language;Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*");picker.addCategory(Intent.CATEGORY_OPENABLE);picker.putExtra(Intent.EXTRA_LOCAL_ONLY,true);openPicker(picker,99);}
    @Override public void requestVoiceCapture(){
        if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED)controller.startVoiceCapture();
        else requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},88);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==89){if(grants.length>0 && grants[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)controller.chooseReminder();else showMessage("Notifications were not enabled. Notes and all other features still work; choose Reminder again if you change your mind.");return;}if(request!=88)return;if(grants.length>0 && grants[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)controller.startVoiceCapture();else showMessage("Microphone access was not granted. Text, checklists, diagrams, OCR and storage still work.");}
    @Override public void exportVoice(String id){voiceExportId=id;Intent picker=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("audio/wav");picker.addCategory(Intent.CATEGORY_OPENABLE);picker.putExtra(Intent.EXTRA_TITLE,"MyNotes-voice-"+id+".wav");openPicker(picker,100);}
    @Override public void exportPdf() {
        Intent picker=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/pdf");picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.putExtra(Intent.EXTRA_TITLE,"MyNotes-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",java.util.Locale.ROOT).format(new java.util.Date())+".pdf");openPicker(picker,98);
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
        if(request<94 || request>100)return;
        if(result!=RESULT_OK || data==null || data.getData()==null) {
            android.util.Log.i("MyNotes","picker outcome=cancelled request="+request);return;
        }
        Uri uri=data.getData();
        if(request==99){controller.recognizeImage(uri,ocrLanguage);return;}
        if(request==100){if(voiceExportId!=null)controller.exportVoice(voiceExportId,uri);return;}
        if(request==98){controller.exportPdf(uri);return;}
        if(request==96) {controller.exportBackup(uri);return;}
        if(request==97) {controller.importBackup(uri);return;}
        try {
            int flags=data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if(flags!=(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)) {
                showMessage("Folder needs read and write access. Storage has not changed.");return;
            }
            getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
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
