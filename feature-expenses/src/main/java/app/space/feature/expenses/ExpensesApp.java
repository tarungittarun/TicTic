package app.space.feature.expenses;

import app.space.core.design.TicIconView;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;
import app.space.core.suite.SettingsGroup;
import app.space.core.suite.SuiteApp;

import java.util.Collections;
import java.util.List;

/** Expense tracking: offline, no permissions, no network, no accounts. */
public final class ExpensesApp implements SuiteApp {

    public static final String ID = "expenses";

    @Override public String id() { return ID; }
    @Override public String label() { return "Expenses"; }
    @Override public String tagline() { return "Track spending by category, on device."; }
    @Override public int iconGlyph() { return TicIconView.EXPENSES; }
    @Override public int defaultOrder() { return 50; }
    @Override public boolean supportsWidgets() { return false; }

    @Override public boolean acceptsRoute(String route) {
        return route == null || route.isEmpty() || "list".equals(route);
    }

    @Override public List<SettingsGroup> settingsGroups() { return Collections.emptyList(); }

    @Override public Screen createScreen(ScreenHost host, String route) {
        return acceptsRoute(route) ? new ExpensesScreen(host) : null;
    }
}
