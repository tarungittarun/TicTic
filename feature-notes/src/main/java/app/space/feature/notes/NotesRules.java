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

    /** One note. A plain value object so the rules can be exercised on the JVM. */
    public static final class Note {
        public final String id;
        public final String title;
        public final String body;
        public final long updatedAt;

        public Note(String id, String title, String body, long updatedAt) {
            this.id = id == null ? "" : id;
            this.title = title == null ? "" : title;
            this.body = body == null ? "" : body;
            this.updatedAt = updatedAt;
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

    /** Caps stored notes so the list cannot grow without bound on a small device. */
    public static List<Note> capped(List<Note> notes, int max) {
        List<Note> all = sortedByNewest(notes);
        if (max <= 0 || all.size() <= max) return all;
        return new ArrayList<>(all.subList(0, max));
    }
}
