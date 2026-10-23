package app.space.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The sharing door's rules: URI shape, containment, mime lookup and rename hygiene. */
public class ShareRulesTest {

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
