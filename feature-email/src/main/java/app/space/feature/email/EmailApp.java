package app.space.feature.email;

import app.space.core.design.TicIconView;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;
import app.space.core.suite.SettingsGroup;
import app.space.core.suite.SuiteApp;

import java.util.Collections;
import java.util.List;

/** Email: Compose and archive mail; sending uses your own mail app. */
public final class EmailApp implements SuiteApp {

    /** Stable id. Persisted in the user's navigation order, so it must never change. */
    public static final String ID = "email";

    @Override public String id() { return ID; }
    @Override public String label() { return "Email"; }
    @Override public String tagline() { return "Compose and archive mail; sending uses your own mail app."; }
    @Override public int iconGlyph() { return TicIconView.EMAIL; }
    @Override public int defaultOrder() { return 150; }
    @Override public boolean supportsWidgets() { return false; }

    @Override public boolean acceptsRoute(String route) {
        // A stale deep link must return false rather than throw: a missing app and an app
        // declining a route look identical to the caller, which is what lets L2/L3 exclude
        // this module without the host needing to know.
        return route == null || route.isEmpty() || "list".equals(route);
    }

    @Override public List<SettingsGroup> settingsGroups() { return Collections.emptyList(); }

    @Override public Screen createScreen(ScreenHost host, String route) {
        return acceptsRoute(route) ? new EmailScreen(host) : null;
    }
}
