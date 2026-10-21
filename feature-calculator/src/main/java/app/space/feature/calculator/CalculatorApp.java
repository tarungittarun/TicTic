package app.space.feature.calculator;

import app.space.core.design.TicIconView;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;
import app.space.core.suite.SettingsGroup;
import app.space.core.suite.SuiteApp;

import java.util.Collections;
import java.util.List;

/**
 * The second app in the suite, and the first that is not the clock.
 *
 * <p>Deliberately the simplest of the thirteen requested apps: offline, no permissions, no
 * network, no storage, and no dependency on anything in {@code :app}. It exists to prove the
 * contract carries a second implementor without special cases -- and to make
 * {@code NavMode.HUB} worth building, since a hub with one registered app is a single tile.
 */
public final class CalculatorApp implements SuiteApp {

    /** Stable id. Persisted in the user's navigation order, so it must never change. */
    public static final String ID = "calculator";

    @Override public String id() { return ID; }

    @Override public String label() { return "Calculator"; }

    @Override public String tagline() { return "Arithmetic that never leaves the device."; }

    @Override public int iconGlyph() { return TicIconView.CALCULATOR; }

    /** After the clock (10) and leaving room, matching NavOrder's spacing convention. */
    @Override public int defaultOrder() { return 20; }

    @Override public boolean supportsWidgets() { return false; }

    @Override public boolean acceptsRoute(String route) {
        // One screen, so the only acceptable routes are "none" and its own name. A stale
        // deep link naming a route this app never had must return false, not throw.
        return route == null || route.isEmpty() || "basic".equals(route);
    }

    @Override public List<SettingsGroup> settingsGroups() { return Collections.emptyList(); }

    @Override public Screen createScreen(ScreenHost host, String route) {
        return acceptsRoute(route) ? new CalculatorScreen(host) : null;
    }
}
