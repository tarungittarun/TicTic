package app.space.shell;

/**
 * How the user reaches each app inside MySpace.
 *
 * <p>Navigation is a user preference, not a design decision baked into the build —
 * the owner's requirement was that it be customisable (SUITE-PLAN.md §3.3). All three
 * render the same registry, so switching costs the user nothing and no app module is
 * aware of which one is active.
 *
 * <p>Each mode carries an explicit persisted code. Ordinals are deliberately not used:
 * reordering the constants in a later release would silently change every user's saved
 * choice.
 */
public enum NavMode {

    /**
     * A grid of app tiles, like a small launcher.
     *
     * <p>The only mode that scales comfortably past roughly a dozen apps, so it becomes
     * the sensible default as the suite grows. Shows each app's tagline.
     */
    HUB(0),

    /**
     * The slim horizontal rail TicTic ships today, showing the user's pinned apps.
     *
     * <p>Kept as the initial default so extracting the clock into the shell does not
     * change how the existing app looks or feels. Gets crowded past six or seven
     * entries, which is why it shows pinned apps rather than all of them.
     */
    RAIL(1),

    /**
     * A side list of every available app.
     *
     * <p>Lists all apps rather than a pinned subset, at the cost of putting discovery
     * one tap behind the menu.
     */
    DRAWER(2);

    private final int code;

    NavMode(int code) {
        this.code = code;
    }

    /** Value written to preferences. */
    public int code() {
        return code;
    }

    /** Default until the user chooses: the rail, which is what TicTic ships today. */
    public static NavMode defaultValue() {
        return RAIL;
    }

    /** Resolves a persisted code, falling back to the default for unknown values. */
    public static NavMode fromCode(int code) {
        for (NavMode mode : values()) {
            if (mode.code == code) return mode;
        }
        return defaultValue();
    }
}
