package app.tictic.clock;

import android.content.Context;
import android.content.SharedPreferences;
import app.space.core.data.AppActivityLog;
import app.space.core.data.AppFormat;
import app.space.core.data.AppPrefs;
import app.space.core.data.AppTheme;
import app.space.feature.clock.AlarmRules;
import app.space.feature.clock.RingingControls;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Legacy facade over the extracted data and clock layers.
 *
 * <p>Every method here is now a one-line delegate. The class is kept only because ~135
 * call sites across 25 files in {@code :app} read through it and roughly fifteen contract
 * tests assert on those call sites; rewriting them all in one pass would be unattributable
 * if the build broke. See SUITE-PLAN.md §6.1.
 *
 * <p>It holds no logic of its own as of step 1d-4. The clock's recurrence rules and
 * ringing-control settings moved to {@code :feature-clock}, which is what let that module
 * exist before any screen moved: a feature may not depend on {@code :app}, so anything a
 * screen needs has to be reachable from a {@code :core-*} or {@code :feature-*} module
 * first. */
final class DataStore {
    /**
     * References {@link AppPrefs#PREF} rather than repeating the literal, so the file
     * name has one source of truth across modules. Divergence here would read settings
     * from a different file than the one being written, silently stranding every user's
     * data with no crash.
     */
    static final String PREF = AppPrefs.PREF;
    static SharedPreferences prefs(Context c) { return AppPrefs.prefs(c); }

    /** Migrate the former shared action to independent keys: Control volume becomes Snooze; Turn off becomes Stop. Values: 1=Snooze, 2=Stop. */
    static void migrateRingingControls(Context c) { RingingControls.migrate(c); }
    static int alarmVolumeAction(Context c, boolean volumeUp) { return RingingControls.volumeAction(c, volumeUp); }
    /** Compatibility accessor: the Volume Up choice. */
    static int alarmVolumeAction(Context c) { return RingingControls.volumeAction(c); }
    static boolean showRingingSnoozeAction(Context c) { return RingingControls.showSnoozeAction(c); }

    static JSONArray array(Context c, String key) { return AppPrefs.array(c, key); }
    static void putArray(Context c, String key, JSONArray a) { AppPrefs.putArray(c, key, a); }

    /** v1.9.2 one-time migration: old app defaults were 10 min / alarm tone. New default is a 30-minute notification ping. */
    static void migrateReminderDefaults(Context c) { AlarmRules.migrateReminderDefaults(c); }

    static JSONObject find(Context c, String key, long id) { return AppPrefs.find(c, key, id); }
    static void log(Context c, String type, String title, String detail) { AppActivityLog.log(c, type, title, detail); }
    static String formatTime(Context c, int hour, int minute) { return AppFormat.formatTime(c, hour, minute); }
    static String formatDate(long millis) { return AppFormat.formatDate(millis); }

    /** When an alarm next fires. The rule itself lives in {@link AlarmRules}, where it is unit-tested. */
    static long nextOccurrence(JSONObject alarm, long from) { return AlarmRules.nextOccurrence(alarm, from); }
    static String daysLabel(int mask) { return AlarmRules.daysLabel(mask); }
    static String nextAlarmText(Context c) { return AlarmRules.nextAlarmText(c); }

    static int themeMode(Context c) { return AppTheme.themeMode(c); } // 0 system, 1 light, 2 dark
    static boolean dark(Context c) { return AppTheme.dark(c); }
    /** @deprecated read the palette from {@link AppTheme#ACCENT_COLORS}; kept so existing call sites compile during the 1d extraction. */
    static final int[] ACCENT_COLORS = AppTheme.ACCENT_COLORS;
    static int accent(Context c) { return AppTheme.accent(c); }
}
