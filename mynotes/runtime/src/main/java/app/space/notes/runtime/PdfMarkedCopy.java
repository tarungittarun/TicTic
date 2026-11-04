package app.space.notes.runtime;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.system.*;
import java.io.*;
import java.security.MessageDigest;
/** Export only an app-created marked PDF into a new empty local file, with descriptor readback. */
public final class PdfMarkedCopy {
 private PdfMarkedCopy(){}
 public static void write(Context context,File generated,Uri destination)throws Exception{
  if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("PDF copy must run off main thread");
  File source=generated.getCanonicalFile();if(!source.getParentFile().equals(context.getCacheDir().getCanonicalFile()) || !source.getName().startsWith("marked-pdf-") || !source.isFile())throw new IOException("Marked export expired; reopen the retained annotation draft");
  if(source.length()<1 || source.length()>128L*1024*1024)throw new IOException("Marked PDF exceeds export safety limit");LocalDeviceDocuments.requireReadable(destination);
  try(FileInputStream input=new FileInputStream(source);ParcelFileDescriptor target=context.getContentResolver().openFileDescriptor(destination,"rw")){
   if(target==null)throw new IOException("Destination unavailable");StructStat a=Os.fstat(input.getFD()),b=Os.fstat(target.getFileDescriptor());if(!OsConstants.S_ISREG(b.st_mode)||b.st_size!=0 || (a.st_dev==b.st_dev && a.st_ino==b.st_ino))throw new IOException("Choose a new empty destination; existing files were not changed");
   MessageDigest expected=MessageDigest.getInstance("SHA-256");
   try(OutputStream output=new ParcelFileDescriptor.AutoCloseOutputStream(target)){
    byte[] buffer=new byte[65536];long bytes=0;for(int n;(n=input.read(buffer))!=-1;){if((bytes+=n)>128L*1024*1024)throw new IOException("Export bound exceeded; draft retained");expected.update(buffer,0,n);output.write(buffer,0,n);}output.flush();Os.fsync(target.getFileDescriptor());Os.lseek(target.getFileDescriptor(),0,OsConstants.SEEK_SET);MessageDigest actual=MessageDigest.getInstance("SHA-256");long observed=0;
    try(InputStream check=new ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.dup(target.getFileDescriptor()))){for(int n;(n=check.read(buffer))!=-1;){if((observed+=n)>128L*1024*1024)throw new IOException("Readback bound exceeded");actual.update(buffer,0,n);}}
    if(observed!=bytes || !MessageDigest.isEqual(expected.digest(),actual.digest()))throw new IOException("Marked PDF copy did not verify; private draft retained");
   }
  }
 }
}
