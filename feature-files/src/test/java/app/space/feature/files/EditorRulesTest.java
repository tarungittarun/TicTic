package app.space.feature.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Find, replace and caret maths for the text editor. The cases below are the ones an editor
 * gets wrong in public: matches at the very end, wrapping from the bottom to the top, a
 * replacement that contains what it replaces, and a caret past the end of the text.
 */
public class EditorRulesTest {

    @Test public void countsOccurrencesWithoutRescanningMatches() {
        assertEquals(3, EditorRules.countOccurrences("aaa", "a"));
        assertEquals(2, EditorRules.countOccurrences("abab", "ab"));
        assertEquals(0, EditorRules.countOccurrences("abc", ""));
        assertEquals(0, EditorRules.countOccurrences("", "a"));
        // Overlaps are not counted twice: "aa" in "aaa" occurs at 0 and 1 only if you rescan
        // inside a match, which is how a replace-all loops forever.
        assertEquals(1, EditorRules.countOccurrences("aaa", "aa"));
    }

    @Test public void replacesLiterallyAndTerminates() {
        assertEquals("x-x-x", EditorRules.replaceAllLiteral("a-a-a", "a", "x"));
        // The replacement contains the search text; a regex or rescan implementation feeds on
        // its own output and never returns.
        assertEquals("aa aa", EditorRules.replaceAllLiteral("a a", "a", "aa"));
        // Literal, not regular expression: a dot is a dot.
        assertEquals("Xab", EditorRules.replaceAllLiteral(".ab", ".", "X"));
        assertEquals("gone", EditorRules.replaceAllLiteral("stay gone stay", "stay ", ""));
        assertEquals("same", EditorRules.replaceAllLiteral("same", "", "x"));
    }

    @Test public void findWrapsAtBothEnds() {
        String text = "one two one two";
        assertEquals(0, EditorRules.nextOccurrence(text, "one", 0));
        assertEquals(8, EditorRules.nextOccurrence(text, "one", 1));
        // Past the last match, find wraps to the first instead of giving up.
        assertEquals(0, EditorRules.nextOccurrence(text, "one", 12));
        assertEquals(8, EditorRules.previousOccurrence(text, "one", 16));
        assertEquals(0, EditorRules.previousOccurrence(text, "one", 8));
        // Before the first match, previous wraps to the last.
        assertEquals(8, EditorRules.previousOccurrence(text, "one", 0));
        assertEquals(-1, EditorRules.nextOccurrence(text, "absent", 0));
        assertEquals(-1, EditorRules.nextOccurrence(text, "", 0));
    }

    @Test public void caretLinesAndColumnsAreOneBased() {
        String text = "first\nsecond\nthird";
        assertEquals(1, EditorRules.lineAt(text, 0));
        assertEquals(1, EditorRules.columnAt(text, 0));
        assertEquals(2, EditorRules.lineAt(text, 6));
        assertEquals(1, EditorRules.columnAt(text, 6));
        assertEquals(2, EditorRules.columnAt(text, 9));
        assertEquals(3, EditorRules.lineAt(text, text.length()));
        // A caret beyond the end is clamped, not an exception: selections arrive from the
        // widget and can race an edit.
        assertEquals(3, EditorRules.lineAt(text, 999));
        assertEquals(6, EditorRules.columnAt(text, 999));
        assertEquals("Ln 2, Col 4", EditorRules.statusLabel(text, 9));
    }

    @Test public void theEditorOffersItselfForTextAndNotForMedia() {
        assertTrue(EditorRules.isEditableName("notes.txt"));
        assertTrue(EditorRules.isEditableName("page.md"));
        assertTrue(EditorRules.isEditableName("budget.csv"));
        assertFalse(EditorRules.isEditableName("song.m4a"));
        assertFalse(EditorRules.isEditableName("photo.png"));
        assertFalse(EditorRules.isEditableName("scan.pdf"));
    }
}
