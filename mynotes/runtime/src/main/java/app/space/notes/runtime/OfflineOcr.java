package app.space.notes.runtime;

import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import com.googlecode.tesseract.android.TessBaseAPI;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Bundled CPU OCR, no downloader, telemetry or network permission. Never call on UI thread. */
public final class OfflineOcr {
    private OfflineOcr() { }
    public static final List<String> LANGUAGES=Collections.unmodifiableList(Arrays.asList("eng","hin","kan"));
    public static final class Result {
        public final String text,language;public final int confidence,width,height;
        Result(String text,String language,int confidence,int width,int height){this.text=text;this.language=language;this.confidence=confidence;this.width=width;this.height=height;}
    }
    public static final class Cancellation {
        private final AtomicBoolean cancelled=new AtomicBoolean();private TessBaseAPI active;
        public void cancel(){cancelled.set(true);synchronized(this){if(active!=null)active.stop();}}
        public boolean cancelled(){return cancelled.get();}
        void check() throws InterruptedException {if(cancelled())throw new InterruptedException("OCR cancelled");}
    }
    public static Result recognize(Context context,Uri source,String language,Cancellation cancellation) throws Exception {
        requireLocal(source);cancellation.check();File image=File.createTempFile("ocr-source-",".image",context.getCacheDir());
        try{
            try(InputStream in=context.getContentResolver().openInputStream(source);OutputStream out=new FileOutputStream(image)){
                if(in==null)throw new IOException("Cannot open this local image");byte[] buffer=new byte[8192];int count,total=0;
                while((count=in.read(buffer))!=-1){cancellation.check();total+=count;if(total>32*1024*1024)throw new IOException("OCR input is larger than 32 MiB; choose a smaller image. Source is unchanged.");out.write(buffer,0,count);}
            }
            Bitmap bitmap=decodeImage(image);try{return recognizeBitmap(context,bitmap,language,cancellation);}finally{bitmap.recycle();}
        }finally{image.delete();}
    }
    public static void requireLocal(Uri uri) throws IOException {
        if(uri==null || !"content".equals(uri.getScheme()) || !Arrays.asList("com.android.externalstorage.documents","com.android.providers.media.documents","media").contains(uri.getAuthority()))
            throw new IOException("Choose an on-device image from Internal storage or SD card. Cloud providers are not accepted.");
    }
    static Bitmap decodeImage(File image) throws Exception {
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(image.getPath(),options);
        if(options.outWidth<=0 || options.outHeight<=0 || options.outWidth>32768 || options.outHeight>32768)throw new IOException("Unsupported image or dimensions");
        int sample=1;while(options.outWidth/(long)sample*(options.outHeight/(long)sample)>4000000L || Math.max(options.outWidth,options.outHeight)/sample>2400)sample*=2;
        options.inJustDecodeBounds=false;options.inSampleSize=sample;options.inPreferredConfig=Bitmap.Config.ARGB_8888;Bitmap bitmap=BitmapFactory.decodeFile(image.getPath(),options);if(bitmap==null)throw new IOException("Cannot decode this image");
        int orientation=ExifInterface.ORIENTATION_NORMAL;try{orientation=new ExifInterface(image.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);}catch(IOException unsupportedMetadata){/* Decoder already verified pixels; no EXIF orientation is also valid. */}
        Matrix matrix=new Matrix();switch(orientation){
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:matrix.setScale(-1,1);break;
            case ExifInterface.ORIENTATION_ROTATE_180:matrix.setRotate(180);break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:matrix.setScale(1,-1);break;
            case ExifInterface.ORIENTATION_TRANSPOSE:matrix.setRotate(90);matrix.postScale(-1,1);break;
            case ExifInterface.ORIENTATION_ROTATE_90:matrix.setRotate(90);break;
            case ExifInterface.ORIENTATION_TRANSVERSE:matrix.setRotate(270);matrix.postScale(-1,1);break;
            case ExifInterface.ORIENTATION_ROTATE_270:matrix.setRotate(270);break;
        }
        if(!matrix.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);if(rotated!=bitmap)bitmap.recycle();bitmap=rotated;}return bitmap;
    }
    public static Result recognizeBitmap(Context context,Bitmap bitmap,String language,Cancellation cancellation) throws Exception {
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("OCR must run off the UI thread");
        if(!LANGUAGES.contains(language))throw new IOException("Choose English, Hindi or Kannada");
        if(bitmap==null || bitmap.isRecycled() || (long)bitmap.getWidth()*bitmap.getHeight()>4000000L)throw new IOException("Unsupported OCR bitmap");
        cancellation.check();File root=installModel(context,language);cancellation.check();TessBaseAPI engine=new TessBaseAPI();
        try{
            if(!engine.init(root.getAbsolutePath(),language))throw new IOException("Bundled OCR model could not initialize");
            synchronized(cancellation){cancellation.active=engine;}cancellation.check();engine.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);engine.setImage(bitmap);
            engine.getHOCRText(0);cancellation.check();String text=engine.getUTF8Text();cancellation.check();return new Result(text==null?"":text,language,engine.meanConfidence(),bitmap.getWidth(),bitmap.getHeight());
        }finally{synchronized(cancellation){cancellation.active=null;engine.recycle();}}
    }
    private static synchronized File installModel(Context context,String language) throws Exception {
        File root=new File(context.getFilesDir(),"offline-ocr-v1"),directory=new File(root,"tessdata");if(!directory.isDirectory() && !directory.mkdirs())throw new IOException("Cannot create private OCR model directory");
        File[] abandoned=directory.listFiles();if(abandoned!=null)for(File file:abandoned)if(file.getName().startsWith("model-") && file.getName().endsWith(".tmp"))file.delete();
        JSONObject manifest;try(InputStream in=context.getAssets().open("ocr/models.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);manifest=new JSONObject(out.toString("UTF-8"));}
        String name=language+".traineddata",expected=manifest.getJSONObject("files").getJSONObject(name).getString("sha256");File target=new File(directory,name);
        if(target.isFile()){try(InputStream in=new FileInputStream(target)){if(digest(in).equals(expected))return root;}}
        File temporary=File.createTempFile("model-",".tmp",directory);
        try{
            try(InputStream in=context.getAssets().open("ocr/"+name);FileOutputStream out=new FileOutputStream(temporary)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);out.flush();out.getFD().sync();}
            try(InputStream in=new FileInputStream(temporary)){if(!digest(in).equals(expected))throw new IOException("Bundled OCR model checksum mismatch");}
            if(!temporary.renameTo(target))throw new IOException("Cannot install verified OCR model");return root;
        }finally{temporary.delete();}
    }
    public static String digest(InputStream in) throws Exception {MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)digest.update(buffer,0,count);StringBuilder out=new StringBuilder();for(byte b:digest.digest())out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
}
