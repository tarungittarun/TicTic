package app.space.notes.runtime;
import android.content.Context;
import app.space.notes.domain.Note;
import java.util.List;
/** Snapshot commits and OS projections share ONE reentrant monitor. Schema upgrades
 * may publish while holding the snapshot lock; a separate class lock caused a real
 * save/projection deadlock. Always re-read the latest private snapshot before publishing.
 */
public final class LocalSystemSurfaces {
 private LocalSystemSurfaces() { }
 public static void refresh(Context context) throws Exception {refresh(context,false);}
 public static void refresh(Context context,boolean force) throws Exception {synchronized(NotesRepository.SNAPSHOT_LOCK){List<Note> notes=new NotesRepository(context).localSnapshot();LocalReminders.reconcile(context,notes,force);NotesWidget.updateAll(context,notes);}}
 public static void deliver(Context context,String id,long at) throws Exception {synchronized(NotesRepository.SNAPSHOT_LOCK){for(Note note:new NotesRepository(context).localSnapshot())if(note.id.equals(id)){LocalReminders.notifyDue(context,note,at,System.currentTimeMillis());return;}}}
}
