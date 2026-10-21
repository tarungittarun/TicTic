package app.space.feature.habits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HabitsRulesTest {

    private static Set<Long> days(long... d) {
        Set<Long> out = new HashSet<>();
        for (long v : d) out.add(v);
        return out;
    }

    @Test public void epochDayIsFloorNotTruncation() {
        // floorDiv matters for timestamps before 1970 and for negative offsets; integer
        // division would round the wrong way and put a completion on the following day.
        assertEquals(0L, HabitsRules.epochDay(0L));
        assertEquals(0L, HabitsRules.epochDay(HabitsRules.MILLIS_PER_DAY - 1));
        assertEquals(1L, HabitsRules.epochDay(HabitsRules.MILLIS_PER_DAY));
        assertEquals(-1L, HabitsRules.epochDay(-1L));
    }

    @Test public void streakCountsConsecutiveDaysEndingToday() {
        assertEquals(3, HabitsRules.currentStreak(days(10, 11, 12), 12));
        assertEquals(1, HabitsRules.currentStreak(days(12), 12));
    }

    @Test public void streakSurvivesUntilTheUserHasHadAChanceToCompleteToday() {
        // Counting only through today would reset every habit at midnight, which reads as
        // losing a streak the user has not actually missed.
        assertEquals(3, HabitsRules.currentStreak(days(10, 11, 12), 13));
    }

    @Test public void streakIsZeroOnceADayIsActuallyMissed() {
        assertEquals(0, HabitsRules.currentStreak(days(10, 11), 13));
        assertEquals(0, HabitsRules.currentStreak(days(10, 12), 12));
        assertEquals(0, HabitsRules.currentStreak(null, 12));
        assertEquals(0, HabitsRules.currentStreak(new HashSet<Long>(), 12));
    }

    @Test public void duplicatesDoNotInflateAStreak() {
        assertEquals(2, HabitsRules.currentStreak(days(11, 11, 12, 12), 12));
        assertEquals(2, HabitsRules.longestStreak(days(11, 11, 12, 12)));
    }

    @Test public void longestStreakIsIndependentOfToday() {
        assertEquals(3, HabitsRules.longestStreak(days(1, 2, 3, 9, 10)));
        assertEquals(2, HabitsRules.longestStreak(days(1, 2, 8, 10)));
        assertEquals(0, HabitsRules.longestStreak(null));
    }

    @Test public void theWindowCountsTodayAndExcludesOlderDays() {
        assertEquals(3, HabitsRules.completionsInWindow(days(6, 7, 10, 12), 12, 7));
        assertEquals(0, HabitsRules.completionsInWindow(days(1, 2), 12, 7));
        assertEquals(0, HabitsRules.completionsInWindow(days(12), 12, 0));
        // A future day must not count as done.
        assertEquals(0, HabitsRules.completionsInWindow(days(13), 12, 7));
    }

    @Test public void weeklyRateIsClampedToOne() {
        assertEquals(1f, HabitsRules.weeklyRate(days(6, 7, 8, 9, 10, 11, 12), 12), 1e-6f);
        assertEquals(0.5f, HabitsRules.weeklyRate(days(9, 10, 11, 12), 12), 1e-6f);
        assertEquals(0f, HabitsRules.weeklyRate(days(1), 12), 1e-6f);
    }

    @Test public void togglingAddsThenRemovesToday() {
        Set<Long> once = HabitsRules.toggled(days(), 12);
        assertTrue(HabitsRules.doneToday(once, 12));
        Set<Long> twice = HabitsRules.toggled(once, 12);
        assertFalse(HabitsRules.doneToday(twice, 12));
    }

    @Test public void togglingDoesNotMutateTheStoredSet() {
        // The screen re-reads from the store after every write, so a mutated input would
        // make the on-screen state and the persisted state disagree.
        Set<Long> stored = days(1, 2);
        HabitsRules.toggled(stored, 12);
        assertEquals(2, stored.size());
        assertFalse(stored.contains(12L));
    }

    @Test public void togglingHandlesNullAsEmpty() {
        assertTrue(HabitsRules.doneToday(HabitsRules.toggled(null, 5), 5));
    }

    @Test public void unsortedInputStillProducesTheRightStreak() {
        assertEquals(4, HabitsRules.currentStreak(days(12, 9, 11, 10), 12));
        assertEquals(4, HabitsRules.longestStreak(new HashSet<>(Arrays.asList(12L, 9L, 11L, 10L))));
    }
}
