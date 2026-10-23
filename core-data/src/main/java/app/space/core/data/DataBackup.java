package app.space.core.data;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * The framework half of export/import: a zip of every SharedPreferences XML and every file
 * under getFilesDir(), and a restore that refuses anything the rules refuse.
 *
 * <p>Exports land in app-specific external storage (no permission needed, survives cache
 * clears, reachable over USB), and the FileProvider paths table covers that directory so the
 * zip can be shared straight out of the toast.
 *
 * <p>Import overwrites in place and the caller restarts the process: SharedPreferences are
 * cached in memory, and a restore that did not restart would be overwritten by the stale
 * cache on the next apply().
 */
public final class DataBackup {

    private DataBackup() { }

    /** Where export zips live; the owner can also reach it over USB. */
    public static File exportsFolder(Context context) {
        File base = context.getExternalFilesDir(null);
        if (base == null) base = context.getFilesDir(); // rare, but an export must not fail here
        File dir = new File(base, "exports");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File sharedPrefsFolder(Context context) {
        return new File(context.getFilesDir().getParentFile(), "shared_prefs");
    }

    /** Writes the backup zip; returns it, or null with nothing half-written. */
    public static File exportZip(Context context) {
        File output = new File(exportsFolder(context),
                "myspace-" + System.currentTimeMillis() + ".zip");
        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(new FileOutputStream(output));
            File prefs = sharedPrefsFolder(context);
            File[] prefFiles = prefs.listFiles();
            if (prefFiles != null) {
                for (File f : prefFiles) {
                    if (f.isFile()) putFile(zip, DataExportRules.prefsEntry(f.getName()), f);
                }
            }
            addTree(zip, context.getFilesDir(), context.getFilesDir());
            zip.close();
            return output;
        } catch (Exception e) {
            closeQuietly(zip);
            if (output.exists()) output.delete();
            return null;
        }
    }

    private static void addTree(ZipOutputStream zip, File root, File dir) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                addTree(zip, root, child);
            } else if (child.isFile()) {
                String relative = root.toURI().relativize(child.toURI()).getPath();
                putFile(zip, DataExportRules.filesEntry(relative), child);
            }
        }
    }

    private static void putFile(ZipOutputStream zip, String entryName, File file) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        FileInputStream in = new FileInputStream(file);
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) zip.write(buffer, 0, read);
        } finally {
            in.close();
        }
        zip.closeEntry();
    }

    /** The result of an import, in numbers the owner can check against what they exported. */
    public static final class Result {
        public final int prefsCount;
        public final int filesCount;
        public final long bytes;
        public final int refused;
        public final String failure;

        Result(int prefsCount, int filesCount, long bytes, int refused, String failure) {
            this.prefsCount = prefsCount;
            this.filesCount = filesCount;
            this.bytes = bytes;
            this.refused = refused;
            this.failure = failure;
        }

        public boolean ok() { return failure == null; }
    }

    /** Reads a backup zip and overwrites prefs and files. Never writes outside the two roots. */
    public static Result importZip(Context context, InputStream source) {
        int prefsCount = 0, filesCount = 0, refused = 0;
        long bytes = 0;
        ZipInputStream zip = null;
        try {
            zip = new ZipInputStream(source);
            ZipEntry entry;
            int entries = 0;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > DataExportRules.MAX_ENTRIES) {
                    return new Result(prefsCount, filesCount, bytes, refused, "too many entries");
                }
                String name = entry.getName();
                if (entry.isDirectory() || !DataExportRules.isAllowedEntry(name)) {
                    if (!entry.isDirectory()) refused++;
                    continue;
                }
                File target;
                String prefsName = DataExportRules.prefsFileName(name);
                if (prefsName != null) {
                    target = new File(sharedPrefsFolder(context), prefsName);
                } else {
                    target = new File(context.getFilesDir(), DataExportRules.filesRelativePath(name));
                }
                // Belt behind the rules' braces: the canonical path must still land inside.
                File root = prefsName != null ? sharedPrefsFolder(context) : context.getFilesDir();
                if (!target.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator)) {
                    refused++;
                    continue;
                }
                if (bytes > DataExportRules.MAX_TOTAL_BYTES) {
                    return new Result(prefsCount, filesCount, bytes, refused, "backup too large");
                }
                File parent = target.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                bytes += writeOut(zip, target);
                if (prefsName != null) prefsCount++;
                else filesCount++;
            }
            return new Result(prefsCount, filesCount, bytes, refused, null);
        } catch (Exception e) {
            return new Result(prefsCount, filesCount, bytes, refused,
                    e.getClass().getSimpleName());
        } finally {
            closeQuietly(zip);
        }
    }

    private static long writeOut(InputStream in, File target) throws IOException {
        OutputStream out = new FileOutputStream(target);
        long total = 0;
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
                total += read;
            }
        } finally {
            out.close();
        }
        return total;
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try { closeable.close(); } catch (Exception ignored) { }
        }
    }
}
