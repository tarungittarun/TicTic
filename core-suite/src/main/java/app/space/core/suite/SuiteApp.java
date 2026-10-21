package app.space.core.suite;

import app.space.core.domain.NavOrder;
import java.util.List;

/**
 * One app inside MySpace.
 *
 * <p>This is the contract that makes "one install, many apps" structural rather than
 * a hardcoded list. Today the clock's navigation is a literal
 * {@code String[] tabs = {"Clock","Reminders","Timer","Stopwatch","Routines","Usage"}}
 * inside a 501-line {@code MainActivity}, which cannot grow to thirteen apps. Each app
 * instead declares itself here, and the shell renders whatever is registered — so
 * adding an app touches no navigation code (SUITE-PLAN.md §3.2).
 *
 * <p><b>Implementations must tolerate being absent.</b> A suite app may be hidden by
 * the user (L1), excluded from a {@code lite} build flavour (L2), or not yet downloaded
 * as an on-demand module (L3). Nothing here may assume another app is present; use
 * {@link ScreenHost#isAppAvailable(String)} and treat {@code false} as normal.
 *
 * <p>Implementations live in {@code :feature-*} modules. {@code :app} assembles the
 * list of those actually present in the build and hands it to the shell — there is no
 * reflection or classpath scanning, so startup stays cheap and R8 keeps working.
 *
 * <p>Extends {@link NavOrder.Entry} so the shell can pass registered apps straight into
 * the pure ordering rules in {@code :core-domain} without an adapter: {@code id()},
 * {@code defaultOrder()} and {@code label()} are declared here with exactly the
 * signatures that interface requires. Those rules are re-declared below rather than
 * inherited silently, because each carries a constraint an implementor must know about.
 */
public interface SuiteApp extends NavOrder.Entry {

    /**
     * Stable identifier, e.g. {@code "clock"} or {@code "calculator"}.
     *
     * <p>Persisted in navigation preferences and used in deep links, so it must never
     * change once shipped. Must be non-empty and contain no comma, because nav
     * preferences store id lists comma-joined; {@code SuiteRegistry} rejects violations
     * at construction rather than corrupting settings later.
     */
    @Override
    String id();

    /** User-visible name shown on the hub tile, rail and drawer. */
    @Override
    String label();

    /** One short line describing the app, shown on the hub tile. May be empty. */
    String tagline();

    /**
     * Icon to draw, as a {@code TicIconView} glyph constant from {@code :core-design}.
     * Drawn locally so the suite needs no icon pack.
     */
    int iconGlyph();

    /**
     * Position in the hub before the user reorders. Lower sorts first. Users' explicit
     * ordering always wins; this only decides where a <em>newly added</em> app appears.
     */
    @Override
    int defaultOrder();

    /** Whether this app contributes home-screen widgets, which surfaces a Studio entry. */
    boolean supportsWidgets();

    /**
     * Whether this app can open the given sub-destination.
     *
     * <p>Modelled on the clock, whose existing widget intents already route to
     * {@code "alarms"}, {@code "reminders"}, {@code "timer"}, {@code "stopwatch"},
     * {@code "routines"} and {@code "usage"}. Keeping routes as strings rather than an
     * enum means a new app defines its own destinations without editing this module.
     */
    boolean acceptsRoute(String route);

    /** Settings categories this app contributes to the Settings page. May be empty. */
    List<SettingsGroup> settingsGroups();

    /**
     * Creates the screen to display.
     *
     * @param host  the shell's services, including palette and app-availability lookup
     * @param route sub-destination to open, or {@code null} for the app's own default.
     *              Callers must have checked {@link #acceptsRoute(String)} first, but
     *              implementations should still fall back rather than throw, because a
     *              stale deep link or shortcut can name a route an older version lacks.
     */
    Screen createScreen(ScreenHost host, String route);
}
