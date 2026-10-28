package app.space.notes.runtime;
import android.content.Context;
import app.space.notes.domain.Note;
import java.util.List;
/** Serializes broadcasts and save projections; always re-read the latest atomic snapshot under this lock. */
public final class LocalSystemSurfaces {
 private LocalSystemSurfaces() { }
 public static synchronized void refresh(Context context) throws Exception {refresh(context,false);}
 public static synchronized void refresh(Context context,boolean force) throws Exception {List<Note> notes=new NotesRepository(context).localSnapshot();LocalReminders.reconcile(context,notes,force);NotesWidget.updateAll(context,notes);}
 public static synchronized void deliver(Context context,String id,long at) throws Exception {for(Note note:new NotesRepository(context).localSnapshot())if(note.id.equals(id)){LocalReminders.notifyDue(context,note,at,System.currentTimeMillis());return;}}
}
