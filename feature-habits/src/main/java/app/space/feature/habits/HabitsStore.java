package app.space.feature.habits;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.space.core.data.AppPrefs;

/** Habit persistence: one JSON array, completions stored as epoch days. */
public final class HabitsStore {

    public static final String KEY = "habits_items";
    public static final int CAP = 100;

    private HabitsStore() {}

    /** One habit and the days it was completed. */
    public static final class Habit {
        public final String id;
        public final String name;
        public final Set<Long> days;

        public Habit(String id, String name, Set<Long> days) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
            this.days = days == null ? new HashSet<Long>() : days;
        }
    }

    public static List<Habit> load(Context c) {
        List<Habit> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(AppPrefs.prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Set<Long> days = new HashSet<>();
                JSONArray d = o.optJSONArray("days");
                if (d != null) for (int j = 0; j < d.length(); j++) days.add(d.getLong(j));
                out.add(new Habit(o.optString("id"), o.optString("name"), days));
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        if (out.size() > CAP) return new ArrayList<>(out.subList(0, CAP));
        return out;
    }

    public static void save(Context c, List<Habit> habits) {
        JSONArray a = new JSONArray();
        try {
            for (Habit h : habits) {
                JSONArray days = new JSONArray();
                List<Long> sorted = new ArrayList<>(h.days);
                java.util.Collections.sort(sorted);
                for (Long d : sorted) days.put(d.longValue());
                a.put(new JSONObject().put("id", h.id).put("name", h.name).put("days", days));
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
