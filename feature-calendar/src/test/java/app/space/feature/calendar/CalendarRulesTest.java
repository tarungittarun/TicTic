package app.space.feature.calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class CalendarRulesTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Test public void daysInMonthCoversEveryLengthIncludingLeapFebruary() {
        assertEquals(31, CalendarRules.daysInMonth(2026, Calendar.JANUARY));
        assertEquals(28, CalendarRules.daysInMonth(2026, Calendar.FEBRUARY));
        assertEquals(29, CalendarRules.daysInMonth(2024, Calendar.FEBRUARY));
        assertEquals(30, CalendarRules.daysInMonth(2026, Calendar.APRIL));
        assertEquals(31, CalendarRules.daysInMonth(2026, Calendar.DECEMBER));
    }

    @Test public void leapYearFollowsTheCenturyRule() {
        assertTrue(CalendarRules.isLeapYear(2024));
        assertFalse(CalendarRules.isLeapYear(2023));
        assertFalse(CalendarRules.isLeapYear(1900));
        assertTrue(CalendarRules.isLeapYear(2000));
    }

    @Test public void theGridIsAlwaysFortyTwoCellsSoTheScreenNeverChangesHeight() {
        for (int m = Calendar.JANUARY; m <= Calendar.DECEMBER; m++) {
            assertEquals(42, CalendarRules.monthGrid(2026, m, Calendar.SUNDAY).size());
            assertEquals(42, CalendarRules.monthGrid(2026, m, Calendar.MONDAY).size());
        }
    }

    @Test public void leadingBlanksShiftWithTheWeeksFirstDay() {
        // 1 Oct 2026 is a Thursday. In a Sunday week it sits in column 4; in a Monday week
        // column 3. Building the grid for one convention and rendering the other shifts
        // every date by a day, which is the bug this pins.
        assertEquals(4, CalendarRules.leadingBlanks(2026, Calendar.OCTOBER, Calendar.SUNDAY));
        assertEquals(3, CalendarRules.leadingBlanks(2026, Calendar.OCTOBER, Calendar.MONDAY));
    }

    @Test public void theGridPlacesTheFirstAndLastDayInTheRightCells() {
        List<Integer> grid = CalendarRules.monthGrid(2026, Calendar.OCTOBER, Calendar.SUNDAY);
        assertEquals(Integer.valueOf(1), grid.get(4));
        assertEquals(Integer.valueOf(31), grid.get(34));
        assertEquals(Integer.valueOf(0), grid.get(0));
        assertEquals(Integer.valueOf(0), grid.get(35));
    }

    @Test public void everyDayAppearsExactlyOnceAndNothingElseAppears() {
        for (int m = Calendar.JANUARY; m <= Calendar.DECEMBER; m++) {
            int days = CalendarRules.daysInMonth(2026, m);
            List<Integer> grid = CalendarRules.monthGrid(2026, m, Calendar.SUNDAY);
            int seen = 0;
            for (int d = 1; d <= days; d++) {
                assertEquals("day " + d + " in month " + m, 1, grid.indexOf(d) >= 0 ? 1 : 0);
                assertTrue(grid.contains(d));
                seen++;
            }
            assertEquals(days, seen);
            assertEquals(42 - days, grid.stream().filter(v -> v == 0).count());
        }
    }

    @Test public void februaryInALeapYearFillsTheGridWithoutOverflow() {
        List<Integer> grid = CalendarRules.monthGrid(2024, Calendar.FEBRUARY, Calendar.SUNDAY);
        assertTrue(grid.contains(29));
        assertFalse(grid.contains(30));
    }

    @Test public void dayStartIsLocalMidnightAndDaysAreHalfOpen() {
        long from = CalendarRules.dayStart(2026, Calendar.OCTOBER, 5, UTC);
        long to = CalendarRules.dayStart(2026, Calendar.OCTOBER, 6, UTC);
        assertEquals(86400000L, to - from);
        List<CalendarRules.Event> events = Arrays.asList(
                ev("a", from), ev("b", to - 1), ev("c", to), ev("d", from - 1));
        // An event exactly at the next midnight belongs to the next day, not both. That is
        // what the half-open range buys, and it is the easiest thing to get wrong here.
        List<CalendarRules.Event> out = CalendarRules.onDay(events, from, to);
        assertEquals(2, out.size());
        assertEquals("a", out.get(0).id);
        assertEquals("b", out.get(1).id);
    }

    @Test public void eventsOnADayAreOrderedEarliestFirstWithStableTies() {
        long base = CalendarRules.dayStart(2026, Calendar.OCTOBER, 5, UTC);
        List<CalendarRules.Event> out = CalendarRules.onDay(Arrays.asList(
                ev("z", base + 3600000L), ev("a", base), ev("y", base + 3600000L)), base, base + 86400000L);
        assertEquals("a", out.get(0).id);
        assertEquals("y", out.get(1).id);
        assertEquals("z", out.get(2).id);
    }

    @Test public void allDayEventsAreNotAttributedToADayRange() {
        long base = CalendarRules.dayStart(2026, Calendar.OCTOBER, 5, UTC);
        CalendarRules.Event allDay = new CalendarRules.Event("x", "t", "", CalendarRules.NO_TIME);
        assertTrue(allDay.allDay());
        assertTrue(CalendarRules.onDay(Arrays.asList(allDay), base, base + 86400000L).isEmpty());
    }

    @Test public void isTodayUsesTheHalfOpenRange() {
        long start = CalendarRules.dayStart(2026, Calendar.OCTOBER, 5, UTC);
        long end = start + 86400000L;
        assertTrue(CalendarRules.isToday(start, start, end));
        assertTrue(CalendarRules.isToday(end - 1, start, end));
        assertFalse(CalendarRules.isToday(end, start, end));
        assertFalse(CalendarRules.isToday(CalendarRules.NO_TIME, start, end));
    }

    @Test public void monthLabelNamesTheMonthAndYear() {
        String label = CalendarRules.monthLabel(2026, Calendar.OCTOBER);
        assertTrue(label, label.endsWith("2026"));
        assertTrue(label, label.toLowerCase(java.util.Locale.ROOT).contains("october"));
    }

    @Test public void weekdayHeadersStartOnTheRequestedDayAndCoverSevenDays() {
        List<String> sunday = CalendarRules.weekdayHeaders(Calendar.SUNDAY);
        assertEquals(7, sunday.size());
        assertTrue(sunday.get(0).toLowerCase(java.util.Locale.ROOT).startsWith("sun"));
        List<String> monday = CalendarRules.weekdayHeaders(Calendar.MONDAY);
        assertTrue(monday.get(0).toLowerCase(java.util.Locale.ROOT).startsWith("mon"));
    }

    @Test public void anUntitledEventGetsAPlaceholderRatherThanAnEmptyRow() {
        assertEquals("Untitled event", new CalendarRules.Event("1", "  ", "", 0L).title);
    }

    private static CalendarRules.Event ev(String id, long when) {
        return new CalendarRules.Event(id, id, "", when);
    }
}
