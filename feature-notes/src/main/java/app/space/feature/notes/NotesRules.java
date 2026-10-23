package app.space.feature.notes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Note ordering, search and preview. Pure Java, so it is really tested: there is no
 * emulator in this project, and anything needing one is effectively untested.
 */
public final class NotesRules {

    private NotesRules() {}

    /** How many card colours the picker offers; Google Notes' colour row, minus the excess. */
    public static final int COLOR_COUNT = 7;

    /** One note. A plain value object so the rules can be exercised on the JVM. */
    public static final class Note {
        public final String id;
        public final String title;
        public final String body;
        public final long updatedAt;
        public final int color;
        public final boolean pinned;
        public final boolean archived;

        public Note(String id, String title, String body, long updatedAt) {
            this(id, title, body, updatedAt, 0, false, false);
        }

        public Note(String id, String title, String body, long updatedAt,
                    int color, boolean pinned, boolean archived) {
            this.id = id == null ? "" : id;
            this.title = title == null ? "" : title;
            this.body = body == null ? "" : body;
            this.updatedAt = updatedAt;
            this.color = Math.max(0, Math.min(COLOR_COUNT - 1, color));
            this.pinned = pinned;
            this.archived = archived;
        }

        /** The text a list row searches and previews. */
        public String searchable() { return title + "\n" + body; }
    }

    /** Newest first. Ties break on id so the order is stable across re-renders. */
    public static List<Note> sortedByNewest(List<Note> notes) {
        List<Note> out = new ArrayList<>(notes == null ? Collections.<Note>emptyList() : notes);
        Collections.sort(out, new Comparator<Note>() {
            @Override public int compare(Note a, Note b) {
                if (a.updatedAt != b.updatedAt) return a.updatedAt < b.updatedAt ? 1 : -1;
                return a.id.compareTo(b.id);
            }
        });
        return out;
    }

    /**
     * Case-insensitive match across title and body. An empty query matches everything,
     * which is what makes the search box safe to leave blank rather than special-cased.
     */
    public static List<Note> filter(List<Note> notes, String query) {
        List<Note> all = sortedByNewest(notes);
        if (query == null || query.trim().isEmpty()) return all;
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Note> out = new ArrayList<>();
        for (Note n : all) {
            if (n.searchable().toLowerCase(Locale.ROOT).contains(q)) out.add(n);
        }
        return out;
    }

    /** One-line preview for a list row: newlines collapsed, then truncated with an ellipsis. */
    public static String preview(String body, int max) {
        if (body == null) return "";
        String flat = body.replaceAll("\\s+", " ").trim();
        if (max <= 0 || flat.length() <= max) return flat;
        return flat.substring(0, max - 1).trim() + "\u2026";
    }

    /** The title to show when the user left it blank. */
    public static String displayTitle(Note note) {
        if (note == null) return "";
        String t = note.title.trim();
        if (!t.isEmpty()) return t;
        String p = preview(note.body, 40);
        return p.isEmpty() ? "Untitled note" : p;
    }

    /**
     * The order a note wall shows: pinned notes first, newest inside each group. A pin that
     * does not reach the top of the wall is a pin in name only.
     */
    public static List<Note> sortedForDisplay(List<Note> notes) {
        List<Note> out = new ArrayList<>(notes == null ? Collections.<Note>emptyList() : notes);
        Collections.sort(out, new Comparator<Note>() {
            @Override public int compare(Note a, Note b) {
                if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
                if (a.updatedAt != b.updatedAt) return a.updatedAt < b.updatedAt ? 1 : -1;
                return a.id.compareTo(b.id);
            }
        });
        return out;
    }

    /** Everything not archived, which is what the main wall shows. */
    public static List<Note> active(List<Note> notes) {
        List<Note> out = new ArrayList<>();
        if (notes == null) return out;
        for (Note n : notes) if (!n.archived) out.add(n);
        return out;
    }

    /** The archive drawer: nothing is deleted by being filed. */
    public static List<Note> archived(List<Note> notes) {
        List<Note> out = new ArrayList<>();
        if (notes == null) return out;
        for (Note n : notes) if (n.archived) out.add(n);
        return out;
    }

    /** A checklist line is its own marker: "[ ] " open, "[x] " done. Plain text, so it survives
     *  export, search and every other thing that reads a note as text. */
    public static boolean isChecklistLine(String line) {
        return line != null && (line.startsWith("[ ] ") || line.startsWith("[x] ")
                || line.startsWith("[X] "));
    }

    /** Flips one line's box; lines that are not checklist lines come back untouched. */
    public static String toggleChecklistLine(String line) {
        if (line == null) return null;
        if (line.startsWith("[ ] ")) return "[x] " + line.substring(4);
        if (line.startsWith("[x] ") || line.startsWith("[X] ")) return "[ ] " + line.substring(4);
        return line;
    }

    /** Flips the checklist line at a zero-based line index, leaving every other line alone. */
    public static String toggleChecklistLineAt(String body, int lineIndex) {
        if (body == null) return null;
        String[] lines = body.split("\n", -1);
        if (lineIndex < 0 || lineIndex >= lines.length) return body;
        lines[lineIndex] = toggleChecklistLine(lines[lineIndex]);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) out.append('\n');
            out.append(lines[i]);
        }
        return out.toString();
    }

    /** The line index a caret offset sits on, for the editor's toggle-the-line-under-me action. */
    public static int lineIndexAt(String body, int caret) {
        if (body == null || caret <= 0) return 0;
        int at = Math.min(caret, body.length());
        int line = 0;
        for (int i = 0; i < at; i++) if (body.charAt(i) == '\n') line++;
        return line;
    }

    /** "3 of 5" for a card corner: done and total checklist lines. */
    public static int[] checklistTally(String body) {
        int done = 0;
        int total = 0;
        if (body == null) return new int[]{0, 0};
        for (String line : body.split("\n", -1)) {
            if (!isChecklistLine(line)) continue;
            total++;
            if (!line.startsWith("[ ] ")) done++;
        }
        return new int[]{done, total};
    }

    /** Caps stored notes so the list cannot grow without bound on a small device. */
    public static List<Note> capped(List<Note> notes, int max) {
        List<Note> all = sortedByNewest(notes);
        if (max <= 0 || all.size() <= max) return all;
        return new ArrayList<>(all.subList(0, max));
    }
}
