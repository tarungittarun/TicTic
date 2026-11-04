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
import app.space.notes.domain.ImageTextSelection;

/** Bundled CPU OCR, no downloader, telemetry or network permission. Never call on UI thread. */
public final class OfflineOcr {
    private OfflineOcr() { }
    public static final List<String> LANGUAGES=Collections.unmodifiableList(Arrays.asList("eng","hin","kan"));
    public static final class Result {
        public final String text,language;public final int confidence,width,height;public final List<ImageTextSelection.Word> words;
        Result(String text,String language,int confidence,int width,int height){this(text,language,confidence,width,height,Collections.emptyList());}
        Result(String text,String language,int confidence,int width,int height,List<ImageTextSelection.Word> words){this.text=text;this.language=language;this.confidence=confidence;this.width=width;this.height=height;this.words=Collections.unmodifiableList(new ArrayList<>(words));}
    }
    public static final class Cancellation {
        private final AtomicBoolean cancelled=new AtomicBoolean();private TessBaseAPI active;
        public void cancel(){cancelled.set(true);synchronized(this){if(active!=null)active.stop();}}
        public boolean cancelled(){return cancelled.get();}
        void check() throws InterruptedException {if(cancelled())throw new InterruptedException("OCR cancelled");}
    }
    /** Preview pixels are the bounded, EXIF-oriented source, not the OCR-normalized image. */
    public static final class Scan implements AutoCloseable {
        public final Result result;public final Bitmap image;private int uses=1;private boolean closed;
        Scan(Result result,Bitmap image){this.result=result;this.image=image;}
        synchronized boolean retain(){if(closed)return false;uses++;return true;}
        synchronized void release(){if(--uses==0)image.recycle();}
        @Override public synchronized void close(){if(!closed){closed=true;release();}}
    }
    public static Result recognize(Context context,Uri source,String language,Cancellation cancellation) throws Exception {
        try(Scan scan=scan(context,source,language,cancellation)){return scan.result;}
    }
    public static Scan scan(Context context,Uri source,String language,Cancellation cancellation) throws Exception {
        requireLocal(source);cancellation.check();File image=File.createTempFile("ocr-source-",".image",context.getCacheDir());
        try{
            try(InputStream in=context.getContentResolver().openInputStream(source);OutputStream out=new FileOutputStream(image)){
                if(in==null)throw new IOException("Cannot open this local image");byte[] buffer=new byte[8192];int count,total=0;
                while((count=in.read(buffer))!=-1){cancellation.check();total+=count;if(total>32*1024*1024)throw new IOException("OCR input is larger than 32 MiB; choose a smaller image. Source is unchanged.");out.write(buffer,0,count);}
            }
            Bitmap bitmap=decodeImage(image);boolean transferred=false;try{Result result=recognizeBitmap(context,bitmap,language,cancellation);Scan scan=new Scan(result,bitmap);transferred=true;return scan;}finally{if(!transferred)bitmap.recycle();}
        }finally{image.delete();}
    }
    public static void requireLocal(Uri uri) throws IOException {
        if(uri==null || !"content".equals(uri.getScheme()) || !Arrays.asList("com.android.externalstorage.documents","com.android.providers.media.documents","com.android.providers.downloads.documents","media").contains(uri.getAuthority()))
            throw new IOException("Choose an on-device image from Internal storage or SD card. Cloud providers are not accepted.");
        if("media".equals(uri.getAuthority()) && uri.getPath()!=null && uri.getPath().contains("/picker") && (uri.getPathSegments().size()<3 || !uri.getPathSegments().get(2).equals("com.android.providers.media.photopicker")))throw new IOException("Choose an on-device photo, not a cloud photo.");
    }
    static Bitmap decodeImage(File image) throws Exception {
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(image.getPath(),options);
        if(options.outWidth<=0 || options.outHeight<=0 || options.outWidth>32768 || options.outHeight>32768)throw new IOException("Unsupported image or dimensions");
        int sample=app.space.notes.domain.OcrRasterScale.decodeSample(options.outWidth,options.outHeight);
        options.inJustDecodeBounds=false;options.inSampleSize=sample;options.inPreferredConfig=Bitmap.Config.ARGB_8888;Bitmap bitmap=BitmapFactory.decodeFile(image.getPath(),options);if(bitmap==null)throw new IOException("Cannot decode this image");
        try{int orientation=ExifInterface.ORIENTATION_NORMAL;try{orientation=new ExifInterface(image.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);}catch(IOException unsupportedMetadata){/* Decoder already verified pixels; no EXIF orientation is also valid. */}
        Matrix matrix=new Matrix();switch(orientation){
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:matrix.setScale(-1,1);break;
            case ExifInterface.ORIENTATION_ROTATE_180:matrix.setRotate(180);break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:matrix.setScale(1,-1);break;
            case ExifInterface.ORIENTATION_TRANSPOSE:matrix.setRotate(90);matrix.postScale(-1,1);break;
            case ExifInterface.ORIENTATION_ROTATE_90:matrix.setRotate(90);break;
            case ExifInterface.ORIENTATION_TRANSVERSE:matrix.setRotate(270);matrix.postScale(-1,1);break;
            case ExifInterface.ORIENTATION_ROTATE_270:matrix.setRotate(270);break;
        }
        if(!matrix.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);if(rotated!=bitmap)bitmap.recycle();bitmap=rotated;}return bitmap;}catch(Exception | OutOfMemoryError failure){bitmap.recycle();throw failure;}
    }
    /** Composite transparency, normalize dark screenshots, upscale small text; never alter the source. */
    public static Bitmap prepareBitmap(Bitmap source){
        double scale=app.space.notes.domain.OcrRasterScale.factor(source.getWidth(),source.getHeight());
        if(scale>1 && readablePaperInk(source))scale=1;
        Bitmap out=Bitmap.createBitmap(Math.max(1,(int)Math.floor(source.getWidth()*scale)),Math.max(1,(int)Math.floor(source.getHeight()*scale)),Bitmap.Config.ARGB_8888);try{Canvas canvas=new Canvas(out);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);canvas.drawBitmap(source,null,new Rect(0,0,out.getWidth(),out.getHeight()),paint);
        long border=0;int samples=0;for(int i=0;i<16;i++){int x=i*(out.getWidth()-1)/15;for(int y:new int[]{0,out.getHeight()-1}){int c=out.getPixel(x,y);border+=(Color.red(c)*299+Color.green(c)*587+Color.blue(c)*114)/1000;samples++;}}
        int dark=0;for(int gy=0;gy<8;gy++)for(int gx=0;gx<8;gx++){int c=out.getPixel((2*gx+1)*out.getWidth()/16,(2*gy+1)*out.getHeight()/16);if((Color.red(c)*299+Color.green(c)*587+Color.blue(c)*114)/1000<80)dark++;}
        if(border/samples<80 && dark>=45){ColorMatrix invert=new ColorMatrix(new float[]{-1,0,0,0,255,0,-1,0,0,255,0,0,-1,0,255,0,0,0,1,0});paint.setColorFilter(new ColorMatrixColorFilter(invert));Bitmap copy=out.copy(Bitmap.Config.ARGB_8888,false);try{canvas.drawBitmap(copy,0,0,paint);}finally{copy.recycle();}}
        return out;}catch(RuntimeException | OutOfMemoryError failure){out.recycle();throw failure;}
    }
    /** Cheap source-only estimate: do not interpolate already readable paper text.
     * No dictionary/reference/language oracle. Unknown scenes keep the bounded fallback.
     */
    private static boolean readablePaperInk(Bitmap source){
        int width=source.getWidth(),height=source.getHeight(),light=0,dark=0;
        if(height>3200 || width>3200)return false;
        for(int gy=0;gy<32;gy++)for(int gx=0;gx<32;gx++){
            int c=source.getPixel(gx*(width-1)/31,gy*(height-1)/31);
            if(Color.alpha(c)<250)continue;
            int l=(Color.red(c)*299+Color.green(c)*587+Color.blue(c)*114)/1000;
            if(l>=245)light++;if(l<=10)dark++;
        }
        if(Math.max(light,dark)<870)return false; // At least 85% near-uniform paper background.
        boolean inverted=dark>light;boolean[] ink=new boolean[height];int[] row=new int[width];
        int step=Math.max(1,width/1600);
        for(int y=0;y<height;y++){
            source.getPixels(row,0,width,0,y,width,1);int count=0;
            for(int x=0;x<width;x+=step){int c=row[x];if(Color.alpha(c)<250)continue;
                int l=(Color.red(c)*299+Color.green(c)*587+Color.blue(c)*114)/1000;
                if(inverted?l>128:l<128)if(++count>=2){ink[y]=true;break;}
            }
        }
        return app.space.notes.domain.OcrRasterScale.readableInkRows(ink);
    }
    /** Fresh inference on a selected source area, not filtering stale full-image words. */
    public static Result recognizeRegion(Context context,Bitmap source,RectF box,String language,Cancellation cancellation)throws Exception{
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("OCR must run off the UI thread");
        cancellation.check();
        if(source==null || source.isRecycled() || (long)source.getWidth()*source.getHeight()>4000000L || box==null || !Float.isFinite(box.left) || !Float.isFinite(box.top) || !Float.isFinite(box.right) || !Float.isFinite(box.bottom) || box.left<0 || box.top<0 || box.right>1 || box.bottom>1 || box.width()<=0 || box.height()<=0)throw new IOException("Choose an area inside the image");
        int w=source.getWidth(),h=source.getHeight(),left=(int)Math.floor(box.left*w),top=(int)Math.floor(box.top*h),right=Math.min(w,(int)Math.ceil(box.right*w)),bottom=Math.min(h,(int)Math.ceil(box.bottom*h));
        if(right<=left || bottom<=top)throw new IOException("Choose a larger image area");
        Bitmap crop=Bitmap.createBitmap(source,left,top,right-left,bottom-top);
        try{return recognizeBitmap(context,crop,language,cancellation);}finally{if(crop!=source)crop.recycle();}
    }
    public static Result recognizeBitmap(Context context,Bitmap bitmap,String language,Cancellation cancellation) throws Exception {
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("OCR must run off the UI thread");
        if(!"auto".equals(language) && !LANGUAGES.contains(language))throw new IOException("Choose English, Hindi or Kannada");
        if(bitmap==null || bitmap.isRecycled() || (long)bitmap.getWidth()*bitmap.getHeight()>4000000L)throw new IOException("Unsupported OCR bitmap");
        cancellation.check();File root;if("auto".equals(language)){root=null;for(String code:LANGUAGES){cancellation.check();root=installModel(context,code);}}else root=installModel(context,language);cancellation.check();TessBaseAPI engine=new TessBaseAPI();Bitmap prepared=null;
        try{
            prepared=prepareBitmap(bitmap);
            if(!engine.init(root.getAbsolutePath(),"auto".equals(language)?"eng+hin+kan":language))throw new IOException("Bundled OCR model could not initialize");
            synchronized(cancellation){cancellation.active=engine;}cancellation.check();engine.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);engine.setVariable("preserve_interword_spaces","1");engine.setImage(prepared);
            String hocr=engine.getHOCRText(0);cancellation.check();String text=engine.getUTF8Text();cancellation.check();int confidence=engine.meanConfidence();if(text==null)text="";{engine.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT);engine.setImage(prepared);String sparse=engine.getUTF8Text();cancellation.check();int score=engine.meanConfidence();
                if(app.space.notes.domain.OcrLineRecovery.eligible(text,confidence,sparse,score)){
                    app.space.notes.domain.OcrLineRecovery.Recovered recovered=app.space.notes.domain.OcrLineRecovery.recover(text,confidence,regions(hocr,prepared.getWidth(),prepared.getHeight(),cancellation),sparse,score,regions(engine.getHOCRText(0),prepared.getWidth(),prepared.getHeight(),cancellation));
                    cancellation.check();if(recovered!=null)return finishRecognition(root,bitmap,prepared,language,recovered.text,recovered.confidence,recovered.words,engine,cancellation);
                }
                if(app.space.notes.domain.OcrAlternatives.prefer(text,confidence,sparse,score)){text=sparse;confidence=score;hocr=engine.getHOCRText(0);}}
            if(app.space.notes.domain.OcrAlternatives.needsBlockCheck(text)){
                cancellation.check();engine.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);engine.setImage(prepared);String block=engine.getUTF8Text();cancellation.check();int score=engine.meanConfidence();
                if(app.space.notes.domain.OcrAlternatives.prefer(text,confidence,block,score)){text=block;confidence=score;hocr=engine.getHOCRText(0);}
            }
            // Local adaptive threshold fallback only after all ordinary passes are empty.
            // Never rewrite already-recognized native-script/document text globally.
            if(text.trim().isEmpty() && engine.setVariable("thresholding_method","2")){
                cancellation.check();engine.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);engine.setImage(prepared);
                String adaptive=engine.getUTF8Text();cancellation.check();int score=engine.meanConfidence();
                if(adaptive!=null && app.space.notes.domain.OcrAlternatives.prefer(text,confidence,adaptive,score)){text=adaptive;confidence=score;hocr=engine.getHOCRText(0);}
            }
            List<ImageTextSelection.Word> chosen=regions(hocr,prepared.getWidth(),prepared.getHeight(),cancellation);
            return finishRecognition(root,bitmap,prepared,language,text,confidence,chosen,engine,cancellation);
        }finally{synchronized(cancellation){cancellation.active=null;try{engine.recycle();}finally{if(prepared!=null)prepared.recycle();}}}
    }
    private static boolean uniformPaperBackground(Bitmap source){
        int light=0,dark=0;
        for(int gy=0;gy<32;gy++)for(int gx=0;gx<32;gx++){
            int c=source.getPixel(gx*(source.getWidth()-1)/31,gy*(source.getHeight()-1)/31);if(Color.alpha(c)<250)continue;
            int l=(Color.red(c)*299+Color.green(c)*587+Color.blue(c)*114)/1000;if(l>=245)light++;if(l<=10)dark++;
        }return Math.max(light,dark)>=870;
    }
    private static Result finishRecognition(File root,Bitmap bitmap,Bitmap prepared,String language,String text,int confidence,List<ImageTextSelection.Word> chosen,TessBaseAPI engine,Cancellation cancellation)throws Exception {
        if("auto".equals(language) && prepared.getWidth()==bitmap.getWidth() && prepared.getHeight()==bitmap.getHeight() && app.space.notes.domain.OcrScriptConsensus.eligible(text,chosen) && uniformPaperBackground(bitmap)){
            Result agreed=kannadaConsensus(root,bitmap,text,chosen,engine,cancellation);if(agreed!=null)return agreed;
        }
        cancellation.check();return new Result(text,language,confidence,bitmap.getWidth(),bitmap.getHeight(),chosen);
    }
    // Two complete native outputs must agree, including source-coordinate coverage.
    // Raw source polarity is deliberate; no expected phrase or dictionary is consulted.
    private static Result kannadaConsensus(File root,Bitmap source,String baseline,List<ImageTextSelection.Word> original,TessBaseAPI primary,Cancellation cancellation)throws Exception {
        TessBaseAPI probe=new TessBaseAPI();Bitmap input=null;
        try{
            cancellation.check();if(!probe.init(root.getAbsolutePath(),"kan"))return null;
            synchronized(cancellation){cancellation.active=probe;}cancellation.check();
            probe.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);probe.setVariable("preserve_interword_spaces","1");
            input=source.copy(Bitmap.Config.ARGB_8888,true);if(input==null)return null;
            probe.setImage(input);String ordinary=probe.getUTF8Text();cancellation.check();int ordinaryScore=probe.meanConfidence();
            List<ImageTextSelection.Word> ordinaryWords=regions(probe.getHOCRText(0),source.getWidth(),source.getHeight(),cancellation);
            if(!probe.setVariable("thresholding_method","2"))return null;
            probe.setImage(input);String adaptive=probe.getUTF8Text();cancellation.check();int adaptiveScore=probe.meanConfidence();
            List<ImageTextSelection.Word> adaptiveWords=regions(probe.getHOCRText(0),source.getWidth(),source.getHeight(),cancellation);
            if(app.space.notes.domain.OcrScriptConsensus.prefer(baseline,original,ordinary,ordinaryScore,ordinaryWords,adaptive,adaptiveScore,adaptiveWords))return new Result(adaptive,"auto",Math.min(ordinaryScore,adaptiveScore),source.getWidth(),source.getHeight(),adaptiveWords);
            return null;
        }finally{synchronized(cancellation){cancellation.active=primary;try{probe.recycle();}finally{if(input!=null)input.recycle();}}}
    }
    /** Bounded native-generated XHTML; no external entities, HTML views, or network fetches. */
    private static List<ImageTextSelection.Word> regions(String hocr,int width,int height,Cancellation cancel)throws InterruptedException {
        List<ImageTextSelection.Word> words=new ArrayList<>();if(hocr==null || hocr.length()>2000000)return words;
        try{
            org.xmlpull.v1.XmlPullParser parser=android.util.Xml.newPullParser();parser.setInput(new StringReader(hocr.replace("&nbsp;","&#160;")));
            int event,line=0,wordDepth=-1;float[] box=null;StringBuilder value=new StringBuilder();
            java.util.regex.Pattern bounds=java.util.regex.Pattern.compile("(?:^|;)\\s*bbox (\\d+) (\\d+) (\\d+) (\\d+)(?:;|$)");
            while((event=parser.next())!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT){cancel.check();
                if(event==org.xmlpull.v1.XmlPullParser.START_TAG){String type=parser.getAttributeValue(null,"class");if("ocr_line".equals(type) || "ocr_header".equals(type))line++;
                    if("ocrx_word".equals(type)){wordDepth=parser.getDepth();value.setLength(0);box=null;String title=parser.getAttributeValue(null,"title");java.util.regex.Matcher match=bounds.matcher(title==null?"":title);if(match.find())box=new float[]{Float.parseFloat(match.group(1))/width,Float.parseFloat(match.group(2))/height,Float.parseFloat(match.group(3))/width,Float.parseFloat(match.group(4))/height};}
                }else if(event==org.xmlpull.v1.XmlPullParser.TEXT && wordDepth>=0){if(value.length()+parser.getText().length()>2000)return Collections.emptyList();value.append(parser.getText());}
                else if(event==org.xmlpull.v1.XmlPullParser.END_TAG && parser.getDepth()==wordDepth){if(box!=null && !value.toString().trim().isEmpty()){if(words.size()==4096)return Collections.emptyList();words.add(new ImageTextSelection.Word(value.toString().trim(),box[0],box[1],box[2],box[3],line));}wordDepth=-1;}
            }
        }catch(InterruptedException cancelled){throw cancelled;}catch(Exception invalid){Diagnostics.record("ocr.regions","unavailable; full text retained");return Collections.emptyList();}return words;
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
