package app.tictic.clock;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class TimeMathTest {
    @Test public void timerUsesElapsedRealtimeDuringSameBootEvenIfWallClockMoves() {
        long left = TimeMath.countdownRemaining(300_000L, 9_000_000L, 1_075_000L,
                1_500_000L, 1_000_000L, 42, 42);
        assertEquals(75_000L, left);
    }

    @Test public void timerFallsBackToWallDeadlineAfterReboot() {
        long left = TimeMath.countdownRemaining(0L, 8_000L, 900_000L,
                3_000L, 1_000L, 41, 42);
        assertEquals(5_000L, left);
    }

    @Test public void pausedTimerReturnsStoredRemainder() {
        assertEquals(12_345L, TimeMath.countdownRemaining(12_345L, 0L, 0L,
                50_000L, 40_000L, 42, 42));
    }

    @Test public void stopwatchUsesMonotonicClockDuringSameBoot() {
        long elapsed = TimeMath.stopwatchElapsed(4_000L, 1_000_000L, 500_000L,
                1_006_250L, 90_000_000L, 7, 7);
        assertEquals(10_250L, elapsed);
    }

    @Test public void stopwatchUsesWallClockAsBootRecoveryFallback() {
        long elapsed = TimeMath.stopwatchElapsed(2_000L, 1_000_000L, 50_000L,
                5_000L, 60_500L, 7, 8);
        assertEquals(12_500L, elapsed);
    }

    @Test public void hundredthsAreRenderedAndNegativeElapsedIsClamped() {
        assertEquals("00:01.23", TimeMath.formatHundredths(1_239L));
        assertEquals("01:00.00", TimeMath.formatHundredths(60_000L));
        assertEquals("00:00.00", TimeMath.formatHundredths(-1L));
    }

    @Test public void timerHundredthsUseCompactMinuteAndHourFormats() {
        assertEquals("00:59.95", TimeMath.formatCountdownHundredths(59_950L));
        assertEquals("01:02:03.04", TimeMath.formatCountdownHundredths(3_723_040L));
        assertEquals("00:00.00", TimeMath.formatCountdownHundredths(-1L));
    }
}
