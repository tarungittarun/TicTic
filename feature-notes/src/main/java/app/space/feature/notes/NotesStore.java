package app.space.feature.notes;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import app.space.core.data.AppPrefs;

/**
 * Notes persistence. One JSON array in the shared preference store.
 *
 * <p>Data outlives the module (SUITE-PLAN.md §3.4): the key is stable and the rows are
 * plain JSON, so notes survive the module being excluded from a {@code lite} build and are
 * readable by the export/import bundle.
 */
public final class NotesStore {

    public static final String KEY = "notes_items";
    public static final int CAP = 500;

    private NotesStore() {}

    public static List<NotesRules.Note> load(Context c) {
        List<NotesRules.Note> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(AppPrefs.prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new NotesRules.Note(
                        o.optString("id"), o.optString("title"), o.optString("body"), o.optLong("updatedAt"),
                        o.optInt("color", 0), o.optBoolean("pinned", false), o.optBoolean("archived", false)));
            }
        } catch (Exception e) {
            // Corrupt or absent data reads as an empty list rather than crashing the screen.
            return new ArrayList<>();
        }
        return NotesRules.capped(out, CAP);
    }

    public static void save(Context c, List<NotesRules.Note> notes) {
        JSONArray a = new JSONArray();
        try {
            for (NotesRules.Note n : NotesRules.capped(notes, CAP)) {
                a.put(new JSONObject().put("id", n.id).put("title", n.title)
                        .put("body", n.body).put("updatedAt", n.updatedAt)
                        .put("color", n.color).put("pinned", n.pinned).put("archived", n.archived));
            }
        } catch (Exception e) {
            return;
        }
        AppPrefs.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    /** Ids must never collide or be reordered; time plus a random suffix is enough here. */
    public static String newId() {
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString((int) (Math.random() * 1679616), 36);
    }
}
