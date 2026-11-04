package app.space.notes;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import app.space.notes.domain.RecognitionMetrics;
import app.space.notes.runtime.OfflineOcr;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
/** Evaluation only. References never enter recognizer/model selection. French is unsupported. */
@RunWith(AndroidJUnit4.class)
public class AdditionalPublicSceneStudy {
 private byte[] asset(String name)throws Exception{try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("public-scenes-v3/"+name)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];for(int n;(n=in.read(buffer))!=-1;){out.write(buffer,0,n);assertTrue(out.size()<2000000);}return out.toByteArray();}}
 private String hash(byte[] data)throws Exception{return OfflineOcr.digest(new ByteArrayInputStream(data));}
 private String pixels(Bitmap image)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));return hash(out.toByteArray());}
 private String normalize(String value){return java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFC).trim().replaceAll("\\s+"," ");}
 @Test public void originalPosterAndObliqueUnsupportedLatinSignsRetainAllOutputs()throws Exception{
  JSONObject manifest=new JSONObject(new String(asset("manifest.json"),StandardCharsets.UTF_8));assertEquals(manifest.getString("license_sha256"),hash(asset("LICENSE")));JSONArray records=manifest.getJSONArray("records"),rows=new JSONArray();assertEquals(2,records.length());int errors=0;
  for(int i=0;i<records.length();i++){
   JSONObject source=records.getJSONObject(i);byte[] raw=asset(source.getString("file"));assertEquals(source.getString("sha256"),hash(raw));BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(raw,0,raw.length,bounds);assertEquals(source.getInt("width"),bounds.outWidth);assertEquals(source.getInt("height"),bounds.outHeight);assertTrue((long)bounds.outWidth*bounds.outHeight<=4000000);Bitmap image=BitmapFactory.decodeByteArray(raw,0,raw.length);assertNotNull(image);String before=pixels(image);JSONObject row=new JSONObject().put("id",source.getString("id")).put("condition",source.getString("condition")).put("reference",source.getString("reference")).put("source_sha256",hash(raw)).put("input_pixels_sha256",before);long began=SystemClock.elapsedRealtime();
   try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(InstrumentationRegistry.getInstrumentation().getTargetContext(),image,"auto",new OfflineOcr.Cancellation());row.put("elapsed_ms",SystemClock.elapsedRealtime()-began).put("status","OK");String reference=source.getString("reference");RecognitionMetrics.Score score=RecognitionMetrics.characters(normalize(reference),normalize(result.text));row.put("reference",reference).put("recognized",result.text).put("cer",score.rate()).put("character_edits",score.edits).put("reference_characters",score.referenceUnits);assertEquals(before,pixels(image));row.put("source_pixels_unchanged",true);}catch(Exception error){errors++;row.put("status","ERROR").put("error",error.toString()).put("elapsed_ms",SystemClock.elapsedRealtime()-began);}finally{image.recycle();}rows.put(row);
   JSONObject report=new JSONObject().put("schema",1).put("expected",2).put("cases",rows).put("production_changed",false).put("model_changed",false).put("training_steps",0).put("physical_acceptance",false).put("scope","Original English illustrated poster and unsupported-French outdoor signs; not French support or untouched heldout data; emulator inference only, first call includes cold model setup, decode/scoring excluded.");try(OutputStream out=new FileOutputStream(new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"additional-public-scenes.json"))){out.write(report.toString().getBytes(StandardCharsets.UTF_8));}
  }assertEquals("Operational errors retained; accuracy misses must stay visible",0,errors);assertEquals(2,rows.length());
 }
}
