package app.space.feature.files;

/**
 * The string arithmetic behind the text editor: searching, replacing and locating the caret.
 *
 * <p>Kept free of Android so it runs as a JVM test. An editor's find/replace is exactly the
 * kind of code that looks trivial and offends people when it is wrong -- an occurrence missed
 * at a wrap boundary, a replacement that re-scans its own output and loops on "a" to "aa" --
 * so the rules live here and the screen only wires them to keys.
 */
public final class EditorRules {

    private EditorRules() {
    }

    /** How many times {@code needle} occurs in {@code text}; an empty needle occurs nowhere. */
    public static int countOccurrences(String text, String needle) {
        if (text == null || needle == null || needle.isEmpty()) return 0;
        int count = 0;
        int from = 0;
        while (true) {
            int at = text.indexOf(needle, from);
            if (at < 0) return count;
            count++;
            from = at + needle.length(); // step past the match: replacements never re-scan themselves
        }
    }

    /**
     * Every occurrence replaced, literally -- no regular expressions, so replacing "." with "!"
     * does not destroy the file. Scans forward past each match, which is what makes "a" to "aa"
     * terminate instead of feeding on its own output.
     */
    public static String replaceAllLiteral(String text, String find, String replacement) {
        if (text == null || find == null || find.isEmpty()) return text;
        String safe = text;
        String with = replacement == null ? "" : replacement;
        StringBuilder out = new StringBuilder();
        int from = 0;
        while (true) {
            int at = safe.indexOf(find, from);
            if (at < 0) {
                out.append(safe, from, safe.length());
                return out.toString();
            }
            out.append(safe, from, at).append(with);
            from = at + find.length();
        }
    }

    /**
     * The next occurrence at or after {@code from}, wrapping to the start once the end is
     * reached, because a find that silently stops at the bottom of a long file reads as a
     * find that does not work.
     */
    public static int nextOccurrence(String text, String needle, int from) {
        if (text == null || needle == null || needle.isEmpty()) return -1;
        int at = text.indexOf(needle, Math.max(0, from));
        if (at >= 0) return at;
        return text.indexOf(needle);
    }

    /** The previous occurrence before {@code from}, wrapping to the end of the text. */
    public static int previousOccurrence(String text, String needle, int from) {
        if (text == null || needle == null || needle.isEmpty()) return -1;
        // from-1, not from: an occurrence starting exactly at `from` is not *before* it, and
        // a negative index is what makes the wrap case fall through to the search from the end.
        int at = text.lastIndexOf(needle, from - 1);
        if (at >= 0) return at;
        return text.lastIndexOf(needle);
    }

    /** One-based line number of a caret offset, clamped to the text's length. */
    public static int lineAt(String text, int caret) {
        if (text == null) return 1;
        int at = clamp(text, caret);
        int line = 1;
        for (int i = 0; i < at; i++) {
            if (text.charAt(i) == '\n') line++;
        }
        return line;
    }

    /** One-based column of a caret offset within its line. */
    public static int columnAt(String text, int caret) {
        if (text == null) return 1;
        int at = clamp(text, caret);
        int nl = text.lastIndexOf('\n', at - 1);
        return at - nl; // nl is -1 on the first line, which makes the column caret+1
    }

    /** The status-line half an editor shows: where the caret is. */
    public static String statusLabel(String text, int caret) {
        return "Ln " + lineAt(text, caret) + ", Col " + columnAt(text, caret);
    }

    /** Whether the text editor is the right tool for a name: text and markup, not media. */
    public static boolean isEditableName(String name) {
        String kind = FileRules.kindOf(name);
        return "Text".equals(kind) || "Markup".equals(kind) || "Sheet".equals(kind);
    }

    private static int clamp(String text, int caret) {
        if (caret < 0) return 0;
        return Math.min(caret, text.length());
    }
}
