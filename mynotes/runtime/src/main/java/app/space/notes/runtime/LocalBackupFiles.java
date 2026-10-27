package app.space.notes.runtime;

import android.content.ContentResolver;
import android.net.Uri;
import java.io.*;
import java.util.*;
import app.space.notes.domain.Note;

public final class LocalBackupFiles {
    private LocalBackupFiles() { }
    public static void requireLocal(Uri uri) throws IOException {
        if(!"content".equals(uri.getScheme()) || !"com.android.externalstorage.documents".equals(uri.getAuthority()))
            throw new IOException("Use Internal storage or SD card through the local Android storage provider, not a cloud provider");
    }
    public static List<Note> read(ContentResolver resolver,Uri uri) throws Exception {
        requireLocal(uri);
        try(InputStream in=resolver.openInputStream(uri)) {return BackupCodec.decode(BackupCodec.read(in));}
    }
    public static void write(ContentResolver resolver,Uri uri,List<Note> notes) throws Exception {
        requireLocal(uri);byte[] bytes=BackupCodec.encode(notes);
        try(OutputStream out=resolver.openOutputStream(uri,"rwt")) {
            if(out==null)throw new IOException("Cannot write backup");out.write(bytes);out.flush();
        }
        try(InputStream in=resolver.openInputStream(uri)) {
            byte[] reopened=BackupCodec.read(in);
            if(!Arrays.equals(bytes,reopened))throw new IOException("Backup readback failed; do not rely on this file");
            BackupCodec.decode(reopened);
        }
    }
}
