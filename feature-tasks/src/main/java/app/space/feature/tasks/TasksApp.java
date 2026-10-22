package app.space.feature.tasks;

import app.space.core.design.TicIconView;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;
import app.space.core.suite.SettingsGroup;
import app.space.core.suite.SuiteApp;

import java.util.Collections;
import java.util.List;

/** Tasks Checklists with due dates and priority. Offline, no permissions, no network. */
public final class TasksApp implements SuiteApp {

    public static final String ID = "tasks";

    @Override public String id() { return ID; }
    @Override public String label() { return "Tasks"; }
    @Override public String tagline() { return "Checklists with due dates and priority."; }
    @Override public int iconGlyph() { return TicIconView.TASKS; }
    @Override public int defaultOrder() { return 70; }
    @Override public boolean supportsWidgets() { return false; }

    @Override public boolean acceptsRoute(String route) {
        return route == null || route.isEmpty() || "list".equals(route);
    }

    @Override public List<SettingsGroup> settingsGroups() { return Collections.emptyList(); }

    @Override public Screen createScreen(ScreenHost host, String route) {
        return acceptsRoute(route) ? new TasksScreen(host) : null;
    }
}
