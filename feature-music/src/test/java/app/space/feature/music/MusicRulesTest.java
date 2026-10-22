package app.space.feature.music;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class MusicRulesTest {

    private static MusicRules.Track t(String id, String title, String source, boolean remote) {
        return new MusicRules.Track(id, title, source, remote, remote ? 0 : 1000);
    }

    @Test public void librarySortsByTitleIgnoringCaseWithStableTies() {
        List<MusicRules.Track> out = MusicRules.sorted(Arrays.asList(
                t("c", "beta", "/m/beta.mp3", false),
                t("b", "Alpha", "/m/alpha.mp3", false),
                t("a", "alpha", "/m/alpha2.mp3", false)));
        assertEquals("a", out.get(0).id);
        assertEquals("b", out.get(1).id);
        assertEquals("c", out.get(2).id);
    }

    @Test public void theSameShuffleSeedAlwaysGivesTheSameOrder() {
        // An unseeded shuffle recomputed per render would move tracks under the user's
        // finger; this is what keeps the queue stable.
        List<MusicRules.Track> library = Arrays.asList(
                t("1", "a", "/1.mp3", false), t("2", "b", "/2.mp3", false),
                t("3", "c", "/3.mp3", false), t("4", "d", "/4.mp3", false));
        assertTrue(MusicRules.sameOrder(MusicRules.shuffled(library, 7L),
                MusicRules.shuffled(library, 7L)));
        assertFalse(MusicRules.sameOrder(MusicRules.shuffled(library, 7L),
                MusicRules.shuffled(library, 8L)));
        assertEquals(4, MusicRules.shuffled(library, 7L).size());
    }

    @Test public void repeatAllWrapsAndRepeatNoneStopsAtTheEnd() {
        // Getting this backwards either stops the player mid-list or loops forever when the
        // user asked it not to.
        assertEquals(1, MusicRules.nextIndex(0, 3, MusicRules.REPEAT_NONE));
        assertEquals(-1, MusicRules.nextIndex(2, 3, MusicRules.REPEAT_NONE));
        assertEquals(0, MusicRules.nextIndex(2, 3, MusicRules.REPEAT_ALL));
        assertEquals(2, MusicRules.nextIndex(2, 3, MusicRules.REPEAT_ONE));
        assertEquals(-1, MusicRules.nextIndex(0, 0, MusicRules.REPEAT_ALL));
    }

    @Test public void previousAlwaysWraps() {
        assertEquals(2, MusicRules.previousIndex(0, 3));
        assertEquals(0, MusicRules.previousIndex(1, 3));
        assertEquals(-1, MusicRules.previousIndex(0, 0));
    }

    @Test public void indexClampingHandlesAnEmptyAndAnOverlongQueue() {
        assertEquals(-1, MusicRules.clampIndex(0, 0));
        assertEquals(0, MusicRules.clampIndex(-5, 3));
        assertEquals(2, MusicRules.clampIndex(9, 3));
        assertEquals(1, MusicRules.clampIndex(1, 3));
    }

    @Test public void durationNeverRendersAsSixtySeconds() {
        assertEquals("0:00", MusicRules.formatDuration(0));
        assertEquals("0:59", MusicRules.formatDuration(59_999));
        assertEquals("1:00", MusicRules.formatDuration(60_000));
        assertEquals("1:00:00", MusicRules.formatDuration(3_600_000));
        assertEquals("0:00", MusicRules.formatDuration(-1));
    }

    @Test public void onlyHttpAndHttpsUrlsMayBeStreamed() {
        // The field is user input from a dialog. Accepting file: or content: would let a
        // pasted string read arbitrary local paths.
        assertTrue(MusicRules.isStreamableUrl("https://example.com/a.mp3"));
        assertTrue(MusicRules.isStreamableUrl("http://example.com/a.mp3"));
        assertFalse(MusicRules.isStreamableUrl("file:///data/local/x.mp3"));
        assertFalse(MusicRules.isStreamableUrl("content://media/x"));
        assertFalse(MusicRules.isStreamableUrl("https://"));
        assertFalse(MusicRules.isStreamableUrl("https://exa mple.com"));
        assertFalse(MusicRules.isStreamableUrl(null));
    }

    @Test public void playableExtensionsAreTheOnesTheFrameworkHandles() {
        assertTrue(MusicRules.isPlayableExtension("a.MP3"));
        assertTrue(MusicRules.isPlayableExtension("a.flac"));
        assertFalse(MusicRules.isPlayableExtension("a.txt"));
        assertFalse(MusicRules.isPlayableExtension("noextension"));
        assertFalse(MusicRules.isPlayableExtension(null));
    }

    @Test public void unplayableItemsAreDroppedFromTheQueue() {
        List<MusicRules.Track> out = MusicRules.playable(Arrays.asList(
                t("1", "good", "/m/a.mp3", false),
                t("2", "notes", "/m/a.txt", false),
                t("3", "stream", "https://x.com/a.mp3", true),
                t("4", "badstream", "file:///x", true),
                t("5", "nosource", "", false), null));
        assertEquals(2, out.size());
        assertEquals("1", out.get(0).id);
        assertEquals("3", out.get(1).id);
        assertEquals(0, MusicRules.playable(null).size());
    }

    @Test public void theRepeatButtonCyclesThroughAllThreeModes() {
        assertEquals(MusicRules.REPEAT_ONE, MusicRules.nextRepeatMode(MusicRules.REPEAT_NONE));
        assertEquals(MusicRules.REPEAT_ALL, MusicRules.nextRepeatMode(MusicRules.REPEAT_ONE));
        assertEquals(MusicRules.REPEAT_NONE, MusicRules.nextRepeatMode(MusicRules.REPEAT_ALL));
        assertEquals("Repeat all", MusicRules.repeatLabel(MusicRules.REPEAT_ALL));
        assertEquals("No repeat", MusicRules.repeatLabel(MusicRules.REPEAT_NONE));
    }

    @Test public void sizeFormattingCrossesEachBoundary() {
        assertEquals("512 B", MusicRules.formatBytes(512));
        assertEquals("1 KB", MusicRules.formatBytes(1024));
        assertEquals("1.0 MB", MusicRules.formatBytes(1024 * 1024));
    }

    @Test public void anUntitledTrackGetsAPlaceholderAndANegativeSizeIsClamped() {
        assertEquals("Untitled", t("1", "  ", "/a.mp3", false).title);
        assertEquals(0, new MusicRules.Track("1", "a", "/a.mp3", false, -99).bytes);
    }
}
