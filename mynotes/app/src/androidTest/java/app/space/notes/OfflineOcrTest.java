package app.space.notes;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.graphics.*;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import java.io.*;
import java.util.*;
import app.space.notes.runtime.OfflineOcr;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class OfflineOcrTest {
    @Test public void bundledModelsMatchPinnedHashes() throws Exception {android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();JSONObject manifest;try(InputStream in=context.getAssets().open("ocr/models.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);manifest=new JSONObject(out.toString("UTF-8"));}for(String language:OfflineOcr.LANGUAGES)try(InputStream in=context.getAssets().open("ocr/"+language+".traineddata")){assertEquals(manifest.getJSONObject("files").getJSONObject(language+".traineddata").getString("sha256"),OfflineOcr.digest(in));}}
    @Test public void offlineEnglishImageRunsRealNativeInference() throws Exception {Bitmap bitmap=Bitmap.createBitmap(1200,400,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(72);canvas.drawText("MEETING NOTES",70,140,paint);canvas.drawText("Monday at 10 AM",70,250,paint);try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),bitmap,"eng",new OfflineOcr.Cancellation());assertTrue(result.text,result.text.toUpperCase(Locale.ROOT).contains("MEETING"));assertTrue(result.text,result.text.toUpperCase(Locale.ROOT).contains("MONDAY"));assertTrue(result.confidence>=0);assertEquals(1200,result.width);}finally{bitmap.recycle();}}
    @Test public void hindiAndKannadaBundledModelsRunWithoutDownloading() throws Exception {Bitmap bitmap=Bitmap.createBitmap(400,300,Bitmap.Config.ARGB_8888);new Canvas(bitmap).drawColor(Color.WHITE);try{for(String language:Arrays.asList("hin","kan")){OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),bitmap,language,new OfflineOcr.Cancellation());assertEquals(language,result.language);assertTrue(result.text,result.text.isEmpty());}}finally{bitmap.recycle();}}
    @Test public void controlledHindiKannadaImagesProduceAuditableDiagnostics() throws Exception {
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();org.json.JSONArray rows=new org.json.JSONArray();String[] languages={"hin","kan"},phrases={"भारत नमस्ते","ಕನ್ನಡ ಭಾಷೆ"},tokens={"भारत","ಕನ್ನಡ"};
        for(int i=0;i<languages.length;i++){Bitmap bitmap=Bitmap.createBitmap(1400,440,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(100);canvas.drawText(phrases[i],70,180,paint);canvas.drawText(phrases[i],70,330,paint);
            try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(context,bitmap,languages[i],new OfflineOcr.Cancellation());assertEquals(languages[i],result.language);assertEquals(1400,result.width);String normalized=java.text.Normalizer.normalize(result.text,java.text.Normalizer.Form.NFC);rows.put(new JSONObject().put("language",languages[i]).put("input",phrases[i]).put("expected_token",tokens[i]).put("recognized",result.text).put("confidence",result.confidence).put("token_match",normalized.contains(tokens[i])).put("scope","controlled system-font diagnostic, not a language-accuracy release gate or physical document corpus"));try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),"ocr-fixture-"+languages[i]+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}}finally{bitmap.recycle();}
        }
        try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),"ocr-corpus-report.json"))){out.write(new JSONObject().put("schema",1).put("cases",rows).toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }
    @Test public void cancelledTaskNeverStartsInference() throws Exception {Bitmap bitmap=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);OfflineOcr.Cancellation cancellation=new OfflineOcr.Cancellation();cancellation.cancel();try{OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),bitmap,"eng",cancellation);fail("cancelled");}catch(InterruptedException expected){assertTrue(cancellation.cancelled());}finally{bitmap.recycle();}}
    @Test public void cloudAndFileUrisAreRefusedBeforeReading() throws Exception {for(String value:Arrays.asList("https://example.invalid/image.png","file:///sdcard/image.png","content://cloud.example/image")){try{OfflineOcr.requireLocal(Uri.parse(value));fail("cloud input must be refused");}catch(IOException expected){assertTrue(expected.getMessage().contains("on-device"));}}}
    @Test public void blankImageDoesNotInventText() throws Exception {Bitmap bitmap=Bitmap.createBitmap(600,400,Bitmap.Config.ARGB_8888);new Canvas(bitmap).drawColor(Color.WHITE);try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),bitmap,"eng",new OfflineOcr.Cancellation());assertTrue(result.text,result.text.isEmpty());}finally{bitmap.recycle();}}
}
