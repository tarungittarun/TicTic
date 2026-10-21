package app.space.feature.clock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

/**
 * The alarm recurrence rule that decides when every alarm in MySpace fires.
 *
 * <p>Previously untestable and therefore untested: the only entry point took a
 * {@code JSONObject}, and {@code org.json} is stubbed as "not mocked" in Android unit
 * tests. Step 1d-4 split field extraction from the arithmetic so the rule could be
 * exercised on the JVM without a real JSON jar on the test classpath.
 *
 * <p>Assertions are deliberately property-based — "the result is strictly after {@code
 * from}", "its hour and minute match", "its weekday is one of the selected bits" — rather
 * than comparisons against a second copy of the arithmetic. Recomputing the expected
 * millis the same way the implementation does would make the test pass even if the rule
 * were wrong, and would break on every timezone and DST change. These hold in any
 * timezone because both the input instant and the assertions use the same default zone.
 */
public class AlarmRulesTest {

    /** An instant at the given local date and time, in the JVM's default timezone. */
    private static long at(int year, int month1to12, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month1to12 - 1, day, hour, minute, 0);
        return c.getTimeInMillis();
    }

    private static Calendar cal(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c;
    }

    private static int field(long millis, int calendarField) {
        return cal(millis).get(calendarField);
    }

    /** Bit index used by both the rule and daysLabel: 0 = Sunday. */
    private static int bitFor(long millis) {
        return cal(millis).get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
    }

    // ---- one-off alarms (daysMask == 0) ----

    @Test
    public void oneOffLaterTodayFiresTodayAtTheSetTime() {
        long from = at(2026, 3, 10, 8, 0);
        // 09:30 is still ahead of 08:00, so this must fire today rather than roll over.
        long result = AlarmRules.nextOccurrence(9, 30, 0, 0, from);

        assertEquals(9, field(result, Calendar.HOUR_OF_DAY));
        assertEquals(30, field(result, Calendar.MINUTE));
        assertEquals(10, field(result, Calendar.DAY_OF_MONTH));
        assertTrue("must be strictly in the future", result > from);
    }

    @Test
    public void oneOffAlreadyPassedTodayRollsToTomorrowNotToday() {
        long from = at(2026, 3, 10, 9, 0);
        long result = AlarmRules.nextOccurrence(7, 30, 0, 0, from);

        assertEquals(11, field(result, Calendar.DAY_OF_MONTH));
        assertEquals(7, field(result, Calendar.HOUR_OF_DAY));
        assertTrue(result > from);
    }

    @Test
    public void oneOffExactlyNowRollsForwardBecauseTheComparisonIsNotStrict() {
        // The rule uses <=, so an alarm landing on the current instant must advance a day
        // rather than fire in the past. Getting this wrong means an alarm that never rings.
        long target = at(2026, 3, 10, 7, 30);
        long result = AlarmRules.nextOccurrence(7, 30, 0, 0, target);

        assertEquals(11, field(result, Calendar.DAY_OF_MONTH));
        assertTrue(result > target);
    }

    @Test
    public void oneOffWithAFuturePickedDateUsesThatDate() {
        long from = at(2026, 3, 10, 8, 0);
        long picked = at(2026, 6, 20, 0, 0);
        long result = AlarmRules.nextOccurrence(6, 15, 0, picked, from);

        assertEquals(Calendar.JUNE, field(result, Calendar.MONTH));
        assertEquals(20, field(result, Calendar.DAY_OF_MONTH));
        assertEquals(6, field(result, Calendar.HOUR_OF_DAY));
        assertEquals(15, field(result, Calendar.MINUTE));
    }

    @Test
    public void oneOffWithAPastPickedDateRollsOneDayPastIt() {
        long from = at(2026, 3, 10, 8, 0);
        long picked = at(2026, 1, 5, 0, 0);
        long result = AlarmRules.nextOccurrence(6, 15, 0, picked, from);

        // The saved date is in the past, so the candidate is too, and the rule adds a day.
        assertEquals(Calendar.JANUARY, field(result, Calendar.MONTH));
        assertEquals(6, field(result, Calendar.DAY_OF_MONTH));
        assertEquals(6, field(result, Calendar.HOUR_OF_DAY));
    }

    // ---- repeating alarms (daysMask != 0) ----

    @Test
    public void everyDayFiresAtTheNextMatchingTime() {
        long from = at(2026, 3, 10, 9, 0);
        long result = AlarmRules.nextOccurrence(7, 30, 127, 0, from);

        assertEquals(11, field(result, Calendar.DAY_OF_MONTH));
        assertEquals(7, field(result, Calendar.HOUR_OF_DAY));
        assertEquals(30, field(result, Calendar.MINUTE));
    }

    @Test
    public void repeatingTodayWithTimeStillAheadFiresToday() {
        long from = at(2026, 3, 10, 6, 0);
        int mask = 1 << bitFor(from);
        long result = AlarmRules.nextOccurrence(7, 30, mask, 0, from);

        assertEquals(10, field(result, Calendar.DAY_OF_MONTH));
        assertEquals(bitFor(from), bitFor(result));
    }

    @Test
    public void repeatingTodayWithTimeAlreadyPassedWaitsAWeekNotADay() {
        // The single most likely off-by-one in this rule: a weekly alarm whose time has
        // passed today must come round in seven days, not fire tomorrow on the wrong day.
        long from = at(2026, 3, 10, 9, 0);
        int mask = 1 << bitFor(from);
        long result = AlarmRules.nextOccurrence(7, 30, mask, 0, from);

        assertEquals("same weekday next week", bitFor(from), bitFor(result));
        assertEquals(17, field(result, Calendar.DAY_OF_MONTH));
    }

    @Test
    public void repeatingResultIsAlwaysOnASelectedWeekday() {
        long from = at(2026, 3, 10, 12, 0);
        int mask = (1 << Calendar.MONDAY - Calendar.SUNDAY) | (1 << Calendar.FRIDAY - Calendar.SUNDAY);
        long result = AlarmRules.nextOccurrence(8, 0, mask, 0, from);

        int bit = bitFor(result);
        assertTrue("result weekday must be one of the selected bits", (mask & (1 << bit)) != 0);
        assertTrue(result > from);
    }

    @Test
    public void maskWithNoReachableWeekdayFallsBackToOneDay() {
        // Bit 7 corresponds to no weekday, so the eight-day search finds nothing. The
        // fallback must still return a future instant rather than 0 or a past time, or the
        // alarm would be scheduled in the past and fire immediately, forever.
        long from = at(2026, 3, 10, 12, 0);
        long result = AlarmRules.nextOccurrence(8, 0, 1 << 7, 0, from);

        assertEquals(from + 86400000L, result);
    }

    @Test
    public void secondsAndMillisAreAlwaysCleared() {
        // A non-zero seconds field would make the alarm fire late by up to a minute and
        // make the "strictly after from" comparison unstable.
        long from = at(2026, 3, 10, 9, 0) + 43_210L;
        long result = AlarmRules.nextOccurrence(7, 30, 0, 0, from);

        assertEquals(0, field(result, Calendar.SECOND));
        assertEquals(0, field(result, Calendar.MILLISECOND));
    }

    // ---- daysLabel ----

    @Test
    public void daysLabelHandlesTheSpecialCases() {
        assertEquals("Once", AlarmRules.daysLabel(0));
        assertEquals("Every day", AlarmRules.daysLabel(127));
    }

    @Test
    public void daysLabelJoinsSelectedInitialsWithAMiddleDot() {
        // Bit 0 = Sunday, bit 1 = Monday, bit 3 = Wednesday.
        assertEquals("S · M · W", AlarmRules.daysLabel(0b0001011));
        assertEquals("M", AlarmRules.daysLabel(0b0000010));
        assertEquals("S", AlarmRules.daysLabel(0b1000000));
    }
}
