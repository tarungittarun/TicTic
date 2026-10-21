package app.space.feature.clock;

import app.space.core.design.TicIconView;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;
import app.space.core.suite.SettingsGroup;
import app.space.core.suite.SuiteApp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The clock, declared as a suite app. The first real implementor of {@link SuiteApp}
 * (SUITE-PLAN.md §6.1 step 1h-iii), and the reference every later app module is written
 * against.
 *
 * <p>Registration is deliberately ahead of screen migration. Until now the contract had no
 * consumer at all, so its identity and routing surface had never been checked against
 * anything real; declaring the app is what turns those from assumptions into requirements.
 * The screens themselves move one at a time afterwards, each behind its own CI-verified
 * commit, because {@code MainActivity} is ~180 KB of inline rendering and a single large
 * move would be undiagnosable without a local compiler.
 *
 * <p>This class holds no state and no Android dependencies beyond the glyph constant, so
 * it can be instantiated by the registry without an {@code Activity} in hand.
 */
public final class ClockApp implements SuiteApp {

    /**
     * The destinations this app answers to.
     *
     * <p>These are exactly the {@code tab} extras that the clock's 24 widget providers and
     * its notification actions already put on their intents, including the legacy
     * {@code "log"} value that predates the rename to {@code "usage"}. A widget placed
     * before the rebrand sends {@code tab=log}; dropping it here would make that widget
     * open the app and show nothing, with no crash to explain why. {@code AlarmRulesTest}
     * cannot cover this — it is a cross-module agreement, so a contract test asserts the
     * two lists match instead.
     */
    public static final List<String> ROUTES = Collections.unmodifiableList(Arrays.asList(
            "clock", "alarms", "reminders", "timer", "stopwatch", "routines", "usage", "log"));

    @Override
    public String id() {
        return "clock";
    }

    @Override
    public String label() {
        return "Clock";
    }

    @Override
    public String tagline() {
        return "Alarms, reminders, timer, stopwatch and routines.";
    }

    @Override
    public int iconGlyph() {
        return TicIconView.CLOCK;
    }

    /**
     * First in the hub by default. The clock is the app this install started as, so a
     * fresh MySpace should open on it rather than on an alphabetical accident.
     */
    @Override
    public int defaultOrder() {
        return 0;
    }

    /**
     * True, and not marginally: the clock ships 24 widget providers across four families.
     * It is the only app in the suite for which Widget Studio is currently the primary
     * home-screen surface rather than an optional extra.
     */
    @Override
    public boolean supportsWidgets() {
        return true;
    }

    @Override
    public boolean acceptsRoute(String route) {
        return route != null && ROUTES.contains(route);
    }

    /**
     * Empty for now. The clock's settings surface is still built inline by
     * {@code MainActivity} — roughly a dozen groups including design, glass, motion,
     * alarms, reminders, timer, routines and about — and moves in step 1h-iv. Returning an
     * empty list rather than a partial one means the host renders no clock categories in a
     * suite Settings page until all of them can be rendered, so a half-migrated group
     * cannot appear as a dead entry.
     */
    @Override
    public List<SettingsGroup> settingsGroups() {
        return Collections.emptyList();
    }

    /**
     * Returns {@code null} for every route until that screen has been moved out of
     * {@code MainActivity}.
     *
     * <p>Null rather than a placeholder is intentional and matches the convention already
     * set by {@link ScreenHost}'s availability methods, which return false or null instead
     * of throwing so that a missing app degrades quietly. A placeholder screen would
     * render as a blank page — a visible regression — whereas null lets the host keep
     * rendering the tab inline exactly as it does today. The migration is therefore
     * invisible to the user until it is complete, one screen at a time.
     */
    @Override
    public Screen createScreen(ScreenHost host, String route) {
        return null;
    }
}
