package app.space.notes.runtime;

import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.*;
import java.security.*;
import java.util.UUID;

/** Real PNG clipboard content, never a UI screenshot or a bitmap Binder extra. */
final class DrawingClipboard {
 private DrawingClipboard() { }
 static Uri write(Context context,Bitmap bitmap)throws IOException{
  File directory=new File(context.getCacheDir(),"drawing-clipboard");if(!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory())throw new IOException("Cannot prepare drawing clipboard");
  File file=new File(directory,UUID.randomUUID()+".png");boolean complete=false;try{try(FileOutputStream out=new FileOutputStream(file)){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Cannot encode clipboard drawing");out.getFD().sync();}Uri uri=FileProvider.getUriForFile(context,context.getPackageName()+".notes.images",file);complete=true;return uri;}finally{if(!complete)file.delete();}
 }
 static void publish(Context context,Uri uri){ClipboardManager clipboard=(ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);if(clipboard==null)throw new IllegalStateException("Clipboard unavailable");clipboard.setPrimaryClip(ClipData.newUri(context.getContentResolver(),"Drawing",uri));}
 static String digest(Bitmap bitmap)throws IOException{
  try{MessageDigest hash=MessageDigest.getInstance("SHA-256");OutputStream discard=new OutputStream(){@Override public void write(int value){}@Override public void write(byte[] bytes,int offset,int length){}};try(DigestOutputStream out=new DigestOutputStream(discard,hash)){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Cannot verify drawing draft");}return hex(hash.digest());}catch(NoSuchAlgorithmException impossible){throw new IOException("Drawing checksum unavailable");}
 }
 static String digest(InputStream input)throws IOException{try{MessageDigest hash=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[8192];long total=0;int count;while((count=input.read(buffer))!=-1){total+=count;if(total>20L*1024*1024)throw new IOException("Drawing draft exceeds bounds");hash.update(buffer,0,count);}return hex(hash.digest());}catch(NoSuchAlgorithmException impossible){throw new IOException("Drawing checksum unavailable");}}
 private static String hex(byte[] bytes){StringBuilder value=new StringBuilder();for(byte b:bytes)value.append(Character.forDigit((b&255)>>>4,16)).append(Character.forDigit(b&15,16));return value.toString();}
}
