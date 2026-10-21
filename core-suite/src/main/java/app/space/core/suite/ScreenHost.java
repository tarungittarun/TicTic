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

    /**
     * Shows a short transient message.
     *
     * <p>Added because screens confirm persistence — "Saved", "Routine deleted" — and a
     * feature module has no business constructing its own {@code Toast} against a context
     * it does not own. Routing it through the host keeps confirmation copy consistent and
     * lets the shell decide presentation.
     */
    void toast(String message);

    /**
     * Tells the shell that persisted data changed, so every surface derived from it is
     * rebuilt: home-screen widgets, and all scheduled alarms, reminders and routines.
     *
     * <p>This is the generic form of a coupling that would otherwise be unfixable. The
     * clock's screens finish a save with {@code WidgetEngine.refreshAll} plus a
     * reschedule of three different schedulers, and all four are {@code :app}-only
     * classes. A screen in {@code :feature-clock} cannot call them, and importing them
     * into the feature module would invert the dependency rule (§3.1).
     *
     * <p>The host owns the mapping from "data changed" to "which surfaces care", so an
     * app never needs to know widgets or schedulers exist. That also means a new surface
     * can be added later without touching any screen.
     */
    void refreshSurfaces();
}
