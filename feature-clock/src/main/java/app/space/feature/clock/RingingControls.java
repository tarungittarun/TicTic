package app.space.feature.clock;

import android.content.Context;
import android.content.SharedPreferences;
import app.space.core.data.AppPrefs;

/**
 * What the hardware volume keys do while an alarm is ringing, and the migration that
 * established those settings. Moved out of {@code DataStore} in step 1d-4.
 *
 * <p>Separate from {@link AlarmRules} because this is about the ringing screen's
 * controls rather than when an alarm fires — different concern, different lifecycle.
 * Both are clock-only, so neither belongs in the shared data layer.
 *
 * <p>Action values: 1 = Snooze, 2 = Stop. The two keys are independent, which was the
 * whole point of the migration below.
 */
public final class RingingControls {

    private RingingControls() {
    }

    /**
     * Splits the former single shared action into independent Volume Up / Volume Down
     * keys.
     *
     * <p>Runs once. Each new key is written only if absent, so a user who has already
     * configured one button keeps that choice when the migration finally reaches them —
     * overwriting unconditionally would reset their settings on upgrade.
     */
    public static void migrate(Context c) {
        SharedPreferences p = AppPrefs.prefs(c);
        if (p.getBoolean("ringing_controls_v111_migrated", false)) return;
        int old = p.getInt("alarm_volume_buttons", 1);
        int action = (old == 2 || old == 3) ? 2 : 1;
        SharedPreferences.Editor e = p.edit();
        if (!p.contains("alarm_volume_up_action")) e.putInt("alarm_volume_up_action", action);
        if (!p.contains("alarm_volume_down_action")) e.putInt("alarm_volume_down_action", action);
        if (!p.contains("alarm_ringing_actions")) e.putInt("alarm_ringing_actions", 0);
        e.putBoolean("ringing_controls_v111_migrated", true).apply();
    }

    /**
     * What the given volume key does while ringing: 1 = Snooze, 2 = Stop.
     *
     * <p>Migrates on read so a key press on an upgraded install can never fall through to
     * an uninitialised preference. Anything other than an explicit 2 is treated as
     * Snooze, because defaulting to Stop would silently discard an alarm the user meant to
     * keep.
     */
    public static int volumeAction(Context c, boolean volumeUp) {
        migrate(c);
        SharedPreferences p = AppPrefs.prefs(c);
        String key = volumeUp ? "alarm_volume_up_action" : "alarm_volume_down_action";
        int value = p.getInt(key, 1);
        return value == 2 ? 2 : 1;
    }

    /** Compatibility accessor: the Volume Up choice. */
    public static int volumeAction(Context c) {
        return volumeAction(c, true);
    }

    /**
     * Whether the ringing screen offers a Snooze action at all. Users who map both volume
     * keys to Stop can turn the on-screen Snooze off, so the ringing screen shows only
     * what will actually do something.
     */
    public static boolean showSnoozeAction(Context c) {
        migrate(c);
        return AppPrefs.prefs(c).getInt("alarm_ringing_actions", 0) == 0;
    }
}
