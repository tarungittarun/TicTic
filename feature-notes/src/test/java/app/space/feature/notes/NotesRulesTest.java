package app.space.feature.notes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NotesRulesTest {

    private static NotesRules.Note note(String id, String title, String body, long updated) {
        return new NotesRules.Note(id, title, body, updated);
    }

    @Test public void sortsNewestFirstAndBreaksTiesStably() {
        List<NotesRules.Note> in = new ArrayList<>(Arrays.asList(
                note("b", "B", "", 100), note("a", "A", "", 100), note("c", "C", "", 300)));
        List<NotesRules.Note> out = NotesRules.sortedByNewest(in);
        assertEquals("c", out.get(0).id);
        // Same timestamp must order by id, or a re-render can reshuffle the list under the
        // user's finger.
        assertEquals("a", out.get(1).id);
        assertEquals("b", out.get(2).id);
    }

    @Test public void searchesTitleAndBodyCaseInsensitively() {
        List<NotesRules.Note> in = Arrays.asList(
                note("1", "Shopping", "Milk and EGGS", 1),
                note("2", "Work", "call the bank", 2));
        assertEquals(1, NotesRules.filter(in, "eggs").size());
        assertEquals(1, NotesRules.filter(in, "BANK").size());
        assertEquals(1, NotesRules.filter(in, "shopping").size());
    }

    @Test public void aBlankQueryMatchesEverything() {
        List<NotesRules.Note> in = Arrays.asList(note("1", "a", "", 1), note("2", "b", "", 2));
        assertEquals(2, NotesRules.filter(in, "").size());
        assertEquals(2, NotesRules.filter(in, null).size());
        assertEquals(2, NotesRules.filter(in, "   ").size());
    }

    @Test public void previewCollapsesNewlinesAndTruncatesWithAnEllipsis() {
        assertEquals("one two three", NotesRules.preview("one\ntwo\n\nthree", 40));
        String p = NotesRules.preview("abcdefghij", 6);
        assertEquals(6, p.length());
        assertTrue(p.endsWith("\u2026"));
        assertEquals("", NotesRules.preview(null, 10));
    }

    @Test public void anUntitledNoteFallsBackToItsBodyThenToAPlaceholder() {
        assertEquals("Shopping", NotesRules.displayTitle(note("1", "Shopping", "", 1)));
        assertEquals("buy milk", NotesRules.displayTitle(note("1", "  ", "buy milk", 1)));
        assertEquals("Untitled note", NotesRules.displayTitle(note("1", "", "", 1)));
    }

    @Test public void theCapKeepsTheNewest() {
        List<NotesRules.Note> in = new ArrayList<>();
        for (int i = 0; i < 10; i++) in.add(note("n" + i, "t", "", i));
        List<NotesRules.Note> out = NotesRules.capped(in, 3);
        assertEquals(3, out.size());
        assertEquals("n9", out.get(0).id);
        assertEquals(in.size(), 10);
    }

    @Test public void nullInputsAreHandledRatherThanThrown() {
        assertTrue(NotesRules.sortedByNewest(null).isEmpty());
        assertTrue(NotesRules.filter(null, "x").isEmpty());
    }
}
