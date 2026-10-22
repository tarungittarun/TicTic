package app.space.core.suite;

import android.content.Context;
import android.view.View;

/**
 * A screen contributed by a {@link SuiteApp}.
 *
 * <p>Deliberately small. The clock's existing screens are built entirely in Java code
 * with no XML layouts, so the contract hands back a {@link View} rather than a layout
 * resource or fragment — introducing fragments here would force a rewrite of every
 * screen for no gain, and would add an AndroidX dependency the project does not yet use.
 *
 * <p>Lifecycle is limited to resume and pause because that is what the existing screens
 * actually need: the clock starts and stops a repaint ticker and records foreground
 * usage time on exactly those two events. Anything broader would be speculation.
 */
public interface Screen {

    /** Title shown in the chrome while this screen is visible. May be empty. */
    String title();

    /**
     * Builds this screen's view tree.
     *
     * <p>Called each time the screen is shown, matching the current clock behaviour
     * where {@code render()} rebuilds the tree rather than diffing it. Colours must come
     * from {@link ScreenHost#palette()} so the screen follows the user's theme, accent
     * and glass settings without recomputing them.
     */
    View build(Context context);

    /**
     * Result of an activity this screen started, for example picking a document or an image.
     *
     * <p>Forwarded by the host, because a screen has no activity of its own and the platform
     * delivers results to the activity. Request codes 90 to 99 are reserved for screens; the
     * clock's own handlers use 81 to 87. The default does nothing, so a screen that starts no
     * activity need not implement it.
     *
     * <p>Delivered for cancelled results too, since a screen that is waiting on a picker has
     * to be able to stop waiting.
     */
    default void onActivityResult(int requestCode, int resultCode, android.content.Intent data) { }

    /** The screen became visible. Start tickers, animations and usage timing here. */
    void onResume();

    /**
     * The screen is no longer visible. Stop tickers and flush usage timing here.
     *
     * <p>Must be safe to call more than once and safe to call without a preceding
     * {@link #onResume()}, because the shell can be torn down by the system at any
     * point. The clock's existing pause path already stops a tone audition and flushes
     * accumulated foreground time, and both must keep happening.
     */
    void onPause();
}
