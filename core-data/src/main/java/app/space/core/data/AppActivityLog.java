package app.space.core.data;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Chronological, on-device activity history — the entries shown on the Usage screen.
 *
 * <p>Third slice of the {@code DataStore} extraction (SUITE-PLAN.md §6.1 step 1d-2).
 * The mechanism is generic and suite-level: a typed, timestamped record with a bounded
 * history. Its callers are app-specific (the clock logs Alarm, Reminder, Routine,
 * Timer, Stopwatch, Widget, Settings and Clock events), which is why the type is a
 * free-form string rather than an enum — a new suite app logs its own types without
 * touching this module.
 *
 * <p>Two behaviours must not change, because both are user-visible promises documented
 * in README.md and PRODUCT.md:
 * <ul>
 *   <li>Nothing is appended when the user has turned off <b>Keep activity history</b>.
 *       The counter increment still happens, because <b>Record usage metrics</b> is a
 *       separate switch with its own check inside {@link UsageTracker}.</li>
 *   <li>History is capped at 100 retained entries, newest first.</li>
 * </ul>
 */
public final class AppActivityLog {

    /** Retained history entries. Bounded so the store cannot grow without limit. */
    public static final int MAX_ENTRIES = 100;

    private AppActivityLog() {}

    /**
     * Appends one history entry and increments the matching usage counter.
     *
     * @param type   event category, e.g. {@code "Alarm"}; normalised to a counter key
     *               by {@link UsageTracker#recordEvent}
     * @param title  short human-readable summary
     * @param detail longer context, may be empty
     */
    public static void log(Context c, String type, String title, String detail) {
        if (AppPrefs.prefs(c).getBoolean("activity_history_enabled", true)) {
            JSONArray old = AppPrefs.array(c, "events"), n = new JSONArray();
            try {
                n.put(new JSONObject().put("at", System.currentTimeMillis()).put("type", type).put("title", title).put("detail", detail));
                // Keeps at most MAX_ENTRIES-1 previous rows, so the new total is MAX_ENTRIES.
                for (int i = 0; i < old.length() && i < MAX_ENTRIES - 1; i++) n.put(old.getJSONObject(i));
            } catch (Exception ignored) {}
            AppPrefs.putArray(c, "events", n);
        }
        UsageTracker.recordEvent(c, type);
    }
}
