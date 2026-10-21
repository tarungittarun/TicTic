package app.space.shell;

import app.space.core.suite.SuiteApp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The apps present in this build, in registration order.
 *
 * <p>Constructed by {@code :app}, which is the only module allowed to see both the
 * shell and every feature module. There is no reflection and no classpath scanning: an
 * explicit list keeps startup cheap, keeps R8 able to shrink unused apps, and makes the
 * {@code full} and {@code lite} flavours differ by which apps are added to the list
 * rather than by runtime checks (SUITE-PLAN.md §3.4 layer L2).
 *
 * <p>Validation happens here, at construction, rather than when navigation preferences
 * are read. An id containing a comma or a duplicate id would corrupt the comma-joined
 * order and hidden lists in {@link NavPrefs}, and that corruption would surface much
 * later as a wrong app opening — far harder to trace than a startup failure naming the
 * offending app.
 */
public final class SuiteRegistry {

    private final List<SuiteApp> apps;

    public SuiteRegistry(List<SuiteApp> apps) {
        if (apps == null) throw new IllegalArgumentException("SuiteRegistry requires a non-null app list");
        List<SuiteApp> copy = new ArrayList<>(apps.size());
        Set<String> seen = new HashSet<>();
        for (SuiteApp app : apps) {
            if (app == null) throw new IllegalArgumentException("SuiteRegistry contains a null SuiteApp");
            String id = app.id();
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("SuiteApp " + app.getClass().getName() + " has an empty id");
            }
            if (id.indexOf(',') >= 0) {
                throw new IllegalArgumentException("SuiteApp id must not contain a comma: " + id);
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Duplicate SuiteApp id: " + id);
            }
            copy.add(app);
        }
        this.apps = Collections.unmodifiableList(copy);
    }

    /** Every registered app, in registration order. Never null, may be empty. */
    public List<SuiteApp> apps() {
        return apps;
    }

    /** The app with this id, or {@code null} if it is not in this build. */
    public SuiteApp find(String id) {
        if (id == null) return null;
        for (SuiteApp app : apps) {
            if (id.equals(app.id())) return app;
        }
        return null;
    }

    /** Whether an app with this id is present in this build. */
    public boolean contains(String id) {
        return find(id) != null;
    }

    /**
     * Finds the app that handles a route, or {@code null} if none does.
     *
     * <p>This is how existing widget deep links keep working after the clock moves
     * behind the shell. Widget intents carry a {@code "tab"} extra with values such as
     * {@code "alarms"}, {@code "timer"}, {@code "stopwatch"}, {@code "routines"} and
     * {@code "usage"}, which {@code MainActivity.applyRoute} maps onto its tab array
     * today. Asking the registry which app accepts the route means the shell never has
     * to hardcode the clock's id or its destination names — and when a second app ships
     * its own routes, they resolve the same way with no change here.
     *
     * <p>Returns the first match, so apps must not claim overlapping routes. Registration
     * order is controlled by {@code :app} and is therefore deterministic.
     */
    public SuiteApp resolveRoute(String route) {
        if (route == null || route.isEmpty()) return null;
        for (SuiteApp app : apps) {
            if (app.acceptsRoute(route)) return app;
        }
        return null;
    }
}
