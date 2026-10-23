package app.space.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The map between app data and zip entries, and the refusals that keep import honest. */
public class DataExportRulesTest {

    @Test public void entriesLiveUnderExactlyTwoRoots() {
        assertEquals("prefs/myspace.xml", DataExportRules.prefsEntry("myspace.xml"));
        assertEquals("files/recordings/a.m4a", DataExportRules.filesEntry("recordings/a.m4a"));
        assertEquals("files/recordings/a.m4a", DataExportRules.filesEntry("/recordings/a.m4a"));
        // A traversal segment in a source path is stripped, not honoured; the rest stays.
        assertEquals("files/secret/a.m4a", DataExportRules.filesEntry("../secret/a.m4a"));
    }

    @Test public void importRefusesEverythingOutsideTheRoots() {
        assertTrue(DataExportRules.isAllowedEntry("prefs/myspace.xml"));
        assertTrue(DataExportRules.isAllowedEntry("files/recordings/a.m4a"));
        assertFalse(DataExportRules.isAllowedEntry("files/"));
        assertFalse(DataExportRules.isAllowedEntry("prefs/"));
        assertFalse(DataExportRules.isAllowedEntry("../shared_prefs/x.xml"));
        assertFalse(DataExportRules.isAllowedEntry("files/../../etc/passwd"));
        assertFalse(DataExportRules.isAllowedEntry("/files/absolute.xml"));
        assertFalse(DataExportRules.isAllowedEntry("etc/passwd"));
        assertFalse(DataExportRules.isAllowedEntry(""));
        assertFalse(DataExportRules.isAllowedEntry(null));
        // Zip-slip spelled with backslashes is refused too.
        assertFalse(DataExportRules.isAllowedEntry("files\\..\\..\\x"));
    }

    @Test public void targetsRoundTripThroughTheRoots() {
        assertEquals("myspace.xml", DataExportRules.prefsFileName("prefs/myspace.xml"));
        assertNull(DataExportRules.prefsFileName("files/a"));
        assertEquals("recordings/a.m4a", DataExportRules.filesRelativePath("files/recordings/a.m4a"));
        assertNull(DataExportRules.filesRelativePath("prefs/a.xml"));
        assertNull(DataExportRules.filesRelativePath("files/"));
    }

    @Test public void theSummaryIsPlainNumbers() {
        assertEquals("3 settings files, 12 data files, 1.5 MB",
                DataExportRules.summary(3, 12, 1572864L));
        assertEquals("0 settings files, 1 data files, 512 B",
                DataExportRules.summary(0, 1, 512L));
    }

    @Test public void stripDotsIsTheSecondLineOfDefence() {
        assertEquals("a/b", DataExportRules.stripDots("./a//b"));
        assertEquals("b", DataExportRules.stripDots("../a/../b"));
        assertEquals("", DataExportRules.stripDots(null));
        assertEquals("", DataExportRules.stripDots("../../"));
    }
}
