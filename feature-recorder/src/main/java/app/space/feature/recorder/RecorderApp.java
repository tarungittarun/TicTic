package app.space.feature.recorder;

import app.space.core.design.TicIconView;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;
import app.space.core.suite.SettingsGroup;
import app.space.core.suite.SuiteApp;

import java.util.Collections;
import java.util.List;

/** Recorder: Voice recordings, stored on this device. */
public final class RecorderApp implements SuiteApp {

    /** Stable id. It is persisted in the user's navigation order, so it must never change. */
    public static final String ID = "recorder";

    @Override public String id() { return ID; }
    @Override public String label() { return "Recorder"; }
    @Override public String tagline() { return "Voice recordings, stored on this device."; }
    @Override public int iconGlyph() { return TicIconView.RECORDER; }
    @Override public int defaultOrder() { return 90; }
    @Override public boolean supportsWidgets() { return false; }

    @Override public boolean acceptsRoute(String route) {
        // A stale deep link naming a route this app never had must return false rather than
        // throw: a missing app and an app declining a route look the same to the caller.
        return route == null || route.isEmpty() || "list".equals(route);
    }

    @Override public List<SettingsGroup> settingsGroups() { return Collections.emptyList(); }

    @Override public Screen createScreen(ScreenHost host, String route) {
        return acceptsRoute(route) ? new RecorderScreen(host) : null;
    }
}
