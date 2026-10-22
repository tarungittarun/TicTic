package app.space.feature.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class FileRulesTest {

    private static FileRules.Entry dir(String name) {
        return new FileRules.Entry(name, true, 0, 0);
    }

    private static FileRules.Entry file(String name, long bytes) {
        return new FileRules.Entry(name, false, bytes, 0);
    }

    @Test public void foldersSortBeforeFilesAndThenAlphabeticallyIgnoringCase() {
        // Interleaving folders with files makes a directory impossible to scan, which is the
        // whole point of a browser.
        List<FileRules.Entry> out = FileRules.sorted(Arrays.asList(
                file("zebra.txt", 1), dir("Beta"), file("apple.txt", 1), dir("alpha")));
        assertEquals("alpha", out.get(0).name);
        assertEquals("Beta", out.get(1).name);
        assertEquals("apple.txt", out.get(2).name);
        assertEquals("zebra.txt", out.get(3).name);
    }

    @Test public void sizeFormattingCrossesEachBoundary() {
        assertEquals("0 B", FileRules.formatBytes(0));
        assertEquals("1023 B", FileRules.formatBytes(1023));
        assertEquals("1 KB", FileRules.formatBytes(1024));
        assertEquals("1.0 MB", FileRules.formatBytes(1024 * 1024));
        assertEquals("1.00 GB", FileRules.formatBytes(1024L * 1024L * 1024L));
        assertEquals("", FileRules.formatBytes(-1));
    }

    @Test public void extensionsAndKinds() {
        assertEquals("txt", FileRules.extensionOf("notes.TXT"));
        assertEquals("", FileRules.extensionOf("README"));
        assertEquals("", FileRules.extensionOf("trailing."));
        assertEquals("gz", FileRules.extensionOf("archive.tar.gz"));
        assertEquals("Image", FileRules.kindOf("photo.JPG"));
        assertEquals("Audio", FileRules.kindOf("voice.m4a"));
        assertEquals("Sheet", FileRules.kindOf("budget.csv"));
        assertEquals("Text", FileRules.kindOf("notes.md"));
        assertEquals("File", FileRules.kindOf("README"));
        // An unknown extension is shown upper-cased rather than as a blank row.
        assertEquals("XYZ", FileRules.kindOf("data.xyz"));
    }

    @Test public void namesThatCouldEscapeTheFolderAreRejected() {
        assertFalse(FileRules.isSafeName(".."));
        assertFalse(FileRules.isSafeName("."));
        assertFalse(FileRules.isSafeName("../etc/passwd"));
        assertFalse(FileRules.isSafeName("a/b"));
        assertFalse(FileRules.isSafeName("a\\b"));
        assertFalse(FileRules.isSafeName(".hidden"));
        assertFalse(FileRules.isSafeName(""));
        assertFalse(FileRules.isSafeName(null));
        assertFalse(FileRules.isSafeName("   "));
        assertTrue(FileRules.isSafeName("budget.csv"));
        assertTrue(FileRules.isSafeName("my file (1).txt"));
    }

    @Test public void joinNeverProducesADoubleSeparator() {
        assertEquals("a/b", FileRules.join("a", "b"));
        assertEquals("a/b", FileRules.join("a/", "b"));
        assertEquals("b", FileRules.join("", "b"));
        assertEquals("a", FileRules.join("a", ""));
    }

    @Test public void breadcrumbsReflectTheWalkedPath() {
        assertTrue(FileRules.crumbs("").isEmpty());
        assertEquals(Arrays.asList("a", "b"), FileRules.crumbs("a/b"));
        assertEquals(Arrays.asList("a", "b"), FileRules.crumbs("a//b"));
        assertEquals("b", FileRules.baseName("a/b"));
        assertEquals("b", FileRules.baseName("a/b/"));
        assertEquals("", FileRules.baseName(""));
    }

    @Test public void navigationOutsideTheRootIsRefused() {
        // Every navigation step is checked against this before a File is constructed, so a
        // name on disk containing a separator cannot walk out of app-private storage.
        assertTrue(FileRules.isInside(""));
        assertTrue(FileRules.isInside("a/b"));
        assertFalse(FileRules.isInside("../a"));
        assertFalse(FileRules.isInside("a/../../b"));
        assertFalse(FileRules.isInside("/absolute"));
    }

    @Test public void theSummarySeparatesFoldersFilesAndBytes() {
        long[] out = FileRules.summarise(Arrays.asList(dir("a"), file("b", 100), file("c", 50), null));
        assertEquals(1, out[0]);
        assertEquals(2, out[1]);
        assertEquals(150, out[2]);
        assertEquals(0, FileRules.summarise(null)[0]);
    }

    @Test public void aDirectoryNeverReportsABogusSize() {
        // File.length() on a directory returns an arbitrary small number; showing it as the
        // folder's size would be meaningless.
        assertEquals(0, dir("anything").bytes);
        assertEquals(0, new FileRules.Entry("x", true, 4096, 0).bytes);
    }
}
