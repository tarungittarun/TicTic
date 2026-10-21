package app.space.core.platform;

import android.content.Context;
import android.os.PowerManager;
import app.space.core.data.AppPrefs;

/**
 * Whether the app should drop to its low-efficiency behaviour right now.
 *
 * <p>Unifies a three-way duplication found in step 1e: {@code MainActivity
 * .batterySaverOptimization()}, {@code WidgetEngine.powerSaverOptimization()} and
 * {@code AnalogClockView.powerSaverOptimization()} were logically identical — same
 * preference, same {@code PowerManager} read, same null guard — under three names in
 * three modules. That is the third such duplication found in four slices, after the corner
 * factors and the colour blend, which says the codebase was written by copying a rule into
 * each surface that needed it rather than sharing one.
 *
 * <p>The cost of drift here is concrete: widgets would keep animating while the app
 * throttled itself, or the analog clock would freeze while the rest of the UI stayed live,
 * and both would look like a rendering bug rather than a preference disagreement.
 *
 * <p>Moved to {@code :core-platform} so {@code :feature-clock} can reach it — the stopwatch
 * screen needs it to pick its tick rate, and a feature module may never depend on
 * {@code :app} where two of the three copies lived.
 */
public final class PowerState {

    private PowerState() {
    }

    /**
     * True when Android is in power-save mode <em>and</em> the user has left adaptive
     * throttling on.
     *
     * <p>The preference gates the OS read rather than the other way round, and defaults to
     * on: a user who turns adaptive power saving off is asking for full-fidelity rendering
     * regardless of what the system reports, and checking the OS first would make their
     * choice depend on a state they cannot see.
     */
    public static boolean isOptimizing(Context context) {
        if (!AppPrefs.prefs(context).getBoolean("adaptive_power_saver", true)) return false;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isPowerSaveMode();
    }
}
