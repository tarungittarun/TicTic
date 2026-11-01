package app.space.notes.runtime;

import android.net.Uri;
import java.io.IOException;
import java.util.Arrays;

/** Read-only intake from known on-device providers, not a blanket content:// exception.
 * SAF tree writes deliberately retain the stricter ExternalStorageProvider policy.
 */
public final class LocalDeviceDocuments {
    private LocalDeviceDocuments() {}
    public static void requireReadable(Uri uri) throws IOException {
        if(uri == null || !"content".equals(uri.getScheme()) || !Arrays.asList(
                "com.android.externalstorage.documents", "com.android.providers.downloads.documents",
                "com.android.providers.media.documents", "media",
                "com.google.android.apps.nbu.files.provider").contains(uri.getAuthority()))
            throw new IOException("Choose an on-device file using Internal storage, SD card or Files by Google. This provider is not accepted for offline opening.");
        // Android's media picker can delegate content to a cloud provider under its own authority.
        if("media".equals(uri.getAuthority()) && uri.getPath()!=null && uri.getPath().contains("/picker") &&
                (uri.getPathSegments().size()<3 || !"com.android.providers.media.photopicker".equals(uri.getPathSegments().get(2))))
            throw new IOException("Choose a downloaded on-device file, not a cloud item.");
    }
}
