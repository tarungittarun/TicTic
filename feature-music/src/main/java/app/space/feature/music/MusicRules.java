package app.space.feature.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Queue arithmetic: ordering, shuffle, repeat and next/previous.
 *
 * <p>Pure so it is tested. Wrap-around at the ends of the queue is the part that gets wrong
 * in practice -- a player that stops dead on the last track when repeat is on looks broken
 * rather than merely unfinished.
 */
public final class MusicRules {

    private MusicRules() {}

    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_ONE = 1;
    public static final int REPEAT_ALL = 2;

    /** A playable item: a file this app owns, or a URL the user supplied. */
    public static final class Track {
        public final String id;
        public final String title;
        public final String source;
        public final boolean remote;
        public final long bytes;

        public Track(String id, String title, String source, boolean remote, long bytes) {
            this.id = id == null ? "" : id;
            this.title = title == null || title.trim().isEmpty() ? "Untitled" : title.trim();
            this.source = source == null ? "" : source;
            this.remote = remote;
            this.bytes = Math.max(0, bytes);
        }
    }

    /** Alphabetical by title, then id, so the list never reshuffles between renders. */
    public static List<Track> sorted(List<Track> tracks) {
        List<Track> out = new ArrayList<>(tracks == null ? Collections.<Track>emptyList() : tracks);
        Collections.sort(out, new Comparator<Track>() {
            @Override public int compare(Track a, Track b) {
                int byTitle = a.title.compareToIgnoreCase(b.title);
                return byTitle != 0 ? byTitle : a.id.compareTo(b.id);
            }
        });
        return out;
    }

    /**
     * A shuffled order for the given seed.
     *
     * <p>Seeded rather than random so the order is reproducible: an unseeded shuffle that
     * recomputed on every render would move tracks under the user's finger, and one that
     * persisted a list would drift from the library as tracks were added.
     */
    public static List<Track> shuffled(List<Track> tracks, long seed) {
        List<Track> out = sorted(tracks);
        Collections.shuffle(out, new Random(seed));
        return out;
    }

    /** Same seed always gives the same order, which is what makes the queue stable. */
    public static boolean sameOrder(List<Track> a, List<Track> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!a.get(i).id.equals(b.get(i).id)) return false;
        return true;
    }

    /**
     * The next index, honouring the repeat mode. Returns -1 when playback should stop.
     *
     * <p>{@code REPEAT_ALL} wraps from the last track to the first; {@code REPEAT_NONE}
     * returns -1 there. Getting this backwards either stops the player mid-list or loops
     * forever when the user asked it not to.
     */
    public static int nextIndex(int current, int size, int repeatMode) {
        if (size <= 0) return -1;
        if (repeatMode == REPEAT_ONE) return current;
        int next = current + 1;
        if (next < size) return next;
        return repeatMode == REPEAT_ALL ? 0 : -1;
    }

    /** Previous always wraps: skipping back from the first track is a normal expectation. */
    public static int previousIndex(int current, int size) {
        if (size <= 0) return -1;
        int previous = current - 1;
        return previous < 0 ? size - 1 : previous;
    }

    public static int clampIndex(int index, int size) {
        if (size <= 0) return -1;
        return index < 0 ? 0 : (index >= size ? size - 1 : index);
    }

    /** mm:ss, or h:mm:ss past an hour. */
    public static String formatDuration(long millis) {
        if (millis < 0) millis = 0;
        long total = millis / 1000L;
        long hours = total / 3600L;
        long minutes = (total % 3600L) / 60L;
        long seconds = total % 60L;
        if (hours > 0) return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    /**
     * Whether a user-supplied URL may be streamed.
     *
     * <p>Restricted to http and https. Accepting {@code file:} or {@code content:} here
     * would let a pasted string read arbitrary local paths, and the field is user input
     * from a dialog.
     */
    public static boolean isStreamableUrl(String url) {
        if (url == null) return false;
        String trimmed = url.trim().toLowerCase(Locale.ROOT);
        return (trimmed.startsWith("http://") || trimmed.startsWith("https://"))
                && trimmed.length() > "https://".length() && trimmed.indexOf(' ') < 0;
    }

    /** Whether an extension is one the framework player is expected to handle. */
    public static boolean isPlayableExtension(String name) {
        if (name == null) return false;
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ext.equals("mp3") || ext.equals("m4a") || ext.equals("aac")
                || ext.equals("wav") || ext.equals("ogg") || ext.equals("flac");
    }

    /**
     * Whether a source the user picked through the system audio picker can be played.
     *
     * <p>A picked file arrives as a {@code content://} URI with no filename, so there is no
     * extension to check. The picker already filtered to audio, which is a stronger signal
     * than a suffix: a file renamed to .mp3 that is not audio would pass the extension test
     * and fail to play, while a picked .opus with no suffix would be wrongly rejected.
     */
    public static boolean isPickedSource(String source) {
        return source != null && source.trim().startsWith("content://");
    }

    /** Tracks this app can actually play, dropping anything unplayable. */
    public static List<Track> playable(List<Track> tracks) {
        List<Track> out = new ArrayList<>();
        if (tracks == null) return out;
        for (Track t : tracks) {
            if (t == null || t.source.isEmpty()) continue;
            if (t.remote) {
                if (isStreamableUrl(t.source)) out.add(t);
            } else if (isPickedSource(t.source) || isPlayableExtension(t.source)) {
                out.add(t);
            }
        }
        return out;
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.ROOT, "%.0f KB", bytes / 1024f);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024f * 1024f));
    }

    public static String repeatLabel(int repeatMode) {
        switch (repeatMode) {
            case REPEAT_ONE: return "Repeat one";
            case REPEAT_ALL: return "Repeat all";
            default: return "No repeat";
        }
    }

    /** Cycles the mode, so one button steps through all three. */
    public static int nextRepeatMode(int repeatMode) {
        return (repeatMode + 1) % 3;
    }
}
