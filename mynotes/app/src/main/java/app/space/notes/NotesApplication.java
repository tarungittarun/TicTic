package app.space.notes;
import android.app.Application;
import app.space.notes.runtime.Diagnostics;
/** Initializes persisted opt-in even for receiver-only/cold-start processes. */
public final class NotesApplication extends Application {
 @Override public void onCreate(){super.onCreate();Diagnostics.initialize(this);}
 @Override public void onTrimMemory(int level){Diagnostics.record("memory.trim","level="+level);super.onTrimMemory(level);}
 @Override public void onLowMemory(){Diagnostics.record("memory.low","system callback");super.onLowMemory();}
}
