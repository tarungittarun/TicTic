package app.space.feature.recorder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Duration formatting, file naming and recording-size arithmetic.
 *
 * <p>Pure so it is tested: a duration shown as 0:60 or a filename with a colon in it are
 * both small, silent, and only appear once the user has recorded something they wanted to
 * keep.
 */
public final class RecorderRules {

    private RecorderRules() {}

    /** 44.1 kHz mono 16-bit PCM in an M4A/AAC container, roughly. Used for the estimate. */
    public static final int BYTES_PER_SECOND = 16000;

    /** Longest recording the UI will start; an unbounded file fills storage silently. */
    public static final long MAX_MILLIS = 3600000L;

    /** mm:ss, or h:mm:ss once an hour is reached. Never 0:60. */
    public static String formatDuration(long millis) {
        if (millis < 0) millis = 0;
        long totalSeconds = millis / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    /** Whether the elapsed time should be shown to the second at all. */
    public static String formatRemaining(long millisLeft) {
        return formatDuration(Math.max(0L, millisLeft));
    }

    public static boolean isPastLimit(long elapsedMillis) {
        return elapsedMillis >= MAX_MILLIS;
    }

    /** Rough size for the on-screen estimate, in bytes. */
    public static long estimatedBytes(long millis) {
        return Math.max(0L, millis / 1000L) * BYTES_PER_SECOND;
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.0f KB", bytes / 1024f);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024f * 1024f));
    }

    /**
     * A filename from a timestamp and an optional label.
     *
     * <p>Colons, slashes and other characters a title might contain are stripped, because
     * they are illegal in a filename on some storage and silently truncate on others.
     */
    public static String fileName(long createdAt, String label) {
        String when = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT)
                .format(new java.util.Date(createdAt));
        String safe = sanitise(label);
        return safe.isEmpty() ? "recording-" + when + ".m4a" : "recording-" + when + "-" + safe + ".m4a";
    }

    /** Keeps letters, digits, dash and underscore; collapses runs of anything else. */
    public static String sanitise(String label) {
        if (label == null) return "";
        String trimmed = label.trim().replaceAll("[^A-Za-z0-9_-]+", "-")
                .replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        return trimmed.length() > 40 ? trimmed.substring(0, 40) : trimmed;
    }

    /** One recording on disk. */
    public static final class Recording {
        public final String fileName;
        public final String label;
        public final long createdAt;
        public final long durationMillis;
        public final long bytes;

        public Recording(String fileName, String label, long createdAt, long durationMillis, long bytes) {
            this.fileName = fileName == null ? "" : fileName;
            this.label = label == null ? "" : label;
            this.createdAt = createdAt;
            this.durationMillis = durationMillis;
            this.bytes = bytes;
        }

        public boolean isUsable() {
            // A file that never got a duration written, or is empty, is a failed recording.
            // Listing it would offer a play button that does nothing.
            return !fileName.isEmpty() && bytes > 0 && durationMillis > 0;
        }
    }

    /** Newest first, and unusable recordings dropped rather than shown as dead rows. */
    public static List<Recording> sorted(List<Recording> recordings) {
        List<Recording> out = new ArrayList<>();
        if (recordings != null) for (Recording r : recordings) if (r != null && r.isUsable()) out.add(r);
        Collections.sort(out, new Comparator<Recording>() {
            @Override public int compare(Recording a, Recording b) {
                if (a.createdAt != b.createdAt) return a.createdAt < b.createdAt ? 1 : -1;
                return a.fileName.compareTo(b.fileName);
            }
        });
        return out;
    }

    public static long totalBytes(List<Recording> recordings) {
        long sum = 0;
        if (recordings != null) for (Recording r : recordings) if (r != null) sum += Math.max(0, r.bytes);
        return sum;
    }
}
