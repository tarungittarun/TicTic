package app.space.feature.files;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Size formatting, kind classification, ordering and path safety for the file browser.
 *
 * <p>Pure, so it is tested. The ordering rule matters more than it looks: folders must come
 * before files, and a naive alphabetical sort interleaves them, which makes a directory
 * impossible to scan.
 */
public final class FileRules {

    private FileRules() {}

    /** One row in the browser. */
    public static final class Entry {
        public final String name;
        public final boolean directory;
        public final long bytes;
        public final long modifiedAt;

        public Entry(String name, boolean directory, long bytes, long modifiedAt) {
            this.name = name == null ? "" : name;
            this.directory = directory;
            this.bytes = directory ? 0 : Math.max(0, bytes);
            this.modifiedAt = modifiedAt;
        }

        public String extension() { return extensionOf(name); }
    }

    /** Folders first, then case-insensitive by name, then by full name for stable ties. */
    public static List<Entry> sorted(List<Entry> entries) {
        List<Entry> out = new ArrayList<>(entries == null ? Collections.<Entry>emptyList() : entries);
        Collections.sort(out, new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) {
                if (a.directory != b.directory) return a.directory ? -1 : 1;
                int byName = a.name.compareToIgnoreCase(b.name);
                return byName != 0 ? byName : a.name.compareTo(b.name);
            }
        });
        return out;
    }

    public static String formatBytes(long bytes) {
        if (bytes < 0) return "";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.ROOT, "%.0f KB", bytes / 1024f);
        if (bytes < 1024L * 1024L * 1024L) return String.format(Locale.ROOT, "%.1f MB", bytes / (1024f * 1024f));
        return String.format(Locale.ROOT, "%.2f GB", bytes / (1024f * 1024f * 1024f));
    }

    /** Lower-case extension without the dot; "" when there is none. */
    public static String extensionOf(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** A short kind label, which is what the row shows instead of a raw extension. */
    public static String kindOf(String name) {
        String ext = extensionOf(name);
        if (ext.isEmpty()) return "File";
        if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif")
                || ext.equals("webp") || ext.equals("bmp")) return "Image";
        if (ext.equals("m4a") || ext.equals("mp3") || ext.equals("wav") || ext.equals("aac")
                || ext.equals("ogg") || ext.equals("flac")) return "Audio";
        if (ext.equals("mp4") || ext.equals("mkv") || ext.equals("webm") || ext.equals("avi")) return "Video";
        if (ext.equals("txt") || ext.equals("md") || ext.equals("log")) return "Text";
        if (ext.equals("csv") || ext.equals("tsv")) return "Sheet";
        if (ext.equals("json") || ext.equals("xml") || ext.equals("html")) return "Markup";
        if (ext.equals("pdf")) return "PDF";
        if (ext.equals("zip") || ext.equals("gz") || ext.equals("tar") || ext.equals("rar")) return "Archive";
        return ext.toUpperCase(Locale.ROOT);
    }

    /**
     * Rejects names that cannot be created or that would escape the folder.
     *
     * <p>{@code ..} and separators are the dangerous cases: a rename accepting them writes
     * outside app-private storage, which is not recoverable and not visible to the user.
     */
    /**
     * Defangs a display name that arrives from a document provider.
     *
     * <p>A SAF provider is another app: its COLUMN_DISPLAY_NAME is untrusted input on its
     * way to {@code new File(...)}, which is exactly the shape isSafeName guards renames
     * against. Separators become underscores, leading dots are stripped so {@code ..} can
     * never climb out of the destination folder, and the length is capped from the front so
     * the extension survives. Empty means "no usable name" and the caller must refuse.
     */
    public static String safeDocumentName(String display) {
        if (display == null) return "";
        String cleaned = display.replace('/', '_').replace('\\', '_').trim();
        while (cleaned.startsWith(".")) cleaned = cleaned.substring(1);
        if (cleaned.isEmpty()) return "";
        if (cleaned.length() > 120) cleaned = cleaned.substring(cleaned.length() - 120);
        return cleaned;
    }

    public static boolean isSafeName(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.equals(".") || trimmed.equals("..")) return false;
        if (trimmed.length() > 120) return false;
        return trimmed.indexOf('/') < 0 && trimmed.indexOf('\\') < 0 && trimmed.indexOf('\0') < 0
                && !trimmed.startsWith(".");
    }

    /** Joins a parent path and a child name, never producing a double separator. */
    public static String join(String parent, String child) {
        if (parent == null || parent.isEmpty()) return child == null ? "" : child;
        if (child == null || child.isEmpty()) return parent;
        return parent.endsWith("/") ? parent + child : parent + "/" + child;
    }

    /** The last segment of a path, for a breadcrumb. */
    public static String baseName(String path) {
        if (path == null || path.isEmpty()) return "";
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int slash = trimmed.lastIndexOf('/');
        return slash < 0 ? trimmed : trimmed.substring(slash + 1);
    }

    /** The segments of a relative path, for the breadcrumb trail. Empty for the root. */
    public static List<String> crumbs(String relativePath) {
        List<String> out = new ArrayList<>();
        if (relativePath == null || relativePath.isEmpty()) return out;
        for (String part : relativePath.split("/")) if (!part.isEmpty()) out.add(part);
        return out;
    }

    /** Whether a relative path stays inside the root. Guards every navigation step. */
    public static boolean isInside(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return true;
        if (relativePath.startsWith("/")) return false;
        for (String part : relativePath.split("/")) {
            if (part.equals("..")) return false;
        }
        return true;
    }

    /** The Samsung-style category a kind belongs to; the browser's top level is categories. */
    public static String categoryOf(String kind) {
        if (kind == null) return "Other";
        if (kind.equals("Image")) return "Images";
        if (kind.equals("Video")) return "Videos";
        if (kind.equals("Audio")) return "Audio";
        if (kind.equals("Sheet")) return "Sheets";
        if (kind.equals("Text") || kind.equals("Markup") || kind.equals("PDF")) return "Documents";
        return "Other";
    }

    /** The category of a file name, through its kind. */
    public static String categoryOfName(String name) {
        return categoryOf(kindOf(name));
    }

    public static final int SORT_NAME = 0;
    public static final int SORT_DATE = 1;
    public static final int SORT_SIZE = 2;

    /**
     * Orders by the chosen mode. Folders stay on top in every mode, including size: a manager
     * where a folder sinks between two large files cannot be scanned, and scanning is the job.
     */
    public static List<Entry> sortBy(List<Entry> entries, final int mode) {
        List<Entry> out = new ArrayList<>(entries == null ? Collections.<Entry>emptyList() : entries);
        Collections.sort(out, new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) {
                if (a.directory != b.directory) return a.directory ? -1 : 1;
                int byMode = 0;
                if (mode == SORT_DATE) byMode = Long.compare(b.modifiedAt, a.modifiedAt);
                else if (mode == SORT_SIZE) byMode = Long.compare(b.bytes, a.bytes);
                if (byMode == 0) byMode = a.name.compareToIgnoreCase(b.name);
                return byMode != 0 ? byMode : a.name.compareTo(b.name);
            }
        });
        return out;
    }

    /** Case-insensitive contains; an empty or blank query matches everything. */
    public static boolean matchesQuery(String name, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        if (name == null) return false;
        return name.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
    }

    /** Counts and total size for the header. */
    public static long[] summarise(List<Entry> entries) {
        long folders = 0;
        long files = 0;
        long bytes = 0;
        if (entries != null) {
            for (Entry e : entries) {
                if (e == null) continue;
                if (e.directory) folders++;
                else { files++; bytes += e.bytes; }
            }
        }
        return new long[]{folders, files, bytes};
    }
}
