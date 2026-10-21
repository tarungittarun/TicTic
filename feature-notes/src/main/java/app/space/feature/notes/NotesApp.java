package app.space.feature.notes;

import app.space.core.design.TicIconView;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;
import app.space.core.suite.SettingsGroup;
import app.space.core.suite.SuiteApp;

import java.util.Collections;
import java.util.List;

/** Notes and tasks: offline, no permissions, no network. */
public final class NotesApp implements SuiteApp {

    public static final String ID = "notes";

    @Override public String id() { return ID; }
    @Override public String label() { return "Notes"; }
    @Override public String tagline() { return "Notes and tasks, stored on this device."; }
    @Override public int iconGlyph() { return TicIconView.WORLD; }
    @Override public int defaultOrder() { return 30; }
    @Override public boolean supportsWidgets() { return false; }

    @Override public boolean acceptsRoute(String route) {
        return route == null || route.isEmpty() || "list".equals(route);
    }

    @Override public List<SettingsGroup> settingsGroups() { return Collections.emptyList(); }

    @Override public Screen createScreen(ScreenHost host, String route) {
        return acceptsRoute(route) ? new NotesScreen(host) : null;
    }
}
