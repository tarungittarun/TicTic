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
    @Test public void tallScreenshotPreprocessingEnlargesSmallTextWithinBudget()throws Exception{
        Bitmap original=Bitmap.createBitmap(705,1568,Bitmap.Config.ARGB_8888);
        new Canvas(original).drawColor(Color.rgb(53,56,63));
        Bitmap prepared=OfflineOcr.prepareBitmap(original);
        try{assertTrue(prepared.getWidth()>1057);assertTrue((long)prepared.getWidth()*prepared.getHeight()<=4000000L);assertEquals(705,original.getWidth());}
        finally{prepared.recycle();original.recycle();}
        Bitmap tall=Bitmap.createBitmap(1200,2670,Bitmap.Config.ARGB_8888);
        File file=File.createTempFile("tall-screenshot-", ".png", InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir());
        try{try(OutputStream output=new FileOutputStream(file)){assertTrue(tall.compress(Bitmap.CompressFormat.PNG,100,output));}
            java.lang.reflect.Method decode=OfflineOcr.class.getDeclaredMethod("decodeImage",File.class);decode.setAccessible(true);
            Bitmap decoded=(Bitmap)decode.invoke(null,file);
            try{assertEquals("Do not halve readable screenshot pixels just because it is tall",1200,decoded.getWidth());assertEquals(2670,decoded.getHeight());}finally{decoded.recycle();}
        }finally{tall.recycle();file.delete();}
    }
    @Test public void readableKannadaPaperPreservesNativeInkAndBothLines()throws Exception{
        // Reproduce independently found public corpus ocr-0268, including its actual italic font.
        for(boolean dark:new boolean[]{false,true}){
            Bitmap source=Bitmap.createBitmap(1600,700,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(source);
            canvas.drawColor(dark?Color.BLACK:Color.WHITE);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(dark?Color.WHITE:Color.BLACK);p.setTextSize(48);p.setTypeface(Typeface.create("monospace",Typeface.ITALIC));
            canvas.drawText("ಕನ್ನಡ ಭಾಷೆ",70,150,p);canvas.drawText("ಭಾರತ ನಮಸ್ಕಾರ",70,280,p);
            Bitmap prepared=OfflineOcr.prepareBitmap(source);
            try{
                assertEquals("Do not interpolate already readable native-script strokes",1600,prepared.getWidth());assertEquals(700,prepared.getHeight());
                assertEquals(1600,source.getWidth());
                OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),source,"auto",new OfflineOcr.Cancellation());
                String text=result.text.replaceAll("\\s+"," ");
                assertTrue(result.text,text.contains("ಕನ್ನಡ ಭಾಷೆ"));assertTrue(result.text,text.contains("ಭಾರತ ನಮಸ್ಕಾರ"));
            }finally{prepared.recycle();source.recycle();}
        }
    }
    @Test public void mixedStyleDialogScreenshotKeepsTitleAndActions()throws Exception{
        // Own-authored recreation: never upload a user's screenshot into CI.
        Bitmap original=Bitmap.createBitmap(705,1568,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(original);canvas.drawColor(Color.rgb(57,53,27));
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.rgb(53,56,63));
        canvas.drawRoundRect(44,658,661,941,32,32,p);
        p.setColor(Color.WHITE);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(34);
        canvas.drawText("OneDoc keeps stopping",84,735,p);
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(24);
        canvas.drawText("App info",217,816,p);canvas.drawText("Close app",217,896,p);
        try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),original,"auto",new OfflineOcr.Cancellation());
            String text=result.text.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
            assertTrue(result.text,text.contains("onedoc keeps stopping"));
            assertTrue(result.text,text.contains("app info"));assertTrue(result.text,text.contains("close app"));
        }finally{original.recycle();}
    }
    @Test public void selectedRegionRunsFreshInferenceWithoutChangingSource()throws Exception{
        Bitmap source=Bitmap.createBitmap(705,1568,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(source);canvas.drawColor(Color.rgb(57,53,27));Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.rgb(53,56,63));canvas.drawRect(40,650,665,950,paint);paint.setColor(Color.WHITE);paint.setTextSize(32);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));canvas.drawText("APP KEEPS STOPPING",80,730,paint);paint.setTextSize(24);paint.setTypeface(Typeface.DEFAULT);canvas.drawText("App info",220,815,paint);canvas.drawText("Close app",220,895,paint);
        try{OfflineOcr.Result result=OfflineOcr.recognizeRegion(InstrumentationRegistry.getInstrumentation().getTargetContext(),source,new RectF(40f/705,650f/1568,665f/705,950f/1568),"eng",new OfflineOcr.Cancellation());String text=result.text.toLowerCase(Locale.ROOT);assertTrue(result.text,text.contains("app keeps stopping"));assertTrue(result.text,text.contains("app info"));assertTrue(result.text,text.contains("close app"));assertEquals(705,source.getWidth());assertFalse(source.isRecycled());assertEquals(Color.rgb(57,53,27),source.getPixel(0,0));}finally{source.recycle();}
    }
    @Test public void invalidOrCancelledRegionNeverRecyclesOriginal()throws Exception{
        Bitmap source=Bitmap.createBitmap(200,100,Bitmap.Config.ARGB_8888);
        try{for(RectF box:Arrays.asList(new RectF(-1,0,1,1),new RectF(0,0,Float.NaN,1),new RectF(1,1,0,0),new RectF(0,0,2,1))){try{OfflineOcr.recognizeRegion(InstrumentationRegistry.getInstrumentation().getTargetContext(),source,box,"eng",new OfflineOcr.Cancellation());fail("Invalid crop must not run inference");}catch(IOException expected){assertFalse(source.isRecycled());}}
            OfflineOcr.Cancellation cancel=new OfflineOcr.Cancellation();cancel.cancel();try{OfflineOcr.recognizeRegion(InstrumentationRegistry.getInstrumentation().getTargetContext(),source,new RectF(0,0,1,1),"eng",cancel);fail("Cancelled");}catch(InterruptedException expected){assertFalse(source.isRecycled());}
        }finally{source.recycle();}
    }
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
    @Test public void localGalleryDownloadsAndPickerUrisAreAccepted() throws Exception {
        for(String value:Arrays.asList("content://media/external/images/media/42","content://com.android.providers.media.documents/document/image%3A42","content://com.android.providers.downloads.documents/document/msf%3A42","content://media/picker_get_content/0/com.android.providers.media.photopicker/media/42"))OfflineOcr.requireLocal(Uri.parse(value));
        app.space.notes.runtime.LocalBackupFiles.requireLocal(Uri.parse("content://com.android.providers.downloads.documents/document/msf%3A42"));
    }
    @Test public void cloudAndFileUrisAreRefusedBeforeReading() throws Exception {for(String value:Arrays.asList("https://example.invalid/image.png","file:///sdcard/image.png","content://cloud.example/image","content://media/picker/0/cloud.example/media/1","content://media/picker/0/cloud.example/media/com.android.providers.media.photopicker/1")){try{OfflineOcr.requireLocal(Uri.parse(value));fail("cloud input must be refused");}catch(IOException expected){assertTrue(expected.getMessage().contains("on-device"));}}}
    @Test public void blankImageDoesNotInventText() throws Exception {Bitmap bitmap=Bitmap.createBitmap(600,400,Bitmap.Config.ARGB_8888);new Canvas(bitmap).drawColor(Color.WHITE);try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),bitmap,"eng",new OfflineOcr.Cancellation());assertTrue(result.text,result.text.isEmpty());}finally{bitmap.recycle();}}

    @Test public void darkSparseScreenshotReadsNativePrintedTextWithoutMutatingSource()throws Exception{Bitmap image=Bitmap.createBitmap(1000,420,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(Color.BLACK);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.WHITE);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(80);canvas.drawText("MEETING",80,160,paint);canvas.drawText("NOTES 25",80,330,paint);try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),image,"eng",new OfflineOcr.Cancellation());assertTrue(result.text,result.text.toUpperCase(Locale.ROOT).contains("MEETING"));assertTrue(result.text,result.text.contains("25"));assertEquals(Color.BLACK,image.getPixel(0,0));}finally{image.recycle();}}
    @Test public void preprocessingCompositesTransparencyAndBoundsSmallImageUpscale(){Bitmap original=Bitmap.createBitmap(200,100,Bitmap.Config.ARGB_8888);Bitmap prepared=OfflineOcr.prepareBitmap(original);try{assertEquals(400,prepared.getWidth());assertEquals(200,prepared.getHeight());assertEquals(Color.WHITE,prepared.getPixel(0,0));assertEquals(Color.TRANSPARENT,original.getPixel(0,0));}finally{prepared.recycle();original.recycle();}}
    @Test public void printedArithmeticSymbolsAreRecognizedWithRealInference()throws Exception{Bitmap image=Bitmap.createBitmap(1600,360,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTypeface(Typeface.MONOSPACE);paint.setTextSize(100);canvas.drawText("TOTAL 25 + 5 = 30",50,210,paint);try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),image,"eng",new OfflineOcr.Cancellation());assertTrue(result.text,result.text.contains("25"));assertTrue(result.text,result.text.contains("+"));assertTrue(result.text,result.text.contains("="));}finally{image.recycle();}}
    @Test public void darkFrameDoesNotInvertALightDocumentInterior(){Bitmap source=Bitmap.createBitmap(600,400,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(source);canvas.drawColor(Color.BLACK);Paint paint=new Paint();paint.setColor(Color.WHITE);canvas.drawRect(20,20,580,380,paint);Bitmap prepared=OfflineOcr.prepareBitmap(source);try{assertEquals(Color.WHITE,prepared.getPixel(prepared.getWidth()/2,prepared.getHeight()/2));assertEquals(Color.WHITE,source.getPixel(300,200));}finally{source.recycle();prepared.recycle();}}
    @Test public void controlledCommonTextCorpusReportsStrictCharacterErrorWithoutErasingSymbols()throws Exception{
        org.json.JSONArray rows=new org.json.JSONArray();String expected="MEETING NOTES TOTAL 25 + 5 = 30";
        for(int variant=0;variant<3;variant++){Bitmap image=Bitmap.createBitmap(1600,420,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(variant==1?Color.BLACK:variant==2?Color.TRANSPARENT:Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(variant==1?Color.WHITE:Color.BLACK);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(80);canvas.drawText("MEETING NOTES",70,150,paint);canvas.drawText("TOTAL 25 + 5 = 30",70,300,paint);int original=image.getPixel(0,0);try{long began=android.os.SystemClock.elapsedRealtime();OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),image,"eng",new OfflineOcr.Cancellation());String actual=result.text.trim().replaceAll("\\s+"," ");app.space.notes.domain.RecognitionMetrics.Score score=app.space.notes.domain.RecognitionMetrics.characters(expected,actual);assertTrue("CER="+score.rate()+" text="+actual,score.rate()<=.20);assertTrue(actual,actual.contains("+"));assertTrue(actual,actual.contains("="));assertEquals(original,image.getPixel(0,0));rows.put(new JSONObject().put("variant",variant).put("reference",expected).put("recognized",actual).put("cer",score.rate()).put("confidence",result.confidence).put("elapsed_ms",android.os.SystemClock.elapsedRealtime()-began));}finally{image.recycle();}}
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),"ocr-common-text-report.json"))){out.write(new JSONObject().put("scope","Three synthetic system-font variants only, no physical/emoji/handwriting accuracy guarantee").put("cases",rows).toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }

    @Test public void automaticBundledRecognitionNeedsNoLanguageChoiceAndKeepsSymbols()throws Exception{Bitmap image=Bitmap.createBitmap(1600,400,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTypeface(Typeface.MONOSPACE);paint.setTextSize(100);canvas.drawText("TOTAL 25 + 5 = 30",50,200,paint);try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),image,"auto",new OfflineOcr.Cancellation());assertEquals("auto",result.language);assertTrue(result.text,result.text.contains("25"));assertTrue(result.text,result.text.contains("+"));assertTrue(result.text,result.text.contains("="));assertEquals(Color.WHITE,image.getPixel(0,0));}finally{image.recycle();}knownNativeOmissionsKeepExistingTextAndCoordinates();}
    @Test public void nativeWordBoxesMatchTheChosenRecognitionPassAndSourceCoordinates()throws Exception{Bitmap image=Bitmap.createBitmap(1200,400,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTypeface(Typeface.MONOSPACE);paint.setTextSize(90);canvas.drawText("ONE 25 + 5",80,220,paint);try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),image,"eng",new OfflineOcr.Cancellation());assertFalse(result.text,result.words.isEmpty());app.space.notes.domain.ImageTextSelection chosen=new app.space.notes.domain.ImageTextSelection(result.words);chosen.all(true);assertTrue(chosen.text(),chosen.text().contains("25"));for(app.space.notes.domain.ImageTextSelection.Word word:result.words){assertTrue(word.left>=0 && word.top>=0 && word.right<=1 && word.bottom<=1);assertTrue(word.right>word.left && word.bottom>word.top);}assertEquals(Color.WHITE,image.getPixel(0,0));}finally{image.recycle();}}
    @Test public void upscaledSmallImagesKeepNormalizedWordBoxesInsideTheSource()throws Exception{Bitmap image=Bitmap.createBitmap(700,220,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTypeface(Typeface.MONOSPACE);paint.setTextSize(65);canvas.drawText("TOTAL 25",40,130,paint);try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),image,"eng",new OfflineOcr.Cancellation());assertFalse(result.words.isEmpty());assertEquals(700,result.width);for(app.space.notes.domain.ImageTextSelection.Word word:result.words){assertTrue(word.right<=1);assertTrue(word.bottom<=1);assertTrue(word.top>.1f && word.bottom<.9f);}}finally{image.recycle();}}


    private void knownNativeOmissionsKeepExistingTextAndCoordinates()throws Exception{
        for(int[] fixture:new int[][]{{48,0},{72,1}}){Bitmap image=Bitmap.createBitmap(2400,1000,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(fixture[1]==1?Color.BLACK:Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(fixture[1]==1?Color.WHITE:Color.BLACK);paint.setTypeface(Typeface.create("monospace",Typeface.ITALIC));paint.setTextSize(fixture[0]);canvas.rotate(2,1200,500);canvas.drawText("ಕನ್ನಡ ಭಾಷೆ",70,150,paint);canvas.drawText("ಭಾರತ ನಮಸ್ಕಾರ",70,280,paint);int generation=image.getGenerationId();
            try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),image,"auto",new OfflineOcr.Cancellation());String expected="ಕನ್ನಡ ಭಾಷೆ ಭಾರತ ನಮಸ್ಕಾರ";assertEquals(expected,result.text.trim().replaceAll("\\s+"," "));app.space.notes.domain.ImageTextSelection selection=new app.space.notes.domain.ImageTextSelection(result.words);selection.all(true);assertEquals(expected,selection.text().trim().replaceAll("\\s+"," "));assertEquals("OCR must never alter source pixels",generation,image.getGenerationId());}catch(AssertionError failure){try{writePublicNativeFailureProbe(image);}catch(Exception diagnostic){android.util.Log.w("NotesPublicBenchmark","Public OCR probe unavailable",diagnostic);}throw failure;}finally{image.recycle();}
        }
    }
    /** Failure-only probes of the OWN-AUTHORED fixture, never user image data or an output oracle. */
    private void writePublicNativeFailureProbe(Bitmap image)throws Exception{
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),"public-native-ocr-input.png"))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}
        org.json.JSONArray rows=new org.json.JSONArray();
        for(String language:new String[]{"eng+hin+kan","kan"}){
            com.googlecode.tesseract.android.TessBaseAPI engine=new com.googlecode.tesseract.android.TessBaseAPI();
            try{
                if(!engine.init(new File(context.getFilesDir(),"offline-ocr-v1").getAbsolutePath(),language))throw new IOException("Public probe model unavailable");
                engine.setVariable("preserve_interword_spaces","1");
                for(int threshold:new int[]{0,2})for(int mode:new int[]{3,6,11}){
                    engine.setVariable("thresholding_method",String.valueOf(threshold));engine.setPageSegMode(mode);engine.setImage(image);
                    long began=android.os.SystemClock.elapsedRealtime();String text=engine.getUTF8Text();
                    rows.put(new JSONObject().put("language",language).put("threshold",threshold).put("psm",mode).put("recognized",text==null?"":text).put("confidence",engine.meanConfidence()).put("elapsed_ms",android.os.SystemClock.elapsedRealtime()-began));
                }
            }finally{engine.recycle();}
        }
        JSONObject report=new JSONObject().put("scope","Failure-only public synthetic fixture; probes never select production output or replace original assertion").put("api",android.os.Build.VERSION.SDK_INT).put("cases",rows);
        try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),"public-native-ocr-probe.json"))){out.write(report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }

}
