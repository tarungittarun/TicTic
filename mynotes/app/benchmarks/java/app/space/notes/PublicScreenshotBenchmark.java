package app.space.notes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import app.space.notes.domain.RecognitionMetrics;
import app.space.notes.runtime.OfflineOcr;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Opt-in, public evaluation only. Neither images nor this class ship in Notes. */
@RunWith(AndroidJUnit4.class)
public class PublicScreenshotBenchmark {
    private byte[] asset(String name) throws Exception {
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("public-screenshots/"+name)) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
            while((n=in.read(b))!=-1){out.write(b,0,n);assertTrue("Bounded public asset",out.size()<2000000);}
            return out.toByteArray();
        }
    }
    private String hash(byte[] bytes)throws Exception{return OfflineOcr.digest(new ByteArrayInputStream(bytes));}
    private byte[] png(Bitmap image){ByteArrayOutputStream out=new ByteArrayOutputStream();assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));return out.toByteArray();}
    private String normalized(String value){return java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFC).trim().replaceAll("\\s+"," ");}
    private void persist(JSONArray rows,int errors,int misses)throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        JSONObject report=new JSONObject().put("schema",1).put("expected",6).put("cases",rows).put("operational_errors",errors).put("accuracy_misses",misses).put("all_accuracy_targets_met",rows.length()==6&&errors==0&&misses==0)
            .put("scope","Public third-party Android screenshots and labelled half-size/JPEG derivatives; not camera photos, private cases, untouched heldout data or physical acceptance")
            .put("timing_scope","Emulator wall-clock production inference only; first call includes cold model provisioning; excludes input decode/transform and scoring")
            .put("model_changed",false).put("training_steps",0).put("physical_acceptance",false);
        try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),"public-screenshot-benchmark.json"))){out.write(report.toString().getBytes(StandardCharsets.UTF_8));}
    }
    @Test public void verifiedPublicScreenshotsMeasureProductionRecognitionAndSourcePreservation()throws Exception {
        JSONObject manifest=new JSONObject(new String(asset("manifest.json"),StandardCharsets.UTF_8));
        assertEquals(manifest.getString("license_sha256"),hash(asset("LICENSE")));
        JSONArray records=manifest.getJSONArray("records"),rows=new JSONArray();assertEquals(2,records.length());int errors=0,misses=0;
        for(int i=0;i<records.length();i++)for(int variant=0;variant<3;variant++){
            JSONObject record=records.getJSONObject(i);byte[] original=asset(record.getString("file"));assertEquals(record.getString("sha256"),hash(original));
            Bitmap decoded=BitmapFactory.decodeByteArray(original,0,original.length);assertNotNull(decoded);
            assertEquals(record.getInt("width"),decoded.getWidth());assertEquals(record.getInt("height"),decoded.getHeight());
            Bitmap image=decoded;byte[] encoded=original;
            if(variant==1){image=Bitmap.createScaledBitmap(decoded,decoded.getWidth()/2,decoded.getHeight()/2,true);encoded=png(image);}
            if(variant==2){ByteArrayOutputStream jpeg=new ByteArrayOutputStream();assertTrue(decoded.compress(Bitmap.CompressFormat.JPEG,50,jpeg));encoded=jpeg.toByteArray();image=BitmapFactory.decodeByteArray(encoded,0,encoded.length);assertNotNull(image);}
            String pixelsBefore=hash(png(image));JSONObject row=new JSONObject().put("id",record.getString("id")+"-"+variant).put("condition",variant==0?"original":variant==1?"half-size-derived":"jpeg50-derived")
                .put("source_sha256",hash(original)).put("input_sha256",hash(encoded)).put("input_bytes",encoded.length).put("width",image.getWidth()).put("height",image.getHeight()).put("language_request","auto");
            long began=SystemClock.elapsedRealtime();
            try {
                // Frozen reference is NOT passed to recognition or output selection.
                OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),image,"auto",new OfflineOcr.Cancellation());
                row.put("elapsed_ms",SystemClock.elapsedRealtime()-began);
                String reference=record.getString("reference");RecognitionMetrics.Score score=RecognitionMetrics.characters(normalized(reference),normalized(result.text));
                boolean met=score.rate()<=.15;if(!met)misses++;
                row.put("status","OK").put("reference",reference).put("recognized",result.text).put("cer",score.rate()).put("character_edits",score.edits).put("reference_characters",score.referenceUnits).put("accuracy_target_met",met).put("selected_language",result.language);
                assertEquals("Input pixels must survive inference",pixelsBefore,hash(png(image)));row.put("source_pixels_unchanged",true);
            } catch(Exception failure){errors++;row.put("status","ERROR").put("error",failure.toString()).put("elapsed_ms",SystemClock.elapsedRealtime()-began).put("accuracy_target_met",false);}
            finally{if(image!=decoded)image.recycle();decoded.recycle();}
            assertEquals(record.getString("sha256"),hash(asset(record.getString("file"))));rows.put(row);persist(rows,errors,misses);
        }
        assertEquals(6,rows.length());assertEquals("All operational errors retained",0,errors);assertEquals("Public accuracy misses retained, never silently qualified",0,misses);
    }
}
