package app.space.feature.calendar;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import app.space.core.data.AppPrefs;
import app.space.core.data.db.EventEntity;
import app.space.core.data.db.MySpaceDatabase;
import app.space.core.data.db.RoomBridge;

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
        RoomBridge.ensurePopulated(c);
        List<EventEntity> rows = MySpaceDatabase.get(c).eventDao().selectAll();
        List<CalendarRules.Event> out = new ArrayList<>();
        for (EventEntity e : rows) {
            out.add(new CalendarRules.Event(e.id, e.title, e.notes, e.whenMs,
                    e.reminderMinutes));
        }
        if (out.size() > CAP) return new ArrayList<>(out.subList(out.size() - CAP, out.size()));
        return out;
    }

    public static void save(Context c, List<CalendarRules.Event> events) {
        List<CalendarRules.Event> capped = events;
        if (capped.size() > CAP) capped = new ArrayList<>(capped.subList(capped.size() - CAP, capped.size()));
        RoomBridge.ensurePopulated(c);
        List<EventEntity> rows = new ArrayList<>();
        JSONArray a = new JSONArray();
        try {
            for (CalendarRules.Event e : capped) {
                EventEntity row = new EventEntity();
                row.id = e.id;
                row.title = e.title;
                row.notes = e.notes;
                row.whenMs = e.when;
                row.reminderMinutes = e.reminderMinutes;
                rows.add(row);
                a.put(new JSONObject().put("id", e.id).put("title", e.title)
                        .put("notes", e.notes).put("when", e.when)
                        .put("reminderMinutes", e.reminderMinutes));
            }
        } catch (Exception e) {
            return;
        }
        MySpaceDatabase db = MySpaceDatabase.get(c);
        db.eventDao().deleteAll();
        db.eventDao().insertAll(rows);
        AppPrefs.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    // ---- the owner's "basic settings": week start, the view the app opens on, and the
    //      reminder the editor pre-picks. -1 / defaults mean "follow the device". ----

    public static int getFirstDay(Context c) {
        return AppPrefs.prefs(c).getInt("calendar_first_day", -1);
    }

    public static void setFirstDay(Context c, int firstDay) {
        AppPrefs.prefs(c).edit().putInt("calendar_first_day", firstDay).apply();
    }

    public static int getDefaultView(Context c) {
        return AppPrefs.prefs(c).getInt("calendar_default_view", 0);
    }

    public static void setDefaultView(Context c, int view) {
        AppPrefs.prefs(c).edit().putInt("calendar_default_view", view).apply();
    }

    public static int getDefaultReminder(Context c) {
        return AppPrefs.prefs(c).getInt("calendar_default_reminder", -1);
    }

    public static void setDefaultReminder(Context c, int minutes) {
        AppPrefs.prefs(c).edit().putInt("calendar_default_reminder", minutes).apply();
    }

    public static String newId() {
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString((int) (Math.random() * 1679616), 36);
    }
}
