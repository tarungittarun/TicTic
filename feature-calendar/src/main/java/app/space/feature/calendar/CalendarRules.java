package app.space.feature.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Month-grid and event-day arithmetic.
 *
 * <p>Pure {@code java.util.Calendar}, no Android, so it runs as a JVM test. That matters
 * more here than in most modules: a month grid is wrong in ways that look plausible. A
 * one-day shift puts every event on the wrong date, and the grid still renders as a neat
 * six rows.
 */
public final class CalendarRules {

    private CalendarRules() {}

    /** Six weeks of seven cells, which is what a month grid always shows. */
    public static final int GRID_CELLS = 42;

    /** One event. {@code when} is millis; {@code NO_TIME} means an all-day event. */
    public static final long NO_TIME = -1L;

    public static final class Event {
        public final String id;
        public final String title;
        public final String notes;
        public final long when;
        /** Minutes before the event to remind; -1 means no reminder. */
        public final int reminderMinutes;

        public Event(String id, String title, String notes, long when) {
            this(id, title, notes, when, -1);
        }

        public Event(String id, String title, String notes, long when, int reminderMinutes) {
            this.id = id == null ? "" : id;
            this.title = title == null || title.trim().isEmpty() ? "Untitled event" : title.trim();
            this.notes = notes == null ? "" : notes;
            this.when = when;
            this.reminderMinutes = reminderMinutes;
        }

        public boolean allDay() { return when == NO_TIME; }
    }

    public static int daysInMonth(int year, int monthZeroBased) {
        Calendar c = utc();
        c.clear();
        c.set(year, monthZeroBased, 1);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
    }

    /**
     * Cells before the 1st of the month, so the grid's first row lines up under the
     * correct weekday. Depends on the week's first day, which is a locale setting: a grid
     * built for a Monday week and read as a Sunday week is shifted by one for every event.
     */
    public static int leadingBlanks(int year, int monthZeroBased, int firstDayOfWeek) {
        Calendar c = utc();
        c.clear();
        c.setFirstDayOfWeek(firstDayOfWeek);
        c.set(year, monthZeroBased, 1);
        int dow = c.get(Calendar.DAY_OF_WEEK);
        return (dow - firstDayOfWeek + 7) % 7;
    }

    /**
     * The 42-cell grid: day-of-month numbers, with 0 for cells outside this month.
     *
     * <p>Always 42 cells so the grid never changes height between months, which would make
     * the whole screen jump when the user pages from February to March.
     */
    public static List<Integer> monthGrid(int year, int monthZeroBased, int firstDayOfWeek) {
        List<Integer> cells = new ArrayList<>(Collections.nCopies(GRID_CELLS, 0));
        int lead = leadingBlanks(year, monthZeroBased, firstDayOfWeek);
        int days = daysInMonth(year, monthZeroBased);
        for (int d = 1; d <= days; d++) {
            int index = lead + d - 1;
            if (index < GRID_CELLS) cells.set(index, d);
        }
        return cells;
    }

    /** Millis at the start of a day-of-month, in the given time zone. */
    public static long dayStart(int year, int monthZeroBased, int day, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone == null ? TimeZone.getDefault() : zone);
        c.clear();
        c.set(year, monthZeroBased, day, 0, 0, 0);
        return c.getTimeInMillis();
    }

    /** Half-open day range, so an event at midnight belongs to one day, not two. */
    public static List<Event> onDay(List<Event> events, long fromInclusive, long toExclusive) {
        List<Event> out = new ArrayList<>();
        if (events == null) return out;
        for (Event e : events) {
            if (e == null) continue;
            if (e.allDay()) continue;
            if (e.when >= fromInclusive && e.when < toExclusive) out.add(e);
        }
        Collections.sort(out, new Comparator<Event>() {
            @Override public int compare(Event a, Event b) {
                if (a.when != b.when) return a.when < b.when ? -1 : 1;
                return a.id.compareTo(b.id);
            }
        });
        return out;
    }

    /** Events in a whole month, earliest first. */
    public static List<Event> inMonth(List<Event> events, long monthStart, long nextMonthStart) {
        return onDay(events, monthStart, nextMonthStart);
    }

    /** Whether an event falls today, which is what the grid marks with a dot. */
    public static boolean isToday(long when, long todayStart, long tomorrowStart) {
        return when != NO_TIME && when >= todayStart && when < tomorrowStart;
    }

    /** The month's name alone; the year travels separately so the header can make it a button. */
    public static String monthName(int year, int monthZeroBased) {
        Calendar c = utc();
        c.clear();
        c.set(year, monthZeroBased, 1);
        return c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
    }

    /**
     * Midnight of the first day of the week holding the given date. The week view is seven
     * columns from here; getting the offset wrong shifts every column by a day, which is the
     * whole view.
     */
    public static long startOfWeek(int year, int monthZeroBased, int day,
                                   int firstDayOfWeek, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone == null ? TimeZone.getDefault() : zone);
        c.clear();
        c.set(year, monthZeroBased, day, 0, 0, 0);
        int offset = (c.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7;
        c.add(Calendar.DAY_OF_YEAR, -offset);
        return c.getTimeInMillis();
    }

    /** The day-of-month a timestamp falls on, in the zone the user lives in. */
    public static int dayOfMonth(long millis, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone == null ? TimeZone.getDefault() : zone);
        c.setTimeInMillis(millis);
        return c.get(Calendar.DAY_OF_MONTH);
    }

    /** "14:05" for a day view row; the locale's 24/12 choice is left to the platform format. */
    public static String timeLabel(long millis, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone == null ? TimeZone.getDefault() : zone);
        c.setTimeInMillis(millis);
        return String.format(Locale.ROOT, "%02d:%02d",
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    public static String monthLabel(int year, int monthZeroBased) {
        Calendar c = utc();
        c.clear();
        c.set(year, monthZeroBased, 1);
        return c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " " + year;
    }

    /** Short weekday headers for the grid, starting on the user's first day of week. */
    public static List<String> weekdayHeaders(int firstDayOfWeek) {
        Calendar c = utc();
        c.clear();
        c.setFirstDayOfWeek(firstDayOfWeek);
        c.set(Calendar.DAY_OF_WEEK, firstDayOfWeek);
        List<String> out = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            out.add(c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()));
            c.add(Calendar.DAY_OF_WEEK, 1);
        }
        return out;
    }

    private static Calendar utc() {
        // Fixed zone for the pure maths: the grid depends only on the date, and letting the
        // default zone in here would make the leading-blank count differ between a JVM test
        // and a device in another hemisphere.
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }
}
