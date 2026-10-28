package app.space.notes.runtime;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import java.io.*;
import java.util.*;
import app.space.notes.domain.NoteImage;
/** Copy local source once; re-encode pixels to strip EXIF/location and cap memory/backup size. */
final class NoteImageFiles {
 static NoteImage importImage(Context c,Uri uri)throws Exception{
  LocalBackupFiles.requireLocal(uri);byte[] bytes;try(InputStream in=c.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Cannot open image");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1){if(out.size()+count>20*1024*1024)throw new IOException("Choose an image smaller than 20 MiB");out.write(buffer,0,count);}bytes=out.toByteArray();}
  BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);if(bounds.outWidth<=0 || bounds.outHeight<=0)throw new IOException("Unsupported image");BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;while(Math.max(bounds.outWidth/options.inSampleSize,bounds.outHeight/options.inSampleSize)>1600)options.inSampleSize*=2;Bitmap decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(decoded==null)throw new IOException("Image could not be decoded");try{android.graphics.Matrix matrix=new android.graphics.Matrix();int orientation=1;try{orientation=new android.media.ExifInterface(new ByteArrayInputStream(bytes)).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1);}catch(IOException unsupported){}
   switch(orientation){case 2:matrix.setScale(-1,1);break;case 3:matrix.setRotate(180);break;case 4:matrix.setScale(1,-1);break;case 5:matrix.setRotate(90);matrix.postScale(-1,1);break;case 6:matrix.setRotate(90);break;case 7:matrix.setRotate(270);matrix.postScale(-1,1);break;case 8:matrix.setRotate(270);break;default:break;}Bitmap oriented=matrix.isIdentity()?decoded:Bitmap.createBitmap(decoded,0,0,decoded.getWidth(),decoded.getHeight(),matrix,true);try{return encode(oriented,"Image");}finally{if(oriented!=decoded)oriented.recycle();}
  }finally{decoded.recycle();}
 }
 static NoteImage encode(Bitmap bitmap,String name)throws IOException{
  Bitmap scaled=bitmap;int size=Math.max(bitmap.getWidth(),bitmap.getHeight());if(size>1600)scaled=Bitmap.createScaledBitmap(bitmap,Math.max(1,bitmap.getWidth()*1600/size),Math.max(1,bitmap.getHeight()*1600/size),true);
  try{ByteArrayOutputStream out=new ByteArrayOutputStream();boolean png=scaled.hasAlpha();if(!scaled.compress(png?Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.JPEG,png?100:85,out))throw new IOException("Cannot encode image");if(out.size()>1024*1024)throw new IOException("Image is too detailed to save safely. Choose a smaller image or simpler drawing.");return new NoteImage(UUID.randomUUID().toString(),name,png?"image/png":"image/jpeg",scaled.getWidth(),scaled.getHeight(),Base64.getEncoder().encodeToString(out.toByteArray()));}finally{if(scaled!=bitmap)scaled.recycle();}
 }
 static Bitmap decode(NoteImage image){byte[] bytes=Base64.getDecoder().decode(image.data);BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);if(bounds.outWidth!=image.width || bounds.outHeight!=image.height)return null;return BitmapFactory.decodeByteArray(bytes,0,bytes.length);}
}
