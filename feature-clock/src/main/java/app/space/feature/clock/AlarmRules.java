package app.space.feature.clock;

import android.content.Context;
import android.content.SharedPreferences;
import app.space.core.data.AppFormat;
import app.space.core.data.AppPrefs;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;

/**
 * The clock's alarm-recurrence rules, moved out of {@code DataStore} in step 1d-4
 * (SUITE-PLAN.md §6.1).
 *
 * <p>These belong to the clock and to no other app, which is why they are here rather
 * than in {@code :core-data}: parking recurrence rules in the shared data layer would
 * make every future suite app depend on them. {@code DataStore} keeps one-line
 * delegates so its ~140 call sites in {@code :app} are untouched.
 *
 * <p>{@link #nextOccurrence(int, int, int, long, long)} is the rule that decides when
 * every alarm in the product fires. It was previously untestable: the only entry point
 * took a {@code JSONObject}, and {@code org.json} is stubbed as "not mocked" in Android
 * unit tests, so testing it would have meant putting a real JSON jar on the test
 * classpath and hoping it won the ordering fight against {@code android.jar}. Splitting
 * field extraction from the arithmetic removes that problem — the pure overload is what
 * carries the behaviour, and it is tested in {@code AlarmRulesTest}.
 */
public final class AlarmRules {

    private AlarmRules() {
    }

    /**
     * Next fire time for an alarm, reading the same four fields the clock has always
     * stored: {@code hour}, {@code minute}, {@code days} (a Sunday-first bitmask, 0 for
     * a one-off) and {@code date_millis}.
     */
    public static long nextOccurrence(JSONObject alarm, long from) {
        return nextOccurrence(alarm.optInt("hour", 7),
                alarm.optInt("minute", 0),
                alarm.optInt("days", 0),
                alarm.optLong("date_millis", 0),
                from);
    }

    /**
     * The recurrence rule itself, with no JSON in sight.
     *
     * @param daysMask Sunday-first bitmask, bit 0 = Sunday; 0 means a one-off alarm
     * @param savedDateMillis the picked calendar date for a one-off, 0 if none
     * @param from the reference instant; the result is always strictly after it
     */
    public static long nextOccurrence(int hour, int minute, int daysMask, long savedDateMillis, long from) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(from);
        if (daysMask == 0) {
            Calendar candidate = (Calendar) now.clone();
            if (savedDateMillis > 0) {
                Calendar selected = (Calendar) now.clone();
                selected.setTimeInMillis(savedDateMillis);
                candidate.set(Calendar.YEAR, selected.get(Calendar.YEAR));
                candidate.set(Calendar.MONTH, selected.get(Calendar.MONTH));
                candidate.set(Calendar.DAY_OF_MONTH, selected.get(Calendar.DAY_OF_MONTH));
            }
            candidate.set(Calendar.HOUR_OF_DAY, hour);
            candidate.set(Calendar.MINUTE, minute);
            candidate.set(Calendar.SECOND, 0);
            candidate.set(Calendar.MILLISECOND, 0);
            if (candidate.getTimeInMillis() <= from) candidate.add(Calendar.DAY_OF_YEAR, 1);
            return candidate.getTimeInMillis();
        }
        for (int add = 0; add < 8; add++) {
            Calendar candidate = (Calendar) now.clone();
            candidate.add(Calendar.DAY_OF_YEAR, add);
            candidate.set(Calendar.HOUR_OF_DAY, hour);
            candidate.set(Calendar.MINUTE, minute);
            candidate.set(Calendar.SECOND, 0);
            candidate.set(Calendar.MILLISECOND, 0);
            int bit = candidate.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
            if ((daysMask & (1 << bit)) != 0 && candidate.getTimeInMillis() > from) {
                return candidate.getTimeInMillis();
            }
        }
        return from + 86400000L;
    }

    /**
     * Compact weekday summary for an alarm row: {@code Once}, {@code Every day}, or the
     * selected initials joined with a middle dot.
     */
    public static String daysLabel(int mask) {
        if (mask == 0) return "Once";
        if (mask == 127) return "Every day";
        String[] names = {"S", "M", "T", "W", "T", "F", "S"};
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if ((mask & (1 << i)) != 0) {
                if (b.length() > 0) b.append(" · ");
                b.append(names[i]);
            }
        }
        return b.toString();
    }

    /**
     * One-line summary of the soonest enabled alarm, for the home screen and widgets.
     * Returns {@code "No alarm set"} when nothing is armed — a widget must never show a
     * blank line or a stale time.
     */
    public static String nextAlarmText(Context c) {
        JSONArray a = AppPrefs.array(c, "alarms");
        JSONObject best = null;
        long bestAt = Long.MAX_VALUE;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && o.optBoolean("enabled", true)) {
                long t = nextOccurrence(o, System.currentTimeMillis());
                if (t < bestAt) {
                    bestAt = t;
                    best = o;
                }
            }
        }
        if (best == null) return "No alarm set";
        return AppFormat.formatTime(c, best.optInt("hour"), best.optInt("minute"))
                + "  ·  " + AppFormat.formatDate(bestAt);
    }

    /**
     * v1.9.2 one-time migration: old app defaults were a 10-minute reminder on the alarm
     * tone. The new default is a 30-minute notification ping.
     *
     * <p>Runs once and records that it ran. It only rewrites alarms still sitting on the
     * old defaults, so a user who deliberately chose 10 minutes keeps their choice — that
     * is what the {@code has("reminder_minutes")} check is for, and losing it would
     * silently override people's settings on upgrade.
     */
    public static void migrateReminderDefaults(Context c) {
        SharedPreferences p = AppPrefs.prefs(c);
        if (p.getBoolean("reminder_defaults_v192_migrated", false)) return;
        JSONArray alarms = AppPrefs.array(c, "alarms");
        boolean changed = false;
        for (int i = 0; i < alarms.length(); i++) {
            JSONObject alarm = alarms.optJSONObject(i);
            if (alarm == null) continue;
            try {
                if (!alarm.has("reminder_minutes") || alarm.optInt("reminder_minutes", 10) == 10) {
                    alarm.put("reminder_minutes", 30);
                    changed = true;
                }
                String sound = alarm.optString("reminder_sound", "");
                if (sound.isEmpty() || "same".equals(sound)) {
                    alarm.put("reminder_sound", "ping");
                    changed = true;
                }
            } catch (Exception ignored) {
            }
        }
        if (changed) AppPrefs.putArray(c, "alarms", alarms);
        p.edit().putBoolean("reminder_defaults_v192_migrated", true).apply();
    }
}
