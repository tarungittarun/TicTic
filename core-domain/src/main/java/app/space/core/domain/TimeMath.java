package app.space.core.domain;

import java.util.Locale;

/**
 * Pure timebase calculations shared by the timer and stopwatch.
 *
 * <p>Extracted from {@code app.tictic.clock} into {@code :core-domain} as the first
 * step of the MySpace modularisation (SUITE-PLAN.md §3.1, §6 Phase 1). Behaviour is
 * unchanged; the class and its methods became {@code public} because they are now
 * consumed across a module boundary.
 *
 * <p>This module deliberately has no Android imports, so these rules stay verifiable
 * on the JVM without an emulator.
 */
public final class TimeMath {
    private TimeMath() {}

    /**
     * Remaining time for a running or paused countdown. Prefers the monotonic
     * {@code elapsedRealtime} deadline while the boot count is unchanged, and falls
     * back to the wall-clock deadline after a reboot or a wall-clock change.
     */
    public static long countdownRemaining(long pausedRemaining, long wallEnd, long elapsedEnd,
                                          long nowWall, long nowElapsed, int startBoot, int currentBoot) {
        if (wallEnd <= 0) return Math.max(0L, pausedRemaining);
        if (startBoot >= 0 && currentBoot >= 0 && startBoot == currentBoot && elapsedEnd > 0) {
            return Math.max(0L, elapsedEnd - nowElapsed);
        }
        return Math.max(0L, wallEnd - nowWall);
    }

    /**
     * Elapsed time for a stopwatch. Accumulates onto the stored base using the
     * monotonic clock while the boot count is unchanged, with wall-clock and
     * stored-elapsed fallbacks for reboot recovery.
     */
    public static long stopwatchElapsed(long savedElapsed, long startElapsed, long startWall,
                                        long nowElapsed, long nowWall, int startBoot, int currentBoot) {
        long base = Math.max(0L, savedElapsed);
        if (startBoot >= 0 && currentBoot >= 0 && startBoot == currentBoot && startElapsed > 0) {
            return base + Math.max(0L, nowElapsed - startElapsed);
        }
        if (startWall > 0) return base + Math.max(0L, nowWall - startWall);
        if (startElapsed > 0) return base + Math.max(0L, nowElapsed - startElapsed);
        return base;
    }

    /** {@code mm:ss.cc}, clamped at zero. */
    public static String formatHundredths(long millis) {
        long centiseconds = Math.max(0L, millis) / 10L;
        return String.format(Locale.ROOT, "%02d:%02d.%02d",
                centiseconds / 6000L, (centiseconds / 100L) % 60L, centiseconds % 100L);
    }

    /** {@code mm:ss.cc}, promoted to {@code hh:mm:ss.cc} once an hour is reached. Clamped at zero. */
    public static String formatCountdownHundredths(long millis) {
        long centiseconds=Math.max(0L,millis)/10L;long seconds=centiseconds/100L;long hundredths=centiseconds%100L;
        long hours=seconds/3600L,minutes=(seconds%3600L)/60L,remainingSeconds=seconds%60L;
        return hours>0?String.format(Locale.ROOT,"%02d:%02d:%02d.%02d",hours,minutes,remainingSeconds,hundredths):String.format(Locale.ROOT,"%02d:%02d.%02d",minutes,remainingSeconds,hundredths);
    }
}
