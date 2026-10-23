package app.space.core.data;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * The suite's single door for handing private files to other apps, and for giving the system
 * camera a private destination.
 *
 * <p>Sprint 3 hand-rolled two ContentProviders for this because useAndroidX=false ruled out
 * FileProvider. Sprint 4 adopted AndroidX, so the hand-rolled doors are gone and this facade
 * delegates to the platform-standard one, declared in core-data's manifest with the paths
 * table in res/xml/myspace_paths.xml. Containment is now FileProvider's job: a path outside
 * the table throws instead of being served.
 */
public final class FileShare {

    private FileShare() { }

    /** Suffix on the package name; matches the FileProvider declared in core-data's manifest. */
    public static final String AUTHORITY_SUFFIX = ".myspace_files";

    public static String authority(Context context) {
        return context.getPackageName() + AUTHORITY_SUFFIX;
    }

    /** The content:// URI another app may be granted; the file must live under getFilesDir(). */
    public static Uri uriFor(Context context, File file) {
        return FileProvider.getUriForFile(context, authority(context), file);
    }

    /** A granted-read ACTION_SEND chooser-ready intent for one private file. */
    public static Intent shareIntent(Context context, File file, String subject) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(ShareRules.mimeFor(file.getName()));
        send.putExtra(Intent.EXTRA_STREAM, uriFor(context, file));
        if (subject != null && !subject.isEmpty()) send.putExtra(Intent.EXTRA_SUBJECT, subject);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return send;
    }

    /** Where captured pages land; private, and emptied once the saved PDF holds them. */
    public static File scannerInbox(Context context) {
        File dir = new File(context.getFilesDir(), "scanner" + File.separator + "inbox");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /**
     * The destination URI for one system-camera capture. The file appears when the camera
     * opens it for writing through FileProvider; nothing is created until then.
     */
    public static Uri newCapture(Context context) {
        File file = new File(scannerInbox(context),
                ShareRules.sanitise("scan-" + System.currentTimeMillis() + ".jpg"));
        return uriFor(context, file);
    }
}
