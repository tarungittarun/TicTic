package app.space.core.data;

import java.util.Locale;

/**
 * The pure half of export/import: how app data maps to zip entries and back, and which entries
 * an import may touch. The map is deliberately total -- every SharedPreferences XML and every
 * file under getFilesDir() -- because "a full data round-trip on a fresh install" is the
 * acceptance line, and a backup that quietly omits an app is a lie with good manners.
 */
public final class DataExportRules {

    private DataExportRules() { }

    /** Zip entries live under exactly two roots; anything else is refused on import. */
    public static final String PREFS_ROOT = "prefs/";
    public static final String FILES_ROOT = "files/";

    /** A backup this large is either a mistake or an attack; both are refused. */
    public static final long MAX_TOTAL_BYTES = 512L * 1024L * 1024L;
    public static final int MAX_ENTRIES = 20000;

    /** The entry name for one SharedPreferences XML file. */
    public static String prefsEntry(String fileName) {
        return PREFS_ROOT + stripDots(fileName);
    }

    /** The entry name for one file inside getFilesDir(), given its relative path. */
    public static String filesEntry(String relativePath) {
        return FILES_ROOT + stripDots(relativePath);
    }

    /**
     * Whether an import may write this entry. Refuses absolute paths, parent traversal and
     * anything outside the two roots -- the zip came from the owner, but it may have travelled
     * through anyone's hands on the way back.
     */
    public static boolean isAllowedEntry(String entry) {
        if (entry == null || entry.isEmpty()) return false;
        if (entry.startsWith("/") || entry.startsWith("\\")) return false;
        if (entry.contains("..")) return false;
        if (!entry.startsWith(PREFS_ROOT) && !entry.startsWith(FILES_ROOT)) return false;
        // "prefs/" alone names the root itself, not a file inside it.
        return entry.length() > (entry.startsWith(PREFS_ROOT) ? PREFS_ROOT.length() : FILES_ROOT.length());
    }

    /** The file name a prefs entry lands as inside shared_prefs/. Null for non-prefs entries. */
    public static String prefsFileName(String entry) {
        if (entry == null || !entry.startsWith(PREFS_ROOT)) return null;
        String name = stripDots(entry.substring(PREFS_ROOT.length()));
        return name.isEmpty() ? null : name;
    }

    /** The relative path a files entry lands at inside getFilesDir(). Null for non-files. */
    public static String filesRelativePath(String entry) {
        if (entry == null || !entry.startsWith(FILES_ROOT)) return null;
        String path = stripDots(entry.substring(FILES_ROOT.length()));
        return path.isEmpty() ? null : path;
    }

    /** Removes traversal segments and leading separators; a pure defence behind isAllowedEntry. */
    public static String stripDots(String path) {
        if (path == null) return "";
        StringBuilder out = new StringBuilder();
        for (String segment : path.replace('\\', '/').split("/")) {
            if (segment.isEmpty() || segment.equals(".")) continue;
            if (segment.equals("..")) continue;
            if (out.length() > 0) out.append('/');
            out.append(segment);
        }
        return out.toString();
    }

    /** The line the owner sees after an export: what went in, in plain numbers. */
    public static String summary(int prefsCount, int filesCount, long bytes) {
        return String.format(Locale.ROOT, "%d settings files, %d data files, %s",
                prefsCount, filesCount, formatBytes(bytes));
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.ROOT, "%.0f KB", bytes / 1024f);
        return String.format(Locale.ROOT, "%.1f MB", bytes / 1024f / 1024f);
    }
}
