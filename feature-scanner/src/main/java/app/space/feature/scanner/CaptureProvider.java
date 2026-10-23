package app.space.feature.scanner;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

import app.space.core.data.ShareRules;

/**
 * The writable door the camera app writes a captured page through.
 *
 * <p>MediaStore.ACTION_IMAGE_CAPTURE needs an EXTRA_OUTPUT URI, and since API 24 a file:// URI
 * there throws FileUriExposedException. androidx FileProvider is unavailable
 * (useAndroidX=false), so this is the hand-rolled equivalent: insert() mints a file inside
 * scanner/inbox, openFile() serves it write-only to the camera and read-only back to us, and
 * every path is containment-checked against our own data directory before it is touched.
 */
public final class CaptureProvider extends ContentProvider {

    static final String AUTHORITY_SUFFIX = ".myspace_capture";

    /** Mints the destination URI for one capture; the file itself appears when the camera writes. */
    public static Uri newCapture(Context context) {
        ContentValues values = new ContentValues();
        values.put(OpenableColumns.DISPLAY_NAME,
                "scan-" + System.currentTimeMillis() + ".jpg");
        return context.getContentResolver().insert(
                Uri.parse("content://" + context.getPackageName() + AUTHORITY_SUFFIX), values);
    }

    static File inbox(Context context) {
        File dir = new File(context.getFilesDir(), "scanner" + File.separator + "inbox");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    @Override public boolean onCreate() { return true; }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Context context = getContext();
        if (context == null) return null;
        String name = values == null ? null : values.getAsString(OpenableColumns.DISPLAY_NAME);
        name = ShareRules.sanitise(name == null ? "scan.jpg" : name);
        if (name.isEmpty()) name = "scan.jpg";
        File file = new File(inbox(context), name);
        return Uri.parse(ShareRules.uriString(context.getPackageName(), file.getAbsolutePath()));
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = resolve(uri);
        if (file == null) throw new FileNotFoundException("Not capturable: " + uri);
        if (mode != null && mode.contains("w")) {
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        File file = resolve(uri);
        if (file == null) return null;
        MatrixCursor cursor = new MatrixCursor(new String[]{
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        cursor.addRow(new Object[]{file.getName(), file.length()});
        return cursor;
    }

    @Override public String getType(Uri uri) { return "image/jpeg"; }

    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Cleanup is the app's own job through File, not a stranger's through this door.
        return 0;
    }

    @Override public int update(Uri uri, ContentValues values, String selection,
                                String[] selectionArgs) { return 0; }

    private File resolve(Uri uri) {
        Context context = getContext();
        if (context == null || uri == null || uri.getPath() == null) return null;
        File root = context.getFilesDir().getParentFile();
        File file = new File(uri.getPath());
        try {
            String rootPath = root == null ? null : root.getCanonicalPath();
            if (!ShareRules.isInsideRoot(rootPath, file.getCanonicalPath())) return null;
        } catch (Exception e) {
            return null;
        }
        return file;
    }
}
