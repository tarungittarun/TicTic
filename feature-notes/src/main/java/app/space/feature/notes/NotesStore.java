package app.space.feature.notes;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import app.space.core.data.AppPrefs;
import app.space.core.data.db.MySpaceDatabase;
import app.space.core.data.db.NoteEntity;
import app.space.core.data.db.RoomBridge;

/**
 * Notes persistence: Room is the store of record in-app; one JSON array in the shared
 * preference store stays as the mirror.
 *
 * <p>The mirror is not legacy -- it is the exchange format. It is what the export/import
 * bundle carries, what a {@code lite} build without this module can still hold, and what
 * RoomBridge repopulates the database from after a restore. Data outlives the module
 * (SUITE-PLAN.md §3.4) because the mirror does.
 */
public final class NotesStore {

    public static final String KEY = "notes_items";
    public static final int CAP = 500;

    private NotesStore() {}

    public static List<NotesRules.Note> load(Context c) {
        RoomBridge.ensurePopulated(c);
        List<NoteEntity> rows = MySpaceDatabase.get(c).noteDao().selectAll();
        List<NotesRules.Note> out = new ArrayList<>();
        for (NoteEntity e : rows) {
            out.add(new NotesRules.Note(e.id, e.title, e.body, e.updatedAt,
                    e.color, e.pinned, e.archived));
        }
        return NotesRules.capped(out, CAP);
    }

    public static void save(Context c, List<NotesRules.Note> notes) {
        List<NotesRules.Note> capped = NotesRules.capped(notes, CAP);
        RoomBridge.ensurePopulated(c);
        List<NoteEntity> rows = new ArrayList<>();
        JSONArray a = new JSONArray();
        try {
            for (NotesRules.Note n : capped) {
                NoteEntity e = new NoteEntity();
                e.id = n.id;
                e.title = n.title;
                e.body = n.body;
                e.updatedAt = n.updatedAt;
                e.color = n.color;
                e.pinned = n.pinned;
                e.archived = n.archived;
                rows.add(e);
                a.put(new JSONObject().put("id", n.id).put("title", n.title)
                        .put("body", n.body).put("updatedAt", n.updatedAt)
                        .put("color", n.color).put("pinned", n.pinned).put("archived", n.archived));
            }
        } catch (Exception e) {
            return;
        }
        MySpaceDatabase db = MySpaceDatabase.get(c);
        db.noteDao().deleteAll();
        db.noteDao().insertAll(rows);
        AppPrefs.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    /** Ids must never collide or be reordered; time plus a random suffix is enough here. */
    public static String newId() {
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString((int) (Math.random() * 1679616), 36);
    }
}
