package app.space.shell;

import android.content.Context;
import android.content.SharedPreferences;
import app.space.core.data.AppPrefs;
import app.space.core.domain.NavOrder;
import app.space.core.suite.SuiteApp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The user's navigation customisation: which mode, which order, which apps are hidden,
 * and whether MySpace opens straight into the last-used app.
 *
 * <p>This is layer <b>L1</b> of the three control layers in SUITE-PLAN.md §3.4. It ships
 * first because it is the cheapest; L2 (build flavours) and L3 (on-demand modules) decide
 * whether an app is present at all, while L1 decides whether the user sees it.
 *
 * <p>Deliberately thin. This class reads and writes preferences and nothing else — every
 * ordering and encoding rule lives in {@link NavOrder} in {@code :core-domain}, where it
 * is pure JVM and unit-tested. Keeping the rules out of an Android module is the only
 * reason they can be tested at all: there is no emulator here, and a subtle ordering bug
 * shows up as a new app that never appears rather than as a crash.
 *
 * <p>Stored in the same preference file as everything else via {@code :core-data}, so one
 * export/import bundle carries the user's layout along with their data. Id lists are
 * comma-joined, which is why {@link SuiteRegistry} rejects an id containing a comma.
 *
 * <p>Reads never write. Resolving the order has to tolerate apps removed and apps added
 * since the user last arranged things, and persisting on every read would hit the disk on
 * each navigation render — it would also destroy the property that a temporarily absent
 * app returns to its original position.
 */
public final class NavPrefs {

    static final String KEY_MODE = "nav_mode";
    static final String KEY_ORDER = "nav_order";
    static final String KEY_HIDDEN = "nav_hidden";
    static final String KEY_OPEN_LAST = "nav_open_last";
    // Route-level keys are namespaced per app, so two apps may each have a destination
    // called "settings" without their arrangements colliding.
    static final String KEY_ROUTE_ORDER = "nav_route_order_";
    static final String KEY_ROUTE_HIDDEN = "nav_route_hidden_";
    static final String KEY_LAST_APP = "nav_last_app";

    private NavPrefs() {}

    private static SharedPreferences prefs(Context c) {
        return AppPrefs.prefs(c);
    }

    // ---- navigation mode -------------------------------------------------------

    public static NavMode mode(Context c) {
        return NavMode.fromCode(prefs(c).getInt(KEY_MODE, NavMode.defaultValue().code()));
    }

    public static void setMode(Context c, NavMode mode) {
        prefs(c).edit().putInt(KEY_MODE, (mode == null ? NavMode.defaultValue() : mode).code()).apply();
    }

    // ---- ordering --------------------------------------------------------------

    /**
     * Every registered app in the user's order, including hidden ones.
     *
     * <p>Apps the user has arranged keep their positions; apps added since are appended by
     * {@link SuiteApp#defaultOrder()}. The rules and their reasoning are in
     * {@link NavOrder#resolve}.
     */
    public static List<SuiteApp> orderedApps(Context c, SuiteRegistry registry) {
        Set<String> stored = NavOrder.parse(prefs(c).getString(KEY_ORDER, ""));
        return NavOrder.resolve(stored, registry.apps());
    }

    /** Apps the user can currently reach: ordered, with hidden ones removed. */
    public static List<SuiteApp> visibleApps(Context c, SuiteRegistry registry) {
        Set<String> hidden = hidden(c);
        List<SuiteApp> visible = new ArrayList<>();
        for (SuiteApp app : orderedApps(c, registry)) {
            if (!hidden.contains(app.id())) visible.add(app);
        }
        return visible;
    }

    /** Persists an explicit ordering. Ids that are not registered are ignored on read. */
    public static void saveOrder(Context c, List<String> ids) {
        prefs(c).edit().putString(KEY_ORDER, NavOrder.join(ids)).apply();
    }

    // ---- hiding ----------------------------------------------------------------

    /** Ids the user has hidden. Never null, may be empty. */
    public static Set<String> hidden(Context c) {
        return NavOrder.parse(prefs(c).getString(KEY_HIDDEN, ""));
    }

    public static boolean isHidden(Context c, String id) {
        return id != null && hidden(c).contains(id);
    }

    /** Replaces the hidden set. Passing an empty set unhides everything. */
    public static void setHidden(Context c, Set<String> ids) {
        prefs(c).edit().putString(KEY_HIDDEN, NavOrder.join(ids)).apply();
    }

    // ---- launch behaviour ------------------------------------------------------

    /** Whether MySpace opens straight into the last-used app instead of showing navigation. */
    public static boolean openLastApp(Context c) {
        return prefs(c).getBoolean(KEY_OPEN_LAST, false);
    }

    public static void setOpenLastApp(Context c, boolean open) {
        prefs(c).edit().putBoolean(KEY_OPEN_LAST, open).apply();
    }

    /** The last app the user opened, or {@code null} if none is recorded. */
    public static String lastApp(Context c) {
        String id = prefs(c).getString(KEY_LAST_APP, "");
        return id.isEmpty() ? null : id;
    }

    public static void setLastApp(Context c, String id) {
        prefs(c).edit().putString(KEY_LAST_APP, id == null ? "" : id).apply();
    }

    // ---- destinations inside one app ------------------------------------------

    /**
     * The destinations of one app that the user can currently reach, in the user's order.
     *
     * <p>Hidden routes are removed first, then {@link NavOrder#resolve} applies the stored
     * order — the same rule used for apps, so a route added in a later release appears in
     * its declared position rather than at the end, and a route the user hid stays hidden
     * even after an unrelated reorder.
     *
     * <p>{@code all} is supplied by the caller rather than read from the registry, because
     * an app owns its own destinations; the shell must not know what they are.
     */
    public static List<NavRoute> visibleRoutes(Context c, String appId, List<NavRoute> all) {
        if (all == null || all.isEmpty()) return java.util.Collections.emptyList();
        Set<String> hidden = NavOrder.parse(prefs(c).getString(KEY_ROUTE_HIDDEN + appId, ""));
        List<NavRoute> kept = new java.util.ArrayList<>(all.size());
        for (NavRoute r : all) {
            if (r != null && !hidden.contains(r.id())) kept.add(r);
        }
        return NavOrder.resolve(NavOrder.parse(prefs(c).getString(KEY_ROUTE_ORDER + appId, "")), kept);
    }

    public static Set<String> hiddenRoutes(Context c, String appId) {
        return NavOrder.parse(prefs(c).getString(KEY_ROUTE_HIDDEN + appId, ""));
    }

    public static void setHiddenRoutes(Context c, String appId, Set<String> ids) {
        prefs(c).edit().putString(KEY_ROUTE_HIDDEN + appId, NavOrder.join(ids)).apply();
    }

    /** Every destination the app declares, in the user's order, including hidden ones. */
    public static List<NavRoute> orderedRoutes(Context c, String appId, List<NavRoute> all) {
        if (all == null || all.isEmpty()) return java.util.Collections.emptyList();
        return NavOrder.resolve(NavOrder.parse(prefs(c).getString(KEY_ROUTE_ORDER + appId, "")), all);
    }

    public static void saveRouteOrder(Context c, String appId, List<String> ids) {
        prefs(c).edit().putString(KEY_ROUTE_ORDER + appId, NavOrder.join(ids)).apply();
    }
}
