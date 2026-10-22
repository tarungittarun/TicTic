package app.space.feature.recorder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class RecorderRulesTest {

    @Test public void durationNeverRendersAsSixtySeconds() {
        assertEquals("0:00", RecorderRules.formatDuration(0));
        assertEquals("0:59", RecorderRules.formatDuration(59_999));
        assertEquals("1:00", RecorderRules.formatDuration(60_000));
        assertEquals("9:59", RecorderRules.formatDuration(599_000));
        assertEquals("10:00", RecorderRules.formatDuration(600_000));
        assertEquals("1:00:00", RecorderRules.formatDuration(3_600_000));
        assertEquals("1:02:03", RecorderRules.formatDuration(3_723_000));
    }

    @Test public void aNegativeElapsedCannotProduceAMinusingDuration() {
        assertEquals("0:00", RecorderRules.formatDuration(-5000));
    }

    @Test public void theOneHourLimitIsEnforced() {
        assertFalse(RecorderRules.isPastLimit(RecorderRules.MAX_MILLIS - 1));
        assertTrue(RecorderRules.isPastLimit(RecorderRules.MAX_MILLIS));
    }

    @Test public void filenamesContainNoCharacterThatIsIllegalOnStorage() {
        // A colon from the timestamp would be legal on some filesystems and silently
        // truncate on others, and the recording would be unfindable afterwards.
        String name = RecorderRules.fileName(1_700_000_000_000L, "Stand up: meeting/notes");
        assertTrue(name, name.endsWith(".m4a"));
        assertFalse(name, name.contains(":"));
        assertFalse(name, name.contains("/"));
        assertFalse(name, name.contains(" "));
        assertTrue(name, name.contains("Stand-up-meeting-notes"));
    }

    @Test public void anEmptyLabelStillProducesAUsableFilename() {
        String name = RecorderRules.fileName(1_700_000_000_000L, "");
        assertTrue(name, name.startsWith("recording-"));
        assertFalse(name, name.endsWith("-.m4a"));
    }

    @Test public void labelsAreSanitisedAndBounded() {
        // A space is not in the allowed set, so it becomes a separator -- it is not dropped.
        // Asserting "abc" here would have required sanitise to delete characters, and a
        // label that silently loses its spaces reads as a different label to the user.
        assertEquals("a-b-c", RecorderRules.sanitise("  a b c  "));
        assertEquals("a-b", RecorderRules.sanitise("a###b"));
        assertEquals("abc", RecorderRules.sanitise("--abc--"));
        assertEquals("", RecorderRules.sanitise(null));
        assertEquals("", RecorderRules.sanitise("!!!"));
        assertEquals(40, RecorderRules.sanitise(repeat("x", 200)).length());
    }

    @Test public void sizeFormattingCrossesEachBoundary() {
        assertEquals("0 B", RecorderRules.formatBytes(0));
        assertEquals("512 B", RecorderRules.formatBytes(512));
        assertEquals("1 KB", RecorderRules.formatBytes(1024));
        assertEquals("1.0 MB", RecorderRules.formatBytes(1024 * 1024));
    }

    @Test public void theSizeEstimateIsProportionalToDuration() {
        assertEquals(RecorderRules.BYTES_PER_SECOND, RecorderRules.estimatedBytes(1000));
        assertEquals(0, RecorderRules.estimatedBytes(500));
        assertEquals(0, RecorderRules.estimatedBytes(-1));
    }

    @Test public void interruptedRecordingsAreDroppedRatherThanListedAsDeadRows() {
        List<RecorderRules.Recording> out = RecorderRules.sorted(Arrays.asList(
                new RecorderRules.Recording("a.m4a", "", 300, 1000, 5000),
                new RecorderRules.Recording("b.m4a", "", 200, 0, 0),
                new RecorderRules.Recording("", "", 100, 1000, 1000)));
        assertEquals(1, out.size());
        assertEquals("a.m4a", out.get(0).fileName);
    }

    @Test public void recordingsSortNewestFirstWithStableTies() {
        List<RecorderRules.Recording> out = RecorderRules.sorted(Arrays.asList(
                new RecorderRules.Recording("z.m4a", "", 100, 1000, 1),
                new RecorderRules.Recording("a.m4a", "", 100, 1000, 1),
                new RecorderRules.Recording("m.m4a", "", 900, 1000, 1)));
        assertEquals("m.m4a", out.get(0).fileName);
        assertEquals("a.m4a", out.get(1).fileName);
        assertEquals("z.m4a", out.get(2).fileName);
    }

    @Test public void totalBytesIgnoresNullsAndNegatives() {
        assertEquals(6000, RecorderRules.totalBytes(Arrays.asList(
                new RecorderRules.Recording("a", "", 1, 1, 1000),
                new RecorderRules.Recording("b", "", 1, 1, 5000),
                new RecorderRules.Recording("c", "", 1, 1, -999), null)));
        assertEquals(0, RecorderRules.totalBytes(null));
    }

    private static String repeat(String s, int times) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < times; i++) out.append(s);
        return out.toString();
    }
}
