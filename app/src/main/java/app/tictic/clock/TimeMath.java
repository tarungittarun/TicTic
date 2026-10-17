package app.tictic.clock;

import java.util.Locale;

/** Pure timebase calculations shared by the timer and stopwatch. */
final class TimeMath {
    private TimeMath() {}

    static long countdownRemaining(long pausedRemaining, long wallEnd, long elapsedEnd,
                                   long nowWall, long nowElapsed, int startBoot, int currentBoot) {
        if (wallEnd <= 0) return Math.max(0L, pausedRemaining);
        if (startBoot >= 0 && currentBoot >= 0 && startBoot == currentBoot && elapsedEnd > 0) {
            return Math.max(0L, elapsedEnd - nowElapsed);
        }
        return Math.max(0L, wallEnd - nowWall);
    }

    static long stopwatchElapsed(long savedElapsed, long startElapsed, long startWall,
                                 long nowElapsed, long nowWall, int startBoot, int currentBoot) {
        long base = Math.max(0L, savedElapsed);
        if (startBoot >= 0 && currentBoot >= 0 && startBoot == currentBoot && startElapsed > 0) {
            return base + Math.max(0L, nowElapsed - startElapsed);
        }
        if (startWall > 0) return base + Math.max(0L, nowWall - startWall);
        if (startElapsed > 0) return base + Math.max(0L, nowElapsed - startElapsed);
        return base;
    }

    static String formatHundredths(long millis) {
        long centiseconds = Math.max(0L, millis) / 10L;
        return String.format(Locale.ROOT, "%02d:%02d.%02d",
                centiseconds / 6000L, (centiseconds / 100L) % 60L, centiseconds % 100L);
    }

    static String formatCountdownHundredths(long millis) {
        long centiseconds=Math.max(0L,millis)/10L;long seconds=centiseconds/100L;long hundredths=centiseconds%100L;
        long hours=seconds/3600L,minutes=(seconds%3600L)/60L,remainingSeconds=seconds%60L;
        return hours>0?String.format(Locale.ROOT,"%02d:%02d:%02d.%02d",hours,minutes,remainingSeconds,hundredths):String.format(Locale.ROOT,"%02d:%02d.%02d",minutes,remainingSeconds,hundredths);
    }
}
