package app.space.core.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Appearance state shared by every MySpace app: theme mode, resolved dark/light,
 * and the sixteen accent colours.
 *
 * <p>This is the first slice extracted out of {@code app.tictic.clock.DataStore}
 * (SUITE-PLAN.md §6.1 step 1d). It moved first because it is what the rest of the
 * design system reads: {@code GlassPreset}, {@code AppSurfaceStyle},
 * {@code AppGlassDrawable}, {@code GelMotion} and {@code AnalogClockView} all need
 * theme or accent values, and none of them can leave {@code :app} until this exists.
 *
 * <p>Logic is unchanged from the original; only the home moved. {@code DataStore}
 * keeps thin delegating methods so its 452 existing call sites are untouched.
 *
 * <p>Appearance state is read from the same preference file as everything else, via
 * {@link AppPrefs#PREF}. That name is intentionally the legacy {@code tictic_local_v1}:
 * changing it would strand every existing setting, alarm and usage record on upgrade,
 * with no crash to reveal it. A contract test pins the literal and asserts that this
 * class and the legacy {@code DataStore} both reference it rather than redefining it.
 */
public final class AppTheme {

    /**
     * References {@link AppPrefs#PREF} rather than repeating the literal, so the file
     * name has exactly one definition across all modules.
     */
    public static final String PREF = AppPrefs.PREF;

    private AppTheme() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /** 0 = follow system, 1 = light, 2 = dark. */
    public static int themeMode(Context c) {
        return prefs(c).getInt("theme", 0);
    }

    /**
     * Resolved dark mode. Honours an explicit choice, otherwise follows the system
     * uiMode. The 0x30/0x20 masks are {@code Configuration.UI_MODE_NIGHT_MASK} and
     * {@code UI_MODE_NIGHT_YES}, written as literals to match the original.
     */
    public static boolean dark(Context c) {
        int t = themeMode(c);
        return t == 2 || (t == 0 && (c.getResources().getConfiguration().uiMode & 0x30) == 0x20);
    }

    /** The sixteen named app accents, in palette order. */
    public static final int[] ACCENT_COLORS = {
            0xff3f7c73, 0xff8064d6, 0xffd46d61, 0xff3477b8,
            0xff94651d, 0xff32846f, 0xffb95078, 0xff52677f,
            0xff2553a5, 0xff754889, 0xff8a761b, 0xff167e83,
            0xffb65337, 0xff526bb5, 0xff516d40, 0xff992f4f
    };

    /** The selected accent, clamped into the palette. */
    public static int accent(Context c) {
        int i = prefs(c).getInt("accent", 0);
        return ACCENT_COLORS[Math.max(0, Math.min(ACCENT_COLORS.length - 1, i))];
    }
}
