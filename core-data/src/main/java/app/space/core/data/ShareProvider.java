package app.space.core.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * The suite's file-sharing door. useAndroidX=false rules out androidx FileProvider, and without
 * a provider any ACTION_SEND of a file:// URI dies with FileUriExposedException on API 24+.
 * This is the hand-rolled equivalent: read-only, unexported except through per-URI grants, and
 * confined to our own data directory by {@link ShareRules#isInsideRoot}.
 *
 * <p>Declared in core-data's manifest, so every module that depends on :core-data can share
 * without declaring anything itself.
 */
public final class ShareProvider extends ContentProvider {

    @Override public boolean onCreate() { return true; }

    /** Builds the granted-read ACTION_SEND intent for one file. */
    public static Intent shareIntent(Context context, File file, String subject) {
        Uri uri = Uri.parse(ShareRules.uriString(context.getPackageName(),
                file.getAbsolutePath()));
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(ShareRules.mimeFor(file.getName()));
        send.putExtra(Intent.EXTRA_STREAM, uri);
        if (subject != null && !subject.isEmpty()) send.putExtra(Intent.EXTRA_SUBJECT, subject);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return send;
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

    @Override public String getType(Uri uri) {
        File file = resolve(uri);
        return file == null ? "application/octet-stream" : ShareRules.mimeFor(file.getName());
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = resolve(uri);
        if (file == null) throw new FileNotFoundException("Not shareable: " + uri);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    /** Read-only provider: the mutating verbs refuse politely. */
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection,
                                String[] selectionArgs) { return 0; }

    /** The containment check every read goes through; null means "refuse". */
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
        return file.isFile() ? file : null;
    }
}
