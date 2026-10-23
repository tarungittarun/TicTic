package app.space.core.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class DocumentRulesTest {

    @Test public void aFieldContainingACommaIsQuoted() {
        // Without quoting, every column after it shifts and the corruption only shows up
        // when the file is opened in something else.
        assertEquals("\"a,b\",c", DocumentRules.encodeRow(Arrays.asList("a,b", "c")));
        assertEquals("a,b", DocumentRules.encodeRow(Arrays.asList("a", "b")));
    }

    @Test public void embeddedQuotesAreDoubledPerRfc4180() {
        assertEquals("\"say \"\"hi\"\"\"", DocumentRules.encodeRow(Arrays.asList("say \"hi\"")));
        assertEquals("\"line\nbreak\"", DocumentRules.encodeRow(Arrays.asList("line\nbreak")));
    }

    @Test public void encodingThenDecodingReturnsTheOriginalFields() {
        List<String> original = Arrays.asList("plain", "with,comma", "with \"quotes\"",
                "with\nnewline", "", "trailing space ");
        List<String> decoded = DocumentRules.decodeRow(DocumentRules.encodeRow(original));
        assertEquals(original, decoded);
    }

    @Test public void decodingHandlesQuotedSeparatorsAndEmptyTrailingFields() {
        assertEquals(Arrays.asList("a", "b,c", "d"), DocumentRules.decodeRow("a,\"b,c\",d"));
        assertEquals(Arrays.asList("a", "", "c"), DocumentRules.decodeRow("a,,c"));
        assertEquals(Arrays.asList("a", ""), DocumentRules.decodeRow("a,"));
        assertEquals(Arrays.asList(""), DocumentRules.decodeRow(""));
        assertTrue(DocumentRules.decodeRow(null).isEmpty());
    }

    @Test public void aWholeSheetRoundTrips() {
        List<List<String>> rows = Arrays.asList(
                Arrays.asList("Item", "Amount"),
                Arrays.asList("Rent, monthly", "12000"),
                Arrays.asList("Food", "350.50"));
        String text = DocumentRules.encodeSheet(rows);
        assertEquals(rows, DocumentRules.decodeSheet(text));
        // Blank lines are dropped so a trailing newline does not become an empty row.
        assertEquals(3, DocumentRules.decodeSheet(text + "\n\n").size());
    }

    @Test public void columnCountIsTheWidestRowAndRowsCanBeNormalisedToIt() {
        List<List<String>> ragged = Arrays.asList(
                Arrays.asList("a", "b", "c"), Arrays.asList("d"), Arrays.asList("e", "f"));
        assertEquals(3, DocumentRules.columnCount(ragged));
        assertEquals(0, DocumentRules.columnCount(null));
        // Padding a ragged sheet is what lets it render as a grid instead of a staircase.
        assertEquals(Arrays.asList("d", "", ""), DocumentRules.normaliseRow(Arrays.asList("d"), 3));
        assertEquals(Arrays.asList("a", "b"), DocumentRules.normaliseRow(Arrays.asList("a", "b", "c"), 2));
    }

    @Test public void numericDetectionDecidesAlignment() {
        assertTrue(DocumentRules.isNumeric("12"));
        assertTrue(DocumentRules.isNumeric(" 12.50 "));
        assertTrue(DocumentRules.isNumeric("-3"));
        assertFalse(DocumentRules.isNumeric("12,5"));
        assertFalse(DocumentRules.isNumeric("abc"));
        assertFalse(DocumentRules.isNumeric(""));
        assertFalse(DocumentRules.isNumeric(null));
    }

    @Test public void columnSumSkipsTheHeaderAndIgnoresNonNumericCells() {
        List<List<String>> rows = Arrays.asList(
                Arrays.asList("Item", "Amount"),
                Arrays.asList("Rent", "12000"),
                Arrays.asList("Food", "350.50"),
                Arrays.asList("Note", "not a number"),
                Arrays.asList("Short"));
        assertEquals(12350.50, DocumentRules.sumColumn(rows, 1, true), 1e-9);
        // Without skipping the header the label row would be silently ignored anyway, but
        // the flag must not change the numeric result here -- it guards the first row only.
        assertEquals(12350.50, DocumentRules.sumColumn(rows, 1, false), 1e-9);
        assertEquals(0, DocumentRules.sumColumn(rows, 9, true), 1e-9);
        assertEquals(0, DocumentRules.sumColumn(null, 0, true), 1e-9);
    }

    @Test public void textStatisticsCountWordsLinesAndCharacters() {
        int[] stats = DocumentRules.textStats("one two\nthree");
        assertEquals(3, stats[0]);
        assertEquals(2, stats[1]);
        assertEquals(13, stats[2]);
        assertEquals(0, DocumentRules.textStats(null)[0]);
        assertEquals(0, DocumentRules.textStats("   ")[0]);
        assertTrue(DocumentRules.statsLabel("one two").contains("2 words"));
    }

    @Test public void documentsSortNewestFirstWithStableTies() {
        List<DocumentRules.Document> out = DocumentRules.sortedByNewest(Arrays.asList(
                doc("b", 100), doc("a", 100), doc("c", 900)));
        assertEquals("c", out.get(0).id);
        assertEquals("a", out.get(1).id);
        assertEquals("b", out.get(2).id);
    }

    @Test public void searchCoversTitleAndBodyAndBlankMatchesEverything() {
        List<DocumentRules.Document> docs = Arrays.asList(
                new DocumentRules.Document("1", "Budget", "rent and food", null, 1),
                new DocumentRules.Document("2", "Diary", "slept well", null, 2));
        assertEquals(1, DocumentRules.filter(docs, "RENT").size());
        assertEquals(1, DocumentRules.filter(docs, "diary").size());
        assertEquals(2, DocumentRules.filter(docs, "").size());
        assertEquals(2, DocumentRules.filter(docs, null).size());
        assertEquals(0, DocumentRules.filter(docs, "nothing").size());
    }

    @Test public void anUntitledDocumentGetsAPlaceholderAndASheetIsDetected() {
        assertEquals("Untitled", new DocumentRules.Document("1", " ", "", null, 0).title);
        assertFalse(new DocumentRules.Document("1", "t", "body", null, 0).isSheet());
        assertTrue(new DocumentRules.Document("1", "t", "", Arrays.asList("a,b"), 0).isSheet());
    }

    private static DocumentRules.Document doc(String id, long updated) {
        return new DocumentRules.Document(id, id, "", null, updated);
    }
}
