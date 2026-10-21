package app.space.core.suite;

import android.content.Context;
import app.space.core.design.Palette;
import java.util.List;

/**
 * What the MySpace chrome owes a screen.
 *
 * <p>This is the only channel through which an app may touch the shell, which is what
 * keeps {@code :feature-*} modules independent of {@code :shell}. Because navigation is
 * user-customisable across hub, rail and drawer (SUITE-PLAN.md §3.3), the shell's
 * rendering will change; apps must not notice.
 *
 * <p><b>The availability methods are the point of this interface.</b> An app can be
 * hidden by the user, excluded from a {@code lite} build flavour, or not yet downloaded
 * as an on-demand module. They therefore return {@code false} and {@code null} rather
 * than throwing, so a missing app is an ordinary outcome and never a crash. Any feature
 * that links to another app must handle the negative case.
 */
public interface ScreenHost {

    /** Themed context for building views. */
    Context context();

    /**
     * Resolved colours for the current appearance.
     *
     * <p>Screens must use this instead of computing their own, so that a theme, accent
     * or glass change is applied identically everywhere and one edit cannot leave a
     * single app looking wrong.
     */
    Palette palette();

    /**
     * Asks the shell to rebuild the visible screen.
     *
     * <p>Used after a change that affects appearance — theme, accent, glass preset,
     * motion style — mirroring the clock's existing behaviour of re-rendering rather
     * than patching individual views.
     */
    void invalidate();

    /**
     * Whether the given app is present in this build and not hidden by the user.
     *
     * <p>Check this before offering any affordance that leads to another app.
     */
    boolean isAppAvailable(String id);

    /**
     * The registered apps the user can currently reach, in the user's order, excluding
     * hidden ones. Never {@code null}; may be empty.
     */
    List<SuiteApp> availableApps();

    /** Opens another app at its default destination. Returns false if it is not available. */
    boolean openApp(String id);

    /**
     * Opens another app at a specific destination.
     *
     * @return false if the app is not available <em>or</em> does not accept the route.
     *         Callers must handle false; a stale widget deep link can name an app the
     *         user has hidden or a route an older version does not have.
     */
    boolean openApp(String id, String route);

    /** Opens the Settings page at its top. */
    void openSettings();

    /**
     * Opens Settings at a specific group contributed by some app.
     *
     * <p>Does nothing if that group is not present, which is the normal case once an
     * app is hidden or excluded from the build.
     */
    void openSettings(String groupId);
}
