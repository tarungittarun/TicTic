package app.space.core.domain;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The ordering rules that decide which apps a user sees, and in what sequence.
 *
 * <p>These matter because MySpace is meant to grow from one app to thirteen, and because
 * apps can also disappear from a given build — hidden by the user, excluded by a
 * {@code lite} flavour, or not yet downloaded as an on-demand module. A failure here is
 * not a crash: it is a new app that never appears, or a layout that quietly rearranges
 * itself. Both are easy to miss in review and awkward to diagnose on a device, which is
 * why the logic is pure and tested here rather than living inside {@code :shell}.
 */
public final class NavOrderTest {

    /** Minimal stand-in for a suite app. */
    private static final class App implements NavOrder.Entry {
        private final String id; private final int order; private final String label;
        App(String id, int order, String label) { this.id = id; this.order = order; this.label = label; }
        @Override public String id() { return id; }
        @Override public int defaultOrder() { return order; }
        @Override public String label() { return label; }
        @Override public String toString() { return id; }
    }

    private static List<String> ids(List<NavOrder.Entry> entries) {
        List<String> out = new ArrayList<>();
        for (NavOrder.Entry e : entries) out.add(e.id());
        return out;
    }

    @Test public void emptyRegistryResolvesToEmptyRatherThanThrowing() {
        assertTrue(NavOrder.resolve(Arrays.asList("clock"), Collections.<App>emptyList()).isEmpty());
        assertTrue(NavOrder.resolve(Arrays.asList("clock"), null).isEmpty());
    }

    @Test public void withNoStoredOrderAppsFollowTheirDefaultOrder() {
        List<App> registered = Arrays.asList(
                new App("expenses", 30, "Expenses"),
                new App("clock", 10, "TicTic"),
                new App("notes", 20, "Notes"));
        assertEquals(Arrays.asList("clock", "notes", "expenses"),
                ids(new ArrayList<NavOrder.Entry>(NavOrder.resolve(null, registered))));
    }

    @Test public void storedArrangementWinsOverDefaultOrder() {
        List<App> registered = Arrays.asList(
                new App("clock", 10, "TicTic"),
                new App("notes", 20, "Notes"),
                new App("expenses", 30, "Expenses"));
        // The user dragged Expenses to the front.
        assertEquals(Arrays.asList("expenses", "clock", "notes"),
                ids(new ArrayList<NavOrder.Entry>(
                        NavOrder.resolve(NavOrder.parse("expenses,clock,notes"), registered))));
    }

    @Test public void appAddedSinceTheLastArrangementIsAppendedByDefaultOrder() {
        List<App> registered = Arrays.asList(
                new App("clock", 10, "TicTic"),
                new App("notes", 20, "Notes"),
                new App("calculator", 5, "Calculator"));
        // Stored order predates the calculator, which has the lowest defaultOrder.
        // It must appear, but must not disturb the arrangement the user already made.
        assertEquals(Arrays.asList("notes", "clock", "calculator"),
                ids(new ArrayList<NavOrder.Entry>(
                        NavOrder.resolve(NavOrder.parse("notes,clock"), registered))));
    }

    @Test public void unregisteredStoredIdIsDroppedButNotForgotten() {
        List<App> withGone = Arrays.asList(
                new App("clock", 10, "TicTic"),
                new App("notes", 20, "Notes"));
        // "scanner" is absent from this build — a lite flavour, or not yet downloaded.
        List<NavOrder.Entry> resolved = new ArrayList<NavOrder.Entry>(
                NavOrder.resolve(NavOrder.parse("scanner,clock,notes"), withGone));
        assertEquals(Arrays.asList("clock", "notes"), ids(resolved));

        // Because the filtered list is never persisted, the same stored string still
        // holds the position. When the scanner is present again it returns to the front
        // rather than being appended as though newly installed.
        List<App> withScanner = Arrays.asList(
                new App("scanner", 40, "Scanner"),
                new App("clock", 10, "TicTic"),
                new App("notes", 20, "Notes"));
        assertEquals(Arrays.asList("scanner", "clock", "notes"),
                ids(new ArrayList<NavOrder.Entry>(
                        NavOrder.resolve(NavOrder.parse("scanner,clock,notes"), withScanner))));
    }

    @Test public void tiesOnDefaultOrderBreakDeterministicallyByLabel() {
        List<App> registered = Arrays.asList(
                new App("zulu", 10, "Zulu"),
                new App("alpha", 10, "Alpha"));
        // Registration order puts zulu first; the label tie-break must override it, or
        // navigation could flicker between renders.
        assertEquals(Arrays.asList("alpha", "zulu"),
                ids(new ArrayList<NavOrder.Entry>(NavOrder.resolve(null, registered))));
    }

    @Test public void duplicateIdsCollapseInBothInputs() {
        List<App> registered = Arrays.asList(
                new App("clock", 10, "TicTic"),
                new App("clock", 10, "TicTic"),
                new App("notes", 20, "Notes"));
        assertEquals(Arrays.asList("clock", "notes"),
                ids(new ArrayList<NavOrder.Entry>(
                        NavOrder.resolve(NavOrder.parse("clock,clock,notes"), registered))));
    }

    @Test public void corruptEntriesDegradeInsteadOfThrowing() {
        List<App> registered = Arrays.asList(
                null,
                new App("clock", 10, "TicTic"),
                new App(null, 20, "No id"));
        // A null entry and a null id are skipped; the usable app still resolves.
        // This runs on every navigation render, so a bad preference must not take the
        // app down.
        assertEquals(Arrays.asList("clock"),
                ids(new ArrayList<NavOrder.Entry>(
                        NavOrder.resolve(Arrays.asList(null, "clock"), registered))));
    }

    @Test public void parseToleratesMessyStoredStrings() {
        assertEquals(Arrays.asList("clock", "notes"),
                new ArrayList<>(NavOrder.parse("clock,,notes,")));
        assertEquals(Arrays.asList("clock", "notes"),
                new ArrayList<>(NavOrder.parse(" clock , notes ")));
        assertTrue(NavOrder.parse("").isEmpty());
        assertTrue(NavOrder.parse(null).isEmpty());
        // De-duplicated, and order preserved because it represents the user's arrangement.
        assertEquals(Arrays.asList("clock", "notes"),
                new ArrayList<>(NavOrder.parse("clock,notes,clock")));
    }

    @Test public void joinSkipsNullsAndBlanks() {
        assertEquals("clock,notes", NavOrder.join(Arrays.asList("clock", null, "", "  ", "notes")));
        assertEquals("", NavOrder.join(null));
        assertEquals("", NavOrder.join(Collections.<String>emptyList()));
    }

    @Test public void joinAndParseRoundTrip() {
        List<String> original = Arrays.asList("clock", "notes", "expenses");
        assertEquals(original, new ArrayList<>(NavOrder.parse(NavOrder.join(original))));
    }
}
