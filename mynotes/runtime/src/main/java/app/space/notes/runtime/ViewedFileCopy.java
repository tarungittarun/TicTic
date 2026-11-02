package app.space.notes.runtime;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import java.io.*;

/** Original-byte copy: no truncating opens; verify the actual writable descriptor before writing. */
public final class ViewedFileCopy {
    private ViewedFileCopy(){}
    public static void write(Context context,Uri original,Uri destination)throws Exception{
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("Copy must run off the UI thread");
        LocalDeviceDocuments.requireReadable(original);LocalDeviceDocuments.requireReadable(destination);
        boolean same=original.equals(destination);
        if(original.getAuthority().equals(destination.getAuthority()) && DocumentsContract.isDocumentUri(context,original) && DocumentsContract.isDocumentUri(context,destination))same|=DocumentsContract.getDocumentId(original).equals(DocumentsContract.getDocumentId(destination));
        if(same)throw new IOException("Choose a different destination; original file was not opened for writing");
        // Different provider URIs/document IDs can name the same physical file.
        // "rw" has no TRUNCATE flag: inspect this exact descriptor, never reopen by URI.
        // This prevents a provider/path swap between a read-only check and a truncating open.
        try(ParcelFileDescriptor source=context.getContentResolver().openFileDescriptor(original,"r");ParcelFileDescriptor target=context.getContentResolver().openFileDescriptor(destination,"rw")){
            if(source==null || target==null)throw new IOException("Cannot verify distinct local files; no copy started");
            StructStat from=Os.fstat(source.getFileDescriptor()),to=Os.fstat(target.getFileDescriptor());
            if(!OsConstants.S_ISREG(from.st_mode) || !OsConstants.S_ISREG(to.st_mode))throw new IOException("Choose physical local files for a verifiable original-preserving copy");
            if(from.st_dev==to.st_dev && from.st_ino==to.st_ino)throw new IOException("Destination aliases the original file; original was not changed");
            if(to.st_size!=0)throw new IOException("Choose a new empty destination; no existing file was overwritten");
            if(from.st_size>64L*1024*1024)throw new IOException("Original exceeds 64 MiB copy safety limit; no copy started");
            try(InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(source);OutputStream output=new ParcelFileDescriptor.AutoCloseOutputStream(target)){
                if(output==null)throw new IOException("Destination is unavailable");long bytes=0;byte[] buffer=new byte[65536];for(int n;(n=input.read(buffer))!=-1;){bytes+=n;if(bytes>64L*1024*1024)throw new IOException("Copy exceeds 64 MiB safety limit; partial destination may remain");output.write(buffer,0,n);}
            }
        }
    }
}
