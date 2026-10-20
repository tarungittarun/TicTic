package app.space.core.data;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Preference and JSON-collection access shared by every MySpace app.
 *
 * <p>Second slice extracted out of {@code app.tictic.clock.DataStore}
 * (SUITE-PLAN.md §6.1 step 1d-2). Carries the four generic storage operations —
 * {@code prefs}, {@code array}, {@code putArray}, {@code find} — which together
 * account for 242 of DataStore's 452 references. Clock-specific helpers stay behind:
 * occurrence rules, day labels, next-alarm text, ringing-control accessors and both
 * migrations belong to {@code :feature-clock} (step 1d-4).
 *
 * <p>{@link #PREF} is the single source of truth for the preference file name.
 * {@code DataStore.PREF} and {@code AppTheme.PREF} both reference it, so the three
 * cannot drift apart. The legacy {@code tictic_local_v1} value is preserved verbatim
 * on purpose: renaming it would silently strand every existing setting, alarm and
 * usage record on upgrade.
 *
 * <p>{@code org.json} is safe here — it is a framework package provided by Android at
 * runtime, so no dependency is introduced.
 */
public final class AppPrefs {

    /** The one on-device store for all MySpace settings and collections. */
    public static final String PREF = "tictic_local_v1";

    private AppPrefs() {}

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /** Reads a JSON array from a preference key, tolerating absent or corrupt values. */
    public static JSONArray array(Context c, String key) {
        try { return new JSONArray(prefs(c).getString(key, "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    public static void putArray(Context c, String key, JSONArray a) {
        prefs(c).edit().putString(key, a.toString()).apply();
    }

    /**
     * Finds the object in a keyed collection whose {@code id} matches.
     *
     * <p>Generic across collections — the clock uses it for {@code "alarms"},
     * {@code "reminders"} and {@code "routines"}, and every suite app with a
     * stored collection will use the same lookup. Returns {@code null} when absent,
     * which callers already handle.
     */
    public static JSONObject find(Context c, String key, long id) {
        JSONArray a = array(c, key);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && o.optLong("id") == id) return o;
        }
        return null;
    }
}
