package app.space.notes;
import android.app.Activity;
import android.os.Bundle;
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
    @Override public void showMessage(String message) {
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }
}
