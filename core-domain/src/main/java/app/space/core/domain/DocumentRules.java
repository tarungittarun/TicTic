package app.space.core.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * CSV encoding and decoding, and plain-text statistics.
 *
 * <p>Moved out of :feature-documents when the owner withdrew the Documents app and folded its
 * spreadsheet and document viewing into Files: the CSV contract outlived the app that first
 * carried it, and core-domain is where suite-wide rules live.
 *
 * <p>The CSV half is the part worth testing hard. Quoting is where a hand-rolled writer
 * silently corrupts data: a comma inside a field that is not quoted shifts every later
 * column, and the damage only shows up when the file is opened somewhere else.
 */
public final class DocumentRules {

    private DocumentRules() {}

    public static final String SEPARATOR = ",";

    /** One stored document. {@code csv} null-or-empty means a plain-text document. */
    public static final class Document {
        public final String id;
        public final String title;
        public final String body;
        public final List<String> csv;
        public final long updatedAt;

        public Document(String id, String title, String body, List<String> csv, long updatedAt) {
            this.id = id == null ? "" : id;
            this.title = title == null || title.trim().isEmpty() ? "Untitled" : title.trim();
            this.body = body == null ? "" : body;
            this.csv = csv == null ? Collections.<String>emptyList() : csv;
            this.updatedAt = updatedAt;
        }

        public boolean isSheet() { return !csv.isEmpty(); }
    }

    /**
     * Encodes one row as CSV. A field is quoted when it contains a comma, a quote, or a
     * line break; embedded quotes are doubled, which is what RFC 4180 requires and what
     * every spreadsheet reader expects.
     */
    public static String encodeRow(List<String> fields) {
        StringBuilder out = new StringBuilder();
        if (fields == null) return "";
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) out.append(SEPARATOR);
            out.append(encodeField(fields.get(i)));
        }
        return out.toString();
    }

    static String encodeField(String value) {
        if (value == null) return "";
        boolean needsQuotes = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        if (!needsQuotes) return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /**
     * Decodes one CSV row, honouring quotes and doubled quotes.
     *
     * <p>Splitting on commas would break the first time a cell contains one, which is the
     * common case as soon as anyone writes prose into a sheet.
     */
    public static List<String> decodeRow(String row) {
        List<String> out = new ArrayList<>();
        if (row == null) return out;
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < row.length() && row.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else inQuotes = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                out.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        out.add(field.toString());
        return out;
    }

    /** A whole sheet to text, one encoded row per line. */
    public static String encodeSheet(List<List<String>> rows) {
        StringBuilder out = new StringBuilder();
        if (rows == null) return "";
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) out.append('\n');
            out.append(encodeRow(rows.get(i)));
        }
        return out.toString();
    }

    /** Text to a whole sheet. Blank trailing lines are dropped. */
    public static List<List<String>> decodeSheet(String text) {
        List<List<String>> out = new ArrayList<>();
        if (text == null) return out;
        String[] lines = text.split("\n", -1);
        for (String line : lines) if (!line.trim().isEmpty()) out.add(decodeRow(line));
        return out;
    }

    /**
     * The widest row, which is the sheet's column count. Rows are padded to it on display
     * so a ragged sheet still renders as a grid rather than as a staircase.
     */
    public static int columnCount(List<List<String>> rows) {
        int max = 0;
        if (rows != null) for (List<String> r : rows) if (r != null && r.size() > max) max = r.size();
        return max;
    }

    /** A row padded or trimmed to exactly {@code columns} cells. */
    public static List<String> normaliseRow(List<String> row, int columns) {
        List<String> out = new ArrayList<>(Collections.nCopies(Math.max(0, columns), ""));
        if (row != null) for (int i = 0; i < row.size() && i < out.size(); i++) out.set(i, row.get(i));
        return out;
    }

    /** Whether a cell can be parsed as a number, which decides right-alignment. */
    public static boolean isNumeric(String value) {
        if (value == null) return false;
        String t = value.trim();
        if (t.isEmpty()) return false;
        try {
            Double.parseDouble(t);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Sums a column, skipping cells that are not numbers rather than failing the sheet. */
    public static double sumColumn(List<List<String>> rows, int column, boolean skipHeader) {
        double sum = 0;
        if (rows == null) return 0;
        for (int i = skipHeader ? 1 : 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row == null || column < 0 || column >= row.size()) continue;
            String cell = row.get(column);
            if (isNumeric(cell)) sum += Double.parseDouble(cell.trim());
        }
        return sum;
    }

    /** Word, line and character counts for a text document. */
    public static int[] textStats(String body) {
        if (body == null || body.trim().isEmpty()) return new int[]{0, 0, 0};
        String[] words = body.trim().split("\\s+");
        int lines = body.split("\n", -1).length;
        return new int[]{words.length, lines, body.length()};
    }

    public static String statsLabel(String body) {
        int[] s = textStats(body);
        return String.format(Locale.ROOT, "%d words \u00b7 %d lines \u00b7 %d characters", s[0], s[1], s[2]);
    }

    /** Newest first, ties on id so the list cannot reshuffle between renders. */
    public static List<Document> sortedByNewest(List<Document> documents) {
        List<Document> out = new ArrayList<>(documents == null ? Collections.<Document>emptyList() : documents);
        Collections.sort(out, new java.util.Comparator<Document>() {
            @Override public int compare(Document a, Document b) {
                if (a.updatedAt != b.updatedAt) return a.updatedAt < b.updatedAt ? 1 : -1;
                return a.id.compareTo(b.id);
            }
        });
        return out;
    }

    /** Case-insensitive title search; blank matches everything. */
    public static List<Document> filter(List<Document> documents, String query) {
        List<Document> all = sortedByNewest(documents);
        if (query == null || query.trim().isEmpty()) return all;
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Document> out = new ArrayList<>();
        for (Document d : all) {
            if (d.title.toLowerCase(Locale.ROOT).contains(q)
                    || d.body.toLowerCase(Locale.ROOT).contains(q)) out.add(d);
        }
        return out;
    }
}
