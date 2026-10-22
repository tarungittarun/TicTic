package app.space.feature.music;

import app.space.core.design.TicIconView;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;
import app.space.core.suite.SettingsGroup;
import app.space.core.suite.SuiteApp;

import java.util.Collections;
import java.util.List;

/** Music: Play files you own and streams you add yourself. */
public final class MusicApp implements SuiteApp {

    /** Stable id. Persisted in the user's navigation order, so it must never change. */
    public static final String ID = "music";

    @Override public String id() { return ID; }
    @Override public String label() { return "Music"; }
    @Override public String tagline() { return "Play files you own and streams you add yourself."; }
    @Override public int iconGlyph() { return TicIconView.MUSIC; }
    @Override public int defaultOrder() { return 130; }
    @Override public boolean supportsWidgets() { return false; }

    @Override public boolean acceptsRoute(String route) {
        // A stale deep link must return false rather than throw: a missing app and an app
        // declining a route look identical to the caller, which is what lets L2/L3 exclude
        // this module without the host needing to know.
        return route == null || route.isEmpty() || "list".equals(route);
    }

    @Override public List<SettingsGroup> settingsGroups() { return Collections.emptyList(); }

    @Override public Screen createScreen(ScreenHost host, String route) {
        return acceptsRoute(route) ? new MusicScreen(host) : null;
    }
}
