package app.space.feature.habits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Streak and rate arithmetic for habit tracking.
 *
 * <p>Pure and day-based rather than millis-based, which is the part worth testing: a
 * streak computed on raw timestamps breaks across daylight-saving changes and at midnight,
 * and it breaks silently -- the number is simply wrong.
 */
public final class HabitsRules {

    private HabitsRules() {}

    public static final long MILLIS_PER_DAY = 86400000L;

    /**
     * Epoch day for a timestamp. All streak maths happens in these units, so a completion
     * at 23:59 and one at 00:01 the next minute are correctly one day apart.
     */
    public static long epochDay(long millis) {
        return Math.floorDiv(millis, MILLIS_PER_DAY);
    }

    /**
     * The current streak: consecutive days ending today, or ending yesterday.
     *
     * <p>Ending yesterday is what keeps a streak alive before the user has had a chance to
     * complete it today. Counting only through today would reset every habit at midnight,
     * which reads as losing a streak the user has not actually missed.
     *
     * @param completionEpochDays days on which the habit was completed, any order,
     *                            duplicates ignored
     * @param todayEpochDay       today, in the same units
     */
    public static int currentStreak(Set<Long> completionEpochDays, long todayEpochDay) {
        if (completionEpochDays == null || completionEpochDays.isEmpty()) return 0;
        Set<Long> days = new HashSet<>(completionEpochDays);
        long cursor;
        if (days.contains(todayEpochDay)) {
            cursor = todayEpochDay;
        } else if (days.contains(todayEpochDay - 1)) {
            cursor = todayEpochDay - 1;
        } else {
            return 0;
        }
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor--;
        }
        return streak;
    }

    /** The longest run of consecutive days ever recorded, independent of today. */
    public static int longestStreak(Set<Long> completionEpochDays) {
        if (completionEpochDays == null || completionEpochDays.isEmpty()) return 0;
        List<Long> days = new ArrayList<>(completionEpochDays);
        Collections.sort(days);
        int best = 1;
        int run = 1;
        for (int i = 1; i < days.size(); i++) {
            long prev = days.get(i - 1);
            long cur = days.get(i);
            if (cur == prev) continue;              // duplicate day, already counted
            if (cur == prev + 1) { run++; } else { run = 1; }
            if (run > best) best = run;
        }
        return best;
    }

    /** Completions within the last {@code windowDays} days, today included. */
    public static int completionsInWindow(Set<Long> completionEpochDays, long todayEpochDay, int windowDays) {
        if (completionEpochDays == null || windowDays <= 0) return 0;
        int count = 0;
        for (Long d : completionEpochDays) {
            if (d == null) continue;
            if (d <= todayEpochDay && d > todayEpochDay - windowDays) count++;
        }
        return count;
    }

    /** Completions in the last week as a 0..1 rate, for a progress bar. */
    public static float weeklyRate(Set<Long> completionEpochDays, long todayEpochDay) {
        return Math.min(1f, completionsInWindow(completionEpochDays, todayEpochDay, 7) / 7f);
    }

    /** Whether the habit is already done today, which decides the toggle's direction. */
    public static boolean doneToday(Set<Long> completionEpochDays, long todayEpochDay) {
        return completionEpochDays != null && completionEpochDays.contains(todayEpochDay);
    }

    /** Adds or removes today, returning a new set so callers cannot mutate stored state. */
    public static Set<Long> toggled(Set<Long> completionEpochDays, long todayEpochDay) {
        Set<Long> out = new HashSet<>(completionEpochDays == null ? Collections.<Long>emptySet() : completionEpochDays);
        if (!out.remove(todayEpochDay)) out.add(todayEpochDay);
        return out;
    }
}
