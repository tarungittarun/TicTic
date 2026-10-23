package app.space.core.data.db;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.space.core.data.AppPrefs;

/**
 * The one-time-in-each-direction door between the JSON mirror in prefs and the Room tables.
 *
 * <p>Population is keyed on emptiness, not on a migrated-flag: an empty table plus a non-empty
 * mirror means "import", whether this is a first run, a fresh install, or the morning after a
 * backup restore that deleted this database and rewrote the prefs underneath it. A flag would
 * have to survive exactly that restore -- and the restored flag would arrive from the source
 * device saying "already migrated", over a database that no longer exists. Emptiness cannot lie.
 */
public final class RoomBridge {

    private RoomBridge() { }

    /** Idempotent; cheap once the tables hold rows. Call before every Room read. */
    public static void ensurePopulated(Context context) {
        MySpaceDatabase db = MySpaceDatabase.get(context);
        if (db.noteDao().countAll() == 0) populateNotes(context, db);
        if (db.eventDao().countAll() == 0) populateEvents(context, db);
    }

    private static void populateNotes(Context context, MySpaceDatabase db) {
        try {
            JSONArray a = new JSONArray(AppPrefs.prefs(context).getString("notes_items", "[]"));
            List<NoteEntity> rows = new ArrayList<>();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                NoteEntity e = new NoteEntity();
                e.id = o.optString("id");
                e.title = o.optString("title");
                e.body = o.optString("body");
                e.updatedAt = o.optLong("updatedAt");
                e.color = o.optInt("color", 0);
                e.pinned = o.optBoolean("pinned", false);
                e.archived = o.optBoolean("archived", false);
                if (!e.id.isEmpty()) rows.add(e);
            }
            if (!rows.isEmpty()) db.noteDao().insertAll(rows);
        } catch (Exception ignored) {
            // A corrupt mirror must not brick the app; the table simply stays empty.
        }
    }

    private static void populateEvents(Context context, MySpaceDatabase db) {
        try {
            JSONArray a = new JSONArray(AppPrefs.prefs(context).getString("calendar_items", "[]"));
            List<EventEntity> rows = new ArrayList<>();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                EventEntity e = new EventEntity();
                e.id = o.optString("id");
                e.title = o.optString("title");
                e.notes = o.optString("notes");
                e.whenMs = o.has("when") ? o.optLong("when") : -1L;
                e.reminderMinutes = o.optInt("reminderMinutes", -1);
                if (!e.id.isEmpty()) rows.add(e);
            }
            if (!rows.isEmpty()) db.eventDao().insertAll(rows);
        } catch (Exception ignored) { }
    }

    /**
     * Called after a backup restore rewrote the prefs on disk: the database that existed
     * before the restore is now describing data that no longer exists, so it goes, and the
     * next ensurePopulated rebuilds it from the restored mirror.
     */
    public static void resetAfterRestore(Context context) {
        MySpaceDatabase.shutdown();
        File dir = new File(context.getFilesDir().getParentFile(), "databases");
        for (String name : new String[]{"myspace.db", "myspace.db-wal", "myspace.db-shm",
                "myspace.db-journal"}) {
            File f = new File(dir, name);
            if (f.exists()) f.delete();
        }
    }
}
