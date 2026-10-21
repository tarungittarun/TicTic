package app.space.feature.calendar;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import app.space.core.data.AppPrefs;

/**
 * Persistence as one JSON array under a stable key.
 *
 * <p>Data outlives the module (SUITE-PLAN.md 3.4): the rows are plain JSON with no
 * module-specific encoding, so they survive the module being excluded from a build and are
 * readable by the export/import bundle. A corrupt or absent blob reads as an empty list
 * rather than crashing the screen.
 */
public final class CalendarStore {

    public static final String KEY = "calendar_items";
    public static final int CAP = 1000;

    private CalendarStore() {}

public static List<CalendarRules.Event> load(Context c) {
        List<CalendarRules.Event> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(AppPrefs.prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new CalendarRules.Event(o.optString("id"), o.optString("title"),
                        o.optString("notes"), o.has("when") ? o.optLong("when") : CalendarRules.NO_TIME));
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        if (out.size() > CAP) return new ArrayList<>(out.subList(out.size() - CAP, out.size()));
        return out;
    }

    public static void save(Context c, List<CalendarRules.Event> events) {
        JSONArray a = new JSONArray();
        try {
            for (CalendarRules.Event e : events) {
                a.put(new JSONObject().put("id", e.id).put("title", e.title)
                        .put("notes", e.notes).put("when", e.when));
            }
        } catch (Exception e) {
            return;
        }
        AppPrefs.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    public static String newId() {
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString((int) (Math.random() * 1679616), 36);
    }
}
