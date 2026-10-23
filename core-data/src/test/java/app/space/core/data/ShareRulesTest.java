package app.space.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The sharing door's rules: URI shape, containment, mime lookup and rename hygiene. */
public class ShareRulesTest {

    @Test public void uriShapeCarriesTheAbsolutePath() {
        assertEquals("content://app.space.one.myspace_share/data/user/0/app.space.one/files/x.m4a",
                ShareRules.uriString("app.space.one", "/data/user/0/app.space.one/files/x.m4a"));
        assertEquals("app.space.one.myspace_share", ShareRules.authority("app.space.one"));
        // Double slashes collapse: the path is rebuilt segment by segment.
        assertEquals("content://p.myspace_share/a/b", ShareRules.uriString("p", "//a//b"));
    }

    @Test public void containmentRefusesEverythingOutsideTheRoot() {
        String root = "/data/user/0/app.space.one";
        assertTrue(ShareRules.isInsideRoot(root, root + "/files/recordings/a.m4a"));
        assertTrue(ShareRules.isInsideRoot(root, root));
        assertFalse(ShareRules.isInsideRoot(root, "/data/user/0/other.app/files/a.m4a"));
        assertFalse(ShareRules.isInsideRoot(root, "/sdcard/Download/a.m4a"));
        assertFalse(ShareRules.isInsideRoot(root, ""));
        assertFalse(ShareRules.isInsideRoot(null, root + "/x"));
        // A sibling directory that merely starts with the root string is still outside.
        assertFalse(ShareRules.isInsideRoot(root, root + ".backup/x"));
    }

    @Test public void mimeLookupHasAFloorNotAHole() {
        assertEquals("audio/mp4", ShareRules.mimeFor("recording-x.m4a"));
        assertEquals("audio/mpeg", ShareRules.mimeFor("song.MP3"));
        assertEquals("application/pdf", ShareRules.mimeFor("scan.pdf"));
        assertEquals("image/jpeg", ShareRules.mimeFor("page.jpeg"));
        assertEquals("application/octet-stream", ShareRules.mimeFor("mystery.bin"));
        assertEquals("application/octet-stream", ShareRules.mimeFor(null));
    }

    @Test public void renamesKeepTheExtensionAndRefuseRubbish() {
        assertEquals("meeting.m4a", ShareRules.renamedFile("meeting", "recording-x.m4a"));
        // Typing the extension yourself must not double it.
        assertEquals("meeting.m4a", ShareRules.renamedFile("meeting.m4a", "recording-x.m4a"));
        assertEquals("meeting.m4a", ShareRules.renamedFile("  meeting  ", "recording-x.m4a"));
        // Path separators are defanged, not honoured.
        assertEquals("a-b.m4a", ShareRules.renamedFile("a/b", "recording-x.m4a"));
        assertNull(ShareRules.renamedFile("   ", "recording-x.m4a"));
        assertNull(ShareRules.renamedFile(null, "recording-x.m4a"));
        // A leading dot would make the file hidden and the label empty.
        assertEquals("hidden.m4a", ShareRules.renamedFile(".hidden", "recording-x.m4a"));
    }

    @Test public void sanitiseDropsWhatCannotTravelInAFileName() {
        assertEquals("abc-123 _.", ShareRules.sanitise("abc-123 _."));
        assertEquals("clean", ShareRules.sanitise("c*l?e|a\"n"));
        assertEquals("", ShareRules.sanitise(null));
    }
}
