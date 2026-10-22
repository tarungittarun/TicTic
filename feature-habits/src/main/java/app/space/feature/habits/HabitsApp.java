package app.space.feature.habits;

import app.space.core.design.TicIconView;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;
import app.space.core.suite.SettingsGroup;
import app.space.core.suite.SuiteApp;

import java.util.Collections;
import java.util.List;

/** Habit tracking: offline, no permissions, no network. */
public final class HabitsApp implements SuiteApp {

    public static final String ID = "habits";

    @Override public String id() { return ID; }
    @Override public String label() { return "Habits"; }
    @Override public String tagline() { return "Daily habits, streaks and weekly rate."; }
    @Override public int iconGlyph() { return TicIconView.HABITS; }
    @Override public int defaultOrder() { return 40; }
    @Override public boolean supportsWidgets() { return false; }

    @Override public boolean acceptsRoute(String route) {
        return route == null || route.isEmpty() || "list".equals(route);
    }

    @Override public List<SettingsGroup> settingsGroups() { return Collections.emptyList(); }

    @Override public Screen createScreen(ScreenHost host, String route) {
        return acceptsRoute(route) ? new HabitsScreen(host) : null;
    }
}
