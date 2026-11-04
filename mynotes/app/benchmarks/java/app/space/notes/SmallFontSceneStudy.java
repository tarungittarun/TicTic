package app.space.notes;

import android.content.Context;
import android.graphics.*;
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
import java.util.*;
import static org.junit.Assert.*;

/** Evaluation only: references score results AFTER inference, never select app output.
 * Candidate crop enlargement is NOT a production change or an accuracy claim.
 */
@RunWith(AndroidJUnit4.class)
public class SmallFontSceneStudy {
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private byte[] asset(String name)throws Exception{try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("public-scenes-v2/"+name)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];for(int n;(n=in.read(buffer))!=-1;){out.write(buffer,0,n);assertTrue(out.size()<2000000);}return out.toByteArray();}}
    private String hash(byte[] data)throws Exception{return OfflineOcr.digest(new ByteArrayInputStream(data));}
    private String pixels(Bitmap image)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));return hash(out.toByteArray());}
    private String normalized(String value){return java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFC).trim().replaceAll("\\s+"," ");}
    private void score(JSONObject row,String reference,String actual)throws Exception{
        RecognitionMetrics.Score result=RecognitionMetrics.characters(normalized(reference),normalized(actual));int foreign=0;for(int at=0;at<actual.length();){int cp=actual.codePointAt(at);at+=Character.charCount(cp);if(Character.isLetter(cp) && Character.UnicodeScript.of(cp)!=Character.UnicodeScript.LATIN)foreign++;}
        row.put("reference",reference).put("recognized",actual).put("cer",result.rate()).put("character_edits",result.edits).put("reference_characters",result.referenceUnits).put("non_latin_letters",foreign).put("empty_control_false_positive",reference.isEmpty()&&!actual.trim().isEmpty());
    }
    private void persist(String name,JSONArray rows,int expected)throws Exception{
        JSONObject report=new JSONObject().put("schema",1).put("expected",expected).put("cases",rows).put("production_changed",false).put("model_changed",false).put("training_steps",0).put("physical_acceptance",false).put("timing_scope","Emulator inference wall clock; first call includes cold model provisioning. Decode/synthesis/upscaling/scoring excluded. Counterbalanced order in the paired font study; no physical-device speed claim.");
        try(OutputStream out=new FileOutputStream(new File(context().getFilesDir(),name+".json"))){out.write(report.toString().getBytes(StandardCharsets.UTF_8));}
    }
    @Test public void frozenPhotoDialogAndNegativeSceneInputsRetainFailures()throws Exception{
        JSONObject manifest=new JSONObject(new String(asset("manifest.json"),StandardCharsets.UTF_8));assertEquals(manifest.getString("license_sha256"),hash(asset("LICENSE")));JSONArray records=manifest.getJSONArray("records"),rows=new JSONArray();assertEquals(5,records.length());int errors=0;
        for(int i=0;i<records.length();i++){
            JSONObject source=records.getJSONObject(i);byte[] raw=asset(source.getString("file"));JSONObject original=source;if(!source.has("sha256"))for(int j=0;j<3;j++)if(records.getJSONObject(j).getString("file").equals(source.getString("file")))original=records.getJSONObject(j);assertEquals(original.getString("sha256"),hash(raw));Bitmap decoded=BitmapFactory.decodeByteArray(raw,0,raw.length);JSONArray box=source.getJSONArray("crop");Bitmap image=Bitmap.createBitmap(decoded,box.getInt(0),box.getInt(1),box.getInt(2)-box.getInt(0),box.getInt(3)-box.getInt(1));String before=pixels(image);JSONObject row=new JSONObject().put("id",source.getString("id")).put("condition",source.getString("condition")).put("source_sha256",hash(raw)).put("input_pixels_sha256",before).put("crop",box).put("width",image.getWidth()).put("height",image.getHeight());long began=SystemClock.elapsedRealtime();
            try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(context(),image,"auto",new OfflineOcr.Cancellation());row.put("elapsed_ms",SystemClock.elapsedRealtime()-began).put("status","OK");score(row,source.getString("reference"),result.text);assertEquals(before,pixels(image));row.put("source_pixels_unchanged",true);}catch(Exception error){errors++;row.put("status","ERROR").put("error",error.toString()).put("elapsed_ms",SystemClock.elapsedRealtime()-began);}finally{if(image!=decoded)image.recycle();decoded.recycle();}rows.put(row);persist("public-scene-study",rows,5);
        }
        assertEquals(5,rows.length());assertEquals("Operational errors retained; accuracy misses are not suppressed",0,errors);
    }
    @Test public void pairedSmallFontCropEnlargementRecordsActualOutputsBeforeAdoption()throws Exception{
        JSONArray rows=new JSONArray();int errors=0,index=0;String[] scripts={"English meeting at 10 AM","भारत नमस्ते","ಕನ್ನಡ ಭಾಷೆ"};
        for(String family:new String[]{"sans-serif","serif","monospace"})for(int size:new int[]{8,10,12,16})for(boolean dark:new boolean[]{false,true})for(String reference:scripts){
            Bitmap crop=Bitmap.createBitmap(400,64,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(crop);canvas.drawColor(dark?Color.BLACK:Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(dark?Color.WHITE:Color.BLACK);paint.setTextSize(size);paint.setTypeface(Typeface.create(family,Typeface.NORMAL));canvas.drawText(reference,12,32,paint);Bitmap enlarged=Bitmap.createScaledBitmap(crop,1600,256,true);String original=pixels(crop);
            // Alternate execution order to avoid always giving the candidate a warm engine/model advantage.
            for(int attempt=0;attempt<2;attempt++){boolean candidate=(attempt+(index%2))%2==1;Bitmap input=candidate?enlarged:crop;JSONObject row=new JSONObject().put("id","font-"+index+(candidate?"-candidate4x":"-baseline")).put("font",family).put("font_px",size).put("dark",dark).put("candidate",candidate).put("input_width",input.getWidth()).put("input_height",input.getHeight()).put("source_pixels_sha256",original);long began=SystemClock.elapsedRealtime();
                try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(context(),input,"auto",new OfflineOcr.Cancellation());row.put("status","OK").put("elapsed_ms",SystemClock.elapsedRealtime()-began);score(row,reference,result.text);assertEquals(original,pixels(crop));row.put("source_pixels_unchanged",true);}catch(Exception error){errors++;row.put("status","ERROR").put("error",error.toString()).put("elapsed_ms",SystemClock.elapsedRealtime()-began);}rows.put(row);persist("small-font-paired-study",rows,144);
            }enlarged.recycle();crop.recycle();index++;
        }assertEquals(72,index);assertEquals(144,rows.length());assertEquals(0,errors);
    }
}
