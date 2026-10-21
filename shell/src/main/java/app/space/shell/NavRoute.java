package app.space.shell;

import app.space.core.domain.NavOrder;

/**
 * One destination inside an app, as a navigation entry.
 *
 * <p>{@link NavPrefs} orders and hides whole apps, but the user's requirement is that
 * navigation be customisable (SUITE-PLAN.md §3.3), and inside the clock the things worth
 * arranging are its six destinations, not the single app that contains them. Rather than
 * write a second ordering rule for routes, a route implements the same
 * {@link NavOrder.Entry} contract an app does, so {@link NavOrder#resolve} — which is pure
 * and unit-tested in {@code :core-domain} — handles both.
 *
 * <p>That matters more than it looks. The alternative was route-specific ordering code in
 * an Android module, where it cannot be tested at all: there is no emulator here, and an
 * ordering bug shows up as a destination that quietly never appears rather than as a crash.
 */
public final class NavRoute implements NavOrder.Entry {

    private final String id;
    private final String label;
    private final int defaultOrder;

    public NavRoute(String id, String label, int defaultOrder) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("NavRoute requires an id");
        if (id.indexOf(',') >= 0) {
            // Ids are comma-joined when persisted; a comma here would corrupt the stored
            // order and surface much later as a wrong destination opening.
            throw new IllegalArgumentException("NavRoute id must not contain a comma: " + id);
        }
        this.id = id;
        this.label = label;
        this.defaultOrder = defaultOrder;
    }

    @Override public String id() { return id; }

    @Override public String label() { return label; }

    @Override public int defaultOrder() { return defaultOrder; }

    @Override public String toString() { return id; }
}
